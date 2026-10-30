package com.nhyle.hyperionmaritime.data.local

import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class QuestionAttemptRecord(
    val timestamp: Long,
    val isCorrect: Boolean,
    val id: String = "${timestamp}:${if (isCorrect) "correct" else "incorrect"}"
)

data class QuestionHistoryEntry(
    val questionId: String,
    val track: String,
    val allTimeCorrect: Int,
    val allTimeIncorrect: Int,
    val attempts: List<QuestionAttemptRecord>
) {
    val totalAttempts: Int get() = allTimeCorrect + allTimeIncorrect

    /**
     * Chronological slice of the latest up to 24 attempts for UI bars.
     * Retains original order (oldest to newest within the slice).
     */
    val latest24Attempts: List<Boolean>
        get() = attempts.takeLast(24).map { it.isCorrect }

    fun formattedSummary(): String {
        val correctUnit = if (allTimeCorrect == 1) "time" else "times"
        val incorrectUnit = if (allTimeIncorrect == 1) "time" else "times"
        return "Correct $allTimeCorrect $correctUnit, incorrect $allTimeIncorrect $incorrectUnit"
    }
}

object QuestionAttemptHistoryManager {
    private const val TAG = "QuestionAttemptHistory"
    const val FILE_NAME = "question_attempt_history.json"
    const val BAK_FILE_NAME = "question_attempt_history.json.bak"

    private val lock = Any()

    @Volatile
    private var cachedEntries: MutableMap<String, QuestionHistoryEntry>? = null

    fun getHistoryFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    fun getBackupFile(context: Context): File {
        return File(context.filesDir, BAK_FILE_NAME)
    }

    fun makeKey(track: String, questionId: String): String {
        return "${track.trim()}::${questionId.trim()}"
    }

    fun getHistory(context: Context, track: String, questionId: String): QuestionHistoryEntry? = synchronized(lock) {
        val entries = loadEntriesInternal(context)
        val key = makeKey(track, questionId)
        return entries[key]
    }

    fun loadAll(context: Context): Map<String, QuestionHistoryEntry> = synchronized(lock) {
        return loadEntriesInternal(context)
    }

    fun recordAttempt(
        context: Context,
        track: String,
        questionId: String,
        isCorrect: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ): QuestionHistoryEntry = synchronized(lock) {
        val currentMap = loadEntriesInternal(context).toMutableMap()
        val key = makeKey(track, questionId)
        val existing = currentMap[key]

        val newRecord = QuestionAttemptRecord(
            timestamp = timestamp,
            isCorrect = isCorrect,
            id = java.util.UUID.randomUUID().toString()
        )
        val updatedEntry = if (existing != null) {
            existing.copy(
                allTimeCorrect = if (isCorrect) existing.allTimeCorrect + 1 else existing.allTimeCorrect,
                allTimeIncorrect = if (!isCorrect) existing.allTimeIncorrect + 1 else existing.allTimeIncorrect,
                attempts = existing.attempts + newRecord
            )
        } else {
            QuestionHistoryEntry(
                questionId = questionId,
                track = track,
                allTimeCorrect = if (isCorrect) 1 else 0,
                allTimeIncorrect = if (!isCorrect) 1 else 0,
                attempts = listOf(newRecord)
            )
        }

        currentMap[key] = updatedEntry
        cachedEntries = currentMap

        val writeSuccess = saveEntriesInternal(context, currentMap)
        if (!writeSuccess) {
            Log.e(TAG, "Safe replacement could not be committed. In-memory state updated, previous file preserved.")
        }

        return updatedEntry
    }

    private fun loadEntriesInternal(context: Context): Map<String, QuestionHistoryEntry> {
        val cached = cachedEntries
        if (cached != null) {
            return cached
        }

        val file = getHistoryFile(context)
        if (!file.exists()) {
            // A process can stop between moving the old file aside and promoting the
            // newly written temp file on API 24/25. Recover the last durable backup.
            val backup = getBackupFile(context)
            if (backup.exists()) {
                try {
                    val backupContent = backup.readText()
                    if (backupContent.isNotBlank()) {
                        val recovered = parseJson(backupContent)
                        backup.copyTo(file, overwrite = true)
                        cachedEntries = recovered.toMutableMap()
                        Log.i(TAG, "Recovered missing history file from backup.")
                        return recovered
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to recover missing history file from backup: ${e.message}")
                }
            }
            val empty = mutableMapOf<String, QuestionHistoryEntry>()
            cachedEntries = empty
            return empty
        }

        val content = try {
            file.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read history file: ${e.message}")
            return cachedEntries ?: emptyMap()
        }

        if (content.isBlank()) {
            val empty = mutableMapOf<String, QuestionHistoryEntry>()
            cachedEntries = empty
            return empty
        }

        try {
            val parsed = parseJson(content)
            cachedEntries = parsed.toMutableMap()
            return parsed
        } catch (e: JSONException) {
            Log.e(TAG, "Malformed JSON detected in history file. Preserving file without overwrite: ${e.message}")
            // Attempt to restore from backup file if available
            val backup = getBackupFile(context)
            if (backup.exists()) {
                try {
                    val bakContent = backup.readText()
                    if (bakContent.isNotBlank()) {
                        val parsedBak = parseJson(bakContent)
                        Log.i(TAG, "Successfully recovered history from backup file.")
                        cachedEntries = parsedBak.toMutableMap()
                        return parsedBak
                    }
                } catch (be: Exception) {
                    Log.e(TAG, "Backup file also corrupt or unreadable: ${be.message}")
                }
            }
            // Return cached or empty WITHOUT overwriting the original file
            return cachedEntries ?: emptyMap()
        }
    }

    private fun parseJson(content: String): Map<String, QuestionHistoryEntry> {
        val root = JSONObject(content)
        val entriesObj = root.optJSONObject("entries") ?: JSONObject()
        val result = mutableMapOf<String, QuestionHistoryEntry>()

        val keys = entriesObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val item = entriesObj.getJSONObject(key)
            val questionId = item.getString("questionId")
            val track = item.getString("track")
            val allTimeCorrect = item.optInt("allTimeCorrect", 0)
            val allTimeIncorrect = item.optInt("allTimeIncorrect", 0)

            val attemptsArray = item.optJSONArray("attempts") ?: JSONArray()
            val attemptsList = mutableListOf<QuestionAttemptRecord>()
            for (i in 0 until attemptsArray.length()) {
                val attObj = attemptsArray.getJSONObject(i)
                val timestamp = attObj.optLong("timestamp", 0L)
                val isCorrect = attObj.optBoolean("isCorrect", false)
                val stableId = attObj.optString("id").takeIf { it.isNotBlank() }
                    ?: "$timestamp:${if (isCorrect) "correct" else "incorrect"}"
                attemptsList.add(QuestionAttemptRecord(timestamp, isCorrect, stableId))
            }

            result[key] = QuestionHistoryEntry(
                questionId = questionId,
                track = track,
                allTimeCorrect = allTimeCorrect,
                allTimeIncorrect = allTimeIncorrect,
                attempts = attemptsList
            )
        }
        return result
    }

    private fun copyFileDurably(source: File, destination: File) {
        source.inputStream().use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
    }

    /**
     * API 24/25 replacement path. Both files live in the same app-private directory,
     * so rename is on the same filesystem. Keep a rollback copy until the new file lands.
     */
    private fun replaceFileWithLegacyRename(tempFile: File, targetFile: File): Boolean {
        val parent = targetFile.parentFile ?: return false
        val previousFile = if (targetFile.exists()) {
            File.createTempFile("qstats_previous_", ".json", parent).also {
                if (!it.delete()) throw IOException("Could not prepare history rollback file")
            }
        } else null

        if (previousFile != null && !targetFile.renameTo(previousFile)) {
            previousFile.delete()
            Log.e(TAG, "Could not move existing history aside; original file was preserved.")
            return false
        }

        if (tempFile.renameTo(targetFile)) {
            previousFile?.delete()
            return true
        }

        val restored = previousFile?.let { !it.exists() || it.renameTo(targetFile) } ?: !targetFile.exists()
        if (!restored) {
            Log.e(TAG, "New history replacement failed and rollback needs recovery from the durable backup.")
        }
        previousFile?.takeIf { it.exists() }?.delete()
        return false
    }

    private fun saveEntriesInternal(context: Context, map: Map<String, QuestionHistoryEntry>): Boolean {
        val targetFile = getHistoryFile(context)
        val backupFile = getBackupFile(context)
        val parentDir = targetFile.parentFile ?: context.filesDir

        val rootJson = JSONObject()
        rootJson.put("version", 1)
        val entriesObj = JSONObject()
        for ((k, entry) in map) {
            val entryObj = JSONObject()
            entryObj.put("questionId", entry.questionId)
            entryObj.put("track", entry.track)
            entryObj.put("allTimeCorrect", entry.allTimeCorrect)
            entryObj.put("allTimeIncorrect", entry.allTimeIncorrect)

            val attemptsArr = JSONArray()
            for (att in entry.attempts) {
                val attObj = JSONObject()
                attObj.put("id", att.id)
                attObj.put("timestamp", att.timestamp)
                attObj.put("isCorrect", att.isCorrect)
                attemptsArr.put(attObj)
            }
            entryObj.put("attempts", attemptsArr)
            entriesObj.put(k, entryObj)
        }
        rootJson.put("entries", entriesObj)
        val jsonString = rootJson.toString()

        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("qstats_tmp_", ".json", parentDir)
            tempFile.writeText(jsonString)

            if (!tempFile.exists() || tempFile.length() == 0L) {
                Log.e(TAG, "Temp file creation failed or was empty")
                return false
            }

            // Create and sync a durable backup before replacing the current history.
            if (targetFile.exists()) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    } else {
                        copyFileDurably(targetFile, backupFile)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Backup creation failed; original history was left untouched: ${e.message}")
                    return false
                }
            }

            // Use NIO atomic replacement where available. Android 7.x (API 24/25)
            // uses same-directory renames with rollback, avoiding File.toPath() which
            // is unavailable below API 26.
            val replaced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    Files.move(
                        tempFile.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                    true
                } catch (_: AtomicMoveNotSupportedException) {
                    try {
                        Files.move(
                            tempFile.toPath(),
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Standard replacement failed: ${e.message}")
                        false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Atomic replacement exception: ${e.message}")
                    false
                }
            } else {
                replaceFileWithLegacyRename(tempFile, targetFile)
            }

            if (!replaced) {
                // CRITICAL REQUIREMENT: Do not fall back to a direct overwrite of the original file
                Log.e(TAG, "Replacement failed. Original file preserved untouched.")
                return false
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Exception during safe persistence: ${e.message}")
            return false
        } finally {
            tempFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
        }
    }


    /**
     * Merge question-attempt telemetry imported from another signed-in device.
     * Attempt IDs make repeated syncs idempotent; historical totals are retained
     * even when an older app version did not persist individual attempt IDs.
     */
    fun mergeSyncedEntries(context: Context, incoming: Map<String, QuestionHistoryEntry>) = synchronized(lock) {
        if (incoming.isEmpty()) return@synchronized
        val merged = loadEntriesInternal(context).toMutableMap()
        for ((key, remote) in incoming) {
            val local = merged[key]
            if (local == null) {
                merged[key] = remote
                continue
            }
            val byId = LinkedHashMap<String, QuestionAttemptRecord>()
            (local.attempts + remote.attempts).forEach { attempt ->
                val stableId = attempt.id.ifBlank {
                    "${attempt.timestamp}:${if (attempt.isCorrect) "correct" else "incorrect"}"
                }
                byId[stableId] = attempt.copy(id = stableId)
            }
            val attempts = byId.values.sortedBy { it.timestamp }
            val correctFromAttempts = attempts.count { it.isCorrect }
            val incorrectFromAttempts = attempts.size - correctFromAttempts
            merged[key] = QuestionHistoryEntry(
                questionId = local.questionId,
                track = local.track,
                allTimeCorrect = maxOf(local.allTimeCorrect, remote.allTimeCorrect, correctFromAttempts),
                allTimeIncorrect = maxOf(local.allTimeIncorrect, remote.allTimeIncorrect, incorrectFromAttempts),
                attempts = attempts
            )
        }
        cachedEntries = merged
        if (!saveEntriesInternal(context, merged)) {
            Log.e(TAG, "Could not persist merged cross-device question-attempt history.")
        }
    }

    fun clearAll(context: Context) = synchronized(lock) {
        cachedEntries = mutableMapOf()
        val file = getHistoryFile(context)
        if (file.exists()) file.delete()
        val backup = getBackupFile(context)
        if (backup.exists()) backup.delete()
    }

    fun resetCache() = synchronized(lock) {
        cachedEntries = null
    }
}
