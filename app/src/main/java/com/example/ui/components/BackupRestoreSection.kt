package com.example.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.backup.model.BackupOperationStatus
import com.example.backup.model.BackupOperationType
import com.example.backup.model.RestoreMode
import com.example.backup.model.RestorePreview
import com.example.backup.model.RestoreResult
import com.example.backup.service.PreparedBackupArtifact
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.BackupOverviewState
import com.example.utils.Exporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MIN_BACKUP_PASSWORD_LENGTH = 8

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
    }
}

private fun copySensitiveTextToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clip = ClipData.newPlainText(label, text)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    cm.setPrimaryClip(clip)
}

private fun readClipboardText(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}

@Composable
fun BackupRestoreSection(
    backupOverview: BackupOverviewState,
    onRefreshOverview: () -> Unit,
    onBeforeExternalAction: () -> Unit,
    onRequestAuth: (title: String, description: String, onAuthorized: () -> Unit) -> Unit,
    onCreateEncryptedBackup: (password: String, fileName: String, callback: (Result<PreparedBackupArtifact>) -> Unit) -> Unit,
    onRecordExternalBackupCompleted: (artifact: PreparedBackupArtifact, onDone: () -> Unit) -> Unit,
    onParseAndDecryptBackup: (fileBytes: ByteArray, password: String, fileSizeBytes: Long, callback: (Result<RestorePreview>) -> Unit) -> Unit,
    onExecuteAtomicRestore: (preview: RestorePreview, mode: RestoreMode, selectedCategories: Set<String>, fileName: String, callback: (Result<RestoreResult>) -> Unit) -> Unit,
    onReadBytesFromUriAsync: (context: Context, uri: Uri, callback: (Result<ByteArray>) -> Unit) -> Unit,
    onWriteBytesToUriAsync: (context: Context, uri: Uri, bytes: ByteArray, callback: (Result<Unit>) -> Unit) -> Unit,
    buildDiagnosticsString: (exception: Exception) -> String,
    onCreateInternalSnapshot: (password: String, label: String, callback: (Result<ByteArray>) -> Unit) -> Unit,
    onDeleteInternalSnapshotAsync: (fileName: String, callback: (Boolean) -> Unit) -> Unit,
    onReadInternalSnapshotAsync: (fileName: String, callback: (ByteArray?) -> Unit) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        onRefreshOverview()
    }

    var pendingBackupArtifact by remember { mutableStateOf<PreparedBackupArtifact?>(null) }
    var pendingRestoreFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingRestorePreview by remember { mutableStateOf<RestorePreview?>(null) }
    var pendingRestoreFileName by remember { mutableStateOf("backup.cardvaultbackup") }
    var pendingDeleteSnapshot by remember { mutableStateOf<File?>(null) }

    var showUnifiedBackupDialog by remember { mutableStateOf(false) }
    var showUnifiedRestoreDialog by remember { mutableStateOf(false) }
    var showDecryptPasswordDialog by remember { mutableStateOf(false) }
    var showRestorePreviewDialog by remember { mutableStateOf(false) }
    var showBackupSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreSuccessResult by remember { mutableStateOf<RestoreResult?>(null) }
    var showAllSnapshots by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }

    var backupExportDestination by remember { mutableIntStateOf(0) }
    var backupPasswordInput by remember { mutableStateOf("") }
    var backupPasswordConfirmInput by remember { mutableStateOf("") }
    var snapshotLabelInput by remember { mutableStateOf("") }
    var restorePasswordInput by remember { mutableStateOf("") }
    var clipboardPayloadInput by remember { mutableStateOf("") }
    var restoreModeSelection by remember { mutableStateOf(RestoreMode.REPLACE) }
    var restoreSelectedCategories by remember { mutableStateOf(setOf("cards", "transactions", "attendance")) }
    var passwordVisible by remember { mutableStateOf(false) }
    var decryptErrorMsg by remember { mutableStateOf<String?>(null) }
    var pickerErrorDiagnostics by remember { mutableStateOf<String?>(null) }

    // Share snapshot safely inside cacheDir/exports/ (matching file_paths.xml!)
    fun shareBackupBytes(bytes: ByteArray, fileName: String) {
        try {
            val exportFile = Exporter.writeExportFile(context, fileName, bytes)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "CardVault Encrypted Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onBeforeExternalAction()
            context.startActivity(Intent.createChooser(shareIntent, "Share Encrypted Backup"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // SAF Create Document launcher (async write off Main thread)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val artifact = pendingBackupArtifact
        pendingBackupArtifact = null
        if (uri != null && artifact != null) {
            isWorking = true
            onWriteBytesToUriAsync(context, uri, artifact.bytes) { writeResult ->
                writeResult.onSuccess {
                    onRecordExternalBackupCompleted(artifact) {
                        isWorking = false
                        showBackupSuccessMessage =
                            "Encrypted backup saved successfully!\nSize: ${formatFileSize(artifact.bytes.size.toLong())}\nFormat: AES-256-GCM (PBKDF2 210k)"
                    }
                }.onFailure { err ->
                    isWorking = false
                    pickerErrorDiagnostics = buildDiagnosticsString(err as? Exception ?: Exception(err))
                }
            }
        }
    }

    // SAF Open Document launcher (async read off Main thread)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isWorking = true
            onReadBytesFromUriAsync(context, uri) { readResult ->
                isWorking = false
                readResult.onSuccess { bytes ->
                    if (bytes.isNotEmpty()) {
                        pendingRestoreFileBytes = bytes
                        pendingRestoreFileName = uri.lastPathSegment ?: "backup.cardvaultbackup"
                        restorePasswordInput = ""
                        decryptErrorMsg = null
                        showDecryptPasswordDialog = true
                    } else {
                        Toast.makeText(context, "Selected backup file was empty.", Toast.LENGTH_LONG).show()
                    }
                }.onFailure { err ->
                    pickerErrorDiagnostics = buildDiagnosticsString(err as? Exception ?: Exception(err))
                }
            }
        }
    }

    // Snapshot delete confirmation
    pendingDeleteSnapshot?.let { snapshotFile ->
        DeleteConfirmationDialog(
            title = "Delete Snapshot?",
            message = "Permanently delete local snapshot '${snapshotFile.nameWithoutExtension}'? This cannot be undone.",
            onConfirm = {
                pendingDeleteSnapshot = null
                onDeleteInternalSnapshotAsync(snapshotFile.name) { deleted ->
                    Toast.makeText(
                        context,
                        if (deleted) "Snapshot deleted." else "Could not delete snapshot.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { pendingDeleteSnapshot = null }
        )
    }

    // Unified Backup & Restore Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("unified_backup_restore_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = "Backup",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "BACKUP & RESTORE VAULT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = IndigoPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "AES-256-GCM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Export and restore cards, transactions, and attendance records with AES-256-GCM encryption (PBKDF2 210k).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val lastBackupTime = backupOverview.lastBackupTimestamp
            val lastRestoreTime = backupOverview.lastRestoreTimestamp
            if (lastBackupTime != null || lastRestoreTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val dateFmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (lastBackupTime != null) {
                        Text(
                            text = "Last Backup: ${dateFmt.format(Date(lastBackupTime))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (lastRestoreTime != null) {
                        Text(
                            text = "Last Restore: ${dateFmt.format(Date(lastRestoreTime))}",
                            fontSize = 11.sp,
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onRequestAuth(
                            "Authorize Vault Backup",
                            "Verify your identity before exporting an encrypted backup."
                        ) {
                            backupPasswordInput = ""
                            backupPasswordConfirmInput = ""
                            snapshotLabelInput = ""
                            backupExportDestination = 0
                            showUnifiedBackupDialog = true
                        }
                    },
                    enabled = !isWorking,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_backup_button")
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Backup Vault", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        onRequestAuth(
                            "Authorize Vault Restore",
                            "Verify your identity before restoring vault data."
                        ) {
                            clipboardPayloadInput = ""
                            restorePasswordInput = ""
                            showUnifiedRestoreDialog = true
                        }
                    },
                    enabled = !isWorking,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("restore_backup_button")
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Vault", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            if (isWorking) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing encrypted backup...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Snapshots list rendered in plain Column (no nested LazyColumn inside verticalScroll!)
            val snapshots = backupOverview.snapshots
            if (snapshots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Saved Snapshots (${snapshots.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                    }
                    if (snapshots.size > 4) {
                        TextButton(onClick = { showAllSnapshots = !showAllSnapshots }) {
                            Text(if (showAllSnapshots) "Show Less" else "View All (${snapshots.size})", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val displayedSnapshots = if (showAllSnapshots) snapshots else snapshots.take(4)
                val dateFmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    displayedSnapshots.forEach { file ->
                        val formattedDate = dateFmt.format(Date(file.lastModified()))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.nameWithoutExtension
                                            .replace("Snapshot_", "")
                                            .replace("CardVault_AutoBackup_", "AutoBackup_"),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$formattedDate • ${formatFileSize(file.length())}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            onReadInternalSnapshotAsync(file.name) { bytes ->
                                                if (bytes != null) {
                                                    shareBackupBytes(bytes, file.name)
                                                } else {
                                                    Toast.makeText(context, "Could not read snapshot.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share Snapshot", tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            onRequestAuth(
                                                "Authorize Snapshot Restore",
                                                "Verify your identity before restoring a snapshot."
                                            ) {
                                                onReadInternalSnapshotAsync(file.name) { bytes ->
                                                    if (bytes != null && bytes.isNotEmpty()) {
                                                        pendingRestoreFileBytes = bytes
                                                        pendingRestoreFileName = file.name
                                                        restorePasswordInput = ""
                                                        decryptErrorMsg = null
                                                        showDecryptPasswordDialog = true
                                                    } else {
                                                        Toast.makeText(context, "Snapshot file could not be read.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore Snapshot", tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { pendingDeleteSnapshot = file },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Snapshot", tint = CrimsonAlert, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent Vault Activity (correctly labels AUTO_BACKUP, CREATE_BACKUP, and RESTORE!)
            val historyList = backupOverview.history
            if (historyList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recent Vault Activity", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                    }
                    val latest = historyList.first()
                    val dateFmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
                    val opLabel = when (latest.operationType) {
                        BackupOperationType.CREATE_BACKUP -> "Backup"
                        BackupOperationType.AUTO_BACKUP -> "Auto-Backup"
                        BackupOperationType.RESTORE -> "Restore"
                    }
                    Text(
                        text = "$opLabel (${dateFmt.format(Date(latest.timestamp))})",
                        fontSize = 10.sp,
                        color = if (latest.status == BackupOperationStatus.SUCCESS) EmeraldSuccess else CrimsonAlert,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Create Backup Dialog
    if (showUnifiedBackupDialog) {
        var createError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = {
                if (!isWorking) {
                    showUnifiedBackupDialog = false
                    backupPasswordInput = ""
                    backupPasswordConfirmInput = ""
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = IndigoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Encrypted Backup", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choose destination and set a strong password (min $MIN_BACKUP_PASSWORD_LENGTH characters):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                Triple(0, "Save to Phone / Cloud Storage", "Exports .cardvaultbackup via system picker"),
                                Triple(1, "Save Device Snapshot", "Stored inside private app storage"),
                                Triple(2, "Copy Encrypted JSON to Clipboard", "Marked sensitive on Android 13+")
                            ).forEach { (idx, title, desc) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { backupExportDestination = idx }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = backupExportDestination == idx,
                                        onClick = { backupExportDestination = idx }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    if (backupExportDestination == 1) {
                        OutlinedTextField(
                            value = snapshotLabelInput,
                            onValueChange = { snapshotLabelInput = it },
                            label = { Text("Snapshot Label (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = backupPasswordInput,
                        onValueChange = {
                            backupPasswordInput = it
                            createError = null
                        },
                        label = { Text("Encryption Password (min $MIN_BACKUP_PASSWORD_LENGTH chars)") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_password_input")
                    )

                    OutlinedTextField(
                        value = backupPasswordConfirmInput,
                        onValueChange = {
                            backupPasswordConfirmInput = it
                            createError = null
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_password_confirm_input")
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CrimsonAlert.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CrimsonAlert, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This password cannot be recovered. Keep it in a safe place.",
                                fontSize = 11.sp,
                                color = CrimsonAlert,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (createError != null) {
                        Text(createError!!, color = CrimsonAlert, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isWorking,
                    onClick = {
                        if (backupPasswordInput.length < MIN_BACKUP_PASSWORD_LENGTH) {
                            createError = "Password must be at least $MIN_BACKUP_PASSWORD_LENGTH characters long."
                            return@Button
                        }
                        if (backupPasswordInput != backupPasswordConfirmInput) {
                            createError = "Passwords do not match."
                            return@Button
                        }

                        val pwd = backupPasswordInput
                        isWorking = true

                        if (backupExportDestination == 1) {
                            val label = snapshotLabelInput.ifBlank { "Snapshot" }
                            onCreateInternalSnapshot(pwd, label) { result ->
                                isWorking = false
                                result.onSuccess {
                                    showUnifiedBackupDialog = false
                                    backupPasswordInput = ""
                                    backupPasswordConfirmInput = ""
                                    snapshotLabelInput = ""
                                    Toast.makeText(context, "Encrypted snapshot saved!", Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    createError = "Failed to save snapshot: ${err.localizedMessage}"
                                }
                            }
                        } else {
                            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
                            val defaultFileName = "CardVault_Backup_$timestamp.cardvaultbackup"

                            onCreateEncryptedBackup(pwd, defaultFileName) { result ->
                                isWorking = false
                                result.onSuccess { artifact ->
                                    showUnifiedBackupDialog = false
                                    backupPasswordInput = ""
                                    backupPasswordConfirmInput = ""

                                    if (backupExportDestination == 2) {
                                        val text = String(artifact.bytes, Charsets.UTF_8)
                                        copySensitiveTextToClipboard(context, "Encrypted CardVault Backup", text)
                                        onRecordExternalBackupCompleted(artifact) {
                                            Toast.makeText(context, "Encrypted backup copied to clipboard!", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        pendingBackupArtifact = artifact
                                        onBeforeExternalAction()
                                        createDocumentLauncher.launch(defaultFileName)
                                    }
                                }.onFailure { err ->
                                    createError = "Failed to encrypt backup: ${err.localizedMessage}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_backup_button")
                ) {
                    if (isWorking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(
                            when (backupExportDestination) {
                                1 -> "Save Snapshot"
                                2 -> "Copy to Clipboard"
                                else -> "Encrypt & Save File"
                            }
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isWorking,
                    onClick = {
                        showUnifiedBackupDialog = false
                        backupPasswordInput = ""
                        backupPasswordConfirmInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Options Dialog
    if (showUnifiedRestoreDialog) {
        var pasteError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                if (!isWorking) {
                    showUnifiedRestoreDialog = false
                    clipboardPayloadInput = ""
                    restorePasswordInput = ""
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = IndigoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Vault Backup", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choose your restore source:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showUnifiedRestoreDialog = false
                                try {
                                    onBeforeExternalAction()
                                    openDocumentLauncher.launch(arrayOf("*/*"))
                                } catch (e: Exception) {
                                    pickerErrorDiagnostics = buildDiagnosticsString(e)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Open File from Phone Storage", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Pick any .cardvaultbackup file from local or cloud drives", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore from Clipboard Text", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedTextField(
                                value = clipboardPayloadInput,
                                onValueChange = {
                                    clipboardPayloadInput = it
                                    pasteError = null
                                },
                                label = { Text("Backup Payload JSON") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 80.dp, max = 120.dp),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val clip = readClipboardText(context)
                                        if (!clip.isNullOrBlank()) {
                                            clipboardPayloadInput = clip
                                        }
                                    }) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                                    }
                                }
                            )

                            OutlinedTextField(
                                value = restorePasswordInput,
                                onValueChange = {
                                    restorePasswordInput = it
                                    pasteError = null
                                },
                                label = { Text("Decryption Password") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (pasteError != null) {
                                Text(pasteError!!, color = CrimsonAlert, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                enabled = !isWorking,
                                onClick = {
                                    if (clipboardPayloadInput.isBlank()) {
                                        pasteError = "Please paste the backup JSON payload."
                                        return@Button
                                    }
                                    if (restorePasswordInput.isBlank()) {
                                        pasteError = "Please enter the password."
                                        return@Button
                                    }
                                    isWorking = true
                                    val bytes = clipboardPayloadInput.trim().toByteArray(Charsets.UTF_8)
                                    onParseAndDecryptBackup(bytes, restorePasswordInput, bytes.size.toLong()) { result ->
                                        isWorking = false
                                        result.onSuccess { preview ->
                                            pendingRestorePreview = preview
                                            pendingRestoreFileBytes = bytes
                                            pendingRestoreFileName = "ClipboardBackup.cardvaultbackup"
                                            showUnifiedRestoreDialog = false
                                            showRestorePreviewDialog = true
                                            pasteError = null
                                        }.onFailure { err ->
                                            pasteError = err.localizedMessage ?: "Failed to decrypt. Check password or JSON."
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isWorking) "Decrypting..." else "Decrypt Clipboard Data")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    enabled = !isWorking,
                    onClick = {
                        showUnifiedRestoreDialog = false
                        clipboardPayloadInput = ""
                        restorePasswordInput = ""
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Decrypt Password Prompt Dialog
    if (showDecryptPasswordDialog && pendingRestoreFileBytes != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isWorking) {
                    showDecryptPasswordDialog = false
                    pendingRestoreFileBytes = null
                    restorePasswordInput = ""
                    decryptErrorMsg = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = IndigoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter Backup Password", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter the password used when creating this AES-256-GCM encrypted backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = restorePasswordInput,
                        onValueChange = {
                            restorePasswordInput = it
                            decryptErrorMsg = null
                        },
                        label = { Text("Backup Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_password_input")
                    )

                    if (decryptErrorMsg != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CrimsonAlert.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = decryptErrorMsg!!,
                                color = CrimsonAlert,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isWorking,
                    onClick = {
                        val fileBytes = pendingRestoreFileBytes ?: return@Button
                        if (restorePasswordInput.isBlank()) {
                            decryptErrorMsg = "Please enter the backup password."
                            return@Button
                        }
                        isWorking = true
                        onParseAndDecryptBackup(fileBytes, restorePasswordInput, fileBytes.size.toLong()) { result ->
                            isWorking = false
                            result.onSuccess { preview ->
                                pendingRestorePreview = preview
                                showDecryptPasswordDialog = false
                                showRestorePreviewDialog = true
                                decryptErrorMsg = null
                            }.onFailure { err ->
                                decryptErrorMsg = err.localizedMessage ?: "Incorrect password or corrupted backup file."
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_decrypt_backup_button")
                ) {
                    Text(if (isWorking) "Decrypting..." else "Decrypt & Preview")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isWorking,
                    onClick = {
                        showDecryptPasswordDialog = false
                        pendingRestoreFileBytes = null
                        restorePasswordInput = ""
                        decryptErrorMsg = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Preview Dialog (uses verified decrypted payload counts & plain Column)
    if (showRestorePreviewDialog && pendingRestorePreview != null) {
        val preview = pendingRestorePreview!!
        val verifiedCards = preview.payload.cards.size
        val verifiedTxs = preview.payload.transactions.size
        val verifiedAtt = preview.payload.attendanceRecords.size

        AlertDialog(
            onDismissRequest = {
                if (!isWorking) {
                    showRestorePreviewDialog = false
                    pendingRestorePreview = null
                    pendingRestoreFileBytes = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = IndigoPrimary)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Restore Backup Preview", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Backup Created: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(preview.createdAt)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Size: ${formatFileSize(preview.fileSizeBytes)} • Schema v${preview.schemaVersion}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Verified Contents: $verifiedCards Cards • $verifiedTxs Transactions • $verifiedAtt Attendance",
                                fontSize = 11.sp,
                                color = IndigoPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text("Select Categories to Restore:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = restoreSelectedCategories.contains("cards"),
                            onCheckedChange = {
                                restoreSelectedCategories = if (it) restoreSelectedCategories + "cards" else restoreSelectedCategories - "cards"
                            }
                        )
                        Text("Cards ($verifiedCards)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = restoreSelectedCategories.contains("transactions"),
                            onCheckedChange = {
                                restoreSelectedCategories = if (it) restoreSelectedCategories + "transactions" else restoreSelectedCategories - "transactions"
                            }
                        )
                        Text("Usage History ($verifiedTxs)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = restoreSelectedCategories.contains("attendance"),
                            onCheckedChange = {
                                restoreSelectedCategories = if (it) restoreSelectedCategories + "attendance" else restoreSelectedCategories - "attendance"
                            }
                        )
                        Text("Attendance Records ($verifiedAtt)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Text("Select Restore Mode:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restoreModeSelection = RestoreMode.REPLACE },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = restoreModeSelection == RestoreMode.REPLACE,
                            onClick = { restoreModeSelection = RestoreMode.REPLACE }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Replace Existing Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Clears selected categories and replaces with backup", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restoreModeSelection = RestoreMode.MERGE },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = restoreModeSelection == RestoreMode.MERGE,
                            onClick = { restoreModeSelection = RestoreMode.MERGE }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Merge With Existing Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Deduplicates and adds non-duplicate backup entries", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (preview.payload.cards.isNotEmpty()) {
                        Text("Cards Preview ($verifiedCards):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            preview.payload.cards.take(10).forEach { card ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(card.nickname.ifBlank { "Unnamed Card" }, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("UID: ${card.uid} • ${card.technology}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            if (preview.payload.cards.size > 10) {
                                Text(
                                    text = "+ ${preview.payload.cards.size - 10} more card(s)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isWorking,
                    onClick = {
                        if (restoreSelectedCategories.isEmpty()) {
                            Toast.makeText(context, "Select at least one category to restore.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isWorking = true
                        onExecuteAtomicRestore(preview, restoreModeSelection, restoreSelectedCategories, pendingRestoreFileName) { result ->
                            isWorking = false
                            result.onSuccess { restoreResult ->
                                showRestorePreviewDialog = false
                                pendingRestorePreview = null
                                pendingRestoreFileBytes = null
                                showRestoreSuccessResult = restoreResult
                            }.onFailure { err ->
                                Toast.makeText(context, "Restore Failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("execute_restore_button")
                ) {
                    Text(if (isWorking) "Restoring..." else "Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isWorking,
                    onClick = {
                        showRestorePreviewDialog = false
                        pendingRestorePreview = null
                        pendingRestoreFileBytes = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Dialogs
    if (showBackupSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { showBackupSuccessMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, tint = EmeraldSuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup Created", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text(showBackupSuccessMessage!!, fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = { showBackupSuccessMessage = null }) { Text("OK") }
            }
        )
    }

    if (showRestoreSuccessResult != null) {
        val res = showRestoreSuccessResult!!
        AlertDialog(
            onDismissRequest = { showRestoreSuccessResult = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = EmeraldSuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Completed", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Database transactional restore completed!", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("• Cards Restored: ${res.restoredCardsCount}", fontSize = 12.sp)
                    Text("• Usage Transactions Restored: ${res.restoredTransactionsCount}", fontSize = 12.sp)
                    Text("• Attendance Records Restored: ${res.restoredAttendanceCount}", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showRestoreSuccessResult = null }) { Text("Done") }
            }
        )
    }

    if (pickerErrorDiagnostics != null) {
        AlertDialog(
            onDismissRequest = { pickerErrorDiagnostics = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CrimsonAlert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Storage Error", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pickerErrorDiagnostics!!,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                            .heightIn(max = 200.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    copySensitiveTextToClipboard(context, "Diagnostics", pickerErrorDiagnostics!!)
                    Toast.makeText(context, "Copied diagnostics to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Copy Diagnostics")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickerErrorDiagnostics = null }) { Text("Close") }
            }
        )
    }
}
