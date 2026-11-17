package com.nhyle.hyperionmaritime.data.billing

import com.nhyle.hyperionmaritime.ui.store.canTapLifetimeUnlock
import com.nhyle.hyperionmaritime.ui.store.lifetimeUnlockButtonLabel
import com.nhyle.hyperionmaritime.ui.store.premiumFeaturesForTrack
import com.nhyle.hyperionmaritime.ui.store.storeDisplayPrice
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
    fun storePricesHideOnlyZeroCentDecimalsWithoutChangingOtherFractions() {
        assertEquals("₱999", storeDisplayPrice("₱999.00"))
        assertEquals("₱1,999", storeDisplayPrice("₱1,999.00"))
        assertEquals("₱29/mo", storeDisplayPrice("₱29.00/mo"))
        assertEquals("₱29.50", storeDisplayPrice("₱29.50"))
        assertEquals("₱999", storeDisplayPrice("₱999"))
        assertEquals("UNLOCK — ₱1,999", lifetimeUnlockButtonLabel("₱1,999.00"))
    }

    @Test
    fun firstPremiumFeatureMatchesSelectedDurationAndPreservesSharedBenefits() {
        val weekly = premiumFeaturesForTrack("12", BillingCycle.WEEKLY)
        val monthly = premiumFeaturesForTrack("12", BillingCycle.MONTHLY)
        val yearly = premiumFeaturesForTrack("12", BillingCycle.YEARLY)
        val lifetime = premiumFeaturesForTrack("12", BillingCycle.YEARLY, lifetimeSelected = true)
        assertEquals("1 week unlimited access", weekly.first())
        assertEquals("1 month unlimited access", monthly.first())
        assertEquals("1 year unlimited access", yearly.first())
        assertEquals("No expiration", lifetime.first())
        assertEquals(11, lifetime.size)
        assertEquals(weekly.drop(1), monthly.drop(1))
        assertEquals(weekly.drop(1), yearly.drop(1))
        assertEquals(weekly.drop(1), lifetime.drop(1))
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
