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
 * The Sigil cloud-backup round-trip for the Private Vault: [PrivateVaultStore.snapshotBytes] ->
 * (wiped store, as a reinstall) -> [PrivateVaultStore.restoreFromBytes] must bring back the whole
 * membership AND approval state. This is the reinstall-recovery fix — without it a reinstall
 * strands the vault (viewing key/salts are unrecoverable) and forgets approvals.
 */
@RunWith(AndroidJUnit4::class)
class PrivateVaultBackupRoundTripTest {

    private val store = PrivateVaultStore(ApplicationProvider.getApplicationContext())
    private val net = MidnightNetwork.UNDEPLOYED
    private val vault = "aa".repeat(32)

    private fun membership() = PrivateVaultStore.Membership(
        address = vault,
        viewingKey = ByteArray(32) { (it + 1).toByte() },
        threshold = 2,
        signerCount = 3,
        thresholdSalt = ByteArray(32) { 0x77 },
        memberSalt = ByteArray(32) { 0x11 },
        isCreator = true,
        invites = listOf("invite-b", "invite-c"),
    )

    @Before fun clean() = store.clear(net, vault)
    @After fun tearDown() = store.clear(net, vault)

    @Test
    fun snapshotThenRestore_recoversMembershipAndApprovals() {
        val m = membership()
        store.save(net, m)
        store.markApproved(net, vault, 0L, true)
        store.markApproved(net, vault, 2L, true)

        val blob = store.snapshotBytes()!!

        // Simulate a reinstall: wipe everything.
        store.clear(net, vault)
        assertNull("precondition: store is empty after wipe", store.get(net))

        // Restore from the backup blob.
        store.restoreFromBytes(blob)

        val got = store.get(net)!!
        assertEquals(m.address, got.address)
        assertArrayEquals(m.viewingKey, got.viewingKey)
        assertArrayEquals(m.thresholdSalt, got.thresholdSalt)
        assertArrayEquals(m.memberSalt, got.memberSalt)
        assertEquals(m.threshold, got.threshold)
        assertEquals(m.signerCount, got.signerCount)
        assertTrue(got.isCreator)
        assertEquals(m.invites.toSet(), got.invites.toSet())
        assertEquals("approvals recovered", setOf(0L, 2L), store.approvedIds(net, vault))
    }

    @Test
    fun snapshot_isNull_whenNothingSaved() {
        assertNull(store.snapshotBytes())
    }

    @Test
    fun restore_ignoresUnknownSchema() {
        store.restoreFromBytes("""{"v":999,"nets":{}}""".toByteArray())
        assertNull(store.get(net)) // declined, no crash
    }
}
