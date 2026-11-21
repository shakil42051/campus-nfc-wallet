package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class NdefRecordInfo(
    val tnf: String,
    val type: String,
    val payloadHex: String,
    val payloadText: String
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("tnf", tnf)
            put("type", type)
            put("payloadHex", payloadHex)
            put("payloadText", payloadText)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): NdefRecordInfo {
            return NdefRecordInfo(
                tnf = json.optString("tnf", "Unknown"),
                type = json.optString("type", ""),
                payloadHex = json.optString("payloadHex", ""),
                payloadText = json.optString("payloadText", "")
            )
        }
    }
}

data class SectorMemoryInfo(
    val index: Int,
    val title: String,
    val isProtected: Boolean,
    val dataHex: String,
    val dataAscii: String,
    val statusMessage: String
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("index", index)
            put("title", title)
            put("isProtected", isProtected)
            put("dataHex", dataHex)
            put("dataAscii", dataAscii)
            put("statusMessage", statusMessage)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): SectorMemoryInfo {
            return SectorMemoryInfo(
                index = json.optInt("index", 0),
                title = json.optString("title", ""),
                isProtected = json.optBoolean("isProtected", true),
                dataHex = json.optString("dataHex", ""),
                dataAscii = json.optString("dataAscii", ""),
                statusMessage = json.optString("statusMessage", "")
            )
        }
    }
}

data class SecurityAnalysis(
    val encryptionType: String,
    val supportsCloning: Boolean,
    val cloneRiskLevel: String, // "High", "Low", "None (Secure Hardware)"
    val explanation: String,
    val hardwareSecurityFeatures: List<String>
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("encryptionType", encryptionType)
            put("supportsCloning", supportsCloning)
            put("cloneRiskLevel", cloneRiskLevel)
            put("explanation", explanation)
            put("hardwareSecurityFeatures", JSONArray(hardwareSecurityFeatures))
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): SecurityAnalysis {
            val featuresList = mutableListOf<String>()
            json.optJSONArray("hardwareSecurityFeatures")?.let { arr ->
                for (i in 0 until arr.length()) {
                    featuresList.add(arr.optString(i))
                }
            }
            return SecurityAnalysis(
                encryptionType = json.optString("encryptionType", "Unknown"),
                supportsCloning = json.optBoolean("supportsCloning", false),
                cloneRiskLevel = json.optString("cloneRiskLevel", "None (Secure Hardware)"),
                explanation = json.optString("explanation", ""),
                hardwareSecurityFeatures = featuresList
            )
        }
    }
}

data class NfcScanResult(
    val uid: String,
    val uidHex: String,
    val uidAscii: String,
    val uidBinary: String,
    val uidLengthBytes: Int,
    val manufacturer: String,
    val primaryTechnology: String,
    val cardStandard: String,
    val technologies: List<String>,
    val atqa: String,
    val sak: String,
    val ats: String,
    val historicalBytes: String,
    val memorySizeBytes: String,
    val blockCount: String,
    val pageCount: String,
    val isNdefSupported: Boolean,
    val isNdefWritable: Boolean,
    val isNdefReadOnly: Boolean,
    val ndefMaxSizeBytes: Int,
    val ndefRecords: List<NdefRecordInfo>,
    val memorySectors: List<SectorMemoryInfo>,
    val securityAnalysis: SecurityAnalysis,
    val rawDataDumpHex: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJsonString(): String {
        val json = JSONObject().apply {
            put("uid", uid)
            put("uidHex", uidHex)
            put("uidAscii", uidAscii)
            put("uidBinary", uidBinary)
            put("uidLengthBytes", uidLengthBytes)
            put("manufacturer", manufacturer)
            put("primaryTechnology", primaryTechnology)
            put("cardStandard", cardStandard)
            put("technologies", JSONArray(technologies))
            put("atqa", atqa)
            put("sak", sak)
            put("ats", ats)
            put("historicalBytes", historicalBytes)
            put("memorySizeBytes", memorySizeBytes)
            put("blockCount", blockCount)
            put("pageCount", pageCount)
            put("isNdefSupported", isNdefSupported)
            put("isNdefWritable", isNdefWritable)
            put("isNdefReadOnly", isNdefReadOnly)
            put("ndefMaxSizeBytes", ndefMaxSizeBytes)
            put("timestamp", timestamp)
            put("rawDataDumpHex", rawDataDumpHex)

            val ndefArr = JSONArray()
            ndefRecords.forEach { ndefArr.put(it.toJsonObject()) }
            put("ndefRecords", ndefArr)

            val sectorArr = JSONArray()
            memorySectors.forEach { sectorArr.put(it.toJsonObject()) }
            put("memorySectors", sectorArr)

            put("securityAnalysis", securityAnalysis.toJsonObject())
        }
        return json.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): NfcScanResult? {
            return try {
                val json = JSONObject(jsonStr)
                val techList = mutableListOf<String>()
                json.optJSONArray("technologies")?.let { arr ->
                    for (i in 0 until arr.length()) techList.add(arr.optString(i))
                }

                val ndefList = mutableListOf<NdefRecordInfo>()
                json.optJSONArray("ndefRecords")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        ndefList.add(NdefRecordInfo.fromJsonObject(arr.getJSONObject(i)))
                    }
                }

                val sectorList = mutableListOf<SectorMemoryInfo>()
                json.optJSONArray("memorySectors")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        sectorList.add(SectorMemoryInfo.fromJsonObject(arr.getJSONObject(i)))
                    }
                }

                val secObject = json.optJSONObject("securityAnalysis")?.let {
                    SecurityAnalysis.fromJsonObject(it)
                } ?: SecurityAnalysis("Hardware Encrypted", false, "None (Secure Hardware)", "Card memory protected by hardware crypto.", emptyList())

                NfcScanResult(
                    uid = json.optString("uid", ""),
                    uidHex = json.optString("uidHex", ""),
                    uidAscii = json.optString("uidAscii", ""),
                    uidBinary = json.optString("uidBinary", ""),
                    uidLengthBytes = json.optInt("uidLengthBytes", 0),
                    manufacturer = json.optString("manufacturer", "Unknown"),
                    primaryTechnology = json.optString("primaryTechnology", "NFC Tag"),
                    cardStandard = json.optString("cardStandard", "ISO/IEC 14443"),
                    technologies = techList,
                    atqa = json.optString("atqa", "N/A"),
                    sak = json.optString("sak", "N/A"),
                    ats = json.optString("ats", "N/A"),
                    historicalBytes = json.optString("historicalBytes", "N/A"),
                    memorySizeBytes = json.optString("memorySizeBytes", "Unknown"),
                    blockCount = json.optString("blockCount", "N/A"),
                    pageCount = json.optString("pageCount", "N/A"),
                    isNdefSupported = json.optBoolean("isNdefSupported", false),
                    isNdefWritable = json.optBoolean("isNdefWritable", false),
                    isNdefReadOnly = json.optBoolean("isNdefReadOnly", false),
                    ndefMaxSizeBytes = json.optInt("ndefMaxSizeBytes", 0),
                    ndefRecords = ndefList,
                    memorySectors = sectorList,
                    securityAnalysis = secObject,
                    rawDataDumpHex = json.optString("rawDataDumpHex", ""),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis())
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
