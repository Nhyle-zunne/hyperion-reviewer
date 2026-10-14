package com.nhyle.hyperionmaritime.data.local

import android.content.Context
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class MistakeEntry(
    val questionId: String,
    val track: String,              // e.g. "GMDSS", "OIC-NW"
    val competenceOrPart: String,
    val consecutiveCorrect: Int = 0,// 0, 1, or 2
    val question: Question          // Store full Question object to avoid failed asset lookups
) {
    val competenceId: String get() = competenceOrPart
    val incorrectCount: Int get() = 1
    val lastAnsweredDate: String get() = "Recently"
}

object MistakesVaultManager {
    fun getVaultFile(context: Context): File {
        return File(context.filesDir, "mistakes_vault.json")
    }

    fun loadEntries(context: Context): List<MistakeEntry> {
        try {
            val file = getVaultFile(context)
            if (!file.exists()) return emptyList()
            val content = file.readText()
            if (content.isBlank()) return emptyList()
            val jsonArray = JSONArray(content)
            val list = mutableListOf<MistakeEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val questionId = obj.optString("questionId", "")
                val track = obj.optString("track", "")
                val competenceOrPart = obj.optString("competenceOrPart", "")
                val consecutiveCorrect = obj.optInt("consecutiveCorrect", 0)
                
                val qObj = obj.optJSONObject("question")
                val question = if (qObj != null) {
                    val id = qObj.optString("id", questionId)
                    val qTrackName = qObj.optString("track", track)
                    val qTrack = try { Track.valueOf(qTrackName) } catch (_: Exception) { Track.DECK_OIC_NW }
                    val functionId = qObj.optString("functionId", "")
                    val competenceId = qObj.optString("competenceId", competenceOrPart)
                    val topic = qObj.optString("topic", "")
                    val questionText = qObj.optString("questionText", "")
                    val optsArray = qObj.optJSONArray("options") ?: JSONArray()
                    val options = mutableListOf<String>()
                    for (j in 0 until optsArray.length()) {
                        options.add(optsArray.getString(j))
                    }
                    val correctAnswerIndex = qObj.optInt("correctAnswerIndex", 0)
                    val explanation = qObj.optString("explanation", "")
                    val diffName = qObj.optString("difficulty", "EASY")
                    val difficulty = try { Difficulty.valueOf(diffName) } catch (_: Exception) { Difficulty.EASY }
                    val isBookmarked = qObj.optBoolean("isBookmarked", false)
                    val isWrongInVault = qObj.optBoolean("isWrongInVault", true)

                    Question(
                        id = id,
                        track = qTrack,
                        functionId = functionId,
                        competenceId = competenceId,
                        topic = topic,
                        questionText = questionText,
                        options = options,
                        correctAnswerIndex = correctAnswerIndex,
                        explanation = explanation,
                        difficulty = difficulty,
                        isBookmarked = isBookmarked,
                        isWrongInVault = isWrongInVault
                    )
                } else {
                    MaritimeQuestionBank.ALL_QUESTIONS.find { it.id == questionId } ?: Question(
                        id = questionId,
                        track = Track.DECK_OIC_NW,
                        functionId = "",
                        competenceId = competenceOrPart,
                        topic = "",
                        questionText = "Question data unavailable",
                        options = listOf("A", "B", "C", "D"),
                        correctAnswerIndex = 0,
                        explanation = "",
                        difficulty = Difficulty.EASY,
                        isWrongInVault = true
                    )
                }

                list.add(
                    MistakeEntry(
                        questionId = questionId,
                        track = track,
                        competenceOrPart = competenceOrPart,
                        consecutiveCorrect = consecutiveCorrect,
                        question = question
                    )
                )
            }
            return list
        } catch (_: Exception) {
            return emptyList()
        }
    }

    fun saveEntries(context: Context, entries: List<MistakeEntry>) {
        try {
            val file = getVaultFile(context)
            val jsonArray = JSONArray()
            entries.forEach { entry ->
                val obj = JSONObject().apply {
                    put("questionId", entry.questionId)
                    put("track", entry.track)
                    put("competenceOrPart", entry.competenceOrPart)
                    put("consecutiveCorrect", entry.consecutiveCorrect)
                    
                    val q = entry.question
                    val qObj = JSONObject().apply {
                        put("id", q.id)
                        put("track", q.track.name)
                        put("functionId", q.functionId)
                        put("competenceId", q.competenceId)
                        put("topic", q.topic)
                        put("questionText", q.questionText)
                        val opts = JSONArray()
                        q.options.forEach { opts.put(it) }
                        put("options", opts)
                        put("correctAnswerIndex", q.correctAnswerIndex)
                        put("explanation", q.explanation)
                        put("difficulty", q.difficulty.name)
                        put("isBookmarked", q.isBookmarked)
                        put("isWrongInVault", q.isWrongInVault)
                    }
                    put("question", qObj)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (_: Exception) {}
    }

    fun recordMistake(context: Context, question: Question, track: String, competenceOrPart: String) {
        val entries = loadEntries(context).toMutableList()
        val index = entries.indexOfFirst { it.questionId == question.id }
        if (index >= 0) {
            val existing = entries[index]
            entries[index] = existing.copy(consecutiveCorrect = 0, question = question, track = track, competenceOrPart = competenceOrPart)
        } else {
            entries.add(
                MistakeEntry(
                    questionId = question.id,
                    track = track,
                    competenceOrPart = competenceOrPart,
                    consecutiveCorrect = 0,
                    question = question
                )
            )
        }
        saveEntries(context, entries)
    }

    fun recordAttempt(context: Context, questionId: String, isCorrect: Boolean) {
        val entries = loadEntries(context).toMutableList()
        val index = entries.indexOfFirst { it.questionId == questionId }
        if (index >= 0) {
            val existing = entries[index]
            if (isCorrect) {
                val newStreak = existing.consecutiveCorrect + 1
                if (newStreak >= 2) {
                    entries.removeAt(index)
                } else {
                    entries[index] = existing.copy(consecutiveCorrect = newStreak)
                }
            } else {
                entries[index] = existing.copy(consecutiveCorrect = 0)
            }
            saveEntries(context, entries)
        }
    }

    fun getEntriesForTrack(context: Context, activeTrack: String): List<MistakeEntry> {
        val all = loadEntries(context)
        return all.filter { it.track.equals(activeTrack, ignoreCase = true) }
    }

    fun deleteEntry(context: Context, questionId: String) {
        val entries = loadEntries(context).filter { it.questionId != questionId }
        saveEntries(context, entries)
    }

    fun clearAll(context: Context) {
        val file = getVaultFile(context)
        if (file.exists()) file.delete()
    }
}
