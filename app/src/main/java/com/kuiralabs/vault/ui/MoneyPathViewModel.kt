package com.kuiralabs.vault.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuiralabs.vault.data.VaultContract
import com.kuiralabs.vault.data.VaultStore
import com.midnight.kuira.core.compact.ContractCallException
import com.midnight.kuira.core.compact.ContractCallStage
import com.midnight.kuira.core.network.MidnightNetwork
import com.midnight.kuira.sdk.MidnightSdk
import com.midnight.kuira.sdk.walletruntime.MidnightSdkProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigInteger
import javax.inject.Inject

/**
 * Drives the "Money Path" demo tab — a self-contained showcase of the kuira-sdk-android #4
 * unshielded money-path fix, deposited into the current PUBLIC vault. It never touches the
 * Public/Private flows: it only calls the additive [VaultContract.depositUnshieldedAutoFund]
 * (no explicit offer) and the existing explicit-offer [VaultContract.depositUnshielded].
 *
 * Three behaviours, one screen:
 *  - auto-fund ON  + no offer  → Layer 2: the SDK funds the deposit from the wallet.
 *  - auto-fund OFF + no offer  → Layer 1: [ContractCallException.UnshieldedValueUnfunded], caught
 *    and shown as a clear typed error.
 *  - explicit offer            → precedence: the app's offer wins over auto-fund.
 */
@HiltViewModel
class MoneyPathViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sdkProvider: MidnightSdkProvider,
    private val vaultStore: VaultStore,
) : ViewModel() {

    /** The Public vault this device points at on the selected network (the deposit target). */
    private val _vaultAddress = MutableStateFlow<String?>(null)
    val vaultAddress: StateFlow<String?> = _vaultAddress.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _callStage = MutableStateFlow<ContractCallStage?>(null)
    val callStage: StateFlow<ContractCallStage?> = _callStage.asStateFlow()

    private val _result = MutableStateFlow<MoneyPathResult?>(null)
    val result: StateFlow<MoneyPathResult?> = _result.asStateFlow()

    val selectedNetwork: StateFlow<MidnightNetwork> get() = sdkProvider.selectedNetwork

    init {
        viewModelScope.launch {
            sdkProvider.sdk.combine(sdkProvider.selectedNetwork) { _, net -> net }
                .collect { net -> _vaultAddress.value = vaultStore.getAddress(net) }
        }
    }

    /** Deposit with NO explicit offer. [autoFund] on → Layer 2; off → Layer 1 typed error. */
    fun depositAutoFund(amountBase: BigInteger, autoFund: Boolean) = run { sdk, address ->
        VaultContract.depositUnshieldedAutoFund(
            context, sdk, address, NATIVE_COLOR, amountBase, autoFund,
        ) { _callStage.value = it }
        _result.value = MoneyPathResult.Success(
            kind = "Auto-funded deposit (Layer 2)",
            detail = "The generated typed call depositUnshielded(color, amount) attached NO funding " +
                "offer — the SDK selected wallet NIGHT UTXOs, built + signed the offer, and the " +
                "deposit landed. Pre-#4 this exact codegen call failed with node error 138.",
        )
    }

    /** Deposit with an explicit funding offer — proves precedence (explicit wins over auto-fund). */
    fun depositSponsored(amountBase: BigInteger) = run { sdk, address ->
        VaultContract.depositUnshielded(
            context, sdk, address, NATIVE_COLOR, amountBase,
        ) { _callStage.value = it }
        _result.value = MoneyPathResult.Success(
            kind = "Explicit-offer deposit (precedence)",
            detail = "The app built its own funding offer; it took precedence over auto-fund " +
                "and the deposit landed. This is the sponsoring / custom-offer path.",
        )
    }

    fun clearResult() { _result.value = null }

    private fun run(block: suspend (MidnightSdk, String) -> Unit) {
        val sdk = sdkProvider.sdk.value ?: run {
            _result.value = MoneyPathResult.Failure("Wallet not ready — unlock the sigil first.")
            return
        }
        val network = sdkProvider.selectedNetwork.value
        val address = vaultStore.getAddress(network) ?: run {
            _result.value = MoneyPathResult.Failure(
                "No Public vault on $network yet — deploy or connect one on the Public tab first.",
            )
            return
        }
        viewModelScope.launch {
            _busy.value = true
            _result.value = null
            try {
                block(sdk, address)
            } catch (e: ContractCallException.UnshieldedValueUnfunded) {
                // Layer 1 — the SDK's clear, typed error (auto-fund off, no offer supplied).
                _result.value = MoneyPathResult.TypedError(
                    e.message ?: "The deposit moved unshielded value with no funding offer.",
                )
            } catch (t: Throwable) {
                Log.e(TAG, "money-path deposit failed", t)
                _result.value = MoneyPathResult.Failure(
                    t.message ?: t::class.simpleName ?: "Unknown error",
                )
            } finally {
                _busy.value = false
                _callStage.value = null
            }
        }
    }

    companion object {
        private const val TAG = "MoneyPathVM"

        /** 32 zero bytes = native NIGHT (matches VaultViewModel.NATIVE_COLOR). */
        private val NATIVE_COLOR = ByteArray(32)
    }
}

/** Outcome of a money-path deposit, shaped for a clear result card. */
sealed interface MoneyPathResult {
    /** A deposit that landed — [kind] names which path (auto-fund or explicit). */
    data class Success(val kind: String, val detail: String) : MoneyPathResult

    /** Layer-1 [ContractCallException.UnshieldedValueUnfunded] — the typed, catchable error. */
    data class TypedError(val detail: String) : MoneyPathResult

    /** Any other failure (wallet not ready, insufficient funds, node error, …). */
    data class Failure(val detail: String) : MoneyPathResult
}
