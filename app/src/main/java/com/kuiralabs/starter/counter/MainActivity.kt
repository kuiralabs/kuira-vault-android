package com.kuiralabs.starter.counter

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import com.kuiralabs.starter.counter.ui.PrivateVaultScreen
import com.kuiralabs.starter.counter.ui.VaultScreen
import com.midnight.kuira.dapp.wallet.WalletAppShell
import com.midnight.kuira.sdk.walletruntime.WalletNotifications
import com.midnight.kuira.sdk.walletruntime.SessionLock
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// AppCompatActivity (not ComponentActivity) because SigilStatusPanel hosts
// a biometric prompt internally and the prompt requires a FragmentActivity
// host — AppCompatActivity satisfies that; ComponentActivity does not.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    /** Session auto-lock (#14) — onUserInteraction resets its foreground idle timer. */
    @Inject lateinit var sessionLock: SessionLock

    // POST_NOTIFICATIONS for the dust-sync Live Update (#235). Best-effort: if the
    // user denies, the background sync still runs — just without a visible notification.
    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (WalletNotifications.shouldRequest(this)) {
            notifPermission.launch(WalletNotifications.PERMISSION)
        }
        setContent {
            // WalletAppShell = SessionLockGate (lock) + WalletOverlayHost (the full-screen
            // Send / Receive / Settings overlays render in the activity window). Both vault
            // flavors live under the ONE shell so they share the lock + wallet overlays.
            WalletAppShell {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        VaultTabs()
                    }
                }
            }
        }
    }

    // The public and private vaults side by side — the whole point of the demo is comparing the
    // two disclosure models. A plain tab switch (no NavHost) keeps a single top-level surface;
    // each screen hosts its own floating PanelBar (wallet/sigil pills), as before.
    @Composable
    private fun VaultTabs() {
        var tab by remember { mutableIntStateOf(0) }
        Column(Modifier.fillMaxSize()) {
            // Keep the tab row clear of the status bar / notch.
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Public") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Private") })
            }
            when (tab) {
                0 -> VaultScreen()
                else -> PrivateVaultScreen()
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionLock.onUserActivity()
    }
}
