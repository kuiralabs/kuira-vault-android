package com.kuiralabs.starter.counter.data

import android.content.Context
import com.midnight.kuira.core.compact.CircuitExecutionException
import com.midnight.kuira.core.compact.ContractCallStage
import com.midnight.kuira.core.compact.MidnightContract
import com.midnight.kuira.core.compact.proving.ProvingKeyManager
import com.midnight.kuira.sdk.MidnightSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

/**
 * App-side wrapper for the PRIVATE Vault — the selective-disclosure sibling of [VaultContract].
 *
 * The public ledger holds only commitments + opaque ciphertext, so this layer does the disclosure
 * work the public vault never had to:
 *  - commitment VALUES come from the contract's OWN pure circuits via [MidnightContract.readLocal]
 *    (no deployed instance needed) — so the app's hashes can never diverge from the contract's.
 *  - proposal contents are encrypted under the vault viewing key ([PrivateVaultCrypto]) into the
 *    contract's opaque payload; the executor decrypts to re-present the preimage `execute` demands.
 *  - deploy mints the viewing key + per-signer secret salts + threshold salt and returns the
 *    invites members join with.
 *
 * Membership is proven by the caller's own key + their secret salt (never on-chain), so nothing
 * here reveals who a signer is. Tier 1 keeps unshielded custody: `execute` still reveals
 * recipient + amount in the transaction (the Tier 2 shielded treasury closes that).
 */
internal object PrivateVaultContract {

    private const val NAME = "private-vault"
    // Dedicated asset dirs (synced by the app's syncPrivateVault* Gradle tasks) so the private
    // vault's keys stay separate from the public vault's plugin-synced runtime/ + keys/.
    private const val CONTRACT_JS_ASSET = "private-vault-runtime/$NAME-contract.js"
    private const val KEYS_DIR = "private-vault-keys"
    const val ROSTER_SIZE = 5   // matches PrivateVault.compact's fixed Vector<5>

    private fun loadAllVerifierKeys(context: Context): Map<String, ByteArray> {
        val files = context.assets.list(KEYS_DIR).orEmpty().filter { it.endsWith(".verifier") }
        return files.associate { file ->
            file.removeSuffix(".verifier") to context.assets.open("$KEYS_DIR/$file").use { it.readBytes() }
        }
    }

    private fun buildHandle(
        context: Context,
        sdk: MidnightSdk,
        address: String?,
        forWrite: Boolean,
        constructorArgs: List<Any?> = emptyList(),
    ): MidnightContract = MidnightContract.create(sdk.config) {
        name = NAME
        contractJs = context.assets.open(CONTRACT_JS_ASSET)
        if (address != null) this.address = address
        if (forWrite) {
            coinPublicKey = sdk.coinPublicKey
            circuitVerifierKeys = loadAllVerifierKeys(context)
        }
        if (constructorArgs.isNotEmpty()) this.constructorArgs = constructorArgs
    }

    // Shape-valid placeholder constructor args for CALLS/READS (values discarded — see VaultContract).
    private fun callConstructorArgs(): List<Any?> = listOf(
        List(ROSTER_SIZE) { i -> ByteArray(32) { (i + 1).toByte() } },
        ByteArray(32) { 0x7f },
    )

    private fun hexToBytes(hex: String): ByteArray {
        val h = hex.removePrefix("0x")
        return ByteArray(h.length / 2) { ((h[it * 2].digitToInt(16) shl 4) or h[it * 2 + 1].digitToInt(16)).toByte() }
    }

    /** Parse a readLocal/read Bytes<32> result (a quoted hex string) to raw bytes. */
    private fun readBytes(json: String): ByteArray = hexToBytes(jsonScalar(json))

    // ── Commitments: computed by the contract's own pure circuits (readLocal), no deploy needed ──

    private suspend fun signerCommitment(h: MidnightContract, pk: ByteArray, salt: ByteArray): ByteArray =
        readBytes(h.readLocal("signerCommitment", pk, salt))

    private suspend fun thresholdCommitment(h: MidnightContract, threshold: Int, salt: ByteArray): ByteArray =
        readBytes(h.readLocal("thresholdCommitment", BigInteger.valueOf(threshold.toLong()), salt))

    private suspend fun proposalCommitment(h: MidnightContract, p: PrivateVaultCrypto.Preimage): ByteArray =
        readBytes(h.readLocal(
            "proposalCommitment", p.recipientIsContract, p.recipient, p.color, p.amount, p.nonce,
        ))

    /** Everything deploy mints: the vault address plus the material members need to join and act. */
    data class DeployResult(
        val address: String,
        val viewingKey: ByteArray,
        val threshold: Int,
        val signerCount: Int,
        val thresholdSalt: ByteArray,
        /** The deployer's own (signer 0) secret salt. */
        val creatorSalt: ByteArray,
        /** One ready-to-share invite per co-signer (indices 1..n of [signerCoinPublicKeys]). */
        val coSignerInvites: List<String>,
    )

    /**
     * Deploy a private vault over [signerCoinPublicKeys] (the deployer first) with [threshold].
     * Mints the viewing key, a secret salt per signer, and the threshold salt; computes the
     * committed roster (padded to [ROSTER_SIZE] with random dummies) via the contract's own pure
     * circuits; deploys; and returns the co-signer invites.
     */
    suspend fun deploy(
        context: Context,
        sdk: MidnightSdk,
        signerCoinPublicKeys: List<ByteArray>,
        threshold: Int,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ): DeployResult {
        require(signerCoinPublicKeys.size in 1..ROSTER_SIZE) { "1..$ROSTER_SIZE signers" }
        require(threshold in 1..signerCoinPublicKeys.size) { "threshold must be 1..signerCount" }
        ProvingKeyManager(context).installCircuitKeysFromAssets(KEYS_DIR)

        val viewingKey = PrivateVaultCrypto.newViewingKey()
        val thresholdSalt = PrivateVaultCrypto.newSalt()
        val salts = signerCoinPublicKeys.map { PrivateVaultCrypto.newSalt() }

        // A local handle (no address) computes commitments against initialState(). It needs
        // shape-valid constructor args: readLocal runs initialState(), which VALIDATES the ctor arg
        // shapes even though a pure commitment circuit never touches state (same as the read path).
        val local = buildHandle(context, sdk, address = null, forWrite = false, constructorArgs = callConstructorArgs())
        val realCommitments = signerCoinPublicKeys.indices.map { i ->
            signerCommitment(local, signerCoinPublicKeys[i], salts[i])
        }
        val roster = (realCommitments + List(ROSTER_SIZE - realCommitments.size) {
            PrivateVaultCrypto.newDummyCommitment()
        })
        val thresholdCommit = thresholdCommitment(local, threshold, thresholdSalt)

        val writeHandle = buildHandle(
            context, sdk, address = null, forWrite = true,
            constructorArgs = listOf(roster, thresholdCommit),
        )
        val address = writeHandle.deploy(onProgress = onProgress).contractAddress

        val signerCount = signerCoinPublicKeys.size
        val invites = (1 until signerCount).map { i ->
            PrivateVaultCrypto.encodeInvite(
                PrivateVaultCrypto.Invite(address, viewingKey, threshold, signerCount, thresholdSalt, salts[i]),
            )
        }
        return DeployResult(address, viewingKey, threshold, signerCount, thresholdSalt, salts[0], invites)
    }

    /** Deposit is identical to the public vault — value isn't hidden in Tier 1. */
    suspend fun depositUnshielded(
        context: Context, sdk: MidnightSdk, address: String, color: ByteArray, amount: BigInteger,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        ProvingKeyManager(context).installCircuitKeysFromAssets(KEYS_DIR)
        val handle = buildHandle(context, sdk, address, forWrite = true, constructorArgs = callConstructorArgs())
        val fundingJson = sdk.buildUnshieldedFundingJson(amount, tokenType = color.toHex())
        handle.call("pvDepositUnshielded", color, amount, onProgress = onProgress, unshieldedFundingJson = fundingJson)
    }

    /**
     * Propose a private withdrawal. Commits to (recipient, amount, nonce) and stores the ciphertext
     * of that preimage; only the commitment + opaque payload reach the chain. [memberSalt] proves
     * the caller is a signer without revealing which one.
     */
    suspend fun proposeWithdrawal(
        context: Context, sdk: MidnightSdk, address: String,
        recipientAddressHash: ByteArray, color: ByteArray, amount: BigInteger,
        viewingKey: ByteArray, memberSalt: ByteArray,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        ProvingKeyManager(context).installCircuitKeysFromAssets(KEYS_DIR)
        val handle = buildHandle(context, sdk, address, forWrite = true, constructorArgs = callConstructorArgs())
        val preimage = PrivateVaultCrypto.Preimage(
            recipientIsContract = false, recipient = recipientAddressHash, color = color,
            amount = amount, nonce = PrivateVaultCrypto.newSalt(),
        )
        val commitment = proposalCommitment(handle, preimage)
        val payload = PrivateVaultCrypto.encryptPreimage(viewingKey, preimage)
        handle.call("pvProposeWithdrawal", commitment, payload, memberSalt, onProgress = onProgress)
    }

    suspend fun approve(
        context: Context, sdk: MidnightSdk, address: String, proposalId: Long, memberSalt: ByteArray,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        ProvingKeyManager(context).installCircuitKeysFromAssets(KEYS_DIR)
        val handle = buildHandle(context, sdk, address, forWrite = true, constructorArgs = callConstructorArgs())
        handle.call("pvApprove", BigInteger.valueOf(proposalId), memberSalt, onProgress = onProgress)
    }

    suspend fun revokeApproval(
        context: Context, sdk: MidnightSdk, address: String, proposalId: Long, memberSalt: ByteArray,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        ProvingKeyManager(context).installCircuitKeysFromAssets(KEYS_DIR)
        val handle = buildHandle(context, sdk, address, forWrite = true, constructorArgs = callConstructorArgs())
        handle.call("pvRevokeApproval", BigInteger.valueOf(proposalId), memberSalt, onProgress = onProgress)
    }

    /**
     * Execute proposal [proposalId]. Decrypts the on-chain payload to recover the preimage the
     * `execute` circuit demands, then presents it alongside the threshold preimage. Permissionless
     * once the count meets threshold — any member with the viewing key + threshold salt can settle.
     */
    suspend fun execute(
        context: Context, sdk: MidnightSdk, address: String, proposalId: Long,
        viewingKey: ByteArray, threshold: Int, thresholdSalt: ByteArray,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        ProvingKeyManager(context).installCircuitKeysFromAssets(KEYS_DIR)
        val handle = buildHandle(context, sdk, address, forWrite = true, constructorArgs = callConstructorArgs())
        val payload = readBytes(handle.read("getProposalPayload", BigInteger.valueOf(proposalId)))
        val p = PrivateVaultCrypto.decryptPreimage(viewingKey, payload)

        val withdrawalJson = sdk.buildUnshieldedWithdrawalJson(
            recipientAddressHash = p.recipient, amount = p.amount, tokenType = p.color.toHex(),
        )
        handle.call(
            "pvExecute", BigInteger.valueOf(proposalId), p.recipientIsContract, p.recipient, p.color,
            p.amount, p.nonce, BigInteger.valueOf(threshold.toLong()), thresholdSalt,
            onProgress = onProgress, unshieldedWithdrawalJson = withdrawalJson,
        )
    }

    fun buildReadHandle(context: Context, sdk: MidnightSdk, address: String): MidnightContract =
        buildHandle(context, sdk, address, forWrite = false, constructorArgs = callConstructorArgs())

    /**
     * Live per-proposal approval counts from the PUBLIC `_approvalCount` ledger map — the one
     * governance value that changes when ANOTHER member's device approves. Approval identities stay
     * hidden (tags), only the counts are public by design. Emits on each on-chain change so the UI
     * re-reads (and re-decrypts) the moment a co-signer acts. Mirrors the public vault's stream.
     */
    fun observeApprovalCounts(handle: MidnightContract): Flow<Map<Long, Int>> =
        handle.observeLedger().map { ledger ->
            val raw = ledger.getRawOrNull("_approvalCount") as? Map<*, *> ?: return@map emptyMap()
            raw.entries.mapNotNull { (k, v) ->
                val id = (k as? Number)?.toLong() ?: (k as? String)?.toLongOrNull() ?: return@mapNotNull null
                val n = (v as? Number)?.toInt() ?: (v as? String)?.toIntOrNull() ?: return@mapNotNull null
                id to n
            }.toMap()
        }

    // ── Reads: a member decrypts each proposal from chain state ──

    /** A private proposal as a member sees it: chain-truth status/count + the DECRYPTED contents. */
    data class MemberProposal(
        val id: Long,
        val recipientHashHex: String,
        val amount: BigInteger,
        val status: Int,
        val approvals: Int,
        /** False if this device's viewing key couldn't decrypt the payload (garbage/wrong-key proposal). */
        val readable: Boolean = true,
    )

    /** Treasury balance for [color] — a single view-call read (the batched path is [loadSnapshot]). */
    suspend fun getUnshieldedBalance(handle: MidnightContract, color: ByteArray): BigInteger =
        BigInteger(jsonScalar(handle.read("getUnshieldedBalance", color)))

    /** The whole member view, read in batched snapshots: treasury balance + decrypted proposals. */
    data class MemberSnapshot(val balance: BigInteger, val proposals: List<MemberProposal>)

    /**
     * Load the whole member view in batched reads. The HEADER batch (proposal count + treasury
     * balance) carries [notIndexedTimeoutMs] so a freshly-created/joined contract that lags the
     * indexer is waited through — not misreported as "read failed" or "0 proposals". A second batch
     * then reads status + approval count + payload per id (ids contiguous from 0), decrypting each
     * payload with [viewingKey]. All reads in a batch see one chain snapshot.
     */
    suspend fun loadSnapshot(
        handle: MidnightContract, viewingKey: ByteArray, color: ByteArray, notIndexedTimeoutMs: Long = 0,
    ): MemberSnapshot {
        val header = handle.readMany(
            listOf(
                MidnightContract.ReadRequest("count", "getProposalCount"),
                MidnightContract.ReadRequest("balance", "getUnshieldedBalance", listOf(color)),
            ),
            notIndexedTimeoutMs,
        )
        fun ok(map: Map<String, MidnightContract.ReadOutcome>, key: String): String {
            val r = map[key] ?: error("missing read '$key'")
            r.error?.let { throw CircuitExecutionException("PrivateVault read '$key' failed: $it") }
            return r.json!!
        }
        val count = jsonScalar(ok(header, "count")).toLong()
        val balance = BigInteger(jsonScalar(ok(header, "balance")))
        if (count == 0L) return MemberSnapshot(balance, emptyList())

        val results = handle.readMany((0 until count).flatMap { id ->
            listOf(
                MidnightContract.ReadRequest("s:$id", "getProposalStatus", listOf(BigInteger.valueOf(id))),
                MidnightContract.ReadRequest("a:$id", "getApprovalCount", listOf(BigInteger.valueOf(id))),
                MidnightContract.ReadRequest("y:$id", "getProposalPayload", listOf(BigInteger.valueOf(id))),
            )
        }, notIndexedTimeoutMs)
        val proposals = (0 until count).map { id ->
            val status = jsonScalar(ok(results, "s:$id")).toInt()
            val approvals = jsonScalar(ok(results, "a:$id")).toInt()
            // A proposal's commitment and ciphertext are independent contract args, so a signer CAN
            // store a garbage/wrong-key payload. Guard EACH decrypt so one unreadable proposal can't
            // fail the whole snapshot (a view-DoS) — surface it as an unreadable row instead.
            val decrypted = runCatching {
                PrivateVaultCrypto.decryptPreimage(viewingKey, hexToBytes(jsonScalar(ok(results, "y:$id"))))
            }.getOrNull()
            if (decrypted != null) {
                MemberProposal(id, decrypted.recipient.toHex(),
                    decrypted.amount, status, approvals, readable = true)
            } else {
                MemberProposal(id, "", BigInteger.ZERO, status, approvals, readable = false)
            }
        }
        return MemberSnapshot(balance, proposals)
    }

    // PrivateProposalStatus ordinals: Inactive=0, Active=1, Executed=2.
    const val STATUS_ACTIVE = 1
    const val STATUS_EXECUTED = 2

    private fun jsonScalar(json: String): String = org.json.JSONTokener(json).nextValue().toString()
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
