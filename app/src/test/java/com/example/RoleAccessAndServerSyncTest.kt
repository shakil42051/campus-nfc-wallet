package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.attendance.service.AuthenticationService
import com.example.attendance.service.DeviceService
import com.example.attendance.service.SyncService
import com.example.data.local.database.CardDatabase
import com.example.data.model.NfcScanResult
import com.example.data.model.SecurityAnalysis
import com.example.data.repository.CardRepository
import com.example.security.CampusRoles
import com.example.security.CryptoProvider
import com.example.security.RoleAccessManager
import com.example.security.RoleAuthResult
import com.example.security.RoleSessionSource
import com.example.sync.CampusServerSyncService
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoleAccessAndServerSyncTest {

    private class TestCryptoProvider : CryptoProvider {
        override fun encrypt(plainText: String): String = "ENC:$plainText"
        override fun decrypt(cipherTextBase64: String): String = cipherTextBase64.removePrefix("ENC:")
    }

    @Test
    fun roleAccessManager_enforcesLeastPrivilegePasscodesAndLockout() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rbac = RoleAccessManager(context)

        // Default cold-start state must always be STUDENT
        assertEquals(CampusRoles.STUDENT, rbac.state.value.activeRole)
        assertFalse(rbac.state.value.canEditAttendanceRecords)
        assertFalse(rbac.state.value.canManageServerAndRoles)
        assertFalse(rbac.state.value.canClearLogsOrRecords)

        // Short passcode rejected
        assertTrue(rbac.initializeAdminPasscode("1234", null).isFailure)

        // Valid Admin + Teacher passcode initialization
        assertTrue(rbac.initializeAdminPasscode("AdminSecret#2026", "Teach123").isSuccess)
        assertTrue(rbac.state.value.isAdmin)
        assertTrue(rbac.state.value.canManageServerAndRoles)
        assertTrue(rbac.state.value.hasAdminPasscode)
        assertTrue(rbac.state.value.hasTeacherPasscode)

        // Drop back to Student
        rbac.lockElevatedSession()
        assertEquals(CampusRoles.STUDENT, rbac.state.value.activeRole)
        assertFalse(rbac.state.value.canManageServerAndRoles)

        // Student cannot change staff passcodes
        assertTrue(rbac.setTeacherPasscode("NewTeach99").isFailure)

        // Teacher cannot unlock ADMIN with Teacher passcode
        val wrongEscalation = rbac.unlockLocalRole(CampusRoles.ADMIN, "Teach123")
        assertTrue(wrongEscalation is RoleAuthResult.InvalidCredentials)
        assertEquals(CampusRoles.STUDENT, rbac.state.value.activeRole)

        // Teacher CAN unlock TEACHER role
        val teacherOk = rbac.unlockLocalRole(CampusRoles.TEACHER, "Teach123")
        assertTrue(teacherOk is RoleAuthResult.Success)
        assertTrue(rbac.state.value.canEditAttendanceRecords)
        assertFalse(rbac.state.value.canManageServerAndRoles)

        // Lockout after 5 failed attempts
        rbac.lockElevatedSession()
        repeat(4) {
            val r = rbac.unlockLocalRole(CampusRoles.ADMIN, "wrongpass")
            assertTrue(r is RoleAuthResult.InvalidCredentials)
        }
        val fifth = rbac.unlockLocalRole(CampusRoles.ADMIN, "wrongpass")
        assertTrue(fifth is RoleAuthResult.LockedOut)
        assertTrue(rbac.getRemainingLockoutSeconds() > 0)
    }

    @Test
    fun campusServerSync_realHttpLoginAndFullWalletSyncEndToEnd() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, CardDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val crypto = TestCryptoProvider()
        val cardRepo = CardRepository(db.cardDao(), db.transactionDao(), crypto)
        val authService = AuthenticationService(crypto)
        val deviceService = DeviceService()
        val attendanceSync = SyncService(
            attendanceDao = db.attendanceDao(),
            configDao = db.attendanceConfigDao(),
            logDao = db.attendanceLogDao(),
            deviceService = deviceService,
            authService = authService
        )
        val rbac = RoleAccessManager(context)
        val serverSync = CampusServerSyncService(
            cardRepository = cardRepo,
            configDao = db.attendanceConfigDao(),
            logDao = db.attendanceLogDao(),
            authService = authService,
            attendanceSyncService = attendanceSync,
            roleAccessManager = rbac
        )

        var receivedAuthHeaderOnCards: String? = null
        var pushedCardsCount = 0
        var pushedTxCount = 0

        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        httpServer.createContext("/api/v1/auth/login") { exchange ->
            val reqBody = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(reqBody)
            if (json.optString("username") == "faculty@campus.edu" && json.optString("password") == "FacultyPass!2026") {
                val resp = JSONObject().apply {
                    put("token", "jwt-verified-token-xyz")
                    put("role", "TEACHER")
                    put("userId", "FAC-2026")
                    put("name", "Dr. Campus Faculty")
                    put("department", "Computer Science")
                }.toString().toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(200, resp.size.toLong())
                exchange.responseBody.use { it.write(resp) }
            } else {
                val resp = """{"message":"Invalid credentials"}""".toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(401, resp.size.toLong())
                exchange.responseBody.use { it.write(resp) }
            }
        }
        httpServer.createContext("/api/v1/wallet/sync/cards") { exchange ->
            receivedAuthHeaderOnCards = exchange.requestHeaders.getFirst("Authorization")
            val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() }
            pushedCardsCount = JSONObject(body).optJSONArray("cards")?.length() ?: 0
            val resp = """{"status":"ok","serverCards":[{"uid":"04:99:88:77:66:55:44","nickname":"Server Issued Library Pass","manufacturer":"NXP Semiconductors","technology":"MIFARE DESFire"}]}"""
                .toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, resp.size.toLong())
            exchange.responseBody.use { it.write(resp) }
        }
        httpServer.createContext("/api/v1/wallet/sync/transactions") { exchange ->
            val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() }
            pushedTxCount = JSONObject(body).optJSONArray("transactions")?.length() ?: 0
            val resp = """{"status":"ok"}""".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, resp.size.toLong())
            exchange.responseBody.use { it.write(resp) }
        }
        httpServer.start()

        try {
            val port = httpServer.address.port

            // 1. Wrong password must fail honestly with HTTP 401 and stay STUDENT
            val badLogin = serverSync.authenticateWithServer("127.0.0.1", port, false, "faculty@campus.edu", "wrong")
            assertFalse(badLogin.isSuccess)
            assertEquals(401, badLogin.httpStatus)
            assertEquals(CampusRoles.STUDENT, rbac.state.value.activeRole)

            // 2. Valid login elevates role strictly from server response (TEACHER)
            val goodLogin = serverSync.authenticateWithServer("127.0.0.1", port, false, "faculty@campus.edu", "FacultyPass!2026")
            assertTrue(goodLogin.isSuccess)
            assertEquals(CampusRoles.TEACHER, rbac.state.value.activeRole)
            assertEquals(RoleSessionSource.SERVER_VERIFIED, rbac.state.value.sessionSource)
            assertTrue(rbac.state.value.canEditAttendanceRecords)
            assertFalse(rbac.state.value.canManageServerAndRoles)

            // 3. Save a local card & transaction, then sync wallet with server
            val sampleScan = NfcScanResult(
                uid = "04:11:22:33:44:55:66",
                uidHex = "04112233445566",
                uidAscii = "test",
                uidBinary = "00000100",
                uidLengthBytes = 7,
                manufacturer = "NXP Semiconductors",
                primaryTechnology = "MIFARE DESFire",
                cardStandard = "ISO/IEC 14443-4",
                technologies = listOf("NfcA", "IsoDep"),
                atqa = "0x0344",
                sak = "0x20",
                ats = "N/A",
                historicalBytes = "N/A",
                memorySizeBytes = "4096",
                blockCount = "N/A",
                pageCount = "N/A",
                isNdefSupported = false,
                isNdefWritable = false,
                isNdefReadOnly = true,
                ndefMaxSizeBytes = 0,
                ndefRecords = emptyList(),
                memorySectors = emptyList(),
                securityAnalysis = SecurityAnalysis("AES-128", false, "None", "Hardware AES", listOf("AES")),
                rawDataDumpHex = ""
            )
            val cardId = cardRepo.saveCardFromScan(sampleScan, "Faculty Main Badge", "Lab Gate")
            cardRepo.logTransaction(cardId, "Faculty Main Badge", sampleScan.uid, "Access Control", "Lab Entry", "SUCCESS", "CS Lab")

            val syncResult = serverSync.syncAllToServer()
            assertTrue(syncResult.message, syncResult.isSuccess)
            assertEquals("Bearer jwt-verified-token-xyz", receivedAuthHeaderOnCards)
            assertEquals(1, pushedCardsCount)
            assertEquals(1, pushedTxCount)
            assertEquals(1, syncResult.cardsPulled)

            // Verify server-provisioned card was imported into encrypted local Room vault
            val allCardsAfterSync = cardRepo.allCards.first()
            assertEquals(2, allCardsAfterSync.size)
            assertTrue(allCardsAfterSync.any { it.uid == "04:99:88:77:66:55:44" })

            // 4. Unlinking clears session token & drops role back to STUDENT
            serverSync.unlinkServer()
            assertEquals(CampusRoles.STUDENT, rbac.state.value.activeRole)
            val storedCfg = db.attendanceConfigDao().getConfig()!!
            assertFalse(storedCfg.isConnected)
            assertEquals("", storedCfg.authToken)
        } finally {
            httpServer.stop(0)
            db.close()
        }
    }
}
