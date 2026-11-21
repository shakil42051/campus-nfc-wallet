package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CardEntity
import com.example.data.model.NfcScanResult
import com.example.ui.animation.bounceClick
import com.example.ui.components.CardVisual
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.ManualCardDialog
import com.example.ui.components.UseCardModal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldChip
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.staggeredEntrance
import com.example.ui.viewmodel.CardFilter
import com.example.ui.viewmodel.DashboardStats
import com.example.ui.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    cardList: List<CardEntity>,
    activeCardsForSwitcher: List<CardEntity> = cardList.filterNot { it.isArchived },
    activeCard: CardEntity?,
    stats: DashboardStats,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: CardFilter,
    onFilterSelect: (CardFilter) -> Unit,
    selectedSortOrder: SortOrder,
    onSortSelect: (SortOrder) -> Unit,
    onCardClick: (CardEntity) -> Unit,
    onSelectActiveCard: (CardEntity) -> Unit,
    onToggleFavorite: (CardEntity) -> Unit,
    onToggleArchive: (CardEntity) -> Unit = {},
    onDeleteCard: (CardEntity) -> Unit,
    onScanFabClick: () -> Unit,
    isServerLinked: Boolean = false,
    serverHostLabel: String = "",
    isWalletSyncing: Boolean = false,
    onSyncOrLinkServerClick: () -> Unit = {},
    onSaveManualCard: (scanResult: NfcScanResult, nickname: String, notes: String) -> Unit = { _, _, _ -> },
    onLogUsage: (card: CardEntity, usageType: String, action: String, location: String) -> Unit = { _, _, _, _ -> }
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var showUseModal by remember { mutableStateOf(false) }
    var cardSelectorMenuExpanded by remember { mutableStateOf(false) }
    var pendingDeleteCard by remember { mutableStateOf<CardEntity?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "active_card_pulse")
    val activePulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "active_pulse_scale"
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onScanFabClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("scan_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Nfc, contentDescription = "Scan NFC Card")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Scan Card", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dashboard Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(0),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Campus NFC Wallet",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "NFC Card Analyzer & Secure Vault",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val pillColor = if (isServerLinked) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            Surface(
                                onClick = { if (!isWalletSyncing) onSyncOrLinkServerClick() },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isServerLinked) EmeraldSuccess.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, pillColor.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .testTag("dashboard_server_sync_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isWalletSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = IndigoPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = when {
                                                isServerLinked -> Icons.Default.Sync
                                                serverHostLabel.isNotBlank() -> Icons.Default.CloudOff
                                                else -> Icons.Default.CloudDone
                                            },
                                            contentDescription = "Server Sync Status",
                                            tint = pillColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = when {
                                            isWalletSyncing -> "Syncing..."
                                            isServerLinked -> "Sync Server"
                                            serverHostLabel.isNotBlank() -> "Offline"
                                            else -> "Link Server"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pillColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = { showManualDialog = true },
                                modifier = Modifier.testTag("dashboard_add_manual_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCard,
                                    contentDescription = "Add Student Card Manually",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Active Selected Card Banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(1)
                            .testTag("active_card_banner"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (activeCard != null) EmeraldSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                        )
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
                                        color = if (activeCard != null) EmeraldSuccess else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.scale(if (activeCard != null) activePulseScale else 1.0f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (activeCard != null) "ACTIVE CAMPUS PASS" else "NO PASS SELECTED",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeCard != null) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (activeCardsForSwitcher.size > 1) {
                                    Box {
                                        OutlinedButton(
                                            onClick = { cardSelectorMenuExpanded = true },
                                            modifier = Modifier
                                                .heightIn(min = 40.dp)
                                                .testTag("banner_switch_card_button"),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Switch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        DropdownMenu(
                                            expanded = cardSelectorMenuExpanded,
                                            onDismissRequest = { cardSelectorMenuExpanded = false }
                                        ) {
                                            Text(
                                                text = "Select Active Pass:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            activeCardsForSwitcher.forEach { item ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = if (item.id == activeCard?.id) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                                contentDescription = null,
                                                                tint = if (item.id == activeCard?.id) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Column {
                                                                Text(item.nickname, fontWeight = FontWeight.Bold)
                                                                Text(
                                                                    "UID: ${item.uid}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontFamily = FontFamily.Monospace
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        onSelectActiveCard(item)
                                                        cardSelectorMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (activeCard != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = activeCard.nickname,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "UID: ${activeCard.uid} • ${activeCard.technology}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { showUseModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                        modifier = Modifier
                                            .heightIn(min = 40.dp)
                                            .testTag("use_active_card_button")
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("USE PASS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Text(
                                    text = "No saved cards in wallet yet. Scan an NFC tag or add a manual student card to select and present it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Quick Stats Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMiniCard(
                            title = "Saved Cards",
                            value = "${stats.totalCards}",
                            icon = Icons.Default.CreditCard,
                            iconTint = IndigoPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            title = "DESFire",
                            value = "${stats.desfireCount}",
                            icon = Icons.Default.Security,
                            iconTint = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            title = "Favorites",
                            value = "${stats.totalFavorites}",
                            icon = Icons.Default.Star,
                            iconTint = GoldChip,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Search Bar & Sort Dropdown
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_text_field"),
                            placeholder = { Text("Search by Nickname, UID, Chip...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box {
                            IconButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier.testTag("sort_menu_button")
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "Sort Cards")
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Newest Scans First") },
                                    onClick = {
                                        onSortSelect(SortOrder.NEWEST)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Oldest Scans First") },
                                    onClick = {
                                        onSortSelect(SortOrder.OLDEST)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Card Name") },
                                    onClick = {
                                        onSortSelect(SortOrder.NAME)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Manufacturer") },
                                    onClick = {
                                        onSortSelect(SortOrder.MANUFACTURER)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Filter Chips Row
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CardFilter.entries) { filter ->
                            val labelText = when (filter) {
                                CardFilter.ALL -> "Active (${stats.totalCards})"
                                CardFilter.FAVORITES -> "Favorites (${stats.totalFavorites})"
                                CardFilter.ARCHIVED -> "Archived (${stats.totalArchived})"
                                else -> filter.name
                            }
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { onFilterSelect(filter) },
                                label = { Text(labelText) },
                                leadingIcon = if (filter == CardFilter.ARCHIVED) {
                                    { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.testTag("filter_chip_${filter.name}")
                            )
                        }
                    }
                }

                if (selectedFilter == CardFilter.ARCHIVED) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Archived Cards Vault",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Archived cards are hidden from active pass selection and quick digital pass presentation.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Cards List or Empty State
                if (cardList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = "No cards",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No cards match your query" else "No Campus NFC Cards Saved",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap the 'Scan Card' button and bring your university ID badge near the back of the phone to read technical data.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = cardList,
                        key = { it.id }
                    ) { card ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        pendingDeleteCard = card
                                        false
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        onToggleArchive(card)
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
                                val isArchiving = direction == SwipeToDismissBoxValue.StartToEnd
                                val bgColor = when {
                                    isDeleting -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                                    isArchiving -> if (card.isArchived) EmeraldSuccess.copy(alpha = 0.9f) else IndigoPrimary.copy(alpha = 0.9f)
                                    else -> Color.Transparent
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(bgColor)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = if (isDeleting) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    if (isDeleting) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Delete Pass",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    } else if (isArchiving) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (card.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                                contentDescription = "Toggle Archive",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = if (card.isArchived) "Unarchive Pass" else "Archive Pass",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            val isActive = card.id == activeCard?.id
                            val badgeText = when {
                                card.isArchived -> "ARCHIVED"
                                isActive -> "ACTIVE PASS"
                                else -> null
                            }
                            val badgeColor = if (card.isArchived) MaterialTheme.colorScheme.secondary else EmeraldSuccess

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_container_${card.id}"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    CardVisual(
                                        nickname = card.nickname,
                                        uid = card.uid,
                                        manufacturer = card.manufacturer,
                                        technology = card.technology,
                                        cardStandard = card.cardStandard,
                                        isFavorite = card.isFavorite,
                                        onFavoriteToggle = { onToggleFavorite(card) },
                                        statusBadgeText = badgeText,
                                        statusBadgeColor = badgeColor,
                                        modifier = Modifier
                                            .clickable { onCardClick(card) }
                                            .then(
                                                if (isActive && !card.isArchived) {
                                                    Modifier.border(2.5.dp, EmeraldSuccess, RoundedCornerShape(20.dp))
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Unified Action Bar with accessible touch targets
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (card.isArchived) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier
                                                    .heightIn(min = 40.dp)
                                                    .bounceClick { onToggleArchive(card) }
                                                    .testTag("card_item_restore_${card.id}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Unarchive,
                                                        contentDescription = "Restore Card",
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Restore Pass",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isActive) EmeraldSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isActive) EmeraldSuccess.copy(alpha = 0.4f) else Color.Transparent
                                                ),
                                                modifier = Modifier
                                                    .heightIn(min = 40.dp)
                                                    .bounceClick { onSelectActiveCard(card) }
                                                    .testTag("card_item_set_active_${card.id}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                        contentDescription = "Select Active Card",
                                                        tint = if (isActive) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isActive) "Active Pass" else "Set Active",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isActive) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { onToggleArchive(card) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("card_item_archive_button_${card.id}")
                                            ) {
                                                Icon(
                                                    imageVector = if (card.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                                    contentDescription = if (card.isArchived) "Restore Card" else "Archive Card",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = { onCardClick(card) },
                                                modifier = Modifier
                                                    .heightIn(min = 40.dp)
                                                    .testTag("card_item_details_button_${card.id}"),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            if (!card.isArchived) {
                                                Button(
                                                    onClick = {
                                                        onSelectActiveCard(card)
                                                        showUseModal = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = EmeraldSuccess,
                                                        contentColor = Color.White
                                                    ),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier
                                                        .heightIn(min = 40.dp)
                                                        .testTag("card_item_use_button_${card.id}"),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Use Pass", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

        if (pendingDeleteCard != null) {
            DeleteConfirmationDialog(
                title = "Delete Card?",
                message = "Are you sure you want to permanently delete this card? This action cannot be undone.",
                onConfirm = {
                    val cardToDel = pendingDeleteCard!!
                    pendingDeleteCard = null
                    onDeleteCard(cardToDel)
                },
                onDismiss = { pendingDeleteCard = null }
            )
        }

        if (showUseModal && activeCard != null) {
            Dialog(
                onDismissRequest = { showUseModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                UseCardModal(
                    activeCard = activeCard,
                    allCards = activeCardsForSwitcher.ifEmpty { listOf(activeCard) },
                    onDismiss = { showUseModal = false },
                    onSelectCard = { onSelectActiveCard(it) },
                    onLogUsage = onLogUsage
                )
            }
        }

        if (showManualDialog) {
            ManualCardDialog(
                onDismiss = { showManualDialog = false },
                onSaveCard = { scanResult, nickname, notes ->
                    onSaveManualCard(scanResult, nickname, notes)
                }
            )
        }
    }
}

@Composable
fun StatMiniCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
