package com.example.backup.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.backup.model.BackupHistoryItem
import com.example.backup.model.BackupOperationStatus
import com.example.backup.model.BackupOperationType
import org.json.JSONArray
import org.json.JSONObject

class BackupHistoryRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("card_vault_backup_history_prefs", Context.MODE_PRIVATE)

    fun getHistory(): List<BackupHistoryItem> {
        val jsonStr = prefs.getString("history_list", "[]") ?: "[]"
        val list = mutableListOf<BackupHistoryItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BackupHistoryItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        fileName = obj.optString("fileName", "backup.cardvaultbackup"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
                        cardsCount = obj.optInt("cardsCount", 0),
                        transactionsCount = obj.optInt("transactionsCount", 0),
                        attendanceCount = obj.optInt("attendanceCount", 0),
                        operationType = runCatching {
                            BackupOperationType.valueOf(obj.optString("operationType", "CREATE_BACKUP"))
                        }.getOrDefault(BackupOperationType.CREATE_BACKUP),
                        status = runCatching {
                            BackupOperationStatus.valueOf(obj.optString("status", "SUCCESS"))
                        }.getOrDefault(BackupOperationStatus.SUCCESS),
                        errorMessage = if (obj.isNull("errorMessage")) null else obj.optString("errorMessage")
                    )
                )
            }
        } catch (e: Exception) {
            // Return parsed items safely without dumping stack trace to stderr
        }
        return list
    }

    fun addHistoryItem(item: BackupHistoryItem) {
        val currentList = getHistory().toMutableList()
        currentList.add(0, item)

        val trimmed = if (currentList.size > 30) currentList.take(30) else currentList

        val arr = JSONArray()
        trimmed.forEach { h ->
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("fileName", h.fileName)
            obj.put("timestamp", h.timestamp)
            obj.put("fileSizeBytes", h.fileSizeBytes)
            obj.put("cardsCount", h.cardsCount)
            obj.put("transactionsCount", h.transactionsCount)
            obj.put("attendanceCount", h.attendanceCount)
            obj.put("operationType", h.operationType.name)
            obj.put("status", h.status.name)
            obj.put("errorMessage", h.errorMessage ?: JSONObject.NULL)
            arr.put(obj)
        }

        prefs.edit().putString("history_list", arr.toString()).apply()
    }

    fun getLastSuccessfulBackupTimestamp(): Long? {
        return getHistory().firstOrNull {
            (it.operationType == BackupOperationType.CREATE_BACKUP || it.operationType == BackupOperationType.AUTO_BACKUP) &&
                it.status == BackupOperationStatus.SUCCESS
        }?.timestamp
    }

    fun getLastSuccessfulRestoreTimestamp(): Long? {
        return getHistory().firstOrNull {
            it.operationType == BackupOperationType.RESTORE && it.status == BackupOperationStatus.SUCCESS
        }?.timestamp
    }
}
