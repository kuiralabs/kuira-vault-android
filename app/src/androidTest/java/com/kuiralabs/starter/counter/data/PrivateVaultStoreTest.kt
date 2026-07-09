package com.kuiralabs.starter.counter.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.midnight.kuira.core.network.MidnightNetwork
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
        assertEquals(m.invites.toSet(), got.invites.toSet())
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
}
