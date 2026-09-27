package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FailRed
import com.example.ui.theme.MaritimeGoldBright
import com.example.ui.theme.SeaCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaritimeTopBar(
    title: String,
    subtitle: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onBookmarksClick: (() -> Unit)? = null,
    onVaultClick: (() -> Unit)? = null,
    onHistoryClick: (() -> Unit)? = null,
    onLegalClick: (() -> Unit)? = null,
    bookmarkCount: Int = 0,
    vaultCount: Int = 0
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
            },
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaritimeGoldBright
                        )
                    }
                } else if (onMenuClick != null) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.testTag("topbar_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open navigation drawer",
                            tint = MaritimeGoldBright
                        )
                    }
                }
            },
            actions = {
                if (showBackButton && onMenuClick != null) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.testTag("topbar_menu_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Navigation Menu",
                            tint = MaritimeGoldBright
                        )
                    }
                }

                if (onLegalClick != null) {
                    IconButton(onClick = onLegalClick) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Legal & Terms",
                            tint = MaritimeGoldBright
                        )
                    }
                }

                if (onSearchClick != null) {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onHistoryClick != null) {
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Session History",
                            tint = MaritimeGoldBright
                        )
                    }
                }

                if (onBookmarksClick != null) {
                    IconButton(onClick = onBookmarksClick) {
                        if (bookmarkCount > 0) {
                            BadgedBox(badge = {
                                Badge(containerColor = MaritimeGoldBright, contentColor = Color.Black) {
                                    Text("$bookmarkCount")
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Bookmarks",
                                    tint = MaritimeGoldBright
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Bookmarks",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (onVaultClick != null) {
                    IconButton(onClick = onVaultClick) {
                        if (vaultCount > 0) {
                            BadgedBox(badge = {
                                Badge(containerColor = FailRed, contentColor = Color.White) {
                                    Text("$vaultCount")
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error Vault",
                                    tint = FailRed
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error Vault",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
