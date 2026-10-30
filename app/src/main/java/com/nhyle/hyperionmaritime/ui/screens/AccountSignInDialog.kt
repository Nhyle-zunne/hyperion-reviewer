package com.nhyle.hyperionmaritime.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf("") }
    val danger = Color(0xFFB91C1C)

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
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (account.isAccountDeleted) "ACCOUNT DELETED" else "HYPERION ACCOUNT",
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.foreground,
                    fontSize = 18.sp,
                    letterSpacing = 0.7.sp
                )
                if (account.isAccountDeleted) {
                    Text(
                        text = "Your previous Hyperion account was deleted. You can sign in again to create a new account, but deleted website licenses and account data cannot be restored or transferred.",
                        color = colors.muted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                } else {
                    Text(
                        text = "Sign in with the same Google account you use on hyperionmaritime.org to sync active website licenses with this app. Sign-in is optional, and Google Play purchases continue to work without it.",
                        color = colors.muted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }

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

                if (account.isDeveloperAdminAccess) {
                    Text(
                        text = "DEVELOPER ACCESS · ALL FIVE TRACKS UNLOCKED",
                        color = colors.accent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.7.sp
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
                
                if (!account.email.isNullOrBlank() && !account.isAccountDeleted) {
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

                    OutlinedButton(
                        onClick = {
                            showDeleteConfirmation = !showDeleteConfirmation
                            deleteConfirmation = ""
                        },
                        enabled = !account.isLoading,
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, danger),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = danger),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text(
                            text = if (showDeleteConfirmation) "CANCEL ACCOUNT DELETION" else "DELETE ACCOUNT",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.7.sp
                        )
                    }

                    if (showDeleteConfirmation) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .border(BorderStroke(1.dp, danger))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Text(
                                text = "PERMANENT ACCOUNT DELETION",
                                color = danger,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "This permanently deletes your Hyperion account and its associated account data, including saved learning progress, payment history, website licenses, and Google Play licenses linked to this Hyperion account. This cannot be undone.",
                                color = colors.foreground,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = "ACTIVE LICENSE WARNING: Any active website/PWA license linked to this account will be removed, and account-linked access in the Android app will be removed too.",
                                color = danger,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = "GOOGLE PLAY BILLING IS SEPARATE: Deleting your Hyperion account does NOT cancel a Google Play subscription. It may continue renewing and charging your Google account until you cancel it separately. The Play purchase may remain usable on this Android device while active. A purchase created with Hyperion account binding may not be linkable to a different Hyperion account after deletion.",
                                color = colors.foreground,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://play.google.com/store/account/subscriptions")
                                            )
                                        )
                                    } catch (_: Exception) {
                                        // The user can still open Play Store and manage subscriptions manually.
                                    }
                                },
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, colors.border),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.foreground),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Text(
                                    text = "MANAGE GOOGLE PLAY SUBSCRIPTIONS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.2.sp
                                )
                            }
                            Text(
                                text = "To continue, type DELETE exactly below.",
                                color = colors.muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = deleteConfirmation,
                                onValueChange = { deleteConfirmation = it },
                                enabled = !account.isLoading,
                                singleLine = true,
                                label = { Text("Type DELETE") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showDeleteConfirmation = false
                                        deleteConfirmation = ""
                                    },
                                    enabled = !account.isLoading,
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, colors.border),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.foreground),
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            SupabaseAccountManager.deleteAccount(context.applicationContext)
                                            if (SupabaseAccountManager.state.value.isAccountDeleted) {
                                                showDeleteConfirmation = false
                                                deleteConfirmation = ""
                                            }
                                        }
                                    },
                                    enabled = !account.isLoading && deleteConfirmation == "DELETE",
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = danger,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    if (account.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = if (account.isLoading) "DELETING…" else "PERMANENTLY DELETE",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.1.sp
                                    )
                                }
                            }
                        }
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
