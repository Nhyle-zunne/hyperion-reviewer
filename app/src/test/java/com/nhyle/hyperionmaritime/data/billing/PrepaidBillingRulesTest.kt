package com.nhyle.hyperionmaritime.data.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrepaidBillingRulesTest {
    @Test
    fun allFiveSellableTracksHaveExactPrepaidBasePlanIds() {
        val expected = mapOf(
            BillingConstants.PRODUCT_OIC_NW_PASS to "oic-nw",
            BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS to "oic-ew",
            BillingConstants.PRODUCT_DECK_MGT_PASS to "deck-mgt",
            BillingConstants.PRODUCT_ENGINE_MGT_PASS to "engine-mgt",
            BillingConstants.PRODUCT_GMDSS_PASS to "gmdss"
        )
        for ((productId, prefix) in expected) {
            assertEquals("$prefix-prepaid-weekly", prepaidBasePlanId(productId, BillingCycle.WEEKLY))
            assertEquals("$prefix-prepaid-monthly", prepaidBasePlanId(productId, BillingCycle.MONTHLY))
            assertEquals("$prefix-prepaid-yearly", prepaidBasePlanId(productId, BillingCycle.YEARLY))
        }
        assertNull(prepaidBasePlanId(BillingConstants.PRODUCT_ALL_ACCESS_PASS, BillingCycle.WEEKLY))
    }

    @Test
    fun initialPurchaseOnlyMatchesAutoRenewingOffers() {
        assertTrue(matchesPlanOffer(
            BillingConstants.PRODUCT_OIC_NW_PASS, BillingCycle.MONTHLY,
            PlayPlanMode.AUTO_RENEWING, "oic-nw-monthly", listOf("P1M")
        ))
        assertFalse(matchesPlanOffer(
            BillingConstants.PRODUCT_OIC_NW_PASS, BillingCycle.MONTHLY,
            PlayPlanMode.AUTO_RENEWING, "oic-nw-prepaid-monthly", listOf("P1M")
        ))
        assertFalse(matchesPlanOffer(
            BillingConstants.PRODUCT_OIC_NW_PASS, BillingCycle.MONTHLY,
            PlayPlanMode.AUTO_RENEWING, "oic-nw-weekly", listOf("P1W")
        ))
    }

    @Test
    fun extensionsOnlyMatchTheIntendedPrepaidPlan() {
        assertTrue(matchesPlanOffer(
            BillingConstants.PRODUCT_OIC_NW_PASS, BillingCycle.MONTHLY,
            PlayPlanMode.PREPAID, "oic-nw-prepaid-monthly", listOf("P1M")
        ))
        assertFalse(matchesPlanOffer(
            BillingConstants.PRODUCT_OIC_NW_PASS, BillingCycle.MONTHLY,
            PlayPlanMode.PREPAID, "oic-nw-monthly", listOf("P1M")
        ))
        assertFalse(matchesPlanOffer(
            BillingConstants.PRODUCT_OIC_NW_PASS, BillingCycle.MONTHLY,
            PlayPlanMode.PREPAID, "oic-nw-prepaid-yearly", listOf("P1Y")
        ))
        assertFalse(matchesPlanOffer(
            BillingConstants.PRODUCT_ALL_ACCESS_PASS, BillingCycle.MONTHLY,
            PlayPlanMode.PREPAID, "all-access-prepaid-monthly", listOf("P1M")
        ))
    }

    @Test
    fun pendingExtensionCannotChangeExistingPlanUntilPurchased() {
        assertEquals(
            PlayPlanMode.AUTO_RENEWING,
            resolvePurchasedPlanMode(PlayPlanMode.AUTO_RENEWING, PlayPlanMode.PREPAID, false, true)
        )
        assertEquals(
            PlayPlanMode.PREPAID,
            resolvePurchasedPlanMode(PlayPlanMode.AUTO_RENEWING, PlayPlanMode.PREPAID, true, false)
        )
        assertEquals(
            PlayPlanMode.PREPAID,
            resolvePurchasedPlanMode(PlayPlanMode.PREPAID, null, false, false)
        )
        assertEquals(
            PlayPlanMode.AUTO_RENEWING,
            resolvePurchasedPlanMode(PlayPlanMode.AUTO_RENEWING, null, false, false)
        )
    }

    @Test
    fun extensionRequiresCurrentPlayPurchaseTokenNotJustWebsiteAccess() {
        assertFalse(canExtendPlayPurchase(null))
        assertFalse(canExtendPlayPurchase(""))
        assertFalse(canExtendPlayPurchase("mock_sandbox_sub_token_123"))
        assertTrue(canExtendPlayPurchase("verified-play-purchase-token"))
    }
    @Test
    fun legacyAllAccessPurchaseCannotBeReplacedWithASingleTrackExtension() {
        assertFalse(canExtendTrackPlayPurchase("legacy-all-access-token", "legacy-all-access-token"))
        assertTrue(canExtendTrackPlayPurchase("individual-oic-token", "legacy-all-access-token"))
        assertTrue(canExtendTrackPlayPurchase("individual-oic-token", null))
        assertFalse(canExtendTrackPlayPurchase("mock_sandbox_sub_token_123", null))
    }

}
