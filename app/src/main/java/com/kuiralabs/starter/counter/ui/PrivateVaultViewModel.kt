package com.kuiralabs.starter.counter.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuiralabs.starter.counter.data.PrivateVaultContract
import com.kuiralabs.starter.counter.data.PrivateVaultCrypto
import com.kuiralabs.starter.counter.data.PrivateVaultStore
import com.kuiralabs.starter.counter.data.PrivateVaultValidation
import com.midnight.kuira.core.compact.ContractCallStage
import com.midnight.kuira.core.compact.MidnightContract
import com.midnight.kuira.core.crypto.address.Bech32m
import com.midnight.kuira.core.network.MidnightNetwork
import com.midnight.kuira.sdk.MidnightSdk
import com.midnight.kuira.sdk.walletruntime.MidnightSdkProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import java.math.BigInteger
import javax.inject.Inject

/**
 * Drives the PRIVATE Vault UI — the selective-disclosure sibling of [VaultViewModel]. The public
 * ledger holds only commitments + ciphertext, so this device reads status + approval counts from
 * chain but DECRYPTS proposal contents locally with the vault viewing key. Membership material
 * (viewing key, salts, threshold salt) lives in [PrivateVaultStore]; you reach [Member] state only
 * by CREATING a vault (you hold the invites to share) or JOINING via an invite.
 */
@HiltViewModel
class PrivateVaultViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sdkProvider: MidnightSdkProvider,
    private val store: PrivateVaultStore,
) : ViewModel() {

    private val _state = MutableStateFlow<PrivateVaultUiState>(PrivateVaultUiState.NotReady)
    val state: StateFlow<PrivateVaultUiState> = _state.asStateFlow()

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
        val membership = store.get(network)
        when {
            sdk == null -> { _state.value = PrivateVaultUiState.NotReady; stopStreams() }
            membership == null -> { _state.value = PrivateVaultUiState.ReadyToStart; stopStreams() }
            else -> { refresh(sdk, membership, network); startApprovalStream(sdk, membership.address) }
        }
    }

    /**
     * Create a private vault: this wallet + up to 4 co-signers (by their coin-key hex), requiring
     * [threshold] approvals. Mints the viewing key + per-signer salts, computes the committed roster
     * via the contract's own circuits, deploys, and stores this device's material + the invites to
     * share. A blank co-signer slot is dropped; a non-blank entry that isn't 64-hex is rejected.
     */
    fun create(threshold: Int, coSignerHexKeys: List<String>) {
        val sdk = sdkProvider.sdk.value ?: return
        val network = sdkProvider.selectedNetwork.value
        val plan = PrivateVaultValidation.planCreate(coSignerHexKeys, threshold, PrivateVaultContract.ROSTER_SIZE)
        val coSigners = when (plan) {
            is PrivateVaultValidation.CreatePlan.Invalid -> { _error.value = plan.error; return }
            is PrivateVaultValidation.CreatePlan.Valid -> plan.coSignerKeys
        }
        val signers = listOf(sdk.coinPublicKey) + coSigners
        runAction {
            val result = PrivateVaultContract.deploy(context, sdk, signers, threshold) { _callStage.value = it }
            store.save(network, PrivateVaultStore.Membership(
                address = result.address,
                viewingKey = result.viewingKey,
                threshold = result.threshold,
                signerCount = result.signerCount,
                thresholdSalt = result.thresholdSalt,
                memberSalt = result.creatorSalt,
                isCreator = true,
                invites = result.coSignerInvites,
            ))
            onSdkOrNetworkChanged(sdk, network)
        }
    }

    /** Join a vault someone else created, by pasting the invite they shared. */
    fun join(inviteText: String) {
        val sdk = sdkProvider.sdk.value ?: return
        val network = sdkProvider.selectedNetwork.value
        val invite = runCatching { PrivateVaultCrypto.decodeInvite(inviteText) }.getOrNull() ?: run {
            _error.value = "That doesn't look like a valid vault invite — paste the exact text you were sent."
            return
        }
        runAction {
            val membership = PrivateVaultStore.Membership(
                address = invite.vaultAddress,
                viewingKey = invite.viewingKey,
                threshold = invite.threshold,
                signerCount = invite.signerCount,
                thresholdSalt = invite.thresholdSalt,
                memberSalt = invite.memberSalt,
                isCreator = false,
                invites = emptyList(),
            )
            // Read once before saving, so a bad/indexer-lagging invite doesn't leave a broken state.
            val loaded = loadWithIndexerRetry(sdk, membership) ?: return@runAction
            store.save(network, membership)
            _state.value = loaded
            _error.value = null
            startApprovalStream(sdk, membership.address)
        }
    }

    fun deposit(amountBase: BigInteger) = withVault { sdk, _, m ->
        PrivateVaultContract.depositUnshielded(context, sdk, m.address, NATIVE_COLOR, amountBase) { _callStage.value = it }
    }

    fun propose(recipient: String, amountBase: BigInteger) = withVault { sdk, _, m ->
        val hashHex = recipientToHashHex(sdk, recipient) ?: run {
            _error.value = "Recipient must be an unshielded wallet address on the current network " +
                "(same prefix as your own address)."
            return@withVault
        }
        PrivateVaultContract.proposeWithdrawal(
            context, sdk, m.address,
            recipientAddressHash = hexToBytes(hashHex), color = NATIVE_COLOR, amount = amountBase,
            viewingKey = m.viewingKey, memberSalt = m.memberSalt,
        ) { _callStage.value = it }
    }

    private fun recipientToHashHex(sdk: MidnightSdk, input: String): String? {
        val myHrp = runCatching { Bech32m.decode(sdk.walletAddress).first }.getOrNull() ?: return null
        val (hrp, payload) = runCatching { Bech32m.decode(input.trim()) }.getOrNull() ?: return null
        if (hrp != myHrp || payload.size != 32) return null
        return payload.toHex()
    }

    fun approve(id: Long) = withVault { sdk, network, m ->
        PrivateVaultContract.approve(context, sdk, m.address, id, m.memberSalt) { _callStage.value = it }
        store.markApproved(network, m.address, id, true)
    }

    fun revoke(id: Long) = withVault { sdk, network, m ->
        PrivateVaultContract.revokeApproval(context, sdk, m.address, id, m.memberSalt) { _callStage.value = it }
        store.markApproved(network, m.address, id, false)
    }

    fun execute(id: Long) = withVault { sdk, _, m ->
        PrivateVaultContract.execute(
            context, sdk, m.address, proposalId = id,
            viewingKey = m.viewingKey, threshold = m.threshold, thresholdSalt = m.thresholdSalt,
        ) { _callStage.value = it }
    }

    /** Forget this vault on this device → ReadyToStart (the on-chain contract isn't destroyed). */
    fun disconnect() {
        val network = sdkProvider.selectedNetwork.value
        store.clear(network, store.get(network)?.address)
        onSdkOrNetworkChanged(sdkProvider.sdk.value, network)
    }

    fun myAddress(): String? = sdkProvider.sdk.value?.walletAddress
    fun mySignerKeyHex(): String? = sdkProvider.sdk.value?.coinPublicKey?.toHex()

    // ── chain reads ──

    private fun refresh(sdk: MidnightSdk, m: PrivateVaultStore.Membership, network: MidnightNetwork) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val prev = _state.value as? PrivateVaultUiState.Member
            if (prev != null) _state.value = prev.copy(refreshing = true)
            val loaded = loadWithIndexerRetry(sdk, m)
            if (loaded != null) {
                _state.value = loaded
                _error.value = null
            } else if (prev != null) {
                _state.value = prev.copy(refreshing = false)
            }
        }
    }

    private suspend fun loadWithIndexerRetry(
        sdk: MidnightSdk,
        m: PrivateVaultStore.Membership,
        timeoutMs: Long = 60_000L,
    ): PrivateVaultUiState.Member? {
        return try {
            loadFromChain(sdk, m, notIndexedTimeoutMs = timeoutMs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Private vault chain read failed", e)
            _error.value = "Read failed: ${e.message}"
            null
        }
    }

    private suspend fun loadFromChain(
        sdk: MidnightSdk,
        m: PrivateVaultStore.Membership,
        notIndexedTimeoutMs: Long = 0,
    ): PrivateVaultUiState.Member {
        val network = sdkProvider.selectedNetwork.value
        val handle = readHandleFor(sdk, m.address)
        // ONE batched snapshot (balance + decrypted proposals) against one chain state, with the
        // indexer-lag retry on the header so a just-created/joined vault is waited through.
        val snap = PrivateVaultContract.loadSnapshot(handle, m.viewingKey, NATIVE_COLOR, notIndexedTimeoutMs)

        val myHrp = Bech32m.decode(sdk.walletAddress).first
        val myHashHex = Bech32m.decode(sdk.walletAddress).second.toHex()
        val approvedLocally = store.approvedIds(network, m.address)
        val proposals = snap.proposals.map { p ->
            val executed = p.status == PrivateVaultContract.STATUS_EXECUTED
            PrivateProposalView(
                id = p.id,
                // An unreadable proposal (garbage/wrong-key payload) has no decodable recipient.
                recipientAddress = if (p.readable) Bech32m.encode(myHrp, hexToBytes(p.recipientHashHex)) else "",
                recipientIsMe = p.readable && p.recipientHashHex.equals(myHashHex, ignoreCase = true),
                amount = p.amount,
                approvals = p.approvals,
                threshold = m.threshold,
                executed = executed,
                approvedByMe = !executed && approvedLocally.contains(p.id),
                readable = p.readable,
            )
        }.sortedByDescending { it.id }

        return PrivateVaultUiState.Member(
            address = m.address,
            threshold = m.threshold,
            signerCount = m.signerCount,
            treasuryBalance = snap.balance,
            proposals = proposals,
            isCreator = m.isCreator,
            invites = m.invites,
        )
    }

    private fun startApprovalStream(sdk: MidnightSdk, address: String) {
        approvalStreamJob?.cancel()
        approvalStreamJob = viewModelScope.launch {
            val network = sdkProvider.selectedNetwork.value
            PrivateVaultContract.observeApprovalCounts(readHandleFor(sdk, address))
                .retryWhen { e, _ ->
                    Log.w(TAG, "approval stream error — restarting in 5s", e)
                    delay(5_000)
                    true
                }
                .collect {
                    val m = store.get(network) ?: return@collect
                    refresh(sdk, m, network)
                }
        }
    }

    private fun stopStreams() {
        refreshJob?.cancel(); refreshJob = null
        approvalStreamJob?.cancel(); approvalStreamJob = null
        readHandle = null; readHandleAddress = null
    }

    private fun readHandleFor(sdk: MidnightSdk, address: String): MidnightContract {
        if (readHandle == null || readHandleAddress != address) {
            readHandle = PrivateVaultContract.buildReadHandle(context, sdk, address)
            readHandleAddress = address
        }
        return readHandle!!
    }

    // ── plumbing ──

    private fun withVault(block: suspend (MidnightSdk, MidnightNetwork, PrivateVaultStore.Membership) -> Unit) {
        val sdk = sdkProvider.sdk.value ?: return
        val network = sdkProvider.selectedNetwork.value
        val membership = store.get(network) ?: return
        runAction {
            block(sdk, network, membership)
            refresh(sdk, membership, network)
        }
    }

    private fun runAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                block()
            } catch (t: Throwable) {
                Log.e(TAG, "Private vault action failed", t)
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
        const val TAG = "PrivateVaultVM"
        val NATIVE_COLOR = ByteArray(32)
    }
}
