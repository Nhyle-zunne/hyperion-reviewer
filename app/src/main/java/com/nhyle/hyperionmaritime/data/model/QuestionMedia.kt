package com.nhyle.hyperionmaritime.data.model

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Shared CSV HYPERION_MEDIA column contract for Android and PWA.
 *
 * Examples (inside ONE quoted CSV field):
 * [{"sha256":"<64 hex>","mime":"image/webp","placement":"question","alt":"Radar plot"}]
 *
 * References do NOT grant access to the image bytes. Licensed media must be
 * delivered by an authenticated, hash-verified source and saved privately.
 * Never embed remote URLs, local file paths or base64 image bytes in bank CSV.
 */
data class QuestionImageRef(
    val sha256: String,
    val mime: String,
    val placement: String,
    val alt: String
)

object QuestionMedia {
    const val COLUMN = "HYPERION_MEDIA"
    private const val MAX_ATTACHMENTS = 4
    private val shaPattern = Regex("^[a-f0-9]{64}$")
    private val validMime = setOf("image/webp", "image/png", "image/jpeg")
    private val validPlacement = setOf("question", "explanation")

    fun hasPrompt(text: String, images: List<QuestionImageRef>): Boolean =
        text.isNotBlank() || images.any { it.placement == "question" }

    fun toJson(images: List<QuestionImageRef>): JSONArray = JSONArray().apply {
        images.forEach { ref -> put(JSONObject().apply {
            put("sha256", ref.sha256)
            put("mime", ref.mime)
            put("placement", ref.placement)
            put("alt", ref.alt)
        }) }
    }

    fun fromCsvRow(header: List<String>, row: List<String>): List<QuestionImageRef> {
        val index = header.indexOfFirst { it.trim().equals(COLUMN, ignoreCase = true) }
        return if (index < 0) emptyList() else parse(row.getOrNull(index))
    }

    fun parse(raw: String?): List<QuestionImageRef> {
        if (raw.isNullOrBlank()) return emptyList()
        // An incorrectly quoted CSV cell or huge JSON payload is not allowed
        // to break the question bank or inflate device memory.
        if (raw.length > 2048) return emptyList()
        return try {
            val json = JSONArray(raw)
            if (json.length() !in 1..MAX_ATTACHMENTS) return emptyList()
            val seen = mutableSetOf<Pair<String, String>>()
            (0 until json.length()).map { index ->
                val item = json.getJSONObject(index)
                if (item.length() != 4) throw JSONException("Unexpected media fields")
                val sha = item.getString("sha256")
                val mime = item.getString("mime")
                val placement = item.getString("placement")
                val alt = item.getString("alt")
                if (!shaPattern.matches(sha) ||
                    mime !in validMime ||
                    placement !in validPlacement ||
                    alt.isBlank() || alt.length > 160 ||
                    alt.any { it.isISOControl() } ||
                    !seen.add(sha to placement)
                ) throw JSONException("Invalid image reference")
                QuestionImageRef(sha, mime, placement, alt)
            }
        } catch (_: Exception) {
            emptyList() // Fail closed for attachments, preserve original question.
        }
    }
}
