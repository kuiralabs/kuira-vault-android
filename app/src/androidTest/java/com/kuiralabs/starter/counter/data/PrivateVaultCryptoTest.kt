package com.kuiralabs.starter.counter.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigInteger

/**
 * The Private Vault's app-side disclosure crypto — the encryption that fills the contract's opaque
 * proposal payload and the invite that carries the viewing key. The module itself uses only
 * java.util.Base64 + javax.crypto (no Android framework state); it lives here in androidTest to
 * reuse the existing junit dependency without adding a JVM-test one.
 *
 * Load-bearing invariants: a member (viewing-key holder) recovers the exact preimage the executor
 * must re-present; a NON-member cannot; two identical proposals do not produce identical on-chain
 * ciphertext (no fixed-IV leak).
 */
@RunWith(AndroidJUnit4::class)
class PrivateVaultCryptoTest {

    private fun preimage(amount: BigInteger = BigInteger("123456789012345678901234567890")) =
        PrivateVaultCrypto.Preimage(
            recipientIsContract = false,
            recipient = ByteArray(32) { (it + 1).toByte() },
            color = ByteArray(32) { 0x11 },
            amount = amount,
            nonce = ByteArray(32) { (it * 7 + 3).toByte() },
        )

    private fun assertPreimageEquals(a: PrivateVaultCrypto.Preimage, b: PrivateVaultCrypto.Preimage) {
        assertEquals(a.recipientIsContract, b.recipientIsContract)
        assertArrayEquals(a.recipient, b.recipient)
        assertArrayEquals(a.color, b.color)
        assertEquals(a.amount, b.amount)
        assertArrayEquals(a.nonce, b.nonce)
    }

    @Test
    fun encryptDecrypt_roundTripsThePreimageForAMember() {
        val key = PrivateVaultCrypto.newViewingKey()
        val p = preimage()
        val payload = PrivateVaultCrypto.encryptPreimage(key, p)
        assertEquals(PrivateVaultCrypto.PAYLOAD_BYTES, payload.size)
        assertPreimageEquals(p, PrivateVaultCrypto.decryptPreimage(key, payload))
    }

    @Test
    fun payload_looksOpaque_containsNoPlaintextField() {
        val key = PrivateVaultCrypto.newViewingKey()
        val p = preimage()
        val payload = PrivateVaultCrypto.encryptPreimage(key, p)
        fun contains(hay: ByteArray, needle: ByteArray): Boolean =
            (0..hay.size - needle.size).any { i -> hay.copyOfRange(i, i + needle.size).contentEquals(needle) }
        assertTrue("recipient leaked into payload", !contains(payload, p.recipient))
        assertTrue("nonce leaked into payload", !contains(payload, p.nonce))
    }

    @Test
    fun identicalProposals_produceDifferentCiphertext_noFixedIvLeak() {
        val key = PrivateVaultCrypto.newViewingKey()
        val p = preimage()
        assertNotEquals(
            PrivateVaultCrypto.encryptPreimage(key, p).toList(),
            PrivateVaultCrypto.encryptPreimage(key, p).toList(),
        )
    }

    @Test
    fun aNonMember_wrongViewingKey_cannotDecrypt() {
        val payload = PrivateVaultCrypto.encryptPreimage(PrivateVaultCrypto.newViewingKey(), preimage())
        val attacker = PrivateVaultCrypto.newViewingKey()
        assertThrows(Exception::class.java) { PrivateVaultCrypto.decryptPreimage(attacker, payload) }
    }

    @Test
    fun amount_roundTripsAtTheUint128Boundary() {
        val max = BigInteger.TWO.pow(128) - BigInteger.ONE
        assertEquals(PrivateVaultCrypto.AMOUNT_BYTES, PrivateVaultCrypto.amountToBytes(max).size)
        assertEquals(max, PrivateVaultCrypto.amountFromBytes(PrivateVaultCrypto.amountToBytes(max)))
        assertEquals(BigInteger.ZERO, PrivateVaultCrypto.amountFromBytes(PrivateVaultCrypto.amountToBytes(BigInteger.ZERO)))
        val key = PrivateVaultCrypto.newViewingKey()
        val p = preimage(amount = max)
        assertPreimageEquals(p, PrivateVaultCrypto.decryptPreimage(key, PrivateVaultCrypto.encryptPreimage(key, p)))
    }

    @Test
    fun invite_roundTripsAllFields() {
        val invite = PrivateVaultCrypto.Invite(
            vaultAddress = "ab".repeat(32),
            viewingKey = PrivateVaultCrypto.newViewingKey(),
            threshold = 2,
            signerCount = 3,
            thresholdSalt = PrivateVaultCrypto.newSalt(),
            memberSalt = PrivateVaultCrypto.newSalt(),
        )
        val decoded = PrivateVaultCrypto.decodeInvite(PrivateVaultCrypto.encodeInvite(invite))
        assertEquals(invite.vaultAddress, decoded.vaultAddress)
        assertArrayEquals(invite.viewingKey, decoded.viewingKey)
        assertEquals(invite.threshold, decoded.threshold)
        assertEquals(invite.signerCount, decoded.signerCount)
        assertArrayEquals(invite.thresholdSalt, decoded.thresholdSalt)
        assertArrayEquals(invite.memberSalt, decoded.memberSalt)
    }

    @Test
    fun invite_rejectsGarbage() {
        assertThrows(Exception::class.java) { PrivateVaultCrypto.decodeInvite("not-a-valid-invite") }
    }

    @Test
    fun invite_rejectsTampering() {
        val invite = PrivateVaultCrypto.Invite(
            vaultAddress = "ab".repeat(32),
            viewingKey = PrivateVaultCrypto.newViewingKey(),
            threshold = 2,
            signerCount = 3,
            thresholdSalt = PrivateVaultCrypto.newSalt(),
            memberSalt = PrivateVaultCrypto.newSalt(),
        )
        val good = PrivateVaultCrypto.encodeInvite(invite)
        // Flip one character of the body (inside the address field) without touching the checksum,
        // then re-encode — decode must reject it on the integrity check, not silently accept.
        val body = String(java.util.Base64.getUrlDecoder().decode(good), Charsets.UTF_8).toCharArray()
        body[10] = if (body[10] == 'c') 'd' else 'c'
        val tampered = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(String(body).toByteArray(Charsets.UTF_8))
        assertThrows(Exception::class.java) { PrivateVaultCrypto.decodeInvite(tampered) }
    }
}
