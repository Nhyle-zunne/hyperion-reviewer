package com.nhyle.hyperionmaritime.data.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
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
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.OffsetDateTime

data class SupabaseAccountState(
    val email: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val activeWebsiteLicenses: Int = 0,
    val activePlaySubscriptionsLinked: Int = 0,
    val isDeveloperAdminAccess: Boolean = false
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
    private const val SESSION_PREFS = "hyperion_supabase_session"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000
    private const val SESSION_REFRESH_MARGIN_MS = 60_000L

    private val _state = MutableStateFlow(SupabaseAccountState())
    val state: StateFlow<SupabaseAccountState> = _state.asStateFlow()

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
                }
            }
            _state.value = SupabaseAccountState(
                email = session.email.ifBlank { null },
                isLoading = true,
                message = "Signed in. Checking your website licenses…"
            )
            syncAndUpdateState(activity.applicationContext, session)
        } catch (_: GetCredentialCancellationException) {
            _state.value = _state.value.copy(isLoading = false, message = "Sign-in cancelled.")
        } catch (e: GetCredentialException) {
            _state.value = _state.value.copy(
                isLoading = false,
                message = e.message?.takeIf { it.isNotBlank() } ?: "Google sign-in failed. Please try again."
            )
        } catch (e: Exception) {
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
            val session = withContext(Dispatchers.IO) { getFreshSession(appContext, saved) }
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
            val session = withContext(Dispatchers.IO) { getFreshSession(appContext, saved) }
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
        _state.value = SupabaseAccountState(message = "Signed out. Google Play purchases remain available.")
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
                AccountSyncResult(freshSession, licenseCount, playLinkResult, isDeveloperAdmin)
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
            if (playLinks.hadFailures) {
                messageParts += "Some Google Play subscriptions could not be linked yet. Existing app access is unchanged; try Sync again after the server is configured."
            }
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

    private data class PlayLinkSyncResult(val activeCount: Int, val hadFailures: Boolean)
    private data class AccountSyncResult(
        val session: SupabaseSession,
        val licenseCount: Int,
        val playLinks: PlayLinkSyncResult,
        val isDeveloperAdmin: Boolean
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

            billing.queryPurchasesAsync()
            val purchases = billing.getOwnedPurchasesForAccountLinking()
            var activeCount = 0
            var hadFailures = false
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
                } catch (_: Exception) {
                    // An unconfigured backend or a token already linked to another user must not
                    // block website entitlement sync or alter existing Google Play access.
                    hadFailures = true
                }
            }
            PlayLinkSyncResult(activeCount, hadFailures)
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
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            if (jsonBody != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (jsonBody != null) {
                connection.outputStream.use { stream ->
                    stream.write(jsonBody.toString().toByteArray(StandardCharsets.UTF_8))
                }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
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
