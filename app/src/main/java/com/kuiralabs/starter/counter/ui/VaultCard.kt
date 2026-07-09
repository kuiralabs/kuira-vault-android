package com.kuiralabs.starter.counter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.midnight.kuira.sdk.NIGHT_DECIMALS
import com.midnight.kuira.sdk.formatNight
import java.math.BigInteger

// NIGHT amounts use the SDK's one source of scale + formatter: truncating integer division here
// would render a 1_500_000-base treasury as "1 NIGHT" while the wallet pill shows "1.5".
private fun nightToBase(whole: String): BigInteger? =
    whole.trim().toBigIntegerOrNull()?.takeIf { it > BigInteger.ZERO }
        ?.multiply(BigInteger.TEN.pow(NIGHT_DECIMALS))
private fun baseToNight(base: BigInteger): String = formatNight(base)

// The Vault (OZ multisig treasury) card. Branches on VaultUiState: deploy a 3-signer Vault,
// deposit NIGHT into the treasury, propose a withdrawal, approve to threshold, execute.
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val busy by viewModel.busy.collectAsState()
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

            // Action progress is rendered by VaultScreen as a pinned bottom bar (outside the scroll).
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
    if (!state.isSigner) {
        Text(
            "You are not a signer of this Vault — you can deposit, but only signers can propose or approve.",
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
            // proposeWithdrawal is signer-gated, so only a signer can propose.
            enabled = !busy && state.isSigner && nightToBase(proposeAmt) != null && recipient.isNotBlank(),
        ) { Text("Propose") }
    }

    // ── Proposals list ──
    if (state.proposals.isNotEmpty()) {
        HorizontalDivider()
        Text("Proposals", style = MaterialTheme.typography.labelLarge)
        Text(
            "A withdrawal needs ${state.threshold} approval(s), then Execute. As a signer you must " +
                "approve a proposal (including your own) before it can execute.",
            style = MaterialTheme.typography.bodySmall,
        )
        state.proposals.forEach { p -> ProposalRow(p, busy, state.isSigner, onApprove, onExecute) }
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
    isSigner: Boolean,
    onApprove: (Long) -> Unit,
    onExecute: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Title + a status chip that names the stage in plain words.
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                "Withdrawal #${p.id}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            val (chip, chipColor) = when {
                p.executed -> "✓ Completed" to MaterialTheme.colorScheme.tertiary
                p.thresholdMet -> "Ready to execute" to MaterialTheme.colorScheme.primary
                else -> "Awaiting approvals" to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(chip, style = MaterialTheme.typography.labelMedium, color = chipColor)
        }

        // What moves where — the recipient as a real wallet address, flagged when it's this wallet.
        Text(
            "${baseToNight(p.amount)} NIGHT → ${shortAddress(p.recipientAddress)}" +
                if (p.recipientIsMe) "  (your wallet)" else "",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )

        // Approvals stated absolutely ("2 approvals · 1 required" — never a 2/1 ratio) + the one
        // action valid right now, driven by per-signer state.
        val approvalsText = "${p.approvals} approval(s) · ${p.threshold} required"
        when {
            p.executed -> Text(
                "Sent to ${shortAddress(p.recipientAddress)}${if (p.recipientIsMe) " (your wallet)" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            p.thresholdMet -> {
                Text(
                    "$approvalsText — anyone may settle it now",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Execute is permissionless once the threshold is met.
                    Button(onClick = { onExecute(p.id) }, enabled = !busy) { Text("Execute withdrawal") }
                    // A signer who hasn't approved may still record a (contract-legal) approval.
                    if (isSigner && !p.approvedByMe) {
                        OutlinedButton(onClick = { onApprove(p.id) }, enabled = !busy) { Text("Approve") }
                    }
                }
            }
            isSigner && !p.approvedByMe -> {
                Text(
                    "$approvalsText — your approval is needed",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = { onApprove(p.id) }, enabled = !busy) { Text("Approve") }
            }
            isSigner -> Text(
                "$approvalsText — you approved · waiting for co-signers",
                style = MaterialTheme.typography.bodySmall,
            )
            else -> Text(
                "$approvalsText — waiting for signers",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Head…tail truncation for a Bech32m address — keeps the HRP visible and the tail comparable. */
private fun shortAddress(address: String): String =
    if (address.length <= 26) address else "${address.take(18)}…${address.takeLast(6)}"
