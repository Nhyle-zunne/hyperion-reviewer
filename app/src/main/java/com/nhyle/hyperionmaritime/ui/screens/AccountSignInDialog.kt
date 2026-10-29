package com.nhyle.hyperionmaritime.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhyle.hyperionmaritime.data.auth.SupabaseAccountManager
import com.nhyle.hyperionmaritime.ui.motion.HyperionDialog
import com.nhyle.hyperionmaritime.ui.theme.LocalSpaghettiColors
import kotlinx.coroutines.launch

@Composable
fun AccountSignInDialog(onDismiss: () -> Unit) {
    val colors = LocalSpaghettiColors.current
    val context = LocalContext.current
    val account by SupabaseAccountManager.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    HyperionDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RectangleShape,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "HYPERION ACCOUNT",
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    fontSize = 18.sp,
                    letterSpacing = 0.7.sp
                )
                Text(
                    text = "Sign in with the same Google account you use on hyperionmaritime.org to sync active website licenses with this app. Sign-in is optional, and Google Play purchases continue to work without it.",
                    color = colors.muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                if (!account.email.isNullOrBlank()) {
                    Text(
                        text = "SIGNED IN AS",
                        color = colors.accent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = account.email.orEmpty(),
                        color = colors.foreground,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (account.activePlaySubscriptionsLinked > 0) {
                    Text(
                        text = "GOOGLE PLAY SUBSCRIPTIONS LINKED: ${account.activePlaySubscriptionsLinked}",
                        color = colors.accent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.7.sp
                    )
                }

                account.message?.takeIf { it.isNotBlank() }?.let { status ->
                    Text(
                        text = status,
                        color = colors.muted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            if (account.email.isNullOrBlank()) {
                                val activity = context.findActivity()
                                if (activity != null) {
                                    SupabaseAccountManager.signInWithGoogle(activity)
                                }
                            } else {
                                SupabaseAccountManager.refreshWebsiteLicenses(context.applicationContext)
                            }
                        }
                    },
                    enabled = !account.isLoading,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.accentForeground
                    ),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    if (account.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colors.accentForeground,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                    }
                    Text(
                        text = when {
                            account.isLoading -> "PLEASE WAIT…"
                            account.email.isNullOrBlank() -> "CONTINUE WITH GOOGLE"
                            else -> "SYNC WEBSITE LICENSES"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.7.sp
                    )
                }

                if (!account.email.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { SupabaseAccountManager.signOut(context.applicationContext) },
                        enabled = !account.isLoading,
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.foreground),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text(
                            text = "SIGN OUT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.7.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "CLOSE",
                            color = colors.muted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
