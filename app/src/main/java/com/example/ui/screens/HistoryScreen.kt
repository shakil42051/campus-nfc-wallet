package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.UsageTransactionEntity
import com.example.data.model.NfcScanResult
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.NavigationTransitions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    cardList: List<CardEntity>,
    transactions: List<UsageTransactionEntity> = emptyList(),
    transactionSearchQuery: String = "",
    onTransactionSearchQueryChange: (String) -> Unit = {},
    onLogCustomUsage: (card: CardEntity, usageType: String, action: String, location: String) -> Unit = { _, _, _, _ -> },
    onDeleteTransaction: (Long) -> Unit = {},
    onClearAllTransactions: () -> Unit = {},
    onCardClick: (CardEntity) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedForCompare by remember { mutableStateOf(setOf<Long>()) }
    var isComparing by remember { mutableStateOf(false) }

    // Prune stale IDs if any compared card was deleted or archived
    LaunchedEffect(cardList) {
        val validIds = cardList.map { it.id }.toSet()
        val pruned = selectedForCompare.intersect(validIds)
        if (pruned != selectedForCompare) {
            selectedForCompare = pruned
        }
        if (pruned.size < 2 && isComparing) {
            isComparing = false
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Activity & Usage History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Track pass usage transactions, gate logs, and technology specs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Modern Segmented Control Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isTab0 = selectedTabIndex == 0
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTab0) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isTab0) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTabIndex = 0 }
                            .testTag("tab_usage_history")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = if (isTab0) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Card Usage (${transactions.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isTab0) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTab0) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    val isTab1 = selectedTabIndex == 1
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTab1) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isTab1) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTabIndex = 1 }
                            .testTag("tab_scans_compare")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = if (isTab1) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scans & Compare (${cardList.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isTab1) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTab1) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    NavigationTransitions.tabSlideTransition(targetState > initialState)
                },
                modifier = Modifier.fillMaxSize(),
                label = "HistoryTabAnimatedContent"
            ) { tabIndex ->
                if (tabIndex == 0) {
                    TransactionHistoryTab(
                        transactions = transactions,
                        cards = cardList,
                        searchQuery = transactionSearchQuery,
                        onSearchQueryChange = onTransactionSearchQueryChange,
                        onLogCustomUsage = onLogCustomUsage,
                        onDeleteTransaction = onDeleteTransaction,
                        onClearAll = onClearAllTransactions
                    )
                } else {
                    if (isComparing && selectedForCompare.size == 2) {
                        val ids = selectedForCompare.toList()
                        val cardA = cardList.find { it.id == ids[0] }
                        val cardB = cardList.find { it.id == ids[1] }

                        if (cardA != null && cardB != null) {
                            CardComparisonView(
                                cardA = cardA,
                                cardB = cardB,
                                onClose = { isComparing = false }
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (selectedForCompare.size == 2) {
                                Button(
                                    onClick = { isComparing = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("compare_cards_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "Compare")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Compare Selected 2 Cards Side-by-Side", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            } else if (selectedForCompare.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Select 1 more card to compare (Selected: ${selectedForCompare.size}/2)",
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (cardList.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "No History",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No scan history recorded yet.")
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(cardList, key = { it.id }) { card ->
                                        val isSelected = selectedForCompare.contains(card.id)

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateItem()
                                                .testTag("history_item_${card.id}"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) IndigoPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { checked ->
                                                        selectedForCompare = if (checked) {
                                                            if (selectedForCompare.size < 2) selectedForCompare + card.id else selectedForCompare
                                                        } else {
                                                            selectedForCompare - card.id
                                                        }
                                                    }
                                                )

                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(start = 8.dp)
                                                ) {
                                                    Text(
                                                        text = card.nickname,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "UID: ${card.uid} • ${card.manufacturer}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = dateFormat.format(Date(card.timestamp)),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 10.sp
                                                    )
                                                }

                                                OutlinedButton(
                                                    onClick = { onCardClick(card) }
                                                ) {
                                                    Text("View")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardComparisonView(
    cardA: CardEntity,
    cardB: CardEntity,
    onClose: () -> Unit
) {
    val scanA = remember(cardA) { NfcScanResult.fromJsonString(cardA.rawDataJson) }
    val scanB = remember(cardB) { NfcScanResult.fromJsonString(cardB.rawDataJson) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SIDE-BY-SIDE CARD COMPARISON",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IndigoPrimary
            )
            OutlinedButton(onClick = onClose) {
                Text("Close Comparison")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                CompareRow("Field", cardA.nickname, cardB.nickname, isHeader = true)
                CompareRow("Card UID", cardA.uid, cardB.uid)
                CompareRow("UID Length", "${scanA?.uidLengthBytes ?: 0} Bytes", "${scanB?.uidLengthBytes ?: 0} Bytes")
                CompareRow("Manufacturer", cardA.manufacturer, cardB.manufacturer)
                CompareRow("Primary Tech", cardA.technology, cardB.technology)
                CompareRow("ISO Standard", cardA.cardStandard, cardB.cardStandard)
                CompareRow("ATQA", scanA?.atqa ?: "N/A", scanB?.atqa ?: "N/A")
                CompareRow("SAK", scanA?.sak ?: "N/A", scanB?.sak ?: "N/A")
                CompareRow("Encryption", scanA?.securityAnalysis?.encryptionType ?: "N/A", scanB?.securityAnalysis?.encryptionType ?: "N/A")
                CompareRow("Clone Risk", scanA?.securityAnalysis?.cloneRiskLevel ?: "N/A", scanB?.securityAnalysis?.cloneRiskLevel ?: "N/A")
            }
        }
    }
}

@Composable
fun CompareRow(
    title: String,
    valA: String,
    valB: String,
    isHeader: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valA,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Medium,
            color = if (isHeader) IndigoPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = valB,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Medium,
            color = if (isHeader) EmeraldSuccess else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.2f)
        )
    }
}
