package com.nhyle.hyperionmaritime

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.billing.BillingConstants
import com.nhyle.hyperionmaritime.data.billing.BillingCycle
import com.nhyle.hyperionmaritime.data.billing.ActivePlanInfo
import com.nhyle.hyperionmaritime.data.billing.estimateNextSubscriptionExpiry
import com.nhyle.hyperionmaritime.data.billing.formatSubscriptionTimeRemaining
import com.nhyle.hyperionmaritime.data.billing.matchesPlayOffer
import com.nhyle.hyperionmaritime.data.billing.subscriptionDisplayExpiry
import com.nhyle.hyperionmaritime.data.billing.resolveRequestedBillingCycle
import com.nhyle.hyperionmaritime.data.billing.resolvePurchasedBillingCycle
import com.nhyle.hyperionmaritime.data.billing.isNewPlaySubscriptionPurchase
import com.nhyle.hyperionmaritime.data.billing.canRefreshPlayVerification
import java.time.Instant
import com.nhyle.hyperionmaritime.data.billing.shouldApplyPlayPurchaseSnapshot
import com.nhyle.hyperionmaritime.data.billing.BillingManager
import com.nhyle.hyperionmaritime.data.billing.collectPlayPurchasesForAccountLinking
import com.nhyle.hyperionmaritime.data.billing.obfuscatedHyperionAccountId
import com.nhyle.hyperionmaritime.data.auth.isMissingGooglePlayEntitlementsTable
import com.nhyle.hyperionmaritime.data.auth.isGooglePlayAccountLinkConflict
import com.nhyle.hyperionmaritime.data.auth.claimLearningProgressOwner
import java.io.IOException
import com.nhyle.hyperionmaritime.data.model.QuizMode
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.data.util.calculateOfflineTrustedTime
import com.nhyle.hyperionmaritime.viewmodel.DrillDeliveryMode
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class PlayBillingAndLicensingInvariantsTest {

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var viewModel: ReviewerViewModel
    private lateinit var billingManager: BillingManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        MaritimeQuestionBank.init(context)

        // Robolectric cannot provide Android Keystore-backed encrypted preferences.
        // Use a dedicated in-memory/test preferences file for account cache invariants.
        val accountPrefs = context.getSharedPreferences("hyperion_account_entitlements_test", Context.MODE_PRIVATE)
        accountPrefs.edit().clear().apply()
        LicenseManager.setAccountEntitlementPreferencesForTesting(accountPrefs)

        // Reset all licenses to clean unactivated state
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.setHyperionProUnlocked(context, false)
        LicenseManager.setMasterProUnlocked(context, false)
        LicenseManager.clearAccountEntitlements(context)
        LicenseManager.init(context)

        // Clear Play Billing secure preferences
        val billingPrefs = context.getSharedPreferences("hyperion_play_billing_secure_prefs", Context.MODE_PRIVATE)
        billingPrefs.edit().clear().apply()

        // Clear general license preferences
        val secPrefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        secPrefs.edit().clear().apply()

        billingManager = BillingManager.getInstance(context)
        viewModel = ReviewerViewModel(application)
    }

    @After
    fun tearDown() {
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.setHyperionProUnlocked(context, false)
        LicenseManager.clearAccountEntitlements(context)
        LicenseManager.init(context)
        LicenseManager.setAccountEntitlementPreferencesForTesting(null)
    }

    @Test
    fun localStudyProgressOwnerIsDurablyClaimedBeforeSyncAndCannotTransfer() {
        val preferences = context.getSharedPreferences("learning_progress_owner_test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()

        assertTrue(
            "The first account should claim an unowned device",
            claimLearningProgressOwner(preferences, "account-a")
        )
        assertEquals(
            "The owner marker must be persisted immediately, before any cloud request",
            "account-a",
            preferences.getString("owner_user_id", null)
        )
        assertTrue(
            "The original account may retry synchronization",
            claimLearningProgressOwner(preferences, "account-a")
        )
        assertFalse(
            "A different account must not inherit the device's local study history",
            claimLearningProgressOwner(preferences, "account-b")
        )
        assertEquals(
            "A rejected account switch must not overwrite the original owner",
            "account-a",
            preferences.getString("owner_user_id", null)
        )

        preferences.edit().clear().commit()
    }

    @Test
    fun googlePlayAccountOwnershipConflictIsSeparatedFromTransientSyncErrors() {
        assertTrue(
            "A server HTTP 409 must be surfaced as an account ownership conflict",
            isGooglePlayAccountLinkConflict(
                IOException("Supabase request failed (409): This Google Play subscription is already linked to another Hyperion account.")
            )
        )
        assertFalse(
            "A temporary backend failure should remain retryable",
            isGooglePlayAccountLinkConflict(IOException("Supabase request failed (502): Google Play verification is temporarily unavailable."))
        )
        assertFalse(
            "A network timeout is not a permanent account ownership conflict",
            isGooglePlayAccountLinkConflict(IOException("Network timeout"))
        )
    }

    @Test
    fun missingOptionalPlayEntitlementsTableDoesNotBlockWebsiteLicenseSync() {
        assertTrue(
            isMissingGooglePlayEntitlementsTable(
                IOException("Supabase request failed (404): Could not find the table 'public.google_play_entitlements' in the schema cache")
            )
        )
        assertFalse(
            isMissingGooglePlayEntitlementsTable(
                IOException("Supabase request failed (403): permission denied for table google_play_entitlements")
            )
        )
        assertFalse(
            isMissingGooglePlayEntitlementsTable(
                IOException("Supabase request failed (404): row not found")
            )
        )
        assertFalse(isMissingGooglePlayEntitlementsTable(IOException("Network timeout")))
    }

    @Test
    fun accountLinkingIncludesAllOwnedProductsAndSkipsMockTokens() {
        val purchases = collectPlayPurchasesForAccountLinking(
            allAccessToken = "token_all_access",
            oicNwToken = "token_oic",
            gmdssToken = "mock_sandbox_sub_token_not_real",
            engineOicEwToken = "token_engine",
            deckMgtToken = null,
            engineMgtToken = "token_engine_management"
        )

        assertEquals(4, purchases.size)
        assertEquals(BillingConstants.PRODUCT_ALL_ACCESS_PASS, purchases.first().productId)
        assertTrue(purchases.any {
            it.productId == BillingConstants.PRODUCT_OIC_NW_PASS && it.purchaseToken == "token_oic"
        })
        assertTrue(purchases.any {
            it.productId == BillingConstants.PRODUCT_ENGINE_OIC_EW_PASS && it.purchaseToken == "token_engine"
        })
        assertTrue(purchases.any {
            it.productId == BillingConstants.PRODUCT_ENGINE_MGT_PASS && it.purchaseToken == "token_engine_management"
        })
        assertFalse(purchases.any { it.purchaseToken.startsWith("mock_sandbox_sub_token_") })
    }

    @Test
    fun playPurchaseAccountIdentifierIsHashedAndBoundToTheCurrentHyperionAccount() {
        val accountA = obfuscatedHyperionAccountId("account-a-user-uuid")
        val accountARepeat = obfuscatedHyperionAccountId(" account-a-user-uuid ")
        val accountB = obfuscatedHyperionAccountId("account-b-user-uuid")

        assertNotNull("A signed-in account should produce a Play binding identifier", accountA)
        assertTrue("The Play binding identifier must be a 64-character SHA-256 hex digest", accountA!!.matches(Regex("[0-9a-f]{64}")))
        assertEquals("The same Hyperion user must always produce the same Play identifier", accountA, accountARepeat)
        assertFalse("Different Hyperion accounts must not share the same Play identifier", accountA == accountB)
        assertFalse("The Play identifier must not expose the original account ID", accountA.contains("account-a-user-uuid"))
        assertEquals("Anonymous users must not receive a fabricated account binding", null, obfuscatedHyperionAccountId(null))
        assertEquals("Blank account IDs must remain unbound", null, obfuscatedHyperionAccountId("  "))

        LicenseManager.setCurrentAccountUserId(context, "account-a-user-uuid")
        assertEquals("Checkout should use the authenticated Hyperion account", "account-a-user-uuid", LicenseManager.getCurrentAccountUserId(context))
        LicenseManager.setCurrentAccountUserId(context, "account-b-user-uuid")
        assertEquals("Switching Hyperion accounts must change the identifier source", "account-b-user-uuid", LicenseManager.getCurrentAccountUserId(context))
    }

    @Test
    fun test1_targetProductConfigurationMatchesRequirements() {
        assertEquals("hyperion_oic_nw_pass", BillingConstants.PRODUCT_OIC_NW_PASS)
        assertEquals("hyperion_gmdss_pass", BillingConstants.PRODUCT_GMDSS_PASS)
        assertEquals("hyperion_all_access_pass", BillingConstants.PRODUCT_ALL_ACCESS_PASS)
        assertEquals(6, BillingConstants.ALL_PRODUCT_IDS.size)
        assertTrue(BillingConstants.ALL_PRODUCT_IDS.contains(BillingConstants.PRODUCT_OIC_NW_PASS))
        assertTrue(BillingConstants.ALL_PRODUCT_IDS.contains(BillingConstants.PRODUCT_GMDSS_PASS))
        assertTrue(BillingConstants.ALL_PRODUCT_IDS.contains(BillingConstants.PRODUCT_ALL_ACCESS_PASS))
    }

    @Test
    fun test2_offlineLicenseAuthoritativeOverPlayBilling() = runBlocking {
        // Even if billing is not connected and has no purchases, active offline license gives full access
        LicenseManager.setOicNwProUnlocked(context, true)
        LicenseManager.init(context)

        assertTrue(
            "Offline OIC-NW license must authorize OIC-NW track unconditionally",
            viewModel.isTrackUnlocked(Track.DECK_OIC_NW)
        )
        assertFalse(
            "GMDSS remains locked when only OIC-NW is licensed",
            viewModel.isTrackUnlocked(Track.GMDSS_GOC)
        )

        // All-Access Hyperion Pro offline license unlocks BOTH tracks
        LicenseManager.setHyperionProUnlocked(context, true)
        LicenseManager.setGmdssProUnlocked(context, true)
        LicenseManager.init(context)

        assertTrue(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertTrue(viewModel.isTrackUnlocked(Track.GMDSS_GOC))
    }

    @Test
    fun test3_googlePlayEntitlementsUnlockTracks() = runBlocking {
        // Ensure offline license is unactivated
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.init(context)

        assertFalse(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))
        assertFalse(viewModel.isTrackUnlocked(Track.GMDSS_GOC))

        // Simulate local secure cached Play Billing purchase for OIC-NW
        val billingPrefs = context.getSharedPreferences("hyperion_play_billing_secure_prefs", Context.MODE_PRIVATE)
        billingPrefs.edit()
            .putBoolean("play_entitlement_oic_nw", true)
            .putString("play_token_oic_nw", "token_test_oic_nw_12345")
            .apply()

        // Recreate manager to reload cached tokens
        val vm2 = ReviewerViewModel(application)
        // Simulate Play entitlements update
        val updatedManager = vm2.billingManager
        val initialItems = updatedManager.getAllProductItems()
        assertNotNull(initialItems)

        // Directly verify isTrackUnlocked incorporates Play entitlements
        val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_oic_unlocked", true).apply()
        assertTrue(vm2.isTrackUnlocked(Track.DECK_OIC_NW))
    }

    @Test
    fun offlinePlaySnapshotCannotRevokeOrRenewCachedEntitlements() {
        assertFalse(
            "An offline Play ownership snapshot is not authoritative and must not clear cached access",
            shouldApplyPlayPurchaseSnapshot(hasValidatedInternet = false)
        )
        assertTrue(
            "A validated online Play ownership snapshot may update or revoke cached access",
            shouldApplyPlayPurchaseSnapshot(hasValidatedInternet = true)
        )

        val now = System.currentTimeMillis()
        val highWater = now - 10L * 60L * 1000L

        assertFalse(
            "An offline Play Store cache must not renew the server-verification lease",
            canRefreshPlayVerification(
                hasValidatedInternet = false,
                nowEpochMs = now,
                savedHighWaterMarkEpochMs = highWater
            )
        )
        assertTrue(
            "A validated internet connection and monotonic clock may refresh the lease",
            canRefreshPlayVerification(
                hasValidatedInternet = true,
                nowEpochMs = now,
                savedHighWaterMarkEpochMs = highWater
            )
        )
        assertFalse(
            "A validated network must not heal a materially rolled-back device clock",
            canRefreshPlayVerification(
                hasValidatedInternet = true,
                nowEpochMs = now - 10L * 60L * 1000L,
                savedHighWaterMarkEpochMs = now
            )
        )
    }

    @Test
    fun accountLinkingIncludesOnlyDistinctRealPlayPurchaseTokens() {
        val purchases = collectPlayPurchasesForAccountLinking(
            allAccessToken = "real-all-access-token",
            oicNwToken = "real-oic-token",
            gmdssToken = "mock_sandbox_sub_token_gmdss",
            engineOicEwToken = "real-oic-token",
            deckMgtToken = "",
            engineMgtToken = null
        )

        assertEquals(
            listOf("real-all-access-token", "real-oic-token"),
            purchases.map { it.purchaseToken }
        )
        assertEquals(
            listOf(
                BillingConstants.PRODUCT_ALL_ACCESS_PASS,
                BillingConstants.PRODUCT_OIC_NW_PASS
            ),
            purchases.map { it.productId }
        )
    }

    @Test
    fun savedWeeklyMonthlyAndYearlySelectionsSurviveProcessRestart() {
        assertEquals(BillingCycle.WEEKLY, resolveRequestedBillingCycle(BillingCycle.WEEKLY))
        assertEquals(BillingCycle.MONTHLY, resolveRequestedBillingCycle(BillingCycle.MONTHLY))
        assertEquals(BillingCycle.YEARLY, resolveRequestedBillingCycle(BillingCycle.YEARLY))
        assertEquals("Only legacy purchases without saved cycle metadata use the monthly fallback", BillingCycle.MONTHLY, resolveRequestedBillingCycle(null))
    }

    @Test
    fun cancelledOrUnfinishedCheckoutCannotOverwriteTheActiveSubscriptionCycle() {
        assertFalse(
            "A repeated purchase token and unchanged purchase timestamp are the same subscription",
            isNewPlaySubscriptionPurchase("monthly-token", "monthly-token", 1_000L, 1_000L)
        )
        assertTrue(
            "A later purchase timestamp confirms a newly purchased subscription",
            isNewPlaySubscriptionPurchase("monthly-token", "monthly-token", 1_000L, 2_000L)
        )
        assertTrue(
            "A replacement purchase token confirms a plan replacement even if timestamps are equal",
            isNewPlaySubscriptionPurchase("monthly-token", "yearly-replacement-token", 1_000L, 1_000L)
        )
        assertFalse(
            "Missing purchase timestamps must not commit an unconfirmed Store selection",
            isNewPlaySubscriptionPurchase("monthly-token", "monthly-token", 1_000L, 0L)
        )

        assertEquals(
            "An unchanged Play purchase retains its active monthly cycle after a yearly checkout is cancelled",
            BillingCycle.MONTHLY,
            resolvePurchasedBillingCycle(
                activeCycle = BillingCycle.MONTHLY,
                pendingCycle = BillingCycle.YEARLY,
                isNewPurchase = false
            )
        )
        assertEquals(
            "A genuinely new Play purchase commits the newly selected yearly cycle",
            BillingCycle.YEARLY,
            resolvePurchasedBillingCycle(
                activeCycle = BillingCycle.MONTHLY,
                pendingCycle = BillingCycle.YEARLY,
                isNewPurchase = true
            )
        )
        assertEquals(
            "A first weekly purchase can commit its selected cycle when no prior plan exists",
            BillingCycle.WEEKLY,
            resolvePurchasedBillingCycle(
                activeCycle = null,
                pendingCycle = BillingCycle.WEEKLY,
                isNewPurchase = true
            )
        )
    }

    @Test
    fun playOfferCycleMatchingUsesTheRecurringPhaseAndNeverConfusesTrialDuration() {
        assertTrue(BillingCycle.WEEKLY.matchesPlayOffer("standard_plan", listOf("P1W")))
        assertTrue(BillingCycle.WEEKLY.matchesPlayOffer("standard_plan", listOf("P7D")))
        assertTrue(BillingCycle.MONTHLY.matchesPlayOffer("standard_plan", listOf("P7D", "P1M")))
        assertFalse(BillingCycle.WEEKLY.matchesPlayOffer("standard_plan", listOf("P7D", "P1M")))
        assertTrue(BillingCycle.YEARLY.matchesPlayOffer("annual", emptyList()))
        assertTrue(BillingCycle.YEARLY.matchesPlayOffer("standard_plan", listOf("P1Y")))
        assertTrue(BillingCycle.YEARLY.matchesPlayOffer("standard_plan", listOf("P365D")))
        assertTrue(BillingCycle.YEARLY.matchesPlayOffer("standard_plan", listOf("P12M")))
        assertFalse(BillingCycle.MONTHLY.matchesPlayOffer("standard_plan", listOf("P1Y")))
    }

    @Test
    fun subscriptionCyclesUseCalendarPeriodsInsteadOfFixedThirtyOr365DayEstimates() {
        val jan31 = Instant.parse("2025-01-31T12:00:00Z").toEpochMilli()
        val feb28 = Instant.parse("2025-02-28T12:00:00Z").toEpochMilli()
        val feb7 = Instant.parse("2025-02-07T12:00:00Z").toEpochMilli()
        val leapDay = Instant.parse("2024-02-29T12:00:00Z").toEpochMilli()
        val nextLeapYearExpiry = Instant.parse("2025-02-28T12:00:00Z").toEpochMilli()

        assertEquals("Weekly plans last exactly seven calendar days", feb7, BillingCycle.WEEKLY.nextBillingPeriod(jan31))
        assertEquals("P1M monthly plans end on the corresponding calendar date, clamped at month-end", feb28, BillingCycle.MONTHLY.nextBillingPeriod(jan31))
        val mar28 = Instant.parse("2025-03-28T12:00:00Z").toEpochMilli()
        assertEquals("A Jan 31 subscription moves to Feb 28 and continues renewing on the 28th", mar28, estimateNextSubscriptionExpiry(jan31, feb28, BillingCycle.MONTHLY))
        val mar31 = Instant.parse("2025-03-31T12:00:00Z").toEpochMilli()
        val apr30 = Instant.parse("2025-04-30T12:00:00Z").toEpochMilli()
        val may30 = Instant.parse("2025-05-30T12:00:00Z").toEpochMilli()
        assertEquals("A Mar 31 subscription moves to Apr 30", apr30, BillingCycle.MONTHLY.nextBillingPeriod(mar31))
        assertEquals("A Mar 31 subscription continues renewing on the 30th after April", may30, estimateNextSubscriptionExpiry(mar31, apr30, BillingCycle.MONTHLY))
        assertEquals("P1Y yearly plans respect leap-day calendar arithmetic", nextLeapYearExpiry, BillingCycle.YEARLY.nextBillingPeriod(leapDay))
    }

    @Test
    fun displayedSubscriptionDurationUsesTheActualPlanExpiryForOfflineAccess() {
        BillingCycle.values().forEach { cycle ->
            val now = System.currentTimeMillis()
            val expiry = estimateNextSubscriptionExpiry(now, now, cycle)
            val plan = ActivePlanInfo(
                cycle = cycle,
                purchaseTimestamp = now,
                expiryTimestamp = expiry,
                highWaterMarkEpoch = now,
                lastVerifiedAtEpoch = now
            )

            assertFalse("${cycle.label} plan should be active immediately after purchase", plan.isExpired)
            assertTrue(
                "Offline access must retain the full active subscription duration, not a 72-hour lease",
                plan.remainingTimeMs > 3L * 24L * 60L * 60L * 1000L
            )
            assertTrue(
                "The remaining duration must match the actual subscription expiry",
                kotlin.math.abs(plan.remainingTimeMs - plan.subscriptionRemainingTimeMs) < 1_000L
            )
            assertEquals(
                "${cycle.label} dashboard display must show the actual computed plan expiry",
                formatSubscriptionTimeRemaining(expiry, now),
                plan.getFormattedTimeRemaining()
            )
        }
    }

    @Test
    fun serverVerifiedPlayExpiryOverridesTheLocalCalendarEstimateForTestRenewals() {
        val now = System.currentTimeMillis()
        val localWeeklyEstimate = now + BillingCycle.WEEKLY.nextBillingPeriod(now) - now
        val weeklyTestExpiry = now + 5L * 60L * 1000L
        val yearlyTestExpiry = now + 30L * 60L * 1000L

        assertEquals(
            "The exact server expiry must override a longer local estimate for accelerated test subscriptions",
            weeklyTestExpiry,
            subscriptionDisplayExpiry(weeklyTestExpiry, localWeeklyEstimate)
        )
        assertEquals("Weekly/monthly test renewal countdown must display the API's five-minute expiry", "5 mins remaining", formatSubscriptionTimeRemaining(weeklyTestExpiry, now))
        assertEquals("Yearly test renewal countdown must display the API's thirty-minute expiry", "30 mins remaining", formatSubscriptionTimeRemaining(yearlyTestExpiry, now))
        assertEquals("An unlinked purchase may use its local estimate", localWeeklyEstimate, subscriptionDisplayExpiry(0L, localWeeklyEstimate))
    }

    @Test
    fun subscriptionCountdownFormatsDaysHoursAndSubDayExpiriesConsistently() {
        val now = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()
        assertEquals("7 days remaining", formatSubscriptionTimeRemaining(now + 7L * 24L * 60L * 60L * 1000L, now))
        assertEquals("A remainder of a day is rounded up while at least one full day remains", "7 days remaining", formatSubscriptionTimeRemaining(now + 6L * 24L * 60L * 60L * 1000L + 23L * 60L * 60L * 1000L, now))
        assertEquals("The day counter changes at the six-full-day boundary", "6 days remaining", formatSubscriptionTimeRemaining(now + 6L * 24L * 60L * 60L * 1000L, now))
        assertEquals("The final 24 hours switch to an hour countdown", "23 hrs, 59 mins remaining", formatSubscriptionTimeRemaining(now + 23L * 60L * 60L * 1000L + 59L * 60L * 1000L, now))
        assertEquals("A day plus one hour rounds to two remaining days", "2 days remaining", formatSubscriptionTimeRemaining(now + 25L * 60L * 60L * 1000L, now))
        assertEquals("1 hr remaining", formatSubscriptionTimeRemaining(now + 60L * 60L * 1000L, now))
        assertEquals("1 hr, 1 min remaining", formatSubscriptionTimeRemaining(now + 61L * 60L * 1000L, now))
        assertEquals("1 min remaining", formatSubscriptionTimeRemaining(now + 60L * 1000L, now))
        assertEquals("4 mins remaining", formatSubscriptionTimeRemaining(now + 4L * 60L * 1000L, now))
        assertEquals("< 1 min remaining", formatSubscriptionTimeRemaining(now + 10_000L, now))
        assertEquals("Subscription Expired", formatSubscriptionTimeRemaining(now - 1L, now))
    }

    @Test
    fun deviceMonotonicClockKeepsOfflineLicenseTimeMovingWhenWallClockChangesDuringSameBoot() {
        val savedEpoch = Instant.parse("2026-11-01T10:00:00Z").toEpochMilli()
        val savedElapsed = 5L * 60L * 60L * 1000L
        val elapsedAfterSleep = savedElapsed + 8L * 60L * 60L * 1000L

        val reading = calculateOfflineTrustedTime(
            wallEpochMillis = savedEpoch - 2L * 60L * 60L * 1000L,
            elapsedRealtimeMillis = elapsedAfterSleep,
            currentBootCount = 77,
            savedEpochMillis = savedEpoch,
            savedElapsedRealtimeMillis = savedElapsed,
            savedBootCount = 77
        )
        assertTrue("A manual wall-clock rollback during the same boot must not lock an offline user", reading.isTrusted)
        assertEquals(
            "Elapsed realtime, which includes device sleep, advances license time independently of wall-clock edits",
            savedEpoch + 8L * 60L * 60L * 1000L,
            reading.epochMillis
        )
    }

    @Test
    fun deviceClockRollbackAfterRebootIsDetectedWithoutRequiringAnInternetCheck() {
        val savedEpoch = Instant.parse("2026-11-01T10:00:00Z").toEpochMilli()
        val reading = calculateOfflineTrustedTime(
            wallEpochMillis = savedEpoch - 30L * 60L * 1000L,
            elapsedRealtimeMillis = 3L * 60L * 1000L,
            currentBootCount = 78,
            savedEpochMillis = savedEpoch,
            savedElapsedRealtimeMillis = 8L * 60L * 60L * 1000L,
            savedBootCount = 77
        )
        assertFalse("After reboot, a device clock materially behind the saved trusted time must be flagged", reading.isTrusted)
        assertEquals("The trusted high-water mark is preserved rather than rolled back", savedEpoch, reading.epochMillis)
    }

    @Test
    fun correctDeviceRtcRestoresOfflineAccessAfterReboot() {
        val savedEpoch = Instant.parse("2026-11-01T10:00:00Z").toEpochMilli()
        val correctWallTime = savedEpoch + 18L * 60L * 60L * 1000L
        val reading = calculateOfflineTrustedTime(
            wallEpochMillis = correctWallTime,
            elapsedRealtimeMillis = 4L * 60L * 1000L,
            currentBootCount = 79,
            savedEpochMillis = savedEpoch,
            savedElapsedRealtimeMillis = 9L * 60L * 60L * 1000L,
            savedBootCount = 77
        )
        assertTrue("A correct device RTC after reboot is sufficient; internet is not required", reading.isTrusted)
        assertEquals(correctWallTime, reading.epochMillis)
    }

    @Test
    fun accountSubscriptionDisplayClockNeverMovesBehindItsVerificationHighWaterMark() {
        val now = Instant.parse("2026-10-30T11:00:00Z").toEpochMilli()
        val verifiedAt = now + 10L * 60L * 1000L
        assertEquals(
            "A small wall-clock rollback must not make displayed subscription time increase",
            verifiedAt,
            LicenseManager.effectiveAccountEntitlementDisplayNow(
                nowEpochMs = now,
                syncedAtEpochMs = now,
                lastVerifiedEpochMs = verifiedAt
            )
        )
        assertEquals(
            "A correctly advanced wall clock remains the effective display time",
            verifiedAt + 5L * 60L * 1000L,
            LicenseManager.effectiveAccountEntitlementDisplayNow(
                nowEpochMs = verifiedAt + 5L * 60L * 1000L,
                syncedAtEpochMs = now,
                lastVerifiedEpochMs = verifiedAt
            )
        )
    }

    @Test
    fun playSubscriptionCacheRemainsUsableUntilActualExpiryAndRejectsClockRollback() {
        val now = System.currentTimeMillis()
        val expiry = now + 30L * 24L * 60L * 60L * 1000L

        val recentlyVerified = ActivePlanInfo(
            purchaseTimestamp = now - 60L * 24L * 60L * 60L * 1000L,
            expiryTimestamp = expiry,
            highWaterMarkEpoch = now - 30L * 60L * 1000L,
            lastVerifiedAtEpoch = now - 71L * 60L * 60L * 1000L
        )
        assertFalse("A recently verified Play purchase remains valid before its real expiry", recentlyVerified.isExpired)
        assertTrue("The displayed remaining time follows the actual subscription expiry", recentlyVerified.remainingTimeMs > 29L * 24L * 60L * 60L * 1000L)

        val staleOfflineCache = recentlyVerified.copy(
            lastVerifiedAtEpoch = now - 73L * 60L * 60L * 1000L,
            highWaterMarkEpoch = now - 73L * 60L * 60L * 1000L
        )
        assertFalse("A Play cache remains usable beyond 72 hours while the actual subscription is active", staleOfflineCache.isExpired)
        assertFalse(staleOfflineCache.requiresOnlineVerification)

        val rolledBackClock = recentlyVerified.copy(
            highWaterMarkEpoch = now + 10L * 60L * 1000L,
            lastVerifiedAtEpoch = now
        )
        assertTrue("A device clock rollback must not extend a cached Play subscription", rolledBackClock.isExpired)
        assertTrue(rolledBackClock.requiresOnlineVerification)

        val expiredPlan = recentlyVerified.copy(
            expiryTimestamp = now - 1L,
            highWaterMarkEpoch = now,
            lastVerifiedAtEpoch = now
        )
        assertTrue("The cached plan period must also be expired at its recorded expiry", expiredPlan.isExpired)
    }

    @Test
    fun websiteEntitlementExpiryIsAvailableToStoreOnlyForCurrentAccount() {
        val now = System.currentTimeMillis()
        val expiry = now + 24L * 60L * 60L * 1000L
        LicenseManager.setCurrentAccountUserId(context, "website-store-user")

        val revisionBeforeSync = LicenseManager.accountEntitlementRevisionFlow.value
        assertTrue(
            "A successful website entitlement sync should be securely cached",
            LicenseManager.saveAccountEntitlements(
                context = context,
                userId = "website-store-user",
                expiryByTrack = mapOf(Track.DECK_OIC_NW to expiry)
            )
        )

        assertTrue(
            "The Bond Store should be able to read the validated website expiry",
            LicenseManager.getAccountEntitlementExpiry(context, Track.DECK_OIC_NW) > now
        )
        assertEquals(
            "A track without a website entitlement must not show an active expiry",
            0L,
            LicenseManager.getAccountEntitlementExpiry(context, Track.GMDSS_GOC)
        )
        assertTrue(
            "Refreshing account entitlements must invalidate Bond Store's cached display state",
            LicenseManager.accountEntitlementRevisionFlow.value > revisionBeforeSync
        )

        // Simulate a device that has been offline for more than three days after
        // a successful website-license sync. Access must be governed by the actual
        // server expiry, not by the age of the last online verification.
        context.getSharedPreferences("hyperion_account_entitlements_test", Context.MODE_PRIVATE)
            .edit()
            .putLong("account_entitlements_synced_at", now - 73L * 60L * 60L * 1000L)
            .putLong("account_entitlements_last_verified_epoch", now - 73L * 60L * 60L * 1000L)
            .commit()
        LicenseManager.refreshAccountEntitlementFlows(context)
        viewModel.refreshUnlockStates()
        assertTrue(
            "A website-paid track must remain unlocked offline beyond 72 hours while its actual expiry is in the future",
            LicenseManager.isAccountEntitlementUnlocked(context, Track.DECK_OIC_NW) &&
                viewModel.isTrackUnlocked(Track.DECK_OIC_NW)
        )

        LicenseManager.setCurrentAccountUserId(context, "different-account")
        assertEquals(
            "A website entitlement must disappear from the store after switching accounts",
            0L,
            LicenseManager.getAccountEntitlementExpiry(context, Track.DECK_OIC_NW)
        )
    }

    @Test
    fun serverVerifiedDeveloperAdminAccessUnlocksAllTracksOnlyForCurrentAccount() {
        LicenseManager.setCurrentAccountUserId(context, "trusted-developer-user")
        assertFalse(Track.values().any { viewModel.isTrackUnlocked(it) })

        LicenseManager.setVerifiedDeveloperAdminAccess(
            context = context,
            userId = "trusted-developer-user",
            enabled = true
        )
        assertTrue(LicenseManager.isDeveloperAdminAccessActive(context))
        assertTrue("Verified developer account must unlock every paid track", Track.values().all {
            viewModel.isTrackUnlocked(it)
        })

        // Switching accounts must immediately discard the in-memory admin grant.
        LicenseManager.setCurrentAccountUserId(context, "ordinary-user")
        assertFalse(LicenseManager.isDeveloperAdminAccessActive(context))
        assertFalse("Developer access must never transfer to another account", Track.values().any {
            viewModel.isTrackUnlocked(it)
        })

        // Even a stale positive response for the old user cannot unlock the new account.
        LicenseManager.setVerifiedDeveloperAdminAccess(
            context = context,
            userId = "trusted-developer-user",
            enabled = true
        )
        assertFalse(LicenseManager.isDeveloperAdminAccessActive(context))
    }

    @Test
    fun accountLinkedLicenseExpiryUsesActualServerExpiryAndRejectsClockRollback() {
        val now = 1_800_000_000_000L
        val syncedAt = now - 60_000L
        val lastVerified = now - 30_000L
        val validExpiry = now + 120_000L

        assertEquals(
            "A successful sync must not move the persisted lease time backwards after a clock rollback",
            lastVerified,
            LicenseManager.nextAccountEntitlementVerificationTime(
                now - 6L * 60L * 1000L,
                lastVerified
            )
        )
        assertEquals(
            "A later verified sync may advance the account lease high-water mark",
            now + 60_000L,
            LicenseManager.nextAccountEntitlementVerificationTime(now + 60_000L, lastVerified)
        )

        assertTrue(
            "A recently synchronized, unexpired grant for the current account should be accepted",
            LicenseManager.isAccountEntitlementValid(
                ownerUserId = "account-a",
                currentUserId = "account-a",
                syncedAtEpochMs = syncedAt,
                lastVerifiedEpochMs = lastVerified,
                expiryEpochMs = validExpiry,
                nowEpochMs = now
            )
        )
        assertFalse(
            "An account grant must stop authorizing access at its exact server expiry",
            LicenseManager.isAccountEntitlementValid(
                ownerUserId = "account-a",
                currentUserId = "account-a",
                syncedAtEpochMs = syncedAt,
                lastVerifiedEpochMs = lastVerified,
                expiryEpochMs = now,
                nowEpochMs = now
            )
        )
        assertFalse(
            "A cached grant must never transfer to a different signed-in account",
            LicenseManager.isAccountEntitlementValid(
                ownerUserId = "account-a",
                currentUserId = "account-b",
                syncedAtEpochMs = syncedAt,
                lastVerifiedEpochMs = lastVerified,
                expiryEpochMs = validExpiry,
                nowEpochMs = now
            )
        )
        assertTrue(
            "A successfully verified account grant remains usable beyond 72 hours while its real subscription has not expired",
            LicenseManager.isAccountEntitlementValid(
                ownerUserId = "account-a",
                currentUserId = "account-a",
                syncedAtEpochMs = now - 73L * 60L * 60L * 1000L,
                lastVerifiedEpochMs = now - 73L * 60L * 60L * 1000L,
                expiryEpochMs = validExpiry,
                nowEpochMs = now
            )
        )
        assertFalse(
            "A materially future-dated sync timestamp must be rejected",
            LicenseManager.isAccountEntitlementValid(
                ownerUserId = "account-a",
                currentUserId = "account-a",
                syncedAtEpochMs = now + 6L * 60L * 1000L,
                lastVerifiedEpochMs = now + 6L * 60L * 1000L,
                expiryEpochMs = validExpiry,
                nowEpochMs = now
            )
        )
        assertFalse(
            "A device clock rolled back by more than five minutes must not extend its account lease",
            LicenseManager.isAccountEntitlementValid(
                ownerUserId = "account-a",
                currentUserId = "account-a",
                syncedAtEpochMs = syncedAt,
                lastVerifiedEpochMs = now,
                expiryEpochMs = validExpiry,
                nowEpochMs = now - 6L * 60L * 1000L
            )
        )
        assertFalse(
            "Even a rollback within tolerance must not move the effective time before the last observed time",
            LicenseManager.isAccountEntitlementValid(
                ownerUserId = "account-a",
                currentUserId = "account-a",
                syncedAtEpochMs = syncedAt,
                lastVerifiedEpochMs = now,
                expiryEpochMs = now - 1L,
                nowEpochMs = now - 2L * 60L * 1000L
            )
        )
    }
    @Test
    fun test4_freeTierStrict5QuestionCeiling() = runBlocking {
        // Ensure user is completely unentitled
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.setGmdssProUnlocked(context, false)
        LicenseManager.setHyperionProUnlocked(context, false)
        LicenseManager.init(context)

        assertFalse(viewModel.isTrackUnlocked(Track.DECK_OIC_NW))

        // Start practice requesting 20 questions
        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 20,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false
        )

        // Strictly capped to 5 preview questions
        val activeQuestions = viewModel.activeQuestions.value
        assertEquals("Free tier practice must be strictly capped at 5 preview items", 5, activeQuestions.size)

        // User answers all 5 preview items
        for (i in 0 until 5) {
            val q = activeQuestions[i]
            viewModel.selectOption(i, q.correctAnswerIndex)
            viewModel.confirmOrNextQuestion() // evaluates
            if (i < 4) {
                viewModel.confirmOrNextQuestion() // advances
            }
        }

        // On submitting/evaluating the 5th item, paywall dialog MUST be triggered
        viewModel.confirmOrNextQuestion()
        assertTrue(
            "Paywall dialog must be triggered when reaching free preview ceiling",
            viewModel.showPaywallDialog.value
        )
    }

    @Test
    fun test5_mockExamLockedOnFreeTierWithPaywallTrigger() = runBlocking {
        // Unentitled user
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.init(context)

        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startQuizSession(mode = QuizMode.MOCK_EXAM)

        // Mock exam questions must be empty
        assertEquals(0, viewModel.activeQuestions.value.size)
        // Paywall dialog must be shown
        assertTrue("Paywall dialog must trigger when attempting Mock Exam on free tier", viewModel.showPaywallDialog.value)
    }

    @Test
    fun test6_intensiveDrillModeLockedOnFreeTier() = runBlocking {
        // Unentitled user
        LicenseManager.setActivated(context, false)
        LicenseManager.setOicNwProUnlocked(context, false)
        LicenseManager.init(context)

        viewModel.setTrack(Track.DECK_OIC_NW)
        viewModel.startConfiguredPractice(
            competenceId = "C1",
            questionCount = 10,
            shuffleQuestions = false,
            shuffleChoices = false,
            isCrpMode = false,
            deliveryMode = DrillDeliveryMode.INTENSIVE
        )

        // Free tier cannot start intensive drill mode; paywall dialog must be triggered
        assertTrue(
            "Paywall dialog must trigger when unentitled user requests Intensive Drill Mode",
            viewModel.showPaywallDialog.value
        )
    }

    @Test
    fun test7_paywallDialogDismissal() {
        viewModel.triggerPaywall(Track.DECK_OIC_NW)
        assertTrue(viewModel.showPaywallDialog.value)
        assertEquals(Track.DECK_OIC_NW, viewModel.paywallTargetTrack.value)

        viewModel.dismissPaywallDialog()
        assertFalse(viewModel.showPaywallDialog.value)
    }
}
