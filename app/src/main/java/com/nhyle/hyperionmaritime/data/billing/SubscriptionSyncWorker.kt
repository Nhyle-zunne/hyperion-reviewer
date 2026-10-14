package com.nhyle.hyperionmaritime.data.billing

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Enterprise WorkManager task that periodically checks Google Play billing status.
 * Executes queryPurchasesAsync() to enforce Strict Reset-Before-Reconcile logic,
 * ensuring any canceled, refunded, or revoked Google Play subscriptions immediately
 * revoke local entitlement state without leaving 'sticky' access.
 */
class SubscriptionSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SubscriptionSyncWorker"
        const val WORK_NAME = "subscription_revocation_sync_work"

        /**
         * Schedules or updates the periodic background subscription revocation check.
         */
        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = PeriodicWorkRequestBuilder<SubscriptionSyncWorker>(
                    12, TimeUnit.HOURS,
                    30, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )
                Log.i(TAG, "Periodic subscription sync worker enqueued successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule subscription sync worker: ${e.message}")
            }
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting periodic Google Play subscription revocation check...")
        return try {
            val billingManager = BillingManager.getInstance(applicationContext)
            billingManager.queryPurchasesAsync()
            Log.i(TAG, "Completed periodic Google Play subscription status sync successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing subscription sync worker: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
