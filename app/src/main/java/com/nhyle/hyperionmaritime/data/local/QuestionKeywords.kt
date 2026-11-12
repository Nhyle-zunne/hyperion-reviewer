package com.nhyle.hyperionmaritime.data.local

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

/** Stores token positions only; question text is never duplicated into preferences. */
class QuestionKeywords internal constructor(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("question_keywords", Context.MODE_PRIVATE))

    fun key(track: String, questionId: String, text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$track\u0000$questionId\u0000$text".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun choiceKey(stemKey: String, choiceText: String): String = key("choice-v1", stemKey, choiceText)

    companion object {
        /** Keep all legacy word IDs; append each non-whitespace symbol after them. */
        fun tokens(text: String): List<MatchResult> {
            val words = Regex("[\\p{L}\\p{N}]+(?:['’\\-][\\p{L}\\p{N}]+)*").findAll(text).toList()
            val symbols = Regex("[^\\s\\p{Z}\\p{L}\\p{N}]").findAll(text).toList()
            return words + symbols
        }
    }

    /** Commit the complete draft together; callers keep editing when persistence fails. */
    fun saveAll(selections: Map<String, Set<Int>>): Boolean {
        val previous = selections.keys.associateWith { prefs.getStringSet(it, null)?.toSet() }
        val editor = prefs.edit()
        selections.forEach { (key, selected) -> editor.putStringSet(key, selected.map { it.toString() }.toSet()) }
        if (editor.commit()) return true
        // A failed commit still updates Android's in-memory preferences. Restore the saved draft.
        val restore = prefs.edit()
        previous.forEach { (key, selected) ->
            if (selected == null) restore.remove(key) else restore.putStringSet(key, selected)
        }
        restore.commit()
        return false
    }

    fun load(key: String, wordCount: Int): Set<Int> =
        prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }
            .filter { it in 0 until wordCount }.toSet()

    fun save(key: String, selected: Set<Int>) {
        prefs.edit().putStringSet(key, selected.map { it.toString() }.toSet()).apply()
    }
}
