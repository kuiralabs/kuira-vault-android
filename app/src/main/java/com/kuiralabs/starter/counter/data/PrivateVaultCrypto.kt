package com.kuiralabs.starter.counter.data

import java.math.BigInteger
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * The app-side disclosure machinery for the Private Vault — pure crypto + encoding, no Android
 * framework state, so it unit-tests on the JVM.
 *
 * The contract stores only commitments + an opaque ciphertext per proposal. This is where the
 * plaintext a proposal commits to is packed, encrypted under the VAULT VIEWING KEY (a symmetric
 * key every member holds via their invite), and later recovered by a member's device — including
 * the executor, who must present the preimage to the `execute` circuit. Outsiders reading chain
 * state see only ciphertext.
 *
 * Commitment VALUES are NOT computed here: they are derived by the contract's own pure circuits via
 * [PrivateVaultContract] (`readLocal`), so the app can never diverge from the contract's hash.
 */
internal object PrivateVaultCrypto {

    // ── Sizes (the proposal preimage is fixed-layout → fixed ciphertext width) ──
    const val KEY_BYTES = 32       // AES-256 viewing key
    const val SALT_BYTES = 32      // per-signer secret salt + threshold salt + proposal nonce
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    const val AMOUNT_BYTES = 16    // Uint<128>, big-endian

    // Packed preimage: recipientIsContract(1) + recipient(32) + color(32) + amount(16) + nonce(32).
    private const val PREIMAGE_BYTES = 1 + 32 + 32 + AMOUNT_BYTES + 32 // = 113
    // On-chain payload = IV(12) + GCM(plaintext 113 + tag 16 = 129) = 141, padded to the contract's
    // Bytes<160> cell. The width MUST equal the contract's _proposalPayload cell.
    const val PAYLOAD_BYTES = 160

    private val rng = SecureRandom()

    private fun randomBytes(n: Int): ByteArray = ByteArray(n).also { rng.nextBytes(it) }

    fun newViewingKey(): ByteArray = randomBytes(KEY_BYTES)
    fun newSalt(): ByteArray = randomBytes(SALT_BYTES)
    /** A random 32-byte value used to pad the fixed-width signer roster (no known preimage). */
    fun newDummyCommitment(): ByteArray = randomBytes(32)

    /** The plaintext a proposal commits to — the same fields the `execute` circuit re-presents. */
    data class Preimage(
        val recipientIsContract: Boolean,
        val recipient: ByteArray,   // 32
        val color: ByteArray,       // 32
        val amount: BigInteger,     // Uint<128>
        val nonce: ByteArray,       // 32
    )

    /** Uint<128> ↔ exactly 16 big-endian bytes (BigInteger.toByteArray is variable-width + signed). */
    fun amountToBytes(amount: BigInteger): ByteArray {
        require(amount.signum() >= 0) { "amount must be non-negative" }
        val raw = amount.toByteArray()               // may have a leading sign byte or be short
        val trimmed = raw.dropWhile { it == 0.toByte() }.toByteArray()
        require(trimmed.size <= AMOUNT_BYTES) { "amount exceeds Uint<128>" }
        return ByteArray(AMOUNT_BYTES - trimmed.size) + trimmed
    }

    fun amountFromBytes(bytes: ByteArray): BigInteger = BigInteger(1, bytes)

    private fun packPreimage(p: Preimage): ByteArray {
        require(p.recipient.size == 32 && p.color.size == 32 && p.nonce.size == 32) {
            "recipient/color/nonce must each be 32 bytes"
        }
        return ByteArray(1) { if (p.recipientIsContract) 1 else 0 } +
            p.recipient + p.color + amountToBytes(p.amount) + p.nonce
    }

    private fun unpackPreimage(plain: ByteArray): Preimage {
        require(plain.size == PREIMAGE_BYTES) { "preimage must be $PREIMAGE_BYTES bytes" }
        var i = 0
        fun take(n: Int): ByteArray = plain.copyOfRange(i, i + n).also { i += n }
        val isContract = take(1)[0].toInt() != 0
        val recipient = take(32)
        val color = take(32)
        val amount = amountFromBytes(take(AMOUNT_BYTES))
        val nonce = take(32)
        return Preimage(isContract, recipient, color, amount, nonce)
    }

    /**
     * Encrypt a proposal preimage under [viewingKey] into the fixed-width on-chain payload:
     * `IV(12) || AES-GCM(plaintext)+tag`, zero-padded to [PAYLOAD_BYTES]. A fresh random IV per
     * call keeps two identical proposals from producing identical ciphertext.
     */
    fun encryptPreimage(viewingKey: ByteArray, preimage: Preimage): ByteArray {
        require(viewingKey.size == KEY_BYTES) { "viewing key must be $KEY_BYTES bytes" }
        val iv = randomBytes(IV_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(viewingKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val sealed = cipher.doFinal(packPreimage(preimage))   // ciphertext + 16-byte tag
        val body = iv + sealed
        require(body.size <= PAYLOAD_BYTES) { "payload overflow: ${body.size} > $PAYLOAD_BYTES" }
        return body + ByteArray(PAYLOAD_BYTES - body.size)    // zero-pad to the contract cell width
    }

    /** Recover a proposal preimage from the on-chain payload; throws if [viewingKey] is wrong. */
    fun decryptPreimage(viewingKey: ByteArray, payload: ByteArray): Preimage {
        require(viewingKey.size == KEY_BYTES) { "viewing key must be $KEY_BYTES bytes" }
        require(payload.size == PAYLOAD_BYTES) { "payload must be $PAYLOAD_BYTES bytes" }
        val iv = payload.copyOfRange(0, IV_BYTES)
        // Sealed length is fixed by the plaintext width; the trailing padding is not part of GCM.
        val sealedLen = PREIMAGE_BYTES + GCM_TAG_BITS / 8
        val sealed = payload.copyOfRange(IV_BYTES, IV_BYTES + sealedLen)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(viewingKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return unpackPreimage(cipher.doFinal(sealed))
    }

    /**
     * A vault invite — everything a member needs to JOIN and act, shared once via QR/copy. Carries
     * the shared viewing key (decrypt proposals), the threshold + its salt (so any member can
     * execute), and the recipient's OWN member salt (prove membership). Encoded as URL-safe base64
     * JSON; the member salt is per-recipient, so each co-signer gets a distinct invite.
     */
    data class Invite(
        val vaultAddress: String,
        val viewingKey: ByteArray,
        val threshold: Int,
        /** Real signer count — shared among MEMBERS (the on-chain roster stays padded/hidden). */
        val signerCount: Int,
        val thresholdSalt: ByteArray,
        val memberSalt: ByteArray,
    )

    // Encoded base64(version|addr|viewingKey|threshold|signerCount|thresholdSalt|memberSalt), each
    // field base64-URL. A delimited format (not JSON) keeps this module free of Android's org.json
    // stub so it unit-tests on the JVM; '|' never appears in base64-URL or a hex address.
    private const val INVITE_VERSION = 1
    private const val SEP = "|"

    fun encodeInvite(invite: Invite): String {
        val fields = listOf(
            INVITE_VERSION.toString(),
            invite.vaultAddress,
            b64(invite.viewingKey),
            invite.threshold.toString(),
            invite.signerCount.toString(),
            b64(invite.thresholdSalt),
            b64(invite.memberSalt),
        )
        return b64(fields.joinToString(SEP).toByteArray(Charsets.UTF_8))
    }

    fun decodeInvite(encoded: String): Invite {
        val parts = String(unb64(encoded.trim()), Charsets.UTF_8).split(SEP)
        require(parts.size == 7) { "malformed invite" }
        require(parts[0].toInt() == INVITE_VERSION) { "unsupported invite version" }
        return Invite(
            vaultAddress = parts[1],
            viewingKey = unb64(parts[2]),
            threshold = parts[3].toInt(),
            signerCount = parts[4].toInt(),
            thresholdSalt = unb64(parts[5]),
            memberSalt = unb64(parts[6]),
        )
    }

    private fun b64(b: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(b)
    private fun unb64(s: String): ByteArray = Base64.getUrlDecoder().decode(s)
}
