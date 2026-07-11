package com.kuiralabs.vault.ui

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

private fun nightToBase(whole: String): BigInteger? =
    whole.trim().toBigIntegerOrNull()?.takeIf { it > BigInteger.ZERO }
        ?.multiply(BigInteger.TEN.pow(NIGHT_DECIMALS))
private fun baseToNight(base: BigInteger): String = formatNight(base)
private fun shortAddress(address: String): String =
    if (address.length > 26) "${address.take(18)}…${address.takeLast(6)}" else address

// The Private Vault card — same M-of-N ceremony as VaultCard, but the ledger holds only
// commitments + ciphertext. Branches on PrivateVaultUiState: create/join, then a member view that
// decrypts proposals locally. The privacy story is surfaced in the copy, not hidden in the code.
@Composable
fun PrivateVaultCard(
    modifier: Modifier = Modifier,
    viewModel: PrivateVaultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val error by viewModel.error.collectAsState()

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Private Vault — hidden multisig", style = MaterialTheme.typography.titleMedium)
            Text(
                "Same M-of-N treasury, but signers, amounts, and recipients live on-chain only as " +
                    "commitments. Members share a viewing key; outsiders see ciphertext.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (val s = state) {
                PrivateVaultUiState.NotReady -> NotReadyBody()
                PrivateVaultUiState.ReadyToStart -> StartBody(
                    busy = busy,
                    mySignerKeyHex = viewModel.mySignerKeyHex(),
                    onCreate = viewModel::create,
                    onJoin = viewModel::join,
                    onObserve = viewModel::observe,
                )
                is PrivateVaultUiState.Observer -> ObserverBody(
                    state = s,
                    busy = busy,
                    onDeposit = viewModel::observerDeposit,
                    onStop = viewModel::stopObserving,
                )
                is PrivateVaultUiState.Member -> MemberBody(
                    state = s,
                    busy = busy,
                    defaultRecipient = viewModel.myAddress().orEmpty(),
                    onDeposit = viewModel::deposit,
                    onPropose = viewModel::propose,
                    onApprove = viewModel::approve,
                    onRevoke = viewModel::revoke,
                    onExecute = viewModel::execute,
                    onDisconnect = viewModel::disconnect,
                )
            }

            // Action progress is rendered by PrivateVaultScreen as a pinned bottom bar (always
            // visible regardless of scroll). Here we only surface the last error inline.
            error?.let {
                Text("Last error: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NotReadyBody() {
    Text(
        "Forge your sigil first, then fund the wallet:\n" +
            "  mn airdrop 1000 --wallet <your address> --network undeployed\n" +
            "then register dust in the wallet panel and wait ~30s. Then create or join a private vault.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StartBody(
    busy: Boolean,
    mySignerKeyHex: String?,
    onCreate: (Int, List<String>) -> Unit,
    onJoin: (String) -> Unit,
    onObserve: (String) -> Unit,
) {
    // Default threshold 1 so the very first tap succeeds solo (mirrors the public vault); the
    // signer count grows as co-signer keys are filled in.
    var threshold by remember { mutableStateOf(1) }
    var coSigner2 by remember { mutableStateOf("") }
    var coSigner3 by remember { mutableStateOf("") }
    var inviteText by remember { mutableStateOf("") }
    var observeAddr by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    val signerCount = 1 + listOf(coSigner2, coSigner3).count { it.isNotBlank() }
    val thresholdOk = threshold in 1..signerCount

    Text("Create a private vault", style = MaterialTheme.typography.titleSmall)
    Text("Signatures required", style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..3).forEach { n ->
            FilterChip(selected = threshold == n, onClick = { threshold = n }, label = { Text("$n") })
        }
    }
    Text("You + a co-signer for each key below. Leave both blank for a solo (threshold-1) vault.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedTextField(
        value = coSigner2, onValueChange = { coSigner2 = it },
        label = { Text("Co-signer 2 key (64 hex, optional)") },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = coSigner3, onValueChange = { coSigner3 = it },
        label = { Text("Co-signer 3 key (64 hex, optional)") },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { onCreate(threshold, listOf(coSigner2, coSigner3)) },
        enabled = !busy && thresholdOk, modifier = Modifier.fillMaxWidth(),
    ) { Text("Create $threshold-of-$signerCount private vault") }
    if (!thresholdOk) {
        Text("Threshold $threshold needs $threshold signers — add co-signer keys or lower it.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
    }

    if (mySignerKeyHex != null) {
        Text("Your signer key (send to a creator so they can add you)", style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(mySignerKeyHex, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            TextButton(onClick = { clipboard.setText(AnnotatedString(mySignerKeyHex)) }) { Text("Copy") }
        }
    }

    HorizontalDivider()

    Text("Or join a vault you were invited to", style = MaterialTheme.typography.titleSmall)
    Text(
        "To join as a co-signer: (1) send your signer key above to the creator, (2) they create the " +
            "vault and send you back an invite, (3) paste it here. The invite carries the viewing key " +
            "and your signer material.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = inviteText, onValueChange = { inviteText = it },
        label = { Text("Vault invite") }, modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(
        onClick = { onJoin(inviteText) },
        enabled = !busy && inviteText.isNotBlank(), modifier = Modifier.fillMaxWidth(),
    ) { Text("Join vault") }

    HorizontalDivider()

    Text("Or watch a vault (no invite)", style = MaterialTheme.typography.titleSmall)
    Text(
        "Paste a vault's address to follow it as an observer: you'll see the pot balance and how many " +
            "have approved each proposal, and you can contribute. Contents stay sealed — no viewing key needed.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = observeAddr, onValueChange = { observeAddr = it },
        label = { Text("Vault address (64 hex)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(
        onClick = { onObserve(observeAddr) },
        enabled = !busy && observeAddr.isNotBlank(), modifier = Modifier.fillMaxWidth(),
    ) { Text("Observe vault") }
}

@Composable
private fun MemberBody(
    state: PrivateVaultUiState.Member,
    busy: Boolean,
    defaultRecipient: String,
    onDeposit: (BigInteger) -> Unit,
    onPropose: (String, BigInteger) -> Unit,
    onApprove: (Long) -> Unit,
    onRevoke: (Long) -> Unit,
    onExecute: (Long) -> Unit,
    onDisconnect: () -> Unit,
) {
    var depositAmt by remember { mutableStateOf("") }
    var recipient by remember(defaultRecipient) { mutableStateOf(defaultRecipient) }
    var proposeAmt by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    val header = buildString {
        append("${state.threshold}-of-${state.signerCount} private vault")
        if (state.refreshing) append(" · refreshing…")
    }
    Text(header, style = MaterialTheme.typography.titleSmall)
    Text("Signer roster is padded to a fixed size on-chain — the real count is hidden from outsiders.",
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("${state.address.take(20)}…${state.address.takeLast(8)}",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Treasury: ${baseToNight(state.treasuryBalance)} NIGHT", style = MaterialTheme.typography.headlineSmall)

    // Invites to share (creator only) — the join material for each co-signer.
    if (state.isCreator && state.invites.isNotEmpty()) {
        HorizontalDivider()
        Text("Invites to share", style = MaterialTheme.typography.titleSmall)
        Text(
            "Send each co-signer THEIR invite (each is unique — it carries that person's own signer " +
                "material). Match by the key you entered, shown below. Treat an invite like a secret.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.invites.forEachIndexed { i, invite ->
            // Echo the co-signer's OWN key tail so the creator can't mis-send a secret to the wrong person.
            val keyHex = state.coSignerKeyHexes.getOrNull(i)
            val who = if (keyHex != null) "Co-signer ${i + 2} (…${keyHex.takeLast(6)})" else "Co-signer ${i + 2}"
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("$who: ${shortAddress(invite)}",
                    style = MaterialTheme.typography.bodySmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                TextButton(onClick = { clipboard.setText(AnnotatedString(invite)) }) { Text("Copy") }
            }
        }
    }

    HorizontalDivider()

    // Deposit (permissionless, value not hidden in Tier 1).
    Text("Deposit NIGHT", style = MaterialTheme.typography.titleSmall)
    Text("A deposit is a public transfer in this tier — the amount is visible on-chain.",
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        OutlinedTextField(
            value = depositAmt, onValueChange = { depositAmt = it },
            label = { Text("NIGHT") }, singleLine = true, modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { nightToBase(depositAmt)?.let { onDeposit(it); depositAmt = "" } },
            enabled = !busy && nightToBase(depositAmt) != null,
        ) { Text("Deposit") }
    }

    HorizontalDivider()

    // Propose (contents encrypted on-chain while pending).
    Text("New private withdrawal", style = MaterialTheme.typography.titleSmall)
    Text("Recipient and amount stay hidden while the proposal is pending — only members decrypt them. " +
        "When it's executed, the withdrawal is a public transfer (Tier 1 keeps custody unshielded).",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedTextField(
        value = recipient, onValueChange = { recipient = it },
        label = { Text("Recipient wallet address") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        OutlinedTextField(
            value = proposeAmt, onValueChange = { proposeAmt = it },
            label = { Text("NIGHT") }, singleLine = true, modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { nightToBase(proposeAmt)?.let { onPropose(recipient, it); proposeAmt = "" } },
            enabled = !busy && nightToBase(proposeAmt) != null && recipient.isNotBlank(),
        ) { Text("Propose") }
    }

    if (state.proposals.isNotEmpty()) {
        HorizontalDivider()
        Text("Proposals", style = MaterialTheme.typography.titleSmall)
        Text("Decrypted with your viewing key. Approval counts are public; who approved is not.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.proposals.forEach { ProposalRow(it, busy, onApprove, onRevoke, onExecute) }
    }

    HorizontalDivider()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { clipboard.setText(AnnotatedString(state.address)) }) { Text("Copy vault address") }
        TextButton(onClick = onDisconnect) { Text("Disconnect") }
    }
    Text("“Leave” only forgets this vault on this device — the on-chain contract is untouched.",
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ProposalRow(
    p: PrivateProposalView,
    busy: Boolean,
    onApprove: (Long) -> Unit,
    onRevoke: (Long) -> Unit,
    onExecute: (Long) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            // Display 1-based to match the public tab (the private contract's Counter is 0-based).
            Text("Withdrawal #${p.id + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            val (chip, color) = when {
                p.executed -> "✓ Completed" to MaterialTheme.colorScheme.tertiary
                p.thresholdMet -> "Ready to execute" to MaterialTheme.colorScheme.primary
                else -> "Awaiting approvals" to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(chip, style = MaterialTheme.typography.labelMedium, color = color)
        }
        if (!p.readable) {
            // A proposal whose payload this device's viewing key can't decrypt (garbage / wrong key).
            Text("Can't decrypt this proposal — its payload doesn't match your viewing key.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            Text("${p.approvals} approval(s) · ${p.threshold} required",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        val recipientLabel = "${baseToNight(p.amount)} NIGHT → ${shortAddress(p.recipientAddress)}" +
            if (p.recipientIsMe) " (your wallet)" else ""
        Text(recipientLabel, style = MaterialTheme.typography.bodyMedium)
        Text("${p.approvals} approval(s) · ${p.threshold} required",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        when {
            p.executed -> Text("Sent to ${shortAddress(p.recipientAddress)}" +
                if (p.recipientIsMe) " (your wallet)" else "",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            p.thresholdMet -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onExecute(p.id) }, enabled = !busy) { Text("Execute withdrawal") }
                if (!p.approvedByMe) OutlinedButton(onClick = { onApprove(p.id) }, enabled = !busy) { Text("Approve") }
            }
            !p.approvedByMe -> {
                Text("Your approval is needed", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { onApprove(p.id) }, enabled = !busy) { Text("Approve") }
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("You approved · waiting for co-signers", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = { onRevoke(p.id) }, enabled = !busy) { Text("Revoke") }
            }
        }
    }
}

// The observer (public, read-only) view: the pot + approval counts are public chain facts; proposal
// contents are sealed. Contributing is allowed (deposits are permissionless). No secret material.
@Composable
private fun ObserverBody(
    state: PrivateVaultUiState.Observer,
    busy: Boolean,
    onDeposit: (BigInteger) -> Unit,
    onStop: () -> Unit,
) {
    var depositAmt by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Text("Observing a private vault" + if (state.refreshing) " · refreshing…" else "",
        style = MaterialTheme.typography.titleSmall)
    Text(
        "Public view: here you see the pot and how many approved each proposal — pending proposals' " +
            "recipients and amounts stay sealed. But in this tier the money moves as public transfers " +
            "on-chain, so a chain observer can still see who deposited and who a withdrawal paid. Full " +
            "participant privacy comes with the shielded treasury.",
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text("${state.address.take(20)}…${state.address.takeLast(8)}",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Treasury: ${baseToNight(state.treasuryBalance)} NIGHT", style = MaterialTheme.typography.headlineSmall)

    HorizontalDivider()
    Text("Contribute NIGHT", style = MaterialTheme.typography.titleSmall)
    Text("Anyone can add to the pot — no membership needed.",
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        OutlinedTextField(
            value = depositAmt, onValueChange = { depositAmt = it },
            label = { Text("NIGHT") }, singleLine = true, modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { nightToBase(depositAmt)?.let { onDeposit(it); depositAmt = "" } },
            enabled = !busy && nightToBase(depositAmt) != null,
        ) { Text("Deposit") }
    }

    if (state.proposals.isNotEmpty()) {
        HorizontalDivider()
        Text("Proposals", style = MaterialTheme.typography.titleSmall)
        Text("Sealed — only members can read a proposal's recipient and amount. The approval count is public.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.proposals.forEach { ObserverProposalRow(it) }
    }

    HorizontalDivider()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { clipboard.setText(AnnotatedString(state.address)) }) { Text("Copy vault address") }
        TextButton(onClick = onStop) { Text("Stop observing") }
    }
}

@Composable
private fun ObserverProposalRow(p: ObserverProposalView) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("🔒 Private proposal #${p.id + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            val (chip, color) = if (p.executed) "✓ Completed" to MaterialTheme.colorScheme.tertiary
                else "Pending" to MaterialTheme.colorScheme.onSurfaceVariant
            Text(chip, style = MaterialTheme.typography.labelMedium, color = color)
        }
        // Bare count, not "N of M" — the threshold is a commitment, hidden from an observer.
        Text("${p.approvals} approval(s)", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
