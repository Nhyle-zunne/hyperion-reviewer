package com.nhyle.hyperionmaritime.data.billing

import com.nhyle.hyperionmaritime.ui.store.canTapLifetimeUnlock
import com.nhyle.hyperionmaritime.ui.store.lifetimeUnlockButtonLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifetimeBillingRulesTest {
    @Test
    fun allFiveTrackLifetimeProductsAreDistinctFromRecurringSubscriptions() {
        val expected = mapOf(
            BillingConstants.PRODUCT_OIC_NW_PASS to BillingConstants.PRODUCT_OIC_NW_LIFETIME,
            BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS to BillingConstants.PRODUCT_ENGINE_OIC_EW_LIFETIME,
            BillingConstants.PRODUCT_GMDSS_PASS to BillingConstants.PRODUCT_GMDSS_LIFETIME,
            BillingConstants.PRODUCT_DECK_MGT_PASS to BillingConstants.PRODUCT_DECK_MGT_LIFETIME,
            BillingConstants.PRODUCT_ENGINE_MGT_PASS to BillingConstants.PRODUCT_ENGINE_MGT_LIFETIME
        )
        assertEquals(5, expected.size)
        assertEquals(expected.values.toSet().size, expected.size)
        for ((subscription, lifetime) in expected) {
            assertEquals(lifetime, BillingConstants.lifetimeProductForSubscription(subscription))
            assertFalse(BillingConstants.ALL_PRODUCT_IDS.contains(lifetime))
        }
        assertNull(BillingConstants.lifetimeProductForSubscription(BillingConstants.PRODUCT_ALL_ACCESS_PASS))
    }

    @Test
    fun lifetimePricesMatchApprovedStoreCatalogWithoutUsingSubscriptionCycles() {
        assertEquals("₱999", BillingConstants.getFallbackLifetimePrice(BillingConstants.PRODUCT_OIC_NW_LIFETIME))
        assertEquals("₱999", BillingConstants.getFallbackLifetimePrice(BillingConstants.PRODUCT_ENGINE_OIC_EW_LIFETIME))
        assertEquals("₱599", BillingConstants.getFallbackLifetimePrice(BillingConstants.PRODUCT_GMDSS_LIFETIME))
        assertEquals("₱1,999", BillingConstants.getFallbackLifetimePrice(BillingConstants.PRODUCT_DECK_MGT_LIFETIME))
        assertEquals("₱1,999", BillingConstants.getFallbackLifetimePrice(BillingConstants.PRODUCT_ENGINE_MGT_LIFETIME))
    }

    @Test
    fun onlyAnExactPurchasedSingleNonConsumableCanBeSubmittedForVerification() {
        val id = BillingConstants.PRODUCT_OIC_NW_LIFETIME
        assertTrue(isEligibleLifetimePurchase(true, listOf(id)))
        assertFalse(isEligibleLifetimePurchase(false, listOf(id)))
        assertFalse(isEligibleLifetimePurchase(true, listOf(id, BillingConstants.PRODUCT_GMDSS_LIFETIME)))
        assertFalse(isEligibleLifetimePurchase(true, listOf(BillingConstants.PRODUCT_OIC_NW_PASS)))
        assertFalse(isEligibleLifetimePurchase(true, emptyList()))
    }

    @Test
    fun lifetimeUnlockIsTappableBeforeRemoteCheckoutReadinessCompletes() {
        assertTrue(canTapLifetimeUnlock(lifetimeOwned = false, purchaseInProgress = false))
        assertFalse(canTapLifetimeUnlock(lifetimeOwned = true, purchaseInProgress = false))
        assertFalse(canTapLifetimeUnlock(lifetimeOwned = false, purchaseInProgress = true))
    }

    @Test
    fun lifetimeUnlockUsesSameButtonCopyAsSubscriptionOffers() {
        assertEquals("UNLOCK — ₱999", lifetimeUnlockButtonLabel("₱999"))
        assertEquals("UNLOCK — ₱599", lifetimeUnlockButtonLabel("₱599"))
        assertEquals("UNLOCK — ₱1,999", lifetimeUnlockButtonLabel("₱1,999"))
    }

    @Test
    fun lifetimeCheckoutRequiresLinkedAccountAndAvailablePlayOffer() {
        assertTrue(canOfferLifetimeCheckout("signed-user", true, false, true))
        assertFalse(canOfferLifetimeCheckout(null, true, false, true))
        assertFalse(canOfferLifetimeCheckout("signed-user", false, false, true))
        assertFalse(canOfferLifetimeCheckout("signed-user", true, true, true))
        assertFalse(canOfferLifetimeCheckout("signed-user", true, false, false))
    }
}
