package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.security.RoleAccessState
import com.example.sync.ServerAuthResult
import com.example.sync.WalletSyncResult
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldChip
import com.example.ui.theme.IndigoPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusServerLinkSection(
    serverConfig: AttendanceConfigEntity,
    roleState: RoleAccessState,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onUpdateAutoSync: (Boolean) -> Unit,
    onAuthenticateServer: (
        serverUrl: String,
        port: Int,
        useHttps: Boolean,
        username: String,
        password: String,
        onResult: (ServerAuthResult) -> Unit
    ) -> Unit,
    onLinkWithApiKey: (
        serverUrl: String,
        port: Int,
        useHttps: Boolean,
        authType: String,
        apiKeyOrToken: String,
        providerType: String,
        campusId: String,
        deviceId: String,
        onResult: (WalletSyncResult) -> Unit
    ) -> Unit,
    onUnlinkServer: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    var showLinkDialog by remember { mutableStateOf(false) }
    var showUnlinkConfirm by remember { mutableStateOf(false) }

    val isConfigured = serverConfig.serverUrl.isNotBlank()
    val statusColor = when {
        serverConfig.isConnected -> EmeraldSuccess
        isConfigured -> GoldChip
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("campus_server_sync_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (serverConfig.isConnected) Icons.Default.CloudDone else Icons.Default.CloudSync,
                        contentDescription = "Campus Server Sync",
                        tint = if (serverConfig.isConnected) EmeraldSuccess else IndigoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CAMPUS SERVER & WALLET SYNC",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (serverConfig.isConnected) EmeraldSuccess else IndigoPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = when {
                            serverConfig.isConnected -> "LINKED"
                            isConfigured -> "OFFLINE"
                            else -> "LOCAL ONLY"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isConfigured) {
                val scheme = if (serverConfig.useHttps) "https" else "http"
                Text(
                    text = "$scheme://${serverConfig.serverUrl}:${serverConfig.port} (${serverConfig.providerType})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = buildString {
                        append("Role: ${roleState.activeRole}")
                        if (serverConfig.username.isNotBlank()) append(" • Account: ${serverConfig.username}")
                        if (serverConfig.lastSyncTimestamp > 0L) {
                            append("\nLast Sync: ${dateFormat.format(Date(serverConfig.lastSyncTimestamp))}")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            } else {
                Text(
                    text = "Link your university server to sync saved NFC cards, usage logs, and attendance records, and receive server-verified Student, Teacher, or Admin access.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            if (isConfigured) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Sync Wallet Changes",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automatically push newly scanned cards & usage logs when connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = serverConfig.autoSyncEnabled,
                        onCheckedChange = onUpdateAutoSync,
                        modifier = Modifier.testTag("wallet_auto_sync_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showLinkDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("btn_link_campus_server"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConfigured) "Manage Link" else "Link Server",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isConfigured) {
                    Button(
                        onClick = onSyncNow,
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("btn_sync_wallet_now"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSyncing) "Syncing..." else "Sync Now",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isConfigured) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showUnlinkConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp)
                        .testTag("btn_unlink_server")
                ) {
                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unlink Server & Sign Out", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showUnlinkConfirm) {
        DeleteConfirmationDialog(
            title = "Unlink Campus Server?",
            message = "This will clear your server authentication token, disable cloud sync, and reset any elevated Teacher/Admin session back to Student. Local cards remain safe on your device.",
            onConfirm = {
                showUnlinkConfirm = false
                onUnlinkServer()
            },
            onDismiss = { showUnlinkConfirm = false }
        )
    }

    if (showLinkDialog) {
        LinkCampusServerDialog(
            currentConfig = serverConfig,
            roleState = roleState,
            isWorking = isSyncing,
            onDismiss = { showLinkDialog = false },
            onAuthenticateServer = { host, port, https, user, pass, onDone ->
                onAuthenticateServer(host, port, https, user, pass) { result ->
                    onDone(result.isSuccess, result.message)
                    if (result.isSuccess) {
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        showLinkDialog = false
                    }
                }
            },
            onLinkWithApiKey = { host, port, https, authType, key, provider, campus, device, onDone ->
                onLinkWithApiKey(host, port, https, authType, key, provider, campus, device) { result ->
                    onDone(result.isSuccess, result.message)
                    if (result.isSuccess) {
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        showLinkDialog = false
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkCampusServerDialog(
    currentConfig: AttendanceConfigEntity,
    roleState: RoleAccessState,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onAuthenticateServer: (
        host: String,
        port: Int,
        useHttps: Boolean,
        username: String,
        password: String,
        onDone: (Boolean, String) -> Unit
    ) -> Unit,
    onLinkWithApiKey: (
        host: String,
        port: Int,
        useHttps: Boolean,
        authType: String,
        apiKeyOrToken: String,
        providerType: String,
        campusId: String,
        deviceId: String,
        onDone: (Boolean, String) -> Unit
    ) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Account Login, 1 = API Key (Admin)
    var serverHost by remember { mutableStateOf(currentConfig.serverUrl) }
    var portText by remember { mutableStateOf(currentConfig.port.toString()) }
    var useHttps by remember { mutableStateOf(currentConfig.useHttps) }

    // Account login fields
    var username by remember { mutableStateOf(currentConfig.username) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // API key fields (Admin only if Admin passcode is configured)
    var authType by remember { mutableStateOf(currentConfig.authType) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var providerType by remember { mutableStateOf(currentConfig.providerType) }
    var campusId by remember { mutableStateOf(currentConfig.campusId) }
    var deviceId by remember { mutableStateOf(currentConfig.deviceId) }
    var providerExpanded by remember { mutableStateOf(false) }
    var authExpanded by remember { mutableStateOf(false) }

    var feedbackMsg by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(true) }

    val canEditApiKeyConfig = !roleState.hasAdminPasscode || roleState.canManageServerAndRoles

    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudSync, contentDescription = null, tint = IndigoPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Link Campus Server", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; feedbackMsg = null },
                        text = { Text("Account Login", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; feedbackMsg = null },
                        text = { Text("API Key (Admin)", fontSize = 12.sp) }
                    )
                }

                OutlinedTextField(
                    value = serverHost,
                    onValueChange = { serverHost = it; feedbackMsg = null },
                    label = { Text("Campus Server Host / Domain *") },
                    placeholder = { Text("wallet.university.edu") },
                    leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { input ->
                            if (input.length <= 5 && input.all { it.isDigit() }) {
                                portText = input
                                feedbackMsg = null
                            }
                        },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Switch(checked = useHttps, onCheckedChange = { useHttps = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (useHttps) "HTTPS" else "HTTP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (!useHttps) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = GoldChip, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Use HTTPS for encrypted credentials in transit.",
                            fontSize = 11.sp,
                            color = GoldChip
                        )
                    }
                }

                if (selectedTab == 0) {
                    Text(
                        text = "Sign in with your university account (POST /api/v1/auth/login). Your Student, Teacher, or Admin permissions are issued directly by the server.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; feedbackMsg = null },
                        label = { Text("Username / University Email *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; feedbackMsg = null },
                        label = { Text("Account Password *") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    if (!canEditApiKeyConfig) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Administrator authentication is required to modify direct API Key server links. Sign in as Admin in the Attendance tab or use Account Login.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = providerExpanded,
                            onExpandedChange = { providerExpanded = !providerExpanded }
                        ) {
                            OutlinedTextField(
                                value = providerType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Server Provider") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = providerExpanded,
                                onDismissRequest = { providerExpanded = false }
                            ) {
                                listOf("CUSTOM", "JKTECO", "SUPREMA", "HID", "GALLAGHER").forEach { code ->
                                    DropdownMenuItem(
                                        text = { Text(code) },
                                        onClick = {
                                            providerType = code
                                            providerExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = authExpanded,
                            onExpandedChange = { authExpanded = !authExpanded }
                        ) {
                            OutlinedTextField(
                                value = authType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Auth Header Scheme") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = authExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = authExpanded,
                                onDismissRequest = { authExpanded = false }
                            ) {
                                listOf("API_KEY", "JWT", "OAUTH2", "SESSION").forEach { scheme ->
                                    DropdownMenuItem(
                                        text = { Text(scheme) },
                                        onClick = {
                                            authType = scheme
                                            authExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it; feedbackMsg = null },
                            label = { Text("API Key / Bearer Token (Encrypted)") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle key visibility"
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = campusId,
                            onValueChange = { campusId = it },
                            label = { Text("Campus ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = deviceId,
                            onValueChange = { deviceId = it },
                            label = { Text("Device Terminal ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (feedbackMsg != null) {
                    Text(
                        text = feedbackMsg!!,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (feedbackIsError) MaterialTheme.colorScheme.error else EmeraldSuccess
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isWorking && (selectedTab == 0 || canEditApiKeyConfig),
                onClick = {
                    val host = serverHost.trim()
                    val port = portText.trim().toIntOrNull()
                    if (host.isBlank() || host.contains(" ")) {
                        feedbackIsError = true
                        feedbackMsg = "Enter a valid Server Host / Domain without spaces."
                        return@Button
                    }
                    if (port == null || port !in 1..65535) {
                        feedbackIsError = true
                        feedbackMsg = "Port must be between 1 and 65535."
                        return@Button
                    }

                    if (selectedTab == 0) {
                        if (username.isBlank() || password.isBlank()) {
                            feedbackIsError = true
                            feedbackMsg = "Username and password are required."
                            return@Button
                        }
                        onAuthenticateServer(host, port, useHttps, username, password) { ok, msg ->
                            feedbackIsError = !ok
                            feedbackMsg = msg
                        }
                    } else {
                        onLinkWithApiKey(host, port, useHttps, authType, apiKey, providerType, campusId, deviceId) { ok, msg ->
                            feedbackIsError = !ok
                            feedbackMsg = msg
                        }
                    }
                }
            ) {
                if (isWorking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (selectedTab == 0) "Sign In & Link" else "Test & Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) {
                Text("Cancel")
            }
        }
    )
}
