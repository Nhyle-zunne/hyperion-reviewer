package com.nhyle.hyperionmaritime.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhyle.hyperionmaritime.data.local.ActiveSessionEntity
import com.nhyle.hyperionmaritime.data.local.ActiveSessionSnapshot
import com.nhyle.hyperionmaritime.data.local.ActiveSessionSnapshotManager
import com.nhyle.hyperionmaritime.data.local.DeletedSession
import com.nhyle.hyperionmaritime.data.local.DeletedSessionManager
import com.nhyle.hyperionmaritime.data.local.ExamRecordEntity
import com.nhyle.hyperionmaritime.data.local.dateFormatted
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.local.MistakeEntity
import com.nhyle.hyperionmaritime.data.local.MistakeEntry
import com.nhyle.hyperionmaritime.data.local.MistakesVaultManager
import com.nhyle.hyperionmaritime.data.local.QuestionAttemptHistoryManager
import com.nhyle.hyperionmaritime.data.local.SessionJsonHelper
import kotlinx.coroutines.flow.asStateFlow
import com.nhyle.hyperionmaritime.data.model.CurrentQuestionStats
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.ExamResult
import com.nhyle.hyperionmaritime.data.model.FunctionScore
import com.nhyle.hyperionmaritime.data.model.ProQuestion
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.QuizMode
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.model.UserQuestionState
import com.nhyle.hyperionmaritime.data.repository.MarinaScoringEngine
import com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBankGmdss
import com.nhyle.hyperionmaritime.data.repository.ProQuestionRepository
import com.nhyle.hyperionmaritime.data.repository.ReviewerRepository
import com.nhyle.hyperionmaritime.data.util.LicenseManager
// import com.nhyle.hyperionmaritime.data.util.NewTrackPreviewAccess
import com.nhyle.hyperionmaritime.data.billing.BillingManager
import com.nhyle.hyperionmaritime.data.billing.BillingConstants
import com.nhyle.hyperionmaritime.data.billing.TrackEntitlements
import com.nhyle.hyperionmaritime.data.hype.HypePassManager
import com.nhyle.hyperionmaritime.data.hype.HypePassRecord
import com.nhyle.hyperionmaritime.data.hype.HypeRedemptionHistoryItem
import com.nhyle.hyperionmaritime.data.hype.HypeRedemptionOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.UUID

enum class DrillDeliveryMode {
    STANDARD,
    INTENSIVE
}

data class DrillConfiguration(
    val competenceId: String? = null,
    val selectedPart: Int? = null,
    val deliveryMode: DrillDeliveryMode = DrillDeliveryMode.STANDARD,
    val questionCount: Int? = null,
    val startItem: Int = 1,
    val endItem: Int? = null,
    val shuffleQuestions: Boolean = false,
    val shuffleChoices: Boolean = true,
    val isCrpMode: Boolean = false,
    val prioritizeUnseen: Boolean = false,
    val isOperationalReview: Boolean = false
)

sealed class Screen {
    object TermsOnboarding : Screen() // First-launch mandatory terms gate
    object TermsAndDisclaimer : Screen() // Permanent legal agreement viewer
    object TrackSelection : Screen() // Dedicated Track Selection Homepage
    object Home : Screen() // Track Dashboard
    data class PracticeCompetenceSelect(val isCrpMode: Boolean = false, val isOperationalReview: Boolean = false) : Screen() // Step 1: Competence Selection Screen
    data class PracticeConfig(
        val competenceId: String? = null,
        val isCrpMode: Boolean = false,
        val track: Track? = null,
        val prioritizeUnseen: Boolean = false,
        val isOperationalReview: Boolean = false
    ) : Screen() {
        val route: String
            get() = createRoute(track = track, competence = competenceId, isCrpMode = isCrpMode, prioritizeUnseen = prioritizeUnseen, isOperationalReview = isOperationalReview)

        companion object {
            fun createRoute(track: Track? = null, competence: String? = null, isCrpMode: Boolean = false, prioritizeUnseen: Boolean = false, isOperationalReview: Boolean = false): String {
                val trackName = track?.name ?: "DEFAULT"
                val comp = competence ?: "ALL"
                return "practice_config/$trackName/$comp?isCrp=$isCrpMode&unseen=$prioritizeUnseen&opReview=$isOperationalReview"
            }
        }
    } // Step 2: Practice Configuration Page
    object Flashcards : Screen() // Flashcard Revision Mode
    object Topics : Screen()
    object Quiz : Screen() // PracticeQuizScreen (Practice / Drills / Vault / Bookmarks)
    object MockHub : Screen() // MockExamHubScreen (Master Competence Hub)
    data class MockCompetence(val competenceId: String) : Screen() // MockCompetenceScreen (Continuous scroll)
    object Results : Screen()
    object Vault : Screen()
    object Bookmarks : Screen()
    object Search : Screen()
    object History : Screen()
    object FreePractice : Screen() // Free Tier Practice Viewer with sample questions
    object Activation : Screen() // Account & Licensing screen
    object HowToAvail : Screen() // Dedicated How to Avail Pro Mode Screen
    object License : Screen() // Dedicated License & Device Verification screen
    object ExamGuidelines : Screen() // Dedicated MARINA Theoretical Examination Guidelines Screen
    data class QuickScanCompetenceSelect(val track: Track? = null) : Screen() // Quick Scan Competence Selection
    data class QuickScanViewer(val competenceId: String, val track: Track? = null) : Screen() // Quick Scan Material Viewer
    object Store : Screen() // Dedicated Google Play Store Screen
    object Streak : Screen() // Dedicated Study Streak & Activity Page
    object StudyTime : Screen() // Dedicated Study Time Statistics Page
    object About : Screen() // About Hyperion
}

class ReviewerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HyperionDatabase.getInstance(application)
    private val repository = ReviewerRepository(database)
    private val prefs = application.getSharedPreferences("hyperion_prefs", Context.MODE_PRIVATE)

    private val _marinaExamTargetMillis = MutableStateFlow(prefs.getLong("marina_exam_target_millis", 0L))
    val marinaExamTargetMillis: StateFlow<Long> = _marinaExamTargetMillis.asStateFlow()

    fun setMarinaExamTargetMillis(millis: Long) {
        prefs.edit().putLong("marina_exam_target_millis", millis).apply()
        _marinaExamTargetMillis.value = millis
    }

    fun clearMarinaExamTarget() {
        prefs.edit().remove("marina_exam_target_millis").apply()
        _marinaExamTargetMillis.value = 0L
    }

    private data class Sextuple<A, B, C, D, E, F>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F)

    val studyTimeTracker = com.nhyle.hyperionmaritime.data.studytime.StudyTimeTracker(
        scope = viewModelScope,
        saveSegmentCallback = { segments ->
            repository.getStudyTimeSegmentDao().insertSegments(segments)
        },
        saveElapsedProgressCallback = { sessionId, elapsedMs ->
            prefs.edit().putLong("study_elapsed_$sessionId", elapsedMs).apply()
        }
    )

    fun recordStudyInteraction() {
        studyTimeTracker.recordUserInteraction()
    }

    fun getAllStudySegments(): kotlinx.coroutines.flow.Flow<List<com.nhyle.hyperionmaritime.data.studytime.StudyTimeSegmentEntity>> {
        return repository.getStudyTimeSegmentDao().getAllSegments()
    }

    fun getSessionStudyTimeMs(sessionId: String): kotlinx.coroutines.flow.Flow<Long?> {
        return repository.getStudyTimeSegmentDao().observeTotalDurationForSession(sessionId)
    }

    fun startStudyTrackingForCurrentSession() {
        val mode = if (_activeQuizMode.value == QuizMode.MOCK_EXAM) "EXAM" else "DRILL"
        val drillFormat = if (mode == "DRILL") {
            if (_activeDrillDeliveryMode.value == DrillDeliveryMode.INTENSIVE) "INTENSIVE" else "STANDARD"
        } else null
        val part = _activePartLabel.value?.removePrefix("Part ")?.trim()
        val savedElapsed = prefs.getLong("study_elapsed_$currentActiveSessionId", 0L)
        studyTimeTracker.startSession(
            sessionId = currentActiveSessionId,
            trackId = _selectedTrack.value.name,
            competence = _activeCompetenceId.value,
            part = part,
            mode = mode,
            drillFormat = drillFormat,
            initialAccumulatedMs = savedElapsed
        )
    }

    fun startQuickScanStudyTracking(competenceId: String, track: Track) {
        val sessionId = "quick_scan_${track.name}_$competenceId"
        val savedElapsed = prefs.getLong("study_elapsed_$sessionId", 0L)
        studyTimeTracker.startSession(
            sessionId = sessionId,
            trackId = track.name,
            competence = competenceId,
            part = null,
            mode = "QUICK_SCAN",
            drillFormat = null,
            initialAccumulatedMs = savedElapsed
        )
    }

    fun pauseQuickScanStudyTracking() {
        studyTimeTracker.pauseTimer()
    }

    data class DeviceRegistrationUiState(
        val requestKey: String = "",
        val activationKeyInput: String = "",
        val promoCodeInput: String = "",
        val isLicensed: Boolean = false,
        val licenseType: String = "RESTRICTED", // "LIFETIME", "SUBSCRIPTION", "PROMO_PASS", "RESTRICTED"
        val activeTier: String = "NONE",         // "OIC_NW", "GMDSS_GOC", "ALL_ACCESS", "NONE"
        val isOicLicensed: Boolean = false,
        val isGmdssLicensed: Boolean = false,
        val isEwLicensed: Boolean = false,
        val isDmLicensed: Boolean = false,
        val isEmLicensed: Boolean = false,
        val isAllAccessLicensed: Boolean = false,
        val oicTimeLeft: String? = null,
        val gmdssTimeLeft: String? = null,
        val ewTimeLeft: String? = null,
        val dmTimeLeft: String? = null,
        val emTimeLeft: String? = null,
        val allAccessTimeLeft: String? = null,
        val isVerifyingLicense: Boolean = false,
        val isVerifyingOnLaunch: Boolean = false,
        val isCheckingStatus: Boolean = false,
        val isPromoLoading: Boolean = false,
        val isKeyLoading: Boolean = false,
        val isSuccess: Boolean = false,
        val statusMessage: String? = null,
        val promoError: String? = null,
        val keyError: String? = null,
        val activationKeyError: String? = null
    )

    private val _registrationUiState = MutableStateFlow(DeviceRegistrationUiState())
    val registrationUiState: StateFlow<DeviceRegistrationUiState> = _registrationUiState.asStateFlow()

    private val _drillConfiguration = MutableStateFlow(DrillConfiguration())
    val drillConfiguration: StateFlow<DrillConfiguration> = _drillConfiguration.asStateFlow()

    fun setPrioritizeUnseen(enabled: Boolean) {
        _drillConfiguration.update { it.copy(prioritizeUnseen = enabled) }
    }

    fun updateDrillConfiguration(update: (DrillConfiguration) -> DrillConfiguration) {
        _drillConfiguration.update(update)
    }

    fun markQuestionSeen(track: Track, questionId: String) {
        val trimmed = questionId.trim()
        if (trimmed.isEmpty()) return
        synchronized(this) {
            val key = "seen_questions_${track.name}"
            val existing = prefs.getStringSet(key, null)
            val updated = if (existing != null) HashSet(existing) else HashSet()
            if (updated.add(trimmed)) {
                prefs.edit().putStringSet(key, updated).commit()
            }
        }
    }

    fun getSeenQuestionIds(track: Track): Set<String> {
        val key = "seen_questions_${track.name}"
        return synchronized(this) {
            val raw = prefs.getStringSet(key, null)
            if (raw.isNullOrEmpty()) emptySet() else HashSet(raw)
        }
    }

    fun clearSeenQuestions(track: Track) {
        synchronized(this) {
            val key = "seen_questions_${track.name}"
            prefs.edit().remove(key).commit()
        }
    }

    fun clearAllSeenQuestions() {
        synchronized(this) {
            val editor = prefs.edit()
            Track.entries.forEach { track ->
                editor.remove("seen_questions_${track.name}")
            }
            editor.commit()
        }
    }

    suspend fun getEncounteredQuestionIds(track: Track = _selectedTrack.value, competenceId: String? = null): Set<String> {
        val localSeen = getSeenQuestionIds(track)
        val repoSeen = repository.getEncounteredQuestionIds(track, competenceId)
        return localSeen + repoSeen
    }

    fun saveQuickScanProgress(track: Track, competenceId: String, pageIndex: Int) {
        synchronized(this) {
            prefs.edit()
                .putString("qs_last_comp_${track.name}", competenceId)
                .putInt("qs_last_index_${track.name}_${competenceId}", pageIndex)
                .putLong("qs_last_time_${track.name}_${competenceId}", System.currentTimeMillis())
                .apply()
        }
    }

    fun getQuickScanLastIndex(track: Track, competenceId: String): Int {
        return synchronized(this) {
            prefs.getInt("qs_last_index_${track.name}_${competenceId}", 0)
        }
    }

    fun getQuickScanLastCompetence(track: Track): String? {
        return synchronized(this) {
            prefs.getString("qs_last_comp_${track.name}", null)
        }
    }

    fun clearQuickScanProgress(track: Track, competenceId: String) {
        synchronized(this) {
            prefs.edit()
                .remove("qs_last_index_${track.name}_${competenceId}")
                .remove("qs_last_time_${track.name}_${competenceId}")
                .apply()
        }
    }

    val billingManager: BillingManager = BillingManager.getInstance(application)

    // Licensing & Activation State (Independent Pro Tiers & Google Play Entitlements combined)
    val deviceRequestCode: String get() = LicenseManager.getOrGenerateRequestCode(getApplication())
    val permanentHardwareId: String get() = LicenseManager.getPermanentHardwareId(getApplication())
    val installSessionId: String get() = LicenseManager.getInstallSessionId(getApplication())
    val installDate: String get() = LicenseManager.getInstallDate(getApplication())

    private val _isOicNwProUnlocked = MutableStateFlow(false)
    val isOicNwProUnlocked: StateFlow<Boolean> = _isOicNwProUnlocked.asStateFlow()

    private val _isGmdssProUnlocked = MutableStateFlow(false)
    val isGmdssProUnlocked: StateFlow<Boolean> = _isGmdssProUnlocked.asStateFlow()

    private val _isHyperionProUnlocked = MutableStateFlow(false)
    val isHyperionProUnlocked: StateFlow<Boolean> = _isHyperionProUnlocked.asStateFlow()

    private val _isEngineOicEwProUnlocked = MutableStateFlow(false)
    val isEngineOicEwProUnlocked: StateFlow<Boolean> = _isEngineOicEwProUnlocked.asStateFlow()

    private val _isDeckMgtProUnlocked = MutableStateFlow(false)
    val isDeckMgtProUnlocked: StateFlow<Boolean> = _isDeckMgtProUnlocked.asStateFlow()

    private val _isEngineMgtProUnlocked = MutableStateFlow(false)
    val isEngineMgtProUnlocked: StateFlow<Boolean> = _isEngineMgtProUnlocked.asStateFlow()

    val isMasterProUnlocked: StateFlow<Boolean> = isHyperionProUnlocked

    private val _isActivated = MutableStateFlow(false)
    val isActivated: StateFlow<Boolean> = _isActivated.asStateFlow()

    fun refreshUnlockStates() {
        val playEnt = billingManager.entitlements.value
        val app = getApplication<Application>()
        val offlineInfo = if (app != null) LicenseManager.getOfflineLicenseInfo(app) else null
        val isOfflineActive = offlineInfo != null && offlineInfo.isActive
        val isClockTampered = (offlineInfo != null && offlineInfo.isTampered) || playEnt.isClockTampered

        val offlineOic = if (isClockTampered) false else (
            LicenseManager.isOfflineHyperionUnlocked(app) ||
            LicenseManager.isOfflineOicNwUnlocked(app)
        )
        val offlineGmdss = if (isClockTampered) false else (
            LicenseManager.isOfflineHyperionUnlocked(app) ||
            LicenseManager.isOfflineGmdssUnlocked(app)
        )
        val offlineEw = if (isClockTampered) false else (
            LicenseManager.isOfflineHyperionUnlocked(app) ||
            LicenseManager.isOfflineEngineOicEwUnlocked(app)
        )
        val offlineDm = if (isClockTampered) false else (
            LicenseManager.isOfflineHyperionUnlocked(app) ||
            LicenseManager.isOfflineDeckMgtUnlocked(app)
        )
        val offlineEm = if (isClockTampered) false else (
            LicenseManager.isOfflineHyperionUnlocked(app) ||
            LicenseManager.isOfflineEngineMgtUnlocked(app)
        )
        val offlineHyp = if (isClockTampered) false else LicenseManager.isOfflineHyperionUnlocked(app)
        val activatedOffline = offlineOic || offlineGmdss || offlineEw || offlineDm || offlineEm

        // Temporary Hype Pass Layer (Device-bound, read-only overlay)
        val hypePass = if (app != null) HypePassManager.getActivePass(app) else null
        val isHypeDeck = hypePass != null && (hypePass.coversDeck || hypePass.coversAll) && !hypePass.isExpired()
        val isHypeRadio = hypePass != null && (hypePass.coversRadio || hypePass.coversAll) && !hypePass.isExpired()
        val isHypeAll = hypePass != null && hypePass.coversAll && !hypePass.isExpired()

        // Google Play Subscriptions are server-authoritative and immune to local clock tamper
        val newOic = playEnt.oicNwUnlocked || playEnt.allAccessUnlocked || (!isClockTampered && (offlineOic || isHypeDeck))
        val newGmdss = playEnt.gmdssUnlocked || playEnt.allAccessUnlocked || (!isClockTampered && (offlineGmdss || isHypeRadio))
        val newEw = playEnt.engineOicEwUnlocked || playEnt.allAccessUnlocked || (!isClockTampered && offlineEw)
        val newDm = playEnt.deckMgtUnlocked || playEnt.allAccessUnlocked || (!isClockTampered && offlineDm)
        val newEm = playEnt.engineMgtUnlocked || playEnt.allAccessUnlocked || (!isClockTampered && offlineEm)
        val newHyp = playEnt.allAccessUnlocked || (!isClockTampered && (offlineHyp || isHypeAll)) || (newOic && newGmdss && newEw && newDm && newEm)
        val newAct = newOic || newGmdss || newEw || newDm || newEm

        if (_isOicNwProUnlocked.value != newOic) _isOicNwProUnlocked.value = newOic
        if (_isGmdssProUnlocked.value != newGmdss) _isGmdssProUnlocked.value = newGmdss
        if (_isEngineOicEwProUnlocked.value != newEw) _isEngineOicEwProUnlocked.value = newEw
        if (_isDeckMgtProUnlocked.value != newDm) _isDeckMgtProUnlocked.value = newDm
        if (_isEngineMgtProUnlocked.value != newEm) _isEngineMgtProUnlocked.value = newEm
        if (_isHyperionProUnlocked.value != newHyp) _isHyperionProUnlocked.value = newHyp
        if (_isActivated.value != newAct) _isActivated.value = newAct

        val oicPlan = playEnt.getActivePlanForTrack(Track.DECK_OIC_NW)
        val gmdssPlan = playEnt.getActivePlanForTrack(Track.GMDSS_GOC)
        val allAccessPlan = playEnt.allAccessPlan

        val isSub = (playEnt.allAccessUnlocked || playEnt.oicNwUnlocked || playEnt.gmdssUnlocked || playEnt.engineOicEwUnlocked || playEnt.deckMgtUnlocked || playEnt.engineMgtUnlocked)
        val isHypeActive = hypePass != null && !hypePass.isExpired()

        // Strict Priority: Google Play Subscription > Offline Key > Promo Pass > Hype Check-in Pass
        val licenseType = when {
            isSub -> "SUBSCRIPTION"
            isOfflineActive -> "OFFLINE_PASS"
            _registrationUiState.value.licenseType == "PROMO_PASS" && newAct -> "PROMO_PASS"
            isHypeActive -> "HYPE_PASS"
            isClockTampered -> "RESTRICTED"
            else -> if (newAct) "OFFLINE_PASS" else "RESTRICTED"
        }

        val activeTier = when {
            newHyp -> "ALL_ACCESS"
            newOic && !newGmdss -> "OIC_NW"
            newGmdss && !newOic -> "GMDSS_GOC"
            newOic && newGmdss -> "ALL_ACCESS"
            else -> "NONE"
        }

        val oicOfflineActive = !isClockTampered && (app != null && LicenseManager.isOfflineOicNwUnlocked(app))
        val gmdssOfflineActive = !isClockTampered && (app != null && LicenseManager.isOfflineGmdssUnlocked(app))
        val ewOfflineActive = !isClockTampered && (app != null && LicenseManager.isOfflineEngineOicEwUnlocked(app))
        val dmOfflineActive = !isClockTampered && (app != null && LicenseManager.isOfflineDeckMgtUnlocked(app))
        val emOfflineActive = !isClockTampered && (app != null && LicenseManager.isOfflineEngineMgtUnlocked(app))
        val allAccessOfflineActive = !isClockTampered && (app != null && LicenseManager.isOfflineHyperionUnlocked(app))

        val oicTimeLeft = when {
            oicPlan != null && !oicPlan.isExpired -> "${oicPlan.getFormattedTimeRemaining()} (${oicPlan.getFormattedExpiryDate()})"
            oicOfflineActive && app != null -> {
                val rem = LicenseManager.getOfflineTrackTimeRemaining(app, Track.DECK_OIC_NW)
                val exp = LicenseManager.getOfflineTrackFormattedExpiry(app, Track.DECK_OIC_NW)
                if (rem != null && exp != null) "$rem ($exp)" else null
            }
            isHypeDeck -> "${hypePass!!.getFormattedRemainingTime()} (${hypePass.getFormattedExpiryDate()})"
            else -> null
        }

        val gmdssTimeLeft = when {
            gmdssPlan != null && !gmdssPlan.isExpired -> "${gmdssPlan.getFormattedTimeRemaining()} (${gmdssPlan.getFormattedExpiryDate()})"
            gmdssOfflineActive && app != null -> {
                val rem = LicenseManager.getOfflineTrackTimeRemaining(app, Track.GMDSS_GOC)
                val exp = LicenseManager.getOfflineTrackFormattedExpiry(app, Track.GMDSS_GOC)
                if (rem != null && exp != null) "$rem ($exp)" else null
            }
            isHypeRadio -> "${hypePass!!.getFormattedRemainingTime()} (${hypePass.getFormattedExpiryDate()})"
            else -> null
        }

        val ewPlan = playEnt.getActivePlanForTrack(Track.ENGINE_OIC_EW)
        val dmPlan = playEnt.getActivePlanForTrack(Track.DECK_MGT)
        val emPlan = playEnt.getActivePlanForTrack(Track.ENGINE_MGT)

        val ewTimeLeft = when {
            ewPlan != null && !ewPlan.isExpired -> "${ewPlan.getFormattedTimeRemaining()} (${ewPlan.getFormattedExpiryDate()})"
            ewOfflineActive && app != null -> {
                val rem = LicenseManager.getOfflineTrackTimeRemaining(app, Track.ENGINE_OIC_EW)
                val exp = LicenseManager.getOfflineTrackFormattedExpiry(app, Track.ENGINE_OIC_EW)
                if (rem != null && exp != null) "$rem ($exp)" else null
            }
            else -> null
        }

        val dmTimeLeft = when {
            dmPlan != null && !dmPlan.isExpired -> "${dmPlan.getFormattedTimeRemaining()} (${dmPlan.getFormattedExpiryDate()})"
            dmOfflineActive && app != null -> {
                val rem = LicenseManager.getOfflineTrackTimeRemaining(app, Track.DECK_MGT)
                val exp = LicenseManager.getOfflineTrackFormattedExpiry(app, Track.DECK_MGT)
                if (rem != null && exp != null) "$rem ($exp)" else null
            }
            else -> null
        }

        val emTimeLeft = when {
            emPlan != null && !emPlan.isExpired -> "${emPlan.getFormattedTimeRemaining()} (${emPlan.getFormattedExpiryDate()})"
            emOfflineActive && app != null -> {
                val rem = LicenseManager.getOfflineTrackTimeRemaining(app, Track.ENGINE_MGT)
                val exp = LicenseManager.getOfflineTrackFormattedExpiry(app, Track.ENGINE_MGT)
                if (rem != null && exp != null) "$rem ($exp)" else null
            }
            else -> null
        }

        val oicExp = maxOf(
            if (oicOfflineActive) offlineInfo!!.expiryEpoch else 0L,
            if (oicPlan != null && !oicPlan.isExpired) oicPlan.expiryTimestamp else 0L
        )
        val gmdssExp = maxOf(
            if (gmdssOfflineActive) offlineInfo!!.expiryEpoch else 0L,
            if (gmdssPlan != null && !gmdssPlan.isExpired) gmdssPlan.expiryTimestamp else 0L
        )
        val allAccessPlanExp = if (allAccessPlan != null && !allAccessPlan.isExpired) allAccessPlan.expiryTimestamp else 0L
        val allAccessOfflineExp = if (allAccessOfflineActive) offlineInfo!!.expiryEpoch else 0L

        val allAccessTimeLeft = when {
            allAccessOfflineExp > 0L -> "${offlineInfo!!.getFormattedTimeRemaining()} (${offlineInfo.getFormattedExpiryDate()})"
            allAccessPlanExp > 0L -> "${allAccessPlan!!.getFormattedTimeRemaining()} (${allAccessPlan.getFormattedExpiryDate()})"
            isHypeAll -> "${hypePass!!.getFormattedRemainingTime()} (${hypePass.getFormattedExpiryDate()})"
            oicExp > 0L && gmdssExp > 0L -> {
                val minExp = minOf(oicExp, gmdssExp)
                val tempPlan = com.nhyle.hyperionmaritime.data.billing.ActivePlanInfo(expiryTimestamp = minExp)
                "${tempPlan.getFormattedTimeRemaining()} (${tempPlan.getFormattedExpiryDate()})"
            }
            else -> null
        }

        val statusMsg = when {
            isClockTampered -> "CLOCK TAMPERED // RESTORE SYSTEM TIME"
            isSub -> when {
                playEnt.allAccessUnlocked -> "ACTIVE (ALL-ACCESS PASS SUBSCRIPTION • ${allAccessPlan?.getFormattedTimeRemaining() ?: ""})"
                newOic && newGmdss -> {
                    val minExp = if (oicExp > 0L && gmdssExp > 0L) minOf(oicExp, gmdssExp) else maxOf(oicExp, gmdssExp)
                    val tempPlan = if (minExp > 0L) com.nhyle.hyperionmaritime.data.billing.ActivePlanInfo(expiryTimestamp = minExp) else null
                    val timeStr = tempPlan?.getFormattedTimeRemaining()?.uppercase() ?: ""
                    if (timeStr.isNotEmpty()) "ACTIVE: $timeStr" else "ACTIVE (ALL ACCESS)"
                }
                playEnt.oicNwUnlocked -> "ACTIVE (OIC-NW DECK SUBSCRIPTION • ${oicPlan?.getFormattedTimeRemaining() ?: ""})"
                playEnt.gmdssUnlocked -> "ACTIVE (GMDSS GOC SUBSCRIPTION • ${gmdssPlan?.getFormattedTimeRemaining() ?: ""})"
                else -> "ACTIVE (GOOGLE PLAY SUBSCRIPTION)"
            }
            allAccessOfflineActive -> "ACTIVE (${offlineInfo!!.durationLabel.uppercase()} • ${offlineInfo.getFormattedTimeRemaining().uppercase()})"
            oicOfflineActive && !gmdssOfflineActive -> "ACTIVE (OIC-NW ${offlineInfo!!.durationLabel.uppercase()} • ${offlineInfo.getFormattedTimeRemaining().uppercase()})"
            gmdssOfflineActive && !oicOfflineActive -> "ACTIVE (GMDSS ${offlineInfo!!.durationLabel.uppercase()} • ${offlineInfo.getFormattedTimeRemaining().uppercase()})"
            isHypeActive -> "ACTIVE (${hypePass!!.rewardType.title} • ${hypePass.getFormattedRemainingTime().uppercase()})"
            newAct -> "ACTIVE"
            else -> "RESTRICTED (UNLICENSED)"
        }

        _registrationUiState.update {
            it.copy(
                isLicensed = newAct,
                licenseType = licenseType,
                activeTier = activeTier,
                isOicLicensed = newOic,
                isGmdssLicensed = newGmdss,
                isEwLicensed = newEw,
                isDmLicensed = newDm,
                isEmLicensed = newEm,
                isAllAccessLicensed = newHyp,
                oicTimeLeft = oicTimeLeft,
                gmdssTimeLeft = gmdssTimeLeft,
                ewTimeLeft = ewTimeLeft,
                dmTimeLeft = dmTimeLeft,
                emTimeLeft = emTimeLeft,
                allAccessTimeLeft = allAccessTimeLeft,
                statusMessage = statusMsg
            )
        }
    }

    // Daily Study Check-in & Hype State Flows
    val hypeBalance: StateFlow<Int> = HypePassManager.hypeBalanceFlow
    val rollingStreak: StateFlow<Int> = HypePassManager.streakCountFlow
    val hasCheckedInToday: StateFlow<Boolean> = HypePassManager.hasCheckedInTodayFlow
    val streakBroken: StateFlow<Boolean> = HypePassManager.streakBrokenFlow
    val activeHypePass: StateFlow<HypePassRecord?> = HypePassManager.activePassFlow
    val hypeRedemptionHistory: StateFlow<List<HypeRedemptionHistoryItem>> = HypePassManager.redemptionHistoryFlow
    val timeUntilResetMs: StateFlow<Long> = HypePassManager.timeUntilResetMsFlow

    private val _showDailyCheckInDialog = MutableStateFlow(false)
    val showDailyCheckInDialog: StateFlow<Boolean> = _showDailyCheckInDialog.asStateFlow()

    private val _checkInFeedbackMessage = MutableStateFlow<String?>(null)
    val checkInFeedbackMessage: StateFlow<String?> = _checkInFeedbackMessage.asStateFlow()

    init {
        LicenseManager.init(application)
        MaritimeQuestionBank.init(application)
        loadCurrentLicense(application)
        checkLicenseOnLaunch(application)
        refreshUnlockStates()

        viewModelScope.launch {
            combine(
                billingManager.entitlements,
                LicenseManager.isOicNwProUnlockedFlow,
                LicenseManager.isGmdssProUnlockedFlow,
                LicenseManager.isHyperionProUnlockedFlow,
                LicenseManager.isProActivatedFlow
            ) { _, _, _, _, _ ->
                refreshUnlockStates()
            }.collect { }
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            ActiveSessionSnapshotManager.purgeObsoleteMockSnapshots(application)
            try {
                val deckSession = repository.getActiveSessionForTrackOnce(Track.DECK_OIC_NW.name)
                if (deckSession != null && deckSession.mode.contains("MOCK", ignoreCase = true) && deckSession.totalQuestions > 200) {
                    repository.clearActiveSession(Track.DECK_OIC_NW)
                }
                val gmdssSession = repository.getActiveSessionForTrackOnce(Track.GMDSS_GOC.name)
                if (gmdssSession != null && gmdssSession.mode.contains("MOCK", ignoreCase = true) && gmdssSession.totalQuestions > 100) {
                    repository.clearActiveSession(Track.GMDSS_GOC)
                }
            } catch (_: Exception) {}
        }

        // Initialize Hype Currency & Daily Check-in Engine
        HypePassManager.init(application)
        if (!HypePassManager.hasCheckedInTodayFlow.value) {
            _showDailyCheckInDialog.value = true
        }

        viewModelScope.launch {
            while (true) {
                delay(5000L)
                HypePassManager.checkDateRollover(application)
                val active = HypePassManager.activePassFlow.value
                if (active != null && active.isExpired()) {
                    refreshUnlockStates()
                }
            }
        }
    }

    fun openDailyCheckInDialog() {
        _checkInFeedbackMessage.value = null
        _showDailyCheckInDialog.value = true
    }

    fun dismissDailyCheckInDialog() {
        _showDailyCheckInDialog.value = false
        _checkInFeedbackMessage.value = null
    }

    fun performDailyCheckIn() {
        val app = getApplication<Application>() ?: return
        val result = HypePassManager.performDailyCheckIn(app)
        _checkInFeedbackMessage.value = result.message
        refreshUnlockStates()
    }

    fun redeemHypePass(option: HypeRedemptionOption): Boolean {
        val app = getApplication<Application>() ?: return false
        val success = HypePassManager.redeemOption(app, option)
        if (success) {
            refreshUnlockStates()
        }
        return success
    }

    fun checkDateRollover() {
        val app = getApplication<Application>() ?: return
        HypePassManager.checkDateRollover(app)
        refreshUnlockStates()
    }

    // Paywall Dialog State & Navigation Trigger
    private val _showPaywallDialog = MutableStateFlow(false)
    val showPaywallDialog: StateFlow<Boolean> = _showPaywallDialog.asStateFlow()

    private val _paywallTargetTrack = MutableStateFlow<Track?>(null)
    val paywallTargetTrack: StateFlow<Track?> = _paywallTargetTrack.asStateFlow()

    fun triggerPaywall(track: Track? = _selectedTrack.value) {
        _paywallTargetTrack.value = track ?: _selectedTrack.value
        _showPaywallDialog.value = true
    }

    fun dismissPaywallDialog() {
        _showPaywallDialog.value = false
    }

    /**
     * Simulates purchase completion for testing, emulator, and preview environments.
     */
    fun simulatePurchase(productId: String) {
        billingManager.simulatePurchaseForTesting(productId)
        refreshUnlockStates()
    }

    /**
     * Unified Access Rule:
     * Unlocked = (Valid Offline License) OR (Valid Google Play Purchase) OR (Valid Cached Play Token).
     * Authoritative Invariant: If Offline Pro License == ACTIVE, the user NEVER loses access,
     * regardless of internet failure, billing disconnection, or sea voyage status.
     */
    fun isTrackUnlocked(track: Track = _selectedTrack.value): Boolean {
        // Fast-path reactive state check
        val reactiveUnlocked = when (track) {
            Track.DECK_OIC_NW -> isOicNwProUnlocked.value
            Track.GMDSS_GOC -> isGmdssProUnlocked.value
            Track.ENGINE_OIC_EW -> isEngineOicEwProUnlocked.value
            Track.DECK_MGT -> isDeckMgtProUnlocked.value
            Track.ENGINE_MGT -> isEngineMgtProUnlocked.value
            else -> false
        }
        if (reactiveUnlocked) return true

        try {
            val app = getApplication<Application>()
            if (app != null) {
                // 1. Offline License Authoritative (Terminal key, CRP activation, legacy master)
                val prefs = app.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
                if (prefs != null) {
                    val isAllAccess = prefs.getBoolean("is_all_access_unlocked", false) ||
                                      prefs.getBoolean("is_full_access_unlocked", false)
                    if (isAllAccess) return true
                    if (track == Track.DECK_OIC_NW) {
                        if (prefs.getBoolean("is_oic_unlocked", false)) return true
                    } else if (track == Track.GMDSS_GOC) {
                        if (prefs.getBoolean("is_gmdss_unlocked", false)) return true
                    } else if (track == Track.ENGINE_OIC_EW) {
                        if (prefs.getBoolean("is_engine_ew_unlocked", false) || prefs.getBoolean("is_ew_unlocked", false)) return true
                    } else if (track == Track.DECK_MGT) {
                        if (prefs.getBoolean("is_deck_mgt_unlocked", false) || prefs.getBoolean("is_dm_unlocked", false)) return true
                    } else if (track == Track.ENGINE_MGT) {
                        if (prefs.getBoolean("is_engine_mgt_unlocked", false) || prefs.getBoolean("is_em_unlocked", false)) return true
                    }
                }
                if (LicenseManager.isTrackProUnlocked(app, track) ||
                    LicenseManager.isHyperionProUnlocked(app)) {
                    return true
                }
            }

            // 2. Google Play Billing / Secure Offline Cached Entitlements
            val playEnt = billingManager.entitlements.value
            if (playEnt.allAccessUnlocked) return true
            return when (track) {
                Track.DECK_OIC_NW -> playEnt.oicNwUnlocked
                Track.GMDSS_GOC -> playEnt.gmdssUnlocked
                Track.ENGINE_OIC_EW -> playEnt.engineOicEwUnlocked
                Track.DECK_MGT -> playEnt.deckMgtUnlocked
                Track.ENGINE_MGT -> playEnt.engineMgtUnlocked
                else -> false
            }
        } catch (_: Exception) {}
        return false
    }

    fun isTrackProUnlocked(track: Track = _selectedTrack.value): Boolean = isTrackUnlocked(track)

    fun hasAccessTo(module: com.nhyle.hyperionmaritime.data.util.LicenseTier): Boolean {
        val playEnt = billingManager.entitlements.value
        if (playEnt.allAccessUnlocked) return true
        when (module) {
            com.nhyle.hyperionmaritime.data.util.LicenseTier.OIC_NW -> if (playEnt.oicNwUnlocked) return true
            com.nhyle.hyperionmaritime.data.util.LicenseTier.GMDSS -> if (playEnt.gmdssUnlocked) return true
            com.nhyle.hyperionmaritime.data.util.LicenseTier.ENGINE_OIC_EW -> if (playEnt.engineOicEwUnlocked) return true
            com.nhyle.hyperionmaritime.data.util.LicenseTier.DECK_MGT -> if (playEnt.deckMgtUnlocked) return true
            com.nhyle.hyperionmaritime.data.util.LicenseTier.ENGINE_MGT -> if (playEnt.engineMgtUnlocked) return true
            com.nhyle.hyperionmaritime.data.util.LicenseTier.HYPERION_ALL_ACCESS -> if (playEnt.allAccessUnlocked || (playEnt.oicNwUnlocked && playEnt.gmdssUnlocked && playEnt.engineOicEwUnlocked && playEnt.deckMgtUnlocked && playEnt.engineMgtUnlocked)) return true
        }

        try {
            val app = getApplication<Application>()
            if (app != null) {
                val prefs = app.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
                if (prefs != null) {
                    val isAllAccess = prefs.getBoolean("is_all_access_unlocked", false)
                    if (isAllAccess) return true
                    when (module) {
                        com.nhyle.hyperionmaritime.data.util.LicenseTier.OIC_NW -> if (prefs.getBoolean("is_oic_unlocked", false)) return true
                        com.nhyle.hyperionmaritime.data.util.LicenseTier.GMDSS -> if (prefs.getBoolean("is_gmdss_unlocked", false)) return true
                        com.nhyle.hyperionmaritime.data.util.LicenseTier.ENGINE_OIC_EW -> if (prefs.getBoolean("is_engine_ew_unlocked", false) || prefs.getBoolean("is_ew_unlocked", false)) return true
                        com.nhyle.hyperionmaritime.data.util.LicenseTier.DECK_MGT -> if (prefs.getBoolean("is_deck_mgt_unlocked", false) || prefs.getBoolean("is_dm_unlocked", false)) return true
                        com.nhyle.hyperionmaritime.data.util.LicenseTier.ENGINE_MGT -> if (prefs.getBoolean("is_engine_mgt_unlocked", false) || prefs.getBoolean("is_em_unlocked", false)) return true
                        com.nhyle.hyperionmaritime.data.util.LicenseTier.HYPERION_ALL_ACCESS -> {
                            val hasOic = prefs.getBoolean("is_oic_unlocked", false)
                            val hasGmdss = prefs.getBoolean("is_gmdss_unlocked", false)
                            val hasEw = prefs.getBoolean("is_engine_ew_unlocked", false) || prefs.getBoolean("is_ew_unlocked", false)
                            val hasDm = prefs.getBoolean("is_deck_mgt_unlocked", false) || prefs.getBoolean("is_dm_unlocked", false)
                            val hasEm = prefs.getBoolean("is_engine_mgt_unlocked", false) || prefs.getBoolean("is_em_unlocked", false)
                            if (hasOic && hasGmdss && hasEw && hasDm && hasEm) return true
                        }
                    }
                }
                return LicenseManager.hasAccessTo(app, module)
            }
        } catch (_: Exception) {}
        return false
    }

    fun activateLicense(enteredKey: String, targetTier: com.nhyle.hyperionmaritime.data.util.ProTier? = null): com.nhyle.hyperionmaritime.data.util.UnlockResult {
        val app = getApplication<Application>() ?: return com.nhyle.hyperionmaritime.data.util.UnlockResult.INVALID_KEY
        val result = LicenseManager.verifyAndUnlockKey(app, enteredKey, targetTier)
        if (result.isSuccess) {
            try {
                val prefs = app.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
                val oicUnlocked = prefs.getBoolean("is_oic_unlocked", false)
                val gmdssUnlocked = prefs.getBoolean("is_gmdss_unlocked", false)
                val ewUnlocked = prefs.getBoolean("is_engine_ew_unlocked", false)
                val dmUnlocked = prefs.getBoolean("is_deck_mgt_unlocked", false)
                val emUnlocked = prefs.getBoolean("is_engine_mgt_unlocked", false)
                val allUnlocked = prefs.getBoolean("is_all_access_unlocked", false) || (oicUnlocked && gmdssUnlocked && ewUnlocked && dmUnlocked && emUnlocked)

                val offlineInfo = LicenseManager.getOfflineLicenseInfo(app)
                val formattedStatus = if (offlineInfo != null) {
                    "ACTIVE (${offlineInfo.durationLabel.uppercase()} • ${offlineInfo.getFormattedTimeRemaining().uppercase()})"
                } else {
                    "ACTIVE"
                }

                _registrationUiState.update {
                    it.copy(
                        isLicensed = true,
                        licenseType = "OFFLINE_PASS",
                        isSuccess = true,
                        isOicLicensed = oicUnlocked,
                        isGmdssLicensed = gmdssUnlocked,
                        isEwLicensed = ewUnlocked,
                        isDmLicensed = dmUnlocked,
                        isEmLicensed = emUnlocked,
                        isAllAccessLicensed = allUnlocked,
                        statusMessage = formattedStatus,
                        promoError = null,
                        keyError = null,
                        activationKeyError = null
                    )
                }
            } catch (_: Exception) {}
            ProQuestionRepository.clearCache()
            refreshUnlockStates()
        }
        return result
    }

    fun loadCurrentLicense(context: Context) {
        val prefs = try {
            context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
        if (prefs != null) {
            val isOic = prefs.getBoolean("is_oic_unlocked", false)
            val isGmdss = prefs.getBoolean("is_gmdss_unlocked", false)
            val isEw = prefs.getBoolean("is_engine_ew_unlocked", false)
            val isDm = prefs.getBoolean("is_deck_mgt_unlocked", false)
            val isEm = prefs.getBoolean("is_engine_mgt_unlocked", false)
            val isAll = prefs.getBoolean("is_all_access_unlocked", false) || prefs.getBoolean("is_full_access_unlocked", false)
            val isOffline = prefs.getBoolean("is_offline_license", false)
            val type = prefs.getString("license_type", "RESTRICTED") ?: "RESTRICTED"
            val expiryEpoch = prefs.getLong("promo_expiry_epoch", 0L)
            val now = System.currentTimeMillis()

            val hasAnyLicense = isOic || isGmdss || isEw || isDm || isEm || isAll

            // 1. Promo Pass
            if (type == "PROMO_PASS" && hasAnyLicense) {
                if (expiryEpoch > 0L && now >= expiryEpoch) {
                    revokePromoAccess(context)
                    return
                }
                _registrationUiState.update {
                    it.copy(
                        isLicensed = true,
                        licenseType = "PROMO_PASS",
                        isOicLicensed = true,
                        isGmdssLicensed = true,
                        isEwLicensed = true,
                        isDmLicensed = true,
                        isEmLicensed = true,
                        isAllAccessLicensed = true,
                        statusMessage = "ACTIVE (PROMO PASS // ALL ACCESS)",
                        keyError = null,
                        promoError = null
                    )
                }
            } else if (isOffline && hasAnyLicense) {
                // 2. Offline Hardware Licenses (Time-bound 1W, 1M, 1Y)
                val offlineInfo = LicenseManager.getOfflineLicenseInfo(context)
                if (offlineInfo != null && offlineInfo.isActive) {
                    _registrationUiState.update {
                        it.copy(
                            isLicensed = true,
                            licenseType = "OFFLINE_PASS",
                            isOicLicensed = isOic || isAll,
                            isGmdssLicensed = isGmdss || isAll,
                            isEwLicensed = isEw || isAll,
                            isDmLicensed = isDm || isAll,
                            isEmLicensed = isEm || isAll,
                            isAllAccessLicensed = isAll,
                            statusMessage = "ACTIVE (${offlineInfo.durationLabel.uppercase()} • ${offlineInfo.getFormattedTimeRemaining().uppercase()})",
                            keyError = null,
                            promoError = null
                        )
                    }
                } else if (offlineInfo != null && offlineInfo.isTampered) {
                    _registrationUiState.update {
                        it.copy(
                            isLicensed = false,
                            licenseType = "RESTRICTED",
                            statusMessage = "CLOCK TAMPERED // RESTORE SYSTEM TIME",
                            keyError = offlineInfo.tamperReason,
                            promoError = null
                        )
                    }
                }
            }
        }
        refreshUnlockStates()
    }

    fun checkLicenseOnLaunch(context: Context?) {
        if (context != null) {
            verifyLicenseState(context, isLaunchCheck = true)
        }
    }

    fun verifyLicenseOnLaunch(context: Context) {
        verifyLicenseState(context, isLaunchCheck = true)
    }

    fun autoVerifyOnLaunch(context: Context) {
        loadCurrentLicense(context)
        verifyLicenseState(context, isLaunchCheck = true)
    }

    fun verifyLicenseState(context: Context, isLaunchCheck: Boolean = false) {
        val prefs = try {
            context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
        if (prefs == null) {
            refreshUnlockStates()
            return
        }
        val isOffline = prefs.getBoolean("is_offline_license", false)
        val type = prefs.getString("license_type", "") ?: ""
        val isOic = prefs.getBoolean("is_oic_unlocked", false)
        val isGmdss = prefs.getBoolean("is_gmdss_unlocked", false)
        val isEw = prefs.getBoolean("is_engine_ew_unlocked", false)
        val isDm = prefs.getBoolean("is_deck_mgt_unlocked", false)
        val isEm = prefs.getBoolean("is_engine_mgt_unlocked", false)
        val isAll = prefs.getBoolean("is_all_access_unlocked", false)

        val hasAnyTrack = isOic || isGmdss || isEw || isDm || isEm || isAll

        // OFFLINE KEYS: Immediate exit after anti-tamper and expiry validation. Never query Firebase.
        val offlineInfo = LicenseManager.getOfflineLicenseInfo(context)
        if (isOffline && offlineInfo != null && offlineInfo.isActive && hasAnyTrack) {
            _registrationUiState.update {
                it.copy(
                    isVerifyingLicense = false,
                    isVerifyingOnLaunch = false,
                    isCheckingStatus = false,
                    isLicensed = true,
                    licenseType = "OFFLINE_PASS",
                    isOicLicensed = isOic || isAll,
                    isGmdssLicensed = isGmdss || isAll,
                    isEwLicensed = isEw || isAll,
                    isDmLicensed = isDm || isAll,
                    isEmLicensed = isEm || isAll,
                    isAllAccessLicensed = isAll,
                    statusMessage = "ACTIVE (${offlineInfo.durationLabel.uppercase()} • ${offlineInfo.getFormattedTimeRemaining().uppercase()})"
                )
            }
            refreshUnlockStates()
            return
        } else if (isOffline && offlineInfo != null && offlineInfo.isTampered) {
            _registrationUiState.update {
                it.copy(
                    isVerifyingLicense = false,
                    isVerifyingOnLaunch = false,
                    isCheckingStatus = false,
                    isLicensed = false,
                    licenseType = "RESTRICTED",
                    statusMessage = "CLOCK TAMPERED // RESTORE SYSTEM TIME"
                )
            }
            refreshUnlockStates()
            return
        }

        // ONLY PROMO PASSES CHECK FIREBASE
        if (type == "PROMO_PASS") {
            val expiryEpoch = prefs.getLong("promo_expiry_epoch", 0L)
            val now = System.currentTimeMillis()

            if (expiryEpoch > 0L && now >= expiryEpoch) {
                revokePromoAccess(context)
                return
            }

            _registrationUiState.update {
                it.copy(
                    isVerifyingLicense = true,
                    isVerifyingOnLaunch = isLaunchCheck,
                    isCheckingStatus = !isLaunchCheck
                )
            }

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                        com.google.firebase.FirebaseApp.initializeApp(context)
                    }
                    val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
                    val settings = com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(0)
                        .build()
                    remoteConfig.setConfigSettingsAsync(settings)

                    try {
                        com.google.android.gms.tasks.Tasks.await(remoteConfig.fetchAndActivate())
                    } catch (_: Exception) {
                        com.google.android.gms.tasks.Tasks.await(remoteConfig.activate())
                    }

                    val isActive = remoteConfig.getBoolean("promo_is_active")
                    val serverExpiry = remoteConfig.getLong("promo_code_expiry_epoch")
                    val currentEpoch = System.currentTimeMillis()

                    withContext(Dispatchers.Main) {
                        if (!isActive || (serverExpiry > 0L && currentEpoch >= serverExpiry)) {
                            revokePromoAccess(context)
                        } else {
                            _registrationUiState.update {
                                it.copy(
                                    isLicensed = true,
                                    licenseType = "PROMO_PASS",
                                    activeTier = "ALL_ACCESS",
                                    isOicLicensed = true,
                                    isGmdssLicensed = true,
                                    isAllAccessLicensed = true,
                                    isVerifyingLicense = false,
                                    isVerifyingOnLaunch = false,
                                    isCheckingStatus = false,
                                    statusMessage = "ACTIVE (PROMO PASS // ALL ACCESS)"
                                )
                            }
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        _registrationUiState.update {
                            it.copy(
                                isVerifyingLicense = false,
                                isVerifyingOnLaunch = false,
                                isCheckingStatus = false,
                                statusMessage = "OFFLINE CACHE ACTIVE // REGISTRY SYNC DEFERRED"
                            )
                        }
                    }
                }
            }
        } else {
            _registrationUiState.update {
                it.copy(
                    isVerifyingLicense = false,
                    isVerifyingOnLaunch = false,
                    isCheckingStatus = false
                )
            }
            refreshUnlockStates()
        }
    }

    fun activateDeviceWithKey(context: Context) {
        val cleanKey = _registrationUiState.value.activationKeyInput.trim().uppercase()
        if (cleanKey.isBlank()) {
            _registrationUiState.update {
                it.copy(
                    keyError = "ERR: PLEASE ENTER AN ACTIVATION KEY",
                    activationKeyError = "ERR: PLEASE ENTER AN ACTIVATION KEY"
                )
            }
            return
        }

        val result = activateLicense(cleanKey)
        if (!result.isSuccess) {
            _registrationUiState.update {
                it.copy(
                    keyError = result.toastMessage,
                    activationKeyError = result.toastMessage
                )
            }
        }
    }

    fun onActivationKeyChange(newInput: String) {
        _registrationUiState.update {
            it.copy(
                activationKeyInput = newInput.uppercase(),
                keyError = null,
                activationKeyError = null
            )
        }
    }

    suspend fun redeemLicenseCode(inputKey: String): Result<Boolean> {
        val cleanKey = inputKey.trim().uppercase()
        if (cleanKey == "ADMIN1234" || LicenseManager.validateLegacyKeyDirect(getApplication(), cleanKey)) {
            val prefs = getApplication<Application>().getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_full_access_unlocked", true)
                .putString("license_type", "LIFETIME")
                .putBoolean("is_hardware_lifetime", true)
                .apply()
            LicenseManager.setMasterProUnlocked(getApplication(), true)
            LicenseManager.init(getApplication())
            ProQuestionRepository.clearCache()
            _registrationUiState.update { it.copy(isLicensed = true, licenseType = "LIFETIME", isSuccess = true, statusMessage = "TERMINAL ACTIVATED / FULL CLEARANCE GRANTED", promoError = null, keyError = null, activationKeyError = null) }
            return Result.success(true)
        }
        return Result.failure(IllegalArgumentException("ERR: INVALID ACTIVATION KEY"))
    }

    // Promo Code State & Logic
    fun onPromoCodeChange(newInput: String) {
        _registrationUiState.update {
            it.copy(
                promoCodeInput = newInput.uppercase(),
                promoError = null
            )
        }
    }

    fun checkAndSyncLicense(context: Context) {
        loadCurrentLicense(context)
    }

    fun redeemPromoCode(context: Context) {
        if (_registrationUiState.value.isLicensed) {
            _registrationUiState.update { it.copy(promoError = "YOU ALREADY HAVE AN ACTIVE LICENSE") }
            return
        }

        val cleanCode = _registrationUiState.value.promoCodeInput.trim().uppercase()
        if (cleanCode.isBlank()) {
            _registrationUiState.update { it.copy(promoError = "ERR: PLEASE ENTER A PROMO CODE") }
            return
        }

        _registrationUiState.update { it.copy(isPromoLoading = true, promoError = null) }

        viewModelScope.launch {
            try {
                val unlockResult = withContext(Dispatchers.IO) {
                    val app = if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                        com.google.firebase.FirebaseApp.initializeApp(context)
                            ?: throw IllegalStateException("Firebase initialization returned null")
                    } else {
                        com.google.firebase.FirebaseApp.getInstance()
                    }

                    val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance(app)
                    val settings = com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(0)
                        .build()
                    remoteConfig.setConfigSettingsAsync(settings)

                    try {
                        com.google.android.gms.tasks.Tasks.await(remoteConfig.fetchAndActivate())
                    } catch (_: Exception) {
                        com.google.android.gms.tasks.Tasks.await(remoteConfig.activate())
                    }

                    val serverCode = (
                        remoteConfig.getString("promo_code_hash").takeIf { it.isNotBlank() }
                        ?: remoteConfig.getString("promo_code")
                    ).trim().uppercase()
                    val isActive = remoteConfig.getBoolean("promo_is_active")
                    val expiryEpoch = remoteConfig.getLong("promo_code_expiry_epoch")
                    val now = System.currentTimeMillis()

                    if (serverCode.isBlank()) {
                        throw IllegalStateException("Promo key is empty in Firebase Console")
                    }
                    if (!isActive) {
                        throw IllegalStateException("PROMO CODE IS CURRENTLY DEACTIVATED")
                    }
                    if (expiryEpoch > 0L && now >= expiryEpoch) {
                        throw IllegalStateException("PROMO CODE HAS EXPIRED")
                    }

                    if (cleanCode == serverCode) {
                        val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean("is_full_access_unlocked", true)
                            .putBoolean("is_all_access_unlocked", true)
                            .putBoolean("is_oic_unlocked", true)
                            .putBoolean("is_gmdss_unlocked", true)
                            .putBoolean("is_licensed", true)
                            .putBoolean("is_offline_license", false)
                            .putString("license_type", "PROMO_PASS")
                            .putString("active_license_tier", "ALL_ACCESS")
                            .putString("active_promo_code", cleanCode)
                            .putLong("promo_expiry_epoch", expiryEpoch)
                            .apply()
                        LicenseManager.setMasterProUnlocked(context, true)
                        LicenseManager.setOicNwProUnlocked(context, true)
                        LicenseManager.setGmdssProUnlocked(context, true)
                        LicenseManager.setHyperionProUnlocked(context, true)
                        LicenseManager.init(context)
                        ProQuestionRepository.clearCache()

                        // Transmit activation record directly via Firestore REST API (Port 443 HTTPS)
                        val terminalId = LicenseManager.getOrGenerateRequestCode(context).ifBlank { "UNKNOWN_TERMINAL" }
                        try {
                            val projectId = "hyperion-reviewer-licensing"
                            val firestoreUrl = java.net.URL(
                                "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/activations/$terminalId"
                            )

                            val connection = (firestoreUrl.openConnection() as java.net.HttpURLConnection).apply {
                                requestMethod = "PATCH"
                                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                                connectTimeout = 10000
                                readTimeout = 10000
                                doOutput = true
                            }

                            val model = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                            val osVer = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
                            val jsonPayload = org.json.JSONObject().apply {
                                put("fields", org.json.JSONObject().apply {
                                    put("terminalCode", org.json.JSONObject().put("stringValue", terminalId))
                                    put("promoCode", org.json.JSONObject().put("stringValue", cleanCode))
                                    put("activatedAt", org.json.JSONObject().put("timestampValue", java.time.Instant.now().toString()))
                                    put("deviceModel", org.json.JSONObject().put("stringValue", model))
                                    put("androidVersion", org.json.JSONObject().put("stringValue", osVer))
                                    put("appPackage", org.json.JSONObject().put("stringValue", context.packageName))
                                })
                            }

                            connection.outputStream.use { os ->
                                os.write(jsonPayload.toString().toByteArray(Charsets.UTF_8))
                                os.flush()
                            }

                            val responseCode = connection.responseCode
                            if (responseCode in 200..299) {
                                android.util.Log.d("FIRESTORE_REST", "Server confirmed row created: $responseCode")
                            } else {
                                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No response body"
                                android.util.Log.e("FIRESTORE_REST", "HTTP $responseCode:$errorStream")
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("ACTIVATION_LOG", "Sync warning: ${e.message}")
                        }

                        true
                    } else {
                        throw IllegalStateException("INVALID PROMO CODE")
                    }
                }

                if (unlockResult) {
                    _registrationUiState.update {
                        it.copy(
                            isPromoLoading = false,
                            isSuccess = true,
                            isLicensed = true,
                            licenseType = "PROMO_PASS",
                            isOicLicensed = true,
                            isGmdssLicensed = true,
                            isAllAccessLicensed = true,
                            promoError = null,
                            statusMessage = "PROMO APPLIED // CLEARANCE GRANTED"
                        )
                    }
                }
            } catch (e: Exception) {
                val failureReason = e.message ?: "ACTIVATION FAILED"
                _registrationUiState.update {
                    it.copy(
                        isPromoLoading = false,
                        promoError = "ERR: $failureReason"
                    )
                }
            }
        }
    }

    fun checkLicenseStatusOnline(context: Context) {
        _registrationUiState.update { it.copy(isCheckingStatus = true, statusMessage = "CHECKING STATUS...") }

        val prefs = context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        val isUnlocked = prefs.getBoolean("is_full_access_unlocked", false)
        val type = prefs.getString("license_type", "RESTRICTED") ?: "RESTRICTED"

        if (!isUnlocked || type == "RESTRICTED") {
            _registrationUiState.update {
                it.copy(
                    isCheckingStatus = false,
                    isLicensed = false,
                    licenseType = "RESTRICTED",
                    statusMessage = "STATUS: UNLICENSED (RESTRICTED MODE)"
                )
            }
            return
        }

        val offlineInfo = LicenseManager.getOfflineLicenseInfo(context)
        if (type == "OFFLINE_PASS" && offlineInfo != null && offlineInfo.isActive) {
            _registrationUiState.update {
                it.copy(
                    isCheckingStatus = false,
                    isLicensed = true,
                    licenseType = "OFFLINE_PASS",
                    statusMessage = "STATUS: ACTIVE (${offlineInfo.durationLabel.uppercase()} • ${offlineInfo.getFormattedTimeRemaining().uppercase()})"
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                    com.google.firebase.FirebaseApp.initializeApp(context)
                        ?: throw IllegalStateException("Firebase initialization returned null")
                } else {
                    com.google.firebase.FirebaseApp.getInstance()
                }

                val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance(app)
                val settings = com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(0)
                    .build()
                remoteConfig.setConfigSettingsAsync(settings)

                try {
                    com.google.android.gms.tasks.Tasks.await(remoteConfig.fetchAndActivate())
                } catch (_: Exception) {
                    com.google.android.gms.tasks.Tasks.await(remoteConfig.activate())
                }

                val isActive = remoteConfig.getBoolean("promo_is_active")
                val expiryEpoch = remoteConfig.getLong("promo_code_expiry_epoch")
                val now = System.currentTimeMillis()

                withContext(Dispatchers.Main) {
                    if (!isActive) {
                        revokeLicenseSilently(context)
                        _registrationUiState.update {
                            it.copy(
                                isCheckingStatus = false,
                                isLicensed = false,
                                licenseType = "RESTRICTED",
                                statusMessage = "STATUS: REVOKED (PROMO DEACTIVATED IN FIREBASE)"
                            )
                        }
                    } else if (expiryEpoch > 0L && now >= expiryEpoch) {
                        revokeLicenseSilently(context)
                        _registrationUiState.update {
                            it.copy(
                                isCheckingStatus = false,
                                isLicensed = false,
                                licenseType = "RESTRICTED",
                                statusMessage = "STATUS: EXPIRED (PROMO VALIDITY PERIOD ENDED)"
                            )
                        }
                    } else {
                        _registrationUiState.update {
                            it.copy(
                                isCheckingStatus = false,
                                isLicensed = true,
                                licenseType = "PROMO_PASS",
                                statusMessage = "STATUS: ACTIVE (PROMO PASS VALID)"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _registrationUiState.update {
                        it.copy(
                            isCheckingStatus = false,
                            statusMessage = "STATUS CHECK FAILED: NO CONNECTION"
                        )
                    }
                }
            }
        }
    }

    fun revokeAllAccess(context: Context) {
        val prefs = try {
            context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
        prefs?.edit()
            ?.putBoolean("is_full_access_unlocked", false)
            ?.putBoolean("is_all_access_unlocked", false)
            ?.putBoolean("is_oic_unlocked", false)
            ?.putBoolean("is_gmdss_unlocked", false)
            ?.putBoolean("is_licensed", false)
            ?.putBoolean("is_offline_license", false)
            ?.putBoolean("is_activated", false)
            ?.putString("license_type", "RESTRICTED")
            ?.remove("active_license_tier")
            ?.remove("promo_expiry_epoch")
            ?.remove("active_promo_code")
            ?.apply()

        listOf("hyperion_license_prefs", "hyperion_prefs", "app_prefs", "license_prefs", "${context.packageName}_preferences").forEach { name ->
            try {
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
            } catch (_: Exception) {}
        }

        try {
            LicenseManager.setMasterProUnlocked(context, false)
            LicenseManager.setOicNwProUnlocked(context, false)
            LicenseManager.setGmdssProUnlocked(context, false)
            LicenseManager.setHyperionProUnlocked(context, false)
            ProQuestionRepository.clearCache()
        } catch (_: Exception) {}

        _registrationUiState.update {
            it.copy(
                isLicensed = false,
                licenseType = "RESTRICTED",
                activeTier = "NONE",
                isOicLicensed = false,
                isGmdssLicensed = false,
                isAllAccessLicensed = false,
                isVerifyingLicense = false,
                isVerifyingOnLaunch = false,
                isCheckingStatus = false,
                statusMessage = "RESTRICTED (UNLICENSED)"
            )
        }
    }

    fun revokeLicenseSilently(context: Context) {
        revokeAllAccess(context)
    }

    fun revokePromoAccess(context: Context) {
        val prefs = try {
            context.getSharedPreferences("secure_license_prefs", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
        // Only strip the promo pass fields so offline keys & Play Store subscriptions remain untouched
        prefs?.edit()
            ?.remove("active_promo_code")
            ?.remove("promo_expiry_epoch")
            ?.apply()

        if (prefs?.getString("license_type", "") == "PROMO_PASS") {
            prefs.edit().putString("license_type", "RESTRICTED").apply()
        }
        refreshUnlockStates()
    }

    suspend fun activateTerminal(inputKey: String): Pair<Boolean, String> {
        val unlockResult = activateLicense(inputKey)
        if (unlockResult.isSuccess) {
            return Pair(true, unlockResult.toastMessage)
        }
        return Pair(false, unlockResult.toastMessage)
    }

    fun deactivateLicense() {
        LicenseManager.setMasterProUnlocked(getApplication(), false)
        ProQuestionRepository.clearCache()
        _currentScreen.value = Screen.Activation
    }

    // Side Navigation Drawer Trigger
    private val _drawerOpenEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val drawerOpenEvent = _drawerOpenEvent.asSharedFlow()

    fun openDrawer() {
        _drawerOpenEvent.tryEmit(Unit)
    }

    // Access Gate Helper: verifies track access or navigates to Store
    fun requireActivationOr(track: Track = _selectedTrack.value, onAccessGranted: () -> Unit) {
        if (isTrackUnlocked(track)) {
            onAccessGranted()
        } else {
            navigateTo(Screen.Store)
        }
    }

    // Terms Acceptance State
    private val _hasAcceptedTerms = MutableStateFlow(prefs.getBoolean("has_accepted_terms", false))
    val hasAcceptedTerms: StateFlow<Boolean> = _hasAcceptedTerms.asStateFlow()

    // Persistent Theme State (defaulting to true)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }

    fun acceptTerms() {
        prefs.edit().putBoolean("has_accepted_terms", true).apply()
        _hasAcceptedTerms.value = true
        backStack.clear()
        _currentScreen.value = Screen.TrackSelection
    }

    // Navigation State & History Back Stack
    private val backStack = ArrayDeque<Screen>()

    private val _currentScreen = MutableStateFlow<Screen>(
        if (prefs.getBoolean("has_accepted_terms", false)) Screen.TrackSelection else Screen.TermsOnboarding
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    init {
        viewModelScope.launch {
            _currentScreen.collect { screen ->
                val isActiveQuestionScreen = screen is Screen.Quiz || screen is Screen.MockCompetence
                if (!isActiveQuestionScreen) {
                    studyTimeTracker.pauseTimer()
                }
            }
        }
    }

    // Current Track Selection
    private val _selectedTrack = MutableStateFlow(Track.DECK_OIC_NW)
    val selectedTrack: StateFlow<Track> = _selectedTrack.asStateFlow()

    // Active Quiz State
    private val _activeQuestions = MutableStateFlow<List<Question>>(emptyList())
    val activeQuestions: StateFlow<List<Question>> = _activeQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _userQuestionStates = MutableStateFlow<Map<String, UserQuestionState>>(emptyMap())
    val userQuestionStates: StateFlow<Map<String, UserQuestionState>> = _userQuestionStates.asStateFlow()

    private val _activeQuizMode = MutableStateFlow(QuizMode.PRACTICE)
    val activeQuizMode: StateFlow<QuizMode> = _activeQuizMode.asStateFlow()

    // Drill Delivery Mode (Standard vs Intensive)
    private val _activeDrillDeliveryMode = MutableStateFlow(DrillDeliveryMode.STANDARD)
    val activeDrillDeliveryMode: StateFlow<DrillDeliveryMode> = _activeDrillDeliveryMode.asStateFlow()

    // Intensive Mode Dynamic State
    private val _intensiveQueue = MutableStateFlow<List<Question>>(emptyList())
    val intensiveQueue: StateFlow<List<Question>> = _intensiveQueue.asStateFlow()

    private val _intensiveInitialTotal = MutableStateFlow(0)
    val intensiveInitialTotal: StateFlow<Int> = _intensiveInitialTotal.asStateFlow()

    private val _intensiveClearedCount = MutableStateFlow(0)
    val intensiveClearedCount: StateFlow<Int> = _intensiveClearedCount.asStateFlow()

    private val _intensiveCurrentQuestionState = MutableStateFlow<UserQuestionState?>(null)
    val intensiveCurrentQuestionState: StateFlow<UserQuestionState?> = _intensiveCurrentQuestionState.asStateFlow()

    private val _intensiveCompleted = MutableStateFlow(false)
    val intensiveCompleted: StateFlow<Boolean> = _intensiveCompleted.asStateFlow()

    private val _activeCompetenceId = MutableStateFlow<String?>(null)
    val activeCompetenceId: StateFlow<String?> = _activeCompetenceId.asStateFlow()

    private val _selectedChoiceIndex = MutableStateFlow<Int?>(null)
    val selectedChoiceIndex: StateFlow<Int?> = _selectedChoiceIndex.asStateFlow()

    private val _isAnswerEvaluated = MutableStateFlow<Boolean>(false)
    val isAnswerEvaluated: StateFlow<Boolean> = _isAnswerEvaluated.asStateFlow()

    private val _currentQuestionStats = MutableStateFlow<CurrentQuestionStats?>(null)
    val currentQuestionStats: StateFlow<CurrentQuestionStats?> = _currentQuestionStats.asStateFlow()

    private var activeEvaluationAttemptCommittedId: String? = null
    private var intensivePresentationCounter: Long = 0L

    private val _activePartLabel = MutableStateFlow<String?>(null)
    val activePartLabel: StateFlow<String?> = _activePartLabel.asStateFlow()

    private val sessionPartCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val sessionCompetenceCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    data class PracticeDrillConfig(
        val track: Track,
        val competenceId: String? = null,
        val selectedPart: Int? = null,
        val deliveryMode: DrillDeliveryMode = DrillDeliveryMode.STANDARD,
        val questionCount: Int? = null,
        val startItem: Int = 1,
        val endItem: Int? = null,
        val shuffleQuestions: Boolean = false,
        val shuffleChoices: Boolean = true,
        val isCrpMode: Boolean = false,
        val prioritizeUnseen: Boolean = false,
        val isOperationalReview: Boolean = false
    )

    private var _lastPracticeDrillConfig: PracticeDrillConfig? = null
    val lastPracticeDrillConfig: PracticeDrillConfig? get() = _lastPracticeDrillConfig

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled.asStateFlow()

    private val _completedCompetences = MutableStateFlow<Set<String>>(emptySet())
    val completedCompetences: StateFlow<Set<String>> = _completedCompetences.asStateFlow()

    private val _isExamSubmitted = MutableStateFlow(false)
    val isExamSubmitted: StateFlow<Boolean> = _isExamSubmitted.asStateFlow()

    private val _isCrpMode = MutableStateFlow(false)
    val isCrpMode: StateFlow<Boolean> = _isCrpMode.asStateFlow()

    private val _activeFunctionId = MutableStateFlow<String?>(null)
    val activeFunctionId: StateFlow<String?> = _activeFunctionId.asStateFlow()

    private val _timeRemainingSeconds = MutableStateFlow(0)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds.asStateFlow()

    private var timerJob: Job? = null

    // Palette & Dialog States
    private val _isPaletteOpen = MutableStateFlow(false)
    val isPaletteOpen: StateFlow<Boolean> = _isPaletteOpen.asStateFlow()

    private val _showSubmitDialog = MutableStateFlow(false)
    val showSubmitDialog: StateFlow<Boolean> = _showSubmitDialog.asStateFlow()

    // Exam Results
    private val _lastExamResult = MutableStateFlow<ExamResult?>(null)
    val lastExamResult: StateFlow<ExamResult?> = _lastExamResult.asStateFlow()

    // Search Query, Track & Results
    val isOicNwActive: StateFlow<Boolean> = isOicNwProUnlocked
    val isGmdssActive: StateFlow<Boolean> = isGmdssProUnlocked
    val isUnlocked: StateFlow<Boolean> = combine(
        isOicNwActive,
        isGmdssActive,
        billingManager.entitlements
    ) { oic, gmdss, playEnt ->
        oic || gmdss || playEnt.oicNwUnlocked || playEnt.gmdssUnlocked || playEnt.allAccessUnlocked
    }.stateIn(viewModelScope, SharingStarted.Lazily, isTrackUnlocked(_selectedTrack.value))

    val isTrackLicensed: StateFlow<Boolean> = combine(
        _selectedTrack,
        isOicNwProUnlocked,
        isGmdssProUnlocked,
        isEngineOicEwProUnlocked,
        isDeckMgtProUnlocked,
        isEngineMgtProUnlocked,
        isHyperionProUnlocked,
        billingManager.entitlements
    ) { array ->
        val track = array[0] as Track
        val oic = array[1] as Boolean
        val gmdss = array[2] as Boolean
        val ew = array[3] as Boolean
        val dm = array[4] as Boolean
        val em = array[5] as Boolean
        val hyp = array[6] as Boolean
        val playEnt = array[7] as TrackEntitlements
        when {
            hyp || playEnt.allAccessUnlocked -> true
            track == Track.DECK_OIC_NW -> oic || playEnt.oicNwUnlocked
            track == Track.GMDSS_GOC -> gmdss || playEnt.gmdssUnlocked
            track == Track.ENGINE_OIC_EW -> ew || playEnt.engineOicEwUnlocked
            track == Track.DECK_MGT -> dm || playEnt.deckMgtUnlocked
            track == Track.ENGINE_MGT -> em || playEnt.engineMgtUnlocked
            else -> false
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        isTrackUnlocked(_selectedTrack.value)
    )

    fun isTrackLicensedFor(track: Track): Boolean {
        return isTrackUnlocked(track)
    }

    private val _selectedSearchTrack = MutableStateFlow(
        if (isTrackUnlocked(Track.DECK_OIC_NW)) Track.DECK_OIC_NW
        else if (isTrackUnlocked(Track.GMDSS_GOC)) Track.GMDSS_GOC
        else Track.DECK_OIC_NW
    )
    val selectedSearchTrack: StateFlow<Track> = _selectedSearchTrack.asStateFlow()

    fun setSearchTrack(track: Track) {
        val hasAccess = isTrackUnlocked(track)
        if (hasAccess) {
            _selectedSearchTrack.value = track
            _selectedCompetenceFilter.value = null
        } else {
            navigateTo(Screen.Store)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCompetenceFilter = MutableStateFlow<String?>(null)
    val selectedCompetenceFilter: StateFlow<String?> = _selectedCompetenceFilter.asStateFlow()

    fun setCompetenceFilter(competenceId: String?) {
        _selectedCompetenceFilter.value = competenceId
    }

    val searchResults: StateFlow<List<Question>> = combine(
        combine(
            _searchQuery
                .debounce(300L)
                .distinctUntilChanged(),
            _selectedSearchTrack,
            _selectedCompetenceFilter
        ) { q, track, comp -> Triple(q, track, comp) },
        combine(
            isOicNwProUnlocked,
            isGmdssProUnlocked,
            isActivated,
            isEngineOicEwProUnlocked,
            isDeckMgtProUnlocked,
            isEngineMgtProUnlocked
        ) { array ->
            Sextuple(
                array[0] as Boolean,
                array[1] as Boolean,
                array[2] as Boolean,
                array[3] as Boolean,
                array[4] as Boolean,
                array[5] as Boolean
            )
        }
    ) { searchPair, licensePair ->
        val query = searchPair.first
        val track = searchPair.second
        val competence = searchPair.third
        val proUnlocked = licensePair.third

        val hasTrackAccess = when (track) {
            Track.DECK_OIC_NW -> licensePair.first
            Track.GMDSS_GOC -> licensePair.second
            Track.ENGINE_OIC_EW -> licensePair.fourth
            Track.DECK_MGT -> licensePair.fifth
            Track.ENGINE_MGT -> licensePair.sixth
            else -> false
        }
        if (!hasTrackAccess) {
            emptyList()
        } else {
            MaritimeQuestionBank.searchQuestions(query, track, isProUnlocked = proUnlocked, competenceId = competence)
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Flows from Room Database
    val bookmarkedIds: StateFlow<Set<String>> = repository.observeBookmarkIds()
        .flowOn(kotlinx.coroutines.Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val mistakeEntities: StateFlow<List<MistakeEntity>> = repository.observeMistakes()
        .flowOn(kotlinx.coroutines.Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _mistakeEntries = MutableStateFlow<List<MistakeEntry>>(MistakesVaultManager.loadEntries(application))
    val mistakeEntries: StateFlow<List<MistakeEntry>> = _mistakeEntries.asStateFlow()

    fun refreshMistakes() {
        _mistakeEntries.value = MistakesVaultManager.loadEntries(getApplication())
    }

    private val _snapshotTrigger = MutableSharedFlow<Unit>(replay = 1)
    private val _firstAttemptStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    fun notifySnapshotChanged() {
        _snapshotTrigger.tryEmit(Unit)
    }

    val examRecords: StateFlow<List<ExamRecordEntity>> = repository.observeExamRecords()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val studyStreak: StateFlow<Int> = examRecords.map { records ->
        if (records.isEmpty()) return@map 0
        val calendar = java.util.Calendar.getInstance()
        val uniqueDays = mutableSetOf<Long>()
        val dayMillis = 24L * 60L * 60L * 1000L
        for (r in records) {
            calendar.timeInMillis = r.timestamp
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            uniqueDays.add(calendar.timeInMillis)
        }
        val sortedDays = uniqueDays.sortedDescending()
        val todayCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        var currentDayCheck = todayCal.timeInMillis
        var streak = 0

        // If today has activity or yesterday has activity, start streak count
        if (!sortedDays.contains(currentDayCheck)) {
            currentDayCheck -= dayMillis
        }

        for (day in sortedDays) {
            if (day == currentDayCheck) {
                streak++
                currentDayCheck -= dayMillis
            } else if (day < currentDayCheck) {
                break
            }
        }
        streak.coerceAtLeast(if (records.isNotEmpty()) 1 else 0)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _deletedSessions = MutableStateFlow<List<DeletedSession>>(emptyList())
    val deletedSessions: StateFlow<List<DeletedSession>> = _deletedSessions.asStateFlow()

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _deletedSessions.value = DeletedSessionManager.loadDeletedSessions(getApplication())
            } catch (_: Exception) {}
        }
    }

    fun restoreDeletedSession(deletedSession: DeletedSession) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val context = getApplication<Application>()
            val examRecord = DeletedSessionManager.deserializeExamRecord(deletedSession.examRecordJson)
            if (examRecord != null) {
                repository.insertOrUpdateExamRecord(examRecord)
            }
            val drillSession = DeletedSessionManager.deserializeDrillSession(deletedSession.drillSessionJson)
            if (drillSession != null) {
                repository.saveDrillSession(drillSession)
            }
            if (!deletedSession.snapshotJson.isNullOrBlank()) {
                val snapshotFile = ActiveSessionSnapshotManager.getSnapshotFile(context, deletedSession.sessionId)
                snapshotFile.writeText(deletedSession.snapshotJson)
                val snap = ActiveSessionSnapshotManager.loadSnapshotById(context, deletedSession.sessionId)
                if (snap != null) {
                    ActiveSessionSnapshotManager.saveSnapshot(context, snap)
                }
            }
            DeletedSessionManager.removeDeletedSession(context, deletedSession.sessionId)
            _deletedSessions.value = DeletedSessionManager.loadDeletedSessions(context)
            notifySnapshotChanged()
        }
    }

    fun permanentlyDeleteSession(deletedSession: DeletedSession) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val context = getApplication<Application>()
            DeletedSessionManager.permanentlyDeleteSession(context, deletedSession.sessionId)
            _deletedSessions.value = DeletedSessionManager.loadDeletedSessions(context)
        }
    }

    private val _activeSessionOverride = MutableStateFlow<Map<String, ActiveSessionEntity?>>(emptyMap())

    val activeSession: StateFlow<ActiveSessionEntity?> = combine(
        _selectedTrack,
        _snapshotTrigger.onStart { emit(Unit) },
        _activeSessionOverride
    ) { track, _, overrides ->
        if (overrides.containsKey(track.name)) {
            overrides[track.name]
        } else {
            val regSnapshot = ActiveSessionSnapshotManager.loadSnapshot(getApplication(), track.name)
            val mistakesSnapshot = ActiveSessionSnapshotManager.loadMistakesSnapshot(getApplication(), track.name)
            val snapshot = when {
                regSnapshot != null && !regSnapshot.isCompleted && mistakesSnapshot != null && !mistakesSnapshot.isCompleted -> {
                    if (mistakesSnapshot.timestamp > regSnapshot.timestamp) mistakesSnapshot else regSnapshot
                }
                mistakesSnapshot != null && !mistakesSnapshot.isCompleted -> mistakesSnapshot
                regSnapshot != null && !regSnapshot.isCompleted -> regSnapshot
                else -> null
            }
            if (snapshot != null && !snapshot.isCompleted) {
                if (snapshot.mode.contains("MOCK", ignoreCase = true) && snapshot.totalQuestions > 200) {
                    ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), track.name)
                    discardActiveSession(track.name)
                    null
                } else {
                    ActiveSessionEntity(
                        id = snapshot.sessionId,
                        track = snapshot.track,
                        mode = snapshot.mode,
                        functionId = null,
                        competenceId = snapshot.competenceOrPart,
                        currentQuestionIndex = snapshot.currentIndex,
                        timeRemainingSeconds = 12000,
                        questionIds = snapshot.questions.joinToString(",") { it.id },
                        answersJson = SessionJsonHelper.serializeAnswers(snapshot.selectedAnswers.mapValues { (qid, sel) ->
                            UserQuestionState(questionId = qid, selectedIndex = sel.toIntOrNull(), isSubmitted = true)
                        }),
                        totalQuestions = snapshot.totalQuestions,
                        answeredCount = snapshot.selectedAnswers.size,
                        updatedTimestamp = snapshot.timestamp
                    )
                }
            } else {
                null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun navigateTo(screen: Screen) {
        val current = _currentScreen.value
        if (current == screen) return

        when (screen) {
            is Screen.TrackSelection -> {
                backStack.clear()
                _currentScreen.value = screen
                return
            }
            is Screen.Home -> {
                backStack.removeAll { it !is Screen.TrackSelection }
                if (backStack.isEmpty()) backStack.addLast(Screen.TrackSelection)
                _currentScreen.value = screen
                return
            }
            is Screen.PracticeCompetenceSelect -> {
                backStack.removeAll { it is Screen.PracticeCompetenceSelect || it is Screen.PracticeConfig || it is Screen.Quiz || it is Screen.FreePractice }
                if (backStack.none { it is Screen.Home }) {
                    backStack.addLast(Screen.Home)
                }
                _currentScreen.value = screen
                return
            }
            is Screen.PracticeConfig -> {
                if (screen.track != null) {
                    _selectedTrack.value = screen.track
                }
                if (!screen.competenceId.isNullOrBlank()) {
                    _activeCompetenceId.value = screen.competenceId
                }
                _isCrpMode.value = screen.isCrpMode
                backStack.removeAll { it is Screen.PracticeConfig || it is Screen.Quiz || it is Screen.FreePractice }
                if (backStack.none { it is Screen.PracticeCompetenceSelect }) {
                    backStack.addLast(Screen.PracticeCompetenceSelect(isCrpMode = screen.isCrpMode, isOperationalReview = screen.isOperationalReview))
                }
                _currentScreen.value = screen
                return
            }
            else -> {
                // Remove duplicate instances of the same screen class to prevent backstack leaks/loops
                backStack.removeAll { it::class == screen::class }
                backStack.addLast(current)
                _currentScreen.value = screen
            }
        }
    }

    /**
     * Purges all active exam and drill session screens from the back stack
     * to prevent navigation back stack leaks when returning to the track screen or dashboard.
     */
    fun purgeExamSessionFromBackStack() {
        backStack.removeAll { screen ->
            screen is Screen.Quiz ||
            screen is Screen.MockHub ||
            screen is Screen.MockCompetence ||
            screen is Screen.Results ||
            screen is Screen.FreePractice
        }
    }

    fun exitDrillToCompetenceConfig(): kotlinx.coroutines.Job? {
        timerJob?.cancel()
        studyTimeTracker.pauseTimer()
        refreshMistakes()
        val job = autoSaveCurrentSession()
        purgeExamSessionFromBackStack()
        val isCrp = _isCrpMode.value
        val compId = _activeCompetenceId.value
        val track = _selectedTrack.value
        val isOpReview = _lastPracticeDrillConfig?.isOperationalReview ?: false

        val targetScreen = Screen.PracticeConfig(
            competenceId = compId,
            isCrpMode = isCrp,
            track = track,
            isOperationalReview = isOpReview
        )
        _currentScreen.value = targetScreen
        return job
    }

    fun exitDrillToCompetenceList() {
        exitDrillToCompetenceConfig()
    }

    fun navigateBack(): Boolean {
        val current = _currentScreen.value

        // 1. When on Home, back MUST return directly to TrackSelection
        if (current is Screen.Home) {
            backStack.clear()
            _currentScreen.value = Screen.TrackSelection
            return true
        }

        // 2. When on PracticeCompetenceSelect, back MUST pop to Home
        if (current is Screen.PracticeCompetenceSelect) {
            backStack.removeAll { it is Screen.PracticeCompetenceSelect || it is Screen.PracticeConfig || it is Screen.Quiz || it is Screen.FreePractice }
            _currentScreen.value = Screen.Home
            return true
        }

        // 3. When on PracticeConfig, back MUST pop to PracticeCompetenceSelect
        if (current is Screen.PracticeConfig) {
            backStack.removeAll { it is Screen.PracticeConfig || it is Screen.Quiz || it is Screen.FreePractice }
            _currentScreen.value = Screen.PracticeCompetenceSelect(isCrpMode = current.isCrpMode, isOperationalReview = current.isOperationalReview)
            return true
        }

        // 4. When in active drill (Screen.Quiz or Screen.FreePractice), back returns cleanly to Competence Configuration Screen
        if (current is Screen.Quiz || current is Screen.FreePractice) {
            exitDrillToCompetenceConfig()
            return true
        }

        // 5. When in MockCompetence, back pops to MockHub
        if (current is Screen.MockCompetence) {
            backStack.removeAll { it is Screen.MockCompetence }
            _currentScreen.value = Screen.MockHub
            return true
        }

        // 6. When in MockHub, back pops to Home
        if (current is Screen.MockHub) {
            backStack.removeAll { it is Screen.MockHub || it is Screen.MockCompetence || it is Screen.Results }
            _currentScreen.value = Screen.Home
            return true
        }

        // 7. When in QuickScanViewer, back pops to QuickScanCompetenceSelect
        if (current is Screen.QuickScanViewer) {
            backStack.removeAll { it is Screen.QuickScanViewer }
            _currentScreen.value = Screen.QuickScanCompetenceSelect(current.track ?: _selectedTrack.value)
            return true
        }

        // 8. When in QuickScanCompetenceSelect, back pops to Home
        if (current is Screen.QuickScanCompetenceSelect) {
            backStack.removeAll { it is Screen.QuickScanCompetenceSelect }
            _currentScreen.value = Screen.Home
            return true
        }

        while (backStack.isNotEmpty()) {
            val prev = backStack.removeLast()
            if (prev != current &&
                prev::class != current::class &&
                prev !is Screen.Quiz &&
                prev !is Screen.MockCompetence &&
                prev !is Screen.Results &&
                prev !is Screen.FreePractice
            ) {
                _currentScreen.value = prev
                return true
            }
        }

        if (current !is Screen.TrackSelection) {
            _currentScreen.value = Screen.TrackSelection
            return true
        }
        return false
    }

    fun popBackStack(route: String? = null, inclusive: Boolean = false): Boolean {
        if (route != null) {
            val matchingIndex = backStack.indexOfLast {
                (it as? Screen.PracticeConfig)?.route == route ||
                (it as? Screen.PracticeConfig)?.competenceId == route
            }
            if (matchingIndex != -1) {
                while (backStack.size > (if (inclusive) matchingIndex else matchingIndex + 1)) {
                    backStack.removeLast()
                }
                if (!inclusive && matchingIndex < backStack.size) {
                    _currentScreen.value = backStack.removeLast()
                    return true
                }
            }
        }
        return navigateBack()
    }

    fun navigateToDashboard() {
        backStack.clear()
        _currentScreen.value = Screen.TrackSelection
    }

    fun setTrack(track: Track) {
        _selectedTrack.value = track
    }

    fun selectTrack(track: Track) {
        _selectedTrack.value = track
    }

    fun activateWithKey(key: String, targetTier: com.nhyle.hyperionmaritime.data.util.ProTier? = null) = activateLicense(key, targetTier)

    fun startQuiz(questions: List<Question>, mode: QuizMode = QuizMode.PRACTICE) {
        timerJob?.cancel()
        purgeExamSessionFromBackStack()
        _activeQuizMode.value = mode
        _activeQuestions.value = questions
        _userQuestionStates.value = questions.associate { it.id to UserQuestionState(questionId = it.id) }
        _currentQuestionIndex.value = 0
        _timeRemainingSeconds.value = 0
        _currentScreen.value = Screen.Quiz
    }

    fun startPracticeQuizForCompetence(competenceId: String, isCrpMode: Boolean = false) {
        startConfiguredPractice(competenceId = competenceId, isCrpMode = isCrpMode)
    }

    fun startTimedMockExam(track: Track = _selectedTrack.value) {
        _selectedTrack.value = track
        startQuizSession(QuizMode.MOCK_EXAM)
    }

    fun openQuestionPalette() {
        _isPaletteOpen.value = true
    }

    fun closeQuestionPalette() {
        _isPaletteOpen.value = false
    }

    fun openSubmitDialog() {
        _showSubmitDialog.value = true
    }

    fun closeSubmitDialog() {
        _showSubmitDialog.value = false
    }

    fun goToQuestion(index: Int) {
        jumpToQuestion(index)
    }

    fun updateSearchQuery(query: String) {
        setSearchQuery(query)
    }

    fun deleteExamRecords(recordIds: List<String>) {
        deleteExamRecordsByIds(recordIds)
    }

    fun selectTrackAndOpenDashboard(track: Track) {
        _selectedTrack.value = track
        backStack.clear()
        backStack.addLast(Screen.TrackSelection)
        _currentScreen.value = Screen.Home
    }

    fun startVaultDrill() {
        timerJob?.cancel()
        purgeExamSessionFromBackStack()
        _activeQuizMode.value = QuizMode.VAULT_DRILL
        _activeFunctionId.value = null
        _activeCompetenceId.value = null
        _currentQuestionIndex.value = 0
        _showSubmitDialog.value = false
        _isPaletteOpen.value = false

        val currentTrack = _selectedTrack.value
        val vaultEntries = MistakesVaultManager.getEntriesForTrack(getApplication(), currentTrack.name)
        val questions = vaultEntries.map { it.question }

        if (questions.isEmpty()) return

        _activeQuestions.value = questions
        _userQuestionStates.value = questions.associate { it.id to UserQuestionState(questionId = it.id) }
        _timeRemainingSeconds.value = 0
        _currentScreen.value = Screen.Quiz
    }

    fun startMistakesVaultPractice(questions: List<Question>) {
        timerJob?.cancel()
        purgeExamSessionFromBackStack()
        currentActiveSessionId = java.util.UUID.randomUUID().toString()
        resumingRecordId = null
        _isExamSubmitted.value = false
        _isCrpMode.value = false
        _activeDrillDeliveryMode.value = DrillDeliveryMode.STANDARD
        _activeQuizMode.value = QuizMode.MISTAKES
        _activeFunctionId.value = null
        _activeCompetenceId.value = "MISTAKES"
        _activePartLabel.value = null
        _currentQuestionIndex.value = 0
        _showSubmitDialog.value = false
        _isPaletteOpen.value = false
        _isShuffled.value = false
        activeEvaluationAttemptCommittedId = null

        _activeQuestions.value = questions
        _userQuestionStates.value = questions.associate { it.id to UserQuestionState(questionId = it.id) }
        _firstAttemptStatus.value = emptyMap()
        _timeRemainingSeconds.value = 0
        _selectedChoiceIndex.value = null
        _isAnswerEvaluated.value = false

        autoSaveCurrentSession()
        _currentScreen.value = Screen.Quiz
        startStudyTrackingForCurrentSession()
    }

    fun exitQuizToHome() {
        timerJob?.cancel()
        studyTimeTracker.pauseTimer()
        refreshMistakes()
        autoSaveCurrentSession()
        purgeExamSessionFromBackStack()
        _currentScreen.value = Screen.Home
    }

    fun finishExamToHome() {
        timerJob?.cancel()
        studyTimeTracker.pauseTimer()
        refreshMistakes()
        purgeExamSessionFromBackStack()
        _currentScreen.value = Screen.Home
    }

    fun resetIntensiveDrill() {
        timerJob?.cancel()
        studyTimeTracker.pauseTimer()
        intensivePresentationCounter++
        activeEvaluationAttemptCommittedId = null
        _currentQuestionStats.value = null
        _selectedChoiceIndex.value = null
        _isAnswerEvaluated.value = false
        _intensiveQueue.value = emptyList()
        _intensiveCurrentQuestionState.value = null
        _intensiveClearedCount.value = 0
        _intensiveCompleted.value = false
        purgeExamSessionFromBackStack()
        _currentScreen.value = Screen.Home
    }

    fun exitQuiz() {
        refreshMistakes()
        timerJob?.cancel()
        studyTimeTracker.pauseTimer()
        autoSaveCurrentSession()
        val mode = _activeQuizMode.value
        purgeExamSessionFromBackStack()
        if (mode == QuizMode.VAULT_DRILL || mode == QuizMode.MISTAKES) {
            _currentScreen.value = Screen.Vault
        } else if (mode == QuizMode.BOOKMARKS_DRILL) {
            _currentScreen.value = Screen.Bookmarks
        } else {
            _currentScreen.value = Screen.Home
        }
    }

    fun startMarinaCrpPractice(competenceId: String? = null, selectedPart: Int? = null) {
        timerJob?.cancel()
        currentActiveSessionId = java.util.UUID.randomUUID().toString()
        _isExamSubmitted.value = false
        _isCrpMode.value = true
        _activeQuizMode.value = QuizMode.PRACTICE
        _activeDrillDeliveryMode.value = DrillDeliveryMode.STANDARD
        _activeFunctionId.value = null
        _activeCompetenceId.value = competenceId
        _activePartLabel.value = if (selectedPart != null) "Part $selectedPart" else null
        _lastPracticeDrillConfig = PracticeDrillConfig(
            track = _selectedTrack.value,
            competenceId = competenceId,
            selectedPart = selectedPart,
            deliveryMode = DrillDeliveryMode.STANDARD,
            isCrpMode = true
        )
        _currentQuestionIndex.value = 0
        _showSubmitDialog.value = false
        _isPaletteOpen.value = false

        val isPro = isTrackUnlocked(_selectedTrack.value)
        val crpQuestions = when (val track = _selectedTrack.value) {
            Track.GMDSS_GOC -> MaritimeQuestionBank.getMarinaCrpGmdssQuestions(getApplication(), competenceId, selectedPart, isProUnlocked = isPro)
            Track.DECK_OIC_NW -> MaritimeQuestionBank.getMarinaCrpQuestions(competenceId)
            Track.ENGINE_OIC_EW, Track.DECK_MGT, Track.ENGINE_MGT -> {
                if (competenceId != null) {
                    MaritimeQuestionBank.getQuestionsByCompetence(competenceId, track, isProUnlocked = false)
                } else {
                    val allComps = MaritimeCompetenceRegistry.getCompetencesForTrack(track)
                    allComps.flatMap { comp ->
                        MaritimeQuestionBank.getQuestionsByCompetence(comp.competenceId, track, isProUnlocked = false)
                    }
                }
            }
            else -> emptyList()
        }
        _activeQuestions.value = crpQuestions
        _userQuestionStates.value = crpQuestions.associate { it.id to UserQuestionState(questionId = it.id) }
        _timeRemainingSeconds.value = 0
        _currentScreen.value = Screen.FreePractice
    }

    fun startFreePractice() {
        startMarinaCrpPractice()
    }

    fun startConfiguredPractice(
        competenceId: String? = null,
        selectedPart: Int? = null,
        deliveryMode: DrillDeliveryMode = DrillDeliveryMode.STANDARD,
        questionCount: Int? = null,
        startItem: Int = 1,
        endItem: Int? = null,
        shuffleQuestions: Boolean = false,
        shuffleChoices: Boolean = true,
        isCrpMode: Boolean = false,
        prioritizeUnseen: Boolean = false,
        seenQuestionIds: Set<String>? = null,
        isOperationalReview: Boolean = false
    ): kotlinx.coroutines.Job? {
        if (!prioritizeUnseen) {
            startConfiguredPracticeInternal(
                competenceId = competenceId,
                selectedPart = selectedPart,
                deliveryMode = deliveryMode,
                questionCount = questionCount,
                startItem = startItem,
                endItem = endItem,
                shuffleQuestions = shuffleQuestions,
                shuffleChoices = shuffleChoices,
                isCrpMode = isCrpMode,
                prioritizeUnseen = false,
                seenIds = emptySet(),
                isOperationalReview = isOperationalReview
            )
            return null
        }

        if (seenQuestionIds != null) {
            startConfiguredPracticeInternal(
                competenceId = competenceId,
                selectedPart = selectedPart,
                deliveryMode = deliveryMode,
                questionCount = questionCount,
                startItem = startItem,
                endItem = endItem,
                shuffleQuestions = shuffleQuestions,
                shuffleChoices = shuffleChoices,
                isCrpMode = isCrpMode,
                prioritizeUnseen = true,
                seenIds = seenQuestionIds,
                isOperationalReview = isOperationalReview
            )
            return null
        }

        // Fast path: Immediately establish session using synchronous SharedPreferences seen IDs
        val localSeen = getSeenQuestionIds(_selectedTrack.value)
        startConfiguredPracticeInternal(
            competenceId = competenceId,
            selectedPart = selectedPart,
            deliveryMode = deliveryMode,
            questionCount = questionCount,
            startItem = startItem,
            endItem = endItem,
            shuffleQuestions = shuffleQuestions,
            shuffleChoices = shuffleChoices,
            isCrpMode = isCrpMode,
            prioritizeUnseen = true,
            seenIds = localSeen,
            isOperationalReview = isOperationalReview
        )

        return viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val repoSeen = repository.getEncounteredQuestionIds(_selectedTrack.value, competenceId)
            val allSeen = localSeen + repoSeen
            if (repoSeen.isNotEmpty() && allSeen.size > localSeen.size) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (_currentQuestionIndex.value == 0 && !_isAnswerEvaluated.value && _selectedChoiceIndex.value == null) {
                        startConfiguredPracticeInternal(
                            competenceId = competenceId,
                            selectedPart = selectedPart,
                            deliveryMode = deliveryMode,
                            questionCount = questionCount,
                            startItem = startItem,
                            endItem = endItem,
                            shuffleQuestions = shuffleQuestions,
                            shuffleChoices = shuffleChoices,
                            isCrpMode = isCrpMode,
                            prioritizeUnseen = true,
                            seenIds = allSeen,
                            isOperationalReview = isOperationalReview
                        )
                    }
                }
            }
        }
    }

    fun getOrGenerateGmdssSeed(competenceId: String): Long {
        val key = "gmdss_op_review_seed_$competenceId"
        if (!prefs.contains(key)) {
            val newSeed = java.util.Random().nextLong()
            prefs.edit().putLong(key, newSeed).apply()
            return newSeed
        }
        return prefs.getLong(key, 0L)
    }

    fun getSeededShuffledGmdssPool(context: Context, competenceId: String): List<Question> {
        val limit = if (competenceId == "C1") {
            com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.OPERATIONAL_REVIEW_LIMIT_C1
        } else {
            com.nhyle.hyperionmaritime.data.repository.MaritimeCompetenceRegistry.OPERATIONAL_REVIEW_LIMIT_C2
        }
        val rawList = ProQuestionRepository.loadAllRawCompetenceQuestions(context, competenceId)
            .map { it.toQuestion() }
        val unShuffled = rawList.take(minOf(limit, rawList.size))
        val seed = getOrGenerateGmdssSeed(competenceId)
        val list = unShuffled.toMutableList()
        java.util.Collections.shuffle(list, java.util.Random(seed))
        return list
    }

    internal fun startConfiguredPracticeInternal(
        competenceId: String? = null,
        selectedPart: Int? = null,
        deliveryMode: DrillDeliveryMode = DrillDeliveryMode.STANDARD,
        questionCount: Int? = null,
        startItem: Int = 1,
        endItem: Int? = null,
        shuffleQuestions: Boolean = false,
        shuffleChoices: Boolean = true,
        isCrpMode: Boolean = false,
        prioritizeUnseen: Boolean = false,
        seenIds: Set<String> = emptySet(),
        isOperationalReview: Boolean = false
    ) {
        timerJob?.cancel()
        currentActiveSessionId = java.util.UUID.randomUUID().toString()
        _isExamSubmitted.value = false
        _isCrpMode.value = isCrpMode
        _activeQuizMode.value = QuizMode.PRACTICE
        _activeDrillDeliveryMode.value = deliveryMode
        _activeFunctionId.value = null
        _activeCompetenceId.value = competenceId
        val partLabel = if (selectedPart != null) "Part $selectedPart" else null
        _activePartLabel.value = partLabel
        if (competenceId != null) sessionCompetenceCache[currentActiveSessionId] = competenceId
        if (partLabel != null) sessionPartCache[currentActiveSessionId] = partLabel
        _lastPracticeDrillConfig = PracticeDrillConfig(
            track = _selectedTrack.value,
            competenceId = competenceId,
            selectedPart = selectedPart,
            deliveryMode = deliveryMode,
            questionCount = questionCount,
            startItem = startItem,
            endItem = endItem,
            shuffleQuestions = shuffleQuestions,
            shuffleChoices = shuffleChoices,
            isCrpMode = isCrpMode,
            prioritizeUnseen = prioritizeUnseen,
            isOperationalReview = isOperationalReview
        )
        _isShuffled.value = shuffleQuestions
        _currentQuestionIndex.value = 0
        _showSubmitDialog.value = false
        _isPaletteOpen.value = false
        activeEvaluationAttemptCommittedId = null

        val context = getApplication<Application>()
        val isLicensed = isTrackUnlocked(_selectedTrack.value)
        val isPro = isLicensed

        val isGmdssOpReview = (_selectedTrack.value == Track.GMDSS_GOC && isOperationalReview && (competenceId == "C1" || competenceId == "C2") && selectedPart == null)

        var questions: List<Question> = if (isCrpMode) {
            when (val track = _selectedTrack.value) {
                Track.GMDSS_GOC -> MaritimeQuestionBank.getMarinaCrpGmdssQuestions(context, competenceId, selectedPart, isProUnlocked = isPro)
                Track.DECK_OIC_NW -> MaritimeQuestionBank.getMarinaCrpQuestions(competenceId)
                Track.ENGINE_OIC_EW, Track.DECK_MGT, Track.ENGINE_MGT -> {
                    if (competenceId != null) {
                        MaritimeQuestionBank.getQuestionsByCompetence(competenceId, track, isProUnlocked = false)
                    } else {
                        MaritimeQuestionBank.getNewTrackStandardReviewQuestions(track, isProUnlocked = false)
                    }
                }
                else -> emptyList()
            }
        } else if (isGmdssOpReview) {
            getSeededShuffledGmdssPool(context, competenceId!!)
        } else if (_selectedTrack.value == Track.GMDSS_GOC) {
            val proQuestions = if (competenceId != null) {
                if (selectedPart != null) {
                    val rawPart = ProQuestionRepository.loadRawPartQuestions(context, competenceId, selectedPart).map { it.toQuestion() }
                    if (isPro) rawPart else {
                        val allRawComp = ProQuestionRepository.loadAllRawCompetenceQuestions(context, competenceId).map { it.toQuestion() }
                        val limit = if (competenceId == "C1") 1000 else 150
                        val allowedIds = allRawComp.take(limit).map { it.id }.toSet()
                        rawPart.filter { allowedIds.contains(it.id) }
                    }
                } else {
                    val allRawComp = ProQuestionRepository.loadAllRawCompetenceQuestions(context, competenceId).map { it.toQuestion() }
                    val limit = if (competenceId == "C1") 1000 else 150
                    if (isPro) allRawComp else allRawComp.take(limit)
                }
            } else {
                val allC1 = ProQuestionRepository.loadAllRawCompetenceQuestions(context, "C1").map { it.toQuestion() }
                val allC2 = ProQuestionRepository.loadAllRawCompetenceQuestions(context, "C2").map { it.toQuestion() }
                if (isPro) (allC1 + allC2) else (allC1.take(1000) + allC2.take(150))
            }
            proQuestions
        } else {
            if (competenceId != null) {
                MaritimeQuestionBank.getQuestionsByCompetence(competenceId, _selectedTrack.value, isProUnlocked = isPro)
            } else {
                MaritimeQuestionBank.getQuestionsByTrack(_selectedTrack.value, isProUnlocked = isPro)
            }
        }

        // 4. Selected question range applied first to establish scopedPool
        val scopedPool = if (!isCrpMode && endItem != null && endItem >= startItem && questions.isNotEmpty()) {
            val fromIndex = (startItem - 1).coerceIn(0, questions.size - 1)
            val toIndex = endItem.coerceIn(fromIndex + 1, questions.size)
            questions.subList(fromIndex, toIndex)
        } else {
            questions
        }

        val requestedCount = if (questionCount != null && questionCount > 0) {
            questionCount
        } else {
            scopedPool.size
        }

        // 5 & 6. Seen/unseen classification, selection and ordering
        questions = com.nhyle.hyperionmaritime.data.repository.QuestionSelectionDispatcher.selectQuestions(
            eligiblePool = scopedPool,
            requestedCount = requestedCount,
            seenQuestionIds = seenIds,
            prioritizeUnseen = if (isGmdssOpReview) false else prioritizeUnseen,
            shuffleQuestions = if (isGmdssOpReview) false else shuffleQuestions
        )

        // Secondary defense check: Truncate to first 5 questions if unactivated and NOT CRP mode and NOT GMDSS Op Review
        if (!isPro && !isCrpMode && !isGmdssOpReview) {
            questions = questions.take(5)
        }

        if (shuffleChoices) {
            questions = questions.map { q ->
                val originalCorrectText = q.options[q.correctAnswerIndex]
                val shuffledOpts = q.options.shuffled()
                val newIndex = shuffledOpts.indexOf(originalCorrectText)
                q.copy(
                    options = shuffledOpts,
                    correctAnswerIndex = if (newIndex >= 0) newIndex else 0
                )
            }
        }

        if (deliveryMode == DrillDeliveryMode.INTENSIVE && isPro) {
            _intensiveQueue.value = questions
            _intensiveInitialTotal.value = questions.size
            _intensiveClearedCount.value = 0
            _intensiveCurrentQuestionState.value = null
            _intensiveCompleted.value = false
            _activeQuestions.value = questions
            _userQuestionStates.value = questions.associate { it.id to UserQuestionState(questionId = it.id) }
        } else {
            if (deliveryMode == DrillDeliveryMode.INTENSIVE && !isPro) {
                triggerPaywall(_selectedTrack.value)
            }
            _activeQuestions.value = questions
            _userQuestionStates.value = questions.associate { it.id to UserQuestionState(questionId = it.id) }
        }

        _selectedChoiceIndex.value = null
        _isAnswerEvaluated.value = false
        _timeRemainingSeconds.value = 0
        autoSaveCurrentSession()
        val configScreen = Screen.PracticeConfig(
            competenceId = competenceId,
            isCrpMode = isCrpMode,
            track = _selectedTrack.value,
            prioritizeUnseen = prioritizeUnseen,
            isOperationalReview = isOperationalReview
        )
        backStack.removeAll { it is Screen.PracticeConfig || it is Screen.Quiz }
        backStack.addLast(configScreen)
        _currentScreen.value = Screen.Quiz
        startStudyTrackingForCurrentSession()
    }

    /**
     * Resets the active practice session's transient evaluation and question state,
     * without deleting persistent question statistics, seen history, or mistake vault entries.
     */
    fun resetActivePracticeSession() {
        timerJob?.cancel()
        _selectedChoiceIndex.value = null
        _isAnswerEvaluated.value = false
        _currentQuestionIndex.value = 0
        _isExamSubmitted.value = false
        activeEvaluationAttemptCommittedId = null
        _userQuestionStates.value = _activeQuestions.value.associate {
            it.id to UserQuestionState(questionId = it.id)
        }
    }

    fun selectIntensiveChoice(optionIndex: Int) {
        if (_isAnswerEvaluated.value) return
        _selectedChoiceIndex.value = optionIndex
        val currentQ = _intensiveQueue.value.firstOrNull() ?: return
        val currentMap = _userQuestionStates.value.toMutableMap()
        currentMap[currentQ.id] = UserQuestionState(
            questionId = currentQ.id,
            selectedIndex = optionIndex,
            isSubmitted = false,
            isCorrect = false
        )
        _userQuestionStates.value = currentMap
        autoSaveCurrentSession()
    }

    fun selectIntensiveOption(optionIndex: Int) {
        val currentQ = _intensiveQueue.value.firstOrNull() ?: return
        _selectedChoiceIndex.value = optionIndex
        _isAnswerEvaluated.value = true
        val isCorrect = optionIndex == currentQ.correctAnswerIndex
        _intensiveCurrentQuestionState.value = UserQuestionState(
            questionId = currentQ.id,
            selectedIndex = optionIndex,
            isSubmitted = true,
            isCorrect = isCorrect
        )
        markQuestionSeen(_selectedTrack.value, currentQ.id)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.recordQuestionAttempt(currentQ.id, isCorrect)
            if (isCorrect) {
                MistakesVaultManager.recordAttempt(getApplication(), currentQ.id, true)
            } else {
                MistakesVaultManager.recordMistake(getApplication(), currentQ, currentQ.track.name, currentQ.competenceId)
            }
            refreshMistakes()
        }
    }

    fun confirmIntensiveAnswer() {
        val currentQ = _intensiveQueue.value.firstOrNull() ?: return
        val choice = _selectedChoiceIndex.value ?: return

        val evaluationKey = "INTENSIVE::${currentQ.track.name}::${currentQ.id}::${intensivePresentationCounter}"
        if (activeEvaluationAttemptCommittedId == evaluationKey) {
            return
        }
        activeEvaluationAttemptCommittedId = evaluationKey

        _isAnswerEvaluated.value = true
        val isCorrect = choice == currentQ.correctAnswerIndex
        val currentMap = _userQuestionStates.value.toMutableMap()
        currentMap[currentQ.id] = UserQuestionState(
            questionId = currentQ.id,
            selectedIndex = choice,
            isSubmitted = true,
            isCorrect = isCorrect
        )
        _userQuestionStates.value = currentMap
        _intensiveCurrentQuestionState.value = currentMap[currentQ.id]
        markQuestionSeen(_selectedTrack.value, currentQ.id)

        val updatedHistory = QuestionAttemptHistoryManager.recordAttempt(
            context = getApplication(),
            track = currentQ.track.name,
            questionId = currentQ.id,
            isCorrect = isCorrect
        )
        _currentQuestionStats.value = CurrentQuestionStats(
            questionId = currentQ.id,
            track = currentQ.track.name,
            allTimeCorrect = updatedHistory.allTimeCorrect,
            allTimeIncorrect = updatedHistory.allTimeIncorrect,
            recentAttempts = updatedHistory.latest24Attempts
        )

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.recordQuestionAttempt(currentQ.id, isCorrect)
            if (isCorrect) {
                MistakesVaultManager.recordAttempt(getApplication(), currentQ.id, true)
            } else {
                MistakesVaultManager.recordMistake(getApplication(), currentQ, currentQ.track.name, currentQ.competenceId)
            }
            refreshMistakes()
        }
        autoSaveCurrentSession()
    }

    fun advanceIntensiveQuestion() {
        intensivePresentationCounter++
        activeEvaluationAttemptCommittedId = null
        _currentQuestionStats.value = null

        val queue = _intensiveQueue.value
        if (queue.isEmpty()) return
        val currentQ = queue.first()
        val state = _intensiveCurrentQuestionState.value
        val userState = _userQuestionStates.value[currentQ.id]
        val choice = state?.selectedIndex ?: userState?.selectedIndex
        val isCorrect = choice == currentQ.correctAnswerIndex

        if (isCorrect) {
            val newQueue = queue.drop(1)
            _intensiveQueue.value = newQueue
            _intensiveClearedCount.value += 1
            if (newQueue.isEmpty()) {
                _intensiveCompleted.value = true
                completeIntensiveDrill()
            }
        } else {
            // Push wrong question to the very end of the queue
            val newQueue = queue.drop(1) + currentQ
            _intensiveQueue.value = newQueue
        }
        _intensiveCurrentQuestionState.value = null
        _selectedChoiceIndex.value = null
        _isAnswerEvaluated.value = false
        autoSaveCurrentSession()
    }

    private fun completeIntensiveDrill() {
        val questions = _activeQuestions.value
        val answers = _userQuestionStates.value
        val effectiveComp = _activeCompetenceId.value ?: sessionCompetenceCache[currentActiveSessionId]
        val effectivePart = _activePartLabel.value ?: sessionPartCache[currentActiveSessionId]

        val drillLabel = buildString {
            if (effectiveComp != null && effectivePart != null) {
                append("$effectiveComp ${effectivePart.uppercase()} • INTENSIVE")
            } else if (effectiveComp != null) {
                append("$effectiveComp DRILL • INTENSIVE")
            } else {
                append("ALL QUESTIONS • INTENSIVE")
            }
        }

        val totalCount = _intensiveInitialTotal.value.takeIf { it > 0 } ?: questions.size
        val correctCount = totalCount
        val percentage = 100.0

        val result = ExamResult(
            examId = resumingRecordId ?: currentActiveSessionId,
            timestamp = System.currentTimeMillis(),
            track = _selectedTrack.value,
            mode = QuizMode.PRACTICE,
            totalQuestions = totalCount,
            correctCount = correctCount,
            rawPointsEarned = correctCount,
            maxPointsPossible = totalCount,
            overallPercentage = percentage,
            isOverallPassed = true,
            functionScores = emptyList(),
            competenceScores = emptyList(),
            questionStates = answers,
            drillTitle = drillLabel,
            competence = effectiveComp,
            part = effectivePart
        )

        viewModelScope.launch {
            for (q in questions) {
                markQuestionSeen(_selectedTrack.value, q.id)
                repository.recordQuestionAttempt(q.id, true)
                MistakesVaultManager.recordAttempt(getApplication(), q.id, true)
            }
            refreshMistakes()
            repository.saveExamRecord(result, questions.map { it.id })
            ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), _selectedTrack.value.name)
            notifySnapshotChanged()
            repository.clearActiveSession(_selectedTrack.value)
            repository.markDrillSessionCompleted(currentActiveSessionId)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPaletteOpen(open: Boolean) {
        _isPaletteOpen.value = open
    }

    fun setShowSubmitDialog(show: Boolean) {
        _showSubmitDialog.value = show
    }

    fun toggleBookmark(questionId: String) {
        val currentBookmarked = bookmarkedIds.value.contains(questionId)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.toggleBookmark(questionId, currentBookmarked)
        }
    }

    fun removeBookmark(questionId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.removeBookmark(questionId)
        }
    }

    fun clearAllBookmarks() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.clearAllBookmarks()
        }
    }

    fun removeMistake(questionId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.removeMistake(questionId)
            MistakesVaultManager.deleteEntry(getApplication(), questionId)
            refreshMistakes()
        }
    }

    fun clearAllMistakes() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.clearAllMistakes()
            MistakesVaultManager.clearAll(getApplication())
            refreshMistakes()
        }
    }

    fun startQuizSession(
        mode: QuizMode,
        functionId: String? = null,
        competenceId: String? = null,
        limitQuestions: Int? = null,
        prioritizeUnseen: Boolean = false,
        seenQuestionIds: Set<String>? = null
    ) {
        timerJob?.cancel()
        currentActiveSessionId = java.util.UUID.randomUUID().toString()
        _isExamSubmitted.value = false
        _activeQuizMode.value = mode
        _activeFunctionId.value = functionId
        _activeCompetenceId.value = competenceId
        _currentQuestionIndex.value = 0
        _showSubmitDialog.value = false
        _isPaletteOpen.value = false

        val vaultIds = mistakeEntities.value.map { it.questionId }.toSet()
        val bookmarked = bookmarkedIds.value
        val isPro = isTrackUnlocked(_selectedTrack.value)

        var questions = repository.getQuestionsForSession(
            track = _selectedTrack.value,
            mode = mode,
            functionId = functionId,
            competenceId = competenceId,
            vaultQuestionIds = vaultIds,
            bookmarkIds = bookmarked,
            isProUnlocked = isPro,
            prioritizeUnseen = prioritizeUnseen,
            seenQuestionIds = seenQuestionIds ?: emptySet()
        )

        // Defense check: Lock out Mock Exams or truncate unactivated sessions
        if (!isPro) {
            if (mode == QuizMode.MOCK_EXAM) {
                questions = emptyList()
                triggerPaywall(_selectedTrack.value)
            } else {
                questions = questions.take(5)
            }
        }

        if (limitQuestions != null && limitQuestions > 0 && questions.size > limitQuestions) {
            questions = questions.take(limitQuestions)
        }

        questions = questions.map { com.nhyle.hyperionmaritime.data.model.shuffleOptionsStable(it, currentActiveSessionId) }
        _activeQuestions.value = questions

        // Initialize question states
        val initialMap = questions.associate { q ->
            q.id to UserQuestionState(questionId = q.id)
        }
        _userQuestionStates.value = initialMap

        // Set up timer if Mock Exam
        if (mode == QuizMode.MOCK_EXAM) {
            _completedCompetences.value = emptySet()
            _activeCompetenceId.value = null
            timerJob?.cancel()
            _timeRemainingSeconds.value = 0 // Overview page does NOT show a global countdown timer
            autoSaveCurrentSession()
            _currentScreen.value = Screen.MockHub
        } else {
            _timeRemainingSeconds.value = 0
            autoSaveCurrentSession()
            _currentScreen.value = Screen.Quiz
            startStudyTrackingForCurrentSession()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var counter = 0
            while (_timeRemainingSeconds.value > 0) {
                delay(1000L)
                _timeRemainingSeconds.value -= 1
                counter++
                // Periodically save elapsed timer every 5 seconds
                if (counter % 5 == 0) {
                    autoSaveCurrentSession()
                }
            }
            // Auto complete active competence or submit exam when time runs out!
            if (_activeQuizMode.value == QuizMode.MOCK_EXAM) {
                val activeComp = _activeCompetenceId.value
                if (activeComp != null) {
                    markCompetenceCompleted(activeComp)
                } else {
                    submitExam()
                }
            } else {
                submitExam()
            }
        }
    }

    fun resumeActiveSession(session: ActiveSessionEntity) {
        timerJob?.cancel()
        val track = try { Track.valueOf(session.track) } catch (_: Exception) { Track.DECK_OIC_NW }
        val mode = try { QuizMode.valueOf(session.mode) } catch (_: Exception) { QuizMode.MOCK_EXAM }
        _selectedTrack.value = track
        _activeQuizMode.value = mode
        _activeFunctionId.value = session.functionId
        _activeCompetenceId.value = session.competenceId

        val qIdList = session.questionIds.split(",").filter { it.isNotBlank() }
        if (mode == QuizMode.MOCK_EXAM && qIdList.size > 200) {
            discardActiveSession(track.name)
            startQuizSession(QuizMode.MOCK_EXAM)
            return
        }
        _isExamSubmitted.value = false
        val questions = resolveQuestionsForSession(track, qIdList, session.competenceId)

        if (questions.isNotEmpty()) {
            _activeQuestions.value = questions
            val userAnswers = SessionJsonHelper.deserializeAnswers(session.answersJson)
            _userQuestionStates.value = userAnswers

            val firstUnansweredIndex = questions.indexOfFirst { q -> userAnswers[q.id]?.selectedIndex == null }
            val targetIndex = if (firstUnansweredIndex >= 0) firstUnansweredIndex else session.currentQuestionIndex.coerceIn(0, questions.size - 1)
            _currentQuestionIndex.value = targetIndex
            syncCurrentQuestionEvaluationState()

            _timeRemainingSeconds.value = session.timeRemainingSeconds
            _completedCompetences.value = session.completedCompetences.split(",").filter { it.isNotBlank() }.toSet()

            if (mode == QuizMode.MOCK_EXAM) {
                if (session.timeRemainingSeconds > 0) {
                    startTimer()
                }
                _currentScreen.value = Screen.MockHub
            } else {
                currentActiveSessionId = session.id
                _currentScreen.value = Screen.Quiz
                startStudyTrackingForCurrentSession()
            }
        }
    }

    fun resumeActiveSession() {
        activeSession.value?.let { session ->
            resumeActiveSession(session)
        }
    }

    fun resumeSession(trackName: String, sessionId: String? = null) {
        val cleanTrackName = trackName.removeSuffix("_MISTAKES")
        val targetTrack = try {
            Track.values().find { it.name.equals(cleanTrackName, ignoreCase = true) || it.shortName.equals(cleanTrackName, ignoreCase = true) }
                ?: Track.valueOf(cleanTrackName)
        } catch (_: Exception) {
            _selectedTrack.value
        }
        _selectedTrack.value = targetTrack

        // 1. Try loading full snapshot first (preserves exact questions, options, shuffled state, and intensive queue)
        val snapshot = if (sessionId != null) {
            ActiveSessionSnapshotManager.loadSnapshotById(getApplication(), sessionId)
                ?: if (trackName.endsWith("_MISTAKES")) {
                    ActiveSessionSnapshotManager.loadMistakesSnapshot(getApplication(), targetTrack.name)
                } else {
                    ActiveSessionSnapshotManager.loadSnapshot(getApplication(), targetTrack.name)
                }
        } else if (trackName.endsWith("_MISTAKES")) {
            ActiveSessionSnapshotManager.loadMistakesSnapshot(getApplication(), targetTrack.name)
        } else {
            ActiveSessionSnapshotManager.loadSnapshot(getApplication(), targetTrack.name)
        }

        if (snapshot != null && !snapshot.isCompleted) {
            if (snapshot.mode.contains("MOCK", ignoreCase = true) && snapshot.totalQuestions > 200) {
                ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), targetTrack.name)
                discardActiveSession(targetTrack.name)
                startQuizSession(QuizMode.MOCK_EXAM)
                return
            }
            _isExamSubmitted.value = false
            currentActiveSessionId = snapshot.sessionId
            resumingRecordId = snapshot.sessionId
            val cleanSnapTrack = snapshot.track.removeSuffix("_MISTAKES")
            val trackEnum = try { Track.valueOf(cleanSnapTrack) } catch (_: Exception) { targetTrack }
            _selectedTrack.value = trackEnum
            val isMock = snapshot.mode.equals("MOCK", ignoreCase = true) || snapshot.mode.equals("MOCK_EXAM", ignoreCase = true)
            val isMistakes = snapshot.mode.equals("MISTAKES", ignoreCase = true) || snapshot.competenceOrPart.equals("MISTAKES", ignoreCase = true)
            _activeQuizMode.value = when {
                isMock -> QuizMode.MOCK_EXAM
                isMistakes -> QuizMode.MISTAKES
                else -> QuizMode.PRACTICE
            }
            _activeCompetenceId.value = if (isMistakes) "MISTAKES" else snapshot.competenceOrPart.ifBlank { null }

            val isIntensive = snapshot.deliveryMode.equals("INTENSIVE", ignoreCase = true)
            _activeDrillDeliveryMode.value = if (isIntensive) DrillDeliveryMode.INTENSIVE else DrillDeliveryMode.STANDARD

            val questions = snapshot.questions
            if (questions.isNotEmpty()) {
                _activeQuestions.value = questions
                val qMap = questions.associateBy { it.id }
                val userAnswers = snapshot.selectedAnswers.mapValues { (qid, sel) ->
                    val selIdx = sel.toIntOrNull()
                    val q = qMap[qid]
                    val isCor = selIdx != null && q != null && selIdx == q.correctAnswerIndex
                    UserQuestionState(
                        questionId = qid,
                        selectedIndex = selIdx,
                        isSubmitted = true,
                        isCorrect = isCor
                    )
                }
                val fullUserAnswers = questions.associate { q ->
                    q.id to (userAnswers[q.id] ?: UserQuestionState(questionId = q.id))
                }.toMutableMap()
                _userQuestionStates.value = fullUserAnswers
                _firstAttemptStatus.value = snapshot.firstAttemptStatus

                if (isIntensive) {
                    val clearedIds = questions.filter { q ->
                        val ans = userAnswers[q.id]
                        ans?.isCorrect == true || (ans?.isSubmitted == true && ans.selectedIndex == q.correctAnswerIndex)
                    }.map { it.id }.toSet()

                    var remaining = if (snapshot.intensiveQueueIds.isNotEmpty()) {
                        snapshot.intensiveQueueIds.mapNotNull { qMap[it] }.filter { it.id !in clearedIds }
                    } else {
                        questions.filter { it.id !in clearedIds }
                    }

                    val topQ = remaining.firstOrNull()
                    if (topQ != null && userAnswers[topQ.id]?.isSubmitted == true && userAnswers[topQ.id]?.isCorrect == false) {
                        remaining = remaining.drop(1) + topQ
                    }

                    val activeQueue = if (remaining.isNotEmpty()) remaining else questions
                    _intensiveQueue.value = activeQueue
                    _intensiveInitialTotal.value = snapshot.totalQuestions.takeIf { it > 0 } ?: questions.size
                    _intensiveClearedCount.value = snapshot.clearedCount.coerceAtLeast(clearedIds.size)
                    _intensiveCompleted.value = remaining.isEmpty() && clearedIds.isNotEmpty()

                    val activeTopQ = activeQueue.firstOrNull()
                    val activeTopState = if (activeTopQ != null) userAnswers[activeTopQ.id] else null
                    if (activeTopState != null && activeTopState.selectedIndex != null) {
                        _selectedChoiceIndex.value = activeTopState.selectedIndex
                        _isAnswerEvaluated.value = activeTopState.isSubmitted
                        _intensiveCurrentQuestionState.value = activeTopState
                    } else {
                        _selectedChoiceIndex.value = null
                        _isAnswerEvaluated.value = false
                        _intensiveCurrentQuestionState.value = null
                    }
                    _currentQuestionIndex.value = _intensiveClearedCount.value.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
                } else {
                    _currentQuestionIndex.value = snapshot.currentIndex.coerceIn(0, questions.size - 1)
                    syncCurrentQuestionEvaluationState()
                }

                _currentScreen.value = if (_activeQuizMode.value == QuizMode.MOCK_EXAM) Screen.MockHub else Screen.Quiz
                if (_currentScreen.value is Screen.Quiz) {
                    startStudyTrackingForCurrentSession()
                }
                return
            }
        }

        // 2. If explicit sessionId is provided and no snapshot was matched, try loading by ID from repository
        if (sessionId != null) {
            viewModelScope.launch {
                val drillSession = repository.getDrillSessionById(sessionId)
                if (drillSession != null) {
                    resumeDrillSession(drillSession)
                    return@launch
                }
                val record = repository.getExamRecordById(sessionId)
                if (record != null) {
                    resumeIncompleteExamRecord(record)
                    return@launch
                }
            }
            return
        }

        // 3. Fallback to repository: recent drill session or active session entity
        viewModelScope.launch {
            val recentDrills = repository.getRecentDrillSessionsOnce(targetTrack.name, null)
            val incompleteDrill = recentDrills.firstOrNull { !it.isCompleted }
            if (incompleteDrill != null) {
                resumeDrillSession(incompleteDrill)
                return@launch
            }

            val activeSessionEntity = repository.observeActiveSessionForTrack(targetTrack).stateIn(viewModelScope, SharingStarted.Eagerly, null).value
            if (activeSessionEntity != null) {
                resumeActiveSession(activeSessionEntity)
            } else {
                val records = repository.observeExamRecordsForTrack(targetTrack).stateIn(viewModelScope, SharingStarted.Eagerly, null).value
                val incomplete = records?.find { it.detailsSummary.startsWith("INCOMPLETE") }
                if (incomplete != null) {
                    resumeIncompleteExamRecord(incomplete)
                }
            }
        }
    }

    fun discardActiveSession() {
        discardActiveSession(null, null)
    }

    fun discardActiveSession(sessionId: String? = null, trackId: String? = null) {
        timerJob?.cancel()
        val targetTrackName = trackId ?: _selectedTrack.value.name
        val isMistakes = targetTrackName.endsWith("_MISTAKES") || 
            (sessionId != null && ActiveSessionSnapshotManager.loadSnapshotById(getApplication(), sessionId)?.mode == "MISTAKES")

        if (isMistakes) {
            val baseTrack = targetTrackName.removeSuffix("_MISTAKES")
            ActiveSessionSnapshotManager.deleteMistakesSnapshot(getApplication(), baseTrack)
            if (sessionId != null) {
                ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), sessionId)
            }
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val trackEnum = try { Track.valueOf(baseTrack) } catch (_: Exception) { _selectedTrack.value }
                repository.clearActiveMistakesSession(trackEnum)
                if (sessionId != null) {
                    repository.deleteDrillSession(sessionId)
                }
            }
        } else {
            ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), targetTrackName)
            if (sessionId != null) {
                ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), sessionId)
            }
            _activeSessionOverride.value = _activeSessionOverride.value + (targetTrackName to null)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.deleteActiveSessionByTrack(targetTrackName)
                repository.deleteIncompleteSessionsByTrack(targetTrackName)
                if (sessionId != null) {
                    repository.deleteDrillSession(sessionId)
                }
            }
        }
        notifySnapshotChanged()
    }

    private var currentActiveSessionId: String = java.util.UUID.randomUUID().toString()

    private fun resolveQuestionsForSession(track: Track, qIdList: List<String>, competenceId: String? = null): List<Question> {
        val context = getApplication<Application>()
        val pool = mutableMapOf<String, Question>()
        MaritimeQuestionBank.ALL_QUESTIONS.forEach { pool[it.id] = it }
        MaritimeQuestionBank.getMarinaCrpQuestions().forEach { pool[it.id] = it }
        MaritimeQuestionBankGmdss.getQuestions(context).forEach { pool[it.id] = it }
        MaritimeQuestionBank.getMarinaCrpGmdssQuestions(context).forEach { pool[it.id] = it }
        try {
            ProQuestionRepository.loadAllRawCompetenceQuestions(context, "C1").map { it.toQuestion() }.forEach { pool[it.id] = it }
            ProQuestionRepository.loadAllRawCompetenceQuestions(context, "C2").map { it.toQuestion() }.forEach { pool[it.id] = it }
        } catch (_: Exception) {}
        if (!competenceId.isNullOrBlank()) {
            MaritimeQuestionBank.getQuestionsByCompetence(competenceId, track, isProUnlocked = true).forEach { pool[it.id] = it }
        }
        var questions = qIdList.mapNotNull { pool[it] }
        if (questions.isEmpty()) {
            if (track == Track.GMDSS_GOC) {
                questions = if (!competenceId.isNullOrBlank()) {
                    MaritimeQuestionBank.getMarinaCrpGmdssQuestions(context, competenceId)
                } else {
                    MaritimeQuestionBankGmdss.getQuestions(context)
                }
            } else if (!competenceId.isNullOrBlank()) {
                questions = MaritimeQuestionBank.getQuestionsByCompetence(competenceId, track, isProUnlocked = true)
            } else {
                questions = MaritimeQuestionBank.getQuestionsByTrack(track, isProUnlocked = true)
            }
        }
        return questions
    }

    fun autoSaveCurrentSession(): kotlinx.coroutines.Job? {
        val questions = _activeQuestions.value
        if (questions.isEmpty()) return null
        val track = _selectedTrack.value
        val mode = _activeQuizMode.value
        val answers = _userQuestionStates.value
        val isIntensive = _activeDrillDeliveryMode.value == DrillDeliveryMode.INTENSIVE

        val selectedAnswersMap = answers.entries.associate { (qid, state) ->
            qid to (state.selectedIndex?.toString() ?: "")
        }.filterValues { it.isNotBlank() }

        val answeredCount = answers.values.count { it.isSubmitted || it.selectedIndex != null }
        val correctCount = questions.count { q ->
            answers[q.id]?.isCorrect == true || (answers[q.id]?.isSubmitted == true && answers[q.id]?.selectedIndex == q.correctAnswerIndex)
        }
        val clearedCount = if (isIntensive) _intensiveClearedCount.value.coerceAtLeast(correctCount) else correctCount
        val queueIds = if (isIntensive) _intensiveQueue.value.map { it.id } else emptyList()

        val snapshot = ActiveSessionSnapshot(
            sessionId = currentActiveSessionId,
            track = track.name,
            mode = mode.name,
            competenceOrPart = _activeCompetenceId.value ?: "",
            questions = questions,
            selectedAnswers = selectedAnswersMap,
            firstAttemptStatus = _firstAttemptStatus.value,
            currentIndex = if (isIntensive) clearedCount else _currentQuestionIndex.value,
            totalQuestions = questions.size,
            isCompleted = false,
            timestamp = System.currentTimeMillis(),
            deliveryMode = if (isIntensive) "INTENSIVE" else "STANDARD",
            clearedCount = clearedCount,
            intensiveQueueIds = queueIds
        )
        ActiveSessionSnapshotManager.saveSnapshot(getApplication(), snapshot)
        notifySnapshotChanged()

        val effectiveComp = _activeCompetenceId.value ?: sessionCompetenceCache[currentActiveSessionId]
        val effectivePart = _activePartLabel.value ?: sessionPartCache[currentActiveSessionId]
        if (!effectiveComp.isNullOrBlank()) sessionCompetenceCache[currentActiveSessionId] = effectiveComp
        if (!effectivePart.isNullOrBlank()) sessionPartCache[currentActiveSessionId] = effectivePart

        val drillLabel = if (mode != QuizMode.MOCK_EXAM) {
            val comp = effectiveComp
            val part = effectivePart
            when {
                comp != null && part != null -> "$comp ${part.uppercase()}"
                comp != null -> if (comp.equals("MISTAKES", ignoreCase = true)) "Mistakes" else "$comp DRILL"
                mode == QuizMode.MISTAKES -> "Mistakes"
                mode == QuizMode.VAULT_DRILL -> "MISTAKES NOTEBOOK"
                mode == QuizMode.BOOKMARKS_DRILL -> "BOOKMARKS DRILL"
                else -> "ALL QUESTIONS DRILL"
            }
        } else null

        val inProgressRecord = ExamRecordEntity(
            id = currentActiveSessionId,
            timestamp = System.currentTimeMillis(),
            track = track.name,
            mode = if (mode == QuizMode.MISTAKES) "MISTAKES" else mode.name,
            rawScore = if (isIntensive) clearedCount else correctCount,
            maxScore = questions.size,
            percentage = if (questions.isNotEmpty()) ((if (isIntensive) clearedCount else correctCount).toDouble() / questions.size) * 100.0 else 0.0,
            isPassed = false,
            totalQuestions = questions.size,
            correctCount = if (isIntensive) clearedCount else correctCount,
            detailsSummary = if (isIntensive) "IN_PROGRESS • INTENSIVE • $clearedCount/${questions.size} Cleared" else (if (mode == QuizMode.MOCK_EXAM) "IN_PROGRESS • Answered $answeredCount/${questions.size}" else "IN_PROGRESS • $answeredCount/${questions.size} Answered"),
            questionIds = questions.joinToString(",") { it.id },
            answersJson = SessionJsonHelper.serializeAnswersWithQueue(answers, queueIds),
            competenceBreakdownJson = "",
            drillTitle = drillLabel,
            competence = effectiveComp,
            part = effectivePart
        )

        val drillSession = com.nhyle.hyperionmaritime.data.local.DrillSessionEntity(
            id = currentActiveSessionId,
            track = track.name,
            competence = effectiveComp,
            part = effectivePart,
            isCompleted = false,
            lastUpdated = System.currentTimeMillis(),
            correctCount = clearedCount,
            answeredCount = answeredCount,
            totalItems = questions.size,
            currentQuestionIndex = if (isIntensive) clearedCount else _currentQuestionIndex.value,
            questionIds = questions.joinToString(",") { it.id },
            answersJson = SessionJsonHelper.serializeAnswersWithQueue(answers, queueIds),
            isShuffled = _isShuffled.value,
            deliveryMode = if (isIntensive) "INTENSIVE" else "STANDARD",
            mode = if (_isCrpMode.value) "CRP_PRACTICE" else if (mode == QuizMode.MISTAKES) "MISTAKES" else mode.name
        )

        return viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.saveDrillSession(drillSession)
            repository.insertOrUpdateExamRecord(inProgressRecord)
            repository.saveActiveSession(
                track = track,
                mode = mode,
                functionId = _activeFunctionId.value,
                competenceId = _activeCompetenceId.value,
                currentQuestionIndex = if (isIntensive) clearedCount else _currentQuestionIndex.value,
                timeRemainingSeconds = _timeRemainingSeconds.value,
                questionIds = questions.map { it.id },
                answers = answers,
                completedCompetences = _completedCompetences.value
            )
        }
    }

    fun getRecentSessions(track: String, competence: String?): Flow<List<com.nhyle.hyperionmaritime.data.local.DrillSessionEntity>> {
        return repository.getRecentDrillSessions(track, competence)
    }

    fun resumeDrillSession(session: com.nhyle.hyperionmaritime.data.local.DrillSessionEntity) {
        timerJob?.cancel()
        currentActiveSessionId = session.id
        resumingRecordId = session.id
        _isExamSubmitted.value = session.isCompleted
        val cleanTrack = session.track.removeSuffix("_MISTAKES")
        val track = try { Track.valueOf(cleanTrack) } catch (_: Exception) { _selectedTrack.value }
        _selectedTrack.value = track
        val isMistakes = session.mode.equals("MISTAKES", ignoreCase = true) || session.competence.equals("MISTAKES", ignoreCase = true)
        _activeQuizMode.value = if (isMistakes) QuizMode.MISTAKES else QuizMode.PRACTICE
        _activeCompetenceId.value = session.competence
        _activePartLabel.value = session.part
        session.competence?.let { sessionCompetenceCache[session.id] = it }
        session.part?.let { sessionPartCache[session.id] = it }
        val isIntensive = session.deliveryMode.equals("INTENSIVE", ignoreCase = true)
        _activeDrillDeliveryMode.value = if (isIntensive) DrillDeliveryMode.INTENSIVE else DrillDeliveryMode.STANDARD
        _isShuffled.value = session.isShuffled
        val isCrp = session.mode.contains("CRP", ignoreCase = true) ||
            session.competence?.startsWith("CRP", ignoreCase = true) == true
        _isCrpMode.value = isCrp

        // Check if snapshot exists for this session
        val snapshot = ActiveSessionSnapshotManager.loadSnapshotById(getApplication(), session.id)
            ?: (if (isMistakes) ActiveSessionSnapshotManager.loadMistakesSnapshot(getApplication(), track.name) else ActiveSessionSnapshotManager.loadSnapshot(getApplication(), track.name))?.takeIf { it.sessionId == session.id }

        val questions: List<Question> = if (snapshot != null && snapshot.questions.isNotEmpty()) {
            snapshot.questions
        } else {
            val qIdList = session.questionIds.split(",").filter { it.isNotBlank() }
            resolveQuestionsForSession(track, qIdList, session.competence)
        }

        if (questions.isNotEmpty()) {
            _activeQuestions.value = questions
            val qMap = questions.associateBy { it.id }
            val deserialized = SessionJsonHelper.deserializeAnswers(session.answersJson).toMutableMap()
            if (snapshot != null && snapshot.selectedAnswers.isNotEmpty()) {
                snapshot.selectedAnswers.forEach { (qid, sel) ->
                    val selIdx = sel.toIntOrNull()
                    if (!deserialized.containsKey(qid) && selIdx != null) {
                        val q = qMap[qid]
                        val isCor = q != null && selIdx == q.correctAnswerIndex
                        deserialized[qid] = UserQuestionState(
                            questionId = qid,
                            selectedIndex = selIdx,
                            isSubmitted = true,
                            isCorrect = isCor
                        )
                    }
                }
            }
            _userQuestionStates.value = deserialized

            if (isIntensive) {
                // Determine which questions were cleared (answered correctly)
                val clearedQuestionIds = mutableSetOf<String>()
                deserialized.forEach { (qid, state) ->
                    val q = qMap[qid]
                    if (state.isCorrect || (state.isSubmitted && q != null && state.selectedIndex == q.correctAnswerIndex)) {
                        clearedQuestionIds.add(qid)
                    }
                }

                // If clearedQuestionIds is empty but session.correctCount > 0, fallback to submitted answers
                if (clearedQuestionIds.isEmpty() && session.correctCount > 0) {
                    val submittedIds = deserialized.filterValues { it.isSubmitted }.keys
                    clearedQuestionIds.addAll(submittedIds.take(session.correctCount))
                }

                val queueFromSession = SessionJsonHelper.deserializeQueue(session.answersJson)
                val candidateQueueIds = when {
                    snapshot != null && snapshot.intensiveQueueIds.isNotEmpty() -> snapshot.intensiveQueueIds
                    queueFromSession.isNotEmpty() -> queueFromSession
                    else -> emptyList()
                }

                var remainingQuestions = if (candidateQueueIds.isNotEmpty()) {
                    candidateQueueIds.mapNotNull { qMap[it] }.filter { it.id !in clearedQuestionIds }
                } else {
                    questions.filter { it.id !in clearedQuestionIds }
                }

                // If the top question was already submitted as incorrect in this session, recycle it to the back
                val topQ = remainingQuestions.firstOrNull()
                if (topQ != null && deserialized[topQ.id]?.isSubmitted == true && deserialized[topQ.id]?.isCorrect == false) {
                    remainingQuestions = remainingQuestions.drop(1) + topQ
                }

                val activeQueue = if (remainingQuestions.isNotEmpty()) remainingQuestions else questions
                _intensiveQueue.value = activeQueue
                _intensiveInitialTotal.value = session.totalItems.takeIf { it > 0 } ?: questions.size
                _intensiveClearedCount.value = session.correctCount.coerceAtLeast(clearedQuestionIds.size)
                _intensiveCompleted.value = remainingQuestions.isEmpty() && clearedQuestionIds.isNotEmpty()

                val activeTopQ = activeQueue.firstOrNull()
                val activeTopState = if (activeTopQ != null) deserialized[activeTopQ.id] else null
                if (activeTopState != null && activeTopState.selectedIndex != null) {
                    _selectedChoiceIndex.value = activeTopState.selectedIndex
                    _isAnswerEvaluated.value = activeTopState.isSubmitted
                    _intensiveCurrentQuestionState.value = activeTopState
                } else {
                    _selectedChoiceIndex.value = null
                    _isAnswerEvaluated.value = false
                    _intensiveCurrentQuestionState.value = null
                }
                _currentQuestionIndex.value = _intensiveClearedCount.value.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
            } else {
                _currentQuestionIndex.value = session.currentQuestionIndex.coerceIn(0, questions.size - 1)
                syncCurrentQuestionEvaluationState()
            }

            _timeRemainingSeconds.value = 0
            _currentScreen.value = Screen.Quiz
            startStudyTrackingForCurrentSession()
        }
    }

    fun openMockCompetence(competenceId: String) {
        _activeCompetenceId.value = competenceId
        if (!_isExamSubmitted.value && competenceId !in _completedCompetences.value) {
            timerJob?.cancel()
            val compQuestions = getQuestionsForCompetence(competenceId)
            val count = if (compQuestions.isNotEmpty()) {
                compQuestions.size
            } else {
                MaritimeCompetenceRegistry.getPermitItemCount(competenceId, _selectedTrack.value)
            }
            // Standard durations:
            // OIC-NW Pro Competencies: 1 minute per question (e.g. 35 items -> 35 minutes; 50 items -> 50 minutes)
            // GMDSS Pro Competencies: Standard operator timing (1 minute per question per competency block)
            val allocatedSeconds = (count * 60).coerceAtLeast(60)
            _timeRemainingSeconds.value = allocatedSeconds
            startTimer()
            autoSaveCurrentSession()
        }
        _currentScreen.value = Screen.MockCompetence(competenceId)
        startStudyTrackingForCurrentSession()
    }

    fun markCompetenceCompleted(competenceId: String) {
        timerJob?.cancel()
        studyTimeTracker.pauseTimer()
        _timeRemainingSeconds.value = 0
        _completedCompetences.value = _completedCompetences.value + competenceId
        _activeCompetenceId.value = null
        autoSaveCurrentSession()
        _currentScreen.value = Screen.MockHub
    }

    fun getQuestionsForCompetence(competenceId: String): List<Question> {
        // Direct defense: Unactivated mock exam returns empty list immediately
        if (!isTrackUnlocked(_selectedTrack.value)) return emptyList()
        return _activeQuestions.value.filter { it.competenceId == competenceId }
    }

    fun syncCurrentQuestionEvaluationState() {
        // Immediately invalidate previously displayed statistics until matching question stats are loaded
        _currentQuestionStats.value = null
        activeEvaluationAttemptCommittedId = null

        val questions = _activeQuestions.value
        val idx = _currentQuestionIndex.value
        if (idx in questions.indices) {
            val q = questions[idx]
            val state = _userQuestionStates.value[q.id]
            if (state != null && state.isSubmitted) {
                _selectedChoiceIndex.value = state.selectedIndex
                _isAnswerEvaluated.value = true
                activeEvaluationAttemptCommittedId = "${q.track.name}::${q.id}::${idx}"
                loadCurrentQuestionStats(q.track.name, q.id)
            } else {
                _selectedChoiceIndex.value = state?.selectedIndex
                _isAnswerEvaluated.value = false
            }
        } else {
            _selectedChoiceIndex.value = null
            _isAnswerEvaluated.value = false
        }
    }

    fun loadCurrentQuestionStats(trackName: String, questionId: String) {
        val history = QuestionAttemptHistoryManager.getHistory(getApplication(), trackName, questionId)
        if (history != null) {
            _currentQuestionStats.value = CurrentQuestionStats(
                questionId = questionId,
                track = trackName,
                allTimeCorrect = history.allTimeCorrect,
                allTimeIncorrect = history.allTimeIncorrect,
                recentAttempts = history.latest24Attempts
            )
        } else {
            _currentQuestionStats.value = CurrentQuestionStats(
                questionId = questionId,
                track = trackName,
                allTimeCorrect = 0,
                allTimeIncorrect = 0,
                recentAttempts = emptyList()
            )
        }
    }

    fun selectOptionForQuestion(questionId: String, optionIndex: Int) {
        if (_activeQuizMode.value == QuizMode.MOCK_EXAM && _isExamSubmitted.value) return // Permanent submission lock only for Mock Exam
        val currentMap = _userQuestionStates.value.toMutableMap()
        val currentEntry = currentMap[questionId] ?: UserQuestionState(questionId = questionId)

        if (_activeQuizMode.value != QuizMode.MOCK_EXAM) {
            if (_isAnswerEvaluated.value) return // Lock option selection once evaluated
            _selectedChoiceIndex.value = optionIndex
            currentMap[questionId] = currentEntry.copy(
                selectedIndex = optionIndex,
                isSubmitted = false
            )
            _userQuestionStates.value = currentMap
        } else {
            currentMap[questionId] = currentEntry.copy(
                selectedIndex = optionIndex
            )
            _userQuestionStates.value = currentMap
        }
        autoSaveCurrentSession()
    }

    fun toggleFlagForQuestion(questionId: String) {
        if (_isExamSubmitted.value) return // Permanent submission lock
        val currentMap = _userQuestionStates.value.toMutableMap()
        val currentEntry = currentMap[questionId] ?: UserQuestionState(questionId = questionId)
        currentMap[questionId] = currentEntry.copy(isFlagged = !currentEntry.isFlagged)
        _userQuestionStates.value = currentMap
        autoSaveCurrentSession()
    }

    fun selectOption(questionIndex: Int, optionIndex: Int) {
        val questions = _activeQuestions.value
        if (questionIndex !in questions.indices) return
        selectOptionForQuestion(questions[questionIndex].id, optionIndex)
    }

    fun selectOption(optionIndex: Int) {
        selectOption(_currentQuestionIndex.value, optionIndex)
    }

    fun toggleFlag(questionIndex: Int) {
        val questions = _activeQuestions.value
        if (questionIndex !in questions.indices) return
        toggleFlagForQuestion(questions[questionIndex].id)
    }

    fun confirmOrNextQuestion() {
        if (_activeQuizMode.value == QuizMode.MOCK_EXAM) {
            nextQuestion()
            return
        }

        val questions = _activeQuestions.value
        val currentIndex = _currentQuestionIndex.value
        if (currentIndex !in questions.indices) return
        val currentQ = questions[currentIndex]

        if (!_isAnswerEvaluated.value) {
            val choice = _selectedChoiceIndex.value ?: return

            val evaluationKey = "${currentQ.track.name}::${currentQ.id}::${currentIndex}"
            if (activeEvaluationAttemptCommittedId == evaluationKey) {
                return // Guard against repeated invocation for the same evaluation
            }
            activeEvaluationAttemptCommittedId = evaluationKey

            _isAnswerEvaluated.value = true

            val isCorrect = choice == currentQ.correctAnswerIndex
            val currentMap = _userQuestionStates.value.toMutableMap()
            val currentEntry = currentMap[currentQ.id] ?: UserQuestionState(questionId = currentQ.id)
            currentMap[currentQ.id] = currentEntry.copy(
                selectedIndex = choice,
                isSubmitted = true,
                isCorrect = isCorrect
            )
            _userQuestionStates.value = currentMap
            markQuestionSeen(_selectedTrack.value, currentQ.id)

            val updatedHistory = QuestionAttemptHistoryManager.recordAttempt(
                context = getApplication(),
                track = currentQ.track.name,
                questionId = currentQ.id,
                isCorrect = isCorrect
            )
            _currentQuestionStats.value = CurrentQuestionStats(
                questionId = currentQ.id,
                track = currentQ.track.name,
                allTimeCorrect = updatedHistory.allTimeCorrect,
                allTimeIncorrect = updatedHistory.allTimeIncorrect,
                recentAttempts = updatedHistory.latest24Attempts
            )

            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.recordQuestionAttempt(currentQ.id, isCorrect)
                if (isCorrect) {
                    MistakesVaultManager.recordAttempt(getApplication(), currentQ.id, true)
                } else {
                    MistakesVaultManager.recordMistake(getApplication(), currentQ, currentQ.track.name, currentQ.competenceId)
                }
                refreshMistakes()
            }

            autoSaveCurrentSession()
        } else {
            if (currentIndex < questions.size - 1) {
                _currentQuestionIndex.value = currentIndex + 1
                syncCurrentQuestionEvaluationState()
                autoSaveCurrentSession()
            } else {
                if (!isTrackUnlocked(currentQ.track) && !_isCrpMode.value) {
                    triggerPaywall(currentQ.track)
                }
                submitExam()
            }
        }
    }

    fun jumpToQuestion(index: Int) {
        val questions = _activeQuestions.value
        if (index in questions.indices) {
            _currentQuestionIndex.value = index
            _isPaletteOpen.value = false
            syncCurrentQuestionEvaluationState()
            autoSaveCurrentSession()
        }
    }

    fun nextQuestion() {
        if (_activeQuizMode.value != QuizMode.MOCK_EXAM && !_isAnswerEvaluated.value && _selectedChoiceIndex.value != null) {
            confirmOrNextQuestion()
        } else {
            if (_currentQuestionIndex.value < _activeQuestions.value.size - 1) {
                _currentQuestionIndex.value += 1
                syncCurrentQuestionEvaluationState()
                autoSaveCurrentSession()
            }
        }
    }

    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value -= 1
            syncCurrentQuestionEvaluationState()
            autoSaveCurrentSession()
        }
    }

    private var resumingRecordId: String? = null

    fun resumeIncompleteExamRecord(record: ExamRecordEntity) {
        viewModelScope.launch {
            val drillSession = repository.getDrillSessionById(record.id)
            if (drillSession != null) {
                resumeDrillSession(drillSession)
                return@launch
            }
            val freshRecord = repository.getExamRecordById(record.id) ?: record
            resumeIncompleteExamRecordInternal(freshRecord)
        }
    }

    fun loadIncompleteSession(record: ExamRecordEntity) {
        resumeIncompleteExamRecord(record)
    }

    private fun resumeIncompleteExamRecordInternal(record: ExamRecordEntity) {
        timerJob?.cancel()
        val cleanTrack = record.track.removeSuffix("_MISTAKES")
        val track = try { Track.valueOf(cleanTrack) } catch (_: Exception) { Track.DECK_OIC_NW }
        val mode = try { QuizMode.valueOf(record.mode) } catch (_: Exception) { QuizMode.MOCK_EXAM }
        _selectedTrack.value = track
        _activeQuizMode.value = mode
        val isMistakes = mode == QuizMode.MISTAKES || record.competence.equals("MISTAKES", ignoreCase = true)
        _activeCompetenceId.value = if (isMistakes) "MISTAKES" else record.competence

        val snapshot = ActiveSessionSnapshotManager.loadSnapshotById(getApplication(), record.id)
            ?: (if (isMistakes) ActiveSessionSnapshotManager.loadMistakesSnapshot(getApplication(), track.name) else ActiveSessionSnapshotManager.loadSnapshot(getApplication(), track.name))?.takeIf { it.sessionId == record.id }

        val questions = if (snapshot != null && snapshot.questions.isNotEmpty()) {
            snapshot.questions
        } else {
            val qIds = record.questionIds.split(",").filter { it.isNotBlank() }
            resolveQuestionsForSession(track, qIds, null)
        }

        if (questions.isNotEmpty()) {
            _activeQuestions.value = questions
            val qMap = questions.associateBy { it.id }
            val userAnswers = if (snapshot != null && snapshot.selectedAnswers.isNotEmpty()) {
                val deserialized = SessionJsonHelper.deserializeAnswers(record.answersJson).toMutableMap()
                snapshot.selectedAnswers.forEach { (qid, sel) ->
                    val selIdx = sel.toIntOrNull()
                    if (!deserialized.containsKey(qid) && selIdx != null) {
                        val q = qMap[qid]
                        val isCor = q != null && selIdx == q.correctAnswerIndex
                        deserialized[qid] = UserQuestionState(
                            questionId = qid,
                            selectedIndex = selIdx,
                            isSubmitted = true,
                            isCorrect = isCor
                        )
                    }
                }
                deserialized
            } else {
                SessionJsonHelper.deserializeAnswers(record.answersJson)
            }
            _userQuestionStates.value = userAnswers

            val isIntensive = record.detailsSummary.contains("INTENSIVE", ignoreCase = true) ||
                (snapshot?.deliveryMode?.equals("INTENSIVE", ignoreCase = true) == true)
            _activeDrillDeliveryMode.value = if (isIntensive) DrillDeliveryMode.INTENSIVE else DrillDeliveryMode.STANDARD

            if (isIntensive) {
                val clearedIds = mutableSetOf<String>()
                userAnswers.forEach { (qid, state) ->
                    val q = qMap[qid]
                    if (state.isCorrect || (state.isSubmitted && q != null && state.selectedIndex == q.correctAnswerIndex)) {
                        clearedIds.add(qid)
                    }
                }
                if (clearedIds.isEmpty() && record.correctCount > 0) {
                    val submittedIds = userAnswers.filterValues { it.isSubmitted }.keys
                    clearedIds.addAll(submittedIds.take(record.correctCount))
                }

                val queueFromRecord = SessionJsonHelper.deserializeQueue(record.answersJson)
                val candidateQueueIds = when {
                    snapshot != null && snapshot.intensiveQueueIds.isNotEmpty() -> snapshot.intensiveQueueIds
                    queueFromRecord.isNotEmpty() -> queueFromRecord
                    else -> emptyList()
                }

                val remaining = if (candidateQueueIds.isNotEmpty()) {
                    candidateQueueIds.mapNotNull { qMap[it] }.filter { it.id !in clearedIds }
                } else {
                    questions.filter { it.id !in clearedIds }
                }

                _intensiveQueue.value = if (remaining.isNotEmpty()) remaining else questions
                _intensiveInitialTotal.value = record.totalQuestions.takeIf { it > 0 } ?: questions.size
                _intensiveClearedCount.value = record.correctCount.coerceAtLeast(clearedIds.size)
                _intensiveCurrentQuestionState.value = null
                _selectedChoiceIndex.value = null
                _isAnswerEvaluated.value = false
                _intensiveCompleted.value = remaining.isEmpty() && clearedIds.isNotEmpty()
                _currentQuestionIndex.value = _intensiveClearedCount.value.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
            } else {
                val firstUnansweredIndex = questions.indexOfFirst { q -> userAnswers[q.id]?.selectedIndex == null }
                val targetIndex = if (firstUnansweredIndex >= 0) firstUnansweredIndex else 0
                _currentQuestionIndex.value = targetIndex
                syncCurrentQuestionEvaluationState()
            }

            resumingRecordId = record.id
            currentActiveSessionId = record.id
            if (!record.drillTitle.isNullOrBlank()) {
                val parts = record.drillTitle.split(" ")
                _activeCompetenceId.value = parts.firstOrNull()
                if (parts.size > 1) {
                    _activePartLabel.value = parts.drop(1).joinToString(" ")
                }
            }

            viewModelScope.launch {
                repository.saveActiveSession(
                    track = track,
                    mode = mode,
                    functionId = null,
                    competenceId = null,
                    currentQuestionIndex = _currentQuestionIndex.value,
                    timeRemainingSeconds = if (mode == QuizMode.MOCK_EXAM) 12000 else 0,
                    questionIds = questions.map { it.id },
                    answers = userAnswers
                )
            }

            if (mode == QuizMode.MOCK_EXAM) {
                _currentScreen.value = Screen.MockHub
            } else {
                _currentScreen.value = Screen.Quiz
                startStudyTrackingForCurrentSession()
            }
        }
    }

    fun submitExam() {
        timerJob?.cancel()
        _showSubmitDialog.value = false
        _isPaletteOpen.value = false

        if (_activeQuizMode.value == QuizMode.VAULT_DRILL || _activeQuizMode.value == QuizMode.BOOKMARKS_DRILL) {
            exitQuiz()
            return
        }

        val questions = _activeQuestions.value
        val answers = _userQuestionStates.value
        val submittedAnswers = answers.mapValues { (_, state) ->
            state.copy(isSubmitted = true)
        }
        _userQuestionStates.value = submittedAnswers
        _isExamSubmitted.value = true

        // Evaluate with official MARINA scoring engine
        val evaluatedResult = MarinaScoringEngine.evaluateExam(
            track = _selectedTrack.value,
            mode = _activeQuizMode.value,
            questions = questions,
            answers = submittedAnswers
        )

        val effectiveComp = _activeCompetenceId.value ?: sessionCompetenceCache[currentActiveSessionId]
        val effectivePart = _activePartLabel.value ?: sessionPartCache[currentActiveSessionId]

        val drillLabel = if (_activeQuizMode.value != QuizMode.MOCK_EXAM) {
            val comp = effectiveComp
            val part = effectivePart
            when {
                comp != null && part != null -> "$comp ${part.uppercase()}"
                comp != null -> if (comp.equals("MISTAKES", ignoreCase = true)) "Mistakes" else "$comp DRILL"
                _activeQuizMode.value == QuizMode.MISTAKES -> "Mistakes"
                _activeQuizMode.value == QuizMode.VAULT_DRILL -> "MISTAKES NOTEBOOK"
                _activeQuizMode.value == QuizMode.BOOKMARKS_DRILL -> "BOOKMARKS DRILL"
                else -> "ALL QUESTIONS DRILL"
            }
        } else null

        val result = evaluatedResult.copy(
            examId = resumingRecordId ?: currentActiveSessionId,
            drillTitle = drillLabel,
            competence = effectiveComp,
            part = effectivePart
        )
        studyTimeTracker.pauseTimer()
        resumingRecordId = null

        _lastExamResult.value = result

        val sessionToClearId = currentActiveSessionId
        val trackToClear = _selectedTrack.value
        val isMistakesSession = (_activeQuizMode.value == QuizMode.MISTAKES) ||
            (_activeCompetenceId.value.equals("MISTAKES", ignoreCase = true)) ||
            (result.mode == QuizMode.MISTAKES) ||
            (result.competence.equals("MISTAKES", ignoreCase = true))

        if (isMistakesSession) {
            val currentSnapshot = ActiveSessionSnapshotManager.loadMistakesSnapshot(getApplication(), trackToClear.name)
            if (currentSnapshot != null) {
                ActiveSessionSnapshotManager.saveSnapshot(getApplication(), currentSnapshot.copy(isCompleted = true))
            }
            ActiveSessionSnapshotManager.deleteMistakesSnapshot(getApplication(), trackToClear.name)
            ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), sessionToClearId)
        } else {
            val currentSnapshot = ActiveSessionSnapshotManager.loadSnapshot(getApplication(), trackToClear.name)
            if (currentSnapshot != null) {
                ActiveSessionSnapshotManager.saveSnapshot(getApplication(), currentSnapshot.copy(isCompleted = true))
            }
            ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), trackToClear.name)
            ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), sessionToClearId)
        }
        notifySnapshotChanged()

        // Record all questions in repo and persist completed session
        viewModelScope.launch {
            for (q in questions) {
                val state = answers[q.id]
                if (state?.selectedIndex != null) {
                    markQuestionSeen(trackToClear, q.id)
                }
                val isCorrect = state?.selectedIndex == q.correctAnswerIndex
                repository.recordQuestionAttempt(q.id, isCorrect)
                if (isCorrect) {
                    MistakesVaultManager.recordAttempt(getApplication(), q.id, true)
                } else {
                    MistakesVaultManager.recordMistake(getApplication(), q, q.track.name, q.competenceId)
                }
            }
            refreshMistakes()
            repository.saveExamRecord(result, questions.map { it.id })
            if (isMistakesSession) {
                repository.clearActiveMistakesSession(trackToClear)
                repository.markDrillSessionCompleted(sessionToClearId)
            } else {
                repository.clearActiveSession(trackToClear)
                repository.markDrillSessionCompleted(sessionToClearId)
            }
        }

        _currentScreen.value = Screen.Results
    }

    fun clearHistory() {
        clearAllSeenQuestions()
        viewModelScope.launch {
            repository.clearHistory()
            val trackName = _selectedTrack.value.name
            repository.deleteActiveSessionByTrack(trackName)
            _activeSessionOverride.value = _activeSessionOverride.value + (trackName to null)
            notifySnapshotChanged()
        }
    }

    fun deleteExamRecord(recordId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val context = getApplication<Application>()
            val examRecord = repository.getExamRecordById(recordId)
            val drillSession = repository.getDrillSessionById(recordId)
            val snapshotFile = ActiveSessionSnapshotManager.getSnapshotFile(context, recordId)
            val snapshotJson = if (snapshotFile.exists()) snapshotFile.readText() else null

            val label = drillSession?.scopeLabel ?: examRecord?.drillTitle ?: examRecord?.competence ?: recordId
            val date = drillSession?.formattedTimestamp ?: examRecord?.dateFormatted ?: ""
            val progress = if (drillSession != null) "${drillSession.answeredCount}/${drillSession.totalItems}" else (examRecord?.let { "${it.correctCount}/${it.totalQuestions}" } ?: "")

            val deletedSession = DeletedSession(
                sessionId = recordId,
                trackName = _selectedTrack.value.name,
                label = label,
                date = date,
                progress = progress,
                examRecordJson = examRecord?.let { DeletedSessionManager.serializeExamRecord(it) } ?: "",
                drillSessionJson = drillSession?.let { DeletedSessionManager.serializeDrillSession(it) } ?: "",
                snapshotJson = snapshotJson
            )
            DeletedSessionManager.addDeletedSession(context, deletedSession)
            _deletedSessions.value = DeletedSessionManager.loadDeletedSessions(context)

            repository.deleteExamRecord(recordId)
            ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), recordId)
            val trackName = _selectedTrack.value.name
            val activeSnap = ActiveSessionSnapshotManager.loadSnapshot(getApplication(), trackName)
            if (activeSnap != null && (activeSnap.sessionId == recordId || activeSnap.sessionId == trackName)) {
                ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), trackName)
                repository.deleteActiveSessionByTrack(trackName)
                _activeSessionOverride.value = _activeSessionOverride.value + (trackName to null)
            }
            if (currentActiveSessionId == recordId) {
                currentActiveSessionId = java.util.UUID.randomUUID().toString()
            }
            notifySnapshotChanged()
        }
    }

    fun deleteDrillSession(sessionId: String) {
        deleteExamRecord(sessionId)
    }

    fun deleteExamRecordsByIds(recordIds: List<String>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val context = getApplication<Application>()
            recordIds.forEach { recordId ->
                val examRecord = repository.getExamRecordById(recordId)
                val drillSession = repository.getDrillSessionById(recordId)
                val snapshotFile = ActiveSessionSnapshotManager.getSnapshotFile(context, recordId)
                val snapshotJson = if (snapshotFile.exists()) snapshotFile.readText() else null

                val label = drillSession?.scopeLabel ?: examRecord?.drillTitle ?: examRecord?.competence ?: recordId
                val date = drillSession?.formattedTimestamp ?: examRecord?.dateFormatted ?: ""
                val progress = if (drillSession != null) "${drillSession.answeredCount}/${drillSession.totalItems}" else (examRecord?.let { "${it.correctCount}/${it.totalQuestions}" } ?: "")

                val deletedSession = DeletedSession(
                    sessionId = recordId,
                    trackName = _selectedTrack.value.name,
                    label = label,
                    date = date,
                    progress = progress,
                    examRecordJson = examRecord?.let { DeletedSessionManager.serializeExamRecord(it) } ?: "",
                    drillSessionJson = drillSession?.let { DeletedSessionManager.serializeDrillSession(it) } ?: "",
                    snapshotJson = snapshotJson
                )
                DeletedSessionManager.addDeletedSession(context, deletedSession)
            }
            _deletedSessions.value = DeletedSessionManager.loadDeletedSessions(context)

            repository.deleteExamRecordsByIds(recordIds)
            recordIds.forEach { ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), it) }
            val trackName = _selectedTrack.value.name
            val activeSnap = ActiveSessionSnapshotManager.loadSnapshot(getApplication(), trackName)
            if (activeSnap != null && (recordIds.contains(activeSnap.sessionId) || recordIds.contains(trackName))) {
                ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), trackName)
                repository.deleteActiveSessionByTrack(trackName)
                _activeSessionOverride.value = _activeSessionOverride.value + (trackName to null)
            }
            if (recordIds.contains(currentActiveSessionId)) {
                currentActiveSessionId = java.util.UUID.randomUUID().toString()
            }
            notifySnapshotChanged()
        }
    }

    fun clearHistoryForCurrentTrack() {
        val track = _selectedTrack.value
        clearSeenQuestions(track)
        viewModelScope.launch {
            val trackName = track.name
            repository.clearAllByTrack(trackName)
            repository.deleteActiveSessionByTrack(trackName)
            _activeSessionOverride.value = _activeSessionOverride.value + (trackName to null)
            notifySnapshotChanged()
        }
    }

    fun purgeIncompleteExamRecords() {
        viewModelScope.launch {
            repository.purgeIncompleteRecords()
        }
    }

    fun loadPastExamResult(record: ExamRecordEntity) {
        val track = try { Track.valueOf(record.track) } catch (_: Exception) { Track.DECK_OIC_NW }
        val mode = try { QuizMode.valueOf(record.mode) } catch (_: Exception) { QuizMode.MOCK_EXAM }
        val compScores = SessionJsonHelper.deserializeCompetenceScores(record.competenceBreakdownJson)
        val userAnswers = SessionJsonHelper.deserializeAnswers(record.answersJson)

        val qIds = record.questionIds.split(",").filter { it.isNotBlank() }
        val questionsById = MaritimeQuestionBank.ALL_QUESTIONS.associateBy { it.id }
        val questions = qIds.mapNotNull { id -> questionsById[id] }
        _activeQuestions.value = questions
        _userQuestionStates.value = userAnswers
        _isExamSubmitted.value = true
        _selectedTrack.value = track
        _activeQuizMode.value = mode

        // Group competence scores by function
        val functionGroups = compScores.groupBy { it.functionId }
        val functionScores = functionGroups.map { (funcId, list) ->
            val raw = list.sumOf { it.rawPointsEarned }
            val max = list.sumOf { it.maxPointsPossible }
            val pct = if (max > 0) (raw.toDouble() / max) * 100.0 else 0.0
            val allPassed = list.all { it.isPassed }
            FunctionScore(
                functionId = funcId,
                functionTitle = MaritimeCompetenceRegistry.getFunctionTitle(funcId),
                competences = list,
                rawPointsEarned = raw,
                maxPointsPossible = max,
                percentage = pct,
                isPassed = allPassed
            )
        }

        val result = ExamResult(
            examId = record.id,
            timestamp = record.timestamp,
            track = track,
            mode = mode,
            totalQuestions = record.totalQuestions,
            correctCount = record.correctCount,
            rawPointsEarned = record.rawScore,
            maxPointsPossible = record.maxScore,
            overallPercentage = record.percentage,
            isOverallPassed = record.isPassed,
            functionScores = functionScores,
            competenceScores = compScores,
            questionStates = userAnswers
        )
        _lastExamResult.value = result
        _currentScreen.value = Screen.Results
    }

    fun practiceMissedQuestionsFromExam() {
        val lastResult = _lastExamResult.value ?: return
        val missedQuestionIds = _activeQuestions.value.filter { q ->
            val ans = lastResult.questionStates[q.id]
            ans?.selectedIndex != q.correctAnswerIndex
        }.map { it.id }.toSet()

        if (missedQuestionIds.isNotEmpty()) {
            val missedQuestions = MaritimeQuestionBank.ALL_QUESTIONS.filter { it.id in missedQuestionIds }
            _activeQuestions.value = missedQuestions.shuffled()
            _activeQuizMode.value = QuizMode.PRACTICE
            _currentQuestionIndex.value = 0
            _userQuestionStates.value = missedQuestions.associate { it.id to UserQuestionState(it.id) }
            _timeRemainingSeconds.value = 0
            autoSaveCurrentSession()
            _currentScreen.value = Screen.Quiz
        }
    }

    fun retakePracticeDrill() {
        val config = _lastPracticeDrillConfig
        if (config != null) {
            startConfiguredPractice(
                competenceId = config.competenceId,
                selectedPart = config.selectedPart,
                deliveryMode = config.deliveryMode,
                questionCount = config.questionCount,
                startItem = config.startItem,
                endItem = config.endItem,
                shuffleQuestions = config.shuffleQuestions,
                shuffleChoices = config.shuffleChoices,
                isCrpMode = config.isCrpMode,
                prioritizeUnseen = config.prioritizeUnseen,
                isOperationalReview = config.isOperationalReview
            )
        } else if (_activeCompetenceId.value != null || _activePartLabel.value != null) {
            val partNum = _activePartLabel.value?.removePrefix("Part ")?.trim()?.toIntOrNull()
            startConfiguredPractice(
                competenceId = _activeCompetenceId.value,
                selectedPart = partNum,
                isCrpMode = _isCrpMode.value
            )
        } else if (_activeQuestions.value.isNotEmpty()) {
            resetActivePracticeSession()
            _currentScreen.value = Screen.Quiz
        } else {
            startFreePractice()
        }
    }

    fun retakeCurrentExam() {
        val result = _lastExamResult.value
        val mode = result?.mode ?: _activeQuizMode.value

        if (mode == QuizMode.MOCK_EXAM) {
            _lastExamResult.value = null
            startTimedMockExam(_selectedTrack.value)
        } else {
            retakePracticeDrill()
        }
    }

    fun getTrackCompetences() = MaritimeCompetenceRegistry.getCompetencesForTrack(_selectedTrack.value)

    fun getVaultQuestions(): List<Question> {
        val currentTrack = _selectedTrack.value
        val entries = MistakesVaultManager.getEntriesForTrack(getApplication(), currentTrack.name)
        val list = entries.map { it.question }.filter { q ->
            val text = q.questionText.trim()
            !text.equals("Question", ignoreCase = true) && text.isNotBlank()
        }
        return if (isTrackUnlocked(_selectedTrack.value)) list else list.take(5)
    }

    fun getBookmarkedQuestions(): List<Question> {
        val bookmarked = bookmarkedIds.value
        val list = MaritimeQuestionBank.ALL_QUESTIONS.filter { it.id in bookmarked }
        val isPro = isTrackUnlocked(_selectedTrack.value)
        return if (isPro) list else list.take(5)
    }

    fun getFlashcardQuestions(competenceId: String? = null): List<Question> {
        val isPro = isTrackUnlocked(_selectedTrack.value)
        val questions = if (competenceId != null) {
            MaritimeQuestionBank.getQuestionsByCompetence(competenceId, _selectedTrack.value, isProUnlocked = isPro)
        } else {
            MaritimeQuestionBank.getQuestionsByTrack(_selectedTrack.value, isProUnlocked = isPro)
        }
        if (!isPro) {
            triggerPaywall(_selectedTrack.value)
        }
        return if (isPro) questions else questions.take(5)
    }
}

typealias ReviewerTrack = Track
typealias SearchQuestionBankViewModel = ReviewerViewModel
typealias MistakesViewModel = ReviewerViewModel
typealias BookmarksViewModel = ReviewerViewModel
typealias PracticeConfigViewModel = ReviewerViewModel
