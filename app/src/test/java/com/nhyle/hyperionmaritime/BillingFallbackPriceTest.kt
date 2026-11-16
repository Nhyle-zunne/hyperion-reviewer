package com.nhyle.hyperionmaritime

import com.nhyle.hyperionmaritime.data.billing.BillingConstants
import com.nhyle.hyperionmaritime.data.billing.BillingCycle
import com.nhyle.hyperionmaritime.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingFallbackPriceTest {
    @Test
    fun eachPaidProductHasUpdatedLongAndShortFallbacksForAllBillingCycles() {
        val expected = mapOf(
            BillingConstants.PRODUCT_OIC_NW_PASS to listOf(29, 79, 499),
            BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS to listOf(29, 79, 499),
            BillingConstants.PRODUCT_GMDSS_PASS to listOf(25, 59, 399),
            BillingConstants.PRODUCT_DECK_MGT_PASS to listOf(59, 149, 999),
            BillingConstants.PRODUCT_ENGINE_MGT_PASS to listOf(59, 149, 999)
        )
        val cycles = listOf(
            Triple(BillingCycle.WEEKLY, " / week", "/wk"),
            Triple(BillingCycle.MONTHLY, " / month", "/mo"),
            Triple(BillingCycle.YEARLY, " / year", "/yr")
        )

        for ((productId, prices) in expected) {
            for ((index, value) in cycles.withIndex()) {
                val (cycle, fullSuffix, shortSuffix) = value
                assertEquals(
                    "$productId $cycle long price",
                    "₱${prices[index]}$fullSuffix",
                    BillingConstants.getFallbackPrice(productId, cycle)
                )
                assertEquals(
                    "$productId $cycle short price",
                    "₱${prices[index]}$shortSuffix",
                    BillingConstants.getFallbackShortPrice(productId, cycle)
                )
            }
        }
    }

    @Test
    fun allAccessLegacyPricesRemainUnchangedUntilExplicitlyPriced() {
        val productId = BillingConstants.PRODUCT_ALL_ACCESS_PASS
        assertEquals("₱35 / week", BillingConstants.getFallbackPrice(productId, BillingCycle.WEEKLY))
        assertEquals("₱80 / month", BillingConstants.getFallbackPrice(productId, BillingCycle.MONTHLY))
        assertEquals("₱500 / year", BillingConstants.getFallbackPrice(productId, BillingCycle.YEARLY))
        assertEquals("₱35/wk", BillingConstants.getFallbackShortPrice(productId, BillingCycle.WEEKLY))
        assertEquals("₱80/mo", BillingConstants.getFallbackShortPrice(productId, BillingCycle.MONTHLY))
        assertEquals("₱500/yr", BillingConstants.getFallbackShortPrice(productId, BillingCycle.YEARLY))
    }

    @Test
    fun unrecognisedProductsDoNotInheritAnUnrelatedPrice() {
        for (cycle in listOf(BillingCycle.WEEKLY, BillingCycle.MONTHLY, BillingCycle.YEARLY)) {
            assertEquals("Price unavailable", BillingConstants.getFallbackPrice("missing_product", cycle))
            assertEquals("Price unavailable", BillingConstants.getFallbackShortPrice("missing_product", cycle))
        }
    }
    @Test
    fun lifetimeProductsAreOneTimeOnlyAndNeverIncludedInSubscriptions() {
        val expected = mapOf(
            Track.DECK_OIC_NW to "hyperion_oic_nw_lifetime",
            Track.ENGINE_OIC_EW to "hyperion_engine_oic_ew_lifetime",
            Track.GMDSS_GOC to "hyperion_gmdss_lifetime",
            Track.DECK_MGT to "hyperion_deck_mgt_lifetime",
            Track.ENGINE_MGT to "hyperion_engine_mgt_lifetime"
        )
        for ((track, lifetimeId) in expected) {
            assertEquals(lifetimeId, BillingConstants.lifetimeProductForTrack(track))
        }
        assertEquals(expected.values.toSet(), BillingConstants.LIFETIME_PRODUCT_IDS.toSet())
        assertEquals(5, BillingConstants.LIFETIME_PRODUCT_IDS.size)
        assertEquals(5, BillingConstants.LIFETIME_PRODUCT_IDS.distinct().size)
        for (productId in BillingConstants.LIFETIME_PRODUCT_IDS) {
            org.junit.Assert.assertFalse("One-time products must never be queried as SUBS",
                BillingConstants.ALL_PRODUCT_IDS.contains(productId))
        }
        org.junit.Assert.assertFalse(BillingConstants.LIFETIME_PRODUCT_IDS.any { it.contains("all_access") })
    }

}
