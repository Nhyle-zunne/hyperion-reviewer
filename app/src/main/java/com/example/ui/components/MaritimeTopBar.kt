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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
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
    isSearchLocked: Boolean = false,
    onBookmarksClick: (() -> Unit)? = null,
    onVaultClick: (() -> Unit)? = null,
    onHistoryClick: (() -> Unit)? = null,
    onLegalClick: (() -> Unit)? = null,
    bookmarkCount: Int = 0,
    vaultCount: Int = 0,
    onClearAllClick: (() -> Unit)? = null
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val topBarBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFF0F172A)
    val titleColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White
    val subtitleColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF94A3B8)
    val iconTint = if (isDark) MaterialTheme.colorScheme.primary else Color.White
    val borderStroke = if (isDark) {
        androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    }

    Surface(
        color = topBarBg,
        border = borderStroke
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = subtitleColor,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = iconTint
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
                            tint = iconTint
                        )
                    }
                }
            },
            actions = {
                if (onSearchClick != null) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.testTag("topbar_search_button")
                    ) {
                        if (isSearchLocked) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaritimeGoldBright,
                                        contentColor = Color(0xFF0F172A),
                                        modifier = Modifier.size(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            modifier = Modifier.size(9.dp),
                                            tint = Color(0xFF0F172A)
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search (Locked)",
                                    tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color(0xFF94A3B8)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                if (onHistoryClick != null) {
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Session History",
                            tint = iconTint
                        )
                    }
                }

                if (onBookmarksClick != null) {
                    IconButton(onClick = onBookmarksClick) {
                        if (bookmarkCount > 0) {
                            BadgedBox(badge = {
                                Badge(containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0284C7), contentColor = Color.White) {
                                    Text("$bookmarkCount")
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Bookmarks",
                                    tint = iconTint
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Bookmarks",
                                tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                if (onClearAllClick != null) {
                    IconButton(onClick = onClearAllClick) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Clear All Sessions",
                            tint = FailRed
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = topBarBg,
                titleContentColor = titleColor
            )
        )
    }
}
