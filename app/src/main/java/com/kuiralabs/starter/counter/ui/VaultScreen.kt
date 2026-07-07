package com.kuiralabs.starter.counter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.midnight.kuira.dapp.PanelBar

// The Vault home screen. Same host integration as the counter starter: the sigil + wallet
// pills float over the content (PanelBar floating mode), and the wallet pill owns network
// selection, which VaultViewModel follows so the per-network Vault tracks the current chain.
@Composable
fun VaultScreen(
    modifier: Modifier = Modifier,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val selectedNetwork by viewModel.selectedNetwork.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(top = 56.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Kuira Vault — Multisig Treasury", style = MaterialTheme.typography.headlineMedium)
            VaultCard()
        }

        PanelBar(
            floating = true,
            network = selectedNetwork,
            onNetworkChange = viewModel::selectNetwork,
        )
    }
}
