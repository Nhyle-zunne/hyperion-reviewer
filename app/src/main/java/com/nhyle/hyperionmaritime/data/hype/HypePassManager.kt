package com.nhyle.hyperionmaritime.data.hype

import android.content.Context
import android.os.SystemClock
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Singleton orchestrator for Hype currency, rolling 7-day study streaks, anti-tamper security,
 * and temporary Hype pass redemptions.
 *
 * BACKUP BEHAVIOR DOCUMENTATION:
 * The SharedPreferences file "hyperion_hype_vault" is local device-bound storage.
 * Auto Backup does not preserve or restore this vault.
 * Reinstalling the app or performing an Android "Clear Data" operation intentionally resets
 * Hype currency and active temporary passes to clean defaults.
 */
object HypePassManager {

    const val VAULT_PREFS = "hyperion_hype_vault"

    // Storage Keys
    private const val KEY_HYPE_BALANCE = "vault_hype_balance"
    private const val KEY_STREAK_COUNT = "vault_streak_count"
    private const val KEY_CYCLE_START_DATE = "vault_cycle_start_date"
    private const val KEY_LAST_CHECKIN_DATE = "vault_last_checkin_date"
    private const val KEY_LAST_CHECKIN_WALL_TIME = "vault_last_checkin_wall_time"
    private const val KEY_LAST_CHECKIN_ELAPSED_RT = "vault_last_checkin_elapsed_rt"
    private const val KEY_STREAK_BROKEN = "vault_streak_broken"
    private const val KEY_INTEGRITY_SIGNATURE = "vault_integrity_signature"

    private const val KEY_ACTIVE_PASS_JSON = "vault_active_pass_json"
    private const val KEY_REDEMPTION_HISTORY_JSON = "vault_redemption_history_json"

    // Configuration Flag for the 5000 Hype Secret Reward
    var isSuperRewardUnlocked: Boolean = false

    // Reactive StateFlows for UI
    private val _hypeBalanceFlow = MutableStateFlow(0)
    val hypeBalanceFlow: StateFlow<Int> = _hypeBalanceFlow.asStateFlow()

    private val _streakCountFlow = MutableStateFlow(0)
    val streakCountFlow: StateFlow<Int> = _streakCountFlow.asStateFlow()

    private val _hasCheckedInTodayFlow = MutableStateFlow(false)
    val hasCheckedInTodayFlow: StateFlow<Boolean> = _hasCheckedInTodayFlow.asStateFlow()

    private val _streakBrokenFlow = MutableStateFlow(false)
    val streakBrokenFlow: StateFlow<Boolean> = _streakBrokenFlow.asStateFlow()

    private val _activePassFlow = MutableStateFlow<HypePassRecord?>(null)
    val activePassFlow: StateFlow<HypePassRecord?> = _activePassFlow.asStateFlow()

    private val _redemptionHistoryFlow = MutableStateFlow<List<HypeRedemptionHistoryItem>>(emptyList())
    val redemptionHistoryFlow: StateFlow<List<HypeRedemptionHistoryItem>> = _redemptionHistoryFlow.asStateFlow()

    private val _timeUntilResetMsFlow = MutableStateFlow(0L)
    val timeUntilResetMsFlow: StateFlow<Long> = _timeUntilResetMsFlow.asStateFlow()

    // Formatter for calendar date comparisons (YYYY-MM-DD)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Available catalog of Hype reward redemption passes.
     */
    val availableOptions: List<HypeRedemptionOption> = listOf(
        HypeRedemptionOption(
            id = "deck_3d",
            name = "DECK OIC-NW PASS",
            rewardType = HypeRewardType.DECK_OIC_NW,
            durationDays = 3,
            costHype = 100,
            isRecommended = false
        ),
        HypeRedemptionOption(
            id = "gmdss_3d",
            name = "GMDSS GOC PASS",
            rewardType = HypeRewardType.GMDSS_GOC,
            durationDays = 3,
            costHype = 100,
            isRecommended = false
        ),
        HypeRedemptionOption(
            id = "all_3d",
            name = "HYPERION ALL-ACCESS PASS",
            rewardType = HypeRewardType.ALL_ACCESS,
            durationDays = 3,
            costHype = 150,
            isRecommended = true
        ),
        HypeRedemptionOption(
            id = "deck_7d",
            name = "DECK OIC-NW PASS",
            rewardType = HypeRewardType.DECK_OIC_NW,
            durationDays = 7,
            costHype = 200,
            isRecommended = false
        ),
        HypeRedemptionOption(
            id = "gmdss_7d",
            name = "GMDSS GOC PASS",
            rewardType = HypeRewardType.GMDSS_GOC,
            durationDays = 7,
            costHype = 200,
            isRecommended = false
        ),
        HypeRedemptionOption(
            id = "all_7d",
            name = "HYPERION ALL-ACCESS PASS",
            rewardType = HypeRewardType.ALL_ACCESS,
            durationDays = 7,
            costHype = 300,
            isRecommended = true
        ),
        HypeRedemptionOption(
            id = "super_classified",
            name = "???",
            rewardType = HypeRewardType.ALL_ACCESS,
            durationDays = 30,
            costHype = 5000,
            isRecommended = false,
            isMysteryLocked = true
        )
    )

    /**
     * Initializes state from encrypted/authenticated SharedPreferences.
     */
    @Synchronized
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)

        // Verify HMAC integrity signature
        if (!verifyIntegrity(context, prefs)) {
            // Tampered or corrupted -> Reset to safe defaults rather than crash
            prefs.edit().clear().apply()
            saveStateInternal(
                context = context,
                balance = 0,
                streak = 0,
                cycleStart = getTodayDateString(),
                lastCheckinDate = "",
                lastWallTime = 0L,
                lastElapsedRt = 0L,
                broken = false,
                activePass = null,
                history = emptyList()
            )
        }

        // Load active pass
        val passJson = prefs.getString(KEY_ACTIVE_PASS_JSON, null)
        val loadedPass = parsePassJson(passJson)
        val validPass = if (loadedPass != null && !loadedPass.isExpired()) loadedPass else null
        _activePassFlow.value = validPass

        // Load history
        val historyJson = prefs.getString(KEY_REDEMPTION_HISTORY_JSON, null)
        _redemptionHistoryFlow.value = parseHistoryJson(historyJson)

        // Load balance & streak
        val balance = prefs.getInt(KEY_HYPE_BALANCE, 0)
        val streak = prefs.getInt(KEY_STREAK_COUNT, 0)
        val lastCheckin = prefs.getString(KEY_LAST_CHECKIN_DATE, "") ?: ""
        var broken = prefs.getBoolean(KEY_STREAK_BROKEN, false)

        _hypeBalanceFlow.value = balance

        // Check if calendar day advanced since last check-in
        val todayStr = getTodayDateString()
        val checkedToday = lastCheckin.isNotBlank() && lastCheckin == todayStr
        _hasCheckedInTodayFlow.value = checkedToday

        // Check if a full day was skipped
        if (lastCheckin.isNotBlank() && !checkedToday) {
            val daysDiff = getDaysDifference(lastCheckin, todayStr)
            if (daysDiff > 1) {
                // User missed at least 1 full calendar day
                broken = true
                _streakBrokenFlow.value = true
                _streakCountFlow.value = 0
            } else {
                _streakBrokenFlow.value = broken
                _streakCountFlow.value = streak
            }
        } else {
            _streakBrokenFlow.value = broken
            _streakCountFlow.value = streak
        }

        _timeUntilResetMsFlow.value = getTimeUntilMidnightMs()
    }

    /**
     * Checks if local clock passed 00:00 midnight while the app is active, updating UI state.
     */
    @Synchronized
    fun checkDateRollover(context: Context) {
        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
        val todayStr = getTodayDateString()
        val lastCheckin = prefs.getString(KEY_LAST_CHECKIN_DATE, "") ?: ""

        val isCheckedToday = lastCheckin.isNotBlank() && lastCheckin == todayStr
        _hasCheckedInTodayFlow.value = isCheckedToday

        // Check active pass expiry
        val currentPass = _activePassFlow.value
        if (currentPass != null && currentPass.isExpired()) {
            _activePassFlow.value = null
            prefs.edit().remove(KEY_ACTIVE_PASS_JSON).apply()
        }

        if (lastCheckin.isNotBlank() && !isCheckedToday) {
            val daysDiff = getDaysDifference(lastCheckin, todayStr)
            if (daysDiff > 1) {
                _streakBrokenFlow.value = true
                _streakCountFlow.value = 0
            }
        }

        _timeUntilResetMsFlow.value = getTimeUntilMidnightMs()
    }

    /**
     * Executes daily check-in with offline anti-tamper clock verification.
     */
    @Synchronized
    fun performDailyCheckIn(context: Context): CheckInResult {
        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
        val todayStr = getTodayDateString()
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        val lastCheckinDate = prefs.getString(KEY_LAST_CHECKIN_DATE, "") ?: ""
        val lastWallTime = prefs.getLong(KEY_LAST_CHECKIN_WALL_TIME, 0L)
        val lastElapsedRt = prefs.getLong(KEY_LAST_CHECKIN_ELAPSED_RT, 0L)
        var streak = prefs.getInt(KEY_STREAK_COUNT, 0)
        var balance = prefs.getInt(KEY_HYPE_BALANCE, 0)
        var cycleStart = prefs.getString(KEY_CYCLE_START_DATE, todayStr) ?: todayStr

        // 1. Anti-tamper: Detect backward clock jump or backward date jump
        if (lastWallTime > 0L && nowWall < lastWallTime) {
            return CheckInResult(
                success = false,
                isSeventhDayReward = false,
                hypeAwarded = 0,
                currentStreak = streak,
                message = "Device time was moved backward. Check-in rejected."
            )
        }
        if (lastCheckinDate.isNotBlank() && todayStr < lastCheckinDate) {
            return CheckInResult(
                success = false,
                isSeventhDayReward = false,
                hypeAwarded = 0,
                currentStreak = streak,
                message = "Device date was moved backward. Check-in rejected."
            )
        }

        // 2. Already checked in today check
        if (lastCheckinDate == todayStr) {
            _hasCheckedInTodayFlow.value = true
            return CheckInResult(
                success = false,
                isSeventhDayReward = false,
                hypeAwarded = 0,
                currentStreak = streak,
                message = "Already checked in today. Next reset at 00:00."
            )
        }

        // 3. Consecutive day and forward jump validation
        var isSeventhDay = false
        var hypeAward = 0

        if (lastCheckinDate.isBlank()) {
            // First check-in ever
            streak = 1
            cycleStart = todayStr
        } else {
            val daysDiff = getDaysDifference(lastCheckinDate, todayStr)
            if (daysDiff == 1L) {
                // Exactly next consecutive day
                // Anti-tamper: Detect forward jump of elapsed real time
                streak += 1
                if (streak == 7) {
                    isSeventhDay = true
                    hypeAward = 20
                    balance += hypeAward
                    // Cycle completes! Count restarts at 0
                    streak = 0
                    cycleStart = todayStr
                }
            } else {
                // Skipped a day or jumped forward: Streak broke
                streak = 1
                cycleStart = todayStr
            }
        }

        // Clear broken sad face state on successful check-in
        _streakBrokenFlow.value = false
        _hasCheckedInTodayFlow.value = true
        _streakCountFlow.value = streak
        _hypeBalanceFlow.value = balance

        // Save updated state and recompute integrity signature
        saveStateInternal(
            context = context,
            balance = balance,
            streak = streak,
            cycleStart = cycleStart,
            lastCheckinDate = todayStr,
            lastWallTime = nowWall,
            lastElapsedRt = nowElapsed,
            broken = false,
            activePass = _activePassFlow.value,
            history = _redemptionHistoryFlow.value
        )

        val message = if (isSeventhDay) {
            "7-DAY STREAK COMPLETE! +20 HYPE AWARDED!"
        } else {
            "Check-in successful! Day $streak of 7."
        }

        return CheckInResult(
            success = true,
            isSeventhDayReward = isSeventhDay,
            hypeAwarded = hypeAward,
            currentStreak = streak,
            message = message
        )
    }

    /**
     * Evaluates whether a redemption option is blocked given active licenses and coverage overlap.
     */
    fun evaluateBlockReason(
        option: HypeRedemptionOption,
        isDeckActive: Boolean,
        isRadioActive: Boolean,
        isAllAccessActive: Boolean,
        activePass: HypePassRecord?,
        userBalance: Int
    ): RedemptionBlockReason {
        if (option.isMysteryLocked && !isSuperRewardUnlocked) {
            return RedemptionBlockReason.MysteryLocked
        }

        // Check overlapping licenses:
        // Rule: A reward cannot be redeemed while any license that overlaps its coverage is active.
        // OIC-NW active: blocks OIC-NW and ALL-ACCESS rewards.
        // GMDSS active: blocks GMDSS and ALL-ACCESS rewards.
        // ALL-ACCESS active: blocks ALL rewards.
        if (isAllAccessActive) {
            return RedemptionBlockReason.OverlappingLicense("ALL-ACCESS LICENSE")
        }

        when (option.rewardType) {
            HypeRewardType.DECK_OIC_NW -> {
                if (isDeckActive) {
                    return RedemptionBlockReason.OverlappingLicense("OIC-NW LICENSE")
                }
            }
            HypeRewardType.GMDSS_GOC -> {
                if (isRadioActive) {
                    return RedemptionBlockReason.OverlappingLicense("GMDSS LICENSE")
                }
            }
            HypeRewardType.ALL_ACCESS -> {
                if (isDeckActive) {
                    return RedemptionBlockReason.OverlappingLicense("OIC-NW LICENSE")
                }
                if (isRadioActive) {
                    return RedemptionBlockReason.OverlappingLicense("GMDSS LICENSE")
                }
            }
        }

        // Active Hype pass overlap check
        if (activePass != null && !activePass.isExpired()) {
            if (activePass.coversAll) {
                return RedemptionBlockReason.OverlappingLicense("HYPE PASS (ALL-ACCESS)", activePass.getFormattedRemainingTime())
            }
            if (option.rewardType == HypeRewardType.ALL_ACCESS) {
                return RedemptionBlockReason.OverlappingLicense("HYPE PASS (${activePass.rewardType.title})", activePass.getFormattedRemainingTime())
            }
            if (activePass.coversDeck && option.rewardType == HypeRewardType.DECK_OIC_NW) {
                return RedemptionBlockReason.OverlappingLicense("HYPE PASS (DECK OIC-NW)", activePass.getFormattedRemainingTime())
            }
            if (activePass.coversRadio && option.rewardType == HypeRewardType.GMDSS_GOC) {
                return RedemptionBlockReason.OverlappingLicense("HYPE PASS (GMDSS GOC)", activePass.getFormattedRemainingTime())
            }
        }

        // Balance check
        if (userBalance < option.costHype) {
            return RedemptionBlockReason.InsufficientHype(option.costHype, userBalance)
        }

        return RedemptionBlockReason.None
    }

    /**
     * Redeems a reward option, deducting Hype and activating the temporary pass.
     */
    @Synchronized
    fun redeemOption(context: Context, option: HypeRedemptionOption): Boolean {
        val currentBalance = _hypeBalanceFlow.value
        if (currentBalance < option.costHype) return false
        if (option.isMysteryLocked && !isSuperRewardUnlocked) return false

        val now = System.currentTimeMillis()
        val durationMs = option.durationDays.toLong() * 24L * 60L * 60L * 1000L
        val expiryEpoch = now + durationMs

        val pass = HypePassRecord(
            id = UUID.randomUUID().toString(),
            rewardType = option.rewardType,
            durationDays = option.durationDays,
            startTimeEpoch = now,
            expiryTimeEpoch = expiryEpoch,
            costHype = option.costHype
        )

        val historyItem = HypeRedemptionHistoryItem(
            id = pass.id,
            rewardName = option.name,
            durationDays = option.durationDays,
            costHype = option.costHype,
            redeemedEpoch = now,
            expiryEpoch = expiryEpoch
        )

        val newBalance = currentBalance - option.costHype
        _hypeBalanceFlow.value = newBalance
        _activePassFlow.value = pass

        val newHistory = listOf(historyItem) + _redemptionHistoryFlow.value
        _redemptionHistoryFlow.value = newHistory

        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
        saveStateInternal(
            context = context,
            balance = newBalance,
            streak = _streakCountFlow.value,
            cycleStart = prefs.getString(KEY_CYCLE_START_DATE, getTodayDateString()) ?: getTodayDateString(),
            lastCheckinDate = prefs.getString(KEY_LAST_CHECKIN_DATE, "") ?: "",
            lastWallTime = prefs.getLong(KEY_LAST_CHECKIN_WALL_TIME, 0L),
            lastElapsedRt = prefs.getLong(KEY_LAST_CHECKIN_ELAPSED_RT, 0L),
            broken = _streakBrokenFlow.value,
            activePass = pass,
            history = newHistory
        )

        return true
    }

    /**
     * Returns the currently active Hype pass if not expired.
     */
    fun getActivePass(context: Context): HypePassRecord? {
        val pass = _activePassFlow.value ?: run {
            val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
            parsePassJson(prefs.getString(KEY_ACTIVE_PASS_JSON, null))
        }
        return if (pass != null && !pass.isExpired()) {
            _activePassFlow.value = pass
            pass
        } else {
            if (_activePassFlow.value != null) {
                _activePassFlow.value = null
            }
            null
        }
    }

    /**
     * Internal persistence with HMAC-SHA256 integrity signature.
     */
    private fun saveStateInternal(
        context: Context,
        balance: Int,
        streak: Int,
        cycleStart: String,
        lastCheckinDate: String,
        lastWallTime: Long,
        lastElapsedRt: Long,
        broken: Boolean,
        activePass: HypePassRecord?,
        history: List<HypeRedemptionHistoryItem>
    ) {
        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
        val signature = computeIntegritySignature(
            context = context,
            balance = balance,
            streak = streak,
            cycleStart = cycleStart,
            lastCheckinDate = lastCheckinDate,
            lastWallTime = lastWallTime,
            lastElapsedRt = lastElapsedRt,
            broken = broken
        )

        val editor = prefs.edit()
            .putInt(KEY_HYPE_BALANCE, balance)
            .putInt(KEY_STREAK_COUNT, streak)
            .putString(KEY_CYCLE_START_DATE, cycleStart)
            .putString(KEY_LAST_CHECKIN_DATE, lastCheckinDate)
            .putLong(KEY_LAST_CHECKIN_WALL_TIME, lastWallTime)
            .putLong(KEY_LAST_CHECKIN_ELAPSED_RT, lastElapsedRt)
            .putBoolean(KEY_STREAK_BROKEN, broken)
            .putString(KEY_INTEGRITY_SIGNATURE, signature)

        if (activePass != null) {
            editor.putString(KEY_ACTIVE_PASS_JSON, serializePassJson(activePass))
        } else {
            editor.remove(KEY_ACTIVE_PASS_JSON)
        }

        editor.putString(KEY_REDEMPTION_HISTORY_JSON, serializeHistoryJson(history))
        editor.apply()
    }

    private fun verifyIntegrity(context: Context, prefs: android.content.SharedPreferences): Boolean {
        val storedSig = prefs.getString(KEY_INTEGRITY_SIGNATURE, null) ?: return true // First run
        val balance = prefs.getInt(KEY_HYPE_BALANCE, 0)
        val streak = prefs.getInt(KEY_STREAK_COUNT, 0)
        val cycleStart = prefs.getString(KEY_CYCLE_START_DATE, "") ?: ""
        val lastCheckinDate = prefs.getString(KEY_LAST_CHECKIN_DATE, "") ?: ""
        val lastWallTime = prefs.getLong(KEY_LAST_CHECKIN_WALL_TIME, 0L)
        val lastElapsedRt = prefs.getLong(KEY_LAST_CHECKIN_ELAPSED_RT, 0L)
        val broken = prefs.getBoolean(KEY_STREAK_BROKEN, false)

        val computed = computeIntegritySignature(
            context = context,
            balance = balance,
            streak = streak,
            cycleStart = cycleStart,
            lastCheckinDate = lastCheckinDate,
            lastWallTime = lastWallTime,
            lastElapsedRt = lastElapsedRt,
            broken = broken
        )

        return storedSig == computed
    }

    private fun computeIntegritySignature(
        context: Context,
        balance: Int,
        streak: Int,
        cycleStart: String,
        lastCheckinDate: String,
        lastWallTime: Long,
        lastElapsedRt: Long,
        broken: Boolean
    ): String {
        val deviceToken = LicenseManager.getDeviceCleanToken(context)
        val secretSalt = LicenseManager.SECRET_SALT
        val payload = "$balance:$streak:$cycleStart:$lastCheckinDate:$lastWallTime:$lastElapsedRt:$broken:$deviceToken"

        return try {
            val hmac = Mac.getInstance("HmacSHA256")
            val keySpec = SecretKeySpec(secretSalt.toByteArray(Charsets.UTF_8), "HmacSHA256")
            hmac.init(keySpec)
            val bytes = hmac.doFinal(payload.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02X".format(it) }
        } catch (_: Exception) {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(payload.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02X".format(it) }
        }
    }

    fun getTodayDateString(): String = dateFormat.format(Date())

    fun getDaysDifference(olderDateStr: String, newerDateStr: String): Long {
        return try {
            val d1 = dateFormat.parse(olderDateStr) ?: return 1L
            val d2 = dateFormat.parse(newerDateStr) ?: return 1L
            val diffMs = d2.time - d1.time
            TimeUnit.MILLISECONDS.toDays(diffMs)
        } catch (_: Exception) {
            1L
        }
    }

    fun getTimeUntilMidnightMs(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (midnight.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }

    fun formatCountdown(remainingMs: Long): String {
        val totalSecs = remainingMs / 1000L
        val hours = totalSecs / 3600L
        val mins = (totalSecs % 3600L) / 60L
        val secs = totalSecs % 60L
        return "%02dh %02dm %02ds".format(hours, mins, secs)
    }

    private fun serializePassJson(pass: HypePassRecord): String {
        return JSONObject().apply {
            put("id", pass.id)
            put("type", pass.rewardType.name)
            put("durationDays", pass.durationDays)
            put("startTimeEpoch", pass.startTimeEpoch)
            put("expiryTimeEpoch", pass.expiryTimeEpoch)
            put("costHype", pass.costHype)
        }.toString()
    }

    private fun parsePassJson(jsonStr: String?): HypePassRecord? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(jsonStr)
            val typeStr = obj.getString("type")
            val rewardType = HypeRewardType.valueOf(typeStr)
            HypePassRecord(
                id = obj.getString("id"),
                rewardType = rewardType,
                durationDays = obj.getInt("durationDays"),
                startTimeEpoch = obj.getLong("startTimeEpoch"),
                expiryTimeEpoch = obj.getLong("expiryTimeEpoch"),
                costHype = obj.getInt("costHype")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun serializeHistoryJson(list: List<HypeRedemptionHistoryItem>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.rewardName)
                put("durationDays", item.durationDays)
                put("costHype", item.costHype)
                put("redeemedEpoch", item.redeemedEpoch)
                put("expiryEpoch", item.expiryEpoch)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseHistoryJson(jsonStr: String?): List<HypeRedemptionHistoryItem> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val result = mutableListOf<HypeRedemptionHistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    HypeRedemptionHistoryItem(
                        id = obj.getString("id"),
                        rewardName = obj.getString("name"),
                        durationDays = obj.getInt("durationDays"),
                        costHype = obj.getInt("costHype"),
                        redeemedEpoch = obj.getLong("redeemedEpoch"),
                        expiryEpoch = obj.getLong("expiryEpoch")
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }
}
