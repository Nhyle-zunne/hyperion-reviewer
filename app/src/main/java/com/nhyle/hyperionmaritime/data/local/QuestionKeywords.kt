package com.nhyle.hyperionmaritime.data.local

import android.content.Context
import java.security.MessageDigest

/** Stores word positions only; question text is never duplicated into preferences. */
class QuestionKeywords(context: Context) {
    private val prefs = context.getSharedPreferences("question_keywords", Context.MODE_PRIVATE)

    fun key(track: String, questionId: String, text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$track\u0000$questionId\u0000$text".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun load(key: String, wordCount: Int): Set<Int> =
        prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }
            .filter { it in 0 until wordCount }.toSet()

    fun save(key: String, selected: Set<Int>) {
        prefs.edit().putStringSet(key, selected.map { it.toString() }.toSet()).apply()
    }
}
