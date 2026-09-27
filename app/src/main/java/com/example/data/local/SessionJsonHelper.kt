package com.example.data.local

import com.example.data.model.CompetenceScore
import com.example.data.model.UserQuestionState
import org.json.JSONArray
import org.json.JSONObject

object SessionJsonHelper {

    fun serializeAnswers(answers: Map<String, UserQuestionState>): String {
        val jsonArray = JSONArray()
        for ((_, state) in answers) {
            val obj = JSONObject()
            obj.put("qid", state.questionId)
            if (state.selectedIndex != null) {
                obj.put("sel", state.selectedIndex)
            }
            obj.put("flag", state.isFlagged)
            obj.put("sub", state.isSubmitted)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun deserializeAnswers(jsonStr: String): Map<String, UserQuestionState> {
        val map = mutableMapOf<String, UserQuestionState>()
        if (jsonStr.isBlank()) return map
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val qid = obj.getString("qid")
                val sel = if (obj.has("sel")) obj.getInt("sel") else null
                val flag = obj.optBoolean("flag", false)
                val sub = obj.optBoolean("sub", false)
                map[qid] = UserQuestionState(
                    questionId = qid,
                    selectedIndex = sel,
                    isFlagged = flag,
                    isSubmitted = sub
                )
            }
        } catch (_: Exception) {}
        return map
    }

    fun serializeCompetenceScores(scores: List<CompetenceScore>): String {
        val array = JSONArray()
        for (s in scores) {
            val obj = JSONObject()
            obj.put("cid", s.competenceId)
            obj.put("title", s.competenceTitle)
            obj.put("fid", s.functionId)
            obj.put("qCount", s.questionCount)
            obj.put("cCount", s.correctCount)
            obj.put("raw", s.rawPointsEarned)
            obj.put("max", s.maxPointsPossible)
            obj.put("pct", s.percentage)
            obj.put("pass", s.isPassed)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeCompetenceScores(jsonStr: String): List<CompetenceScore> {
        val list = mutableListOf<CompetenceScore>()
        if (jsonStr.isBlank()) return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CompetenceScore(
                        competenceId = obj.getString("cid"),
                        competenceTitle = obj.optString("title", ""),
                        functionId = obj.optString("fid", ""),
                        questionCount = obj.getInt("qCount"),
                        correctCount = obj.getInt("cCount"),
                        rawPointsEarned = obj.getInt("raw"),
                        maxPointsPossible = obj.getInt("max"),
                        percentage = obj.getDouble("pct"),
                        isPassed = obj.getBoolean("pass")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
