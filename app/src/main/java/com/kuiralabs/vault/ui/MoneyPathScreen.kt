package com.kuiralabs.vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.kuiralabs.vault.ui.theme.Steel
import com.midnight.kuira.sdk.NIGHT_DECIMALS
import java.math.BigInteger

/**
 * "Money Path" tab — a self-contained demo of the kuira-sdk-android #4 unshielded money-path fix,
 * deposited into the current Public vault. Nothing here touches the Public/Private flows.
 *
 * Three behaviours: the generated typed `depositUnshielded(color, amount)` call with auto-fund ON
 * (Layer 2) and OFF (Layer 1 typed error), plus an explicit-offer deposit (precedence).
 */
@Composable
fun MoneyPathScreen(viewModel: MoneyPathViewModel = hiltViewModel()) {
    val vaultAddress by viewModel.vaultAddress.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val callStage by viewModel.callStage.collectAsState()
    val result by viewModel.result.collectAsState()

    var amount by remember { mutableStateOf("1") }
    var autoFund by remember { mutableStateOf(true) }

    val amountBase = amount.trim().toBigIntegerOrNull()?.takeIf { it > BigInteger.ZERO }
        ?.multiply(BigInteger.TEN.pow(NIGHT_DECIMALS))
    val canDeposit = !busy && amountBase != null && vaultAddress != null

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Money Path", style = MaterialTheme.typography.headlineSmall, color = Steel.OnSteel)
        Text(
            "Exercises the unshielded money-path fix (kuira-sdk-android #4) against the Public vault: " +
                "the generated typed deposit call, the clear typed error, and an explicit sponsoring offer.",
            style = MaterialTheme.typography.bodyMedium, color = Steel.OnSteelDim,
        )

        if (vaultAddress == null) {
            InfoCard(
                tint = Warn,
                title = "No Public vault yet",
                body = "Deploy or connect a Public vault on the Public tab first — this tab deposits into it.",
            )
        } else {
            Text("Target vault: ${shortHex(vaultAddress!!)}", style = MaterialTheme.typography.bodySmall, color = Steel.OnSteelDim)
        }

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter(Char::isDigit) },
            label = { Text("Amount (NIGHT)") },
            singleLine = true,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        // ── Auto-fund toggle ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = autoFund,
                onCheckedChange = { autoFund = it },
                enabled = !busy,
                colors = SwitchDefaults.colors(checkedTrackColor = Steel.Accent),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (autoFund) "Auto-fund ON → SDK funds it (Layer 2)"
                else "Auto-fund OFF → no offer → Layer 1 typed error",
                style = MaterialTheme.typography.bodyMedium,
                color = if (autoFund) Steel.Accent else Warn,
            )
        }

        // ── Deposit via the GENERATED typed call (no manual offer) ──
        Button(
            onClick = { amountBase?.let { viewModel.depositAutoFund(it, autoFund) } },
            enabled = canDeposit,
            colors = ButtonDefaults.buttonColors(containerColor = Steel.Accent, contentColor = Steel.Ink),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Deposit — generated depositUnshielded(color, amount)") }

        // ── Explicit / sponsor offer (the escape hatch codegen can't express) ──
        OutlinedButton(
            onClick = { amountBase?.let { viewModel.depositSponsored(it) } },
            enabled = canDeposit,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Sponsor deposit — explicit offer (precedence)") }

        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Steel.Accent, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(callStage?.let { it::class.simpleName } ?: "Working…", color = Steel.OnSteelDim, style = MaterialTheme.typography.bodySmall)
            }
        }

        result?.let { r ->
            HorizontalDivider(color = Steel.Line)
            when (r) {
                is MoneyPathResult.Success -> InfoCard(Ok, "✓ ${r.kind}", r.detail, viewModel::clearResult)
                is MoneyPathResult.TypedError -> InfoCard(
                    Warn, "⚑ UnshieldedValueUnfunded (Layer 1)",
                    "The SDK threw a clear, typed error a dApp can catch:\n\n${r.detail}", viewModel::clearResult,
                )
                is MoneyPathResult.Failure -> InfoCard(Bad, "✗ Failed", r.detail, viewModel::clearResult)
            }
        }
    }
}

@Composable
private fun InfoCard(tint: Color, title: String, body: String, onDismiss: (() -> Unit)? = null) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Steel.Dark),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = tint)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Steel.OnSteelDim)
            if (onDismiss != null) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Dismiss") }
            }
        }
    }
}

/** First 8 / last 6 of a hex address for a compact label. */
private fun shortHex(addr: String): String =
    if (addr.length > 18) "${addr.take(8)}…${addr.takeLast(6)}" else addr

private val Ok = Color(0xFF7FC49A)   // green-steel — a landed deposit
private val Warn = Color(0xFFD9A441) // amber — the intended typed error / setup notice
private val Bad = Color(0xFFC47F86)  // red-steel — an unexpected failure
