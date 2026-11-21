package com.example.attendance.domain.provider

class CustomUniversityProvider : BaseHttpAttendanceProvider(
    providerName = "CUSTOM",
    healthPath = "/api/attendance/health",
    punchPath = "/api/attendance/punch",
    historyPathPrefix = "/api/attendance/history",
    userAgent = "CampusNfcWallet-Custom-Client/1.0"
)
