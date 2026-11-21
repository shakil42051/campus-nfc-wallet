package com.example.backup.migration

import com.example.backup.model.BackupException

object BackupMigrationManager {

    const val CURRENT_SCHEMA_VERSION = 2

    fun checkSchemaCompatibility(schemaVersion: Int) {
        if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw BackupException.UnsupportedVersion(schemaVersion)
        }
    }

    fun migrateIfNeeded(payloadJson: String, fromVersion: Int): String {
        checkSchemaCompatibility(fromVersion)
        // v1 and v2 share the same decrypted payload schema (v2 adds GCM AAD header authentication & higher PBKDF2 rounds).
        return payloadJson
    }
}
