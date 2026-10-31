package com.nhyle.hyperionmaritime.data.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nhyle.hyperionmaritime.BuildConfig
import com.nhyle.hyperionmaritime.data.billing.BillingManager
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.sync.LearningProgressSyncAdapter
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.time.OffsetDateTime

data class SupabaseAccountState(
    val email: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val activeWebsiteLicenses: Int = 0,
    val activePlaySubscriptionsLinked: Int = 0,
    val isDeveloperAdminAccess: Boolean = false,
    val isAccountDeleted: Boolean = false
)

private data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val expiresAtMillis: Long
)

/**
 * The Google Play entitlement table is an optional extension until its backend migration
 * is released. Treat only a PostgREST "table not found" 404 for that exact table as empty;
 * permission, session, and network errors must still fail closed.
 */
internal fun isGooglePlayAccountLinkConflict(error: Throwable): Boolean =
    error is IOException && error.message.orEmpty().contains("Supabase request failed (409):", ignoreCase = true)

private val learningProgressOwnerLock = Any()

/**
 * Durably claims this device's offline study data for one Hyperion account before
 * any cloud read/write begins. A synchronous commit prevents process death from
 * losing the ownership marker after private progress has already been uploaded.
 */
internal fun claimLearningProgressOwner(
    preferences: SharedPreferences,
    userId: String
): Boolean {
    if (userId.isBlank()) return false
    return synchronized(learningProgressOwnerLock) {
        val currentOwner = preferences.getString("owner_user_id", null)
        when {
            currentOwner == userId -> true
            !currentOwner.isNullOrBlank() -> false
            else -> preferences.edit().putString("owner_user_id", userId).commit()
        }
    }
}

internal fun isMissingGooglePlayEntitlementsTable(error: Throwable): Boolean {
    if (error !is IOException) return false
    val message = error.message.orEmpty()
    return message.contains("Supabase request failed (404):", ignoreCase = true) &&
        message.contains("google_play_entitlements", ignoreCase = true) &&
        (message.contains("table", ignoreCase = true) || message.contains("schema cache", ignoreCase = true))
}

/**
 * Optional native Google sign-in against Hyperion's existing Supabase Auth project.
 * Website entitlements are loaded with the user's JWT and remain protected by database RLS.
 */
object SupabaseAccountManager {
    private const val TAG = "HyperionGoogleSignIn"
    private const val SESSION_PREFS = "hyperion_supabase_session"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000
    private const val SESSION_REFRESH_MARGIN_MS = 60_000L

    private val _state = MutableStateFlow(SupabaseAccountState())
    val state: StateFlow<SupabaseAccountState> = _state.asStateFlow()
    private val learningProgressSyncMutex = Mutex()
    private const val LEARNING_PROGRESS_BACKGROUND_COOLDOWN_MS = 5 * 60 * 1000L
    private const val LEARNING_PROGRESS_LAST_SUCCESS_KEY = "last_successful_sync_at"


    suspend fun signInWithGoogle(activity: Activity) {
        _state.value = _state.value.copy(isLoading = true, message = null)
        try {
            val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
            if (webClientId.isBlank()) {
                throw IllegalStateException(
                    "Google sign-in needs setup for this build. Configure HYPERION_GOOGLE_WEB_CLIENT_ID with the Web OAuth client ID configured for Google in Supabase, then rebuild."
                )
            }

            val rawNonce = randomNonce()

            // This screen exposes a persistent "Continue with Google" button, so use
            // Google's explicit button flow directly. The dismissible Credential Manager
            // bottom sheet is a separate optional UI and should not be auto-retried when
            // users dismiss it.
            val explicitSignInOption = GetSignInWithGoogleOption.Builder(webClientId)
                .setNonce(sha256Hex(rawNonce))
                .build()
            val explicitSignInRequest = GetCredentialRequest.Builder()
                .addCredentialOption(explicitSignInOption)
                .build()

            val credentialManager = CredentialManager.create(activity)
            val credentialResponse = credentialManager.getCredential(
                context = activity,
                request = explicitSignInRequest
            )
            val credential = credentialResponse.credential as? CustomCredential
                ?: throw IllegalStateException("Google sign-in did not return a Google ID token.")
            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                throw IllegalStateException("Google sign-in returned an unsupported credential.")
            }

            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val appContext = activity.applicationContext
            val session = withContext(Dispatchers.IO) {
                exchangeGoogleIdToken(idToken, rawNonce).also {
                    LicenseManager.setCurrentAccountUserId(appContext, it.userId)
                    storeSession(appContext, it)
                    recordAccountActivity(it)
                }
            }
            _state.value = SupabaseAccountState(
                email = session.email.ifBlank { null },
                isLoading = true,
                message = "Signed in. Checking your website licenses…"
            )
            syncAndUpdateState(activity.applicationContext, session)
        } catch (e: GetCredentialCancellationException) {
            // Credential Manager can surface this for both a user dismissal and certain
            // authorization/configuration failures. Keep tokens and credential payloads
            // out of logs; record only the exception type/message and stack for diagnosis.
            Log.w(TAG, "Google Credential Manager cancellation: ${e.javaClass.simpleName}: ${e.message.orEmpty()}", e)
            _state.value = _state.value.copy(
                isLoading = false,
                message = "Google sign-in was interrupted. Please try again. If this keeps happening, send support the Android Logcat error."
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Credential Manager error: ${e.javaClass.simpleName}: ${e.message.orEmpty()}", e)
            _state.value = _state.value.copy(
                isLoading = false,
                message = e.message?.takeIf { it.isNotBlank() } ?: "Google sign-in failed. Please try again."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed: ${e.javaClass.simpleName}: ${e.message.orEmpty()}", e)
            _state.value = _state.value.copy(
                isLoading = false,
                message = e.message?.takeIf { it.isNotBlank() } ?: "Unable to sign in right now. Please try again."
            )
        }
    }

    suspend fun restoreSessionAndSync(context: Context) {
        val appContext = context.applicationContext
        val saved = try {
            withContext(Dispatchers.IO) { readSession(appContext) }
        } catch (_: Exception) {
            null
        }
        if (saved == null) {
            LicenseManager.setCurrentAccountUserId(appContext, null)
            _state.value = SupabaseAccountState()
            return
        }

        LicenseManager.setCurrentAccountUserId(appContext, saved.userId)
        _state.value = SupabaseAccountState(
            email = saved.email.ifBlank { null },
            isLoading = false
        )
        try {
            val session = withContext(Dispatchers.IO) {
                getFreshSession(appContext, saved).also(::recordAccountActivity)
            }
            syncAndUpdateState(appContext, session)
        } catch (_: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                message = "Could not refresh website licenses. Connect to the internet and tap Sync to retry."
            )
        }
    }

    suspend fun refreshWebsiteLicenses(context: Context) {
        val appContext = context.applicationContext
        _state.value = _state.value.copy(isLoading = true, message = null)
        try {
            val saved = withContext(Dispatchers.IO) { readSession(appContext) }
                ?: throw IllegalStateException("Sign in with Google first to sync your website licenses.")
            val session = withContext(Dispatchers.IO) {
                getFreshSession(appContext, saved).also(::recordAccountActivity)
            }
            syncAndUpdateState(appContext, session)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                message = e.message?.takeIf { it.isNotBlank() } ?: "Website license sync failed. Please try again."
            )
        }
    }

    /**
     * After a successful Google Play purchase, link it to the current Hyperion account
     * when the user has already signed in. Account linking remains optional: when there
     * is no saved Hyperion session, the local Play purchase is left untouched and the
     * user can link it later by signing in or tapping Sync Website Licenses.
     */
    suspend fun syncAfterPlayPurchaseIfSignedIn(context: Context) {
        val appContext = context.applicationContext
        val saved = withContext(Dispatchers.IO) { readSession(appContext) } ?: return
        syncAndUpdateState(appContext, saved)
    }

    fun signOut(context: Context) {
        try {
            sessionPreferences(context.applicationContext).edit().clear().apply()
        } catch (_: Exception) {
            // Still clear the separately stored account entitlement cache.
        }
        LicenseManager.clearAccountEntitlements(context.applicationContext)
        _state.value = SupabaseAccountState(
            message = "Signed out. Your study history remains saved on this device and is not transferred to another Hyperion account. Google Play purchases remain available."
        )
    }

    /**
     * Permanently deletes the authenticated Hyperion account through the same guarded
     * Edge Function used by the website. The server checks pending payments and removes
     * account-owned records before deleting the Auth user. Google Play billing is external
     * to Hyperion and is intentionally not cancelled or cleared from the device.
     */
    suspend fun deleteAccount(context: Context) {
        val appContext = context.applicationContext
        _state.value = _state.value.copy(isLoading = true, message = null, isAccountDeleted = false)
        try {
            val saved = withContext(Dispatchers.IO) { readSession(appContext) }
                ?: throw IllegalStateException("Your Hyperion session is missing. Sign in again before deleting your account.")
            val session = withContext(Dispatchers.IO) { getFreshSession(appContext, saved) }
            val responseBody = withContext(Dispatchers.IO) {
                requestText(
                    method = "POST",
                    url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/delete-account",
                    accessToken = session.accessToken,
                    jsonBody = JSONObject().put("confirmation", "DELETE")
                )
            }
            val response = JSONObject(responseBody)
            if (!response.optBoolean("success", false)) {
                throw IOException(
                    response.optString("error").takeIf { it.isNotBlank() }
                        ?: "Account deletion could not be confirmed. Please try again or contact support."
                )
            }

            withContext(Dispatchers.IO) {
                sessionPreferences(appContext).edit().clear().commit()
                // Clear only Hyperion account grants. Preserve Google Play Billing's
                // locally verified subscription cache because Play subscriptions are
                // owned and managed separately by the user's Google Play account.
                LicenseManager.clearAccountEntitlements(appContext)
            }
            _state.value = SupabaseAccountState(
                message = "Your Hyperion account and linked server-side account data have been deleted. Study history stored locally on this device remains here and will not be transferred to another Hyperion account. Google Play subscriptions are not cancelled and may continue billing until you cancel them in Google Play.",
                isAccountDeleted = true
            )
        } catch (error: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                message = error.message?.takeIf { it.isNotBlank() }
                    ?: "Unable to delete your account. Please try again or contact support.",
                isAccountDeleted = false
            )
        }
    }

    /**
     * Keep the existing account-retention activity timestamp accurate for signed-in
     * Android users as well as website users. The server throttles writes to one per
     * account every 15 minutes. This is best-effort and never blocks sign-in or study.
     */
    private fun recordAccountActivity(session: SupabaseSession) {
        try {
            requestText(
                method = "POST",
                url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/record-user-activity",
                accessToken = session.accessToken,
                jsonBody = JSONObject()
            )
        } catch (_: Exception) {
            // Activity reporting is non-critical; keep account and study access available.
        }
    }

    /**
     * Synchronize Android's offline-first study records with the same protected snapshot
     * used by the website/PWA. A device is bound to its first successfully synced account:
     * switching accounts pauses study sync rather than uploading one person's local data
     * into another person's account.
     */
    suspend fun syncLearningProgressInBackground(context: Context) {
        val appContext = context.applicationContext
        val saved = withContext(Dispatchers.IO) { readSession(appContext) } ?: return
        withContext(Dispatchers.IO) {
            val fresh = getFreshSession(appContext, saved)
            syncLearningProgressNow(appContext, fresh, enforceBackgroundCooldown = true)
        }
    }

    private suspend fun syncLearningProgressNow(
        context: Context,
        session: SupabaseSession,
        enforceBackgroundCooldown: Boolean = false
    ): String = learningProgressSyncMutex.withLock {
        val prefs = context.getSharedPreferences("hyperion_learning_sync", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastSuccess = prefs.getLong(LEARNING_PROGRESS_LAST_SUCCESS_KEY, 0L)
        if (enforceBackgroundCooldown && lastSuccess > 0L &&
            now - lastSuccess < LEARNING_PROGRESS_BACKGROUND_COOLDOWN_MS
        ) {
            return@withLock "Study progress was synchronized recently."
        }

        val result = syncLearningProgressUnlocked(context, session)
        // Persist the cooldown only after the server sync and local snapshot application
        // both completed successfully. Manual account syncs also suppress a duplicate
        // background worker immediately following sign-in or a user-initiated refresh.
        prefs.edit().putLong(LEARNING_PROGRESS_LAST_SUCCESS_KEY, System.currentTimeMillis()).commit()
        result
    }

    private suspend fun syncLearningProgressUnlocked(context: Context, session: SupabaseSession): String {
        val prefs = context.getSharedPreferences("hyperion_learning_sync", Context.MODE_PRIVATE)
        if (!claimLearningProgressOwner(prefs, session.userId)) {
            throw IOException(
                "Study progress sync is paused because this device's local study data is linked to another Hyperion account, or its ownership marker could not be saved. Sign in with the original account to keep the data private."
            )
        }

        val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/learning-progress"
        var readResponse = JSONObject(requestText(
            method = "POST",
            url = endpoint,
            accessToken = session.accessToken,
            jsonBody = JSONObject().put("action", "read")
        ))
        var remoteSnapshot = readResponse.optJSONObject("data")
        var expectedUpdatedAt: Any? = readResponse.opt("updated_at")
            ?.takeUnless { it == JSONObject.NULL }
        var saved = false
        var cloudSnapshotApplied = false

        // Avoid a database UPDATE when the native merge produced no data changes.
        // The Edge Function sanitizes the snapshot on every read, so the already-read
        // remote snapshot is safe to apply locally when its storage is unchanged.
        // mergeNativeIntoSnapshot mutates the supplied JSONObject, including its nested
        // storage object. Capture the actual server value first and merge into a deep copy
        // so the no-op comparison cannot accidentally compare the object to itself.
        val remoteStorageJson = remoteSnapshot?.optJSONObject("storage")?.toString()
        val initialMergedSnapshot = LearningProgressSyncAdapter.mergeNativeIntoSnapshot(
            context,
            remoteSnapshot?.let { JSONObject(it.toString()) }
        )
        val mergedStorageJson = initialMergedSnapshot.optJSONObject("storage")?.toString()
        if (remoteSnapshot != null && remoteStorageJson == mergedStorageJson) {
            LearningProgressSyncAdapter.applyCloudSnapshot(context, remoteSnapshot)
            return "Study progress is already up to date across your signed-in devices."
        }

        // A version conflict means another signed-in device changed the snapshot
        // since our read. Re-read and merge again rather than replacing that work.
        for (attempt in 0 until 3) {
            val mergedSnapshot = if (attempt == 0) {
                initialMergedSnapshot
            } else {
                LearningProgressSyncAdapter.mergeNativeIntoSnapshot(
                    context,
                    remoteSnapshot?.let { JSONObject(it.toString()) }
                )
            }
            val writeBody = JSONObject()
                .put("action", "write")
                .put("data", mergedSnapshot)
                .put("expected_updated_at", expectedUpdatedAt ?: JSONObject.NULL)
                .put("return_data", true)

            try {
                val writeResponse = JSONObject(requestText(
                    method = "POST",
                    url = endpoint,
                    accessToken = session.accessToken,
                    jsonBody = writeBody
                ))
                if (!writeResponse.optBoolean("ok", false)) {
                    throw IOException("The study progress service did not confirm the synchronization.")
                }
                val sanitizedSnapshot = writeResponse.optJSONObject("data")
                if (sanitizedSnapshot != null) {
                    LearningProgressSyncAdapter.applyCloudSnapshot(context, sanitizedSnapshot)
                    cloudSnapshotApplied = true
                }
                saved = true
                break
            } catch (error: IOException) {
                val isVersionConflict = error.message.orEmpty().contains(
                    "Supabase request failed (409):",
                    ignoreCase = true
                )
                if (!isVersionConflict || attempt == 2) throw error

                readResponse = JSONObject(requestText(
                    method = "POST",
                    url = endpoint,
                    accessToken = session.accessToken,
                    jsonBody = JSONObject().put("action", "read")
                ))
                remoteSnapshot = readResponse.optJSONObject("data")
                expectedUpdatedAt = readResponse.opt("updated_at")
                    ?.takeUnless { it == JSONObject.NULL }
            }
        }
        if (!saved) {
            throw IOException("Study progress changed repeatedly on another device. Your local study data is safe and will retry.")
        }

        // Updated servers return the already-sanitized snapshot with successful writes.
        // Keep the authenticated read-back fallback for older server deployments.
        if (!cloudSnapshotApplied) {
            val verifiedResponse = JSONObject(requestText(
                method = "POST",
                url = endpoint,
                accessToken = session.accessToken,
                jsonBody = JSONObject().put("action", "read")
            ))
            LearningProgressSyncAdapter.applyCloudSnapshot(context, verifiedResponse.optJSONObject("data"))
        }
        return "Study progress synchronized across your signed-in devices."
    }

    private suspend fun syncAndUpdateState(context: Context, initialSession: SupabaseSession) {
        // A previous admin verification must not survive an unsuccessful or changed-account recheck.
        LicenseManager.setVerifiedDeveloperAdminAccess(context, initialSession.userId, false)
        _state.value = _state.value.copy(
            email = initialSession.email.ifBlank { null },
            isLoading = true,
            isDeveloperAdminAccess = false
        )
        try {
            val result = withContext(Dispatchers.IO) {
                val freshSession = getFreshSession(context, initialSession)
                val isDeveloperAdmin = verifyDeveloperAdminAccess(freshSession) == true
                LicenseManager.setVerifiedDeveloperAdminAccess(context, freshSession.userId, isDeveloperAdmin)
                val playLinkResult = syncGooglePlayPurchases(context, freshSession)
                val licenseCount = syncEntitlements(context, freshSession)
                val studyProgressMessage = try {
                    syncLearningProgressNow(context, freshSession)
                } catch (progressError: Exception) {
                    progressError.message?.takeIf { it.isNotBlank() }
                        ?: "Study progress sync is pending. Your Android study data remains saved locally."
                }
                AccountSyncResult(freshSession, licenseCount, playLinkResult, isDeveloperAdmin, studyProgressMessage)
            }
            val session = result.session
            val count = result.licenseCount
            val playLinks = result.playLinks
            val messageParts = mutableListOf<String>()
            messageParts += if (result.isDeveloperAdmin) {
                "Developer access verified by Hyperion. All five tracks are unlocked."
            } else if (count == 0) {
                "No active website or linked Google Play license was found for this account."
            } else {
                "Synced $count active license track${if (count == 1) "" else "s"}."
            }
            if (playLinks.activeCount > 0) {
                messageParts += "Verified ${playLinks.activeCount} Google Play subscription${if (playLinks.activeCount == 1) "" else "s"} for this account."
            }
            if (playLinks.accountMismatch) {
                messageParts += "A Google Play subscription is linked to a different Hyperion account. Sign in with the original account to sync it. Google Play access on this device is unchanged."
            }
            if (playLinks.hadFailures) {
                messageParts += "Some Google Play subscriptions could not be verified or linked. Existing app access is unchanged; check your connection and tap Sync to retry."
            }
            messageParts += result.studyProgressMessage
            _state.value = SupabaseAccountState(
                email = session.email.ifBlank { null },
                isLoading = false,
                activeWebsiteLicenses = count,
                activePlaySubscriptionsLinked = playLinks.activeCount,
                isDeveloperAdminAccess = result.isDeveloperAdmin,
                message = messageParts.joinToString(" ")
            )
        } catch (_: Exception) {
            _state.value = _state.value.copy(
                email = initialSession.email.ifBlank { null },
                isLoading = false,
                isDeveloperAdminAccess = LicenseManager.isDeveloperAdminAccessActive(context),
                message = "Signed in, but website licenses could not be refreshed. Connect to the internet and tap Sync to retry."
            )
        }
    }

    private data class PlayLinkSyncResult(
        val activeCount: Int,
        val hadFailures: Boolean,
        val accountMismatch: Boolean = false
    )
    private data class AccountSyncResult(
        val session: SupabaseSession,
        val licenseCount: Int,
        val playLinks: PlayLinkSyncResult,
        val isDeveloperAdmin: Boolean,
        val studyProgressMessage: String
    )

    /**
     * Reuses the website's check-admin action, which validates the Supabase JWT
     * and checks the server-only ADMIN_EMAILS allowlist. A 403 means ordinary
     * user; network/auth errors fail closed and never grant developer privileges.
     */
    private fun verifyDeveloperAdminAccess(session: SupabaseSession): Boolean? {
        return try {
            val response = requestText(
                method = "POST",
                url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/admin-payment-review",
                accessToken = session.accessToken,
                jsonBody = JSONObject().put("action", "check-admin")
            )
            JSONObject(response).optBoolean("isAdmin", false)
        } catch (error: IOException) {
            if (error.message.orEmpty().contains("Supabase request failed (403):")) false else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun syncGooglePlayPurchases(context: Context, session: SupabaseSession): PlayLinkSyncResult {
        return try {
            val billing = BillingManager.getInstance(context.applicationContext)
            // BillingManager connects asynchronously during construction. Do not read
            // its cached snapshot before Play Billing is ready, or a valid owned
            // subscription could be missed during a first sign-in/cold start.
            val connected = withTimeoutOrNull(15_000L) {
                billing.entitlements.first { it.billingConnected }
            }
            if (connected == null) return PlayLinkSyncResult(activeCount = 0, hadFailures = true)

            if (!billing.queryPurchasesAsync()) {
                // Never link tokens from an older in-memory snapshot after Play's
                // current ownership query failed or could not run online.
                return PlayLinkSyncResult(activeCount = 0, hadFailures = true)
            }
            val purchases = billing.getOwnedPurchasesForAccountLinking()
            var activeCount = 0
            var hadFailures = false
            var accountMismatch = false
            for (purchase in purchases) {
                try {
                    val body = requestText(
                        method = "POST",
                        url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/link-google-play-subscription",
                        accessToken = session.accessToken,
                        jsonBody = JSONObject().put("purchaseToken", purchase.purchaseToken)
                    )
                    val response = JSONObject(body)
                    if (response.optBoolean("active", false)) activeCount++
                } catch (error: Exception) {
                    // A 409 is a permanent account-ownership conflict, not a transient backend
                    // failure. Explain that the purchase must be synced by its original account.
                    if (isGooglePlayAccountLinkConflict(error)) accountMismatch = true
                    else hadFailures = true
                }
            }
            PlayLinkSyncResult(activeCount, hadFailures, accountMismatch)
        } catch (_: Exception) {
            PlayLinkSyncResult(activeCount = 0, hadFailures = true)
        }
    }

    private fun exchangeGoogleIdToken(idToken: String, rawNonce: String): SupabaseSession {        val payload = JSONObject()
            .put("provider", "google")
            .put("id_token", idToken)
            .put("nonce", rawNonce)
        val body = requestText(
            method = "POST",
            url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/auth/v1/token?grant_type=id_token",
            jsonBody = payload
        )
        return parseSession(JSONObject(body), fallback = null)
    }

    private fun getFreshSession(context: Context, session: SupabaseSession): SupabaseSession {
        if (session.expiresAtMillis > System.currentTimeMillis() + SESSION_REFRESH_MARGIN_MS) {
            return session
        }
        val payload = JSONObject().put("refresh_token", session.refreshToken)
        val body = requestText(
            method = "POST",
            url = BuildConfig.SUPABASE_URL.trimEnd('/') + "/auth/v1/token?grant_type=refresh_token",
            jsonBody = payload
        )
        return parseSession(JSONObject(body), fallback = session).also { storeSession(context, it) }
    }

    private fun syncEntitlements(context: Context, session: SupabaseSession): Int {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/"
        val websiteRows = JSONArray(requestText(
            method = "GET",
            url = baseUrl + "entitlements?select=track_id,expires_at,status&status=eq.active",
            accessToken = session.accessToken
        ))
        val playRows = try {
            JSONArray(requestText(
                method = "GET",
                url = baseUrl + "google_play_entitlements?select=track_id,expires_at,status&status=eq.active",
                accessToken = session.accessToken
            ))
        } catch (error: IOException) {
            // Older production schemas won't have this optional table before the linking
            // migration is released. Keep ordinary website-license sync working while the
            // optional Google Play-linking backend is unavailable.
            if (isMissingGooglePlayEntitlementsTable(error)) JSONArray() else throw error
        }
        val now = System.currentTimeMillis()
        val expiryByTrack = Track.values().associateWith { 0L }.toMutableMap()

        fun applyRows(rows: JSONArray) {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                if (row.optString("status") != "active") continue
                val track = when (row.optString("track_id")) {
                    "DECK_OIC_NW" -> Track.DECK_OIC_NW
                    "GMDSS_GOC" -> Track.GMDSS_GOC
                    "ENGINE_OIC_EW" -> Track.ENGINE_OIC_EW
                    "DECK_MGT" -> Track.DECK_MGT
                    "ENGINE_MGT" -> Track.ENGINE_MGT
                    else -> null
                } ?: continue
                val expiryText = row.optString("expires_at")
                if (expiryText.isBlank() || expiryText == "null") continue
                val expiry = try { parseEpochMillis(expiryText) } catch (_: Exception) { continue }
                if (expiry > now) expiryByTrack[track] = maxOf(expiryByTrack[track] ?: 0L, expiry)
            }
        }
        applyRows(websiteRows)
        applyRows(playRows)

        if (!LicenseManager.saveAccountEntitlements(context, session.userId, expiryByTrack)) {
            throw IllegalStateException("Could not securely cache account licenses on this device.")
        }
        return expiryByTrack.values.count { it > now }
    }

    private fun parseEpochMillis(value: String): Long {
        return try {
            Instant.parse(value).toEpochMilli()
        } catch (_: Exception) {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        }
    }

    private fun parseSession(json: JSONObject, fallback: SupabaseSession?): SupabaseSession {
        val user = json.optJSONObject("user")
        val accessToken = json.optString("access_token")
        val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
            ?: fallback?.refreshToken.orEmpty()
        val userId = user?.optString("id")?.takeIf { it.isNotBlank() }
            ?: fallback?.userId.orEmpty()
        val email = user?.optString("email")?.takeIf { it.isNotBlank() }
            ?: fallback?.email.orEmpty()
        val expiresIn = json.optLong("expires_in", 3600L).coerceAtLeast(60L)
        if (accessToken.isBlank() || refreshToken.isBlank() || userId.isBlank()) {
            throw IOException("Supabase did not return a complete authenticated session.")
        }
        return SupabaseSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            email = email,
            expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000L
        )
    }

    private fun storeSession(context: Context, session: SupabaseSession) {
        sessionPreferences(context).edit()
            .putString("access_token", session.accessToken)
            .putString("refresh_token", session.refreshToken)
            .putString("user_id", session.userId)
            .putString("email", session.email)
            .putLong("expires_at_ms", session.expiresAtMillis)
            .apply()
    }

    private fun readSession(context: Context): SupabaseSession? {
        val prefs = sessionPreferences(context)
        val accessToken = prefs.getString("access_token", null) ?: return null
        val refreshToken = prefs.getString("refresh_token", null) ?: return null
        val userId = prefs.getString("user_id", null) ?: return null
        return SupabaseSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            email = prefs.getString("email", "").orEmpty(),
            expiresAtMillis = prefs.getLong("expires_at_ms", 0L)
        )
    }

    private fun sessionPreferences(context: Context): SharedPreferences {
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            SESSION_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun requestText(
        method: String,
        url: String,
        accessToken: String? = null,
        jsonBody: JSONObject? = null
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "gzip")
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            if (jsonBody != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (jsonBody.optString("action") == "write" &&
                    url.endsWith("/functions/v1/learning-progress")) {
                    setRequestProperty("Content-Encoding", "gzip")
                }
            }
        }

        try {
            if (jsonBody != null) {
                val rawBody = jsonBody.toString().toByteArray(StandardCharsets.UTF_8)
                val requestBody = if (
                    jsonBody.optString("action") == "write" &&
                    url.endsWith("/functions/v1/learning-progress")
                ) {
                    ByteArrayOutputStream().use { output ->
                        GZIPOutputStream(output).use { gzip -> gzip.write(rawBody) }
                        output.toByteArray()
                    }
                } else rawBody
                connection.setFixedLengthStreamingMode(requestBody.size)
                connection.outputStream.use { stream ->
                    stream.write(requestBody)
                }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val decodedStream = if (
                stream != null && connection.getHeaderField("Content-Encoding")
                    ?.contains("gzip", ignoreCase = true) == true
            ) GZIPInputStream(stream) else stream
            val body = decodedStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val apiMessage = try {
                    val error = JSONObject(body)
                    error.optString("msg").takeIf { it.isNotBlank() }
                        ?: error.optString("message").takeIf { it.isNotBlank() }
                        ?: error.optString("error_description").takeIf { it.isNotBlank() }
                        ?: error.optString("error").takeIf { it.isNotBlank() }
                        ?: "Request rejected."
                } catch (_: Exception) {
                    "Request rejected."
                }
                throw IOException("Supabase request failed ($status): $apiMessage")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }
}
