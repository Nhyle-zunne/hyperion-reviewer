package com.nhyle.hyperionmaritime.data.mtestm

import android.content.Context
import android.util.Log
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ImportedSheetsManager {

    private const val TAG = "ImportedSheetsManager"
    private const val FILE_NAME = "mtestm_imported_sheets.json"

    private fun getFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    fun loadSheets(context: Context): List<ImportedSheet> {
        val file = getFile(context)
        if (!file.exists()) return emptyList()

        return try {
            val content = file.readText()
            val jsonArray = JSONArray(content)
            val list = mutableListOf<ImportedSheet>()

            for (i in 0 until jsonArray.length()) {
                val sheetObj = jsonArray.getJSONObject(i)
                val id = sheetObj.optString("id", "sheet_${System.currentTimeMillis()}_$i")
                val title = sheetObj.optString("title", "Imported Sheet")
                val count = sheetObj.optInt("questionCount", 0)
                val importedAt = sheetObj.optLong("importedAtMillis", System.currentTimeMillis())

                val questionsArray = sheetObj.optJSONArray("questions") ?: JSONArray()
                val questions = mutableListOf<Question>()

                for (j in 0 until questionsArray.length()) {
                    val qObj = questionsArray.getJSONObject(j)
                    val optArray = qObj.optJSONArray("options") ?: JSONArray()
                    val options = mutableListOf<String>()
                    for (k in 0 until optArray.length()) {
                        options.add(optArray.getString(k))
                    }

                    questions.add(
                        Question(
                            id = qObj.optString("id", "q_${i}_${j}"),
                            track = Track.OIC_NW,
                            functionId = qObj.optString("functionId", "IMPORTED"),
                            competenceId = qObj.optString("competenceId", title),
                            topic = qObj.optString("topic", title),
                            questionText = qObj.optString("questionText", ""),
                            options = options,
                            correctAnswerIndex = qObj.optInt("correctAnswerIndex", 0),
                            explanation = qObj.optString("explanation", ""),
                            difficulty = Difficulty.MODERATE
                        )
                    )
                }

                list.add(
                    ImportedSheet(
                        id = id,
                        title = title,
                        questionCount = if (questions.isNotEmpty()) questions.size else count,
                        importedAtMillis = importedAt,
                        questions = questions
                    )
                )
            }
            Log.d(TAG, "Loaded ${list.size} sheet(s) from ${file.absolutePath}")
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load imported sheets from storage", e)
            emptyList()
        }
    }

    fun saveSheets(context: Context, sheets: List<ImportedSheet>) {
        try {
            val file = getFile(context)
            file.parentFile?.mkdirs()
            val jsonArray = JSONArray()

            sheets.forEach { sheet ->
                val sheetObj = JSONObject().apply {
                    put("id", sheet.id)
                    put("title", sheet.title)
                    put("questionCount", sheet.questions.size)
                    put("importedAtMillis", sheet.importedAtMillis)

                    val questionsArray = JSONArray()
                    sheet.questions.forEach { q ->
                        val qObj = JSONObject().apply {
                            put("id", q.id)
                            put("functionId", q.functionId)
                            put("competenceId", q.competenceId)
                            put("topic", q.topic)
                            put("questionText", q.questionText)
                            put("correctAnswerIndex", q.correctAnswerIndex)
                            put("explanation", q.explanation)

                            val optArray = JSONArray()
                            q.options.forEach { optArray.put(it) }
                            put("options", optArray)
                        }
                        questionsArray.put(qObj)
                    }
                    put("questions", questionsArray)
                }
                jsonArray.put(sheetObj)
            }

            file.writeText(jsonArray.toString())
            Log.d(TAG, "Saved ${sheets.size} sheet(s) to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save imported sheets to storage", e)
        }
    }

    fun addSheets(context: Context, newSheets: List<ImportedSheet>) {
        val current = loadSheets(context).toMutableList()
        // Replace sheets with identical titles or add new ones
        newSheets.forEach { newSheet ->
            current.removeAll { it.title.equals(newSheet.title, ignoreCase = true) }
            current.add(0, newSheet)
        }
        saveSheets(context, current)
    }

    fun deleteSheet(context: Context, sheetId: String) {
        val current = loadSheets(context).toMutableList()
        current.removeAll { it.id == sheetId }
        saveSheets(context, current)
    }

    fun clearAll(context: Context) {
        val file = getFile(context)
        if (file.exists()) {
            file.delete()
        }
    }
}