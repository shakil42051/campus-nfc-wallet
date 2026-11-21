package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.UsageTransactionEntity
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.UseCardModal
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldChip
import com.example.ui.theme.IndigoPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryTab(
    transactions: List<UsageTransactionEntity>,
    cards: List<CardEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onLogCustomUsage: (card: CardEntity, usageType: String, action: String, location: String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var sortAscending by remember { mutableStateOf(false) } // false = Newest First
    var showLogModal by remember { mutableStateOf(false) }
    var selectedModalCard by remember(cards) { mutableStateOf(cards.firstOrNull()) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var txToDeleteId by remember { mutableStateOf<Long?>(null) }

    val filterCategories = listOf(
        FilterCategory("ALL", "All", Icons.AutoMirrored.Filled.ReceiptLong),
        FilterCategory("NFC", "NFC", Icons.Default.Nfc),
        FilterCategory("QR", "QR / Barcode", Icons.Default.QrCode),
        FilterCategory("STUDENT", "Student ID", Icons.Default.Badge),
        FilterCategory("MANUAL", "Manual", Icons.Default.EditNote),
        FilterCategory("ACCESS", "Access", Icons.Default.LockOpen),
        FilterCategory("TRANSIT", "Transit", Icons.Default.DirectionsBus),
        FilterCategory("PAYMENT", "Payment", Icons.Default.Payment)
    )

    // Search query is already applied at Room SQL level; apply type category filter and sort order here
    val filteredList = remember(transactions, selectedTypeFilter, sortAscending) {
        val filtered = transactions.filter { tx ->
            when (selectedTypeFilter) {
                "ALL" -> true
                "NFC" -> tx.usageType.contains("NFC", ignoreCase = true) || tx.purposeOrAction.contains("NFC", ignoreCase = true)
                "QR" -> tx.usageType.contains("QR", ignoreCase = true) || tx.purposeOrAction.contains("Barcode", ignoreCase = true)
                "STUDENT" -> tx.cardNickname.contains("Student", ignoreCase = true) || tx.usageType.contains("Attendance", ignoreCase = true)
                "MANUAL" -> tx.usageType.contains("Manual", ignoreCase = true) || tx.terminalLocation.contains("Manual", ignoreCase = true)
                "ACCESS" -> tx.usageType.contains("Access", ignoreCase = true)
                "TRANSIT" -> tx.usageType.contains("Transit", ignoreCase = true)
                "PAYMENT" -> tx.usageType.contains("Payment", ignoreCase = true)
                else -> true
            }
        }

        if (sortAscending) {
            filtered.sortedBy { it.timestamp }
        } else {
            filtered.sortedByDescending { it.timestamp }
        }
    }

    val groupedTransactions = remember(filteredList) {
        filteredList.groupBy { tx -> getGroupDateHeader(tx.timestamp) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- TOP TOOLBAR & SEARCH ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search logs, UID, location...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("transaction_search_input"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (cards.isNotEmpty()) {
                Button(
                    onClick = {
                        selectedModalCard = cards.firstOrNull()
                        showLogModal = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("simulate_usage_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log Usage", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- SECONDARY ACTION ROW: Count Badge, Reset Filters, Sort, Clear ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = IndigoPrimary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${filteredList.size} Records Found",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (searchQuery.isNotBlank() || selectedTypeFilter != "ALL") {
                    TextButton(
                        onClick = {
                            onSearchQueryChange("")
                            selectedTypeFilter = "ALL"
                        }
                    ) {
                        Text("Reset", fontSize = 12.sp)
                    }
                }

                IconButton(
                    onClick = { sortAscending = !sortAscending },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("tx_sort_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = if (sortAscending) "Oldest First" else "Newest First",
                        tint = if (sortAscending) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (transactions.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear All Logs",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // --- FILTER CHIPS ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filterCategories.forEach { category ->
                val isSelected = selectedTypeFilter == category.id
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTypeFilter = category.id },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(category.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("tx_filter_${category.id}")
                )
            }
        }

        // --- LIST OF TRANSACTIONS / EMPTY STATE ---
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = "No Usage Logs",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (transactions.isEmpty()) "No activity history recorded yet." else "No matching transactions found.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Use your pass or tap 'Add Log' to record gate and card activities.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedTransactions.forEach { (dateHeader, itemsInGroup) ->
                    item(key = "header_$dateHeader") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader.uppercase(Locale.US),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "${itemsInGroup.size} entries",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    items(itemsInGroup, key = { it.id }) { tx ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        txToDeleteId = tx.id
                                        false
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        val associatedCard = cards.find { it.id == tx.cardId }
                                        if (associatedCard != null) {
                                            onLogCustomUsage(associatedCard, tx.usageType, tx.purposeOrAction, tx.terminalLocation)
                                            Toast.makeText(context, "Repeated activity: ${tx.purposeOrAction}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Original card no longer exists in vault.", Toast.LENGTH_SHORT).show()
                                        }
                                        false
                                    }
                                    else -> false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            backgroundContent = {
                                val direction = dismissState.dismissDirection
                                val isDeleting = direction == SwipeToDismissBoxValue.EndToStart
                                val isRepeating = direction == SwipeToDismissBoxValue.StartToEnd
                                val bgColor = when {
                                    isDeleting -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                                    isRepeating -> EmeraldSuccess.copy(alpha = 0.9f)
                                    else -> Color.Transparent
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(bgColor)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = if (isDeleting) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    if (isDeleting) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Delete",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Record",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else if (isRepeating) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Replay,
                                                contentDescription = "Repeat Activity",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = "Repeat Log",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            TransactionItemCard(
                                transaction = tx,
                                dateFormat = dateFormat,
                                onDelete = { txToDeleteId = tx.id }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog to Log a Custom Pass Transaction (card selector works properly!)
    val modalCard = selectedModalCard ?: cards.firstOrNull()
    if (showLogModal && modalCard != null) {
        Dialog(
            onDismissRequest = { showLogModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            UseCardModal(
                activeCard = modalCard,
                allCards = cards,
                onDismiss = { showLogModal = false },
                onSelectCard = { picked -> selectedModalCard = picked },
                onLogUsage = { card, type, action, location ->
                    onLogCustomUsage(card, type, action, location)
                    Toast.makeText(context, "Logged activity for ${card.nickname}", Toast.LENGTH_SHORT).show()
                    showLogModal = false
                }
            )
        }
    }

    if (txToDeleteId != null) {
        DeleteConfirmationDialog(
            title = "Delete History Record?",
            message = "Are you sure you want to permanently delete this activity log item? This action cannot be undone.",
            onConfirm = {
                onDeleteTransaction(txToDeleteId!!)
                txToDeleteId = null
            },
            onDismiss = { txToDeleteId = null }
        )
    }

    if (showClearConfirm) {
        DeleteConfirmationDialog(
            title = "Clear All Usage Records?",
            message = "Are you sure you want to permanently delete all history logs for all cards? This action cannot be undone.",
            onConfirm = {
                onClearAll()
                showClearConfirm = false
                Toast.makeText(context, "All usage transaction logs cleared successfully", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}

private data class FilterCategory(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private fun getGroupDateHeader(timestamp: Long): String {
    val now = Calendar.getInstance()
    val itemTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isSameDay = now.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == itemTime.get(Calendar.DAY_OF_YEAR)
    if (isSameDay) return "Today"

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == itemTime.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == itemTime.get(Calendar.DAY_OF_YEAR)
    if (isYesterday) return "Yesterday"

    return "Older Activity"
}

@Composable
fun TransactionItemCard(
    transaction: UsageTransactionEntity,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    val typeIcon = when {
        transaction.usageType.contains("Access", ignoreCase = true) -> Icons.Default.LockOpen
        transaction.usageType.contains("Transit", ignoreCase = true) -> Icons.Default.DirectionsBus
        transaction.usageType.contains("Attendance", ignoreCase = true) -> Icons.Default.School
        transaction.usageType.contains("Payment", ignoreCase = true) -> Icons.Default.Payment
        transaction.usageType.contains("QR", ignoreCase = true) || transaction.purposeOrAction.contains("Barcode", ignoreCase = true) -> Icons.Default.QrCode
        transaction.usageType.contains("Student", ignoreCase = true) -> Icons.Default.Badge
        transaction.usageType.contains("Manual", ignoreCase = true) -> Icons.Default.EditNote
        else -> Icons.Default.Nfc
    }

    val typeColor = when {
        transaction.usageType.contains("Access", ignoreCase = true) -> EmeraldSuccess
        transaction.usageType.contains("Transit", ignoreCase = true) -> IndigoPrimary
        transaction.usageType.contains("Attendance", ignoreCase = true) -> Color(0xFF8E24AA)
        transaction.usageType.contains("Payment", ignoreCase = true) -> Color(0xFFF57C00)
        else -> MaterialTheme.colorScheme.secondary
    }

    val statusUpper = transaction.status.uppercase(Locale.US)
    val (statusColor, statusIcon) = when {
        statusUpper in listOf("SUCCESS", "EMULATED", "SYNCED", "VERIFIED") -> EmeraldSuccess to Icons.Default.CheckCircle
        statusUpper in listOf("PENDING", "QUEUED") -> GoldChip to Icons.Default.Schedule
        statusUpper in listOf("DENIED", "FAILED", "ERROR") -> CrimsonAlert to Icons.Default.Error
        else -> IndigoPrimary to Icons.Default.CheckCircle
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_item_${transaction.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = typeColor.copy(alpha = 0.12f)
                ) {
                    Box(
                        modifier = Modifier.size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = transaction.usageType,
                            tint = typeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.purposeOrAction,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${transaction.cardNickname} • UID: ${transaction.cardUid}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = IndigoPrimary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = transaction.status,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("delete_tx_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Log",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat.format(Date(transaction.timestamp)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                if (transaction.terminalLocation.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = transaction.terminalLocation,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
