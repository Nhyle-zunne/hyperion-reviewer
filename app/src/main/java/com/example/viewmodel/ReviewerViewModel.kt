package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActiveSessionEntity
import com.example.data.local.ActiveSessionSnapshot
import com.example.data.local.ActiveSessionSnapshotManager
import com.example.data.local.ExamRecordEntity
import com.example.data.local.HyperionDatabase
import com.example.data.local.MistakeEntity
import com.example.data.local.MistakeEntry
import com.example.data.local.MistakesVaultManager
import com.example.data.local.SessionJsonHelper
import com.example.data.model.Difficulty
import com.example.data.model.ExamResult
import com.example.data.model.FunctionScore
import com.example.data.model.ProQuestion
import com.example.data.model.Question
import com.example.data.model.QuizMode
import com.example.data.model.Track
import com.example.data.model.UserQuestionState
import com.example.data.repository.MarinaScoringEngine
import com.example.data.repository.MaritimeCompetenceRegistry
import com.example.data.repository.MaritimeQuestionBank
import com.example.data.repository.ProQuestionRepository
import com.example.data.repository.ReviewerRepository
import com.example.data.util.LicenseManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.UUID

enum class DrillDeliveryMode {
    STANDARD,
    INTENSIVE
}

sealed class Screen {
    object TermsOnboarding : Screen() // First-launch mandatory terms gate
    object TermsAndDisclaimer : Screen() // Permanent legal agreement viewer
    object TrackSelection : Screen() // Dedicated Track Selection Homepage
    object Home : Screen() // Track Dashboard
    data class PracticeCompetenceSelect(val isCrpMode: Boolean = false) : Screen() // Step 1: Competence Selection Screen
    data class PracticeConfig(val competenceId: String? = null, val isCrpMode: Boolean = false) : Screen() // Step 2: Practice Configuration Page
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
    object About : Screen() // About Hyperion
}

class ReviewerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HyperionDatabase.getInstance(application)
    private val repository = ReviewerRepository(database)
    private val prefs = application.getSharedPreferences("hyperion_prefs", Context.MODE_PRIVATE)

    init {
        LicenseManager.init(application)
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
    }

    // Licensing & Activation State (Independent Pro Tiers)
    val deviceRequestCode: String get() = LicenseManager.getOrGenerateRequestCode(getApplication())
    val permanentHardwareId: String get() = LicenseManager.getPermanentHardwareId(getApplication())
    val installSessionId: String get() = LicenseManager.getInstallSessionId(getApplication())
    val installDate: String get() = LicenseManager.getInstallDate(getApplication())

    val isOicNwProUnlocked: StateFlow<Boolean> = LicenseManager.isOicNwProUnlockedFlow
    val isGmdssProUnlocked: StateFlow<Boolean> = LicenseManager.isGmdssProUnlockedFlow
    val isHyperionProUnlocked: StateFlow<Boolean> = LicenseManager.isHyperionProUnlockedFlow
    val isMasterProUnlocked: StateFlow<Boolean> = LicenseManager.isHyperionProUnlockedFlow
    val isActivated: StateFlow<Boolean> = LicenseManager.isProActivatedFlow

    fun isTrackProUnlocked(track: Track = _selectedTrack.value): Boolean {
        return LicenseManager.isTrackProUnlocked(getApplication(), track)
    }

    fun hasAccessTo(module: com.example.data.util.LicenseTier): Boolean {
        return LicenseManager.hasAccessTo(getApplication(), module)
    }

    fun activateLicense(enteredKey: String, targetTier: com.example.data.util.ProTier? = null): com.example.data.util.UnlockResult {
        val result = LicenseManager.verifyAndUnlockKey(getApplication(), enteredKey, targetTier)
        if (result.isSuccess) {
            ProQuestionRepository.clearCache()
        }
        return result
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

    // Access Gate Helper: routes directly to Account & Device Help screen if track is not unlocked
    fun requireActivationOr(track: Track = _selectedTrack.value, onAccessGranted: () -> Unit) {
        if (LicenseManager.isTrackProUnlocked(getApplication(), track)) {
            onAccessGranted()
        } else {
            navigateTo(Screen.HowToAvail)
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

    private val _completedCompetences = MutableStateFlow<Set<String>>(emptySet())
    val completedCompetences: StateFlow<Set<String>> = _completedCompetences.asStateFlow()

    private val _isExamSubmitted = MutableStateFlow(false)
    val isExamSubmitted: StateFlow<Boolean> = _isExamSubmitted.asStateFlow()

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

    // Search Query & Results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCompetenceFilter = MutableStateFlow<String?>(null)
    val selectedCompetenceFilter: StateFlow<String?> = _selectedCompetenceFilter.asStateFlow()

    fun setCompetenceFilter(competenceId: String?) {
        _selectedCompetenceFilter.value = competenceId
    }

    val searchResults: StateFlow<List<Question>> = combine(
        _searchQuery
            .debounce(300L)
            .distinctUntilChanged(),
        _selectedTrack,
        isActivated
    ) { query, track, proUnlocked ->
        MaritimeQuestionBank.searchQuestions(query, track, isProUnlocked = proUnlocked)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // Flows from Room Database
    val bookmarkedIds: StateFlow<Set<String>> = repository.observeBookmarkIds()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val mistakeEntities: StateFlow<List<MistakeEntity>> = repository.observeMistakes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    val examRecords: StateFlow<List<ExamRecordEntity>> = combine(
        _selectedTrack.flatMapLatest { track -> repository.observeExamRecordsForTrack(track) },
        _snapshotTrigger.onStart { emit(Unit) }
    ) { dbRecords, _ ->
        val track = _selectedTrack.value
        val snapshot = ActiveSessionSnapshotManager.loadSnapshot(getApplication(), track.name)
        val list = dbRecords.toMutableList()
        if (snapshot != null && !snapshot.isCompleted) {
            val answeredCount = snapshot.selectedAnswers.size
            val pct = if (snapshot.totalQuestions > 0) (answeredCount.toDouble() / snapshot.totalQuestions) * 100.0 else 0.0
            val incompleteRecord = ExamRecordEntity(
                id = snapshot.sessionId,
                track = snapshot.track,
                mode = snapshot.mode,
                timestamp = snapshot.timestamp,
                rawScore = answeredCount,
                maxScore = snapshot.totalQuestions,
                percentage = pct,
                isPassed = pct >= 70.0,
                correctCount = answeredCount,
                totalQuestions = snapshot.totalQuestions,
                competenceBreakdownJson = "",
                answersJson = SessionJsonHelper.serializeAnswers(snapshot.selectedAnswers.mapValues { (qid, sel) ->
                    UserQuestionState(questionId = qid, selectedIndex = sel.toIntOrNull(), isSubmitted = true)
                }),
                detailsSummary = "INCOMPLETE - $answeredCount/${snapshot.totalQuestions} Answered",
                questionIds = snapshot.questions.joinToString(",") { it.id }
            )
            list.removeAll { it.id == snapshot.sessionId }
            list.add(0, incompleteRecord)
        }
        list
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activeSessionOverride = MutableStateFlow<Map<String, ActiveSessionEntity?>>(emptyMap())

    val activeSession: StateFlow<ActiveSessionEntity?> = combine(
        _selectedTrack,
        _snapshotTrigger.onStart { emit(Unit) },
        _activeSessionOverride
    ) { track, _, overrides ->
        if (overrides.containsKey(track.name)) {
            overrides[track.name]
        } else {
            val snapshot = ActiveSessionSnapshotManager.loadSnapshot(getApplication(), track.name)
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
        backStack.addLast(current)
        _currentScreen.value = screen
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
            screen is Screen.PracticeConfig ||
            screen is Screen.PracticeCompetenceSelect ||
            screen is Screen.FreePractice
        }
    }

    fun navigateBack(): Boolean {
        // When on Home (OIC-NW or GMDSS Track menu), back MUST always return directly to Dashboard
        if (_currentScreen.value is Screen.Home) {
            backStack.clear()
            _currentScreen.value = Screen.TrackSelection
            return true
        }

        while (backStack.isNotEmpty()) {
            val prev = backStack.removeLast()
            if (prev != _currentScreen.value &&
                prev !is Screen.Quiz &&
                prev !is Screen.MockCompetence &&
                prev !is Screen.Results &&
                prev !is Screen.FreePractice
            ) {
                _currentScreen.value = prev
                return true
            }
        }

        if (_currentScreen.value !is Screen.TrackSelection) {
            _currentScreen.value = Screen.TrackSelection
            return true
        }
        return false
    }

    fun popBackStack(): Boolean = navigateBack()

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

    fun activateWithKey(key: String, targetTier: com.example.data.util.ProTier? = null) = activateLicense(key, targetTier)

    fun clearAllBookmarks() {
        viewModelScope.launch {
            repository.clearAllBookmarks()
        }
    }

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

    fun exitQuizToHome() {
        timerJob?.cancel()
        refreshMistakes()
        purgeExamSessionFromBackStack()
        _currentScreen.value = Screen.Home
    }

    fun finishExamToHome() {
        timerJob?.cancel()
        refreshMistakes()
        purgeExamSessionFromBackStack()
        _currentScreen.value = Screen.Home
    }

    fun resetIntensiveDrill() {
        timerJob?.cancel()
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
        val mode = _activeQuizMode.value
        purgeExamSessionFromBackStack()
        if (mode == QuizMode.VAULT_DRILL) {
            _currentScreen.value = Screen.Vault
        } else if (mode == QuizMode.BOOKMARKS_DRILL) {
            _currentScreen.value = Screen.Bookmarks
        } else {
            _currentScreen.value = Screen.Home
        }
    }

    fun startMarinaCrpPractice(competenceId: String? = null, selectedPart: Int? = null) {
        timerJob?.cancel()
        _activeQuizMode.value = QuizMode.PRACTICE
        _activeDrillDeliveryMode.value = DrillDeliveryMode.STANDARD
        _activeFunctionId.value = null
        _activeCompetenceId.value = competenceId
        _currentQuestionIndex.value = 0
        _showSubmitDialog.value = false
        _isPaletteOpen.value = false

        val crpQuestions = if (_selectedTrack.value == Track.GMDSS_GOC) {
            MaritimeQuestionBank.getMarinaCrpGmdssQuestions(getApplication(), competenceId, selectedPart)
        } else {
            MaritimeQuestionBank.getMarinaCrpQuestions(competenceId)
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
        shuffleQuestions: Boolean = false,
        shuffleChoices: Boolean = true,
        isCrpMode: Boolean = false
    ) {
        timerJob?.cancel()
        _activeQuizMode.value = QuizMode.PRACTICE
        _activeDrillDeliveryMode.value = deliveryMode
        _activeFunctionId.value = null
        _activeCompetenceId.value = competenceId
        _currentQuestionIndex.value = 0
        _showSubmitDialog.value = false
        _isPaletteOpen.value = false

        val isPro = isActivated.value
        val context = getApplication<Application>()

        var questions: List<Question> = if (isCrpMode) {
            if (_selectedTrack.value == Track.GMDSS_GOC) {
                MaritimeQuestionBank.getMarinaCrpGmdssQuestions(context, competenceId, selectedPart)
            } else {
                MaritimeQuestionBank.getMarinaCrpQuestions(competenceId)
            }
        } else if (_selectedTrack.value == Track.GMDSS_GOC) {
            val proQuestions = if (competenceId != null) {
                if (selectedPart != null) {
                    ProQuestionRepository.loadPartQuestions(context, competenceId, selectedPart)
                } else {
                    ProQuestionRepository.loadAllCompetenceQuestions(context, competenceId)
                }
            } else {
                ProQuestionRepository.loadAllCompetenceQuestions(context, "C1") +
                ProQuestionRepository.loadAllCompetenceQuestions(context, "C2")
            }
            proQuestions.map { it.toQuestion() }
        } else {
            if (competenceId != null) {
                MaritimeQuestionBank.getQuestionsByCompetence(competenceId, _selectedTrack.value, isProUnlocked = isPro)
            } else {
                MaritimeQuestionBank.getQuestionsByTrack(_selectedTrack.value, isProUnlocked = isPro)
            }
        }

        // Secondary defense check: Truncate to first 5 questions if unactivated and NOT CRP mode
        if (!isPro && !isCrpMode) {
            questions = questions.take(5)
        }

        if (shuffleQuestions && (isPro || !isCrpMode)) {
            questions = questions.shuffled()
        }

        if (questionCount != null && questionCount > 0 && questions.size > questionCount) {
            questions = questions.take(questionCount)
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
        } else {
            _activeQuestions.value = questions
            _userQuestionStates.value = questions.associate { it.id to UserQuestionState(questionId = it.id) }
        }

        _timeRemainingSeconds.value = 0
        autoSaveCurrentSession()
        _currentScreen.value = Screen.Quiz
    }

    fun selectIntensiveOption(optionIndex: Int) {
        val currentQ = _intensiveQueue.value.firstOrNull() ?: return
        val isCorrect = optionIndex == currentQ.correctAnswerIndex
        _intensiveCurrentQuestionState.value = UserQuestionState(
            questionId = currentQ.id,
            selectedIndex = optionIndex,
            isSubmitted = true
        )
        viewModelScope.launch {
            repository.recordQuestionAttempt(currentQ.id, isCorrect)
        }
    }

    fun advanceIntensiveQuestion() {
        val queue = _intensiveQueue.value
        if (queue.isEmpty()) return
        val currentQ = queue.first()
        val state = _intensiveCurrentQuestionState.value
        val isCorrect = state?.selectedIndex == currentQ.correctAnswerIndex

        if (isCorrect) {
            val newQueue = queue.drop(1)
            _intensiveQueue.value = newQueue
            _intensiveClearedCount.value += 1
            if (newQueue.isEmpty()) {
                _intensiveCompleted.value = true
            }
        } else {
            // Push wrong question to the very end of the queue
            val newQueue = queue.drop(1) + currentQ
            _intensiveQueue.value = newQueue
        }
        _intensiveCurrentQuestionState.value = null
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
        viewModelScope.launch {
            repository.toggleBookmark(questionId, currentBookmarked)
        }
    }

    fun startQuizSession(
        mode: QuizMode,
        functionId: String? = null,
        competenceId: String? = null,
        limitQuestions: Int? = null
    ) {
        timerJob?.cancel()
        _isExamSubmitted.value = false
        _activeQuizMode.value = mode
        _activeFunctionId.value = functionId
        _activeCompetenceId.value = competenceId
        _currentQuestionIndex.value = 0
        _showSubmitDialog.value = false
        _isPaletteOpen.value = false

        val vaultIds = mistakeEntities.value.map { it.questionId }.toSet()
        val bookmarked = bookmarkedIds.value
        val isPro = isActivated.value

        var questions = repository.getQuestionsForSession(
            track = _selectedTrack.value,
            mode = mode,
            functionId = functionId,
            competenceId = competenceId,
            vaultQuestionIds = vaultIds,
            bookmarkIds = bookmarked,
            isProUnlocked = isPro
        )

        // Defense check: Lock out Mock Exams or truncate unactivated sessions
        if (!isPro) {
            if (mode == QuizMode.MOCK_EXAM) {
                questions = emptyList()
            } else {
                questions = questions.take(5)
            }
        }

        if (limitQuestions != null && limitQuestions > 0 && questions.size > limitQuestions) {
            questions = questions.take(limitQuestions)
        }

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
        var questions = qIdList.mapNotNull { id -> MaritimeQuestionBank.ALL_QUESTIONS.find { it.id == id } }
        if (questions.isEmpty() && qIdList.isNotEmpty()) {
            val allTrackQ = MaritimeQuestionBank.getQuestionsByTrack(track, isProUnlocked = true)
            questions = qIdList.mapNotNull { id -> allTrackQ.find { it.id == id } }
        }
        if (questions.isEmpty()) {
            questions = if (!session.competenceId.isNullOrBlank()) {
                MaritimeQuestionBank.getQuestionsByCompetence(session.competenceId)
            } else {
                MaritimeQuestionBank.getQuestionsByTrack(track, isProUnlocked = true)
            }
        }

        if (questions.isNotEmpty()) {
            _activeQuestions.value = questions
            val userAnswers = SessionJsonHelper.deserializeAnswers(session.answersJson)
            _userQuestionStates.value = userAnswers

            val firstUnansweredIndex = questions.indexOfFirst { q -> userAnswers[q.id]?.selectedIndex == null }
            val targetIndex = if (firstUnansweredIndex >= 0) firstUnansweredIndex else session.currentQuestionIndex.coerceIn(0, questions.size - 1)
            _currentQuestionIndex.value = targetIndex

            _timeRemainingSeconds.value = session.timeRemainingSeconds
            _completedCompetences.value = session.completedCompetences.split(",").filter { it.isNotBlank() }.toSet()

            if (mode == QuizMode.MOCK_EXAM) {
                if (session.timeRemainingSeconds > 0) {
                    startTimer()
                }
                _currentScreen.value = Screen.MockHub
            } else {
                _currentScreen.value = Screen.Quiz
            }
        }
    }

    fun resumeActiveSession() {
        activeSession.value?.let { session ->
            resumeActiveSession(session)
        }
    }

    fun resumeSession(trackName: String, sessionId: String? = null) {
        val targetTrack = try {
            Track.values().find { it.name.equals(trackName, ignoreCase = true) || it.shortName.equals(trackName, ignoreCase = true) }
                ?: Track.valueOf(trackName)
        } catch (_: Exception) {
            _selectedTrack.value
        }
        _selectedTrack.value = targetTrack

        val snapshot = ActiveSessionSnapshotManager.loadSnapshot(getApplication(), targetTrack.name)
        if (snapshot != null && !snapshot.isCompleted) {
            if (snapshot.mode.contains("MOCK", ignoreCase = true) && snapshot.totalQuestions > 200) {
                ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), targetTrack.name)
                discardActiveSession(targetTrack.name)
                startQuizSession(QuizMode.MOCK_EXAM)
                return
            }
            _isExamSubmitted.value = false
            currentActiveSessionId = snapshot.sessionId
            val trackEnum = try { Track.valueOf(snapshot.track) } catch (_: Exception) { targetTrack }
            _selectedTrack.value = trackEnum
            _activeQuizMode.value = if (snapshot.mode.equals("MOCK", ignoreCase = true)) QuizMode.MOCK_EXAM else QuizMode.PRACTICE
            _activeCompetenceId.value = snapshot.competenceOrPart.ifBlank { null }

            val questions = snapshot.questions
            if (questions.isNotEmpty()) {
                _activeQuestions.value = questions
                val userAnswers = snapshot.selectedAnswers.mapValues { (qid, sel) ->
                    UserQuestionState(questionId = qid, selectedIndex = sel.toIntOrNull(), isSubmitted = true)
                }
                _userQuestionStates.value = userAnswers
                _currentQuestionIndex.value = snapshot.currentIndex.coerceIn(0, questions.size - 1)
                _firstAttemptStatus.value = snapshot.firstAttemptStatus

                _currentScreen.value = if (_activeQuizMode.value == QuizMode.MOCK_EXAM) Screen.MockHub else Screen.Quiz
                return
            }
        }

        viewModelScope.launch {
            if (sessionId != null) {
                val record = repository.getExamRecordById(sessionId)
                if (record != null) {
                    resumeIncompleteExamRecord(record)
                    return@launch
                }
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
        timerJob?.cancel()
        val trackName = _selectedTrack.value.name
        ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), trackName)
        
        // Set local UI state activeSession = null immediately on Main dispatcher
        _activeSessionOverride.value = _activeSessionOverride.value + (trackName to null)
        notifySnapshotChanged()
        
        discardActiveSession(trackName)
    }

    fun discardActiveSession(trackId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // 1. Delete active session pointer and active session row
            repository.deleteActiveSessionByTrack(trackId)
            // 2. If it was pre-inserted into session_logs, purge it
            repository.deleteIncompleteSessionsByTrack(trackId)
        }
    }

    private var currentActiveSessionId: String = java.util.UUID.randomUUID().toString()

    private fun autoSaveCurrentSession() {
        val questions = _activeQuestions.value
        if (questions.isEmpty()) return
        val track = _selectedTrack.value
        val mode = _activeQuizMode.value
        val answers = _userQuestionStates.value

        val selectedAnswersMap = answers.entries.associate { (qid, state) ->
            qid to (state.selectedIndex?.toString() ?: "")
        }.filterValues { it.isNotBlank() }

        val snapshot = ActiveSessionSnapshot(
            sessionId = currentActiveSessionId,
            track = track.name,
            mode = mode.name,
            competenceOrPart = _activeCompetenceId.value ?: "",
            questions = questions,
            selectedAnswers = selectedAnswersMap,
            firstAttemptStatus = _firstAttemptStatus.value,
            currentIndex = _currentQuestionIndex.value,
            totalQuestions = questions.size,
            isCompleted = false,
            timestamp = System.currentTimeMillis()
        )
        ActiveSessionSnapshotManager.saveSnapshot(getApplication(), snapshot)
        notifySnapshotChanged()

        viewModelScope.launch {
            repository.saveActiveSession(
                track = track,
                mode = mode,
                functionId = _activeFunctionId.value,
                competenceId = _activeCompetenceId.value,
                currentQuestionIndex = _currentQuestionIndex.value,
                timeRemainingSeconds = _timeRemainingSeconds.value,
                questionIds = questions.map { it.id },
                answers = answers,
                completedCompetences = _completedCompetences.value
            )
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
    }

    fun markCompetenceCompleted(competenceId: String) {
        timerJob?.cancel()
        _timeRemainingSeconds.value = 0
        _completedCompetences.value = _completedCompetences.value + competenceId
        _activeCompetenceId.value = null
        autoSaveCurrentSession()
        _currentScreen.value = Screen.MockHub
    }

    fun getQuestionsForCompetence(competenceId: String): List<Question> {
        // Direct defense: Unactivated mock exam returns empty list immediately
        if (!isActivated.value) return emptyList()
        return _activeQuestions.value.filter { it.competenceId == competenceId }
    }

    fun selectOptionForQuestion(questionId: String, optionIndex: Int) {
        if (_isExamSubmitted.value) return // Permanent submission lock: cannot alter answers after submission
        val currentMap = _userQuestionStates.value.toMutableMap()
        val currentEntry = currentMap[questionId] ?: UserQuestionState(questionId = questionId)

        if (_activeQuizMode.value != QuizMode.MOCK_EXAM) {
            currentMap[questionId] = currentEntry.copy(
                selectedIndex = optionIndex,
                isSubmitted = true
            )
            _userQuestionStates.value = currentMap

            val question = _activeQuestions.value.find { it.id == questionId }
            if (question != null) {
                val isCorrect = optionIndex == question.correctAnswerIndex
                viewModelScope.launch {
                    repository.recordQuestionAttempt(question.id, isCorrect)
                    if (isCorrect) {
                        MistakesVaultManager.recordAttempt(getApplication(), question.id, true)
                    } else {
                        MistakesVaultManager.recordMistake(getApplication(), question, question.track.name, question.competenceId)
                    }
                    refreshMistakes()
                }
            }
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

    fun toggleFlag(questionIndex: Int) {
        val questions = _activeQuestions.value
        if (questionIndex !in questions.indices) return
        toggleFlagForQuestion(questions[questionIndex].id)
    }

    fun jumpToQuestion(index: Int) {
        val questions = _activeQuestions.value
        if (index in questions.indices) {
            _currentQuestionIndex.value = index
            _isPaletteOpen.value = false
            autoSaveCurrentSession()
        }
    }

    fun nextQuestion() {
        if (_currentQuestionIndex.value < _activeQuestions.value.size - 1) {
            _currentQuestionIndex.value += 1
            autoSaveCurrentSession()
        }
    }

    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value -= 1
            autoSaveCurrentSession()
        }
    }

    private var resumingRecordId: String? = null

    fun resumeIncompleteExamRecord(record: ExamRecordEntity) {
        viewModelScope.launch {
            val freshRecord = repository.getExamRecordById(record.id) ?: record
            resumeIncompleteExamRecordInternal(freshRecord)
        }
    }

    fun loadIncompleteSession(record: ExamRecordEntity) {
        resumeIncompleteExamRecord(record)
    }

    private fun resumeIncompleteExamRecordInternal(record: ExamRecordEntity) {
        timerJob?.cancel()
        val track = try { Track.valueOf(record.track) } catch (_: Exception) { Track.DECK_OIC_NW }
        val mode = try { QuizMode.valueOf(record.mode) } catch (_: Exception) { QuizMode.MOCK_EXAM }
        _selectedTrack.value = track
        _activeQuizMode.value = mode

        val qIds = record.questionIds.split(",").filter { it.isNotBlank() }
        var questions = qIds.mapNotNull { id -> MaritimeQuestionBank.ALL_QUESTIONS.find { it.id == id } }
        if (questions.isEmpty() && qIds.isNotEmpty()) {
            val allTrackQ = MaritimeQuestionBank.getQuestionsByTrack(track, isProUnlocked = true)
            questions = qIds.mapNotNull { id -> allTrackQ.find { it.id == id } }
        }

        if (questions.isNotEmpty()) {
            _activeQuestions.value = questions
            val userAnswers = SessionJsonHelper.deserializeAnswers(record.answersJson)
            _userQuestionStates.value = userAnswers

            val firstUnansweredIndex = questions.indexOfFirst { q -> userAnswers[q.id]?.selectedIndex == null }
            val targetIndex = if (firstUnansweredIndex >= 0) firstUnansweredIndex else 0
            _currentQuestionIndex.value = targetIndex

            resumingRecordId = record.id

            viewModelScope.launch {
                repository.saveActiveSession(
                    track = track,
                    mode = mode,
                    functionId = null,
                    competenceId = null,
                    currentQuestionIndex = targetIndex,
                    timeRemainingSeconds = if (mode == QuizMode.MOCK_EXAM) 12000 else 0,
                    questionIds = questions.map { it.id },
                    answers = userAnswers
                )
            }

            if (mode == QuizMode.MOCK_EXAM) {
                _currentScreen.value = Screen.MockHub
            } else {
                _currentScreen.value = Screen.Quiz
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
        val result = evaluatedResult.copy(
            examId = resumingRecordId ?: evaluatedResult.examId
        )
        resumingRecordId = null

        _lastExamResult.value = result

        // Record all questions in repo and clear active session
        viewModelScope.launch {
            for (q in questions) {
                val state = answers[q.id]
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
            val currentSnapshot = ActiveSessionSnapshotManager.loadSnapshot(getApplication(), _selectedTrack.value.name)
            if (currentSnapshot != null) {
                ActiveSessionSnapshotManager.saveSnapshot(getApplication(), currentSnapshot.copy(isCompleted = true))
            }
            ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), _selectedTrack.value.name)
            notifySnapshotChanged()
            repository.clearActiveSession(_selectedTrack.value)
        }

        _currentScreen.value = Screen.Results
    }

    fun deleteExamRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteExamRecord(recordId)
            val trackName = _selectedTrack.value.name
            repository.deleteActiveSessionByTrack(trackName)
            _activeSessionOverride.value = _activeSessionOverride.value + (trackName to null)
            notifySnapshotChanged()
        }
    }

    fun deleteExamRecordsByIds(recordIds: List<String>) {
        viewModelScope.launch {
            repository.deleteExamRecordsByIds(recordIds)
            val trackName = _selectedTrack.value.name
            repository.deleteActiveSessionByTrack(trackName)
            _activeSessionOverride.value = _activeSessionOverride.value + (trackName to null)
            notifySnapshotChanged()
        }
    }

    fun clearHistoryForCurrentTrack() {
        viewModelScope.launch {
            val trackName = _selectedTrack.value.name
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
        val questions = qIds.mapNotNull { id -> MaritimeQuestionBank.ALL_QUESTIONS.find { it.id == id } }
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

    fun retakeCurrentExam() {
        val mode = _activeQuizMode.value
        val compId = _activeCompetenceId.value

        if (mode == QuizMode.MOCK_EXAM) {
            _lastExamResult.value = null
            startQuizSession(mode = QuizMode.MOCK_EXAM)
        } else {
            // Reset active quiz session state
            _activeQuestions.value = emptyList()
            _userQuestionStates.value = emptyMap()
            _currentQuestionIndex.value = 0
            _timeRemainingSeconds.value = 0

            when (mode) {
                QuizMode.PRACTICE -> {
                    _currentScreen.value = Screen.PracticeConfig(compId)
                }
                QuizMode.VAULT_DRILL -> {
                    _currentScreen.value = Screen.Vault
                }
                QuizMode.BOOKMARKS_DRILL -> {
                    _currentScreen.value = Screen.Bookmarks
                }
                else -> {
                    _currentScreen.value = Screen.Home
                }
            }
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
        return if (isActivated.value) list else list.take(5)
    }

    fun getBookmarkedQuestions(): List<Question> {
        val bookmarked = bookmarkedIds.value
        val list = MaritimeQuestionBank.ALL_QUESTIONS.filter { it.id in bookmarked }
        return if (isActivated.value) list else list.take(5)
    }

    fun getFlashcardQuestions(competenceId: String? = null): List<Question> {
        val isPro = isActivated.value
        val questions = if (competenceId != null) {
            MaritimeQuestionBank.getQuestionsByCompetence(competenceId, _selectedTrack.value, isProUnlocked = isPro)
        } else {
            MaritimeQuestionBank.getQuestionsByTrack(_selectedTrack.value, isProUnlocked = isPro)
        }
        return if (isPro) questions else questions.take(5)
    }
}
