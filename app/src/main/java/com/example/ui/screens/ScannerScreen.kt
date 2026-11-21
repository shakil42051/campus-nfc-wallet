package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BatchCardItem
import com.example.data.model.NfcScanResult
import com.example.nfc.rememberNfcHardwareState
import com.example.ui.components.CardVisual
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.HexViewer
import com.example.ui.components.ManualCardDialog
import com.example.ui.components.NfcDisabledPopupDialog
import com.example.ui.components.SecurityBadgeCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MotionEasing

@Composable
private fun TechDetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(0.38f)
                .padding(end = 8.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.62f)
        )
    }
}

@Composable
fun ScannerScreen(
    latestScan: NfcScanResult?,
    wasAutoSaved: Boolean = false,
    errorMessage: String?,
    isDeveloperMode: Boolean = false,
    isBatchMode: Boolean = false,
    batchQueue: List<BatchCardItem> = emptyList(),
    onToggleBatchMode: (Boolean) -> Unit = {},
    onSaveCard: (scanResult: NfcScanResult, nickname: String, notes: String) -> Unit = { _, _, _ -> },
    onResetScan: () -> Unit = {},
    onClearError: () -> Unit = {},
    onRemoveBatchItem: (String) -> Unit = {},
    onUpdateBatchNickname: (String, String) -> Unit = { _, _ -> },
    onClearBatchQueue: () -> Unit = {},
    onSaveBatchQueue: (onSuccess: (Int) -> Unit) -> Unit = {},
    onAddManualToBatch: (NfcScanResult, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val nfcState = rememberNfcHardwareState()
    val isNfcSupported = nfcState.isSupported
    val isNfcEnabled = nfcState.isEnabled

    var nicknameInput by remember(latestScan) {
        mutableStateOf(latestScan?.let { "Campus Badge (${it.uid.takeLast(5)})" } ?: "")
    }
    var notesInput by remember(latestScan) { mutableStateOf("") }
    var cardSavedState by remember(latestScan, wasAutoSaved) { mutableStateOf(wasAutoSaved) }
    var showManualDialog by remember { mutableStateOf(false) }
    var showNfcOffDialog by remember { mutableStateOf(false) }

    if (showNfcOffDialog) {
        NfcDisabledPopupDialog(onDismiss = { showNfcOffDialog = false })
    }

    if (showManualDialog) {
        ManualCardDialog(
            onDismiss = { showManualDialog = false },
            onSaveCard = { scanResult, nickname, notes ->
                if (isBatchMode) {
                    onAddManualToBatch(scanResult, nickname, notes)
                    Toast.makeText(context, "Added '$nickname' to batch queue!", Toast.LENGTH_SHORT).show()
                } else {
                    onSaveCard(scanResult, nickname, notes)
                    Toast.makeText(context, "Card saved to wallet database!", Toast.LENGTH_SHORT).show()
                }
                showManualDialog = false
            }
        )
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NFC Card Radar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Hold campus ID badge steady to back of device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Prominent Scan Error Banner (previously never displayed!)
            if (!errorMessage.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("scanner_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Scan Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        TextButton(onClick = onClearError) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Mode Selector Switch
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("scan_mode_selector")
            ) {
                Row(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Surface(
                        onClick = { onToggleBatchMode(false) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (!isBatchMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_single_scan")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = "Single Scan",
                                tint = if (!isBatchMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Single Scan",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (!isBatchMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { onToggleBatchMode(true) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isBatchMode) IndigoPrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_batch_scan")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Batch Mode",
                                tint = if (isBatchMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Batch Mode",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isBatchMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (batchQueue.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isBatchMode) EmeraldSuccess else IndigoPrimary
                                ) {
                                    Text(
                                        text = "${batchQueue.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual Input Shortcut Banner
            Card(
                onClick = { showManualDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_manual_entry_banner")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCard,
                            contentDescription = "Manual Input",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Can't scan your ID card? Add Manually",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Add student ID, badge number & scannable Code 128 barcode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showManualDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text("Add Now", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isBatchMode) {
                BatchScanningView(
                    batchQueue = batchQueue,
                    onOpenManualDialog = { showManualDialog = true },
                    onRemoveBatchItem = onRemoveBatchItem,
                    onUpdateBatchNickname = onUpdateBatchNickname,
                    onClearBatchQueue = onClearBatchQueue,
                    onSaveBatchQueue = onSaveBatchQueue
                )
            } else {
                if (!isNfcSupported) {
                    NfcWarningBanner(
                        title = "NFC Hardware Unsupported",
                        description = "Device lacks 13.56MHz NFC controller. Use the manual entry option above to add student cards & barcode passes."
                    )
                } else if (!isNfcEnabled) {
                    NfcWarningBanner(
                        title = "NFC Disabled in Settings",
                        description = "Your phone's NFC is currently turned OFF. Tap below to open system NFC Settings and turn it ON.",
                        onAction = { showNfcOffDialog = true }
                    )
                } else if (latestScan == null) {
                    NfcPulseRadar()

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = IndigoPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Why doesn't my student ID card scan automatically?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Many campus cards use 125kHz Low-Frequency RFID or optical barcodes for punch machines. Android phones only have 13.56MHz NFC chips. Tap 'Add Manually' above to create your digital ID pass & scannable Code 128 barcode!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    var isDetectedVisible by remember(latestScan) { mutableStateOf(false) }
                    LaunchedEffect(latestScan) {
                        isDetectedVisible = true
                    }

                    val detectedScale by animateFloatAsState(
                        targetValue = if (isDetectedVisible) 1.0f else 0.95f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "detectedScale"
                    )

                    val detectedAlpha by animateFloatAsState(
                        targetValue = if (isDetectedVisible) 1.0f else 0.0f,
                        animationSpec = tween(durationMillis = 350, easing = MotionEasing.Emphasized),
                        label = "detectedAlpha"
                    )

                    val detectedElevation by animateDpAsState(
                        targetValue = if (isDetectedVisible) 8.dp else 0.dp,
                        animationSpec = tween(durationMillis = 350, easing = MotionEasing.EmphasizedDecelerate),
                        label = "detectedElevation"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = detectedScale
                                scaleY = detectedScale
                                alpha = detectedAlpha
                            }
                            .testTag("scan_results_card"),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = detectedElevation),
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
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Scanned",
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "CARD DETECTED",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                }

                                OutlinedButton(onClick = onResetScan) {
                                    Text("Scan Another")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            CardVisual(
                                nickname = nicknameInput.ifBlank { "Campus Badge" },
                                uid = latestScan.uid,
                                manufacturer = latestScan.manufacturer,
                                technology = latestScan.primaryTechnology,
                                cardStandard = latestScan.cardStandard
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = nicknameInput,
                                onValueChange = { nicknameInput = it },
                                label = { Text("Assign Card Nickname") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("nickname_input"),
                                singleLine = true,
                                enabled = !cardSavedState
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = notesInput,
                                onValueChange = { notesInput = it },
                                label = { Text("Optional Notes (Gate, Room, Owner)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scan_notes_input"),
                                singleLine = true,
                                enabled = !cardSavedState
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onSaveCard(latestScan, nicknameInput, notesInput)
                                    cardSavedState = true
                                    Toast.makeText(context, "Card saved to wallet database!", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !cardSavedState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_card_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    when {
                                        wasAutoSaved -> "Auto-Saved to Wallet!"
                                        cardSavedState -> "Saved to Wallet!"
                                        else -> "Save Card to Wallet"
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Card Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TechDetailRow("UID", latestScan.uid)
                            TechDetailRow("Manufacturer", latestScan.manufacturer)
                            TechDetailRow("Standard", latestScan.cardStandard)
                            TechDetailRow("Supported Tech", latestScan.technologies.joinToString(", "))

                            if (isDeveloperMode) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Developer Hardware Diagnostics",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                TechDetailRow("UID Hex", latestScan.uidHex)
                                TechDetailRow("UID ASCII", latestScan.uidAscii)
                                TechDetailRow("UID Binary", latestScan.uidBinary)
                                TechDetailRow("ATQA", latestScan.atqa)
                                TechDetailRow("SAK", latestScan.sak)
                                TechDetailRow("ATS", latestScan.ats)
                                TechDetailRow("Historical Bytes", latestScan.historicalBytes)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SecurityBadgeCard(analysis = latestScan.securityAnalysis)

                            if (isDeveloperMode) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HexViewer(memorySectors = latestScan.memorySectors)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun NfcPulseRadar() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseRadar")

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Scale"
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Alpha"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Scale"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Alpha"
    )

    val iconPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPulse"
    )

    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .scale(scale1)
                    .background(IndigoPrimary.copy(alpha = alpha1), shape = CircleShape)
                    .border(1.5.dp, IndigoPrimary.copy(alpha = alpha1), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(170.dp)
                    .scale(scale2)
                    .background(IndigoPrimary.copy(alpha = alpha2), shape = CircleShape)
                    .border(1.5.dp, IndigoPrimary.copy(alpha = alpha2), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(IndigoPrimary.copy(alpha = 0.12f), shape = CircleShape)
                    .border(1.dp, IndigoPrimary.copy(alpha = 0.3f), CircleShape)
            )

            Surface(
                shape = CircleShape,
                color = IndigoPrimary,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(72.dp)
                    .scale(iconPulse)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = "NFC Searching",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = IndigoPrimary.copy(alpha = textAlpha),
                modifier = Modifier.size(8.dp)
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SEARCHING FOR 13.56MHz NFC TAGS...",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = IndigoPrimary.copy(alpha = textAlpha),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun NfcWarningBanner(
    title: String,
    description: String,
    onAction: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onAction) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open NFC Settings")
                }
            }
        }
    }
}

@Composable
fun BatchScanningView(
    batchQueue: List<BatchCardItem>,
    onOpenManualDialog: () -> Unit,
    onRemoveBatchItem: (String) -> Unit,
    onUpdateBatchNickname: (String, String) -> Unit,
    onClearBatchQueue: () -> Unit,
    onSaveBatchQueue: (onSuccess: (Int) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        DeleteConfirmationDialog(
            title = "Clear Batch Queue?",
            message = "Remove all ${batchQueue.size} unsaved cards from the batch queue?",
            onConfirm = {
                showClearConfirm = false
                onClearBatchQueue()
            },
            onDismiss = { showClearConfirm = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active Batch Radar Status Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, IndigoPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "QUICK BATCH SCANNING ACTIVE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IndigoPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap multiple ID badges against the phone in rapid succession. Every card will be added to your queue below without stopping.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onOpenManualDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_add_manual_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCard,
                        contentDescription = "Add Manual",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Card Manually to Queue", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Queue Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "QUEUED CARDS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = IndigoPrimary
                ) {
                    Text(
                        text = "${batchQueue.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (batchQueue.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.heightIn(min = 38.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Queue", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (batchQueue.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Batch Queue is Empty",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hold physical NFC cards near the back of your device or tap 'Add Card Manually to Queue' to queue multiple cards.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                batchQueue.forEachIndexed { index, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("batch_item_${item.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
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
                                        color = IndigoPrimary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = IndigoPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "UID: ${item.scanResult.uid}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${item.scanResult.manufacturer} • ${item.scanResult.primaryTechnology}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onRemoveBatchItem(item.id) },
                                    modifier = Modifier.testTag("remove_batch_item_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove from Queue",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = item.nickname,
                                onValueChange = { onUpdateBatchNickname(item.id, it) },
                                label = { Text("Card Nickname") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("batch_item_nickname_${item.id}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onSaveBatchQueue { count ->
                            Toast.makeText(
                                context,
                                "Successfully saved $count cards to your wallet!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_save_batch_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Confirm Batch")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirm & Save All (${batchQueue.size} Cards)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
