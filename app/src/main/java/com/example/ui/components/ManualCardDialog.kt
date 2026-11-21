package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CardEntity
import com.example.data.model.NfcScanResult
import com.example.data.model.SecurityAnalysis
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.animation.shakeEffect
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCardDialog(
    initialCard: CardEntity? = null,
    onDismiss: () -> Unit,
    onSaveCard: (scanResult: NfcScanResult, nickname: String, notes: String) -> Unit
) {
    val context = LocalContext.current

    val initialScan = remember(initialCard) {
        initialCard?.let { NfcScanResult.fromJsonString(it.rawDataJson) }
    }

    val initialMap = remember(initialScan, initialCard) {
        val map = mutableMapOf<String, String>()
        val dump = initialScan?.rawDataDumpHex ?: ""
        if (dump.contains("STUDENT_NAME:")) {
            dump.split("|").forEach { part ->
                val kv = part.split(":", limit = 2)
                if (kv.size == 2) map[kv[0].trim()] = kv[1].trim()
            }
        }
        map
    }

    var universityName by remember(initialCard) {
        mutableStateOf(initialMap["UNIV"] ?: initialCard?.manufacturer?.takeIf { it != "CAMPUS STUDENT PASS" } ?: "")
    }
    var studentName by remember(initialCard) {
        mutableStateOf(initialMap["STUDENT_NAME"] ?: "")
    }
    var program by remember(initialCard) { mutableStateOf(initialMap["DEPT"] ?: "") }
    var batch by remember(initialCard) { mutableStateOf(initialMap["BATCH"] ?: "") }
    var studentCode by remember(initialCard) {
        mutableStateOf(initialMap["CODE"] ?: initialCard?.uid ?: "")
    }
    var barcodeNumber by remember(initialCard) {
        mutableStateOf(initialMap["BARCODE"] ?: initialCard?.uid ?: "")
    }
    var bloodGroup by remember(initialCard) { mutableStateOf(initialMap["BLOOD"] ?: "") }
    var dob by remember(initialCard) { mutableStateOf(initialMap["DOB"] ?: "") }
    var nicknameInput by remember(initialCard) { mutableStateOf(initialCard?.nickname ?: "") }
    var notesInput by remember(initialCard) { mutableStateOf(initialCard?.notes ?: "") }

    var submittedAttempted by remember { mutableStateOf(false) }

    // Validation checks
    val isNicknameValid = nicknameInput.isNotBlank() || universityName.isNotBlank() || studentName.isNotBlank() || initialCard != null
    val isBarcodeValid = barcodeNumber.isBlank() || barcodeNumber.length >= 3

    val techOptions = listOf(
        "125 kHz Proximity / Optical Punch Barcode",
        "Barcode & Digital Student ID Pass",
        "MIFARE Classic 1K / 4K",
        "ISO/IEC 14443-3A (NTAG215/216)",
        "DESFire EV1 / EV2 Smart Card"
    )
    var selectedTech by remember(initialCard) {
        mutableStateOf(initialCard?.technology?.takeIf { it.isNotBlank() } ?: techOptions[0])
    }
    var techMenuExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .heightIn(max = 680.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = IndigoPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (initialCard != null) Icons.Default.Edit else Icons.Default.AddCard,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (initialCard != null) "Edit Card Details" else "Manual Card Entry",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (initialCard != null) "Update badge parameters & barcode" else "Create a digital pass with turnstile barcode",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_manual_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Section 1: Card Identification ---
                Text(
                    text = "CARD IDENTIFICATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { nicknameInput = it },
                    label = { Text("Card Nickname") },
                    placeholder = { Text("e.g. Campus Badge (2D:54)") },
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = IndigoPrimary) },
                    supportingText = {
                        if (submittedAttempted && !isNicknameValid) {
                            Text("Please enter a nickname or university name", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Label shown on your wallet cards", fontSize = 11.sp)
                        }
                    },
                    isError = submittedAttempted && !isNicknameValid,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_nickname_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = universityName,
                    onValueChange = { universityName = it },
                    label = { Text("University / Institution Name") },
                    placeholder = { Text("e.g. SOUTHEAST UNIVERSITY") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    supportingText = { Text("Header printed on the pass banner", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_university_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- Section 2: Student Profile Details ---
                Text(
                    text = "STUDENT PROFILE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    label = { Text("Student Full Name") },
                    placeholder = { Text("e.g. Alex Morgan") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = studentCode,
                        onValueChange = { studentCode = it },
                        label = { Text("Student Code / ID") },
                        placeholder = { Text("202400100200") },
                        leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("manual_code_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = program,
                        onValueChange = { program = it },
                        label = { Text("Program") },
                        placeholder = { Text("CSE") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("manual_program_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = batch,
                        onValueChange = { batch = it },
                        label = { Text("Batch") },
                        placeholder = { Text("65") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_batch_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = { bloodGroup = it },
                        label = { Text("Blood Group") },
                        placeholder = { Text("O+") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_blood_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth") },
                    placeholder = { Text("15 AUG 2004") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_dob_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- Section 3: Credentials & Technology ---
                Text(
                    text = "ACCESS & BARCODE CREDENTIALS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = barcodeNumber,
                    onValueChange = { barcodeNumber = it },
                    label = { Text("Barcode / UID Number") },
                    placeholder = { Text("e.g. 0C:D0:2D:54 or 0232330005101296") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = IndigoPrimary) },
                    supportingText = {
                        if (submittedAttempted && !isBarcodeValid) {
                            Text("Barcode UID should be at least 3 characters", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Renders an optical Code128 scanner barcode", fontSize = 11.sp)
                        }
                    },
                    isError = submittedAttempted && !isBarcodeValid,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_barcode_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = techMenuExpanded,
                    onExpandedChange = { techMenuExpanded = !techMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedTech,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Card Technology Standard") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = techMenuExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("manual_tech_select")
                    )
                    ExposedDropdownMenu(
                        expanded = techMenuExpanded,
                        onDismissRequest = { techMenuExpanded = false }
                    ) {
                        techOptions.forEach { tech ->
                            DropdownMenuItem(
                                text = { Text(tech, fontSize = 13.sp) },
                                onClick = {
                                    selectedTech = tech
                                    techMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes / Description (Optional)") },
                    placeholder = { Text("e.g. Optical turnstile punch badge") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_notes_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Hardware & Turnstile Note Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Barcode UID renders an optical Code128 scanner image on screen for turnstile gate punch readers.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Full-width Primary Button & Cancel Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            submittedAttempted = true
                            if (!isNicknameValid || !isBarcodeValid) {
                                Toast.makeText(context, "Please check form validation errors", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val rawUid = barcodeNumber.trim().ifBlank {
                                studentCode.trim().ifBlank {
                                val randomHex = (1..4).map { String.format(Locale.US, "%02X", (0..255).random()) }.joinToString(":")
                                    randomHex
                                }
                            }

                            val formattedUid = rawUid.uppercase()
                            val uniNameFinal = universityName.trim().ifBlank { "CAMPUS STUDENT PASS" }
                            val sNameFinal = studentName.trim().ifBlank { "Student Cardholder" }

                            val manualResult = NfcScanResult(
                                uid = formattedUid,
                                uidHex = formattedUid.replace(":", "").replace("-", ""),
                                uidAscii = formattedUid,
                                uidBinary = "01001100 01001111",
                                uidLengthBytes = formattedUid.length,
                                manufacturer = uniNameFinal,
                                primaryTechnology = selectedTech,
                                cardStandard = "Campus Student ID / Barcode Pass",
                                technologies = listOf(selectedTech, "Barcode Scanner", "Proximity ID"),
                                atqa = "0x0004",
                                sak = "0x08",
                                ats = "N/A",
                                historicalBytes = "STUDENT_ID_${studentCode.trim()}",
                                memorySizeBytes = "1024 Bytes (Standard)",
                                blockCount = "64",
                                pageCount = "16",
                                isNdefSupported = true,
                                isNdefWritable = false,
                                isNdefReadOnly = true,
                                ndefMaxSizeBytes = 1024,
                                ndefRecords = emptyList(),
                                memorySectors = emptyList(),
                                securityAnalysis = SecurityAnalysis(
                                    encryptionType = "Optical Barcode / Student Credentials",
                                    supportsCloning = true,
                                    cloneRiskLevel = "Low (Passcode & Barcode Protected)",
                                    explanation = "Student ID registered for campus turnstile optical & RFID punch entry.",
                                    hardwareSecurityFeatures = listOf("Optical Barcode Reader Support", "University Student ID Validation")
                                ),
                                rawDataDumpHex = "STUDENT_NAME:$sNameFinal|CODE:${studentCode.trim()}|DEPT:${program.trim()}|BATCH:${batch.trim()}|BLOOD:${bloodGroup.trim()}|DOB:${dob.trim()}|BARCODE:${formattedUid}|UNIV:$uniNameFinal"
                            )

                            val finalNickname = nicknameInput.trim().ifBlank { "$uniNameFinal Badge" }
                            onSaveCard(
                                manualResult,
                                finalNickname,
                                notesInput.trim()
                            )

                            Toast.makeText(
                                context,
                                if (initialCard != null) "Card updated successfully!" else "Card saved to wallet successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shakeEffect(submittedAttempted && (!isNicknameValid || !isBarcodeValid))
                            .testTag("save_manual_card_button")
                    ) {
                        Text(
                            text = if (initialCard != null) "Save Changes" else "Save Card to Wallet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cancel_manual_card_button")
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
}
