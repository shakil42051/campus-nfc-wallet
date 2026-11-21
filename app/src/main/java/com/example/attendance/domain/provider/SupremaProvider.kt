package com.example.attendance.domain.provider

class SupremaProvider : BaseHttpAttendanceProvider(
    providerName = "SUPREMA",
    healthPath = "/api/v2/health",
    punchPath = "/api/v2/events/punch",
    historyPathPrefix = "/api/v2/events/history",
    userAgent = "CampusNfcWallet-Suprema-Client/1.0"
)
