package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.NfcWalletApplication
import com.example.attendance.ui.AttendanceScreen
import com.example.attendance.ui.AttendanceViewModel
import com.example.data.local.entity.CardEntity
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.CardDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.NavigationTransitions
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Vault", Icons.Filled.CreditCard, Icons.Outlined.CreditCard)
    object Scanner : Screen("scanner", "Scanner", Icons.Filled.Nfc, Icons.Outlined.Nfc)
    object Attendance : Screen("attendance", "Attendance", Icons.Filled.School, Icons.Outlined.School)
    object History : Screen("history", "History", Icons.Filled.History, Icons.Outlined.History)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    object About : Screen("about", "About", Icons.Filled.Info, Icons.Outlined.Info)
    object CardDetail : Screen("card_detail/{cardId}", "Card Detail", Icons.Filled.CreditCard, Icons.Outlined.CreditCard) {
        fun createRoute(cardId: Long) = "card_detail/$cardId"
    }
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Scanner,
    Screen.Attendance,
    Screen.History,
    Screen.Settings
)

private sealed interface CardDetailLoadState {
    data object Loading : CardDetailLoadState
    data class Loaded(val card: CardEntity) : CardDetailLoadState
    data object NotFound : CardDetailLoadState
}

@Composable
fun MainNavGraph(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val walletSyncMessage by viewModel.walletSyncMessage.collectAsStateWithLifecycle()
    val isLocked = settings.isAppLockEnabled && !isUnlocked

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(walletSyncMessage) {
        walletSyncMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearWalletSyncMessage()
        }
    }

    fun navigateTopLevel(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val showBottomBar = bottomNavScreens.any { it.route == currentRoute }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavScreens.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            val iconScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.18f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "navIconScale"
                            )
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            letterSpacing = (-0.3).sp
                                        )
                                    )
                                },
                                selected = isSelected,
                                onClick = { navigateTopLevel(screen.route) },
                                modifier = Modifier.testTag("nav_${screen.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                enterTransition = {
                    val targetRoute = targetState.destination.route
                    val initialRoute = initialState.destination.route
                    val isTabTransition = bottomNavScreens.any { it.route == targetRoute } &&
                        bottomNavScreens.any { it.route == initialRoute }
                    if (isTabTransition) NavigationTransitions.TabEnter else NavigationTransitions.ForwardEnter
                },
                exitTransition = {
                    val targetRoute = targetState.destination.route
                    val initialRoute = initialState.destination.route
                    val isTabTransition = bottomNavScreens.any { it.route == targetRoute } &&
                        bottomNavScreens.any { it.route == initialRoute }
                    if (isTabTransition) NavigationTransitions.TabExit else NavigationTransitions.ForwardExit
                },
                popEnterTransition = {
                    val targetRoute = targetState.destination.route
                    val initialRoute = initialState.destination.route
                    val isTabTransition = bottomNavScreens.any { it.route == targetRoute } &&
                        bottomNavScreens.any { it.route == initialRoute }
                    if (isTabTransition) NavigationTransitions.TabEnter else NavigationTransitions.BackEnter
                },
                popExitTransition = {
                    val targetRoute = targetState.destination.route
                    val initialRoute = initialState.destination.route
                    val isTabTransition = bottomNavScreens.any { it.route == targetRoute } &&
                        bottomNavScreens.any { it.route == initialRoute }
                    if (isTabTransition) NavigationTransitions.TabExit else NavigationTransitions.BackExit
                }
            ) {
                composable(Screen.Dashboard.route) {
                    val cardList by viewModel.cardList.collectAsStateWithLifecycle()
                    val allActiveCards by viewModel.allActiveCards.collectAsStateWithLifecycle()
                    val activeCard by viewModel.activeCard.collectAsStateWithLifecycle()
                    val stats by viewModel.stats.collectAsStateWithLifecycle()
                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
                    val selectedSortOrder by viewModel.selectedSortOrder.collectAsStateWithLifecycle()
                    val serverConfig by viewModel.serverConfig.collectAsStateWithLifecycle()
                    val isWalletSyncing by viewModel.isWalletSyncing.collectAsStateWithLifecycle()

                    DashboardScreen(
                        cardList = cardList,
                        activeCardsForSwitcher = allActiveCards,
                        activeCard = activeCard,
                        stats = stats,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.searchQuery.value = it },
                        selectedFilter = selectedFilter,
                        onFilterSelect = { viewModel.selectedFilter.value = it },
                        selectedSortOrder = selectedSortOrder,
                        onSortSelect = { viewModel.selectedSortOrder.value = it },
                        onCardClick = { card ->
                            navController.navigate(Screen.CardDetail.createRoute(card.id))
                        },
                        onSelectActiveCard = { viewModel.selectActiveCard(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onToggleArchive = { viewModel.toggleArchive(it) },
                        onDeleteCard = { card ->
                            viewModel.deleteCard(card)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Card '${card.nickname}' deleted.",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreLastDeletedCard()
                                }
                            }
                        },
                        onScanFabClick = { navigateTopLevel(Screen.Scanner.route) },
                        isServerLinked = serverConfig.isConnected,
                        serverHostLabel = serverConfig.serverUrl,
                        isWalletSyncing = isWalletSyncing,
                        onSyncOrLinkServerClick = {
                            if (serverConfig.serverUrl.isNotBlank()) {
                                viewModel.syncWalletWithServer()
                            } else {
                                navigateTopLevel(Screen.Settings.route)
                            }
                        },
                        onSaveManualCard = { scanResult, nickname, notes ->
                            viewModel.saveCard(scanResult, nickname, notes)
                        },
                        onLogUsage = { card, usageType, action, location ->
                            viewModel.logTransaction(card.id, card.nickname, card.uid, usageType, action, "SUCCESS", location)
                        }
                    )
                }

                composable(Screen.Scanner.route) {
                    val latestScan by viewModel.latestScan.collectAsStateWithLifecycle()
                    val latestScanAutoSaved by viewModel.latestScanAutoSaved.collectAsStateWithLifecycle()
                    val scanErrorMessage by viewModel.scanErrorMessage.collectAsStateWithLifecycle()
                    val isBatchMode by viewModel.isBatchMode.collectAsStateWithLifecycle()
                    val batchQueue by viewModel.batchQueue.collectAsStateWithLifecycle()

                    ScannerScreen(
                        latestScan = latestScan,
                        wasAutoSaved = latestScanAutoSaved,
                        errorMessage = scanErrorMessage,
                        isDeveloperMode = settings.isDeveloperMode,
                        isBatchMode = isBatchMode,
                        batchQueue = batchQueue,
                        onToggleBatchMode = { viewModel.setBatchMode(it) },
                        onSaveCard = { scanResult, nickname, notes ->
                            viewModel.saveCard(scanResult, nickname, notes)
                        },
                        onResetScan = { viewModel.clearLatestScan() },
                        onClearError = { viewModel.clearScanError() },
                        onRemoveBatchItem = { id -> viewModel.removeFromBatchQueue(id) },
                        onUpdateBatchNickname = { id, name -> viewModel.updateBatchItemNickname(id, name) },
                        onClearBatchQueue = { viewModel.clearBatchQueue() },
                        onSaveBatchQueue = { onFinished -> viewModel.saveBatchQueue(onFinished) },
                        onAddManualToBatch = { scan, nick, notes -> viewModel.addToBatchQueue(scan, nick, notes) }
                    )
                }

                composable(Screen.History.route) {
                    val allActiveCards by viewModel.allActiveCards.collectAsStateWithLifecycle()
                    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
                    val txSearchQuery by viewModel.transactionSearchQuery.collectAsStateWithLifecycle()

                    HistoryScreen(
                        cardList = allActiveCards,
                        transactions = transactions,
                        transactionSearchQuery = txSearchQuery,
                        onTransactionSearchQueryChange = { viewModel.transactionSearchQuery.value = it },
                        onLogCustomUsage = { card, usageType, action, location ->
                            viewModel.logTransaction(card.id, card.nickname, card.uid, usageType, action, "SUCCESS", location)
                        },
                        onDeleteTransaction = { id ->
                            viewModel.deleteTransaction(id)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Transaction record deleted.",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreLastDeletedTransaction()
                                }
                            }
                        },
                        onClearAllTransactions = { viewModel.clearAllTransactions() },
                        onCardClick = { card ->
                            navController.navigate(Screen.CardDetail.createRoute(card.id))
                        }
                    )
                }

                composable(Screen.Attendance.route) {
                    val context = LocalContext.current
                    val appContainer = (context.applicationContext as NfcWalletApplication).container
                    val attendanceViewModel: AttendanceViewModel = viewModel(
                        factory = AttendanceViewModel.Factory(
                            repository = appContainer.attendanceRepository,
                            cardRepository = appContainer.cardRepository,
                            roleAccessManager = appContainer.roleAccessManager,
                            campusServerSyncService = appContainer.campusServerSyncService
                        )
                    )
                    AttendanceScreen(viewModel = attendanceViewModel)
                }

                composable(Screen.Settings.route) {
                    val backupOverview by viewModel.backupOverview.collectAsStateWithLifecycle()
                    val serverConfig by viewModel.serverConfig.collectAsStateWithLifecycle()
                    val roleState by viewModel.roleAccessState.collectAsStateWithLifecycle()
                    val isWalletSyncing by viewModel.isWalletSyncing.collectAsStateWithLifecycle()

                    SettingsScreen(
                        settings = settings,
                        serverConfig = serverConfig,
                        roleState = roleState,
                        isWalletSyncing = isWalletSyncing,
                        onSyncWalletNow = { viewModel.syncWalletWithServer() },
                        onUpdateAutoSync = { viewModel.updateAutoSyncEnabled(it) },
                        onAuthenticateWalletServer = { host, port, https, user, pass, cb ->
                            viewModel.authenticateWalletServer(host, port, https, user, pass, cb)
                        },
                        onLinkWalletServerWithApiKey = { host, port, https, authType, key, provider, campus, device, cb ->
                            viewModel.linkWalletServerWithApiKey(host, port, https, authType, key, provider, campus, device, cb)
                        },
                        onUnlinkWalletServer = { viewModel.unlinkWalletServer() },
                        backupOverview = backupOverview,
                        onRefreshBackupOverview = { viewModel.refreshBackupOverview() },
                        onBeforeExternalAction = { viewModel.suppressNextAutoLock() },
                        onUpdateVibration = { viewModel.updateVibration(it) },
                        onUpdateSound = { viewModel.updateSound(it) },
                        onUpdateAutoExport = { viewModel.updateAutoExport(it) },
                        onUpdateDarkMode = { viewModel.updateDarkMode(it) },
                        onUpdateDeveloperMode = { viewModel.updateDeveloperMode(it) },
                        onUpdateBiometricEnabled = { viewModel.updateBiometricEnabled(it) },
                        onUpdateRequireAuthForBackupRestore = { viewModel.updateRequireAuthForBackupRestore(it) },
                        onUpdateAppLockEnabled = { viewModel.updateAppLockEnabled(it) },
                        onUpdateAutoLockTimeoutSeconds = { viewModel.updateAutoLockTimeoutSeconds(it) },
                        onUpdateAuthMethod = { viewModel.updateAuthMethod(it) },
                        onLockAppNow = { viewModel.lockApp() },
                        onSetSecurityPassword = { viewModel.setSecurityPassword(it) },
                        onSetSecurityPinAndQuestion = { pin, question, answer ->
                            viewModel.setSecurityPinAndQuestion(pin, question, answer)
                        },
                        onSetSecurityQuestionAndAnswer = { question, answer ->
                            viewModel.setSecurityQuestionAndAnswer(question, answer)
                        },
                        onClearSecurityPin = { viewModel.clearSecurityPin() },
                        onVerifyPin = { pin -> viewModel.verifyPin(pin) },
                        onVerifyPassword = { pass -> viewModel.verifyPassword(pass) },
                        onVerifySecretAnswer = { answer -> viewModel.verifySecretAnswer(answer) },
                        onClearAllCards = { viewModel.clearAllCards() },
                        onNavigateToAbout = { navController.navigate(Screen.About.route) },
                        onCreateEncryptedBackup = { pass, fn, cb -> viewModel.createEncryptedBackup(pass, fn, cb) },
                        onRecordExternalBackupCompleted = { artifact, done ->
                            viewModel.recordExternalBackupCompleted(artifact, done)
                        },
                        onParseAndDecryptBackup = { bytes, pass, size, cb ->
                            viewModel.parseAndDecryptBackup(bytes, pass, size, cb)
                        },
                        onExecuteAtomicRestore = { preview, mode, cats, fn, cb ->
                            viewModel.executeRestore(preview, mode, cats, fn, cb)
                        },
                        onReadBytesFromUriAsync = { ctx, uri, cb -> viewModel.readBytesFromUriAsync(ctx, uri, cb) },
                        onWriteBytesToUriAsync = { ctx, uri, bytes, cb -> viewModel.writeBytesToUriAsync(ctx, uri, bytes, cb) },
                        buildDiagnosticsString = { ex -> viewModel.buildDiagnosticsString(ex) },
                        onCreateInternalSnapshot = { pass, label, cb -> viewModel.createInternalSnapshot(pass, label, cb) },
                        onDeleteInternalSnapshotAsync = { fn, cb -> viewModel.deleteSnapshotAsync(fn, cb) },
                        onReadInternalSnapshotAsync = { fn, cb -> viewModel.readSnapshotBytesAsync(fn, cb) }
                    )
                }

                composable(Screen.About.route) {
                    AboutScreen(
                        onBackClick = { navController.popBackStack() },
                        onBeforeExternalAction = { viewModel.suppressNextAutoLock() }
                    )
                }

                composable(
                    route = Screen.CardDetail.route,
                    arguments = listOf(navArgument("cardId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val cardId = backStackEntry.arguments?.getLong("cardId") ?: 0L
                    val loadStateFlow = remember(cardId) {
                        viewModel.getCardById(cardId).map { card ->
                            if (card != null) CardDetailLoadState.Loaded(card) else CardDetailLoadState.NotFound
                        }
                    }
                    val loadState by loadStateFlow.collectAsStateWithLifecycle(initialValue = CardDetailLoadState.Loading)
                    val activeCard by viewModel.activeCard.collectAsStateWithLifecycle()
                    val allActiveCards by viewModel.allActiveCards.collectAsStateWithLifecycle()
                    val cardTransactionsFlow = remember(cardId) { viewModel.getTransactionsForCard(cardId) }
                    val cardTransactions by cardTransactionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                    when (val state = loadState) {
                        is CardDetailLoadState.Loaded -> {
                            CardDetailScreen(
                                card = state.card,
                                activeCardId = activeCard?.id,
                                allCards = allActiveCards,
                                cardTransactions = cardTransactions,
                                isDeveloperMode = settings.isDeveloperMode,
                                onSelectActiveCard = { viewModel.selectActiveCard(it) },
                                onBackClick = { navController.popBackStack() },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onToggleArchive = { viewModel.toggleArchive(it) },
                                onDeleteClick = { card ->
                                    viewModel.deleteCard(card)
                                    navController.popBackStack()
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Card '${card.nickname}' deleted.",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreLastDeletedCard()
                                        }
                                    }
                                },
                                onUpdateCard = { existingCard, scanResult, nickname, notes ->
                                    viewModel.updateCardDetails(existingCard, scanResult, nickname, notes)
                                },
                                onLogUsage = { card, usageType, action, location ->
                                    viewModel.logTransaction(card.id, card.nickname, card.uid, usageType, action, "SUCCESS", location)
                                },
                                onBeforeExternalAction = { viewModel.suppressNextAutoLock() }
                            )
                        }

                        CardDetailLoadState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        CardDetailLoadState.NotFound -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Card Not Found",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "This card may have been deleted or removed during a backup restore.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(onClick = { navController.popBackStack() }) {
                                        Text("Go Back")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay AppLockScreen on top of NavHost so locking preserves navigation & unsaved state
        if (isLocked) {
            BackHandler(enabled = true) {
                // Consume back navigation while locked
            }
            Surface(modifier = Modifier.fillMaxSize()) {
                AppLockScreen(
                    settings = settings,
                    onVerifyPin = { viewModel.verifyPin(it) },
                    onVerifyPassword = { viewModel.verifyPassword(it) },
                    onVerifySecretAnswer = { viewModel.verifySecretAnswer(it) },
                    onRecordFailedAttempt = { viewModel.recordFailedAuthAttempt() },
                    onResetFailedAttempts = { viewModel.resetFailedAuthAttempts() },
                    getRemainingLockoutSeconds = { viewModel.getRemainingLockoutSeconds() },
                    onUnlocked = { viewModel.unlockApp() }
                )
            }
        }
    }
}
