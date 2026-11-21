package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.UsageTransactionEntity
import com.example.data.model.NfcScanResult
import com.example.data.model.SecurityAnalysis
import com.example.ui.components.CardVisual
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.HexViewer
import com.example.ui.components.SecurityBadgeCard
import com.example.ui.components.StudentPassCard
import com.example.ui.components.UseCardModal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldChip
import com.example.ui.theme.IndigoPrimary
import com.example.utils.Exporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun TechDetailRow(label: String, value: String) {
    if (value.isBlank()) return
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable {
                clipboardManager.setText(AnnotatedString(value))
                Toast.makeText(context, "Copied $label: $value", Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 5.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(0.38f)
                .padding(end = 8.dp)
        )
        Row(
            modifier = Modifier.weight(0.62f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $label",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

private fun encodeUidToHexString(uidValue: String): String {
    val hexCandidate = uidValue.replace(":", "").replace(" ", "").trim()
    val isPureHex = hexCandidate.length >= 4 && hexCandidate.length % 2 == 0 && hexCandidate.all {
        it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
    }
    return if (isPureHex) {
        hexCandidate.uppercase(Locale.US)
    } else {
        uidValue.toByteArray(Charsets.UTF_8).joinToString("") { String.format(Locale.US, "%02X", it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    card: CardEntity,
    activeCardId: Long? = null,
    allCards: List<CardEntity> = emptyList(),
    cardTransactions: List<UsageTransactionEntity> = emptyList(),
    isDeveloperMode: Boolean = false,
    onSelectActiveCard: (CardEntity) -> Unit = {},
    onBackClick: () -> Unit,
    onToggleFavorite: (CardEntity) -> Unit,
    onToggleArchive: (CardEntity) -> Unit = {},
    onDeleteClick: (CardEntity) -> Unit,
    onUpdateCard: (existingCard: CardEntity, scanResult: NfcScanResult, nickname: String, notes: String) -> Unit = { _, _, _, _ -> },
    onLogUsage: (card: CardEntity, usageType: String, action: String, location: String) -> Unit = { _, _, _, _ -> },
    onBeforeExternalAction: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scanResult = remember(card) { NfcScanResult.fromJsonString(card.rawDataJson) }
    val txDateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    var exportMenuExpanded by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var showUseModal by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isActive = card.id == activeCardId

    // Parse student metadata if present
    val rawDump = scanResult?.rawDataDumpHex ?: ""
    val hadStudentMetadata = rawDump.contains("STUDENT_NAME:")
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

    // Editable State Variables for Inline Edit Mode
    var nicknameEdit by remember(card) { mutableStateOf(card.nickname) }
    var universityNameEdit by remember(card) {
        mutableStateOf(studentMap["UNIV"] ?: card.manufacturer.takeIf { it != "CAMPUS STUDENT PASS" } ?: "")
    }
    var studentNameEdit by remember(card) { mutableStateOf(studentMap["STUDENT_NAME"] ?: "") }
    var programEdit by remember(card) { mutableStateOf(studentMap["DEPT"] ?: "") }
    var batchEdit by remember(card) { mutableStateOf(studentMap["BATCH"] ?: "") }
    var studentCodeEdit by remember(card) { mutableStateOf(studentMap["CODE"] ?: card.uid) }
    var barcodeNumberEdit by remember(card) { mutableStateOf(studentMap["BARCODE"] ?: card.uid) }
    var bloodGroupEdit by remember(card) { mutableStateOf(studentMap["BLOOD"] ?: "") }
    var dobEdit by remember(card) { mutableStateOf(studentMap["DOB"] ?: "") }
    var selectedTechEdit by remember(card) { mutableStateOf(card.technology.ifBlank { "MIFARE Classic 1K / 4K" }) }
    var notesEdit by remember(card) { mutableStateOf(card.notes) }
    var techDropdownExpanded by remember { mutableStateOf(false) }

    val techOptions = listOf(
        "MIFARE Classic 1K / 4K",
        "125 kHz Proximity / Optical Punch Barcode",
        "ISO/IEC 14443-3A (NTAG215/216)",
        "DESFire EV1 / EV2 Smart Card",
        "Barcode & Digital Student ID Pass",
        "ISO 14443-4 IsoDep Smart Card"
    )

    val safeFileBase = remember(card.nickname) { Exporter.sanitizeFilename(card.nickname, "campus_card") }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = card.nickname,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "UID: ${card.uid}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isEditMode = !isEditMode },
                        modifier = Modifier.testTag("edit_card_button")
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "View Mode" else "Edit Card",
                            tint = if (isEditMode) EmeraldSuccess else MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { onToggleFavorite(card) }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = if (card.isFavorite) GoldChip else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onToggleArchive(card) },
                        modifier = Modifier.testTag("archive_card_button")
                    ) {
                        Icon(
                            imageVector = if (card.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (card.isArchived) "Restore Card" else "Archive Card",
                            tint = if (card.isArchived) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(onClick = { exportMenuExpanded = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Export")
                        }

                        DropdownMenu(
                            expanded = exportMenuExpanded,
                            onDismissRequest = { exportMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as JSON") },
                                onClick = {
                                    exportMenuExpanded = false
                                    scanResult?.let {
                                        onBeforeExternalAction()
                                        Exporter.shareTextFile(context, "$safeFileBase.json", Exporter.generateJson(it), "application/json")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as CSV") },
                                onClick = {
                                    exportMenuExpanded = false
                                    scanResult?.let {
                                        onBeforeExternalAction()
                                        Exporter.shareTextFile(context, "$safeFileBase.csv", Exporter.generateCsv(it), "text/csv")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as TXT Report") },
                                onClick = {
                                    exportMenuExpanded = false
                                    scanResult?.let {
                                        onBeforeExternalAction()
                                        Exporter.shareTextFile(context, "$safeFileBase.txt", Exporter.generateTxt(it), "text/plain")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Raw HEX Dump") },
                                onClick = {
                                    exportMenuExpanded = false
                                    scanResult?.let {
                                        onBeforeExternalAction()
                                        Exporter.shareTextFile(context, "${safeFileBase}_hex.txt", Exporter.generateHexDump(it), "text/plain")
                                    }
                                }
                            )
                        }
                    }

                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Card", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (showDeleteConfirm) {
            DeleteConfirmationDialog(
                title = "Delete Card?",
                message = "Are you sure you want to permanently delete this card? This action cannot be undone.",
                onConfirm = {
                    showDeleteConfirm = false
                    onDeleteClick(card)
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Screen Mode Switch Pill Bar
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("detail_mode_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Surface(
                            onClick = { isEditMode = false },
                            shape = RoundedCornerShape(20.dp),
                            color = if (!isEditMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_view_details")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "View Details",
                                    tint = if (!isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "View Details",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isEditMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = { isEditMode = true },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isEditMode) IndigoPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_edit_details")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Card Mode",
                                    tint = if (isEditMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Edit Card",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEditMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (card.isArchived) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "CARD IS ARCHIVED",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "This card is stored in your archive. Restore it to select as your active campus pass.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onToggleArchive(card) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (isEditMode) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_mode_panel"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = IndigoPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "EDIT CARD & PASS INFORMATION",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Modify card nickname, campus ID pass credentials, optical barcode number, and notes below. Hardware NFC payloads are preserved unless student pass metadata is edited.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Card Identity",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = nicknameEdit,
                                onValueChange = { nicknameEdit = it },
                                label = { Text("Card Nickname") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_nickname_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Student Pass Credentials (Optional)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = universityNameEdit,
                                onValueChange = { universityNameEdit = it },
                                label = { Text("University / Institution Name") },
                                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_university_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = studentNameEdit,
                                onValueChange = { studentNameEdit = it },
                                label = { Text("Student Name") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_student_name_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = studentCodeEdit,
                                    onValueChange = { studentCodeEdit = it },
                                    label = { Text("Student Code / ID") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("edit_code_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = programEdit,
                                    onValueChange = { programEdit = it },
                                    label = { Text("Program / Dept") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .testTag("edit_program_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = batchEdit,
                                    onValueChange = { batchEdit = it },
                                    label = { Text("Batch") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .testTag("edit_batch_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = barcodeNumberEdit,
                                    onValueChange = { barcodeNumberEdit = it },
                                    label = { Text("Optical Barcode UID") },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("edit_barcode_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = bloodGroupEdit,
                                    onValueChange = { bloodGroupEdit = it },
                                    label = { Text("Blood Group") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("edit_blood_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = dobEdit,
                                    onValueChange = { dobEdit = it },
                                    label = { Text("Date of Birth") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("edit_dob_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "NFC Hardware & Notes",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            ExposedDropdownMenuBox(
                                expanded = techDropdownExpanded,
                                onExpandedChange = { techDropdownExpanded = !techDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedTechEdit,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("NFC Hardware Standard") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = techDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                        .testTag("edit_tech_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = techDropdownExpanded,
                                    onDismissRequest = { techDropdownExpanded = false }
                                ) {
                                    techOptions.forEach { tech ->
                                        DropdownMenuItem(
                                            text = { Text(tech) },
                                            onClick = {
                                                selectedTechEdit = tech
                                                techDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = notesEdit,
                                onValueChange = { notesEdit = it },
                                label = { Text("Notes & Comments") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_notes_input")
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { isEditMode = false }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = {
                                        val barcodeTrimmed = barcodeNumberEdit.trim()
                                        val codeTrimmed = studentCodeEdit.trim()
                                        val uidValue = when {
                                            barcodeTrimmed.isNotBlank() && barcodeTrimmed != card.uid -> barcodeTrimmed
                                            codeTrimmed.isNotBlank() && codeTrimmed != card.uid -> codeTrimmed
                                            else -> card.uid
                                        }
                                        val uidChanged = uidValue != card.uid
                                        val hasStudentInput = studentNameEdit.isNotBlank() ||
                                            programEdit.isNotBlank() ||
                                            batchEdit.isNotBlank() ||
                                            bloodGroupEdit.isNotBlank() ||
                                            dobEdit.isNotBlank() ||
                                            (barcodeTrimmed.isNotBlank() && barcodeTrimmed != card.uid)

                                        val shouldWriteStudentDump = hadStudentMetadata || hasStudentInput
                                        val uniNameFinal = universityNameEdit.trim().ifBlank {
                                            scanResult?.manufacturer?.ifBlank { "CAMPUS STUDENT PASS" } ?: "CAMPUS STUDENT PASS"
                                        }
                                        val sNameFinal = studentNameEdit.trim().ifBlank { "Student Cardholder" }
                                        val studentDumpString =
                                            "STUDENT_NAME:$sNameFinal|CODE:$codeTrimmed|DEPT:${programEdit.trim()}|BATCH:${batchEdit.trim()}|BLOOD:${bloodGroupEdit.trim()}|DOB:${dobEdit.trim()}|BARCODE:$barcodeTrimmed|UNIV:$uniNameFinal"

                                        val updatedScan = if (scanResult != null) {
                                            scanResult.copy(
                                                uid = uidValue,
                                                uidHex = if (uidChanged) encodeUidToHexString(uidValue) else scanResult.uidHex,
                                                manufacturer = uniNameFinal,
                                                primaryTechnology = selectedTechEdit,
                                                rawDataDumpHex = if (shouldWriteStudentDump) studentDumpString else scanResult.rawDataDumpHex
                                            )
                                        } else {
                                            NfcScanResult(
                                                uid = uidValue,
                                                uidHex = encodeUidToHexString(uidValue),
                                                uidAscii = uidValue,
                                                uidBinary = uidValue.toByteArray(Charsets.UTF_8).take(8).joinToString(" ") {
                                                    String.format(Locale.US, "%8s", Integer.toBinaryString(it.toInt() and 0xFF)).replace(' ', '0')
                                                },
                                                uidLengthBytes = uidValue.toByteArray(Charsets.UTF_8).size,
                                                manufacturer = uniNameFinal,
                                                primaryTechnology = selectedTechEdit,
                                                cardStandard = "Campus Student ID / Barcode Pass",
                                                technologies = listOf(selectedTechEdit, "Barcode Scanner"),
                                                atqa = "N/A",
                                                sak = "N/A",
                                                ats = "N/A",
                                                historicalBytes = if (codeTrimmed.isNotBlank()) "STUDENT_ID_$codeTrimmed" else "N/A",
                                                memorySizeBytes = "N/A",
                                                blockCount = "N/A",
                                                pageCount = "N/A",
                                                isNdefSupported = false,
                                                isNdefWritable = false,
                                                isNdefReadOnly = true,
                                                ndefMaxSizeBytes = 0,
                                                ndefRecords = emptyList(),
                                                memorySectors = emptyList(),
                                                securityAnalysis = SecurityAnalysis(
                                                    encryptionType = "Optical Barcode / Digital ID Pass",
                                                    supportsCloning = false,
                                                    cloneRiskLevel = "Low (Optical Pass)",
                                                    explanation = "Student ID registered for campus turnstile optical punch entry.",
                                                    hardwareSecurityFeatures = listOf("Code 128B Optical Barcode")
                                                ),
                                                rawDataDumpHex = if (shouldWriteStudentDump) studentDumpString else ""
                                            )
                                        }

                                        onUpdateCard(
                                            card,
                                            updatedScan,
                                            nicknameEdit.trim().ifBlank { "$uniNameFinal Badge" },
                                            notesEdit.trim()
                                        )

                                        Toast.makeText(context, "Card updated successfully!", Toast.LENGTH_SHORT).show()
                                        isEditMode = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    modifier = Modifier.testTag("save_card_changes_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Changes")
                                }
                            }
                        }
                    }
                } else {
                    // View Mode
                    CardVisual(
                        nickname = card.nickname,
                        uid = card.uid,
                        manufacturer = card.manufacturer,
                        technology = card.technology,
                        cardStandard = card.cardStandard,
                        isFavorite = card.isFavorite,
                        onFavoriteToggle = { onToggleFavorite(card) },
                        statusBadgeText = when {
                            card.isArchived -> "ARCHIVED"
                            isActive -> "ACTIVE PASS"
                            else -> null
                        },
                        statusBadgeColor = if (card.isArchived) MaterialTheme.colorScheme.secondary else EmeraldSuccess
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Single Card Selection & Use Bar
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_active_card_bar"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) EmeraldSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isActive) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isActive) "ACTIVE CAMPUS PASS" else "INACTIVE IN WALLET",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isActive) "Ready for optical pass & usage logging" else "Tap 'Set Active' to select as default pass",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!isActive && !card.isArchived) {
                                    OutlinedButton(
                                        onClick = { onSelectActiveCard(card) },
                                        modifier = Modifier
                                            .heightIn(min = 40.dp)
                                            .testTag("set_active_button"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Set Active", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (!card.isArchived) {
                                    Button(
                                        onClick = {
                                            if (!isActive) onSelectActiveCard(card)
                                            showUseModal = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                        modifier = Modifier
                                            .heightIn(min = 40.dp)
                                            .testTag("use_card_now_button"),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Use Pass", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Card Notes Section (previously never rendered!)
                    if (card.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_notes_section"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Notes,
                                        contentDescription = null,
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "NOTES",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = IndigoPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = card.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // High-Contrast Technology Indicator Badge Component
                    HighContrastTechBadges(
                        technology = card.technology,
                        cardStandard = card.cardStandard,
                        manufacturer = card.manufacturer,
                        uid = card.uid,
                        scanResult = scanResult
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (scanResult != null) {
                        val isStudentCard = hadStudentMetadata ||
                            card.nickname.contains("University", ignoreCase = true) ||
                            card.nickname.contains("Badge", ignoreCase = true) ||
                            card.nickname.contains("Student", ignoreCase = true) ||
                            card.nickname.contains("Pass", ignoreCase = true)

                        if (isStudentCard) {
                            val uniName = studentMap["UNIV"]?.ifBlank { null } ?: card.manufacturer.ifBlank { "CAMPUS STUDENT PASS" }
                            val sName = studentMap["STUDENT_NAME"]?.ifBlank { null } ?: "Student Cardholder"
                            val dept = studentMap["DEPT"] ?: ""
                            val batchVal = studentMap["BATCH"] ?: ""
                            val codeVal = studentMap["CODE"]?.ifBlank { null } ?: card.uid
                            val barcodeVal = studentMap["BARCODE"]?.ifBlank { null } ?: card.uid
                            val blood = studentMap["BLOOD"] ?: ""
                            val dobVal = studentMap["DOB"] ?: ""

                            StudentPassCard(
                                universityName = uniName,
                                studentName = sName,
                                program = dept,
                                batch = batchVal,
                                studentCode = codeVal,
                                barcodeNumber = barcodeVal,
                                bloodGroup = blood,
                                dob = dobVal
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Quick UID Copy Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(scanResult.uid))
                                    Toast.makeText(context, "Card UID copied: ${scanResult.uid}", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Nfc,
                                        contentDescription = null,
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "NFC CARD UID / IDENTIFIER",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = scanResult.uid,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
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
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = IndigoPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy UID", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Per-Card Recent Usage Logs
                        if (cardTransactions.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_recent_transactions"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "RECENT CARD ACTIVITY (${cardTransactions.size})",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSuccess
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    cardTransactions.take(5).forEachIndexed { idx, tx ->
                                        if (idx > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tx.purposeOrAction,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${tx.terminalLocation.ifBlank { tx.usageType }} • ${txDateFormat.format(Date(tx.timestamp))}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = tx.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tx.status.equals("SUCCESS", ignoreCase = true)) EmeraldSuccess else IndigoPrimary
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Hardware Technical Specifications Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "HARDWARE TECHNICAL SPECIFICATIONS",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                TechDetailRow("Card UID", scanResult.uid)
                                TechDetailRow("Manufacturer", scanResult.manufacturer)
                                TechDetailRow("Card Standard", scanResult.cardStandard)
                                TechDetailRow("Memory Size", scanResult.memorySizeBytes)

                                if (isDeveloperMode) {
                                    TechDetailRow("UID Hex", scanResult.uidHex)
                                    TechDetailRow("UID ASCII", scanResult.uidAscii)
                                    TechDetailRow("UID Binary", scanResult.uidBinary)
                                    TechDetailRow("ATQA", scanResult.atqa)
                                    TechDetailRow("SAK", scanResult.sak)
                                    TechDetailRow("ATS", scanResult.ats)
                                    TechDetailRow("Historical Bytes", scanResult.historicalBytes)
                                    TechDetailRow("Block Count", scanResult.blockCount)
                                    TechDetailRow("Page Count", scanResult.pageCount)
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Enable Developer Mode in Settings to inspect raw ATQA/SAK registers, binary UID dumps, and sector hex memory.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Security & Vulnerability Analysis
                        SecurityBadgeCard(analysis = scanResult.securityAnalysis)

                        Spacer(modifier = Modifier.height(16.dp))

                        // NDEF Records if available
                        if (scanResult.isNdefSupported && scanResult.ndefRecords.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "NDEF RECORD INSPECTOR",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    scanResult.ndefRecords.forEachIndexed { idx, record ->
                                        Text(
                                            text = "Record #$idx [${record.tnf}] - Type: ${record.type}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Payload: ${record.payloadText}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isDeveloperMode) {
                                            Text(
                                                text = "Hex: ${record.payloadHex}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Sector & Memory Inspector (Developer Mode)
                        if (isDeveloperMode) {
                            HexViewer(memorySectors = scanResult.memorySectors)
                        }
                    } else {
                        Text(
                            text = "Unable to decode stored card metadata.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }

        if (showUseModal) {
            Dialog(
                onDismissRequest = { showUseModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                UseCardModal(
                    activeCard = card,
                    allCards = allCards.ifEmpty { listOf(card) },
                    onDismiss = { showUseModal = false },
                    onSelectCard = { onSelectActiveCard(it) },
                    onLogUsage = onLogUsage
                )
            }
        }
    }
}

/**
 * High-Contrast Visual Indicator Component for NFC Tag Technologies
 */
@Composable
fun HighContrastTechBadges(
    technology: String,
    cardStandard: String,
    manufacturer: String,
    uid: String,
    scanResult: NfcScanResult?
) {
    val techLower = technology.lowercase(Locale.US)
    val stdLower = cardStandard.lowercase(Locale.US)

    val isMifare = techLower.contains("mifare")
    val isDesfire = techLower.contains("desfire")
    val isNtag = techLower.contains("ntag") || stdLower.contains("ntag")
    val is125k = techLower.contains("125") || stdLower.contains("125") || techLower.contains("proximity") || techLower.contains("barcode")
    val isIsoDep = techLower.contains("isodep") || stdLower.contains("14443-4")

    val (bgColor, accentColor, iconVector, techTitle, freqTag, descNote) = when {
        isDesfire -> Tuple6(
            Color(0xFF311B92),
            Color(0xFFD1C4E9),
            Icons.Default.Lock,
            "DESFire EV1 / EV2 / EV3 Smart Card",
            "13.56 MHz HF (AES-128 Crypto)",
            "High-security smart card with hardware AES encryption and multi-application memory vaults."
        )
        isMifare -> Tuple6(
            Color(0xFF0D47A1),
            Color(0xFFBBDEFB),
            Icons.Default.Memory,
            "MIFARE Classic 1K / 4K Technology",
            "13.56 MHz HF (ISO 14443-A)",
            "Industry standard 13.56MHz contactless memory card with Crypto-1 sector key protection."
        )
        isNtag -> Tuple6(
            Color(0xFF004D40),
            Color(0xFFB2DFDB),
            Icons.Default.Nfc,
            "NTAG213 / NTAG215 / NTAG216",
            "13.56 MHz HF (NFC Forum Type 2)",
            "High-speed NDEF tag standard compatible with standard Android / iOS smartphone readers."
        )
        is125k -> Tuple6(
            Color(0xFF4A148C),
            Color(0xFFE1BEE7),
            Icons.Default.QrCode,
            "125 kHz Proximity Pass / Optical Barcode",
            "125 kHz LF / Optical Pass",
            "Campus punch badge optimized for optical turnstile barcode readers and low-frequency entry."
        )
        isIsoDep -> Tuple6(
            Color(0xFF006064),
            Color(0xFFB2EBF2),
            Icons.Default.Security,
            "ISO 14443-4 IsoDep Smart Card",
            "13.56 MHz HF (APDU Protocol)",
            "Complies with high-security smart card APDU instruction sets and cryptographic handshakes."
        )
        else -> Tuple6(
            Color(0xFF1F2937),
            Color(0xFFE5E7EB),
            Icons.Default.Bolt,
            technology.ifBlank { "ISO/IEC 14443-A RFID Tag" },
            "13.56 MHz High Frequency",
            "Standard high-frequency contactless identity token."
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tech_indicator_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
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
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(
                            modifier = Modifier.padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = techTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = freqTag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HighContrastPillBadge(
                    text = if (is125k) "125 kHz LF" else "13.56 MHz HF",
                    badgeColor = Color.White.copy(alpha = 0.25f),
                    textColor = Color.White
                )

                HighContrastPillBadge(
                    text = manufacturer.ifBlank { "Unknown" },
                    badgeColor = Color.White.copy(alpha = 0.25f),
                    textColor = Color.White
                )

                scanResult?.let {
                    HighContrastPillBadge(
                        text = "${it.uidLengthBytes}B UID",
                        badgeColor = accentColor,
                        textColor = bgColor
                    )
                }

                if (scanResult?.isNdefSupported == true) {
                    HighContrastPillBadge(
                        text = "NDEF Ready",
                        badgeColor = EmeraldSuccess,
                        textColor = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = descNote,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun HighContrastPillBadge(
    text: String,
    badgeColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = badgeColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

data class Tuple6<A, B, C, D, E, F>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F
)
