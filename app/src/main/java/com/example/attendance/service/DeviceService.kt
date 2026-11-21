package com.example.attendance.service

import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.attendance.domain.provider.AttendanceProvider
import com.example.attendance.domain.provider.JkTecoProvider
import com.example.attendance.domain.provider.SupremaProvider
import com.example.attendance.domain.provider.HidProvider
import com.example.attendance.domain.provider.GallagherProvider
import com.example.attendance.domain.provider.CustomUniversityProvider

class DeviceService {

    private val providers: Map<String, AttendanceProvider> = mapOf(
        "JKTECO" to JkTecoProvider(),
        "SUPREMA" to SupremaProvider(),
        "HID" to HidProvider(),
        "GALLAGHER" to GallagherProvider(),
        "CUSTOM" to CustomUniversityProvider()
    )

    fun getProvider(type: String): AttendanceProvider {
        return providers[type.uppercase()] ?: providers["JKTECO"]!!
    }

    suspend fun checkDeviceHealth(config: AttendanceConfigEntity): String {
        val provider = getProvider(config.providerType)
        return provider.getDeviceStatus(config)
    }
}
