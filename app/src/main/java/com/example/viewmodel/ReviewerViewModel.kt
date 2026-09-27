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
    object PracticeCompetenceSelect : Screen() // Step 1: Competence Selection Screen
    data class PracticeConfig(val competenceId: String? = null) : Screen() // Step 2: Practice Configuration Page
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
    object About : Screen() // About Hyperion
}

class ReviewerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HyperionDatabase.getInstance(application)
    private val repository = ReviewerRepository(database)
    private val prefs = application.getSharedPreferences("hyperion_prefs", Context.MODE_PRIVATE)

    init {
        LicenseManager.init(application)
    }

    // Licensing & Activation State (Single reactive state across app)
    val deviceRequestCode: String = LicenseManager.getOrGenerateRequestCode(application)

    val isActivated: StateFlow<Boolean> = LicenseManager.isProActivatedFlow

    fun activateLicense(enteredKey: String): Boolean {
        val isValid = LicenseManager.verifyKey(deviceRequestCode, enteredKey)
        if (isValid) {
            LicenseManager.setActivated(getApplication(), true)
        }
        return isValid
    }

    fun deactivateLicense() {
        LicenseManager.setActivated(getApplication(), false)
        ProQuestionRepository.clearCache()
        _currentScreen.value = Screen.Activation
    }

    // Side Navigation Drawer Trigger
    private val _drawerOpenEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val drawerOpenEvent = _drawerOpenEvent.asSharedFlow()

    fun openDrawer() {
        _drawerOpenEvent.tryEmit(Unit)
    }

    // Access Gate Helper: routes directly to Activation screen without modal popups
    fun requireActivationOr(onAccessGranted: () -> Unit) {
        if (LicenseManager.isActivated(getApplication())) {
            onAccessGranted()
        } else {
            navigateTo(Screen.Activation)
        }
    }

    // Terms Acceptance State
    private val _hasAcceptedTerms = MutableStateFlow(prefs.getBoolean("has_accepted_terms", false))
    val hasAcceptedTerms: StateFlow<Boolean> = _hasAcceptedTerms.asStateFlow()

    fun acceptTerms() {
        prefs.edit().putBoolean("has_accepted_terms", true).apply()
        _hasAcceptedTerms.value = true
        _currentScreen.value = Screen.TrackSelection
    }

    // Navigation State - starts at TermsOnboarding if not accepted, else TrackSelection
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

    private val _searchResults = MutableStateFlow<List<Question>>(emptyList())
    val searchResults: StateFlow<List<Question>> = _searchResults.asStateFlow()

    // Flows from Room Database
    val bookmarkedIds: StateFlow<Set<String>> = repository.observeBookmarkIds()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val mistakeEntities: StateFlow<List<MistakeEntity>> = repository.observeMistakes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    val activeSession: StateFlow<ActiveSessionEntity?> = combine(
        _selectedTrack,
        _snapshotTrigger.onStart { emit(Unit) }
    ) { track, _ ->
        val snapshot = ActiveSessionSnapshotManager.loadSnapshot(getApplication(), track.name)
        if (snapshot != null && !snapshot.isCompleted) {
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
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setTrack(track: Track) {
        _selectedTrack.value = track
    }

    fun selectTrackAndOpenDashboard(track: Track) {
        _selectedTrack.value = track
        _currentScreen.value = Screen.Home
    }

    fun startFreePractice() {
        timerJob?.cancel()
        _activeQuizMode.value = QuizMode.PRACTICE
        _activeDrillDeliveryMode.value = DrillDeliveryMode.STANDARD
        _activeFunctionId.value = null
        _activeCompetenceId.value = null
        _currentQuestionIndex.value = 0
        _showSubmitDialog.value = false
        _isPaletteOpen.value = false

        val deckQuestions = MaritimeQuestionBank.getQuestionsByTrack(Track.DECK_OIC_NW).take(5)
        val gmdssQuestions = MaritimeQuestionBank.getQuestionsByTrack(Track.GMDSS_GOC).take(5)
        val freeList = deckQuestions + gmdssQuestions

        _activeQuestions.value = freeList
        _userQuestionStates.value = freeList.associate { it.id to UserQuestionState(questionId = it.id) }
        _timeRemainingSeconds.value = 0
        _currentScreen.value = Screen.FreePractice
    }

    fun startConfiguredPractice(
        competenceId: String? = null,
        selectedPart: Int? = null,
        deliveryMode: DrillDeliveryMode = DrillDeliveryMode.STANDARD,
        questionCount: Int? = null,
        shuffleQuestions: Boolean = false,
        shuffleChoices: Boolean = true
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

        var questions: List<Question> = if (_selectedTrack.value == Track.GMDSS_GOC) {
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

        // Secondary defense check: Truncate to first 5 questions if unactivated
        if (!isPro) {
            questions = questions.take(5)
        }

        if (shuffleQuestions) {
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

        if (deliveryMode == DrillDeliveryMode.INTENSIVE) {
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

    fun resetIntensiveDrill() {
        _intensiveCompleted.value = false
        _intensiveClearedCount.value = 0
        _intensiveCurrentQuestionState.value = null
        _currentScreen.value = Screen.Home
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _searchResults.value = MaritimeQuestionBank.searchQuestions(query, _selectedTrack.value, isProUnlocked = isActivated.value)
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
            val totalSeconds = when (_selectedTrack.value) {
                Track.DECK_OIC_NW -> 200 * 60 // 200 mins
                Track.GMDSS_GOC -> 100 * 60 // 100 mins
            }
            _timeRemainingSeconds.value = totalSeconds
            startTimer()
            autoSaveCurrentSession()
            _currentScreen.value = Screen.MockHub
        } else {
            _timeRemainingSeconds.value = 0
            autoSaveCurrentSession()
            _currentScreen.value = Screen.Quiz
        }
    }

    private fun startTimer() {
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
            // Auto submit when time runs out!
            submitExam()
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
        ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), _selectedTrack.value.name)
        notifySnapshotChanged()
        val questions = _activeQuestions.value
        val answers = _userQuestionStates.value
        if (questions.isNotEmpty() && answers.values.any { it.selectedIndex != null }) {
            viewModelScope.launch {
                repository.saveIncompleteSession(
                    track = _selectedTrack.value,
                    mode = _activeQuizMode.value,
                    questions = questions,
                    answers = answers
                )
                repository.clearActiveSession(_selectedTrack.value)
            }
        } else {
            viewModelScope.launch {
                repository.clearActiveSession(_selectedTrack.value)
            }
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
        _currentScreen.value = Screen.MockCompetence(competenceId)
    }

    fun markCompetenceCompleted(competenceId: String) {
        _completedCompetences.value = _completedCompetences.value + competenceId
        autoSaveCurrentSession()
        _currentScreen.value = Screen.MockHub
    }

    fun getQuestionsForCompetence(competenceId: String): List<Question> {
        // Direct defense: Unactivated mock exam returns empty list immediately
        if (!isActivated.value) return emptyList()
        return _activeQuestions.value.filter { it.competenceId == competenceId }
    }

    fun selectOptionForQuestion(questionId: String, optionIndex: Int) {
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

        val questions = _activeQuestions.value
        val answers = _userQuestionStates.value
        _activeQuestions.value = emptyList()

        // Evaluate with official MARINA scoring engine
        val evaluatedResult = MarinaScoringEngine.evaluateExam(
            track = _selectedTrack.value,
            mode = _activeQuizMode.value,
            questions = questions,
            answers = answers
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
            }
            repository.saveExamRecord(result, questions.map { it.id })
            ActiveSessionSnapshotManager.deleteSnapshot(getApplication(), _selectedTrack.value.name)
            notifySnapshotChanged()
            repository.clearActiveSession(_selectedTrack.value)
        }

        _currentScreen.value = Screen.Results
    }

    fun deleteExamRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteExamRecord(recordId)
            repository.clearActiveSession(_selectedTrack.value)
        }
    }

    fun deleteExamRecordsByIds(recordIds: List<String>) {
        viewModelScope.launch {
            repository.deleteExamRecordsByIds(recordIds)
            repository.clearActiveSession(_selectedTrack.value)
        }
    }

    fun clearHistoryForCurrentTrack() {
        viewModelScope.launch {
            repository.clearExamRecordsByTrack(_selectedTrack.value)
            repository.clearActiveSession(_selectedTrack.value)
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
        startQuizSession(
            mode = _activeQuizMode.value,
            functionId = _activeFunctionId.value,
            competenceId = _activeCompetenceId.value
        )
    }

    fun getTrackCompetences() = MaritimeCompetenceRegistry.getCompetencesForTrack(_selectedTrack.value)

    fun getVaultQuestions(): List<Question> {
        val vaultIds = mistakeEntities.value.map { it.questionId }.toSet()
        val list = MaritimeQuestionBank.ALL_QUESTIONS.filter { it.id in vaultIds }
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
