package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.backup.model.RestoreMode
import com.example.backup.model.RestorePreview
import com.example.backup.model.RestoreResult
import com.example.backup.service.PreparedBackupArtifact
import com.example.data.repository.AppSettings
import com.example.data.repository.AuthMethods
import com.example.data.repository.SettingsRepository
import com.example.security.RoleAccessState
import com.example.sync.ServerAuthResult
import com.example.sync.WalletSyncResult
import com.example.ui.components.AppLockSetupWizardDialog
import com.example.ui.components.BackupRestoreSection
import com.example.ui.components.CampusServerLinkSection
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.SecurityAuthModal
import com.example.ui.components.SetupPinDialog
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.BackupOverviewState

private data class PendingSettingsAuth(
    val title: String,
    val description: String,
    val onAuthorized: () -> Unit
)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    serverConfig: AttendanceConfigEntity = AttendanceConfigEntity(),
    roleState: RoleAccessState = RoleAccessState(),
    isWalletSyncing: Boolean = false,
    onSyncWalletNow: () -> Unit = {},
    onUpdateAutoSync: (Boolean) -> Unit = {},
    onAuthenticateWalletServer: (String, Int, Boolean, String, String, (ServerAuthResult) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onLinkWalletServerWithApiKey: (String, Int, Boolean, String, String, String, String, String, (WalletSyncResult) -> Unit) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onUnlinkWalletServer: () -> Unit = {},
    backupOverview: BackupOverviewState,
    onRefreshBackupOverview: () -> Unit = {},
    onBeforeExternalAction: () -> Unit = {},
    onUpdateVibration: (Boolean) -> Unit,
    onUpdateSound: (Boolean) -> Unit,
    onUpdateAutoExport: (Boolean) -> Unit,
    onUpdateDarkMode: (Boolean) -> Unit,
    onUpdateDeveloperMode: (Boolean) -> Unit,
    onUpdateBiometricEnabled: (Boolean) -> Unit = {},
    onUpdateRequireAuthForBackupRestore: (Boolean) -> Unit = {},
    onUpdateAppLockEnabled: (Boolean) -> Unit = {},
    onUpdateAutoLockTimeoutSeconds: (Int) -> Unit = {},
    onUpdateAuthMethod: (String) -> Unit = {},
    onLockAppNow: () -> Unit = {},
    onSetSecurityPassword: (String) -> Unit = {},
    onSetSecurityPinAndQuestion: (pin: String, question: String, answer: String) -> Unit = { _, _, _ -> },
    onSetSecurityQuestionAndAnswer: (question: String, answer: String) -> Unit = { _, _ -> },
    onClearSecurityPin: () -> Unit = {},
    onVerifyPin: (String) -> Boolean = { false },
    onVerifyPassword: (String) -> Boolean = { false },
    onVerifySecretAnswer: (String) -> Boolean = { false },
    onClearAllCards: () -> Unit,
    onNavigateToAbout: (() -> Unit)? = null,
    onCreateEncryptedBackup: (password: String, fileName: String, callback: (Result<PreparedBackupArtifact>) -> Unit) -> Unit = { _, _, _ -> },
    onRecordExternalBackupCompleted: (artifact: PreparedBackupArtifact, onDone: () -> Unit) -> Unit = { _, done -> done() },
    onParseAndDecryptBackup: (fileBytes: ByteArray, password: String, fileSizeBytes: Long, callback: (Result<RestorePreview>) -> Unit) -> Unit = { _, _, _, _ -> },
    onExecuteAtomicRestore: (preview: RestorePreview, mode: RestoreMode, selectedCategories: Set<String>, fileName: String, callback: (Result<RestoreResult>) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onReadBytesFromUriAsync: (context: Context, uri: Uri, callback: (Result<ByteArray>) -> Unit) -> Unit = { _, _, _ -> },
    onWriteBytesToUriAsync: (context: Context, uri: Uri, bytes: ByteArray, callback: (Result<Unit>) -> Unit) -> Unit = { _, _, _, _ -> },
    buildDiagnosticsString: (exception: Exception) -> String = { it.localizedMessage ?: "Error" },
    onCreateInternalSnapshot: (password: String, label: String, callback: (Result<ByteArray>) -> Unit) -> Unit = { _, _, _ -> },
    onDeleteInternalSnapshotAsync: (fileName: String, callback: (Boolean) -> Unit) -> Unit = { _, _ -> },
    onReadInternalSnapshotAsync: (fileName: String, callback: (ByteArray?) -> Unit) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showSetupPinDialog by remember { mutableStateOf(false) }
    var showWizardDialog by remember { mutableStateOf(false) }
    var showPasswordSetupDialog by remember { mutableStateOf(false) }
    var showAuthMethodDialog by remember { mutableStateOf(false) }
    var showAutoLockTimerDialog by remember { mutableStateOf(false) }
    var pendingAuth by remember { mutableStateOf<PendingSettingsAuth?>(null) }

    fun requireAuthIfConfigured(
        title: String,
        description: String,
        enforce: Boolean = settings.hasAnyCredential,
        onAuthorized: () -> Unit
    ) {
        if (enforce && settings.hasAnyCredential) {
            pendingAuth = PendingSettingsAuth(title, description, onAuthorized)
        } else {
            onAuthorized()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Wallet Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Configure hardware feedback, backups, and security preferences",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Biometric & Security PIN Protection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "BIOMETRIC & PIN SECURITY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = "Lock App on Launch",
                        subtitle = "Require PIN, Password, or Strong Biometrics when opening the app",
                        icon = Icons.Default.Security,
                        checked = settings.isAppLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showWizardDialog = true
                            } else {
                                requireAuthIfConfigured(
                                    title = "Disable App Lock",
                                    description = "Verify your identity before turning off App Lock."
                                ) {
                                    onUpdateAppLockEnabled(false)
                                }
                            }
                        },
                        testTag = "app_lock_switch"
                    )

                    SettingSwitchRow(
                        title = "Fingerprint / Biometric Lock",
                        subtitle = "Use device strong biometrics to unlock sensitive actions",
                        icon = Icons.Default.Fingerprint,
                        checked = settings.isBiometricEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !settings.hasAnyCredential) {
                                showSetupPinDialog = true
                            } else if (!enabled && settings.isAppLockEnabled) {
                                requireAuthIfConfigured(
                                    title = "Disable Biometrics",
                                    description = "Verify your identity before changing biometric settings."
                                ) {
                                    onUpdateBiometricEnabled(false)
                                }
                            } else {
                                onUpdateBiometricEnabled(enabled)
                            }
                        },
                        testTag = "biometric_switch"
                    )

                    SettingSwitchRow(
                        title = "Require Auth for Backup & Restore",
                        subtitle = "Prompt for credentials before exporting or restoring vault backups",
                        icon = Icons.Default.Shield,
                        checked = settings.requireAuthForBackupRestore,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                onUpdateRequireAuthForBackupRestore(true)
                            } else {
                                requireAuthIfConfigured(
                                    title = "Disable Backup Protection",
                                    description = "Verify your identity before removing backup authorization."
                                ) {
                                    onUpdateRequireAuthForBackupRestore(false)
                                }
                            }
                        },
                        testTag = "backup_auth_switch"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Auto-Lock Timer Button
                    OutlinedButton(
                        onClick = { showAutoLockTimerDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_lock_timer_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Auto-Lock Timer: " + when (settings.autoLockTimeoutSeconds) {
                                30 -> "30 seconds"
                                60 -> "1 minute"
                                300 -> "5 minutes"
                                else -> "Immediately"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Unlock Method Selector Button
                    OutlinedButton(
                        onClick = {
                            requireAuthIfConfigured(
                                title = "Change Unlock Method",
                                description = "Verify your identity before changing the unlock method.",
                                enforce = settings.isAppLockEnabled
                            ) {
                                showAuthMethodDialog = true
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_method_selector_button")
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Unlock Method: ${AuthMethods.displayLabel(settings.authMethod)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (settings.isAppLockEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onLockAppNow,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lock_app_now_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Lock App Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (settings.hasSecurityPin) {
                                    requireAuthIfConfigured(
                                        title = "Change Security PIN",
                                        description = "Verify your current credentials before updating PIN."
                                    ) {
                                        showSetupPinDialog = true
                                    }
                                } else {
                                    showSetupPinDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manage_pin_button")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = if (settings.hasSecurityPin) "Change PIN" else "Set PIN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (settings.hasSecurityPassword) {
                                    requireAuthIfConfigured(
                                        title = "Change Master Password",
                                        description = "Verify your current credentials before updating password."
                                    ) {
                                        showPasswordSetupDialog = true
                                    }
                                } else {
                                    showPasswordSetupDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manage_password_button")
                        ) {
                            Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = if (settings.hasSecurityPassword) "Change Password" else "Set Password",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scanner Feedback Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NFC SCANNER FEEDBACK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = "Haptic Vibration",
                        subtitle = "Vibrate when NFC card is detected",
                        icon = Icons.Default.Vibration,
                        checked = settings.enableVibration,
                        onCheckedChange = onUpdateVibration,
                        testTag = "vibration_switch"
                    )

                    SettingSwitchRow(
                        title = "Scan Audio Beep",
                        subtitle = "Play notification tone on card read",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        checked = settings.enableSound,
                        onCheckedChange = onUpdateSound,
                        testTag = "sound_switch"
                    )

                    SettingSwitchRow(
                        title = "Auto-Save Scanned Cards",
                        subtitle = "Automatically save each scanned NFC tag directly into the vault",
                        icon = Icons.Default.Save,
                        checked = settings.autoExportOnScan,
                        onCheckedChange = onUpdateAutoExport,
                        testTag = "auto_export_switch"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // UI & Developer Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "APPLICATION PREFERENCES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = "Dark Theme",
                        subtitle = "Use high-contrast dark palette across all screens",
                        icon = Icons.Default.DarkMode,
                        checked = settings.isDarkMode,
                        onCheckedChange = onUpdateDarkMode,
                        testTag = "dark_mode_switch"
                    )

                    SettingSwitchRow(
                        title = "Developer Mode",
                        subtitle = "Show raw SAK/ATQA binary decoders, UID bitmasks, and sector hex dumps",
                        icon = Icons.Default.Code,
                        checked = settings.isDeveloperMode,
                        onCheckedChange = onUpdateDeveloperMode,
                        testTag = "developer_mode_switch"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Android Keystore Security Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Android Keystore",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "ANDROID KEYSTORE ENCRYPTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Master Key: CampusNfcWalletMasterKey (AES-256-GCM)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Saved card payloads and server API credentials are encrypted at rest in Room using Android Keystore AES-256-GCM. Portable backups use PBKDF2-HMAC-SHA256 (210,000 iterations) + AES-256-GCM.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campus Server Link & Wallet Sync Section
            CampusServerLinkSection(
                serverConfig = serverConfig,
                roleState = roleState,
                isSyncing = isWalletSyncing,
                onSyncNow = onSyncWalletNow,
                onUpdateAutoSync = onUpdateAutoSync,
                onAuthenticateServer = onAuthenticateWalletServer,
                onLinkWithApiKey = onLinkWalletServerWithApiKey,
                onUnlinkServer = onUnlinkWalletServer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Storage Backup & Restore Section
            BackupRestoreSection(
                backupOverview = backupOverview,
                onRefreshOverview = onRefreshBackupOverview,
                onBeforeExternalAction = onBeforeExternalAction,
                onRequestAuth = { title, description, onAuthorized ->
                    requireAuthIfConfigured(
                        title = title,
                        description = description,
                        enforce = settings.requireAuthForBackupRestore && settings.hasAnyCredential,
                        onAuthorized = onAuthorized
                    )
                },
                onCreateEncryptedBackup = onCreateEncryptedBackup,
                onRecordExternalBackupCompleted = onRecordExternalBackupCompleted,
                onParseAndDecryptBackup = onParseAndDecryptBackup,
                onExecuteAtomicRestore = onExecuteAtomicRestore,
                onReadBytesFromUriAsync = onReadBytesFromUriAsync,
                onWriteBytesToUriAsync = onWriteBytesToUriAsync,
                buildDiagnosticsString = buildDiagnosticsString,
                onCreateInternalSnapshot = onCreateInternalSnapshot,
                onDeleteInternalSnapshotAsync = onDeleteInternalSnapshotAsync,
                onReadInternalSnapshotAsync = onReadInternalSnapshotAsync
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Data Management
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DATA STORAGE MANAGEMENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonAlert
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            requireAuthIfConfigured(
                                title = "Clear All Saved Cards",
                                description = "Verify your identity before clearing the saved card vault."
                            ) {
                                showClearDialog = true
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_all_cards_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Clear")
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Clear All Saved Cards from Room DB")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Developer & About App Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_about_developer_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Developer Profile",
                                tint = IndigoPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "ABOUT & DEVELOPER PROFILE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary,
                                letterSpacing = 0.8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (onNavigateToAbout != null) {
                            TextButton(
                                onClick = onNavigateToAbout,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.testTag("open_about_screen_button")
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Full About", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "MD.Shakil Ahmad Nahid",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Campus NFC Wallet • Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Southeast University", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            try {
                                onBeforeExternalAction()
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:shakilahmad42052@gmail.com")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "No email app installed on this device.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("shakilahmad42052@gmail.com", style = MaterialTheme.typography.bodySmall, color = IndigoPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    if (onNavigateToAbout != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToAbout,
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_view_about_details")
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Full About & NFC Standards Guide", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // Security Authorization Modal for Sensitive Settings & Backup Actions
        pendingAuth?.let { request ->
            SecurityAuthModal(
                settings = settings,
                actionTitle = request.title,
                actionDescription = request.description,
                onDismiss = { pendingAuth = null },
                onAuthenticated = {
                    val action = request.onAuthorized
                    pendingAuth = null
                    action()
                },
                onVerifyPin = onVerifyPin,
                onVerifyPassword = onVerifyPassword,
                onVerifySecretAnswer = onVerifySecretAnswer
            )
        }

        // Clear All Saved Cards Confirmation Dialog (previously missing!)
        if (showClearDialog) {
            DeleteConfirmationDialog(
                title = "Clear All Saved Cards?",
                message = "This will permanently delete every saved NFC card and digital student pass from local Room storage. Create an encrypted backup first if you want to keep a copy.",
                onConfirm = {
                    showClearDialog = false
                    onClearAllCards()
                    Toast.makeText(context, "All saved cards cleared from vault.", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showClearDialog = false }
            )
        }

        if (showWizardDialog) {
            AppLockSetupWizardDialog(
                onDismiss = { showWizardDialog = false },
                onCompleteSetup = { method, pin, password, question, answer ->
                    if (pin.isNotBlank()) {
                        onSetSecurityPinAndQuestion(pin, question, answer)
                    } else if (question.isNotBlank() && answer.isNotBlank()) {
                        onSetSecurityQuestionAndAnswer(question, answer)
                    }
                    if (password.isNotBlank()) {
                        onSetSecurityPassword(password)
                    }
                    onUpdateAuthMethod(method)
                    onUpdateBiometricEnabled(AuthMethods.usesBiometric(method))
                    onUpdateAppLockEnabled(true)
                    showWizardDialog = false
                    Toast.makeText(context, "App Lock setup complete!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Setup PIN & Recovery Dialog
        if (showSetupPinDialog) {
            SetupPinDialog(
                currentSettings = settings,
                onDismiss = { showSetupPinDialog = false },
                onSavePinAndQuestion = { pin, question, answer ->
                    onSetSecurityPinAndQuestion(pin, question, answer)
                },
                onClearPin = {
                    onClearSecurityPin()
                    if (AuthMethods.usesPin(settings.authMethod) && !settings.hasSecurityPassword) {
                        onUpdateAppLockEnabled(false)
                    }
                }
            )
        }

        // Setup Master Password Dialog
        if (showPasswordSetupDialog) {
            var newPass by remember { mutableStateOf("") }
            var confirmPass by remember { mutableStateOf("") }
            var passError by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { showPasswordSetupDialog = false },
                title = { Text("Set Master Password", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Enter an ${SettingsRepository.MIN_PASSWORD_LENGTH}+ character master password to protect your app.",
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it; passError = null },
                            label = { Text("New Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it; passError = null },
                            label = { Text("Confirm Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (passError != null) {
                            Text(passError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPass.length < SettingsRepository.MIN_PASSWORD_LENGTH) {
                                passError = "Password must be at least ${SettingsRepository.MIN_PASSWORD_LENGTH} characters long"
                            } else if (newPass != confirmPass) {
                                passError = "Passwords do not match"
                            } else {
                                onSetSecurityPassword(newPass)
                                Toast.makeText(context, "Master Password Saved!", Toast.LENGTH_SHORT).show()
                                showPasswordSetupDialog = false
                            }
                        }
                    ) {
                        Text("Save Password")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPasswordSetupDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Select Auto-Lock Timer Dialog
        if (showAutoLockTimerDialog) {
            val timerOptions = listOf(
                0 to "Immediately",
                30 to "30 seconds",
                60 to "1 minute",
                300 to "5 minutes"
            )

            AlertDialog(
                onDismissRequest = { showAutoLockTimerDialog = false },
                title = { Text("Auto-Lock Timer", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Choose how quickly the app locks when moved to the background:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        timerOptions.forEach { (seconds, label) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (settings.autoLockTimeoutSeconds == seconds) IndigoPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUpdateAutoLockTimeoutSeconds(seconds)
                                        Toast.makeText(context, "Auto-lock timer updated: $label", Toast.LENGTH_SHORT).show()
                                        showAutoLockTimerDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = settings.autoLockTimeoutSeconds == seconds,
                                        onClick = {
                                            onUpdateAutoLockTimeoutSeconds(seconds)
                                            Toast.makeText(context, "Auto-lock timer updated: $label", Toast.LENGTH_SHORT).show()
                                            showAutoLockTimerDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAutoLockTimerDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Select Authentication Method Dialog
        if (showAuthMethodDialog) {
            val options = listOf(
                AuthMethods.FINGERPRINT_PIN to AuthMethods.displayLabel(AuthMethods.FINGERPRINT_PIN),
                AuthMethods.FINGERPRINT_PASSWORD to AuthMethods.displayLabel(AuthMethods.FINGERPRINT_PASSWORD),
                AuthMethods.PIN_ONLY to AuthMethods.displayLabel(AuthMethods.PIN_ONLY),
                AuthMethods.PASSWORD_ONLY to AuthMethods.displayLabel(AuthMethods.PASSWORD_ONLY)
            )

            fun selectMethod(key: String) {
                if (AuthMethods.usesPin(key) && !settings.hasSecurityPin) {
                    showAuthMethodDialog = false
                    Toast.makeText(context, "Please set a 4–6 digit PIN first.", Toast.LENGTH_SHORT).show()
                    showSetupPinDialog = true
                    return
                }
                if (AuthMethods.usesPassword(key) && !settings.hasSecurityPassword) {
                    showAuthMethodDialog = false
                    Toast.makeText(context, "Please set an 8+ character Master Password first.", Toast.LENGTH_SHORT).show()
                    showPasswordSetupDialog = true
                    return
                }
                onUpdateAuthMethod(key)
                onUpdateBiometricEnabled(AuthMethods.usesBiometric(key))
                Toast.makeText(context, "Unlock method updated!", Toast.LENGTH_SHORT).show()
                showAuthMethodDialog = false
            }

            AlertDialog(
                onDismissRequest = { showAuthMethodDialog = false },
                title = { Text("Select Unlock Method", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        options.forEach { (key, label) ->
                            val isSelected = AuthMethods.normalize(settings.authMethod) == key
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) IndigoPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectMethod(key) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectMethod(key) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAuthMethodDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
