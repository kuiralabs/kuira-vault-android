package com.kuiralabs.vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.midnight.kuira.dapp.ContractCallProgressBar

// The public Vault content, rendered inside MainActivity's tab host. The floating wallet/sigil
// pills are hoisted to the window root (MainActivity). The action progress (deploy / deposit /
// propose / approve / execute) is PINNED at the bottom here — outside the scroll — matching the
// Private tab, so it stays visible wherever the user has scrolled and whichever button they tapped.
@Composable
fun VaultScreen(
    modifier: Modifier = Modifier,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val busy by viewModel.busy.collectAsState()
    val callStage by viewModel.callStage.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                // Leave room so the last card content isn't hidden behind the pinned progress bar.
                .padding(bottom = if (busy) 72.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VaultCard(viewModel = viewModel)
        }

        if (busy) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                    val stage = callStage
                    if (stage == null) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Working…", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        ContractCallProgressBar(
                            stage = stage,
                            accent = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
