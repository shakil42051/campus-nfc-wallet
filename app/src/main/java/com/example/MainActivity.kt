package com.example

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.nfc.NfcCardAnalyzer
import com.example.ui.navigation.MainNavGraph
import com.example.ui.theme.CampusNfcWalletTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null
    private var nfcIntentFilters: Array<IntentFilter>? = null
    private var techListsArray: Array<Array<String>>? = null

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as NfcWalletApplication).container
        val factory = ViewModelFactory(appContainer)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setupNfcForegroundDispatch()

        // Protect sensitive wallet content & Recents thumbnails whenever App Lock is enabled
        lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                if (settings.isAppLockEnabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        // Only handle launch NFC intent on fresh creation (prevent replay on screen rotation)
        if (savedInstanceState == null) {
            handleNfcIntent(intent)
        }

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            CampusNfcWalletTheme(darkTheme = settings.isDarkMode) {
                MainNavGraph(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun setupNfcForegroundDispatch() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            runCatching { addDataType("*/*") }
        }
        val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)

        nfcIntentFilters = arrayOf(ndefFilter, techFilter, tagFilter)

        techListsArray = arrayOf(
            arrayOf("android.nfc.tech.IsoDep"),
            arrayOf("android.nfc.tech.MifareClassic"),
            arrayOf("android.nfc.tech.MifareUltralight"),
            arrayOf("android.nfc.tech.NfcA"),
            arrayOf("android.nfc.tech.NfcB"),
            arrayOf("android.nfc.tech.NfcF"),
            arrayOf("android.nfc.tech.NfcV"),
            arrayOf("android.nfc.tech.Ndef")
        )
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter
        if (adapter != null && adapter.isEnabled) {
            runCatching {
                adapter.enableForegroundDispatch(this, nfcPendingIntent, nfcIntentFilters, techListsArray)
            }
        }
        if (::viewModel.isInitialized) {
            viewModel.onAppResumed()
        }
    }

    override fun onPause() {
        super.onPause()
        runCatching {
            nfcAdapter?.disableForegroundDispatch(this)
        }
        if (::viewModel.isInitialized) {
            viewModel.onAppPaused(isChangingConfigurations = isChangingConfigurations)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            // Do not ingest NFC tags while wallet is locked
            if (!viewModel.isUnlocked.value && viewModel.settings.value.isAppLockEnabled) {
                intent.action = null
                return
            }

            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            // Clear action so configuration changes will not replay the same tag intent
            intent.action = null

            if (tag != null) {
                lifecycleScope.launch {
                    try {
                        val scanResult = withContext(Dispatchers.IO) {
                            NfcCardAnalyzer.analyzeTag(tag)
                        }
                        viewModel.onTagScanned(scanResult)
                    } catch (e: Exception) {
                        viewModel.onScanError("Error reading NFC Tag: ${e.localizedMessage ?: "Tag moved too fast"}")
                    }
                }
            } else {
                viewModel.onScanError("NFC Tag object was empty or unreachable.")
            }
        }
    }
}
