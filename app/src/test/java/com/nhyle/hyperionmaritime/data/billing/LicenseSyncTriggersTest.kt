package com.nhyle.hyperionmaritime.data.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenseSyncTriggersTest {
    @Test fun routinePlayRefreshWithUnchangedOwnershipMustNotCallAccountServer() {
        val owned = setOf("active-weekly-token", "active-monthly-token")
        assertFalse(shouldSyncLinkedAccountAfterPlayRefresh(false, owned, owned))
        assertFalse(shouldSyncLinkedAccountAfterPlayRefresh(false, emptySet(), emptySet()))
    }

    @Test fun checkoutOrOwnershipChangeMustStillSyncLinkedAccount() {
        val before = setOf("existing-token")
        val after = before + "new-purchase-token"
        assertTrue(shouldSyncLinkedAccountAfterPlayRefresh(true, before, before))
        assertTrue(shouldSyncLinkedAccountAfterPlayRefresh(false, before, after))
        assertTrue(shouldSyncLinkedAccountAfterPlayRefresh(false, after, before))
    }
}
