package com.example.data.local

import android.content.Context
import com.example.data.model.Difficulty
import com.example.data.model.Question
import com.example.data.model.Track
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ActiveSessionSnapshot(
    val sessionId: String,
    val track: String,              // e.g., "GMDSS", "OIC-NW"
    val mode: String,               // "PRACTICE" or "MOCK"
    val competenceOrPart: String,
    val questions: List<Question>,  // Full serialized Question models in their exact shuffled order
    val selectedAnswers: Map<String, String>, // questionId -> selected choice
    val firstAttemptStatus: Map<String, Boolean>,
    val currentIndex: Int,
    val totalQuestions: Int,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

object ActiveSessionSnapshotManager {
    fun getSnapshotFile(context: Context, trackName: String): File {
        val sanitized = trackName.lowercase().replace(" ", "_").replace("-", "_")
        return File(context.filesDir, "active_session_$sanitized.json")
    }

    fun saveSnapshot(context: Context, snapshot: ActiveSessionSnapshot) {
        try {
            val file = getSnapshotFile(context, snapshot.track)
            val json = JSONObject().apply {
                put("sessionId", snapshot.sessionId)
                put("track", snapshot.track)
                put("mode", snapshot.mode)
                put("competenceOrPart", snapshot.competenceOrPart)
                
                val qArray = JSONArray()
                snapshot.questions.forEach { q ->
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
                    qArray.put(qObj)
                }
                put("questions", qArray)

                val ansObj = JSONObject()
                snapshot.selectedAnswers.forEach { (k, v) -> ansObj.put(k, v) }
                put("selectedAnswers", ansObj)

                val statObj = JSONObject()
                snapshot.firstAttemptStatus.forEach { (k, v) -> statObj.put(k, v) }
                put("firstAttemptStatus", statObj)

                put("currentIndex", snapshot.currentIndex)
                put("totalQuestions", snapshot.totalQuestions)
                put("isCompleted", snapshot.isCompleted)
                put("timestamp", snapshot.timestamp)
            }
            file.writeText(json.toString())
        } catch (_: Exception) {}
    }

    fun loadSnapshot(context: Context, trackName: String): ActiveSessionSnapshot? {
        try {
            val file = getSnapshotFile(context, trackName)
            if (!file.exists()) return null
            val content = file.readText()
            if (content.isBlank()) return null
            val json = JSONObject(content)
            
            val sessionId = json.optString("sessionId", java.util.UUID.randomUUID().toString())
            val track = json.getString("track")
            val mode = json.optString("mode", "PRACTICE")
            val competenceOrPart = json.optString("competenceOrPart", "")
            
            val questions = mutableListOf<Question>()
            val qArray = json.optJSONArray("questions")
            if (qArray != null) {
                for (i in 0 until qArray.length()) {
                    val qObj = qArray.getJSONObject(i)
                    val id = qObj.optString("id", "")
                    val qTrackName = qObj.optString("track", track)
                    val qTrack = try { Track.valueOf(qTrackName) } catch (_: Exception) { Track.DECK_OIC_NW }
                    val functionId = qObj.optString("functionId", "")
                    val competenceId = qObj.optString("competenceId", "")
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
                    val isWrongInVault = qObj.optBoolean("isWrongInVault", false)

                    questions.add(
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
                    )
                }
            }

            // Fallback for older snapshots that had questionIds instead of questions
            if (questions.isEmpty()) {
                val qIdsArray = json.optJSONArray("questionIds")
                if (qIdsArray != null) {
                    val trackEnum = try { Track.valueOf(track) } catch (_: Exception) { Track.DECK_OIC_NW }
                    val allQ = com.example.data.repository.MaritimeQuestionBank.getQuestionsByTrack(trackEnum, isProUnlocked = true)
                    for (i in 0 until qIdsArray.length()) {
                        val qId = qIdsArray.getString(i)
                        allQ.find { it.id == qId }?.let { questions.add(it) }
                    }
                }
            }

            val ansObj = json.optJSONObject("selectedAnswers") ?: JSONObject()
            val answers = mutableMapOf<String, String>()
            ansObj.keys().forEach { key ->
                answers[key] = ansObj.getString(key)
            }

            val statObj = json.optJSONObject("firstAttemptStatus") ?: JSONObject()
            val statuses = mutableMapOf<String, Boolean>()
            statObj.keys().forEach { key ->
                statuses[key] = statObj.getBoolean(key)
            }

            val currentIndex = json.optInt("currentIndex", 0)
            val totalQuestions = json.optInt("totalQuestions", questions.size)
            val isCompleted = json.optBoolean("isCompleted", false)
            val timestamp = json.optLong("timestamp", System.currentTimeMillis())

            return ActiveSessionSnapshot(
                sessionId = sessionId,
                track = track,
                mode = mode,
                competenceOrPart = competenceOrPart,
                questions = questions,
                selectedAnswers = answers,
                firstAttemptStatus = statuses,
                currentIndex = currentIndex,
                totalQuestions = totalQuestions,
                isCompleted = isCompleted,
                timestamp = timestamp
            )
        } catch (_: Exception) {
            return null
        }
    }

    fun deleteSnapshot(context: Context, trackName: String) {
        try {
            val file = getSnapshotFile(context, trackName)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }
}
