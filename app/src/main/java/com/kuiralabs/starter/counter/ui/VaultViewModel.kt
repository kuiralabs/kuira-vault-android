package com.kuiralabs.starter.counter.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuiralabs.starter.counter.data.VaultContract
import com.kuiralabs.starter.counter.data.VaultStore
import com.midnight.kuira.core.compact.ContractCallException
import com.midnight.kuira.core.compact.ContractCallStage
import com.midnight.kuira.core.compact.MidnightContract
import com.midnight.kuira.core.crypto.address.Bech32m
import com.midnight.kuira.core.network.MidnightNetwork
import com.midnight.kuira.sdk.MidnightSdk
import com.midnight.kuira.sdk.walletruntime.MidnightSdkProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigInteger
import javax.inject.Inject

/**
 * Drives the Vault (multisig treasury) UI. State is read from CHAIN — threshold, treasury balance,
 * proposals (recipient/amount/status/approvals), and whether this wallet is a signer — via the
 * view-call read path. So a device that CONNECTS to a Vault it didn't deploy sees the same truth as
 * the deployer, and the live `_approvalCount` stream refreshes approvals as other signers act.
 * [VaultStore] persists only the address this device points at (+ a threshold hint for first paint).
 */
@HiltViewModel
class VaultViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sdkProvider: MidnightSdkProvider,
    private val vaultStore: VaultStore,
) : ViewModel() {

    private val _state = MutableStateFlow<VaultUiState>(VaultUiState.NotReady)
    val state: StateFlow<VaultUiState> = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _callStage = MutableStateFlow<ContractCallStage?>(null)
    val callStage: StateFlow<ContractCallStage?> = _callStage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val selectedNetwork: StateFlow<MidnightNetwork> get() = sdkProvider.selectedNetwork
    fun selectNetwork(network: MidnightNetwork) = sdkProvider.selectNetwork(network)

    private var refreshJob: Job? = null
    private var approvalStreamJob: Job? = null
    private var readHandle: MidnightContract? = null
    private var readHandleAddress: String? = null

    init {
        viewModelScope.launch {
            sdkProvider.sdk.combine(sdkProvider.selectedNetwork) { sdk, net -> sdk to net }
                .collect { (sdk, network) -> onSdkOrNetworkChanged(sdk, network) }
        }
    }

    private fun onSdkOrNetworkChanged(sdk: MidnightSdk?, network: MidnightNetwork) {
        val address = vaultStore.getAddress(network)
        when {
            sdk == null -> { _state.value = VaultUiState.NotReady; stopStreams() }
            address == null -> { _state.value = VaultUiState.ReadyToDeploy; stopStreams() }
            else -> { refresh(sdk, address, network); startApprovalStream(sdk, address) }
        }
    }

    /** Deploy a 3-signer Vault requiring [threshold] approvals (this wallet + two co-signers). */
    fun deploy(threshold: Int, coSignerHexKeys: List<String>) {
        val sdk = sdkProvider.sdk.value ?: return
        val network = sdkProvider.selectedNetwork.value
        val coSigners = (0 until 2).map { i ->
            val entry = coSignerHexKeys.getOrNull(i)?.trim().orEmpty()
            if (entry.length == 64 && entry.all { it in "0123456789abcdefABCDEF" }) hexToBytes(entry)
            else ByteArray(32) { (0xC0 + i).toByte() } // distinct placeholder co-signer
        }
        val signers = listOf(sdk.coinPublicKey) + coSigners
        runAction {
            val address = VaultContract.deploy(context, sdk, signers, threshold) { _callStage.value = it }
            vaultStore.save(network, address, threshold)
            onSdkOrNetworkChanged(sdk, network)
        }
    }

    /** Connect to a Vault someone else deployed by its [addressHex] (a co-signer joining). */
    fun connect(addressHex: String) {
        val sdk = sdkProvider.sdk.value ?: return
        val network = sdkProvider.selectedNetwork.value
        val address = addressHex.trim().lowercase()
        if (address.length != 64 || !address.all { it in "0123456789abcdef" }) {
            _error.value = "Enter a 64-hex Vault address"
            return
        }
        runAction {
            // Prove it's a readable Vault before saving (a bad address would leave a broken state).
            val handle = VaultContract.buildReadHandle(context, sdk, address)
            val threshold = VaultContract.getThreshold(handle)
            vaultStore.save(network, address, threshold)
            onSdkOrNetworkChanged(sdk, network)
        }
    }

    fun deposit(amountBase: BigInteger) = withVault { sdk, _, address ->
        VaultContract.depositUnshielded(context, sdk, address, NATIVE_COLOR, amountBase) { _callStage.value = it }
    }

    fun propose(recipient: String, amountBase: BigInteger) = withVault { sdk, _, address ->
        val hashHex = recipientToHashHex(recipient) ?: run {
            _error.value = "Invalid recipient — paste a wallet address or a 64-hex address hash"
            return@withVault
        }
        VaultContract.proposeWithdrawal(
            context, sdk, address,
            recipientAddressHash = hexToBytes(hashHex), color = NATIVE_COLOR, amount = amountBase,
        ) { _callStage.value = it }
    }

    /** A recipient is either a Bech32m wallet address (decode to its 32-byte hash) or the raw hash. */
    private fun recipientToHashHex(input: String): String? {
        val s = input.trim()
        if (s.length == 64 && s.all { it in "0123456789abcdefABCDEF" }) return s.lowercase()
        return runCatching { Bech32m.decode(s).second.toHex() }.getOrNull()?.takeIf { it.length == 64 }
    }

    fun approve(id: Long) = withVault { sdk, _, address ->
        VaultContract.approve(context, sdk, address, id) { _callStage.value = it }
    }

    fun execute(id: Long) = withVault { sdk, _, address ->
        val handle = readHandleFor(sdk, address)
        val p = VaultContract.getProposal(handle, id)
        VaultContract.execute(
            context, sdk, address,
            proposalId = id, recipientAddressHash = hexToBytes(p.recipientHashHex),
            color = NATIVE_COLOR, amount = p.amountBase,
        ) { _callStage.value = it }
    }

    /** Forget the current Vault → ReadyToDeploy (the on-chain contract isn't destroyed). */
    fun disconnect() {
        val network = sdkProvider.selectedNetwork.value
        vaultStore.clear(network)
        onSdkOrNetworkChanged(sdkProvider.sdk.value, network)
    }

    /** This wallet's own Bech32m address — a convenient default withdrawal recipient. */
    fun myAddress(): String? = sdkProvider.sdk.value?.walletAddress

    /** This wallet's signer public key, hex — share it so a deployer can add it as a co-signer. */
    fun mySignerKeyHex(): String? = sdkProvider.sdk.value?.coinPublicKey?.toHex()

    // ── chain reads ──

    private fun refresh(sdk: MidnightSdk, address: String, network: MidnightNetwork) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val prev = _state.value as? VaultUiState.Deployed
            if (prev != null) _state.value = prev.copy(refreshing = true)
            val loaded = loadWithIndexerRetry(sdk, address)
            if (loaded != null) {
                _state.value = loaded
                _error.value = null // a successful read clears any stale transient error
            } else if (prev != null) {
                _state.value = prev.copy(refreshing = false)
            }
        }
    }

    // A freshly-deployed (or just-connected) contract isn't in the indexer for a beat, so the first
    // read throws "Contract not found". Retry until it lands rather than surfacing a transient error.
    private suspend fun loadWithIndexerRetry(
        sdk: MidnightSdk,
        address: String,
        timeoutMs: Long = 60_000L,
    ): VaultUiState.Deployed? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            try {
                return loadFromChain(sdk, address)
            } catch (e: ContractCallException.StateFetchFailed) {
                if (System.currentTimeMillis() > deadline) {
                    _error.value = "Read failed: ${e.message}"
                    return null
                }
                delay(2_000) // contract not indexed yet
            } catch (e: Exception) {
                _error.value = "Read failed: ${e.message}"
                return null
            }
        }
    }

    private suspend fun loadFromChain(sdk: MidnightSdk, address: String): VaultUiState.Deployed {
        val handle = readHandleFor(sdk, address)
        val threshold = VaultContract.getThreshold(handle)
        val signerCount = VaultContract.getSignerCount(handle)
        val balance = VaultContract.getUnshieldedBalance(handle, NATIVE_COLOR)
        val canApprove = runCatching { VaultContract.isSignerByKey(handle, sdk.coinPublicKey) }.getOrDefault(false)
        val proposals = VaultContract.listProposals(handle).map { p ->
            ProposalView(
                id = p.id,
                recipientLabel = "…${p.proposal.recipientHashHex.takeLast(8)}",
                recipientHashHex = p.proposal.recipientHashHex,
                amount = p.proposal.amountBase,
                approvals = p.approvals,
                threshold = threshold,
                executed = p.proposal.status == VaultContract.PROPOSAL_STATUS_EXECUTED,
            )
        }.sortedByDescending { it.id }
        return VaultUiState.Deployed(
            address = address,
            threshold = threshold,
            signerCount = signerCount,
            treasuryBalance = balance,
            proposals = proposals,
            canApprove = canApprove,
        )
    }

    // The live _approvalCount stream just triggers a chain refresh when approvals change (e.g. a
    // co-signer on another device approves), so the X/N counts stay current without polling.
    private fun startApprovalStream(sdk: MidnightSdk, address: String) {
        approvalStreamJob?.cancel()
        approvalStreamJob = viewModelScope.launch {
            VaultContract.observeApprovalCounts(readHandleFor(sdk, address))
                .catch { /* non-fatal background read */ }
                .collect { refresh(sdk, address, sdkProvider.selectedNetwork.value) }
        }
    }

    private fun stopStreams() {
        refreshJob?.cancel(); refreshJob = null
        approvalStreamJob?.cancel(); approvalStreamJob = null
        readHandle = null; readHandleAddress = null
    }

    private fun readHandleFor(sdk: MidnightSdk, address: String): MidnightContract {
        if (readHandle == null || readHandleAddress != address) {
            readHandle = VaultContract.buildReadHandle(context, sdk, address)
            readHandleAddress = address
        }
        return readHandle!!
    }

    // ── plumbing ──

    private fun withVault(block: suspend (MidnightSdk, MidnightNetwork, String) -> Unit) {
        val sdk = sdkProvider.sdk.value ?: return
        val network = sdkProvider.selectedNetwork.value
        val address = vaultStore.getAddress(network) ?: return
        runAction {
            block(sdk, network, address)
            refresh(sdk, address, network) // re-read chain truth after the tx finalizes
        }
    }

    private fun runAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                block()
            } catch (t: Throwable) {
                _error.value = t.message ?: t::class.simpleName ?: "Unknown error"
            } finally {
                _busy.value = false
                _callStage.value = null
            }
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private companion object {
        val NATIVE_COLOR = ByteArray(32) // 32 zero bytes = native NIGHT
    }
}
