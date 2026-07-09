package com.kuiralabs.starter.counter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

// The Private Vault home screen — sibling of [VaultScreen]. The floating PanelBar (wallet/sigil
// pills) owns network selection, which PrivateVaultViewModel follows. Sits inside MainActivity's
// tab host, which already handles the top status-bar inset, so this screen doesn't re-pad the top.
@Composable
fun PrivateVaultScreen(
    modifier: Modifier = Modifier,
    viewModel: PrivateVaultViewModel = hiltViewModel(),
) {
    val selectedNetwork by viewModel.selectedNetwork.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(top = 56.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Private Vault", style = MaterialTheme.typography.headlineMedium)
            PrivateVaultCard()
        }

        PanelBar(
            floating = true,
            network = selectedNetwork,
            onNetworkChange = viewModel::selectNetwork,
        )
    }
}
