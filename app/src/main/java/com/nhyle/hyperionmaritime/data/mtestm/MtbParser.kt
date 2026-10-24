package com.nhyle.hyperionmaritime.data.mtestm

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class MtbSheetInfo(
    val examId: Int,
    val title: String,
    val questionCount: Int
)

data class ImportedSheet(
    val id: String,
    val title: String,
    val questionCount: Int = 0,
    val importedAtMillis: Long = System.currentTimeMillis(),
    val questions: List<Question>
)

object MtbParser {

    /**
     * Extracts mtestm.db from an .mtb (ZIP archive) file to cache directory.
     */
    fun extractMtbDbToCache(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            var targetFile: File? = null

            while (entry != null) {
                if (entry.name.endsWith(".db", ignoreCase = true) || entry.name.contains("mtestm", ignoreCase = true)) {
                    val tempDb = File(context.cacheDir, "mtb_${System.currentTimeMillis()}.db")
                    val outputStream = FileOutputStream(tempDb)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (zipInputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                    outputStream.close()
                    targetFile = tempDb
                    break
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
            inputStream.close()
            targetFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reads all exams (sheets) available in the extracted mtestm.db.
     */
    fun listSheets(dbFile: File): List<MtbSheetInfo> {
        val list = mutableListOf<MtbSheetInfo>()
        if (!dbFile.exists()) return list

        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT id, title, questionscount FROM exams ORDER BY id ASC", null)
            while (cursor.moveToNext()) {
                val id = cursor.getInt(0)
                val title = cursor.getString(1)?.trim().orEmpty()
                val count = cursor.getInt(2)
                list.add(MtbSheetInfo(examId = id, title = title, questionCount = count))
            }
            cursor.close()
        } catch (_: Exception) {
        } finally {
            try { db?.close() } catch (_: Exception) {}
        }
        return list
    }

    /**
     * Parses questions for the selected exam IDs into structured Question and ImportedSheet objects.
     */
    fun parseSelectedSheets(dbFile: File, selectedExamIds: Set<Int>): List<ImportedSheet> {
        val importedSheets = mutableListOf<ImportedSheet>()
        if (!dbFile.exists() || selectedExamIds.isEmpty()) return importedSheets

        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

            for (examId in selectedExamIds) {
                // Fetch sheet title
                var sheetTitle = "Sheet $examId"
                val examCursor = db.rawQuery("SELECT title FROM exams WHERE id = ?", arrayOf(examId.toString()))
                if (examCursor.moveToNext()) {
                    sheetTitle = examCursor.getString(0)?.trim().orEmpty().ifBlank { "Sheet $examId" }
                }
                examCursor.close()

                val questionsList = mutableListOf<Question>()

                // Query all questions for this exam
                val qCursor = db.rawQuery(
                    "SELECT q.no, q.descid, q.optiondescid, q.explanationdescid FROM questions q WHERE q.examid = ? ORDER BY q.no ASC",
                    arrayOf(examId.toString())
                )

                while (qCursor.moveToNext()) {
                    val qNo = qCursor.getInt(0)
                    val descId = qCursor.getInt(1)
                    val optId = qCursor.getInt(2)
                    val expId = qCursor.getInt(3)

                    // 1. Question Text
                    var questionText = ""
                    val textCursor = db.rawQuery(
                        "SELECT desc FROM questiondescs WHERE examid = ? AND id = ?",
                        arrayOf(examId.toString(), descId.toString())
                    )
                    if (textCursor.moveToNext()) {
                        questionText = textCursor.getString(0)?.trim().orEmpty()
                    }
                    textCursor.close()

                    if (questionText.isBlank()) continue

                    // 2. Options (ordered by option number)
                    val options = mutableListOf<String>()
                    val optCursor = db.rawQuery(
                        "SELECT desc FROM questiondescs WHERE examid = ? AND id = ? ORDER BY no ASC",
                        arrayOf(examId.toString(), optId.toString())
                    )
                    while (optCursor.moveToNext()) {
                        val optText = optCursor.getString(0)?.trim().orEmpty()
                        if (optText.isNotEmpty()) {
                            options.add(optText)
                        }
                    }
                    optCursor.close()

                    if (options.size < 2) continue

                    // 3. Explanation
                    var explanation = ""
                    if (expId > 0) {
                        val expCursor = db.rawQuery(
                            "SELECT desc FROM questiondescs WHERE examid = ? AND id = ?",
                            arrayOf(examId.toString(), expId.toString())
                        )
                        if (expCursor.moveToNext()) {
                            explanation = expCursor.getString(0)?.trim().orEmpty()
                        }
                        expCursor.close()
                    }

                    // 4. Correct Answer Index
                    var correctIndex = 0
                    val ansCursor = db.rawQuery(
                        "SELECT answer FROM correctanswers WHERE examid = ? AND no = ? LIMIT 1",
                        arrayOf(examId.toString(), qNo.toString())
                    )
                    if (ansCursor.moveToNext()) {
                        val rawAns = ansCursor.getString(0)?.trim().orEmpty().uppercase()
                        correctIndex = when (rawAns) {
                            "1", "A" -> 0
                            "2", "B" -> 1
                            "3", "C" -> 2
                            "4", "D" -> 3
                            else -> (rawAns.toIntOrNull()?.minus(1))?.coerceIn(0, options.lastIndex) ?: 0
                        }
                    }
                    ansCursor.close()

                    val questionId = "mtestm_${examId}_${qNo}"
                    questionsList.add(
                        Question(
                            id = questionId,
                            track = Track.OIC_NW,
                            functionId = "IMPORTED",
                            competenceId = sheetTitle,
                            topic = sheetTitle,
                            questionText = questionText,
                            options = options,
                            correctAnswerIndex = correctIndex.coerceIn(0, options.lastIndex),
                            explanation = explanation.ifBlank { "Imported from MTestM ($sheetTitle)." },
                            difficulty = Difficulty.MODERATE
                        )
                    )
                }
                qCursor.close()

                if (questionsList.isNotEmpty()) {
                    importedSheets.add(
                        ImportedSheet(
                            id = "sheet_${examId}_${System.currentTimeMillis()}",
                            title = sheetTitle,
                            questionCount = questionsList.size,
                            importedAtMillis = System.currentTimeMillis(),
                            questions = questionsList
                        )
                    )
                }
            }
        } catch (_: Exception) {
        } finally {
            try { db?.close() } catch (_: Exception) {}
        }
        return importedSheets
    }

    // ==========================================
    // --- CSV & Excel (.xlsx) Import / Export Engine ---
    // ==========================================

    fun parseCsv(context: Context, uri: Uri, fallbackTitle: String): List<ImportedSheet> {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
        val rows = parseCsvStream(inputStream)
        val sheetTitle = fallbackTitle.removeSuffix(".csv").ifBlank { "Imported CSV" }
        val questions = rowsToQuestions(rows, sheetTitle)
        if (questions.isEmpty()) return emptyList()
        return listOf(
            ImportedSheet(
                id = "csv_${System.currentTimeMillis()}",
                title = sheetTitle,
                questionCount = questions.size,
                importedAtMillis = System.currentTimeMillis(),
                questions = questions
            )
        )
    }

    fun parseXlsx(context: Context, uri: Uri, fallbackTitle: String): List<ImportedSheet> {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
        val tempDir = File(context.cacheDir, "xlsx_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        try {
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && (entry.name.startsWith("xl/") || entry.name.contains("sharedStrings") || entry.name.contains("sheet"))) {
                    val outFile = File(tempDir, entry.name.replace("/", "_"))
                    val fos = FileOutputStream(outFile)
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (zipInputStream.read(buffer).also { len = it } != -1) {
                        fos.write(buffer, 0, len)
                    }
                    fos.flush()
                    fos.close()
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            // 1. Read shared strings table
            val sharedStrings = mutableListOf<String>()
            val sharedStringsFile = tempDir.listFiles()?.firstOrNull { it.name.contains("sharedStrings", ignoreCase = true) }
            if (sharedStringsFile != null && sharedStringsFile.exists()) {
                parseSharedStrings(sharedStringsFile, sharedStrings)
            }

            // 2. Read all sheets/worksheets inside the workbook
            val sheetFiles = tempDir.listFiles()?.filter {
                it.name.contains("worksheets_sheet", ignoreCase = true) || (it.name.contains("sheet", ignoreCase = true) && !it.name.contains("sharedStrings"))
            }?.sortedBy { it.name } ?: emptyList()

            val baseTitle = fallbackTitle.removeSuffix(".xlsx").ifBlank { "Excel Workbook" }
            val importedSheets = mutableListOf<ImportedSheet>()

            for ((sheetIndex, sheetFile) in sheetFiles.withIndex()) {
                val sheetName = if (sheetFiles.size == 1) baseTitle else "$baseTitle (Sheet ${sheetIndex + 1})"
                val rows = parseXlsxSheetRows(sheetFile, sharedStrings)
                val questions = rowsToQuestions(rows, sheetName)
                if (questions.isNotEmpty()) {
                    importedSheets.add(
                        ImportedSheet(
                            id = "xlsx_${sheetIndex}_${System.currentTimeMillis()}",
                            title = sheetName,
                            questionCount = questions.size,
                            importedAtMillis = System.currentTimeMillis(),
                            questions = questions
                        )
                    )
                }
            }
            return importedSheets
        } catch (_: Exception) {
            return emptyList()
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun parseSharedStrings(file: File, outList: MutableList<String>) {
        try {
            val parser = android.util.Xml.newPullParser()
            parser.setInput(file.inputStream(), "UTF-8")
            var eventType = parser.eventType
            val currentString = StringBuilder()
            var inT = false
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        if (parser.name.equals("t", ignoreCase = true)) {
                            inT = true
                        } else if (parser.name.equals("si", ignoreCase = true)) {
                            currentString.setLength(0)
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> {
                        if (inT) currentString.append(parser.text)
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        if (parser.name.equals("t", ignoreCase = true)) {
                            inT = false
                        } else if (parser.name.equals("si", ignoreCase = true)) {
                            outList.add(currentString.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
    }

    private fun parseXlsxSheetRows(file: File, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            val parser = android.util.Xml.newPullParser()
            parser.setInput(file.inputStream(), "UTF-8")
            var eventType = parser.eventType
            var currentRow = mutableMapOf<Int, String>()
            var currentCellIndex = 0
            var currentCellType = ""
            val currentVal = StringBuilder()
            var inValue = false

            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "row" -> currentRow = mutableMapOf()
                            "c" -> {
                                val cellRef = parser.getAttributeValue(null, "r") ?: ""
                                currentCellIndex = getColumnIndex(cellRef)
                                currentCellType = parser.getAttributeValue(null, "t") ?: ""
                                currentVal.setLength(0)
                            }
                            "v", "t" -> inValue = true
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> {
                        if (inValue) currentVal.append(parser.text)
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        when (parser.name.lowercase()) {
                            "v", "t" -> inValue = false
                            "c" -> {
                                val rawStr = currentVal.toString().trim()
                                val resolved = if (currentCellType == "s") {
                                    val idx = rawStr.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawStr
                                } else {
                                    rawStr
                                }
                                currentRow[currentCellIndex] = resolved
                            }
                            "row" -> {
                                if (currentRow.isNotEmpty()) {
                                    val maxCol = (currentRow.keys.maxOrNull() ?: 0)
                                    val rowList = (0..maxCol).map { col -> currentRow[col].orEmpty() }
                                    rows.add(rowList)
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return rows
    }

    private fun getColumnIndex(cellRef: String): Int {
        var col = 0
        for (char in cellRef) {
            if (char in 'A'..'Z') {
                col = col * 26 + (char - 'A' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }

    private fun parseCsvStream(inputStream: java.io.InputStream): List<List<String>> {
        val text = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val result = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                if (c == '"') {
                    inQuotes = true
                } else if (c == ',') {
                    currentRow.add(currentField.toString().trim())
                    currentField.setLength(0)
                } else if (c == '\r') {
                    if (i + 1 < text.length && text[i + 1] == '\n') i++
                    currentRow.add(currentField.toString().trim())
                    currentField.setLength(0)
                    if (currentRow.any { it.isNotBlank() }) result.add(currentRow.toList())
                    currentRow.clear()
                } else if (c == '\n') {
                    currentRow.add(currentField.toString().trim())
                    currentField.setLength(0)
                    if (currentRow.any { it.isNotBlank() }) result.add(currentRow.toList())
                    currentRow.clear()
                } else {
                    currentField.append(c)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotBlank() }) result.add(currentRow.toList())
        }
        return result
    }

    private fun rowsToQuestions(rows: List<List<String>>, sheetTitle: String): List<Question> {
        val questions = mutableListOf<Question>()
        for ((index, row) in rows.withIndex()) {
            if (index == 0 && (row.getOrNull(1)?.contains("question", ignoreCase = true) == true || row.getOrNull(0)?.contains("no", ignoreCase = true) == true)) {
                continue
            }
            if (row.size < 6) continue

            val qNo = row.getOrNull(0)?.trim() ?: "${index + 1}"
            val qText = row.getOrNull(1)?.trim() ?: continue
            if (qText.isBlank()) continue

            val optA = row.getOrNull(2)?.trim() ?: ""
            val optB = row.getOrNull(3)?.trim() ?: ""
            val optC = row.getOrNull(4)?.trim() ?: ""
            val optD = row.getOrNull(5)?.trim() ?: ""
            val rawAns = row.getOrNull(6)?.trim()?.uppercase() ?: "A"
            val explanation = row.getOrNull(7)?.trim() ?: ""

            val options = listOf(optA, optB, optC, optD).filter { it.isNotEmpty() }
            if (options.size < 2) continue

            val correctIndex = when (rawAns) {
                "1", "A" -> 0
                "2", "B" -> 1
                "3", "C" -> 2
                "4", "D" -> 3
                else -> (rawAns.toIntOrNull()?.minus(1))?.coerceIn(0, options.lastIndex) ?: 0
            }

            questions.add(
                Question(
                    id = "import_${System.currentTimeMillis()}_${index}_${qNo}",
                    track = Track.OIC_NW,
                    functionId = "IMPORTED",
                    competenceId = sheetTitle,
                    topic = sheetTitle,
                    questionText = qText,
                    options = options,
                    correctAnswerIndex = correctIndex.coerceIn(0, options.lastIndex),
                    explanation = explanation.ifBlank { "Imported from $sheetTitle." },
                    difficulty = Difficulty.MODERATE
                )
            )
        }
        return questions
    }

    fun exportToCsv(sheet: ImportedSheet): String {
        val sb = StringBuilder()
        sb.append("No,Question,Choice A,Choice B,Choice C,Choice D,Answer,Explanation\n")
        sheet.questions.forEachIndexed { index, q ->
            val optA = q.options.getOrElse(0) { "" }
            val optB = q.options.getOrElse(1) { "" }
            val optC = q.options.getOrElse(2) { "" }
            val optD = q.options.getOrElse(3) { "" }
            val ansLetter = when (q.correctAnswerIndex) {
                0 -> "A"
                1 -> "B"
                2 -> "C"
                3 -> "D"
                else -> "A"
            }
            sb.append(escapeCsv((index + 1).toString())).append(",")
            sb.append(escapeCsv(q.questionText)).append(",")
            sb.append(escapeCsv(optA)).append(",")
            sb.append(escapeCsv(optB)).append(",")
            sb.append(escapeCsv(optC)).append(",")
            sb.append(escapeCsv(optD)).append(",")
            sb.append(escapeCsv(ansLetter)).append(",")
            sb.append(escapeCsv(q.explanation)).append("\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\r", "")
        return if (clean.contains(",") || clean.contains("\"") || clean.contains("\n")) {
            "\"" + clean.replace("\"", "\"\"") + "\""
        } else {
            clean
        }
    }
}