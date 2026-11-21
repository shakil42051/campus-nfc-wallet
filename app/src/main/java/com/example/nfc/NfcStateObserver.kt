package com.example.nfc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

data class NfcHardwareState(
    val isSupported: Boolean,
    val isEnabled: Boolean
)

/**
 * Observes live NFC adapter state across both ACTION_ADAPTER_STATE_CHANGED broadcasts
 * and Lifecycle ON_RESUME events (e.g. returning from Android NFC Settings).
 */
@Composable
fun rememberNfcHardwareState(): NfcHardwareState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val adapter = remember(context) { NfcAdapter.getDefaultAdapter(context) }

    var isEnabled by remember(adapter) { mutableStateOf(adapter?.isEnabled == true) }

    DisposableEffect(context, lifecycleOwner, adapter) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isEnabled = adapter?.isEnabled == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                    val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
                    isEnabled = state == NfcAdapter.STATE_ON
                }
            }
        }
        runCatching {
            context.registerReceiver(receiver, IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED))
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return NfcHardwareState(
        isSupported = adapter != null,
        isEnabled = isEnabled
    )
}
