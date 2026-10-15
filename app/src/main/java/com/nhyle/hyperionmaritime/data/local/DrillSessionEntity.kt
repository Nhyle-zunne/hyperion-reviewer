package com.nhyle.hyperionmaritime.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "drill_sessions",
    indices = [
        Index(value = ["track", "competence", "isCompleted"]),
        Index(value = ["track", "isCompleted"])
    ]
)
data class DrillSessionEntity(
    @PrimaryKey val id: String,
    val track: String,
    val competence: String? = null,
    val part: String? = null,
    val isCompleted: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val correctCount: Int = 0,
    val answeredCount: Int = 0,
    val totalItems: Int = 0,
    val currentQuestionIndex: Int = 0,
    val questionIds: String = "",
    val answersJson: String = "",
    val isShuffled: Boolean = false,
    val deliveryMode: String = "STANDARD",
    val mode: String = "PRACTICE"
) {
    val timestamp: Long
        get() = lastUpdated

    val formattedTimestamp: String
        get() = DrillSessionFormatter.formatTimestamp(lastUpdated)

    val scopeLabel: String
        get() = DrillSessionFormatter.formatScopeLabel(competence, part, track)

    val passingRate: Double
        get() = DrillSessionFormatter.calculatePassingPercentage(correctCount, answeredCount)
}

object DrillSessionFormatter {
    fun formatTimestamp(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.US)
        return sdf.format(java.util.Date(millis))
    }

    fun formatScopeLabel(competence: String?, part: String?, track: String): String {
        val comp = competence?.trim() ?: ""
        val p = part?.trim() ?: ""
        val isGmdss = track.contains("GMDSS", ignoreCase = true) || track.equals("GMDSS_GOC", ignoreCase = true)

        if (comp.equals("MISTAKES", ignoreCase = true)) {
            return "Mistakes"
        }

        return when {
            comp.isNotEmpty() && p.isNotEmpty() && !p.equals("All Parts", ignoreCase = true) && !p.equals("ALL", ignoreCase = true) -> {
                val cleanPart = p.replace(Regex("(?i)part\\s*"), "P").trim()
                val finalPart = if (!cleanPart.startsWith("P", ignoreCase = true)) "P$cleanPart" else cleanPart.uppercase()
                "$comp $finalPart"
            }
            comp.isNotEmpty() && isGmdss && (p.isEmpty() || p.equals("All Parts", ignoreCase = true) || p.equals("ALL", ignoreCase = true)) -> {
                "$comp · ALL PARTS"
            }
            comp.isNotEmpty() -> {
                comp
            }
            p.isNotEmpty() -> {
                val cleanPart = p.replace(Regex("(?i)part\\s*"), "P").trim()
                if (!cleanPart.startsWith("P", ignoreCase = true)) "P$cleanPart" else cleanPart.uppercase()
            }
            else -> track.replace("_", "-")
        }
    }

    fun formatCompetenceLabel(competence: String?, part: String?, track: String): String {
        val comp = competence?.trim() ?: ""
        val p = part?.trim() ?: ""
        if (comp.equals("MISTAKES", ignoreCase = true)) {
            return "Mistakes"
        }
        return when {
            comp.isNotEmpty() && p.isNotEmpty() -> {
                val cleanPart = if (p.startsWith("Part", ignoreCase = true)) p else "Part $p"
                "$comp $cleanPart"
            }
            comp.isNotEmpty() -> comp
            p.isNotEmpty() -> p
            else -> track.replace("_", "-")
        }
    }

    fun calculatePassingPercentage(correctCount: Int, answeredCount: Int): Double {
        return if (answeredCount > 0) {
            (correctCount.toDouble() / answeredCount.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    fun formatPassingPercentage(correctCount: Int, answeredCount: Int): String {
        val pct = calculatePassingPercentage(correctCount, answeredCount)
        return String.format(java.util.Locale.US, "%.2f%%", pct)
    }

    fun formatProgress(answeredCount: Int, totalItems: Int): String {
        return "$answeredCount/$totalItems"
    }

    fun formatSessionLine(session: DrillSessionEntity): String {
        val compLabel = formatCompetenceLabel(session.competence, session.part, session.track)
        val pctStr = formatPassingPercentage(session.correctCount, session.answeredCount)
        val progStr = formatProgress(session.answeredCount, session.totalItems)
        return "$compLabel / $pctStr / $progStr"
    }
}
