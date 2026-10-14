package com.nhyle.hyperionmaritime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import com.nhyle.hyperionmaritime.ui.components.DailyCheckInDialog
import com.nhyle.hyperionmaritime.ui.components.HyperionDrawerContent
import com.nhyle.hyperionmaritime.ui.components.PaywallDialog
import com.nhyle.hyperionmaritime.ui.screens.*
import com.nhyle.hyperionmaritime.ui.store.StoreScreen
import com.nhyle.hyperionmaritime.ui.theme.AppTheme
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel
import com.nhyle.hyperionmaritime.viewmodel.Screen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ReviewerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MaritimeQuestionBank.init(applicationContext)
        LicenseManager.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            AppTheme(isDarkMode = isDarkMode) {
                HyperionApp(viewModel = viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.studyTimeTracker.onAppBackground()
    }

    override fun onStart() {
        super.onStart()
        val screen = viewModel.currentScreen.value
        val isActiveQuestionScreen = screen is Screen.Quiz || screen is Screen.MockCompetence
        if (isActiveQuestionScreen) {
            viewModel.studyTimeTracker.onAppForeground()
        }
    }
}

@Composable
fun HyperionApp(viewModel: ReviewerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val showPaywall by viewModel.showPaywallDialog.collectAsState()
    val showDailyCheckIn by viewModel.showDailyCheckInDialog.collectAsState()

    // Handle drawer opening events emitted from ViewModel
    LaunchedEffect(Unit) {
        viewModel.drawerOpenEvent.collect {
            drawerState.open()
        }
    }

    // Back handling: close drawer if open, else delegate to ViewModel
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen !is Screen.TermsOnboarding && currentScreen !is Screen.Quiz,
        drawerContent = {
            HyperionDrawerContent(
                viewModel = viewModel,
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            when (val screen = currentScreen) {
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
                    isOperationalReview = screen.isOperationalReview
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
                is Screen.ExamGuidelines -> ExamGuidelinesScreen(
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
            }
        }
    }

    if (showPaywall) {
        PaywallDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissPaywallDialog() }
        )
    }

    val termsAccepted by viewModel.hasAcceptedTerms.collectAsState()
    val isDashboard = currentScreen is Screen.TrackSelection || currentScreen is Screen.Home

    if (showDailyCheckIn && termsAccepted && isDashboard) {
        DailyCheckInDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissDailyCheckInDialog() }
        )
    }
}
