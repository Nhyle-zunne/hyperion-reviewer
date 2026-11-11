package com.nhyle.hyperionmaritime.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nhyle.hyperionmaritime.BuildConfig
import com.nhyle.hyperionmaritime.data.auth.SupabaseAccountManager
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.util.LicenseManager
import java.util.concurrent.TimeUnit

/**
 * Background update checks are an opt-in paid feature, Wi-Fi only, and never
 * run on ordinary quiz navigation. No new worker is enqueued for a free user.
 */
internal object VerifiedBankRefreshScheduler {
    private const val DAILY_MS = 24 * 60 * 60 * 1000L
    fun enqueueAfterLicensedSync(context: Context) {
        if (!BuildConfig.SMART_BANK_UPDATES_ENABLED ||
            Track.values().none { LicenseManager.isTrackProUnlocked(context, it) }) return
        val owner = SupabaseAccountManager.savedSignedInOwnerId(context) ?: return
        val prefs = context.getSharedPreferences("hyperion_bank_update_checks_v1",Context.MODE_PRIVATE)
        val key = "queued:$owner"
        val now = System.currentTimeMillis()
        val previous = prefs.getLong(key,0)
        if (previous > 0 && now >= previous && now - previous < DAILY_MS) return
        val work = OneTimeWorkRequestBuilder<VerifiedBankRefreshWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL,30,TimeUnit.MINUTES)
            .build()
        // A failed commit must not convince future launches that we queued work.
        if (!prefs.edit().putLong(key,now).commit()) return
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "hyperion-verified-bank-refresh:$owner",ExistingWorkPolicy.KEEP,work
        )
    }
}

internal class VerifiedBankRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        if (!BuildConfig.SMART_BANK_UPDATES_ENABLED) return Result.success()
        return try {
            // Includes identity, latest license and release revision checks.
            SupabaseAccountManager.refreshLicensedQuestionBanks(applicationContext)
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
