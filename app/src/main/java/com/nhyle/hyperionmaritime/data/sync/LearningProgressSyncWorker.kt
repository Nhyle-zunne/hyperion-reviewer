package com.nhyle.hyperionmaritime.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nhyle.hyperionmaritime.data.auth.SupabaseAccountManager

/**
 * Network-constrained background retry for account-linked study progress.
 * It is a no-op for users who have not signed in.
 */
class LearningProgressSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            SupabaseAccountManager.syncLearningProgressInBackground(applicationContext)
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 4) Result.retry() else Result.failure()
        }
    }
}
