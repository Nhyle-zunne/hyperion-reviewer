package com.nhyle.hyperionmaritime.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class DeletedSession(
    val sessionId: String,
    val trackName: String,
    val label: String,
    val date: String,
    val progress: String,
    val examRecordJson: String,
    val drillSessionJson: String,
    val snapshotJson: String?,
    val deletedTimestamp: Long = System.currentTimeMillis()
)

val DeletedSession.remainingDays: Int
    get() {
        val now = System.currentTimeMillis()
        val elapsedMs = (now - deletedTimestamp).coerceAtLeast(0)
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val remainingMs = (thirtyDaysMs - elapsedMs).coerceAtLeast(0)
        return (remainingMs / (24 * 60 * 60 * 1000)).toInt()
    }

val DeletedSession.expirationLabel: String
    get() {
        val days = remainingDays
        return when {
            days <= 0 -> "EXPIRES TODAY"
            days == 1 -> "EXPIRES IN 1 DAY"
            else -> "EXPIRES IN $days DAYS"
        }
    }

object DeletedSessionManager {
    private fun getStorageFile(context: Context?): File? {
        if (context == null) return null
        return try {
            File(context.filesDir, "deleted_sessions.json")
        } catch (_: Exception) {
            null
        }
    }

    fun loadDeletedSessions(context: Context?): List<DeletedSession> {
        val file = getStorageFile(context) ?: return emptyList()
        if (!file.exists()) return emptyList()
        try {
            val content = file.readText()
            val jsonArray = JSONArray(content)
            val list = mutableListOf<DeletedSession>()
            val now = System.currentTimeMillis()
            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
            var hasExpired = false

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val deletedTimestamp = obj.optLong("deletedTimestamp", now)
                if (now - deletedTimestamp > thirtyDaysMs) {
                    hasExpired = true
                    continue
                }
                list.add(
                    DeletedSession(
                        sessionId = obj.getString("sessionId"),
                        trackName = obj.getString("trackName"),
                        label = obj.getString("label"),
                        date = obj.getString("date"),
                        progress = obj.getString("progress"),
                        examRecordJson = obj.optString("examRecordJson", ""),
                        drillSessionJson = obj.optString("drillSessionJson", ""),
                        snapshotJson = obj.optString("snapshotJson", null),
                        deletedTimestamp = deletedTimestamp
                    )
                )
            }
            if (hasExpired) {
                saveDeletedSessions(context, list)
            }
            return list
        } catch (_: Exception) {
            return emptyList()
        }
    }

    fun saveDeletedSessions(context: Context?, sessions: List<DeletedSession>) {
        try {
            val file = getStorageFile(context) ?: return
            val jsonArray = JSONArray()
            sessions.forEach { ds ->
                val obj = JSONObject().apply {
                    put("sessionId", ds.sessionId)
                    put("trackName", ds.trackName)
                    put("label", ds.label)
                    put("date", ds.date)
                    put("progress", ds.progress)
                    put("examRecordJson", ds.examRecordJson)
                    put("drillSessionJson", ds.drillSessionJson)
                    put("snapshotJson", ds.snapshotJson)
                    put("deletedTimestamp", ds.deletedTimestamp)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (_: Exception) {}
    }

    fun addDeletedSession(context: Context?, session: DeletedSession) {
        val current = loadDeletedSessions(context).toMutableList()
        current.removeAll { it.sessionId == session.sessionId }
        current.add(0, session)
        saveDeletedSessions(context, current)
    }

    fun removeDeletedSession(context: Context?, sessionId: String) {
        val current = loadDeletedSessions(context).toMutableList()
        current.removeAll { it.sessionId == sessionId }
        saveDeletedSessions(context, current)
    }

    fun permanentlyDeleteSession(context: Context?, sessionId: String) {
        removeDeletedSession(context, sessionId)
        if (context != null) {
            try {
                ActiveSessionSnapshotManager.deleteSnapshot(context, sessionId)
            } catch (_: Exception) {}
        }
    }

    fun serializeExamRecord(record: ExamRecordEntity): String {
        return JSONObject().apply {
            put("id", record.id)
            put("timestamp", record.timestamp)
            put("track", record.track)
            put("mode", record.mode)
            put("rawScore", record.rawScore)
            put("maxScore", record.maxScore)
            put("percentage", record.percentage)
            put("isPassed", record.isPassed)
            put("totalQuestions", record.totalQuestions)
            put("correctCount", record.correctCount)
            put("detailsSummary", record.detailsSummary)
            put("questionIds", record.questionIds)
            put("answersJson", record.answersJson)
            put("competenceBreakdownJson", record.competenceBreakdownJson)
            put("drillTitle", record.drillTitle ?: JSONObject.NULL)
            put("competence", record.competence ?: JSONObject.NULL)
            put("part", record.part ?: JSONObject.NULL)
        }.toString()
    }

    fun deserializeExamRecord(jsonStr: String): ExamRecordEntity? {
        if (jsonStr.isBlank()) return null
        try {
            val obj = JSONObject(jsonStr)
            return ExamRecordEntity(
                id = obj.getString("id"),
                timestamp = obj.getLong("timestamp"),
                track = obj.getString("track"),
                mode = obj.getString("mode"),
                rawScore = obj.getInt("rawScore"),
                maxScore = obj.getInt("maxScore"),
                percentage = obj.getDouble("percentage"),
                isPassed = obj.getBoolean("isPassed"),
                totalQuestions = obj.getInt("totalQuestions"),
                correctCount = obj.getInt("correctCount"),
                detailsSummary = obj.getString("detailsSummary"),
                questionIds = obj.optString("questionIds", ""),
                answersJson = obj.optString("answersJson", ""),
                competenceBreakdownJson = obj.optString("competenceBreakdownJson", ""),
                drillTitle = if (obj.has("drillTitle") && !obj.isNull("drillTitle")) obj.getString("drillTitle") else null,
                competence = if (obj.has("competence") && !obj.isNull("competence")) obj.getString("competence") else null,
                part = if (obj.has("part") && !obj.isNull("part")) obj.getString("part") else null
            )
        } catch (_: Exception) {
            return null
        }
    }

    fun serializeDrillSession(session: DrillSessionEntity): String {
        return JSONObject().apply {
            put("id", session.id)
            put("track", session.track)
            put("competence", session.competence ?: JSONObject.NULL)
            put("part", session.part ?: JSONObject.NULL)
            put("isCompleted", session.isCompleted)
            put("lastUpdated", session.lastUpdated)
            put("correctCount", session.correctCount)
            put("answeredCount", session.answeredCount)
            put("totalItems", session.totalItems)
            put("currentQuestionIndex", session.currentQuestionIndex)
            put("questionIds", session.questionIds)
            put("answersJson", session.answersJson)
            put("isShuffled", session.isShuffled)
            put("deliveryMode", session.deliveryMode)
            put("mode", session.mode)
        }.toString()
    }

    fun deserializeDrillSession(jsonStr: String): DrillSessionEntity? {
        if (jsonStr.isBlank()) return null
        try {
            val obj = JSONObject(jsonStr)
            return DrillSessionEntity(
                id = obj.getString("id"),
                track = obj.getString("track"),
                competence = if (obj.has("competence") && !obj.isNull("competence")) obj.getString("competence") else null,
                part = if (obj.has("part") && !obj.isNull("part")) obj.getString("part") else null,
                isCompleted = obj.optBoolean("isCompleted", false),
                lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis()),
                correctCount = obj.optInt("correctCount", 0),
                answeredCount = obj.optInt("answeredCount", 0),
                totalItems = obj.optInt("totalItems", 0),
                currentQuestionIndex = obj.optInt("currentQuestionIndex", 0),
                questionIds = obj.optString("questionIds", ""),
                answersJson = obj.optString("answersJson", ""),
                isShuffled = obj.optBoolean("isShuffled", false),
                deliveryMode = obj.optString("deliveryMode", "STANDARD"),
                mode = obj.optString("mode", "PRACTICE")
            )
        } catch (_: Exception) {
            return null
        }
    }
}
