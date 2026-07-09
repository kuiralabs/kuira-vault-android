package com.kuiralabs.vault.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.midnight.kuira.core.network.MidnightNetwork
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [PrivateVaultStore] — the encrypted per-device membership + local approval state. Runs on-device
 * (EncryptedSharedPreferences needs a real KeyStore-backed context).
 *
 * The centerpiece is [approvedIds_areIsolatedPerVault]: the "did I approve?" set was once keyed by
 * NETWORK only, so approving on one vault suppressed the Approve button on a DIFFERENT vault reached
 * on the same network (it could stall below threshold). It's now keyed by (network, vault address);
 * this test reproduces that isolation and fails if the keying regresses.
 */
@RunWith(AndroidJUnit4::class)
class PrivateVaultStoreTest {

    private val store = PrivateVaultStore(ApplicationProvider.getApplicationContext())
    private val net = MidnightNetwork.UNDEPLOYED
    private val vaultA = "aa".repeat(32)
    private val vaultB = "bb".repeat(32)

    private fun membership(address: String, creator: Boolean = true) = PrivateVaultStore.Membership(
        address = address,
        viewingKey = ByteArray(32) { (it + 1).toByte() },
        threshold = 2,
        signerCount = 3,
        thresholdSalt = ByteArray(32) { 0x77 },
        memberSalt = ByteArray(32) { 0x11 },
        isCreator = creator,
        invites = if (creator) listOf("invite-b", "invite-c") else emptyList(),
        coSignerKeyHexes = if (creator) listOf("bb".repeat(32), "cc".repeat(32)) else emptyList(),
    )

    @Before fun clean() = wipe()
    @After fun tearDown() = wipe()
    private fun wipe() {
        store.clear(net, vaultA)
        store.clear(net, vaultB)
    }

    @Test
    fun saveAndGet_roundTripsAllFields() {
        val m = membership(vaultA)
        store.save(net, m)
        val got = store.get(net)!!
        assertEquals(m.address, got.address)
        assertArrayEquals(m.viewingKey, got.viewingKey)
        assertEquals(m.threshold, got.threshold)
        assertEquals(m.signerCount, got.signerCount)
        assertArrayEquals(m.thresholdSalt, got.thresholdSalt)
        assertArrayEquals(m.memberSalt, got.memberSalt)
        assertTrue(got.isCreator)
        // Order matters: invites[i] must stay aligned with coSignerKeyHexes[i] (the "Co-signer 2/3"
        // labels + key echo depend on it) — a StringSet round-trip would have scrambled this.
        assertEquals(m.invites, got.invites)
        assertEquals(m.coSignerKeyHexes, got.coSignerKeyHexes)
    }

    @Test
    fun get_returnsNull_whenNothingSaved() {
        assertNull(store.get(net))
    }

    @Test
    fun approvedIds_areIsolatedPerVault() {
        // Approve proposal 0 on vault A.
        store.markApproved(net, vaultA, 0L, true)
        assertEquals(setOf(0L), store.approvedIds(net, vaultA))
        // Vault B (same network, different address) must see NO approvals — the regression.
        assertTrue("vault B must not inherit vault A's approvals", store.approvedIds(net, vaultB).isEmpty())
        // And B's own approvals stay separate.
        store.markApproved(net, vaultB, 5L, true)
        assertEquals(setOf(0L), store.approvedIds(net, vaultA))
        assertEquals(setOf(5L), store.approvedIds(net, vaultB))
    }

    @Test
    fun markApproved_addsAndRemoves() {
        store.markApproved(net, vaultA, 1L, true)
        store.markApproved(net, vaultA, 2L, true)
        assertEquals(setOf(1L, 2L), store.approvedIds(net, vaultA))
        store.markApproved(net, vaultA, 1L, false)
        assertEquals(setOf(2L), store.approvedIds(net, vaultA))
    }

    @Test
    fun clear_wipesMembershipAndApprovals() {
        store.save(net, membership(vaultA))
        store.markApproved(net, vaultA, 0L, true)
        store.clear(net, vaultA)
        assertNull(store.get(net))
        assertTrue(store.approvedIds(net, vaultA).isEmpty())
    }

    /**
     * Pre-ordering-fix builds stored invites as an unordered StringSet; the fix reads ordered JSON.
     * Without a fallback, an in-place upgrade over a creator's old vault reads null → drops the
     * invites, and each co-signer's per-signer salt lives ONLY inside its invite → unrecoverable.
     * This writes the legacy StringSet directly and asserts [get] still recovers the invites.
     */
    @Test
    fun get_migratesLegacyStringSetInvites() {
        store.save(net, membership(vaultA))           // full membership (invites as JSON)
        rawPrefs().edit()                              // clobber just the invites key with legacy format
            .remove("pvault.invites.${net.name}")
            .putStringSet("pvault.invites.${net.name}", setOf("invite-b", "invite-c"))
            .apply()
        val got = store.get(net)!!
        assertEquals(setOf("invite-b", "invite-c"), got.invites.toSet()) // recovered (order not guaranteed for legacy)
    }

    /**
     * Restore is REPLACE, not union: a backup taken before an approval must not resurrect it if the
     * backup is restored over local state where that id was since approved (or vice-versa). Here the
     * backup captures zero approvals; approving locally afterwards must be wiped by the restore.
     */
    @Test
    fun restore_replacesApprovals_soStaleLocalStateIsCleared() {
        store.save(net, membership(vaultA))
        val backup = store.snapshotBytes()!!           // approved = [] captured
        store.markApproved(net, vaultA, 0L, true)      // later approve 0 locally
        store.restoreFromBytes(backup)                 // backup says none approved
        assertTrue("restore must replace approvals, not union", store.approvedIds(net, vaultA).isEmpty())
    }

    /** One corrupted/partial network entry must not abort restore of the healthy siblings. */
    @Test
    fun restore_skipsMalformedNetwork_restoresOthers() {
        store.save(net, membership(vaultA))
        val blob = JSONObject(String(store.snapshotBytes()!!))
        store.clear(net, vaultA)
        // Inject a valid-enum sibling ("PREVIEW") whose body is missing the required "addr".
        blob.getJSONObject("nets").put("PREVIEW", JSONObject().put("th", 1))
        store.restoreFromBytes(blob.toString().toByteArray())
        assertEquals(vaultA, store.get(net)?.address)  // healthy network restored despite bad sibling
    }

    private fun rawPrefs() = EncryptedSharedPreferences.create(
        ApplicationProvider.getApplicationContext(),
        "kuira-private-vault-prefs",
        MasterKey.Builder(ApplicationProvider.getApplicationContext<Context>())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
