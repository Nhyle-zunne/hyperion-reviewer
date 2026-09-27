package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.ui.components.HyperionDrawerContent
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ActivationScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.FlashcardScreen
import com.example.ui.screens.FreePracticeScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MockCompetenceScreen
import com.example.ui.screens.MockExamHubScreen
import com.example.ui.screens.PracticeCompetenceSelectScreen
import com.example.ui.screens.PracticeConfigScreen
import com.example.ui.screens.PracticeQuizScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.TermsAndDisclaimerScreen
import com.example.ui.screens.TermsOnboardingScreen
import com.example.ui.screens.TopicScreen
import com.example.ui.screens.TrackSelectionScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ReviewerViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ReviewerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReviewerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ReviewerApp(viewModel: ReviewerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.drawerOpenEvent.collect {
            drawerState.open()
        }
    }

    if (currentScreen is Screen.TermsOnboarding) {
        TermsOnboardingScreen(onAcceptTerms = { viewModel.acceptTerms() })
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                HyperionDrawerContent(
                    viewModel = viewModel,
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                when (screen) {
                    is Screen.TermsOnboarding -> TermsOnboardingScreen(onAcceptTerms = { viewModel.acceptTerms() })
                    is Screen.TermsAndDisclaimer -> TermsAndDisclaimerScreen(onBackClick = { viewModel.navigateTo(Screen.Home) })
                    is Screen.TrackSelection -> TrackSelectionScreen(viewModel = viewModel)
                    is Screen.Home -> HomeScreen(viewModel = viewModel)
                    is Screen.PracticeCompetenceSelect -> PracticeCompetenceSelectScreen(viewModel = viewModel)
                    is Screen.PracticeConfig -> PracticeConfigScreen(viewModel = viewModel, competenceId = screen.competenceId)
                    is Screen.Flashcards -> FlashcardScreen(viewModel = viewModel)
                    is Screen.Topics -> TopicScreen(viewModel = viewModel)
                    is Screen.Quiz -> PracticeQuizScreen(viewModel = viewModel)
                    is Screen.MockHub -> MockExamHubScreen(viewModel = viewModel)
                    is Screen.MockCompetence -> MockCompetenceScreen(viewModel = viewModel, competenceId = screen.competenceId)
                    is Screen.Results -> ResultsScreen(viewModel = viewModel)
                    is Screen.Vault -> VaultScreen(viewModel = viewModel)
                    is Screen.Bookmarks -> BookmarksScreen(viewModel = viewModel)
                    is Screen.Search -> SearchScreen(viewModel = viewModel)
                    is Screen.History -> HistoryScreen(viewModel = viewModel)
                    is Screen.FreePractice -> FreePracticeScreen(viewModel = viewModel)
                    is Screen.Activation -> ActivationScreen(viewModel = viewModel)
                    is Screen.About -> AboutScreen(viewModel = viewModel)
                }
            }
        }
    }
}
