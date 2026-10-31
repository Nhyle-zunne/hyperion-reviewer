package com.nhyle.hyperionmaritime

import com.nhyle.hyperionmaritime.data.billing.BillingConstants
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.ui.components.paywallProductIdForTrack
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
}
