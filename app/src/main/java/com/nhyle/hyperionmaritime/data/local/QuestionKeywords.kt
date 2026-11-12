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
        private val wordPattern = Regex("[\\p{L}\\p{N}]+(?:['’\\-][\\p{L}\\p{N}]+)*")
        /** Keep all legacy word IDs; append each non-whitespace symbol after them. */
        fun tokens(text: String): List<MatchResult> {
            val words = wordPattern.findAll(text).toList()
            val symbols = Regex("[^\\s\\p{Z}\\p{L}\\p{N}]").findAll(text).toList()
            return words + symbols
        }

        fun toggleSelection(text: String, selected: Set<Int>, tokenId: Int): Set<Int> {
            val tokens = tokens(text)
            if (tokenId !in tokens.indices) return selected
            val wordCount = wordPattern.findAll(text).count()
            val affected = if (tokenId < wordCount) {
                setOf(tokenId) + tokens.withIndex().filter { (id, token) ->
                    id >= wordCount && token.range.first in tokens[tokenId].range
                }.map { it.index }
            } else setOf(tokenId)
            return if (tokenId in selected) selected - affected else selected + affected
        }

        private fun expandLegacySelection(text: String, selected: Set<Int>): Set<Int> {
            val tokens = tokens(text)
            val wordCount = wordPattern.findAll(text).count()
            return selected + tokens.withIndex().filter { (id, token) ->
                id >= wordCount && selected.any { word -> word < wordCount && token.range.first in tokens[word].range }
            }.map { it.index }
        }

        /** Symbols own their paint, even when a containing legacy word remains selected. */
        fun highlightRanges(text: String, selected: Set<Int>): List<IntRange> {
            val tokens = tokens(text)
            val wordCount = wordPattern.findAll(text).count()
            val symbolOffsets = tokens.drop(wordCount).flatMap { it.range.toList() }.toSet()
            val highlighted = BooleanArray(text.length)
            tokens.forEachIndexed { id, token ->
                if (id in selected) token.range.forEach { offset ->
                    if (id >= wordCount || offset !in symbolOffsets) highlighted[offset] = true
                }
            }
            val ranges = mutableListOf<IntRange>()
            var start = 0
            while (start < highlighted.size) {
                if (!highlighted[start]) { start++; continue }
                var end = start
                while (end + 1 < highlighted.size && highlighted[end + 1]) end++
                ranges += start..end
                start = end + 1
            }
            return ranges
        }
    }

    /** Commit the complete draft together; callers keep editing when persistence fails. */
    fun saveAll(selections: Map<String, Set<Int>>): Boolean {
        val previous = selections.keys.associateWith { prefs.getStringSet(it, null)?.toSet() }
        val previousVersions = selections.keys.associateWith {
            if (prefs.contains("${it}_independent_symbols")) prefs.getBoolean("${it}_independent_symbols", false) else null
        }
        val editor = prefs.edit()
        selections.forEach { (key, selected) ->
            editor.putStringSet(key, selected.map { it.toString() }.toSet())
            editor.putBoolean("${key}_independent_symbols", true)
        }
        if (editor.commit()) return true
        // A failed commit still updates Android's in-memory preferences. Restore the saved draft.
        val restore = prefs.edit()
        previous.forEach { (key, selected) ->
            if (selected == null) restore.remove(key) else restore.putStringSet(key, selected)
        }
        previousVersions.forEach { (key, versioned) ->
            if (versioned == null) restore.remove("${key}_independent_symbols")
            else restore.putBoolean("${key}_independent_symbols", versioned)
        }
        restore.commit()
        return false
    }

    fun loadText(key: String, text: String): Set<Int> {
        val selected = load(key, tokens(text).size)
        return if (prefs.getBoolean("${key}_independent_symbols", false)) selected
        else expandLegacySelection(text, selected)
    }

    fun load(key: String, wordCount: Int): Set<Int> =
        prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }
            .filter { it in 0 until wordCount }.toSet()

    /** Legacy word-index writer; new drafts use saveAll with the independent-symbol marker. */
    fun save(key: String, selected: Set<Int>) {
        prefs.edit().putStringSet(key, selected.map { it.toString() }.toSet())
            .remove("${key}_independent_symbols").apply()
    }
}
