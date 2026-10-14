package com.nhyle.hyperionmaritime.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nhyle.hyperionmaritime.viewmodel.ReviewerViewModel

@Composable
fun QuizScreen(
    viewModel: ReviewerViewModel,
    modifier: Modifier = Modifier
) {
    PracticeQuizScreen(viewModel = viewModel, modifier = modifier)
}
