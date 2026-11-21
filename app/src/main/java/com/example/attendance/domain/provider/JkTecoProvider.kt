package com.example.attendance.domain.provider

class JkTecoProvider : BaseHttpAttendanceProvider(
    providerName = "JKTECO",
    healthPath = "/iclock/cdata?options=all",
    punchPath = "/iclock/cdata?table=ATTLOG",
    historyPathPrefix = "/api/v1/attendance/history",
    userAgent = "CampusNfcWallet-JKTECO-Client/1.0"
)
