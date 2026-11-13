package com.nhyle.hyperionmaritime

import com.nhyle.hyperionmaritime.data.billing.BillingConstants
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.ui.components.paywallProductIdForTrack
import com.nhyle.hyperionmaritime.ui.screens.ReviewerHomeMode
import com.nhyle.hyperionmaritime.ui.screens.reviewerHomeModesForLicense
import com.nhyle.hyperionmaritime.ui.store.premiumFeaturesForTrack
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class PaywallDialogTrackMappingTest {

    @Test
    fun eachTrackOpensItsOwnStoreProduct() {
        assertEquals(BillingConstants.PRODUCT_OIC_NW_PASS, paywallProductIdForTrack(Track.DECK_OIC_NW))
        assertEquals(BillingConstants.PRODUCT_GMDSS_PASS, paywallProductIdForTrack(Track.GMDSS_GOC))
        assertEquals(BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS, paywallProductIdForTrack(Track.ENGINE_OIC_EW))
        assertEquals(BillingConstants.PRODUCT_DECK_MGT_PASS, paywallProductIdForTrack(Track.DECK_MGT))
        assertEquals(BillingConstants.PRODUCT_ENGINE_MGT_PASS, paywallProductIdForTrack(Track.ENGINE_MGT))
    }

    @Test
    fun onlyFreeReviewerAndUnlockAreVisibleWithoutTrackAccess() {
        val freeModes = reviewerHomeModesForLicense(false)
        assertEquals(listOf(ReviewerHomeMode.FREE, ReviewerHomeMode.UNLOCK), freeModes)
        assertFalse(freeModes.contains(ReviewerHomeMode.TIMED_MOCK))
    }

    @Test
    fun licensedTracksHaveAllSevenPremiumModesWithoutFree() {
        val modes = reviewerHomeModesForLicense(true)
        assertEquals(listOf(
            ReviewerHomeMode.PRACTICE_ALL, ReviewerHomeMode.QUICK_SCAN,
            ReviewerHomeMode.TIMED_MOCK, ReviewerHomeMode.FLASHCARDS,
            ReviewerHomeMode.MISTAKES, ReviewerHomeMode.SESSION_LOG,
            ReviewerHomeMode.SEARCH
        ), modes)
        assertFalse(modes.contains(ReviewerHomeMode.FREE))
        assertFalse(modes.contains(ReviewerHomeMode.UNLOCK))
        assertEquals(reviewerHomeModesForLicense(false), reviewerHomeModesForLicense(false))
    }

    @Test
    fun everyProductPopupContainsNineRelevantBenefits() {
        for (count in listOf("12", "2", "13", "18", "14")) {
            val perks = premiumFeaturesForTrack(count)
            assertEquals(9, perks.size)
            assertEquals(9, perks.distinct().size)
            assertTrue(perks.contains("Unlimited timed mock exams"))
            assertTrue(perks.contains("Full question bank across $count competencies"))
            assertTrue(perks.contains("Quick Scan Mode"))
            assertTrue(perks.contains("Mistakes Notebook and bookmarks"))
            assertTrue(perks.contains("Offline review after downloading"))
            assertTrue(perks.contains("Study history and cross-device progress sync"))
        }
    }
}
