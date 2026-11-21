package com.example.attendance.ui

import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.attendance.data.entity.AttendanceEntity
import com.example.attendance.data.entity.AttendanceLogEntity
import com.example.attendance.domain.model.AttendanceStats
import com.example.data.local.entity.CardEntity
import com.example.nfc.rememberNfcHardwareState
import com.example.security.CampusRoles
import com.example.security.RoleAccessManager
import com.example.security.RoleAccessState
import com.example.security.RoleSessionSource
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.NfcDisabledPopupDialog
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldChip
import com.example.ui.theme.IndigoPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class AttendanceTab(val label: String, val icon: ImageVector, val testTag: String) {
    TERMINAL("Terminal", Icons.Default.Nfc, "tab_card_punch"),
    DASHBOARD("Dashboard", Icons.Default.Fingerprint, "tab_attendance"),
    ANALYTICS("Analytics", Icons.Default.PieChart, "tab_analytics"),
    PROFILE("User Profile", Icons.Default.AccountCircle, "tab_user_profile"),
    SERVER_CONFIG("Server Config", Icons.Default.Settings, "tab_server_config"),
    AUDIT_LOGS("Audit Logs", Icons.Default.History, "tab_audit_logs")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val roleState by viewModel.roleState.collectAsStateWithLifecycle()
    val filteredRecords by viewModel.filteredRecords.collectAsStateWithLifecycle()
    val allRecords by viewModel.records.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val savedCards by viewModel.savedCards.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val isAuthenticatingServer by viewModel.isAuthenticatingServer.collectAsStateWithLifecycle()
    val testResult by viewModel.testResult.collectAsStateWithLifecycle()
    val syncFeedback by viewModel.syncFeedback.collectAsStateWithLifecycle()
    val timeRange by viewModel.selectedTimeRange.collectAsStateWithLifecycle()

    val isAdmin = roleState.canManageServerAndRoles
    val canEditStatus = roleState.canEditAttendanceRecords

    val visibleTabs = remember(roleState.activeRole) {
        when (roleState.activeRole) {
            CampusRoles.ADMIN -> AttendanceTab.entries.toList()
            CampusRoles.TEACHER -> listOf(
                AttendanceTab.TERMINAL,
                AttendanceTab.DASHBOARD,
                AttendanceTab.ANALYTICS,
                AttendanceTab.PROFILE,
                AttendanceTab.AUDIT_LOGS
            )
            else -> listOf(
                AttendanceTab.TERMINAL,
                AttendanceTab.DASHBOARD,
                AttendanceTab.ANALYTICS,
                AttendanceTab.PROFILE
            )
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val safeTabIndex = selectedTabIndex.coerceIn(0, visibleTabs.lastIndex)
    if (safeTabIndex != selectedTabIndex) {
        selectedTabIndex = safeTabIndex
    }

    var showTestDialog by remember { mutableStateOf(false) }
    var showRoleAuthDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    LaunchedEffect(syncFeedback) {
        syncFeedback?.let { fb ->
            Toast.makeText(context, fb.message, Toast.LENGTH_SHORT).show()
            viewModel.clearSyncFeedback()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Surface
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Attendance",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Campus Attendance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = config.providerType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = config.activeStudentName.ifBlank { "Unlinked Profile" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Sync Button (48dp touch target)
                    IconButton(
                        onClick = { viewModel.performManualSync() },
                        enabled = !isSyncing,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_manual_sync")
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                shape = CircleShape
                            )
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Attendance Now",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Connection Status & Role Switcher Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusTint = if (config.isConnected) EmeraldSuccess else GoldChip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusTint.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, statusTint.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusTint, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    config.isConnected -> "${config.providerType} Online"
                                    config.serverUrl.isBlank() -> "Local Mode"
                                    else -> "Server Offline"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusTint
                            )
                        }
                    }

                    // Protected Role Access Indicator & Staff/Admin Auth Control (no open privilege switching!)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val roleTint = when (roleState.activeRole) {
                            CampusRoles.ADMIN -> CrimsonAlert
                            CampusRoles.TEACHER -> IndigoPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = roleTint.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, roleTint.copy(alpha = 0.45f)),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (roleState.activeRole) {
                                        CampusRoles.ADMIN -> Icons.Default.AdminPanelSettings
                                        CampusRoles.TEACHER -> Icons.Default.Security
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = null,
                                    tint = roleTint,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = buildString {
                                        append(roleState.activeRole.lowercase(Locale.US).replaceFirstChar { it.uppercase() })
                                        if (roleState.sessionSource == RoleSessionSource.SERVER_VERIFIED) {
                                            append(" • Server")
                                        }
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = roleTint
                                )
                            }
                        }

                        if (roleState.isStudent) {
                            Surface(
                                onClick = { showRoleAuthDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = IndigoPrimary,
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .testTag("btn_staff_admin_login")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Staff or Admin Sign In",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Staff Login",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Surface(
                                onClick = { viewModel.dropToStudentRole() },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .testTag("btn_lock_staff_session")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock Staff Session",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Lock Role",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Role-aware Scrollable Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = safeTabIndex,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            visibleTabs.forEachIndexed { index, tab ->
                Tab(
                    selected = safeTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(tab.label, maxLines = 1) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    modifier = Modifier.testTag(tab.testTag)
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (visibleTabs[safeTabIndex]) {
                AttendanceTab.TERMINAL -> CardPunchTerminalView(
                    config = config,
                    savedCards = savedCards,
                    records = allRecords,
                    canEditStatus = canEditStatus,
                    onPunchWithCard = { cardUid, cardNickname, type, location, status ->
                        viewModel.punchWithCard(cardUid, cardNickname, type, location, status)
                    },
                    onUpdateRecordStatus = { id, status ->
                        viewModel.updateRecordStatus(id, status)
                    },
                    dateFormat = dateFormat
                )
                AttendanceTab.DASHBOARD -> AttendanceDashboardView(
                    config = config,
                    records = filteredRecords,
                    stats = stats,
                    timeRange = timeRange,
                    canEditStatus = canEditStatus,
                    onTimeRangeChange = { viewModel.setTimeRange(it) },
                    onPunch = { type, loc, status -> viewModel.punchAttendance(type, loc, status) },
                    onUpdateRecordStatus = { id, status -> viewModel.updateRecordStatus(id, status) },
                    dateFormat = dateFormat
                )
                AttendanceTab.ANALYTICS -> AttendanceAnalyticsView(
                    stats = stats,
                    records = if (config.activeStudentId.isNotBlank()) {
                        allRecords.filter { it.studentId == config.activeStudentId }
                    } else {
                        allRecords
                    }
                )
                AttendanceTab.PROFILE -> UserProfileSettingsView(
                    config = config,
                    roleState = roleState,
                    savedCards = savedCards,
                    onUpdateProfile = { name, id, dept, email, cardUid, cardNickname ->
                        viewModel.updateProfile(name, id, dept, email, cardUid, cardNickname)
                    }
                )
                AttendanceTab.SERVER_CONFIG -> ServerConfigAdminView(
                    config = config,
                    roleState = roleState,
                    isTesting = isTesting,
                    onSaveConfig = { viewModel.saveServerConfig(it) },
                    onTestConnection = {
                        viewModel.testServerConnection(it)
                        showTestDialog = true
                    },
                    onSetTeacherPasscode = { pass, cb -> viewModel.setTeacherPasscode(pass, cb) },
                    onRotateAdminPasscode = { cur, next, cb -> viewModel.rotateAdminPasscode(cur, next, cb) },
                    onUnlinkServer = { viewModel.unlinkServerSession() }
                )
                AttendanceTab.AUDIT_LOGS -> AuditLogsView(
                    logs = logs,
                    canClearLogs = isAdmin,
                    onClearLogs = { viewModel.clearLogs() },
                    dateFormat = dateFormat
                )
            }
        }
    }

    if (showRoleAuthDialog) {
        RoleAccessAuthDialog(
            config = config,
            roleState = roleState,
            isAuthenticatingServer = isAuthenticatingServer,
            getRemainingLockoutSeconds = { viewModel.getRemainingRoleLockoutSeconds() },
            onDismiss = { showRoleAuthDialog = false },
            onAuthenticateServer = { host, port, https, user, pass, cb ->
                viewModel.authenticateWithServer(host, port, https, user, pass, cb)
            },
            onInitializeAdminPasscode = { adminPass, teacherPass, cb ->
                viewModel.initializeAdminPasscode(adminPass, teacherPass, cb)
            },
            onUnlockLocalRole = { target, pass, cb ->
                viewModel.unlockLocalRole(target, pass, cb)
            }
        )
    }

    // Connection Test Result Dialog (only shown once testResult finishes)
    if (showTestDialog && !isTesting && testResult != null) {
        val result = testResult!!
        AlertDialog(
            onDismissRequest = {
                showTestDialog = false
                viewModel.clearTestResult()
            },
            icon = {
                Icon(
                    imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (result.isSuccess) EmeraldSuccess else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (result.isSuccess) "${result.provider} Connection Verified" else "${result.provider} Connection Failed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(text = result.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Provider: ${result.provider} | Latency: ${result.latencyMs}ms",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showTestDialog = false
                    viewModel.clearTestResult()
                }) {
                    Text("OK")
                }
            }
        )
    }
}

// ==========================================
// 1. LOCAL ATTENDANCE PUNCH TERMINAL VIEW
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardPunchTerminalView(
    config: AttendanceConfigEntity,
    savedCards: List<CardEntity>,
    records: List<AttendanceEntity>,
    canEditStatus: Boolean,
    onPunchWithCard: (String, String, String, String, String) -> Unit,
    onUpdateRecordStatus: (Long, String) -> Unit,
    dateFormat: SimpleDateFormat
) {
    val nfcState = rememberNfcHardwareState()
    var showNfcOffDialog by remember { mutableStateOf(false) }

    var selectedCardUid by remember(config.linkedCardUid, savedCards) {
        mutableStateOf(
            config.linkedCardUid.ifBlank { savedCards.firstOrNull()?.uid ?: "LOCAL-PASS" }
        )
    }
    var selectedCardNickname by remember(config.linkedCardNickname, savedCards) {
        mutableStateOf(
            config.linkedCardNickname.ifBlank { savedCards.firstOrNull()?.nickname ?: "Unlinked Campus Pass" }
        )
    }

    var selectedLocation by remember { mutableStateOf("Main Gate #1 Turnstile") }
    var selectedPunchType by remember { mutableStateOf("PUNCH_IN") }
    var selectedStatus by remember { mutableStateOf("PRESENT") }
    var showSuccessBanner by remember { mutableStateOf(false) }
    var lastPunchTimeText by remember { mutableStateOf("") }

    var expandedLocationMenu by remember { mutableStateOf(false) }
    var expandedCardMenu by remember { mutableStateOf(false) }

    val locations = listOf(
        "Main Gate #1 Turnstile",
        "Science Building Entrance #2",
        "Central Library Gate",
        "Student Union Gym Entrance",
        "Cafeteria POS Terminal #4",
        "Dormitory Access Gate B"
    )

    if (showNfcOffDialog) {
        NfcDisabledPopupDialog(onDismiss = { showNfcOffDialog = false })
    }

    if (expandedCardMenu) {
        AlertDialog(
            onDismissRequest = { expandedCardMenu = false },
            title = { Text("Select Card Badge", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (config.linkedCardUid.isNotBlank()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCardUid == config.linkedCardUid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCardUid = config.linkedCardUid
                                    selectedCardNickname = config.linkedCardNickname.ifBlank { "Primary Badge" }
                                    expandedCardMenu = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${config.linkedCardNickname.ifBlank { "Primary Badge" }} (Profile Card)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(text = "UID: ${config.linkedCardUid}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    savedCards.forEach { card ->
                        if (card.uid != config.linkedCardUid) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCardUid == card.uid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCardUid = card.uid
                                        selectedCardNickname = card.nickname
                                        expandedCardMenu = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = card.nickname, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "UID: ${card.uid} • ${card.manufacturer}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (config.linkedCardUid.isBlank() && savedCards.isEmpty()) {
                        Text(
                            text = "No saved cards in your wallet yet. Add a card in Vault or link a UID in User Profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expandedCardMenu = false }) { Text("Close") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Terminal Header Box
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldSuccess.copy(alpha = 0.16f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${config.providerType} Attendance Terminal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Device: ${config.deviceId.ifBlank { "Local Mobile" }} • ${if (config.isConnected) "Online Sync" else "Local Queue Mode"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (nfcState.isEnabled) EmeraldSuccess else CrimsonAlert,
                            modifier = Modifier.clickable {
                                if (nfcState.isSupported && !nfcState.isEnabled) showNfcOffDialog = true
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color.White, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        !nfcState.isSupported -> "NO NFC"
                                        nfcState.isEnabled -> "NFC ON"
                                        else -> "NFC OFF"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedLocationMenu,
                        onExpandedChange = { expandedLocationMenu = !expandedLocationMenu }
                    ) {
                        OutlinedTextField(
                            value = selectedLocation,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Terminal Location") },
                            leadingIcon = { Icon(Icons.Default.DoorSliding, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLocationMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLocationMenu,
                            onDismissRequest = { expandedLocationMenu = false }
                        ) {
                            locations.forEach { loc ->
                                DropdownMenuItem(
                                    text = { Text(loc) },
                                    onClick = {
                                        selectedLocation = loc
                                        expandedLocationMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Card Picker
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Active Card Badge",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select the card credential attached to this attendance log",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedCardMenu = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = selectedCardNickname,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "UID: $selectedCardUid",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            TextButton(onClick = { expandedCardMenu = true }) {
                                Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Punch Mode & Status Selector
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Punch Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("PUNCH_IN", "Punch In", Icons.AutoMirrored.Filled.Login),
                            Triple("PUNCH_OUT", "Punch Out", Icons.AutoMirrored.Filled.Logout),
                            Triple("GATE_ACCESS", "Gate Log", Icons.Default.DoorSliding)
                        ).forEach { (mode, label, icon) ->
                            val isSelected = selectedPunchType == mode
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clickable { selectedPunchType = mode }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "3. Attendance Status Tag",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("PRESENT", "Present", EmeraldSuccess),
                            Triple("LATE", "Late", Color(0xFFF59E0B)),
                            Triple("SICK", "Sick", Color(0xFF06B6D4)),
                            Triple("EXCUSED", "Excused", Color(0xFFA855F7)),
                            Triple("ABSENT", "Absent", CrimsonAlert)
                        ).forEach { (stKey, stLabel, stColor) ->
                            val isSelected = selectedStatus == stKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) stColor else stColor.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, if (isSelected) stColor else stColor.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 42.dp)
                                    .clickable { selectedStatus = stKey }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = stLabel,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else stColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Honest Local Attendance Punch Action Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "4. Record Attendance Punch",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (config.serverUrl.isNotBlank()) {
                            "Logs punch locally and submits to your configured ${config.providerType} server."
                        } else {
                            "Logs punch locally in offline queue. Configure a server in Admin Settings to enable sync."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onPunchWithCard(
                                selectedCardUid,
                                selectedCardNickname,
                                selectedPunchType,
                                selectedLocation,
                                selectedStatus
                            )
                            showSuccessBanner = true
                            lastPunchTimeText = dateFormat.format(Date())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_tap_card_to_machine")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Record ${selectedPunchType.replace('_', ' ')} Now",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedVisibility(visible = showSuccessBanner) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldSuccess.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Attendance Recorded • $lastPunchTimeText",
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "$selectedCardNickname [$selectedCardUid] • $selectedLocation",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Punches List (with status editing wired for Teacher/Admin)
        item {
            Text(
                text = "Recent Attendance Punches",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (records.isEmpty()) {
            item {
                Text(
                    text = "No attendance records yet. Use the button above to record your first entry.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(records.take(6), key = { it.id }) { record ->
                AttendanceRecordCard(
                    record = record,
                    dateFormat = dateFormat,
                    onUpdateStatus = if (canEditStatus) onUpdateRecordStatus else null
                )
            }
        }
    }
}

// ==========================================
// 2. ATTENDANCE DASHBOARD VIEW
// ==========================================
@Composable
private fun AttendanceDashboardView(
    config: AttendanceConfigEntity,
    records: List<AttendanceEntity>,
    stats: AttendanceStats,
    timeRange: String,
    canEditStatus: Boolean,
    onTimeRangeChange: (String) -> Unit,
    onPunch: (String, String, String) -> Unit,
    onUpdateRecordStatus: (Long, String) -> Unit,
    dateFormat: SimpleDateFormat
) {
    var quickStatus by remember { mutableStateOf("PRESENT") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Rate Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Overall Attendance Rate",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f%%", stats.attendancePercentage),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    stats.totalDays == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                                    stats.attendancePercentage >= 80f -> EmeraldSuccess
                                    else -> GoldChip
                                }
                            )
                        }

                        val badgeColor = when {
                            stats.totalDays == 0 -> MaterialTheme.colorScheme.outline
                            stats.attendancePercentage >= 80f -> EmeraldSuccess
                            else -> GoldChip
                        }
                        Surface(
                            shape = CircleShape,
                            color = badgeColor.copy(alpha = 0.16f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when {
                                        stats.totalDays == 0 -> "N/A"
                                        stats.attendancePercentage >= 80f -> "GOOD"
                                        else -> "RISK"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatChip("Present", "${stats.presentCount}", EmeraldSuccess)
                        StatChip("Late", "${stats.lateCount}", GoldChip)
                        StatChip("Sick", "${stats.sickCount}", Color(0xFF06B6D4))
                        StatChip("Excused", "${stats.excusedCount}", Color(0xFFA855F7))
                        StatChip("Absent", "${stats.absentCount}", CrimsonAlert)
                    }
                }
            }
        }

        // Quick Punch Box
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Punch Action",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${config.providerType} Terminal: ${config.deviceId.ifBlank { "Local" }} • ${config.campusId.ifBlank { "Default Campus" }}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("PRESENT", "Present", EmeraldSuccess),
                            Triple("LATE", "Late", GoldChip),
                            Triple("SICK", "Sick", Color(0xFF06B6D4)),
                            Triple("EXCUSED", "Excused", Color(0xFFA855F7)),
                            Triple("ABSENT", "Absent", CrimsonAlert)
                        ).forEach { (stKey, stLabel, stColor) ->
                            val isSel = quickStatus == stKey
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) stColor else stColor.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .clickable { quickStatus = stKey }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = stLabel,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else stColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onPunch("PUNCH_IN", "Main Gate #1 Turnstile", quickStatus) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_punch_in")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Punch In", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onPunch("PUNCH_OUT", "Main Gate #1 Turnstile", quickStatus) },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_punch_out")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Punch Out", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Functional Time Range Filter Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "TODAY" to "Today",
                    "WEEK" to "7 Days",
                    "MONTH" to "30 Days",
                    "ALL" to "All"
                ).forEach { (key, label) ->
                    val isSelected = timeRange == key
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clickable { onTimeRangeChange(key) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Records Header
        item {
            Text(
                text = "Attendance Records (${records.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (records.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Attendance Records in This Range",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap 'Punch In' or adjust the date filter above.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(records, key = { it.id }) { record ->
                AttendanceRecordCard(
                    record = record,
                    dateFormat = dateFormat,
                    onUpdateStatus = if (canEditStatus) onUpdateRecordStatus else null
                )
            }
        }
    }
}

// ==========================================
// 3. ATTENDANCE ANALYTICS & CHARTS VIEW
// ==========================================
@Composable
private fun AttendanceAnalyticsView(
    stats: AttendanceStats,
    records: List<AttendanceEntity>
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Attendance Donut Chart Card (fixed math — no pending double-counting)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Attendance Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Breakdown by recorded status (${stats.syncPendingCount} pending sync)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        val total = stats.totalDays.toFloat()
                        val slices = listOf(
                            stats.presentCount.toFloat() to EmeraldSuccess,
                            stats.lateCount.toFloat() to GoldChip,
                            stats.sickCount.toFloat() to Color(0xFF06B6D4),
                            stats.excusedCount.toFloat() to Color(0xFFA855F7),
                            stats.absentCount.toFloat() to CrimsonAlert
                        )

                        val strokeWidth = 28.dp

                        Canvas(modifier = Modifier.size(170.dp)) {
                            // Base track ring
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth.toPx())
                            )

                            if (total > 0f) {
                                var startAngle = -90f
                                slices.forEach { (count, color) ->
                                    if (count > 0f) {
                                        val sweep = (count / total) * 360f
                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
                                        )
                                        startAngle += sweep
                                    }
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.US, "%.1f%%", stats.attendancePercentage),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${stats.totalDays} Records",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ChartLegendBadge("Present", "${stats.presentCount}", EmeraldSuccess)
                        ChartLegendBadge("Late", "${stats.lateCount}", GoldChip)
                        ChartLegendBadge("Sick", "${stats.sickCount}", Color(0xFF06B6D4))
                        ChartLegendBadge("Excused", "${stats.excusedCount}", Color(0xFFA855F7))
                        ChartLegendBadge("Absent", "${stats.absentCount}", CrimsonAlert)
                    }
                }
            }
        }

        // Last 7 Days Punch Volume Bar Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Last 7 Days Punch Volume",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Daily punches recorded over the past 7 days",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val countsByDay = remember(records) {
                        val counts = IntArray(7)
                        val cutoff = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                            add(Calendar.DAY_OF_YEAR, -6)
                        }.timeInMillis

                        val cal = Calendar.getInstance()
                        records.filter { it.timestamp >= cutoff }.forEach { r ->
                            cal.timeInMillis = r.timestamp
                            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                            val idx = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                            if (idx in 0..6) counts[idx]++
                        }
                        counts
                    }

                    val maxCount = (countsByDay.maxOrNull() ?: 1).coerceAtLeast(1)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weekDays.forEachIndexed { idx, day ->
                            val count = countsByDay[idx]
                            val barHeightFraction = if (count == 0) 0.02f else (count.toFloat() / maxCount.toFloat()).coerceIn(0.12f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight(barHeightFraction * 0.72f)
                                        .background(
                                            color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendBadge(label: String, count: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(text = count, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==========================================
// 4. USER PROFILE SETTINGS VIEW
// ==========================================
@Composable
private fun UserProfileSettingsView(
    config: AttendanceConfigEntity,
    roleState: RoleAccessState,
    savedCards: List<CardEntity>,
    onUpdateProfile: (String, String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var studentName by remember(config) { mutableStateOf(config.activeStudentName) }
    var studentId by remember(config) { mutableStateOf(config.activeStudentId) }
    var department by remember(config) { mutableStateOf(config.department) }
    var email by remember(config) { mutableStateOf(config.email) }
    var cardUid by remember(config) { mutableStateOf(config.linkedCardUid) }
    var cardNickname by remember(config) { mutableStateOf(config.linkedCardNickname) }
    val userRole = roleState.activeRole
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Smart ID Pass Header
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = studentName.ifBlank { "Student Profile" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$userRole • ID: ${studentId.ifBlank { "Not Set" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "PASS PROFILE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (department.isNotBlank()) {
                        Text(text = "Department: $department", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    if (email.isNotBlank()) {
                        Text(text = "Email: $email", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text(
                        text = if (cardUid.isNotBlank()) "Linked Card: ${cardNickname.ifBlank { "Badge" }} [$cardUid]" else "No NFC card linked yet",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Profile Form
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Profile & Linked NFC Badge",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it; validationError = null },
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it; validationError = null },
                    label = { Text("Student / Staff ID *") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department / Major") },
                    leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; validationError = null },
                    label = { Text("University Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cardNickname,
                    onValueChange = { cardNickname = it },
                    label = { Text("Linked Card Nickname") },
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cardUid,
                    onValueChange = { cardUid = it },
                    label = { Text("Linked NFC Card UID") },
                    leadingIcon = { Icon(Icons.Default.Nfc, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        when {
                            studentName.isBlank() -> validationError = "Full Name is required."
                            studentId.isBlank() -> validationError = "Student / Staff ID is required."
                            email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                                validationError = "Please enter a valid email address."
                            else -> {
                                validationError = null
                                onUpdateProfile(studentName, studentId, department, email, cardUid, cardNickname)
                                Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_save_profile")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Profile Settings", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Link Card from Saved Wallet Cards
        if (savedCards.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Link from Saved Wallet Cards",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select any stored NFC card to link as your attendance badge",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    savedCards.forEach { card ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = card.nickname, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = "UID: ${card.uid} • ${card.manufacturer}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(
                                onClick = {
                                    cardUid = card.uid
                                    cardNickname = card.nickname
                                    onUpdateProfile(
                                        studentName,
                                        studentId,
                                        department,
                                        email,
                                        card.uid,
                                        card.nickname
                                    )
                                    Toast.makeText(context, "Linked '${card.nickname}'!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Link Card", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// 5. SERVER CONFIG ADMIN VIEW
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerConfigAdminView(
    config: AttendanceConfigEntity,
    roleState: RoleAccessState,
    isTesting: Boolean,
    onSaveConfig: (AttendanceConfigEntity) -> Unit,
    onTestConnection: (AttendanceConfigEntity) -> Unit,
    onSetTeacherPasscode: (String, (Result<Unit>) -> Unit) -> Unit,
    onRotateAdminPasscode: (String, String, (Result<Unit>) -> Unit) -> Unit,
    onUnlinkServer: () -> Unit
) {
    val context = LocalContext.current
    var serverUrl by remember(config) { mutableStateOf(config.serverUrl) }
    var port by remember(config) { mutableStateOf(config.port.toString()) }
    var useHttps by remember(config) { mutableStateOf(config.useHttps) }
    var authType by remember(config) { mutableStateOf(config.authType) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var orgId by remember(config) { mutableStateOf(config.organizationId) }
    var campusId by remember(config) { mutableStateOf(config.campusId) }
    var deviceId by remember(config) { mutableStateOf(config.deviceId) }
    var providerType by remember(config) { mutableStateOf(config.providerType) }

    var expandedProvider by remember { mutableStateOf(false) }
    var expandedAuth by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    fun validateAndBuildConfig(): AttendanceConfigEntity? {
        val trimmedUrl = serverUrl.trim()
        val portInt = port.trim().toIntOrNull()
        if (trimmedUrl.isNotBlank() && trimmedUrl.contains(" ")) {
            formError = "Server URL cannot contain spaces."
            return null
        }
        if (portInt == null || portInt !in 1..65535) {
            formError = "Port must be a valid number between 1 and 65535."
            return null
        }
        formError = null
        return config.copy(
            serverUrl = trimmedUrl,
            port = portInt,
            useHttps = useHttps,
            authType = authType,
            apiKey = apiKey.trim(),
            organizationId = orgId.trim(),
            campusId = campusId.trim(),
            deviceId = deviceId.trim(),
            providerType = providerType
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Attendance Server Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedProvider,
                    onExpandedChange = { expandedProvider = !expandedProvider }
                ) {
                    OutlinedTextField(
                        value = providerType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Attendance Hardware Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProvider) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedProvider,
                        onDismissRequest = { expandedProvider = false }
                    ) {
                        listOf(
                            "JKTECO" to "JKTECO (iClock Push API)",
                            "SUPREMA" to "Suprema (BioStar 2 REST API)",
                            "HID" to "HID Global (Origo Cloud API)",
                            "GALLAGHER" to "Gallagher (Command Centre API)",
                            "CUSTOM" to "Custom University REST API"
                        ).forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    providerType = code
                                    expandedProvider = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it; formError = null },
                    label = { Text("Server Host / Domain") },
                    placeholder = { Text("attendance.university.edu") },
                    leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { input ->
                            if (input.length <= 5 && input.all { it.isDigit() }) {
                                port = input
                                formError = null
                            }
                        },
                        label = { Text("Port (1–65535)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Switch(
                            checked = useHttps,
                            onCheckedChange = { useHttps = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (useHttps) "HTTPS" else "HTTP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                if (!useHttps) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = GoldChip, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Android blocks cleartext HTTP by default. Use HTTPS for production servers.",
                            fontSize = 11.sp,
                            color = GoldChip
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedAuth,
                    onExpandedChange = { expandedAuth = !expandedAuth }
                ) {
                    OutlinedTextField(
                        value = authType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Authentication Scheme") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAuth) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAuth,
                        onDismissRequest = { expandedAuth = false }
                    ) {
                        listOf("API_KEY", "JWT", "OAUTH2", "SESSION").forEach { scheme ->
                            DropdownMenuItem(
                                text = { Text(scheme) },
                                onClick = {
                                    authType = scheme
                                    expandedAuth = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key / Bearer Token (Keystore Encrypted)") },
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle API Key Visibility"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = orgId,
                    onValueChange = { orgId = it },
                    label = { Text("Organization ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = campusId,
                    onValueChange = { campusId = it },
                    label = { Text("Campus ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text("Device Terminal ID") },
                    leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (formError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val newCfg = validateAndBuildConfig() ?: return@OutlinedButton
                            if (newCfg.serverUrl.isBlank()) {
                                formError = "Enter a Server Host / Domain before testing connection."
                                return@OutlinedButton
                            }
                            onTestConnection(newCfg)
                        },
                        enabled = !isTesting,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_test_connection")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Test Connection")
                        }
                    }

                    Button(
                        onClick = {
                            val newCfg = validateAndBuildConfig() ?: return@Button
                            onSaveConfig(newCfg)
                            Toast.makeText(context, "Server configuration saved securely!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_save_server_config")
                    ) {
                        Text("Save Config")
                    }
                }
            }
        }

        // Admin-Only Role & Staff Passcode Management Card
        var teacherPassInput by remember { mutableStateOf("") }
        var currentAdminPassInput by remember { mutableStateOf("") }
        var newAdminPassInput by remember { mutableStateOf("") }
        var rbacStatusMsg by remember { mutableStateOf<String?>(null) }
        var showUnlinkConfirm by remember { mutableStateOf(false) }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = IndigoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Role Access & Passcode Controls (Admin)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Manage local PBKDF2-hashed staff passcodes for offline Teacher/Admin authentication. When linked to your university server, roles can also be issued via server login.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = teacherPassInput,
                    onValueChange = { teacherPassInput = it; rbacStatusMsg = null },
                    label = {
                        Text(
                            if (roleState.hasTeacherPasscode) "New Teacher Passcode (${RoleAccessManager.MIN_TEACHER_PASSCODE_LENGTH}+ chars)"
                            else "Set Teacher Passcode (${RoleAccessManager.MIN_TEACHER_PASSCODE_LENGTH}+ chars)"
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        onSetTeacherPasscode(teacherPassInput) { res ->
                            if (res.isSuccess) {
                                teacherPassInput = ""
                                rbacStatusMsg = "Teacher passcode updated securely."
                                Toast.makeText(context, "Teacher passcode saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                rbacStatusMsg = res.exceptionOrNull()?.localizedMessage ?: "Failed to update Teacher passcode."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (roleState.hasTeacherPasscode) "Update Teacher Passcode" else "Set Teacher Passcode")
                }

                if (roleState.hasAdminPasscode) {
                    OutlinedTextField(
                        value = currentAdminPassInput,
                        onValueChange = { currentAdminPassInput = it; rbacStatusMsg = null },
                        label = { Text("Current Administrator Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = newAdminPassInput,
                    onValueChange = { newAdminPassInput = it; rbacStatusMsg = null },
                    label = { Text("New Administrator Passcode (${RoleAccessManager.MIN_ADMIN_PASSCODE_LENGTH}+ chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onRotateAdminPasscode(currentAdminPassInput, newAdminPassInput) { res ->
                            if (res.isSuccess) {
                                currentAdminPassInput = ""
                                newAdminPassInput = ""
                                rbacStatusMsg = "Administrator passcode rotated securely."
                                Toast.makeText(context, "Admin passcode updated!", Toast.LENGTH_SHORT).show()
                            } else {
                                rbacStatusMsg = res.exceptionOrNull()?.localizedMessage ?: "Failed to update Admin passcode."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Administrator Passcode")
                }

                if (rbacStatusMsg != null) {
                    Text(
                        text = rbacStatusMsg!!,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (config.serverUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = { showUnlinkConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unlink Server & Clear Session Token")
                    }
                }
            }
        }

        if (showUnlinkConfirm) {
            DeleteConfirmationDialog(
                title = "Unlink Campus Server?",
                message = "Clear the stored server token and return to Student mode?",
                onConfirm = {
                    showUnlinkConfirm = false
                    onUnlinkServer()
                },
                onDismiss = { showUnlinkConfirm = false }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// 6. AUDIT LOGS VIEW
// ==========================================
@Composable
private fun AuditLogsView(
    logs: List<AttendanceLogEntity>,
    canClearLogs: Boolean,
    onClearLogs: () -> Unit,
    dateFormat: SimpleDateFormat
) {
    var showClearLogsDialog by remember { mutableStateOf(false) }

    if (showClearLogsDialog) {
        DeleteConfirmationDialog(
            title = "Clear Audit Logs?",
            message = "Are you sure you want to permanently erase all ${logs.size} attendance audit logs? This cannot be undone.",
            onConfirm = {
                showClearLogsDialog = false
                onClearLogs()
            },
            onDismiss = { showClearLogsDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "System & Audit Logs (${logs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (canClearLogs && logs.isNotEmpty()) {
                TextButton(onClick = { showClearLogsDialog = true }) {
                    Text("Clear Logs", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No log records captured.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "[${log.logType}] ${log.provider}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (log.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = dateFormat.format(Date(log.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = log.message, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (log.details.isNotBlank()) {
                                Text(
                                    text = log.details,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SHARED HELPER COMPONENTS
// ==========================================
@Composable
private fun StatChip(
    label: String,
    value: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
private fun AttendanceRecordCard(
    record: AttendanceEntity,
    dateFormat: SimpleDateFormat,
    onUpdateStatus: ((Long, String) -> Unit)? = null
) {
    var showStatusDropdown by remember { mutableStateOf(false) }

    val statusColor = when (record.status) {
        "PRESENT", "ON_TIME" -> EmeraldSuccess
        "LATE" -> GoldChip
        "SICK" -> Color(0xFF06B6D4)
        "EXCUSED", "LEAVE" -> Color(0xFFA855F7)
        else -> CrimsonAlert
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val punchColor = if (record.usageType == "PUNCH_IN") EmeraldSuccess else IndigoPrimary
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = punchColor.copy(alpha = 0.14f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (record.usageType == "PUNCH_IN") Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = punchColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${record.usageType.replace('_', ' ')} • ${record.studentName.ifBlank { "Student" }}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Box {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.16f),
                            modifier = Modifier.clickable(enabled = onUpdateStatus != null) {
                                showStatusDropdown = true
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = record.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor
                                )
                                if (onUpdateStatus != null) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Status",
                                        tint = statusColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        if (onUpdateStatus != null) {
                            DropdownMenu(
                                expanded = showStatusDropdown,
                                onDismissRequest = { showStatusDropdown = false }
                            ) {
                                listOf(
                                    "PRESENT" to "Present (On Time)",
                                    "LATE" to "Late",
                                    "SICK" to "Sick Leave",
                                    "EXCUSED" to "Excused / Leave",
                                    "ABSENT" to "Absent"
                                ).forEach { (stKey, stLabel) ->
                                    DropdownMenuItem(
                                        text = { Text(stLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            onUpdateStatus(record.id, stKey)
                                            showStatusDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${record.location} (${record.deviceId})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (record.syncStatus != "SYNCED") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Pending Sync",
                                tint = GoldChip,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = record.syncStatus,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldChip
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Card: ${record.cardNickname} [${record.cardUid}] • ${dateFormat.format(Date(record.timestamp))}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }
        }
    }
}
