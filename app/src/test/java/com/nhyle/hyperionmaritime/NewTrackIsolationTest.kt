package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.billing.BillingConstants
import com.nhyle.hyperionmaritime.data.billing.BillingManager
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NewTrackIsolationTest {

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var viewModel: ReviewerViewModel
    private lateinit var billingManager: BillingManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        MaritimeQuestionBank.init(context)

        // Reset all licenses
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.setEngineOicEwProUnlocked(context, false)
        LicenseManager.setDeckMgtProUnlocked(context, false)
        LicenseManager.setEngineMgtProUnlocked(context, false)
        LicenseManager.setHyperionProUnlocked(context, false)
        LicenseManager.init(context)

        // Clear prefs
        context.getSharedPreferences("hyperion_play_billing_secure_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE).edit().clear().apply()

        BillingManager.resetForTesting()
        billingManager = BillingManager.getInstance(context)
        billingManager.resetEntitlementsForTesting()
        viewModel = ReviewerViewModel(application)
    }

    @After
    fun tearDown() {
        BillingManager.resetForTesting()
    }

    @Test
    fun testEngineEwPassIsolation() {
        assertFalse(viewModel.isTrackUnlocked(Track.ENGINE_OIC_EW))
        
        viewModel.simulatePurchase(BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS)
        
        assertTrue("Engine OIC-EW must be unlocked", viewModel.isTrackUnlocked(Track.ENGINE_OIC_EW))
        assertFalse("Deck OIC-NW must remain locked", viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertFalse("GMDSS must remain locked", viewModel.isTrackUnlocked(Track.GMDSS_GOC))
        assertFalse("Deck MGT must remain locked", viewModel.isTrackUnlocked(Track.DECK_MGT))
        assertFalse("Engine MGT must remain locked", viewModel.isTrackUnlocked(Track.ENGINE_MGT))
    }

    @Test
    fun testDeckMgtPassIsolation() {
        assertFalse(viewModel.isTrackUnlocked(Track.DECK_MGT))
        
        viewModel.simulatePurchase(BillingConstants.PRODUCT_DECK_MGT_PASS)
        
        assertTrue("Deck Management must be unlocked", viewModel.isTrackUnlocked(Track.DECK_MGT))
        assertFalse("Engine OIC-EW must remain locked", viewModel.isTrackUnlocked(Track.ENGINE_OIC_EW))
        assertFalse("Engine MGT must remain locked", viewModel.isTrackUnlocked(Track.ENGINE_MGT))
    }

    @Test
    fun testEngineMgtPassIsolation() {
        assertFalse(viewModel.isTrackUnlocked(Track.ENGINE_MGT))
        
        viewModel.simulatePurchase(BillingConstants.PRODUCT_ENGINE_MGT_PASS)
        
        assertTrue("Engine Management must be unlocked", viewModel.isTrackUnlocked(Track.ENGINE_MGT))
        assertFalse("Engine OIC-EW must remain locked", viewModel.isTrackUnlocked(Track.ENGINE_OIC_EW))
        assertFalse("Deck MGT must remain locked", viewModel.isTrackUnlocked(Track.DECK_MGT))
    }

    @Test
    fun testAllAccessPassUnlocksEverything() {
        viewModel.simulatePurchase(BillingConstants.PRODUCT_ALL_ACCESS_PASS)
        
        assertTrue(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertTrue(viewModel.isTrackUnlocked(Track.GMDSS_GOC))
        assertTrue(viewModel.isTrackUnlocked(Track.ENGINE_OIC_EW))
        assertTrue(viewModel.isTrackUnlocked(Track.DECK_MGT))
        assertTrue(viewModel.isTrackUnlocked(Track.ENGINE_MGT))
    }
}
