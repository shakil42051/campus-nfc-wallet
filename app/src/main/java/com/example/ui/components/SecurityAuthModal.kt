package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.fragment.app.FragmentActivity
import com.example.data.repository.AppSettings
import com.example.data.repository.AuthMethods
import com.example.data.repository.SettingsRepository
import com.example.ui.animation.bounceClick
import com.example.ui.animation.pulseEffect
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.utils.BiometricUtils

/**
 * Dialog to set up or manage a 4–6 digit numeric PIN and hashed secret recovery question.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupPinDialog(
    currentSettings: AppSettings,
    onDismiss: () -> Unit,
    onSavePinAndQuestion: (pin: String, question: String, answer: String) -> Unit,
    onClearPin: () -> Unit
) {
    val context = LocalContext.current
    var pinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    val presetQuestions = listOf(
        "What was the name of your first pet?",
        "What city were you born in?",
        "What was your high school name?",
        "What is your mother's maiden name?",
        "What was your favorite childhood food?"
    )
    var selectedQuestion by remember {
        mutableStateOf(currentSettings.securityQuestion.ifBlank { presetQuestions.first() })
    }
    var customAnswer by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = IndigoPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentSettings.hasSecurityPin) "Manage App Security PIN" else "Set Up App Security PIN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Set a 4–6 digit numeric PIN and secret recovery answer to protect sensitive wallet actions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pinText,
                    onValueChange = { input ->
                        if (input.length <= SettingsRepository.MAX_PIN_LENGTH && input.all { it.isDigit() }) {
                            pinText = input
                            validationError = null
                        }
                    },
                    label = { Text("4 to 6 Digit Numeric PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(
                                imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle PIN Visibility"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_input")
                )

                OutlinedTextField(
                    value = confirmPinText,
                    onValueChange = { input ->
                        if (input.length <= SettingsRepository.MAX_PIN_LENGTH && input.all { it.isDigit() }) {
                            confirmPinText = input
                            validationError = null
                        }
                    },
                    label = { Text("Confirm Numeric PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_confirm_pin_input")
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SECRET RECOVERY QUESTION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary
                )

                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = !menuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedQuestion,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Security Question") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        presetQuestions.forEach { q ->
                            DropdownMenuItem(
                                text = { Text(q, fontSize = 12.sp) },
                                onClick = {
                                    selectedQuestion = q
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customAnswer,
                    onValueChange = { customAnswer = it; validationError = null },
                    label = { Text("Secret Answer (Hashed Securely)") },
                    placeholder = { Text("Enter recovery answer...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_secret_answer_input")
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pinText.length !in SettingsRepository.MIN_PIN_LENGTH..SettingsRepository.MAX_PIN_LENGTH || !pinText.all { it.isDigit() } ->
                            validationError = "PIN must be 4–6 numeric digits."
                        pinText != confirmPinText ->
                            validationError = "PIN code and confirmation do not match."
                        customAnswer.trim().length < 2 ->
                            validationError = "Please provide a secret recovery answer (min 2 chars)."
                        else -> {
                            onSavePinAndQuestion(pinText, selectedQuestion, customAnswer.trim())
                            Toast.makeText(context, "Security PIN & Recovery Question saved!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("Save PIN")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (currentSettings.hasSecurityPin) {
                    TextButton(
                        onClick = {
                            onClearPin()
                            Toast.makeText(context, "Security PIN removed", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    ) {
                        Text("Remove PIN", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * Biometric / PIN / Password / Recovery Authorization Prompt protecting sensitive wallet actions.
 */
@Composable
fun SecurityAuthModal(
    settings: AppSettings,
    actionTitle: String = "Authorize Action",
    actionDescription: String = "Verify your identity to proceed.",
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onVerifyPassword: (String) -> Boolean = { false },
    onVerifySecretAnswer: (String) -> Boolean
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val preferPassword = !settings.hasSecurityPin && settings.hasSecurityPassword

    var inputCredential by remember { mutableStateOf("") }
    var usePasswordMode by remember { mutableStateOf(preferPassword) }
    var isForgotMode by remember { mutableStateOf(false) }
    var secretAnswerInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }

    fun triggerBiometricPrompt() {
        if (activity != null) {
            val (supported, reason) = BiometricUtils.checkBiometricSupport(context)
            if (!supported) {
                Toast.makeText(context, reason, Toast.LENGTH_SHORT).show()
                return
            }
            BiometricUtils.showSystemBiometricPrompt(
                activity = activity,
                title = actionTitle,
                subtitle = actionDescription,
                negativeButtonText = if (usePasswordMode) "Use Password" else "Use PIN",
                onSuccess = {
                    onAuthenticated()
                },
                onFailedAttempt = {
                    authError = "Biometric not recognized. Try again."
                },
                onError = { err ->
                    authError = err
                },
                onCancel = {}
            )
        }
    }

    LaunchedEffect(Unit) {
        if (settings.isBiometricEnabled || AuthMethods.usesBiometric(settings.authMethod)) {
            triggerBiometricPrompt()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = EmeraldSuccess)
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionTitle, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = actionDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isForgotMode) {
                    if (settings.isBiometricEnabled || AuthMethods.usesBiometric(settings.authMethod)) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = IndigoPrimary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick { triggerBiometricPrompt() }
                                .testTag("biometric_quick_auth_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Fingerprint Sensor",
                                    tint = IndigoPrimary,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .pulseEffect()
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Scan Biometric to Authorize",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = IndigoPrimary
                                    )
                                    Text(
                                        text = "Uses Class 3 strong system biometrics",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (usePasswordMode) "— OR ENTER PASSWORD —" else "— OR ENTER PIN —",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = inputCredential,
                        onValueChange = { input ->
                            if (!usePasswordMode) {
                                if (input.length <= SettingsRepository.MAX_PIN_LENGTH && input.all { it.isDigit() }) {
                                    inputCredential = input
                                    authError = null
                                }
                            } else {
                                inputCredential = input
                                authError = null
                            }
                        },
                        label = {
                            Text(if (usePasswordMode) "Master Password" else "Security PIN (4–6 Digits)")
                        },
                        isError = authError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (usePasswordMode) KeyboardType.Password else KeyboardType.NumberPassword
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_pin_input")
                    )

                    if (authError != null) {
                        Text(
                            text = authError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (settings.hasSecurityPin && settings.hasSecurityPassword) {
                            TextButton(onClick = {
                                usePasswordMode = !usePasswordMode
                                inputCredential = ""
                                authError = null
                            }) {
                                Text(if (usePasswordMode) "Use PIN" else "Use Password", fontSize = 11.sp)
                            }
                        }

                        if (settings.securityQuestion.isNotBlank() && settings.hasSecurityAnswer) {
                            TextButton(
                                onClick = { isForgotMode = true; authError = null },
                                modifier = Modifier.testTag("forgot_pin_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Recovery Question", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "SECRET RECOVERY QUESTION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = settings.securityQuestion,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = secretAnswerInput,
                        onValueChange = { secretAnswerInput = it; authError = null },
                        label = { Text("Your Secret Answer") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secret_answer_recovery_input")
                    )

                    if (authError != null) {
                        Text(
                            text = authError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    }

                    TextButton(onClick = { isForgotMode = false; authError = null }) {
                        Text("Back to PIN / Password")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isForgotMode) {
                        val ok = if (usePasswordMode) onVerifyPassword(inputCredential) else onVerifyPin(inputCredential)
                        if (ok) {
                            onAuthenticated()
                        } else {
                            authError = if (usePasswordMode) "Incorrect password." else "Incorrect PIN code."
                        }
                    } else {
                        if (onVerifySecretAnswer(secretAnswerInput)) {
                            onAuthenticated()
                        } else {
                            authError = "Incorrect recovery answer."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                modifier = Modifier.testTag("confirm_auth_button")
            ) {
                Text(if (isForgotMode) "Verify & Proceed" else "Authorize")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
