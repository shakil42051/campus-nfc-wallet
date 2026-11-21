package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardEntity
import com.example.data.model.NfcScanResult
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary

/**
 * Digital Pass Presenter & Usage Logger Modal.
 * Presents a real scannable ISO/IEC 15417 Code 128B optical barcode and lets the user
 * record manual gate/attendance/transit usage logs without falsely claiming hardware UID HCE cloning.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UseCardModal(
    activeCard: CardEntity,
    allCards: List<CardEntity>,
    onDismiss: () -> Unit,
    onSelectCard: (CardEntity) -> Unit,
    onLogUsage: (card: CardEntity, usageType: String, action: String, location: String) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val scanResult = remember(activeCard) { NfcScanResult.fromJsonString(activeCard.rawDataJson) }
    var cardSelectorExpanded by remember { mutableStateOf(false) }
    var showFullBrightnessPass by remember { mutableStateOf(false) }
    var lastLoggedSummary by remember { mutableStateOf<String?>(null) }

    val usageTypes = listOf("Access Control", "Attendance", "Transit Gate", "Library Check-in", "Manual Check-in")
    var selectedUsageType by remember { mutableStateOf(usageTypes.first()) }
    var usageDropdownExpanded by remember { mutableStateOf(false) }
    var locationInput by remember { mutableStateOf("Main Gate Turnstile") }

    val rawDump = scanResult?.rawDataDumpHex ?: ""
    val studentMap = remember(rawDump) {
        val map = mutableMapOf<String, String>()
        if (rawDump.contains("STUDENT_NAME:")) {
            rawDump.split("|").forEach { part ->
                val kv = part.split(":", limit = 2)
                if (kv.size == 2) map[kv[0].trim()] = kv[1].trim()
            }
        }
        map
    }
    val studentName = studentMap["STUDENT_NAME"].orEmpty().ifBlank { activeCard.nickname }
    val studentCode = studentMap["CODE"].orEmpty().ifBlank { activeCard.uid }
    val barcodeData = studentMap["BARCODE"].orEmpty().ifBlank { activeCard.uid }
    val uniName = studentMap["UNIV"].orEmpty().ifBlank { activeCard.manufacturer.ifBlank { "CAMPUS PASS" } }

    if (showFullBrightnessPass) {
        EntryPunchPassDialog(
            universityName = uniName,
            studentName = studentName,
            studentCode = studentCode,
            barcodeNumber = barcodeData,
            onDismiss = { showFullBrightnessPass = false }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("use_card_modal_dialog"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldSuccess.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DIGITAL PASS & USAGE LOG",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Optical Barcode & Check-In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_use_modal_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card Selector Dropdown
                if (allCards.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { cardSelectorExpanded = true }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = IndigoPrimary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "SELECTED PASS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = activeCard.nickname,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = IndigoPrimary.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Switch", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = cardSelectorExpanded,
                                onDismissRequest = { cardSelectorExpanded = false }
                            ) {
                                allCards.forEach { cardItem ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (cardItem.id == activeCard.id) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (cardItem.id == activeCard.id) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(cardItem.nickname, fontWeight = FontWeight.Bold)
                                                    Text("UID: ${cardItem.uid}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        },
                                        onClick = {
                                            onSelectCard(cardItem)
                                            cardSelectorExpanded = false
                                            lastLoggedSummary = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Visual Pass Card
                CardVisual(
                    nickname = activeCard.nickname,
                    uid = activeCard.uid,
                    manufacturer = activeCard.manufacturer,
                    technology = activeCard.technology,
                    cardStandard = activeCard.cardStandard,
                    isFavorite = activeCard.isFavorite
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Real ISO/IEC 15417 Code 128B Optical Barcode Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OPTICAL TURNSTILE BARCODE (CODE 128B)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        BarcodeVisualizer(
                            code = barcodeData,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showFullBrightnessPass = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Max-Brightness Fullscreen Pass", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Honest Hardware Limitation Notice
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Standard Android devices cannot emulate fixed hardware RFID UIDs (MIFARE / 125 kHz). Present the Code 128B barcode above to optical readers, or log your pass usage below.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Honest Manual Usage Logger
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LOG PASS ACTIVITY",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        ExposedDropdownMenuBox(
                            expanded = usageDropdownExpanded,
                            onExpandedChange = { usageDropdownExpanded = !usageDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedUsageType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Activity Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = usageDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = usageDropdownExpanded,
                                onDismissRequest = { usageDropdownExpanded = false }
                            ) {
                                usageTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            selectedUsageType = type
                                            usageDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = locationInput,
                            onValueChange = { locationInput = it },
                            label = { Text("Gate / Terminal Location") },
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val loc = locationInput.trim().ifBlank { "Campus Terminal" }
                                val actionDesc = "$selectedUsageType — ${activeCard.nickname}"
                                onLogUsage(activeCard, selectedUsageType, actionDesc, loc)
                                lastLoggedSummary = "Logged $selectedUsageType at $loc"
                                Toast.makeText(context, "Recorded usage for ${activeCard.nickname}", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("test_gate_reader_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Record Pass Usage", fontWeight = FontWeight.Bold)
                        }

                        AnimatedVisibility(
                            visible = lastLoggedSummary != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            lastLoggedSummary?.let { msg ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("gate_access_result_card")
                                ) {
                                    Text(
                                        text = msg,
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("done_using_card_button")
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
