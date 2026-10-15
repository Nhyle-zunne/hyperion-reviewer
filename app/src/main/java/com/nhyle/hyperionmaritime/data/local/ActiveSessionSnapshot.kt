package com.nhyle.hyperionmaritime.data.local

import android.content.Context
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track
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
    val timestamp: Long = System.currentTimeMillis(),
    val deliveryMode: String = "STANDARD",
    val clearedCount: Int = 0,
    val intensiveQueueIds: List<String> = emptyList()
)

object ActiveSessionSnapshotManager {
    fun getSnapshotFile(context: Context, sessionId: String): File {
        return File(context.filesDir, "session_$sessionId.json")
    }

    fun saveSnapshot(context: Context, snapshot: ActiveSessionSnapshot) {
        try {
            val file = getSnapshotFile(context, snapshot.sessionId)
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
                put("deliveryMode", snapshot.deliveryMode)
                put("clearedCount", snapshot.clearedCount)
                val qQueueArray = JSONArray()
                snapshot.intensiveQueueIds.forEach { qQueueArray.put(it) }
                put("intensiveQueueIds", qQueueArray)
            }
            val jsonStr = json.toString()
            file.writeText(jsonStr)
            val isMistakes = snapshot.mode.equals("MISTAKES", ignoreCase = true) ||
                snapshot.competenceOrPart.equals("MISTAKES", ignoreCase = true)
            val trackFileSuffix = if (isMistakes) "${snapshot.track}_MISTAKES" else snapshot.track
            val trackFile = File(context.filesDir, "active_session_$trackFileSuffix.json")
            trackFile.writeText(jsonStr)
        } catch (_: Exception) {}
    }

    fun loadSnapshotById(context: Context, sessionId: String): ActiveSessionSnapshot? {
        try {
            val file = getSnapshotFile(context, sessionId)
            if (!file.exists()) return null
            return parseSnapshotFile(file)
        } catch (_: Exception) {
            return null
        }
    }

    fun loadSnapshot(context: Context, trackName: String): ActiveSessionSnapshot? {
        try {
            val trackFile = File(context.filesDir, "active_session_$trackName.json")
            if (trackFile.exists()) {
                val snap = parseSnapshotFile(trackFile)
                if (snap != null && !snap.isCompleted) {
                    if (snap.mode.contains("MOCK", ignoreCase = true) && snap.totalQuestions > 200) {
                        deleteSnapshot(context, snap.sessionId)
                        deleteSnapshot(context, trackName)
                        return null
                    }
                    return snap
                }
            }
        } catch (_: Exception) {}
        val all = loadAllSnapshots(context)
        val found = all.firstOrNull { 
            (it.track.equals(trackName, ignoreCase = true) || it.sessionId.equals(trackName, ignoreCase = true)) &&
            !it.mode.equals("MISTAKES", ignoreCase = true) &&
            !it.competenceOrPart.equals("MISTAKES", ignoreCase = true)
        }
        if (found != null && found.mode.contains("MOCK", ignoreCase = true) && found.totalQuestions > 200) {
            deleteSnapshot(context, found.sessionId)
            deleteSnapshot(context, trackName)
            return null
        }
        return found
    }

    fun loadMistakesSnapshot(context: Context, trackName: String): ActiveSessionSnapshot? {
        try {
            val trackFile = File(context.filesDir, "active_session_${trackName}_MISTAKES.json")
            if (trackFile.exists()) {
                val snap = parseSnapshotFile(trackFile)
                if (snap != null && !snap.isCompleted) {
                    return snap
                }
            }
        } catch (_: Exception) {}
        val all = loadAllSnapshots(context)
        return all.firstOrNull {
            it.track.equals(trackName, ignoreCase = true) &&
            (it.mode.equals("MISTAKES", ignoreCase = true) || it.competenceOrPart.equals("MISTAKES", ignoreCase = true)) &&
            !it.isCompleted
        }
    }

    fun purgeObsoleteMockSnapshots(context: Context) {
        try {
            val dir = context.filesDir
            val files = dir.listFiles { _, name -> (name.startsWith("session_") || name.startsWith("active_session_")) && name.endsWith(".json") }
            files?.forEach { file ->
                val snap = parseSnapshotFile(file)
                if (snap != null && snap.mode.contains("MOCK", ignoreCase = true) && snap.totalQuestions > 200) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    fun loadAllSnapshots(context: Context): List<ActiveSessionSnapshot> {
        val list = mutableListOf<ActiveSessionSnapshot>()
        try {
            val dir = context.filesDir
            val files = dir.listFiles { _, name -> (name.startsWith("session_") || name.startsWith("active_session_")) && name.endsWith(".json") }
            files?.forEach { file ->
                parseSnapshotFile(file)?.let { list.add(it) }
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.timestamp }
    }

    private fun parseSnapshotFile(file: File): ActiveSessionSnapshot? {
        try {
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
                    val allQ = com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank.getQuestionsByTrack(trackEnum, isProUnlocked = true)
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
            val deliveryMode = json.optString("deliveryMode", "STANDARD")
            val clearedCount = json.optInt("clearedCount", 0)
            val queueIdsArray = json.optJSONArray("intensiveQueueIds")
            val intensiveQueueIds = mutableListOf<String>()
            if (queueIdsArray != null) {
                for (i in 0 until queueIdsArray.length()) {
                    intensiveQueueIds.add(queueIdsArray.getString(i))
                }
            }

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
                timestamp = timestamp,
                deliveryMode = deliveryMode,
                clearedCount = clearedCount,
                intensiveQueueIds = intensiveQueueIds
            )
        } catch (_: Exception) {
            return null
        }
    }

    fun deleteSnapshot(context: Context, sessionId: String) {
        try {
            val file = getSnapshotFile(context, sessionId)
            if (file.exists()) {
                file.delete()
            }
            // Also check legacy track file name
            val trackFile = File(context.filesDir, "active_session_$sessionId.json")
            if (trackFile.exists()) {
                trackFile.delete()
            }
        } catch (_: Exception) {}
    }

    fun deleteMistakesSnapshot(context: Context, trackName: String) {
        try {
            val trackFile = File(context.filesDir, "active_session_${trackName}_MISTAKES.json")
            if (trackFile.exists()) {
                trackFile.delete()
            }
        } catch (_: Exception) {}
    }
}
