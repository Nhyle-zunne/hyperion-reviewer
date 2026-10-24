package com.nhyle.hyperionmaritime.data.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.play.core.review.ReviewManagerFactory

object InAppReviewHelper {
    private const val PREFS_NAME = "hyperion_review_prefs"
    private const val KEY_LAST_REVIEW_TIME = "last_review_prompt_millis"
    // Cooldown interval between review requests (minimum 4 days) to prevent user fatigue
    private const val MIN_DAYS_BETWEEN_PROMPTS = 4L

    fun maybePromptReview(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastPromptTime = prefs.getLong(KEY_LAST_REVIEW_TIME, 0L)
        val now = System.currentTimeMillis()
        val cooldownMillis = MIN_DAYS_BETWEEN_PROMPTS * 24 * 60 * 60 * 1000L

        if (now - lastPromptTime < cooldownMillis) {
            return
        }

        try {
            val reviewManager = ReviewManagerFactory.create(activity)
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener {
                        // Flow finished (user submitted, cancelled, or Google quota silently completed).
                        // Record timestamp to enforce spacing between prompts.
                        prefs.edit().putLong(KEY_LAST_REVIEW_TIME, System.currentTimeMillis()).apply()
                    }
                }
            }
        } catch (_: Exception) {
            // Silently ignore to guarantee the app flow is never interrupted
        }
    }
}

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}