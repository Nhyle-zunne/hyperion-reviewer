package com.nhyle.hyperionmaritime.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Standardized universal 3-line hamburger drawer button for Hyperion.
 * Resolves icon tint dynamically to MaterialTheme.colorScheme.primary (#00E676 in Dark, #008744 / #15803D in Light).
 */
@Composable
fun HyperionDrawerButton(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onOpenDrawer,
        modifier = modifier
            .size(40.dp)
            .testTag("topbar_menu_button")
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Open Navigation Drawer",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}
