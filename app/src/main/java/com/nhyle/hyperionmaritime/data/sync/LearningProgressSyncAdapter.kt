package com.nhyle.hyperionmaritime.data.sync

import android.content.Context
import com.nhyle.hyperionmaritime.data.local.BookmarkEntity
import com.nhyle.hyperionmaritime.data.local.ExamRecordEntity
import com.nhyle.hyperionmaritime.data.local.HyperionDatabase
import com.nhyle.hyperionmaritime.data.local.MistakeEntity
import com.nhyle.hyperionmaritime.data.local.MistakeEntry
import com.nhyle.hyperionmaritime.data.local.MistakesVaultManager
import com.nhyle.hyperionmaritime.data.local.QuestionAttemptHistoryManager
import com.nhyle.hyperionmaritime.data.local.SessionJsonHelper
import com.nhyle.hyperionmaritime.data.local.QuestionAttemptRecord
import com.nhyle.hyperionmaritime.data.local.QuestionHistoryEntry
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track
import com.nhyle.hyperionmaritime.data.repository.MaritimeQuestionBank
import com.nhyle.hyperionmaritime.data.studytime.StudyTimeSegmentEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

/**
 * Bridges Android's existing offline-first stores to the PWA's version-1
 * learning-progress snapshot. No Room schema changes are required.
 *
 * Cloud reads/writes remain in SupabaseAccountManager and go through the
 * authenticated learning-progress Edge Function, which verifies saved-question
 * entitlement before question-bearing records are persisted or returned.
 */
object LearningProgressSyncAdapter {
    private const val HISTORY_KEY = "hyperion-history"
    private const val STUDY_TIME_KEY = "hyperion-study-time"
    private const val VAULT_KEY = "hyperion-vault"
    private const val BOOKMARKS_KEY = "hyperion-bookmarks"
    private const val QUESTION_ATTEMPTS_KEY = "hyperion-question-attempt-history"
    private const val DELETIONS_KEY = "hyperion-progress-deletions"

    suspend fun mergeNativeIntoSnapshot(context: Context, remote: JSONObject?): JSONObject {
        val appContext = context.applicationContext
        val database = HyperionDatabase.getInstance(appContext)
        val questionIndex = loadQuestionIndex(database)
        val storage = remote?.optJSONObject("storage") ?: JSONObject()
        val remoteDeletionLedger = runCatching { JSONObject(storage.optString(DELETIONS_KEY, "{}")) }.getOrNull()
        val deletionLedger = LearningProgressDeletionLedger.merge(appContext, remoteDeletionLedger)
        storage.put(DELETIONS_KEY, deletionLedger.toString())

        mergeStore(
            storage, BOOKMARKS_KEY, "items", "savedAt",
            filterDeletedItems(nativeBookmarks(database, questionIndex), deletionLedger, "bookmarks", "savedAt"), 500
        )
        filterDeletedStore(storage, BOOKMARKS_KEY, "items", "savedAt", "bookmarks", deletionLedger)
        mergeStore(
            storage, VAULT_KEY, "items", "lastWrongAt",
            filterDeletedItems(nativeMistakes(appContext, database), deletionLedger, "vault", "lastWrongAt"), 500
        )
        filterDeletedStore(storage, VAULT_KEY, "items", "lastWrongAt", "vault", deletionLedger)
        mergeStore(
            storage, STUDY_TIME_KEY, "segments", "at",
            nativeStudySegments(database), 400
        )
        mergeNativeHistory(storage, database)
        mergeNativeQuestionAttemptHistory(storage, appContext)

        return (remote?.let { JSONObject(it.toString()) } ?: JSONObject()).apply {
            put("version", 1)
            put("savedAt", System.currentTimeMillis())
            put("activeSessionChangedAt", optLong("activeSessionChangedAt", 0L))
            if (!has("changedSections") || optJSONArray("changedSections") == null) {
                put("changedSections", JSONArray())
            }
            put("storage", storage)
            if (!has("activeSession")) put("activeSession", JSONObject.NULL)
        }
    }

    suspend fun applyCloudSnapshot(context: Context, snapshot: JSONObject?) {
        if (snapshot == null) return
        val storage = snapshot.optJSONObject("storage") ?: return
        val appContext = context.applicationContext
        val database = HyperionDatabase.getInstance(appContext)
        val remoteDeletionLedger = runCatching { JSONObject(storage.optString(DELETIONS_KEY, "{}")) }.getOrNull()
        val deletionLedger = LearningProgressDeletionLedger.merge(appContext, remoteDeletionLedger)
        storage.put(DELETIONS_KEY, deletionLedger.toString())
        applyLocalTombstones(appContext, database, deletionLedger)
        filterDeletedStore(storage, BOOKMARKS_KEY, "items", "savedAt", "bookmarks", deletionLedger)
        filterDeletedStore(storage, VAULT_KEY, "items", "lastWrongAt", "vault", deletionLedger)

        val questionIndex = loadQuestionIndex(database).toMutableMap()
        val bookmarkQuestions = questionItems(storage, BOOKMARKS_KEY, questionIndex)
        if (bookmarkQuestions.isNotEmpty()) {
            database.questionDao().insertAll(bookmarkQuestions.mapNotNull { it.toQuestion()?.toEntity() })
            val bookmarks = questionItems(storage, BOOKMARKS_KEY, questionIndex).mapNotNull { item ->
                val savedAt = item.optLong("savedAt", 0L).takeIf { it > 0L } ?: System.currentTimeMillis()
                item.optString("id").takeIf { it.isNotBlank() }?.let { BookmarkEntity(it, savedAt) }
            }
            if (bookmarks.isNotEmpty()) database.bookmarkDao().insertBookmarks(bookmarks)
        }

        applyCloudMistakes(context, database, storage)
        applyCloudStudySegments(database, storage)
        applyCloudHistory(database, storage)
        applyCloudQuestionAttemptHistory(context.applicationContext, storage)
    }

    private suspend fun loadQuestionIndex(database: HyperionDatabase): Map<String, Question> {
        val result = LinkedHashMap<String, Question>()
        MaritimeQuestionBank.ALL_QUESTIONS.forEach { result[it.id] = it }
        Track.values().forEach { track ->
            database.questionDao().getQuestionsByTrack(track.name).forEach { entity ->
                entity.toQuestion()?.let { result.putIfAbsent(it.id, it) }
            }
        }
        return result
    }

    private suspend fun nativeBookmarks(
        database: HyperionDatabase,
        questions: Map<String, Question>
    ): JSONArray {
        val result = JSONArray()
        database.bookmarkDao().getAllBookmarksOnce().forEach { bookmark ->
            val question = questions[bookmark.questionId] ?: return@forEach
            result.put(question.toQuestionJson().put("savedAt", bookmark.addedTimestamp))
        }
        return result
    }

    private suspend fun nativeMistakes(context: Context, database: HyperionDatabase): JSONArray {
        val roomMistakes = database.mistakeDao().getAllMistakesOnce().associateBy { it.questionId }
        val result = JSONArray()
        MistakesVaultManager.loadEntries(context).forEach { entry ->
            val question = entry.question
            val local = roomMistakes[entry.questionId]
            val lastWrong = local?.lastFailedTimestamp ?: 1L
            result.put(question.toQuestionJson()
                .put("wrongCount", (local?.timesFailed ?: 1).coerceAtLeast(1))
                .put("lastWrongAt", lastWrong)
                .put("lastSelectedIndex", JSONObject.NULL))
        }
        return result
    }


    private fun filterDeletedItems(
        items: JSONArray,
        ledger: JSONObject,
        collection: String,
        timestampKey: String
    ): JSONArray {
        val filtered = JSONArray()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank() || !LearningProgressDeletionLedger.isDeleted(
                    ledger, collection, id, item.optLong(timestampKey, 0L)
                )
            ) {
                filtered.put(item)
            }
        }
        return filtered
    }

    private fun filterDeletedStore(
        storage: JSONObject,
        key: String,
        arrayName: String,
        timestampKey: String,
        collection: String,
        ledger: JSONObject
    ) {
        val wrapper = runCatching { JSONObject(storage.optString(key, "{}")) }.getOrNull() ?: return
        val state = wrapper.optJSONObject("state") ?: return
        val items = state.optJSONArray(arrayName) ?: return
        val filtered = filterDeletedItems(items, ledger, collection, timestampKey)
        state.put(arrayName, filtered)
        wrapper.put("state", state)
        storage.put(key, wrapper.toString())
    }

    private suspend fun applyLocalTombstones(
        context: Context,
        database: HyperionDatabase,
        ledger: JSONObject
    ) {
        database.bookmarkDao().getAllBookmarksOnce()
            .filter { bookmark ->
                LearningProgressDeletionLedger.isDeleted(
                    ledger, "bookmarks", bookmark.questionId, bookmark.addedTimestamp
                )
            }
            .forEach { database.bookmarkDao().removeBookmark(it.questionId) }

        val roomMistakes = database.mistakeDao().getAllMistakesOnce().associateBy { it.questionId }
        val localVault = MistakesVaultManager.loadEntries(context)
        val deletedMistakes = localVault.filter { entry ->
            LearningProgressDeletionLedger.isDeleted(
                ledger, "vault", entry.questionId, roomMistakes[entry.questionId]?.lastFailedTimestamp ?: 1L
            )
        }
        deletedMistakes.forEach { database.mistakeDao().removeMistake(it.questionId) }
        if (deletedMistakes.isNotEmpty()) {
            val deletedIds = deletedMistakes.map { it.questionId }.toSet()
            MistakesVaultManager.saveEntries(context, localVault.filterNot { it.questionId in deletedIds })
        }
    }

    private suspend fun nativeStudySegments(database: HyperionDatabase): JSONArray {
        val segments = database.studyTimeSegmentDao().getAllSegmentsOnce()
            .sortedByDescending { it.endEpochMs }
            .take(400)
        val result = JSONArray()
        segments.forEach { segment ->
            val track = runCatching { Track.valueOf(segment.trackId).name }.getOrNull() ?: return@forEach
            val day = Instant.ofEpochMilli(segment.startEpochMs)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()
            val mode = if (segment.mode.equals("EXAM", ignoreCase = true)) "MOCK" else "DRILL"
            result.put(JSONObject()
                .put("id", segment.segmentId)
                .put("day", day)
                .put("mode", mode)
                .put("track", track)
                .put("durationMs", segment.durationMs.coerceAtLeast(0L))
                .put("at", segment.endEpochMs))
        }
        return result
    }

    private suspend fun mergeNativeHistory(storage: JSONObject, database: HyperionDatabase) {
        val segments = database.studyTimeSegmentDao().getAllSegmentsOnce().groupBy { it.sessionId }
        val entries = JSONArray()
        database.examRecordDao().getAllExamRecordsOnce().forEach { record ->
            val summary = record.detailsSummary.uppercase()
            if (summary.startsWith("IN_PROGRESS") || summary.startsWith("INCOMPLETE") || record.id.startsWith("inc_")) {
                return@forEach
            }
            val track = runCatching { Track.valueOf(record.track).name }.getOrNull() ?: return@forEach
            val mode = when {
                record.mode.contains("MOCK", true) -> "MOCK"
                record.mode.contains("DRILL", true) || record.mode.contains("MISTAKE", true) || record.competence.equals("MISTAKES", true) -> "DRILL"
                else -> "PRACTICE"
            }
            val duration = segments[record.id].orEmpty().sumOf { it.durationMs }.coerceAtLeast(0L)
            val finishedAt = record.timestamp.coerceAtLeast(0L)
            val answeredCount = if (record.answersJson.isNotBlank()) {
                SessionJsonHelper.deserializeAnswers(record.answersJson).values.count { it.selectedIndex != null }
            } else {
                record.totalQuestions
            }.coerceIn(0, record.totalQuestions.coerceAtLeast(0))
            entries.put(JSONObject()
                .put("id", record.id)
                .put("mode", mode)
                .put("track", track)
                .put("competenceId", record.competence ?: JSONObject.NULL)
                .put("part", record.part?.toIntOrNull() ?: JSONObject.NULL)
                .put("correct", record.correctCount)
                .put("total", record.totalQuestions)
                .put("answered", answeredCount)
                .put("percent", record.percentage)
                .put("durationMs", duration)
                .put("startedAt", (finishedAt - duration).coerceAtLeast(0L))
                .put("finishedAt", finishedAt))
        }
        mergeStore(storage, HISTORY_KEY, "entries", "finishedAt", entries, 200)
    }


    /**
     * Store question-level attempt telemetry as a transport-only progress section.
     * The PWA preserves this opaque section even though its current UI does not
     * render Android's per-question attempt bars.
     */
    private fun mergeNativeQuestionAttemptHistory(storage: JSONObject, context: Context) {
        val localEntries = QuestionAttemptHistoryManager.loadAll(context)
        val localStateEntries = JSONObject()
        localEntries.forEach { (key, entry) ->
            val attempts = JSONArray()
            entry.attempts.forEach { attempt ->
                attempts.put(JSONObject()
                    .put("id", attempt.id)
                    .put("timestamp", attempt.timestamp)
                    .put("isCorrect", attempt.isCorrect))
            }
            localStateEntries.put(key, JSONObject()
                .put("questionId", entry.questionId)
                .put("track", entry.track)
                .put("allTimeCorrect", entry.allTimeCorrect)
                .put("allTimeIncorrect", entry.allTimeIncorrect)
                .put("attempts", attempts))
        }

        val remoteWrapper = runCatching {
            JSONObject(storage.optString(QUESTION_ATTEMPTS_KEY, "{}"))
        }.getOrNull() ?: JSONObject()
        val remoteEntries = remoteWrapper.optJSONObject("state")?.optJSONObject("entries") ?: JSONObject()
        val mergedEntries = JSONObject()
        val keys = LinkedHashSet<String>()
        remoteEntries.keys().forEach { keys.add(it) }
        localStateEntries.keys().forEach { keys.add(it) }

        for (key in keys) {
            val remoteEntry = remoteEntries.optJSONObject(key) ?: JSONObject()
            val localEntry = localStateEntries.optJSONObject(key) ?: JSONObject()
            val byId = LinkedHashMap<String, JSONObject>()
            listOf(remoteEntry, localEntry).forEach { entry ->
                val attempts = entry.optJSONArray("attempts") ?: JSONArray()
                for (index in 0 until attempts.length()) {
                    val attempt = attempts.optJSONObject(index) ?: continue
                    val id = attempt.optString("id").takeIf { it.isNotBlank() }
                        ?: "${attempt.optLong("timestamp")}:${if (attempt.optBoolean("isCorrect")) "correct" else "incorrect"}"
                    byId[id] = JSONObject(attempt.toString()).put("id", id)
                }
            }
            val mergedAttempts = JSONArray()
            byId.values.sortedBy { it.optLong("timestamp") }.forEach { mergedAttempts.put(it) }
            val correctFromAttempts = byId.values.count { it.optBoolean("isCorrect") }
            val incorrectFromAttempts = byId.size - correctFromAttempts
            val base = if (localEntry.length() > 0) localEntry else remoteEntry
            mergedEntries.put(key, JSONObject()
                .put("questionId", base.optString("questionId"))
                .put("track", base.optString("track"))
                .put("allTimeCorrect", maxOf(remoteEntry.optInt("allTimeCorrect"), localEntry.optInt("allTimeCorrect"), correctFromAttempts))
                .put("allTimeIncorrect", maxOf(remoteEntry.optInt("allTimeIncorrect"), localEntry.optInt("allTimeIncorrect"), incorrectFromAttempts))
                .put("attempts", mergedAttempts))
        }

        storage.put(QUESTION_ATTEMPTS_KEY, JSONObject()
            .put("version", 1)
            .put("state", JSONObject().put("entries", mergedEntries))
            .toString())
    }

    private fun applyCloudQuestionAttemptHistory(context: Context, storage: JSONObject) {
        val wrapper = runCatching { JSONObject(storage.optString(QUESTION_ATTEMPTS_KEY, "{}")) }.getOrNull() ?: return
        val entries = wrapper.optJSONObject("state")?.optJSONObject("entries") ?: return
        val incoming = LinkedHashMap<String, QuestionHistoryEntry>()
        val keys = entries.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val item = entries.optJSONObject(key) ?: continue
            val questionId = item.optString("questionId").takeIf { it.isNotBlank() } ?: continue
            val track = item.optString("track").takeIf { it.isNotBlank() } ?: continue
            val attemptsArray = item.optJSONArray("attempts") ?: JSONArray()
            val attempts = mutableListOf<QuestionAttemptRecord>()
            for (index in 0 until attemptsArray.length()) {
                val attempt = attemptsArray.optJSONObject(index) ?: continue
                val timestamp = attempt.optLong("timestamp", 0L)
                val isCorrect = attempt.optBoolean("isCorrect", false)
                val id = attempt.optString("id").takeIf { it.isNotBlank() }
                    ?: "${timestamp}:${if (isCorrect) "correct" else "incorrect"}"
                attempts += QuestionAttemptRecord(timestamp, isCorrect, id)
            }
            incoming[key] = QuestionHistoryEntry(
                questionId = questionId,
                track = track,
                allTimeCorrect = item.optInt("allTimeCorrect", attempts.count { it.isCorrect }).coerceAtLeast(0),
                allTimeIncorrect = item.optInt("allTimeIncorrect", attempts.count { !it.isCorrect }).coerceAtLeast(0),
                attempts = attempts
            )
        }
        QuestionAttemptHistoryManager.mergeSyncedEntries(context, incoming)
    }

    private fun mergeStore(
        storage: JSONObject,
        key: String,
        arrayName: String,
        timestampName: String,
        localItems: JSONArray,
        limit: Int
    ) {
        val wrapper = storage.optString(key, null)?.let {
            runCatching { JSONObject(it) }.getOrNull()
        } ?: JSONObject()
        val state = wrapper.optJSONObject("state") ?: JSONObject()
        val existing = state.optJSONArray(arrayName) ?: JSONArray()
        val byId = LinkedHashMap<String, JSONObject>()

        for (index in 0 until existing.length()) {
            val item = existing.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isNotBlank()) byId[id] = item
        }
        for (index in 0 until localItems.length()) {
            val item = localItems.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            val previous = byId[id]
            if (previous == null || item.optLong(timestampName) >= previous.optLong(timestampName)) {
                byId[id] = item
            }
        }

        val merged = byId.values.sortedByDescending { it.optLong(timestampName) }.take(limit)
        val output = JSONArray()
        merged.forEach { output.put(it) }
        state.put(arrayName, output)
        wrapper.put("state", state)
        if (!wrapper.has("version")) wrapper.put("version", 0)
        storage.put(key, wrapper.toString())
    }

    private fun questionItems(
        storage: JSONObject,
        key: String,
        questionIndex: MutableMap<String, Question>
    ): List<JSONObject> {
        val wrapper = runCatching { JSONObject(storage.optString(key, "{}")) }.getOrNull() ?: return emptyList()
        val items = wrapper.optJSONObject("state")?.optJSONArray("items") ?: return emptyList()
        val result = mutableListOf<JSONObject>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            item.toQuestion()?.let { question ->
                questionIndex[question.id] = question
                result += item
            }
        }
        return result
    }

    private suspend fun applyCloudMistakes(
        context: Context,
        database: HyperionDatabase,
        storage: JSONObject
    ) {
        val current = MistakesVaultManager.loadEntries(context).associateBy { it.questionId }.toMutableMap()
        val roomRows = database.mistakeDao().getAllMistakesOnce().associateBy { it.questionId }.toMutableMap()
        val wrapper = runCatching { JSONObject(storage.optString(VAULT_KEY, "{}")) }.getOrNull()
        val remoteItems = wrapper?.optJSONObject("state")?.optJSONArray("items") ?: JSONArray()

        for (index in 0 until remoteItems.length()) {
            val item = remoteItems.optJSONObject(index) ?: continue
            val question = item.toQuestion() ?: continue
            val id = question.id
            val lastWrong = item.optLong("lastWrongAt", 0L).coerceAtLeast(1L)
            val prior = roomRows[id]
            if (prior == null || lastWrong >= prior.lastFailedTimestamp) {
                current[id] = MistakeEntry(
                    questionId = id,
                    track = question.track.name,
                    competenceOrPart = question.competenceId,
                    consecutiveCorrect = current[id]?.consecutiveCorrect ?: 0,
                    question = question
                )
                val row = MistakeEntity(
                    questionId = id,
                    consecutiveCorrect = current[id]?.consecutiveCorrect ?: 0,
                    timesFailed = item.optInt("wrongCount", 1).coerceAtLeast(1),
                    lastFailedTimestamp = lastWrong
                )
                database.mistakeDao().insertOrUpdate(row)
                roomRows[id] = row
            }
        }
        // Preserve native-only mistake entries: the service may filter question-bearing
        // cloud records when the current account has no active entitlement for that track.
        val merged = current.values.toList()
        MistakesVaultManager.saveEntries(context, merged)
    }

    private suspend fun applyCloudStudySegments(database: HyperionDatabase, storage: JSONObject) {
        val wrapper = runCatching { JSONObject(storage.optString(STUDY_TIME_KEY, "{}")) }.getOrNull() ?: return
        val items = wrapper.optJSONObject("state")?.optJSONArray("segments") ?: return
        val existing = database.studyTimeSegmentDao().getAllSegmentsOnce().map { it.segmentId }.toHashSet()
        val newSegments = mutableListOf<StudyTimeSegmentEntity>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val id = item.optString("id")
            if (id.isBlank() || id in existing) continue
            val track = item.optString("track")
            if (runCatching { Track.valueOf(track) }.isFailure) continue
            val end = item.optLong("at", 0L)
            val duration = item.optLong("durationMs", 0L)
            if (end <= 0L || duration <= 0L || duration > 6L * 60L * 60L * 1000L) continue
            val mode = if (item.optString("mode").equals("MOCK", true)) "EXAM" else "DRILL"
            val part = item.optString("part").takeIf { it.isNotBlank() && it != "null" }
            newSegments += StudyTimeSegmentEntity(
                segmentId = id,
                sessionId = "PWA:$id",
                trackId = track,
                competence = item.optString("competenceId").takeIf { it.isNotBlank() && it != "null" },
                part = part,
                mode = mode,
                drillFormat = null,
                startEpochMs = end - duration,
                endEpochMs = end,
                durationMs = duration
            )
            existing += id
        }
        if (newSegments.isNotEmpty()) database.studyTimeSegmentDao().insertSegments(newSegments)
    }

    private suspend fun applyCloudHistory(database: HyperionDatabase, storage: JSONObject) {
        val wrapper = runCatching { JSONObject(storage.optString(HISTORY_KEY, "{}")) }.getOrNull() ?: return
        val items = wrapper.optJSONObject("state")?.optJSONArray("entries") ?: return
        val existing = database.examRecordDao().getAllExamRecordsOnce().map { it.id }.toHashSet()
        val newRecords = mutableListOf<ExamRecordEntity>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val id = item.optString("id")
            val deletedAt = item.optLong("deletedAt", 0L)
            if (id.isBlank() || id in existing || deletedAt > 0L) continue
            val track = item.optString("track")
            if (runCatching { Track.valueOf(track) }.isFailure) continue
            val mode = when (item.optString("mode")) {
                "MOCK" -> "MOCK_EXAM"
                "DRILL" -> "DRILL"
                else -> "PRACTICE"
            }
            val total = item.optInt("total", 0).coerceAtLeast(0)
            val correct = item.optInt("correct", 0).coerceIn(0, total)
            val timestamp = item.optLong("finishedAt", 0L).coerceAtLeast(1L)
            val competence = item.optString("competenceId").takeIf { it.isNotBlank() && it != "null" }
            val part = item.opt("part")?.let { if (it == JSONObject.NULL) null else it.toString() }
            newRecords += ExamRecordEntity(
                id = id,
                timestamp = timestamp,
                track = track,
                mode = mode,
                rawScore = correct,
                maxScore = total,
                percentage = item.optDouble("percent", if (total > 0) correct * 100.0 / total else 0.0),
                isPassed = item.optDouble("percent", 0.0) >= 70.0,
                totalQuestions = total,
                correctCount = correct,
                detailsSummary = "SYNCED_FROM_HYPERION_ACCOUNT",
                questionIds = "",
                answersJson = "",
                competenceBreakdownJson = "",
                drillTitle = null,
                competence = competence,
                part = part
            )
            existing += id
        }
        if (newRecords.isNotEmpty()) database.examRecordDao().insertRecords(newRecords)
    }

    private fun Question.toQuestionJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("track", track.name)
        put("functionId", functionId)
        put("competenceId", competenceId)
        put("topic", topic)
        put("questionText", questionText)
        put("options", JSONArray(options))
        put("correctAnswerIndex", correctAnswerIndex)
        put("explanation", explanation)
        put("difficulty", difficulty.name)
    }

    private fun JSONObject.toQuestion(): Question? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val track = runCatching { Track.valueOf(optString("track")) }.getOrNull() ?: return null
        val text = optString("questionText")
        val optionsJson = optJSONArray("options") ?: return null
        val options = (0 until optionsJson.length()).map { optionsJson.optString(it) }
        if (text.isBlank() || options.isEmpty()) return null
        val correct = optInt("correctAnswerIndex", -1)
        if (correct !in options.indices) return null
        val difficulty = runCatching { Difficulty.valueOf(optString("difficulty")) }.getOrDefault(Difficulty.EASY)
        return Question(
            id = id,
            track = track,
            functionId = optString("functionId"),
            competenceId = optString("competenceId"),
            topic = optString("topic"),
            questionText = text,
            options = options,
            correctAnswerIndex = correct,
            explanation = optString("explanation"),
            difficulty = difficulty
        )
    }

    private fun com.nhyle.hyperionmaritime.data.local.QuestionEntity.toQuestion(): Question? {
        val track = runCatching { Track.valueOf(track) }.getOrNull() ?: return null
        val options = runCatching { JSONArray(optionsJson) }.getOrNull() ?: return null
        val difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.EASY)
        val values = (0 until options.length()).map { options.optString(it) }
        if (questionText.isBlank() || values.isEmpty() || correctAnswerIndex !in values.indices) return null
        return Question(
            id = id,
            track = track,
            functionId = functionId,
            competenceId = competence,
            topic = topic,
            questionText = questionText,
            options = values,
            correctAnswerIndex = correctAnswerIndex,
            explanation = explanation,
            difficulty = difficulty
        )
    }

    private fun Question.toEntity() = com.nhyle.hyperionmaritime.data.local.QuestionEntity(
        id = id,
        track = track.name,
        competence = competenceId,
        functionId = functionId,
        topic = topic,
        questionText = questionText,
        optionsJson = JSONArray(options).toString(),
        correctAnswerIndex = correctAnswerIndex,
        explanation = explanation,
        difficulty = difficulty.name
    )
}
