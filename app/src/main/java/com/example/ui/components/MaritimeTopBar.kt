package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalSpaghettiColors

/**
 * Editorial MIAN "Spaghetti" Top App Bar:
 * Flat surface, 3dp bottom horizontal rule, Heavy Grotesk typography, strict left alignment.
 */
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
    val colors = LocalSpaghettiColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.foreground,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.muted,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                if (showBackButton) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("topbar_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.foreground
                        )
                    }
                } else if (onMenuClick != null) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.testTag("topbar_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = colors.foreground
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
                                        containerColor = colors.accent,
                                        contentColor = colors.accentForeground,
                                        modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            modifier = Modifier.size(8.dp),
                                            tint = colors.accentForeground
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search (Locked)",
                                    tint = colors.muted
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = colors.foreground
                            )
                        }
                    }
                }

                if (onHistoryClick != null) {
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Session History",
                            tint = colors.foreground
                        )
                    }
                }

                if (onBookmarksClick != null) {
                    IconButton(onClick = onBookmarksClick) {
                        if (bookmarkCount > 0) {
                            BadgedBox(badge = {
                                Badge(
                                    containerColor = colors.accent,
                                    contentColor = colors.accentForeground,
                                    modifier = Modifier.clip(RoundedCornerShape(2.dp))
                                ) {
                                    Text(
                                        text = "$bookmarkCount",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Bookmarks",
                                    tint = colors.foreground
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Bookmarks",
                                tint = colors.foreground
                            )
                        }
                    }
                }

                if (onVaultClick != null) {
                    IconButton(onClick = onVaultClick) {
                        if (vaultCount > 0) {
                            BadgedBox(badge = {
                                Badge(
                                    containerColor = colors.accent,
                                    contentColor = colors.accentForeground,
                                    modifier = Modifier.clip(RoundedCornerShape(2.dp))
                                ) {
                                    Text(
                                        text = "$vaultCount",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Vault",
                                    tint = colors.foreground
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Vault",
                                tint = colors.foreground
                            )
                        }
                    }
                }

                if (onClearAllClick != null) {
                    IconButton(onClick = onClearAllClick) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Clear All",
                            tint = colors.accent
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colors.surface,
                titleContentColor = colors.foreground,
                navigationIconContentColor = colors.foreground,
                actionIconContentColor = colors.foreground
            )
        )

        // 3dp Bottom Rule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(colors.border)
        )
    }
}
