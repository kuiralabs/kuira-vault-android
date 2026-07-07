package com.kuiralabs.starter.counter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.midnight.kuira.dapp.ContractCallProgressBar
import java.math.BigInteger

private val NIGHT_UNIT: BigInteger = BigInteger.valueOf(1_000_000L)
private fun nightToBase(whole: String): BigInteger? =
    whole.trim().toBigIntegerOrNull()?.takeIf { it > BigInteger.ZERO }?.multiply(NIGHT_UNIT)
private fun baseToNight(base: BigInteger): String = base.divide(NIGHT_UNIT).toString()

// The Vault (OZ multisig treasury) card. Branches on VaultUiState: deploy a 3-signer Vault,
// deposit NIGHT into the treasury, propose a withdrawal, approve to threshold, execute.
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val callStage by viewModel.callStage.collectAsState()
    val error by viewModel.error.collectAsState()

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Vault — multisig treasury", style = MaterialTheme.typography.titleMedium)

            when (val s = state) {
                VaultUiState.NotReady -> NotReadyBody()
                VaultUiState.ReadyToDeploy -> DeployBody(
                    busy = busy,
                    mySignerKeyHex = viewModel.mySignerKeyHex(),
                    onDeploy = viewModel::deploy,
                    onConnect = viewModel::connect,
                )
                is VaultUiState.Deployed -> DeployedBody(
                    state = s,
                    busy = busy,
                    defaultRecipient = viewModel.myAddress(),
                    onDeposit = viewModel::deposit,
                    onPropose = viewModel::propose,
                    onApprove = viewModel::approve,
                    onExecute = viewModel::execute,
                    onDisconnect = viewModel::disconnect,
                )
            }

            if (busy) {
                if (callStage == null) CircularProgressIndicator()
                else ContractCallProgressBar(
                    stage = callStage,
                    accent = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (error != null) {
                Text("Last error: $error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NotReadyBody() {
    Text(
        text = "Forge a sigil above, then fund the wallet and register dust.\n\n" +
            "    mn airdrop 1000 --wallet <addr> --network undeployed\n" +
            "    mn dust register --wallet <addr> --network undeployed",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun DeployBody(
    busy: Boolean,
    mySignerKeyHex: String?,
    onDeploy: (Int, List<String>) -> Unit,
    onConnect: (String) -> Unit,
) {
    var threshold by remember { mutableStateOf(1) }
    var coSigner1 by remember { mutableStateOf("") }
    var coSigner2 by remember { mutableStateOf("") }

    Text(
        "Deploy a 3-signer Vault. This wallet is signer 1; add two co-signers or leave them blank " +
            "for placeholders (threshold 1 lets you run the full flow solo).",
        style = MaterialTheme.typography.bodyMedium,
    )

    Text("Approvals required (of 3):", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..3).forEach { n ->
            FilterChip(selected = threshold == n, onClick = { threshold = n }, label = { Text("$n") })
        }
    }

    OutlinedTextField(
        value = coSigner1, onValueChange = { coSigner1 = it },
        label = { Text("Co-signer 2 key (64 hex, optional)") },
        singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy,
    )
    OutlinedTextField(
        value = coSigner2, onValueChange = { coSigner2 = it },
        label = { Text("Co-signer 3 key (64 hex, optional)") },
        singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy,
    )

    Button(onClick = { onDeploy(threshold, listOf(coSigner1, coSigner2)) }, enabled = !busy) {
        Text("Deploy $threshold-of-3 Vault")
    }

    if (mySignerKeyHex != null) {
        val clipboard = LocalClipboardManager.current
        Text(
            "Your signer key (share so a deployer can add you as a co-signer):",
            style = MaterialTheme.typography.labelSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                mySignerKeyHex, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { clipboard.setText(AnnotatedString(mySignerKeyHex)) }) { Text("Copy") }
        }
    }

    HorizontalDivider()

    // ── Connect to a Vault someone else deployed (a co-signer joining) ──
    Text("Or connect to an existing Vault", style = MaterialTheme.typography.labelLarge)
    var connectAddr by remember { mutableStateOf("") }
    OutlinedTextField(
        value = connectAddr, onValueChange = { connectAddr = it },
        label = { Text("Vault contract address (64 hex)") },
        singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy,
    )
    OutlinedButton(
        onClick = { onConnect(connectAddr) },
        enabled = !busy && connectAddr.trim().length == 64,
    ) { Text("Connect") }
}

@Composable
private fun DeployedBody(
    state: VaultUiState.Deployed,
    busy: Boolean,
    defaultRecipient: String?,
    onDeposit: (BigInteger) -> Unit,
    onPropose: (String, BigInteger) -> Unit,
    onApprove: (Long) -> Unit,
    onExecute: (Long) -> Unit,
    onDisconnect: () -> Unit,
) {
    Text(
        "${state.threshold}-of-${state.signerCount} Vault" +
            (if (state.refreshing) " · refreshing…" else "") +
            "\n${state.address.take(20)}…${state.address.takeLast(8)}",
        style = MaterialTheme.typography.bodySmall,
    )
    Text("Treasury: ${baseToNight(state.treasuryBalance)} NIGHT", style = MaterialTheme.typography.headlineSmall)
    if (!state.canApprove) {
        Text(
            "You are not a signer of this Vault — view only (you can deposit + propose, but not approve).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }

    // ── Deposit ──
    var depositAmt by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        OutlinedTextField(
            value = depositAmt, onValueChange = { depositAmt = it },
            label = { Text("NIGHT") }, singleLine = true, enabled = !busy, modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { nightToBase(depositAmt)?.let { onDeposit(it); depositAmt = "" } },
            enabled = !busy && nightToBase(depositAmt) != null,
        ) { Text("Deposit") }
    }

    HorizontalDivider()

    // ── New proposal ──
    Text("New withdrawal proposal", style = MaterialTheme.typography.labelLarge)
    var recipient by remember(defaultRecipient) { mutableStateOf(defaultRecipient.orEmpty()) }
    var proposeAmt by remember { mutableStateOf("") }
    OutlinedTextField(
        value = recipient, onValueChange = { recipient = it },
        label = { Text("Recipient wallet address") }, singleLine = true, enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        OutlinedTextField(
            value = proposeAmt, onValueChange = { proposeAmt = it },
            label = { Text("NIGHT") }, singleLine = true, enabled = !busy, modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { nightToBase(proposeAmt)?.let { onPropose(recipient.trim(), it); proposeAmt = "" } },
            enabled = !busy && nightToBase(proposeAmt) != null && recipient.isNotBlank(),
        ) { Text("Propose") }
    }

    // ── Proposals list ──
    if (state.proposals.isNotEmpty()) {
        HorizontalDivider()
        Text("Proposals", style = MaterialTheme.typography.labelLarge)
        state.proposals.forEach { p -> ProposalRow(p, busy, state.canApprove, onApprove, onExecute) }
    }

    HorizontalDivider()
    val clipboard = LocalClipboardManager.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { clipboard.setText(AnnotatedString(state.address)) }) { Text("Copy vault address") }
        TextButton(onClick = onDisconnect, enabled = !busy) { Text("Disconnect") }
    }
    Text("Balance + proposals read live from chain. Share the address so co-signers can Connect.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ProposalRow(
    p: ProposalView,
    busy: Boolean,
    canApprove: Boolean,
    onApprove: (Long) -> Unit,
    onExecute: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "#${p.id} → ${p.recipientLabel} · ${baseToNight(p.amount)} NIGHT" +
                (if (p.executed) " · executed" else " · ${p.approvals}/${p.threshold} approved"),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (!p.executed) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Approve only if this wallet is a signer; execute is permissionless once threshold met.
                OutlinedButton(onClick = { onApprove(p.id) }, enabled = !busy && canApprove) { Text("Approve") }
                Button(onClick = { onExecute(p.id) }, enabled = !busy && p.settleable) { Text("Execute") }
            }
        }
    }
}
