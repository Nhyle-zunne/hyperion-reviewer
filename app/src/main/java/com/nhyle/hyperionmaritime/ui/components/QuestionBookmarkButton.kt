package com.nhyle.hyperionmaritime.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * A shared question bookmark control used by Practice All Questions and
 * Quick Scan. Callers supply the state from ReviewerViewModel.bookmarkedIds
 * and toggle the canonical Question.id via ReviewerViewModel.toggleBookmark.
 * No mode-specific bookmark collection or question copy is created.
 */
@Composable
fun QuestionBookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .testTag("question_bookmark_button")
            .semantics {
                stateDescription = if (isBookmarked) "Saved" else "Not saved"
            }
    ) {
        Icon(
            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            contentDescription = "Bookmark",
            tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
