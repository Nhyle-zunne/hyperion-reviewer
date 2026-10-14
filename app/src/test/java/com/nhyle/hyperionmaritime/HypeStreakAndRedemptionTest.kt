package com.nhyle.hyperionmaritime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhyle.hyperionmaritime.data.hype.HypePassManager
import com.nhyle.hyperionmaritime.data.hype.HypePassRecord
import com.nhyle.hyperionmaritime.data.hype.HypeRedemptionOption
import com.nhyle.hyperionmaritime.data.hype.HypeRewardType
import com.nhyle.hyperionmaritime.data.hype.RedemptionBlockReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HypeStreakAndRedemptionTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences(HypePassManager.VAULT_PREFS, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        HypePassManager.isSuperRewardUnlocked = false
        HypePassManager.init(context)
    }

    @Test
    fun testCase1_FirstCheckIn() {
        assertFalse("Initially should not be checked in today", HypePassManager.hasCheckedInTodayFlow.value)
        assertEquals(0, HypePassManager.streakCountFlow.value)
        assertEquals(0, HypePassManager.hypeBalanceFlow.value)

        val result = HypePassManager.performDailyCheckIn(context)
        assertTrue("First check-in should succeed", result.success)
        assertEquals("Streak should be 1 after first check-in", 1, result.currentStreak)
        assertEquals("No Hype awarded on day 1", 0, result.hypeAwarded)
        assertEquals(1, HypePassManager.streakCountFlow.value)
        assertTrue(HypePassManager.hasCheckedInTodayFlow.value)
        assertFalse(HypePassManager.streakBrokenFlow.value)
    }

    @Test
    fun testCase2_MissedDayBreaksStreak() {
        // Perform day 1 check-in
        HypePassManager.performDailyCheckIn(context)
        assertEquals(1, HypePassManager.streakCountFlow.value)

        // Simulate that last check-in was 3 days ago
        val prefs = context.getSharedPreferences(HypePassManager.VAULT_PREFS, Context.MODE_PRIVATE)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        val threeDaysAgoStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        prefs.edit()
            .putString("vault_last_checkin_date", threeDaysAgoStr)
            .putLong("vault_last_checkin_wall_time", cal.timeInMillis)
            .apply()

        // Re-evaluate date rollover
        HypePassManager.checkDateRollover(context)

        assertTrue("Streak should be marked broken", HypePassManager.streakBrokenFlow.value)
        assertEquals("Streak count should reset to 0", 0, HypePassManager.streakCountFlow.value)
        assertFalse("User has not checked in today", HypePassManager.hasCheckedInTodayFlow.value)

        // Check in now restarts streak to 1 and clears broken state
        val checkInResult = HypePassManager.performDailyCheckIn(context)
        assertTrue(checkInResult.success)
        assertEquals(1, checkInResult.currentStreak)
        assertFalse("Broken state must clear after check-in", HypePassManager.streakBrokenFlow.value)
    }

    @Test
    fun testCase3_SeventhDayReward() {
        val prefs = context.getSharedPreferences(HypePassManager.VAULT_PREFS, Context.MODE_PRIVATE)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        // Setup state at Day 6 consecutive check-ins
        prefs.edit()
            .putInt("vault_streak_count", 6)
            .putInt("vault_hype_balance", 0)
            .putString("vault_last_checkin_date", yesterdayStr)
            .putLong("vault_last_checkin_wall_time", cal.timeInMillis)
            .apply()

        HypePassManager.init(context)
        assertEquals(6, HypePassManager.streakCountFlow.value)

        // Perform 7th check-in today
        val result = HypePassManager.performDailyCheckIn(context)
        assertTrue(result.success)
        assertTrue("Must be flagged as 7th day reward", result.isSeventhDayReward)
        assertEquals("Must award +20 Hype", 20, result.hypeAwarded)
        assertEquals("Hype balance must be 20", 20, HypePassManager.hypeBalanceFlow.value)
        assertEquals("Streak count must restart at 0 after 7th check-in", 0, HypePassManager.streakCountFlow.value)
    }

    @Test
    fun testCase4_MidnightRolloverWhileAppIsOpen() {
        HypePassManager.performDailyCheckIn(context)
        assertTrue(HypePassManager.hasCheckedInTodayFlow.value)

        // Simulate date rollover to next day
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        val prefs = context.getSharedPreferences(HypePassManager.VAULT_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString("vault_last_checkin_date", yesterday).apply()

        HypePassManager.checkDateRollover(context)

        assertFalse("Check-in should become available again after midnight rollover", HypePassManager.hasCheckedInTodayFlow.value)
    }

    @Test
    fun testCase5_OfflineCheckIn() {
        // App is fully offline with zero internet access
        val result = HypePassManager.performDailyCheckIn(context)
        assertTrue("Check-in must succeed 100% offline at sea", result.success)
        assertTrue(HypePassManager.hasCheckedInTodayFlow.value)
    }

    @Test
    fun testCase6_ClockMovedBackwardAntiTamper() {
        HypePassManager.performDailyCheckIn(context)

        // Simulate device time jumping backward by 2 hours
        val prefs = context.getSharedPreferences(HypePassManager.VAULT_PREFS, Context.MODE_PRIVATE)
        val futureTime = System.currentTimeMillis() + (2L * 3600L * 1000L)
        prefs.edit()
            .putLong("vault_last_checkin_wall_time", futureTime)
            .apply()

        // Attempting to check in when current time is earlier than recorded wall time
        val result = HypePassManager.performDailyCheckIn(context)
        assertFalse("Check-in must be rejected when clock moves backward", result.success)
        assertTrue(result.message.contains("backward", ignoreCase = true))
    }

    @Test
    fun testCase7_ClockMovedForwardAntiTamper() {
        HypePassManager.performDailyCheckIn(context)

        // Simulate calendar jumping forward 5 days without elapsed real time
        val prefs = context.getSharedPreferences(HypePassManager.VAULT_PREFS, Context.MODE_PRIVATE)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -5)
        val fiveDaysAgo = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        prefs.edit()
            .putString("vault_last_checkin_date", fiveDaysAgo)
            .putInt("vault_streak_count", 4)
            .apply()

        HypePassManager.checkDateRollover(context)
        assertTrue("Streak must break when days are skipped", HypePassManager.streakBrokenFlow.value)
        assertEquals("Streak count must return to 0", 0, HypePassManager.streakCountFlow.value)
    }

    @Test
    fun testCase8_TimezoneChangePreservesDate() {
        // Same calendar day check-in attempt under timezone shift
        HypePassManager.performDailyCheckIn(context)
        val todayStr = HypePassManager.getTodayDateString()

        // Difference with same date should be 0 days
        val diff = HypePassManager.getDaysDifference(todayStr, todayStr)
        assertEquals("Same day difference is 0", 0L, diff)
        assertTrue(HypePassManager.hasCheckedInTodayFlow.value)
    }

    @Test
    fun testCase9_InsufficientHypeBlocksRedeem() {
        val option = HypePassManager.availableOptions.first { it.id == "deck_3d" } // Cost: 100 Hype
        val reason = HypePassManager.evaluateBlockReason(
            option = option,
            isDeckActive = false,
            isRadioActive = false,
            isAllAccessActive = false,
            activePass = null,
            userBalance = 50 // Only 50 Hype
        )

        assertTrue(reason is RedemptionBlockReason.InsufficientHype)
        assertEquals("INSUFFICIENT HYPE (NEED 100)", (reason as RedemptionBlockReason.InsufficientHype).getFormattedReason())

        // Attempting redemption should return false
        val redeemed = HypePassManager.redeemOption(context, option)
        assertFalse("Redemption must fail when balance is insufficient", redeemed)
    }

    @Test
    fun testCase10_RedeemBlockedByActiveGooglePlaySubscription() {
        val deckOption = HypePassManager.availableOptions.first { it.id == "deck_3d" }
        val radioOption = HypePassManager.availableOptions.first { it.id == "gmdss_3d" }
        val allAccessOption = HypePassManager.availableOptions.first { it.id == "all_3d" }

        // User has active Deck subscription
        val deckReason = HypePassManager.evaluateBlockReason(
            option = deckOption,
            isDeckActive = true,
            isRadioActive = false,
            isAllAccessActive = false,
            activePass = null,
            userBalance = 500
        )
        assertTrue("Deck reward must be blocked when Deck subscription is active", deckReason is RedemptionBlockReason.OverlappingLicense)

        val allReason = HypePassManager.evaluateBlockReason(
            option = allAccessOption,
            isDeckActive = true,
            isRadioActive = false,
            isAllAccessActive = false,
            activePass = null,
            userBalance = 500
        )
        assertTrue("ALL-ACCESS reward must be blocked when Deck subscription is active", allReason is RedemptionBlockReason.OverlappingLicense)

        val radioReason = HypePassManager.evaluateBlockReason(
            option = radioOption,
            isDeckActive = true,
            isRadioActive = false,
            isAllAccessActive = false,
            activePass = null,
            userBalance = 500
        )
        assertEquals("Radio reward must remain available when only Deck is active", RedemptionBlockReason.None, radioReason)
    }

    @Test
    fun testCase11_RedeemBlockedByKeyActivatedLicense() {
        val allAccessOption = HypePassManager.availableOptions.first { it.id == "all_7d" }
        val deckOption = HypePassManager.availableOptions.first { it.id == "deck_7d" }
        val radioOption = HypePassManager.availableOptions.first { it.id == "gmdss_7d" }

        // All-Access SHA-256 offline key is active
        val reasonAll = HypePassManager.evaluateBlockReason(
            option = allAccessOption,
            isDeckActive = true,
            isRadioActive = true,
            isAllAccessActive = true,
            activePass = null,
            userBalance = 1000
        )
        assertTrue(reasonAll is RedemptionBlockReason.OverlappingLicense)

        val reasonDeck = HypePassManager.evaluateBlockReason(
            option = deckOption,
            isDeckActive = true,
            isRadioActive = true,
            isAllAccessActive = true,
            activePass = null,
            userBalance = 1000
        )
        assertTrue("Deck must be blocked when All-Access is active", reasonDeck is RedemptionBlockReason.OverlappingLicense)

        val reasonRadio = HypePassManager.evaluateBlockReason(
            option = radioOption,
            isDeckActive = true,
            isRadioActive = true,
            isAllAccessActive = true,
            activePass = null,
            userBalance = 1000
        )
        assertTrue("Radio must be blocked when All-Access is active", reasonRadio is RedemptionBlockReason.OverlappingLicense)
    }

    @Test
    fun testCase12_RedeemBlockedByActiveHypePass() {
        // User currently has an active 3-Day Deck Hype pass
        val activeDeckPass = HypePassRecord(
            id = "pass_1",
            rewardType = HypeRewardType.DECK_OIC_NW,
            durationDays = 3,
            startTimeEpoch = System.currentTimeMillis(),
            expiryTimeEpoch = System.currentTimeMillis() + (3L * 24L * 3600L * 1000L),
            costHype = 100
        )

        val deckOption = HypePassManager.availableOptions.first { it.id == "deck_7d" }
        val allOption = HypePassManager.availableOptions.first { it.id == "all_3d" }
        val radioOption = HypePassManager.availableOptions.first { it.id == "gmdss_3d" }

        val deckReason = HypePassManager.evaluateBlockReason(
            option = deckOption,
            isDeckActive = false,
            isRadioActive = false,
            isAllAccessActive = false,
            activePass = activeDeckPass,
            userBalance = 500
        )
        assertTrue("Deck pass must be blocked when Deck Hype pass is active", deckReason is RedemptionBlockReason.OverlappingLicense)

        val allReason = HypePassManager.evaluateBlockReason(
            option = allOption,
            isDeckActive = false,
            isRadioActive = false,
            isAllAccessActive = false,
            activePass = activeDeckPass,
            userBalance = 500
        )
        assertTrue("All-Access pass must be blocked when Deck Hype pass is active", allReason is RedemptionBlockReason.OverlappingLicense)

        val radioReason = HypePassManager.evaluateBlockReason(
            option = radioOption,
            isDeckActive = false,
            isRadioActive = false,
            isAllAccessActive = false,
            activePass = activeDeckPass,
            userBalance = 500
        )
        assertEquals("Radio reward remains available", RedemptionBlockReason.None, radioReason)
    }

    @Test
    fun testCase13_CrossCoverageBlocking() {
        val allOption = HypePassManager.availableOptions.first { it.id == "all_3d" }
        val radioOption = HypePassManager.availableOptions.first { it.id == "gmdss_3d" }

        // OIC-NW active blocks ALL-ACCESS, but GMDSS stays available
        val allReason = HypePassManager.evaluateBlockReason(
            option = allOption,
            isDeckActive = true,
            isRadioActive = false,
            isAllAccessActive = false,
            activePass = null,
            userBalance = 500
        )
        assertTrue("OIC-NW active must block ALL-ACCESS", allReason is RedemptionBlockReason.OverlappingLicense)

        val radioReason = HypePassManager.evaluateBlockReason(
            option = radioOption,
            isDeckActive = true,
            isRadioActive = false,
            isAllAccessActive = false,
            activePass = null,
            userBalance = 500
        )
        assertEquals("GMDSS reward stays available", RedemptionBlockReason.None, radioReason)
    }

    @Test
    fun testCase14_UnblockAfterExpiry() {
        val deckOption = HypePassManager.availableOptions.first { it.id == "deck_3d" }

        // Pass expired 1 hour ago
        val expiredPass = HypePassRecord(
            id = "pass_old",
            rewardType = HypeRewardType.DECK_OIC_NW,
            durationDays = 3,
            startTimeEpoch = System.currentTimeMillis() - (4L * 24L * 3600L * 1000L),
            expiryTimeEpoch = System.currentTimeMillis() - (3600L * 1000L),
            costHype = 100
        )
        assertTrue(expiredPass.isExpired())

        val reason = HypePassManager.evaluateBlockReason(
            option = deckOption,
            isDeckActive = false,
            isRadioActive = false,
            isAllAccessActive = false,
            activePass = expiredPass,
            userBalance = 300
        )
        assertEquals("Expired pass must unblock rewards", RedemptionBlockReason.None, reason)
    }
}
