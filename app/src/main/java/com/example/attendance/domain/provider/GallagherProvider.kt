package com.example.attendance.domain.provider

class GallagherProvider : BaseHttpAttendanceProvider(
    providerName = "GALLAGHER",
    healthPath = "/api/health",
    punchPath = "/api/events/attendance",
    historyPathPrefix = "/api/events/attendance",
    userAgent = "CampusNfcWallet-Gallagher-Client/1.0"
)
