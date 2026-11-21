package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.AuthMethods
import com.example.data.repository.SettingsRepository
import com.example.ui.theme.IndigoPrimary
import com.example.utils.BiometricUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSetupWizardDialog(
    onDismiss: () -> Unit,
    onCompleteSetup: (method: String, pin: String, password: String, question: String, answer: String) -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) }

    var selectedOption by remember { mutableStateOf(AuthMethods.FINGERPRINT_PIN) }
    var biometricErrorMessage by remember { mutableStateOf<String?>(null) }

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showSecret by remember { mutableStateOf(false) }

    val presetQuestions = listOf(
        "What was the name of your first pet?",
        "What city were you born in?",
        "What was your high school name?",
        "What is your mother's maiden name?",
        "What was your favorite childhood food?"
    )
    var question by remember { mutableStateOf(presetQuestions.first()) }
    var questionMenuExpanded by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 560.dp)
                .testTag("app_lock_wizard_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Wizard",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (step == 1) "App Lock Setup Wizard" else "Set Security Credentials",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (step == 1) "How would you like to protect your wallet?" else "Configure your backup unlock key and recovery question",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (step == 1) {
                    WizardOptionCard(
                        title = "Fingerprint + PIN (Recommended)",
                        description = "Strong biometrics backed by a 4–6 digit numeric PIN.",
                        icon = Icons.Default.Fingerprint,
                        isSelected = selectedOption == AuthMethods.FINGERPRINT_PIN,
                        badgeText = "RECOMMENDED",
                        onClick = {
                            selectedOption = AuthMethods.FINGERPRINT_PIN
                            val (supported, message) = BiometricUtils.checkBiometricSupport(context)
                            biometricErrorMessage = if (!supported) message else null
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    WizardOptionCard(
                        title = "Fingerprint + Password",
                        description = "Strong biometrics backed by an 8+ character master password.",
                        icon = Icons.Default.Fingerprint,
                        isSelected = selectedOption == AuthMethods.FINGERPRINT_PASSWORD,
                        onClick = {
                            selectedOption = AuthMethods.FINGERPRINT_PASSWORD
                            val (supported, message) = BiometricUtils.checkBiometricSupport(context)
                            biometricErrorMessage = if (!supported) message else null
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    WizardOptionCard(
                        title = "PIN Only",
                        description = "4 to 6-digit numeric passcode protection.",
                        icon = Icons.Default.Key,
                        isSelected = selectedOption == AuthMethods.PIN_ONLY,
                        onClick = {
                            selectedOption = AuthMethods.PIN_ONLY
                            biometricErrorMessage = null
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    WizardOptionCard(
                        title = "Master Password Only",
                        description = "Alphanumeric password (minimum 8 characters).",
                        icon = Icons.Default.Password,
                        isSelected = selectedOption == AuthMethods.PASSWORD_ONLY,
                        onClick = {
                            selectedOption = AuthMethods.PASSWORD_ONLY
                            biometricErrorMessage = null
                        }
                    )

                    if (biometricErrorMessage != null && AuthMethods.usesBiometric(selectedOption)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = biometricErrorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (AuthMethods.usesBiometric(selectedOption)) {
                                    val (supported, message) = BiometricUtils.checkBiometricSupport(context)
                                    if (!supported) {
                                        biometricErrorMessage = message
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                }
                                validationError = null
                                step = 2
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier.testTag("wizard_next_button")
                        ) {
                            Text("Continue", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    val needsPin = AuthMethods.usesPin(selectedOption)
                    val needsPassword = AuthMethods.usesPassword(selectedOption)

                    if (needsPin) {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { input ->
                                if (input.length <= SettingsRepository.MAX_PIN_LENGTH && input.all { it.isDigit() }) {
                                    pin = input
                                    validationError = null
                                }
                            },
                            label = { Text("Set 4–6 Digit Numeric PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showSecret = !showSecret }) {
                                    Icon(
                                        imageVector = if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle visibility"
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wizard_pin_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { input ->
                                if (input.length <= SettingsRepository.MAX_PIN_LENGTH && input.all { it.isDigit() }) {
                                    confirmPin = input
                                    validationError = null
                                }
                            },
                            label = { Text("Confirm Numeric PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wizard_confirm_pin_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (needsPassword) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                validationError = null
                            },
                            label = { Text("Set Master Password (min 8 chars)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showSecret = !showSecret }) {
                                    Icon(
                                        imageVector = if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle visibility"
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wizard_password_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                validationError = null
                            },
                            label = { Text("Confirm Master Password") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wizard_confirm_password_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = questionMenuExpanded,
                        onExpandedChange = { questionMenuExpanded = !questionMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = question,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Recovery Question") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = questionMenuExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = questionMenuExpanded,
                            onDismissRequest = { questionMenuExpanded = false }
                        ) {
                            presetQuestions.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q, fontSize = 12.sp) },
                                    onClick = {
                                        question = q
                                        questionMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = answer,
                        onValueChange = {
                            answer = it
                            validationError = null
                        },
                        label = { Text("Secret Recovery Answer *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_answer_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { step = 1; validationError = null }) {
                            Text("Back")
                        }

                        Button(
                            onClick = {
                                if (needsPin) {
                                    if (pin.length !in SettingsRepository.MIN_PIN_LENGTH..SettingsRepository.MAX_PIN_LENGTH || !pin.all { it.isDigit() }) {
                                        validationError = "PIN must be 4–6 numeric digits."
                                        return@Button
                                    }
                                    if (pin != confirmPin) {
                                        validationError = "PIN codes do not match."
                                        return@Button
                                    }
                                }

                                if (needsPassword) {
                                    if (password.length < SettingsRepository.MIN_PASSWORD_LENGTH) {
                                        validationError = "Master password must be at least ${SettingsRepository.MIN_PASSWORD_LENGTH} characters."
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        validationError = "Passwords do not match."
                                        return@Button
                                    }
                                }

                                if (answer.trim().length < 2) {
                                    validationError = "Please enter a secret recovery answer (min 2 characters)."
                                    return@Button
                                }

                                validationError = null
                                onCompleteSetup(
                                    selectedOption,
                                    if (needsPin) pin else "",
                                    if (needsPassword) password else "",
                                    question,
                                    answer.trim()
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier.testTag("wizard_finish_button")
                        ) {
                            Text("Enable App Lock", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) IndigoPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) BorderStroke(2.dp, IndigoPrimary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = IndigoPrimary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )

                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = IndigoPrimary
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
