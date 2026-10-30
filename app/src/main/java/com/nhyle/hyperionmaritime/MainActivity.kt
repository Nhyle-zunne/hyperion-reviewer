package com.nhyle.hyperionmaritime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import com.nhyle.hyperionmaritime.ui.motion.hyperionPageTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.auth.SupabaseAccountManager
import com.nhyle.hyperionmaritime.data.billing.BillingManager
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.ui.components.DailyCheckInDialog
import com.nhyle.hyperionmaritime.ui.components.ResultsLoadingDialog
import com.nhyle.hyperionmaritime.ui.components.HyperionDrawerContent
import com.nhyle.hyperionmaritime.ui.components.PaywallDialog
import com.nhyle.hyperionmaritime.ui.screens.*
import com.nhyle.hyperionmaritime.ui.store.StoreScreen
import com.nhyle.hyperionmaritime.ui.theme.AppTheme
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nhyle.hyperionmaritime.data.sync.LearningProgressSyncWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhyle.hyperionmaritime.ui.motion.HyperionDialog
import androidx.compose.ui.window.DialogProperties
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors

class MainActivity : ComponentActivity() {
    private val viewModel: ReviewerViewModel by viewModels()
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MaritimeQuestionBank.init(applicationContext)
        LicenseManager.init(applicationContext)
        scheduleLearningProgressSync()
        lifecycleScope.launch {
            SupabaseAccountManager.restoreSessionAndSync(applicationContext)
        }

        // Warm up the question bank on an I/O thread so opening a track is instant
        lifecycleScope.launch(Dispatchers.IO) {
            MaritimeQuestionBank.ALL_QUESTIONS
        }

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForInAppUpdates()

        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            AppTheme(isDarkMode = isDarkMode) {
                HyperionApp(viewModel = viewModel)
            }
        }
    }

    private fun scheduleLearningProgressSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<LearningProgressSyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "hyperion-account-learning-progress-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Queue a network-constrained upload after the study-time tracker has had
     * time to persist its asynchronous background writes. This supplements
     * sign-in synchronization and the 15-minute periodic retry.
     */
    private fun scheduleImmediateLearningProgressSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<LearningProgressSyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(3, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "hyperion-account-learning-progress-sync-after-background",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun checkForInAppUpdates() {
        if (!isOnline(this)) {
            // Offline Guard: Abort update check immediately for offline / at-sea users
            return
        }

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        REQ_CODE_IMMEDIATE_UPDATE
                    )
                } catch (_: Exception) {
                    // Silently handle error to prevent blocking user
                }
            }
        }.addOnFailureListener {
            // Catch errors, missing Play Store, or network drops silently
        }
    }

    override fun onResume() {
        super.onResume()
        BillingManager.getInstance(applicationContext).queryPurchasesOnResume()
        if (::appUpdateManager.isInitialized && isOnline(this)) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            REQ_CODE_IMMEDIATE_UPDATE
                        )
                    } catch (_: Exception) {
                        // Silently handle error
                    }
                }
            }.addOnFailureListener {
                // Silently handle failure
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.studyTimeTracker.onAppBackground()
        scheduleImmediateLearningProgressSync()
    }

    override fun onStart() {
        super.onStart()
        val screen = viewModel.currentScreen.value
        val isActiveQuestionScreen = screen is Screen.Quiz || screen is Screen.MockCompetence || screen is Screen.QuickScanViewer
        if (isActiveQuestionScreen) {
            viewModel.studyTimeTracker.onAppForeground()
        }
    }

    companion object {
        private const val REQ_CODE_IMMEDIATE_UPDATE = 999
    }
}

@Composable
fun HyperionApp(viewModel: ReviewerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val showPaywall by viewModel.showPaywallDialog.collectAsStateWithLifecycle()
    val showDailyCheckIn by viewModel.showDailyCheckInDialog.collectAsStateWithLifecycle()
    var showAccountDialog by remember { mutableStateOf(false) }
    val termsAccepted by viewModel.hasAcceptedTerms.collectAsStateWithLifecycle()
    val showBridge by viewModel.showBridge.collectAsStateWithLifecycle()
    val isRepoWarming by viewModel.isRepoWarming.collectAsStateWithLifecycle()
    val showResultsLoading by viewModel.showResultsLoading.collectAsStateWithLifecycle()

    // Handle drawer opening events emitted from ViewModel
    LaunchedEffect(Unit) {
        viewModel.drawerOpenEvent.collect {
            drawerState.open()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen !is Screen.TermsOnboarding && currentScreen !is Screen.Quiz,
        drawerContent = {
            HyperionDrawerContent(
                viewModel = viewModel,
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                },
                onAccountClick = {
                    scope.launch { drawerState.close() }
                    showAccountDialog = true
                }
            )
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
                transitionSpec = { hyperionPageTransition() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    is Screen.TermsOnboarding -> TermsOnboardingScreen(
                        viewModel = viewModel
                    )
                    is Screen.TermsAndDisclaimer -> TermsAndDisclaimerScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.TrackSelection -> TrackSelectionScreen(
                        viewModel = viewModel
                    )
                    is Screen.Home -> HomeScreen(
                        viewModel = viewModel
                    )
                    is Screen.PracticeCompetenceSelect -> PracticeCompetenceSelectScreen(
                        viewModel = viewModel,
                        isCrpMode = screen.isCrpMode,
                        isOperationalReview = screen.isOperationalReview
                    )
                    is Screen.PracticeConfig -> PracticeConfigScreen(
                        viewModel = viewModel,
                        competenceId = screen.competenceId,
                        isCrpMode = screen.isCrpMode,
                        isOperationalReview = screen.isOperationalReview,
                        isBookmarksMode = screen.isBookmarksMode,
                        isMistakesMode = screen.isMistakesMode
                    )
                    is Screen.Flashcards -> FlashcardScreen(
                        viewModel = viewModel
                    )
                    is Screen.Topics -> TopicScreen(
                        viewModel = viewModel
                    )
                    is Screen.Quiz -> QuizScreen(
                        viewModel = viewModel
                    )
                    is Screen.MockHub -> MockExamHubScreen(
                        viewModel = viewModel
                    )
                    is Screen.MockCompetence -> MockCompetenceScreen(
                        viewModel = viewModel,
                        competenceId = screen.competenceId
                    )
                    is Screen.Results -> ResultsScreen(
                        viewModel = viewModel
                    )
                    is Screen.Vault -> VaultScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.Bookmarks -> BookmarksScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.Search -> SearchScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.History -> HistoryScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.FreePractice -> FreePracticeScreen(
                        viewModel = viewModel
                    )
                    is Screen.Activation -> ActivationScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.HowToAvail -> HowToAvailScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.License -> LicenseScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.QuickScanCompetenceSelect -> QuickScanCompetenceSelectScreen(
                        viewModel = viewModel,
                        track = screen.track
                    )
                    is Screen.QuickScanViewer -> QuickScanViewerScreen(
                        viewModel = viewModel,
                        competenceId = screen.competenceId,
                        track = screen.track
                    )
                    is Screen.SessionReview -> SessionReviewScreen(
                        viewModel = viewModel,
                        sessionId = screen.sessionId
                    )                    
                    is Screen.Store -> StoreScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.Streak -> StreakScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.StudyTime -> StudyTimeScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.About -> AboutScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.PrivacyPolicy -> PrivacyPolicyScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    is Screen.MTestMImport -> MTestMImportScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { viewModel.openDrawer() }
                    )
                    else -> TrackSelectionScreen(
                        viewModel = viewModel
                    )
                }
            }

            AnimatedVisibility(
                visible = showBridge && termsAccepted,
                enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(240, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(150))
            ) {
                TerminalLoadingDialog(
                    isRepoWarming = isRepoWarming,
                    onClose = { viewModel.closeBridge() }
                )
            }

            AnimatedVisibility(
                visible = showResultsLoading,
                enter = fadeIn(tween(200, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(140))
            ) {
                ResultsLoadingDialog(onFinished = { viewModel.dismissResultsLoading() })
            }
            }
        }
    }

    // Back handling: close drawer if open.
    // Wrapped in key(drawerState.isOpen) so when the drawer opens, a fresh BackHandler
    // is composed and registered at the top of OnBackPressedDispatcher's callback stack,
    // guaranteeing it intercepts the back press before any underlying screen BackHandler.
    key(drawerState.isOpen) {
        if (drawerState.isOpen) {
            BackHandler(enabled = true) {
                scope.launch { drawerState.close() }
            }
        }
    }

    if (showPaywall) {
        PaywallDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissPaywallDialog() }
        )
    }

    val isDashboard = currentScreen is Screen.TrackSelection || currentScreen is Screen.Home

    if (showDailyCheckIn && termsAccepted && isDashboard && !showBridge) {
        DailyCheckInDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissDailyCheckInDialog() }
        )
    }

    if (showAccountDialog) {
        AccountSignInDialog(onDismiss = { showAccountDialog = false })
    }
}

@Composable
fun TerminalLoadingDialog(isRepoWarming: Boolean = false, onClose: () -> Unit = {}) {
    val colors = LocalSpaghettiColors.current
    val stepState = remember { mutableIntStateOf(0) }
    var minStep1Reached by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    val isReady = countdown != null

    BackHandler(enabled = true) { /* Intercept back press while indexing */ }

    LaunchedEffect(Unit) {
        delay(350L)
        stepState.intValue = 1
        delay(400L)
        minStep1Reached = true
    }

    val readyForStep2 = minStep1Reached && !isRepoWarming
    LaunchedEffect(readyForStep2) {
        if (readyForStep2) {
            stepState.intValue = 2
            delay(150L)
            stepState.intValue = 3
            delay(150L)
            countdown = 1
            delay(250L)
            onClose()
        }
    }

    val transition = rememberInfiniteTransition(label = "terminalLoading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.88f))
            .clickable(enabled = isReady) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 420.dp)
                .background(colors.surface)
                .border(BorderStroke(1.5.dp, colors.border), RectangleShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colors.accent)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(colors.surface2)
                            .border(1.dp, colors.border),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(rotation)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "HYPERION BRIDGE TERMINAL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.foreground,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isReady) "QUESTION BANKS INDEXED" else "INDEXING QUESTION BANKS...",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface2)
                        .border(1.dp, colors.border)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "> [SYS] MOUNTING LOCAL STORAGE",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.foreground,
                        fontSize = 11.sp
                    )
                    if (stepState.intValue >= 1) {
                        Text(
                            text = "> [DB] WARMING QUESTION REPOSITORY...",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = colors.foreground,
                            fontSize = 11.sp
                        )
                    }
                    if (stepState.intValue >= 2) {
                        Text(
                            text = "> [SEC] HARDWARE CREDENTIALS VERIFIED",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = colors.success,
                            fontSize = 11.sp
                        )
                    }
                    if (stepState.intValue >= 3) {
                        Text(
                            text = "> [OK] INTERFACE READY",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = colors.success,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RectangleShape,
                    color = if (isReady) colors.success.copy(alpha = 0.15f) else colors.surface2,
                    border = BorderStroke(1.dp, if (isReady) colors.success else colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isReady) "SYSTEM READY · OPENING IN ${countdown ?: 3}..." else "PLEASE WAIT · SYSTEM INITIALIZING",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isReady) colors.success else colors.muted,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
