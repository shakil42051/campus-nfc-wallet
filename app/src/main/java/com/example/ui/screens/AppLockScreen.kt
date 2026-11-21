package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.delay

/**
 * App Lock Screen supporting Strong Biometrics, 4–6 digit PIN, Master Password,
 * hashed secret recovery question, and persistent 5-attempt lockout protection.
 */
@Composable
fun AppLockScreen(
    settings: AppSettings,
    onVerifyPin: (String) -> Boolean,
    onVerifyPassword: (String) -> Boolean,
    onVerifySecretAnswer: (String) -> Boolean,
    onRecordFailedAttempt: () -> Pair<Int, Int>,
    onResetFailedAttempts: () -> Unit,
    getRemainingLockoutSeconds: () -> Int,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val authMethod = AuthMethods.normalize(settings.authMethod)

    fun defaultCredentialMode(): String = when {
        AuthMethods.usesPassword(authMethod) && settings.hasSecurityPassword -> "PASSWORD"
        settings.hasSecurityPin -> "PIN"
        settings.hasSecurityPassword -> "PASSWORD"
        else -> "PIN"
    }

    // View Modes: "BIOMETRIC", "PIN", "PASSWORD", "FORGOT_RECOVERY"
    var currentMode by remember(authMethod, settings.hasSecurityPin, settings.hasSecurityPassword) {
        val initialMode = when {
            AuthMethods.usesBiometric(authMethod) -> "BIOMETRIC"
            else -> defaultCredentialMode()
        }
        mutableStateOf(initialMode)
    }

    var pinInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var secretAnswerInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Persistent lockout synced with SettingsRepository (survives rotation & app restart)
    var lockoutSeconds by remember { mutableIntStateOf(getRemainingLockoutSeconds()) }

    fun handleSuccessfulUnlock() {
        onResetFailedAttempts()
        onUnlocked()
    }

    fun handleFailedAttempt() {
        val (attempts, lockSecs) = onRecordFailedAttempt()
        if (lockSecs > 0) {
            lockoutSeconds = lockSecs
            errorMessage = "Too many failed attempts! Locked for ${lockSecs}s."
        } else {
            val remaining = (SettingsRepository.MAX_FAILED_ATTEMPTS - attempts).coerceAtLeast(1)
            errorMessage = "Incorrect credentials! ($remaining attempts remaining)"
        }
    }

    fun launchBiometricPrompt() {
        if (activity == null) {
            currentMode = defaultCredentialMode()
            return
        }
        val (supported, reason) = BiometricUtils.checkBiometricSupport(context)
        if (!supported) {
            errorMessage = reason
            currentMode = defaultCredentialMode()
            return
        }
        val fallbackLabel = when {
            AuthMethods.usesPassword(authMethod) && settings.hasSecurityPassword -> "Use Password Instead"
            settings.hasSecurityPin -> "Use PIN Instead"
            settings.hasSecurityPassword -> "Use Password Instead"
            else -> "Cancel"
        }
        BiometricUtils.showSystemBiometricPrompt(
            activity = activity,
            title = "Verify your identity",
            subtitle = "Authenticate to unlock Campus NFC Wallet",
            negativeButtonText = fallbackLabel,
            onSuccess = {
                handleSuccessfulUnlock()
            },
            onFailedAttempt = {
                errorMessage = "Biometric not recognized. Please try again."
            },
            onError = { err ->
                errorMessage = err
                if (settings.hasAnyCredential) {
                    currentMode = defaultCredentialMode()
                }
            },
            onCancel = {
                if (settings.hasAnyCredential) {
                    currentMode = defaultCredentialMode()
                }
            }
        )
    }

    LaunchedEffect(currentMode, lockoutSeconds) {
        if (currentMode == "BIOMETRIC" && lockoutSeconds == 0) {
            launchBiometricPrompt()
        }
    }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = getRemainingLockoutSeconds()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .testTag("app_lock_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Shield Logo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (lockoutSeconds > 0) Icons.Default.LockClock else Icons.Default.Shield,
                        contentDescription = "Wallet Protection",
                        tint = if (lockoutSeconds > 0) MaterialTheme.colorScheme.error else IndigoPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Campus Wallet Guard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            lockoutSeconds > 0 -> "Security Lockout Active"
                            currentMode == "BIOMETRIC" -> "Use strong biometrics to continue"
                            currentMode == "PIN" -> "Enter 4–6 digit security PIN"
                            currentMode == "PASSWORD" -> "Enter account master password"
                            else -> "Verify secret recovery question"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                // Error / Lockout Banner
                if (lockoutSeconds > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.LockClock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Try again in ${lockoutSeconds}s",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (lockoutSeconds == 0) {
                    when (currentMode) {
                        "BIOMETRIC" -> {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = IndigoPrimary.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .bounceClick {
                                        launchBiometricPrompt()
                                    }
                                    .testTag("app_lock_biometric_trigger")
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Scan Fingerprint",
                                        tint = IndigoPrimary,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .pulseEffect()
                                    )
                                    Text(
                                        text = "Touch Phone Sensor to Unlock",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = IndigoPrimary
                                    )
                                    Text(
                                        text = "Tap to launch system biometric scanner",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                if (settings.hasSecurityPin) {
                                    TextButton(onClick = { currentMode = "PIN"; errorMessage = null }) {
                                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Use PIN", fontSize = 12.sp)
                                    }
                                }
                                if (settings.hasSecurityPassword) {
                                    TextButton(onClick = { currentMode = "PASSWORD"; errorMessage = null }) {
                                        Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Use Password", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        "PIN" -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(SettingsRepository.MAX_PIN_LENGTH) { index ->
                                    val isFilled = index < pinInput.length
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isFilled) IndigoPrimary else MaterialTheme.colorScheme.outlineVariant
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Keypad (every wrong submission is verified explicitly on submit to prevent brute-force bypass)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val keys = listOf(
                                    listOf("1", "2", "3"),
                                    listOf("4", "5", "6"),
                                    listOf("7", "8", "9"),
                                    listOf("C", "0", "DEL")
                                )
                                keys.forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        row.forEach { key ->
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        errorMessage = null
                                                        when (key) {
                                                            "DEL" -> if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                                            "C" -> pinInput = ""
                                                            else -> {
                                                                if (pinInput.length < SettingsRepository.MAX_PIN_LENGTH) {
                                                                    pinInput += key
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .testTag("app_lock_key_$key")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (key == "DEL") {
                                                        Icon(
                                                            Icons.AutoMirrored.Filled.Backspace,
                                                            contentDescription = "Delete",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = key,
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (onVerifyPin(pinInput)) {
                                        Toast.makeText(context, "PIN Verified!", Toast.LENGTH_SHORT).show()
                                        pinInput = ""
                                        handleSuccessfulUnlock()
                                    } else {
                                        handleFailedAttempt()
                                        pinInput = ""
                                    }
                                },
                                enabled = pinInput.length >= SettingsRepository.MIN_PIN_LENGTH && settings.hasSecurityPin,
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("app_lock_verify_pin_button")
                            ) {
                                Text("Unlock with PIN", fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (AuthMethods.usesBiometric(authMethod)) {
                                    TextButton(onClick = { currentMode = "BIOMETRIC"; errorMessage = null }) {
                                        Text("Try Fingerprint", fontSize = 12.sp)
                                    }
                                } else if (settings.hasSecurityPassword) {
                                    TextButton(onClick = { currentMode = "PASSWORD"; errorMessage = null }) {
                                        Text("Use Password", fontSize = 12.sp)
                                    }
                                }
                                TextButton(onClick = { currentMode = "FORGOT_RECOVERY"; errorMessage = null }) {
                                    Text("Forgot PIN?", fontSize = 12.sp)
                                }
                            }
                        }

                        "PASSWORD" -> {
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    errorMessage = null
                                },
                                label = { Text("Account Password") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("app_lock_password_input")
                            )

                            Button(
                                onClick = {
                                    if (onVerifyPassword(passwordInput)) {
                                        Toast.makeText(context, "Password Verified!", Toast.LENGTH_SHORT).show()
                                        passwordInput = ""
                                        handleSuccessfulUnlock()
                                    } else {
                                        handleFailedAttempt()
                                        passwordInput = ""
                                    }
                                },
                                enabled = passwordInput.length >= SettingsRepository.MIN_PASSWORD_LENGTH && settings.hasSecurityPassword,
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("app_lock_verify_password_button")
                            ) {
                                Text("Unlock with Password", fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (AuthMethods.usesBiometric(authMethod)) {
                                    TextButton(onClick = { currentMode = "BIOMETRIC"; errorMessage = null }) {
                                        Text("Try Fingerprint", fontSize = 12.sp)
                                    }
                                } else if (settings.hasSecurityPin) {
                                    TextButton(onClick = { currentMode = "PIN"; errorMessage = null }) {
                                        Text("Use PIN", fontSize = 12.sp)
                                    }
                                }
                                TextButton(onClick = { currentMode = "FORGOT_RECOVERY"; errorMessage = null }) {
                                    Text("Forgot Password?", fontSize = 12.sp)
                                }
                            }
                        }

                        "FORGOT_RECOVERY" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (settings.securityQuestion.isNotBlank() && settings.hasSecurityAnswer) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
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
                                        onValueChange = {
                                            secretAnswerInput = it
                                            errorMessage = null
                                        },
                                        label = { Text("Your Secret Answer") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("app_lock_secret_answer_input")
                                    )

                                    Button(
                                        onClick = {
                                            if (onVerifySecretAnswer(secretAnswerInput)) {
                                                Toast.makeText(
                                                    context,
                                                    "Recovery answer verified! Access granted.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                secretAnswerInput = ""
                                                handleSuccessfulUnlock()
                                            } else {
                                                handleFailedAttempt()
                                                secretAnswerInput = ""
                                            }
                                        },
                                        enabled = secretAnswerInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Text("Verify & Unlock", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        text = "No recovery question is configured. Use your PIN, Password, or Biometrics to unlock.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        currentMode = defaultCredentialMode()
                                        errorMessage = null
                                    },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Back to ${if (defaultCredentialMode() == "PASSWORD") "Password" else "PIN"}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
