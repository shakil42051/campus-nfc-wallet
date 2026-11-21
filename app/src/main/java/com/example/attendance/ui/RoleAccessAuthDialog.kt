package com.example.attendance.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendance.data.entity.AttendanceConfigEntity
import com.example.security.CampusRoles
import com.example.security.RoleAccessManager
import com.example.security.RoleAccessState
import com.example.security.RoleAuthResult
import com.example.sync.ServerAuthResult
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.delay

@Composable
fun RoleAccessAuthDialog(
    config: AttendanceConfigEntity,
    roleState: RoleAccessState,
    isAuthenticatingServer: Boolean,
    getRemainingLockoutSeconds: () -> Int,
    onDismiss: () -> Unit,
    onAuthenticateServer: (
        serverUrl: String,
        port: Int,
        useHttps: Boolean,
        username: String,
        password: String,
        onResult: (ServerAuthResult) -> Unit
    ) -> Unit,
    onInitializeAdminPasscode: (
        adminPasscode: String,
        teacherPasscode: String?,
        onResult: (Result<Unit>) -> Unit
    ) -> Unit,
    onUnlockLocalRole: (
        targetRole: String,
        passcode: String,
        onResult: (RoleAuthResult) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(if (config.serverUrl.isNotBlank()) 0 else 1) }

    // Server Sign-In fields
    var serverHost by remember { mutableStateOf(config.serverUrl) }
    var portText by remember { mutableStateOf(config.port.toString()) }
    var useHttps by remember { mutableStateOf(config.useHttps) }
    var username by remember { mutableStateOf(config.username) }
    var serverPassword by remember { mutableStateOf("") }
    var showServerPass by remember { mutableStateOf(false) }

    // Local Staff Passcode fields
    var targetRole by remember { mutableStateOf(CampusRoles.TEACHER) }
    var localPasscode by remember { mutableStateOf("") }
    var showLocalPass by remember { mutableStateOf(false) }

    // First-time Admin Initialization fields
    var newAdminPass by remember { mutableStateOf("") }
    var confirmAdminPass by remember { mutableStateOf("") }
    var initialTeacherPass by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableIntStateOf(getRemainingLockoutSeconds()) }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = getRemainingLockoutSeconds()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isAuthenticatingServer) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = IndigoPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Staff & Admin Access", fontWeight = FontWeight.Bold)
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
                        onClick = { selectedTab = 0; errorMessage = null },
                        text = { Text("Server Login", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; errorMessage = null },
                        text = {
                            Text(
                                if (roleState.hasAdminPasscode) "Staff Passcode" else "Setup Admin",
                                fontSize = 12.sp
                            )
                        }
                    )
                }

                if (selectedTab == 0) {
                    Text(
                        text = "Authenticate with your university server (POST /api/v1/auth/login). Your Teacher or Admin permissions are verified and issued by the server.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = serverHost,
                        onValueChange = { serverHost = it; errorMessage = null },
                        label = { Text("Campus Server Host *") },
                        placeholder = { Text("attendance.university.edu") },
                        leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = portText,
                            onValueChange = { input ->
                                if (input.length <= 5 && input.all { it.isDigit() }) {
                                    portText = input
                                    errorMessage = null
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

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorMessage = null },
                        label = { Text("Staff / Admin Username or Email *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = serverPassword,
                        onValueChange = { serverPassword = it; errorMessage = null },
                        label = { Text("Password *") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (showServerPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showServerPass = !showServerPass }) {
                                Icon(
                                    imageVector = if (showServerPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    if (!roleState.hasAdminPasscode) {
                        Text(
                            text = "First-time setup: create a protected Administrator Passcode (${RoleAccessManager.MIN_ADMIN_PASSCODE_LENGTH}+ chars) so students cannot open Teacher or Admin controls. Stored locally using salted PBKDF2-HMAC-SHA256.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = newAdminPass,
                            onValueChange = { newAdminPass = it; errorMessage = null },
                            label = { Text("Administrator Passcode (8+ chars) *") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            visualTransformation = if (showLocalPass) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showLocalPass = !showLocalPass }) {
                                    Icon(
                                        imageVector = if (showLocalPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle passcode visibility"
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("init_admin_passcode_input")
                        )

                        OutlinedTextField(
                            value = confirmAdminPass,
                            onValueChange = { confirmAdminPass = it; errorMessage = null },
                            label = { Text("Confirm Administrator Passcode *") },
                            visualTransformation = if (showLocalPass) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_admin_passcode_input")
                        )

                        OutlinedTextField(
                            value = initialTeacherPass,
                            onValueChange = { initialTeacherPass = it; errorMessage = null },
                            label = { Text("Teacher Passcode (Optional, 6+ chars)") },
                            visualTransformation = if (showLocalPass) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("init_teacher_passcode_input")
                        )
                    } else {
                        if (lockoutSeconds > 0) {
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
                                        Icons.Default.LockClock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Too many failed attempts. Try again in ${lockoutSeconds}s.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Select the role you want to unlock and enter the authorized staff passcode.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = targetRole == CampusRoles.TEACHER,
                                    onClick = { targetRole = CampusRoles.TEACHER; errorMessage = null },
                                    label = { Text("Teacher") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = targetRole == CampusRoles.ADMIN,
                                    onClick = { targetRole = CampusRoles.ADMIN; errorMessage = null },
                                    label = { Text("Administrator") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = localPasscode,
                                onValueChange = { localPasscode = it; errorMessage = null },
                                label = {
                                    Text(
                                        if (targetRole == CampusRoles.ADMIN) "Administrator Passcode" else "Teacher / Admin Passcode"
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                visualTransformation = if (showLocalPass) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showLocalPass = !showLocalPass }) {
                                        Icon(
                                            imageVector = if (showLocalPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle passcode visibility"
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("local_role_passcode_input")
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isAuthenticatingServer && (selectedTab == 0 || lockoutSeconds == 0),
                onClick = {
                    if (selectedTab == 0) {
                        val host = serverHost.trim()
                        val port = portText.trim().toIntOrNull()
                        if (host.isBlank() || host.contains(" ")) {
                            errorMessage = "Enter a valid Server Host."
                            return@Button
                        }
                        if (port == null || port !in 1..65535) {
                            errorMessage = "Port must be between 1 and 65535."
                            return@Button
                        }
                        if (username.isBlank() || serverPassword.isBlank()) {
                            errorMessage = "Username and password are required."
                            return@Button
                        }
                        onAuthenticateServer(host, port, useHttps, username, serverPassword) { res ->
                            if (res.isSuccess) {
                                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                onDismiss()
                            } else {
                                errorMessage = res.message
                            }
                        }
                    } else if (!roleState.hasAdminPasscode) {
                        if (newAdminPass.length < RoleAccessManager.MIN_ADMIN_PASSCODE_LENGTH) {
                            errorMessage = "Admin passcode must be at least ${RoleAccessManager.MIN_ADMIN_PASSCODE_LENGTH} characters."
                            return@Button
                        }
                        if (newAdminPass != confirmAdminPass) {
                            errorMessage = "Admin passcodes do not match."
                            return@Button
                        }
                        if (initialTeacherPass.isNotBlank() && initialTeacherPass.length < RoleAccessManager.MIN_TEACHER_PASSCODE_LENGTH) {
                            errorMessage = "Teacher passcode must be at least ${RoleAccessManager.MIN_TEACHER_PASSCODE_LENGTH} characters."
                            return@Button
                        }
                        onInitializeAdminPasscode(
                            newAdminPass,
                            initialTeacherPass.ifBlank { null }
                        ) { res ->
                            if (res.isSuccess) {
                                Toast.makeText(context, "Administrator access initialized and unlocked!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to initialize Admin access."
                            }
                        }
                    } else {
                        onUnlockLocalRole(targetRole, localPasscode) { result ->
                            when (result) {
                                is RoleAuthResult.Success -> {
                                    Toast.makeText(context, "Unlocked ${result.role} session.", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                is RoleAuthResult.InvalidCredentials -> {
                                    errorMessage = "Incorrect passcode (${result.remainingAttempts} attempts left)."
                                    localPasscode = ""
                                }
                                is RoleAuthResult.LockedOut -> {
                                    lockoutSeconds = result.remainingSeconds
                                    errorMessage = "Locked out for ${result.remainingSeconds}s."
                                    localPasscode = ""
                                }
                                is RoleAuthResult.NotConfigured -> {
                                    errorMessage = result.message
                                }
                            }
                        }
                    }
                }
            ) {
                if (isAuthenticatingServer) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    when {
                        selectedTab == 0 -> "Sign In"
                        !roleState.hasAdminPasscode -> "Create & Unlock Admin"
                        else -> "Unlock ${if (targetRole == CampusRoles.ADMIN) "Admin" else "Teacher"}"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isAuthenticatingServer) {
                Text("Cancel")
            }
        }
    )
}
