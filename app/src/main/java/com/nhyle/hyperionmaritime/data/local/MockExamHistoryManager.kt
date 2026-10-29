package com.nhyle.hyperionmaritime.data.local

import android.content.Context
import android.content.SharedPreferences
import com.nhyle.hyperionmaritime.data.model.Track

object MockExamHistoryManager {
    private const val PREFS_NAME = "hyperion_mock_exam_history"
    private const val KEY_PREFIX = "mock_seen_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun buildKey(track: Track, competenceId: String): String {
        return "$KEY_PREFIX${track.name}_${competenceId.uppercase()}"
    }

    @Synchronized
    fun getSeenQuestionIds(context: Context, track: Track, competenceId: String): Set<String> {
        val prefs = getPrefs(context)
        val rawSet = prefs.getStringSet(buildKey(track, competenceId), null) ?: return emptySet()
        // Defensive copy to prevent mutating the internal SharedPreferences set
        return HashSet(rawSet)
    }

    @Synchronized
    fun saveSeenQuestionIds(context: Context, track: Track, competenceId: String, ids: Set<String>) {
        val prefs = getPrefs(context)
        val copy = HashSet(ids)
        prefs.edit().putStringSet(buildKey(track, competenceId), copy).apply()
    }

    @Synchronized
    fun clearHistoryForCompetence(context: Context, track: Track, competenceId: String) {
        val prefs = getPrefs(context)
        prefs.edit().remove(buildKey(track, competenceId)).apply()
    }
}
