package com.kuiralabs.starter.counter.data

import android.content.Context
import com.midnight.kuira.core.compact.CircuitExecutionException
import com.midnight.kuira.core.compact.ContractCallStage
import com.midnight.kuira.core.compact.MidnightContract
import com.midnight.kuira.core.compact.proving.ProvingKeyManager
import com.midnight.kuira.sdk.MidnightSdk
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
    private const val CONTRACT_JS_ASSET = "runtime/$NAME-contract.js"
    private const val KEYS_DIR = "keys"
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
        ProvingKeyManager(context).installCircuitKeysFromAssets()

        val viewingKey = PrivateVaultCrypto.newViewingKey()
        val thresholdSalt = PrivateVaultCrypto.newSalt()
        val salts = signerCoinPublicKeys.map { PrivateVaultCrypto.newSalt() }

        // A local handle (no address) computes commitments against initialState().
        val local = buildHandle(context, sdk, address = null, forWrite = false)
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

        val invites = (1 until signerCoinPublicKeys.size).map { i ->
            PrivateVaultCrypto.encodeInvite(
                PrivateVaultCrypto.Invite(address, viewingKey, threshold, thresholdSalt, salts[i]),
            )
        }
        return DeployResult(address, viewingKey, threshold, thresholdSalt, salts[0], invites)
    }

    /** Deposit is identical to the public vault — value isn't hidden in Tier 1. */
    suspend fun depositUnshielded(
        context: Context, sdk: MidnightSdk, address: String, color: ByteArray, amount: BigInteger,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        ProvingKeyManager(context).installCircuitKeysFromAssets()
        val handle = buildHandle(context, sdk, address, forWrite = true, constructorArgs = callConstructorArgs())
        val fundingJson = sdk.buildUnshieldedFundingJson(amount, tokenType = color.toHex())
        handle.call("depositUnshielded", color, amount, onProgress = onProgress, unshieldedFundingJson = fundingJson)
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
        ProvingKeyManager(context).installCircuitKeysFromAssets()
        val handle = buildHandle(context, sdk, address, forWrite = true, constructorArgs = callConstructorArgs())
        val preimage = PrivateVaultCrypto.Preimage(
            recipientIsContract = false, recipient = recipientAddressHash, color = color,
            amount = amount, nonce = PrivateVaultCrypto.newSalt(),
        )
        val commitment = proposalCommitment(handle, preimage)
        val payload = PrivateVaultCrypto.encryptPreimage(viewingKey, preimage)
        handle.call("proposeWithdrawal", commitment, payload, memberSalt, onProgress = onProgress)
    }

    suspend fun approve(
        context: Context, sdk: MidnightSdk, address: String, proposalId: Long, memberSalt: ByteArray,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        ProvingKeyManager(context).installCircuitKeysFromAssets()
        val handle = buildHandle(context, sdk, address, forWrite = true, constructorArgs = callConstructorArgs())
        handle.call("approve", BigInteger.valueOf(proposalId), memberSalt, onProgress = onProgress)
    }

    suspend fun revokeApproval(
        context: Context, sdk: MidnightSdk, address: String, proposalId: Long, memberSalt: ByteArray,
        onProgress: (suspend (ContractCallStage) -> Unit)? = null,
    ) {
        ProvingKeyManager(context).installCircuitKeysFromAssets()
        val handle = buildHandle(context, sdk, address, forWrite = true, constructorArgs = callConstructorArgs())
        handle.call("revokeApproval", BigInteger.valueOf(proposalId), memberSalt, onProgress = onProgress)
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
        ProvingKeyManager(context).installCircuitKeysFromAssets()
        val handle = buildHandle(context, sdk, address, forWrite = true, constructorArgs = callConstructorArgs())
        val payload = readBytes(handle.read("getProposalPayload", BigInteger.valueOf(proposalId)))
        val p = PrivateVaultCrypto.decryptPreimage(viewingKey, payload)

        val withdrawalJson = sdk.buildUnshieldedWithdrawalJson(
            recipientAddressHash = p.recipient, amount = p.amount, tokenType = p.color.toHex(),
        )
        handle.call(
            "execute", BigInteger.valueOf(proposalId), p.recipientIsContract, p.recipient, p.color,
            p.amount, p.nonce, BigInteger.valueOf(threshold.toLong()), thresholdSalt,
            onProgress = onProgress, unshieldedWithdrawalJson = withdrawalJson,
        )
    }

    fun buildReadHandle(context: Context, sdk: MidnightSdk, address: String): MidnightContract =
        buildHandle(context, sdk, address, forWrite = false, constructorArgs = callConstructorArgs())

    // ── Reads: a member decrypts each proposal from chain state ──

    /** A private proposal as a member sees it: chain-truth status/count + the DECRYPTED contents. */
    data class MemberProposal(
        val id: Long,
        val recipientHashHex: String,
        val colorHex: String,
        val amount: BigInteger,
        val status: Int,
        val approvals: Int,
    )

    suspend fun getProposalCount(handle: MidnightContract): Long =
        jsonScalar(handle.read("getProposalCount")).toLong()

    suspend fun getUnshieldedBalance(handle: MidnightContract, color: ByteArray): BigInteger =
        BigInteger(jsonScalar(handle.read("getUnshieldedBalance", color)))

    /**
     * Load every proposal a member can see: batched status + count + payload per id, decrypting each
     * payload with [viewingKey]. Ids are contiguous from 0; [getProposalCount] bounds the walk.
     */
    suspend fun listProposals(
        handle: MidnightContract, viewingKey: ByteArray, notIndexedTimeoutMs: Long = 0,
    ): List<MemberProposal> {
        val count = try {
            jsonScalar(handle.read("getProposalCount")).toLong()
        } catch (e: Exception) {
            if (notIndexedTimeoutMs > 0) 0L else throw e
        }
        if (count == 0L) return emptyList()

        val requests = (0 until count).flatMap { id ->
            listOf(
                MidnightContract.ReadRequest("s:$id", "getProposalStatus", listOf(BigInteger.valueOf(id))),
                MidnightContract.ReadRequest("a:$id", "getApprovalCount", listOf(BigInteger.valueOf(id))),
                MidnightContract.ReadRequest("y:$id", "getProposalPayload", listOf(BigInteger.valueOf(id))),
            )
        }
        val results = handle.readMany(requests, notIndexedTimeoutMs)
        fun ok(key: String): String {
            val r = results[key] ?: error("missing read '$key'")
            r.error?.let { throw CircuitExecutionException("PrivateVault read '$key' failed: $it") }
            return r.json!!
        }
        return (0 until count).map { id ->
            val payload = hexToBytes(jsonScalar(ok("y:$id")))
            val p = PrivateVaultCrypto.decryptPreimage(viewingKey, payload)
            MemberProposal(
                id = id,
                recipientHashHex = p.recipient.toHex(),
                colorHex = p.color.toHex(),
                amount = p.amount,
                status = jsonScalar(ok("s:$id")).toInt(),
                approvals = jsonScalar(ok("a:$id")).toInt(),
            )
        }
    }

    // PrivateProposalStatus ordinals: Inactive=0, Active=1, Executed=2.
    const val STATUS_INACTIVE = 0
    const val STATUS_ACTIVE = 1
    const val STATUS_EXECUTED = 2

    private fun jsonScalar(json: String): String = org.json.JSONTokener(json).nextValue().toString()
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
