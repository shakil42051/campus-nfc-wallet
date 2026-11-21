package com.example.attendance.domain.provider

class HidProvider : BaseHttpAttendanceProvider(
    providerName = "HID",
    healthPath = "/api/v1/health",
    punchPath = "/api/v1/access/events",
    historyPathPrefix = "/api/v1/access/history",
    userAgent = "CampusNfcWallet-HID-Client/1.0"
)
