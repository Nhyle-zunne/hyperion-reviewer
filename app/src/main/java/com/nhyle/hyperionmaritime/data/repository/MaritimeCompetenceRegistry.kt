package com.nhyle.hyperionmaritime.data.repository

import com.nhyle.hyperionmaritime.data.model.CompetenceInfo
import com.nhyle.hyperionmaritime.data.model.Track

object MaritimeCompetenceRegistry {

    const val OPERATIONAL_REVIEW_LIMIT_C1 = 900
    const val OPERATIONAL_REVIEW_LIMIT_C2 = 130

    val ALL_DECK_COMPETENCES get() = DECK_COMPETENCES
    val ALL_GMDSS_COMPETENCES get() = GMDSS_COMPETENCES

    val DECK_COMPETENCES = listOf(
        CompetenceInfo(
            competenceId = "C1",
            functionId = "F1",
            functionName = "NAVIGATION AT THE OPERATIONAL LEVEL",
            track = Track.DECK_OIC_NW,
            code = "C1",
            title = "Plan and conduct a passage and determine position",
            officialTOSItems = 831,
            description = "Terrestrial & celestial navigation, voyage planning, chart work, gyro & magnetic compass errors, tides & currents."
        ),
        CompetenceInfo(
            competenceId = "C2",
            functionId = "F1",
            functionName = "NAVIGATION AT THE OPERATIONAL LEVEL",
            track = Track.DECK_OIC_NW,
            code = "C2",
            title = "Maintain a safe navigational watch",
            officialTOSItems = 570,
            description = "COLREGs 1972 (Rules 1-38), STCW Watchkeeping Section A-VIII/2, bridge procedures, lookout duties."
        ),
        CompetenceInfo(
            competenceId = "C3",
            functionId = "F1",
            functionName = "NAVIGATION AT THE OPERATIONAL LEVEL",
            track = Track.DECK_OIC_NW,
            code = "C3",
            title = "Use of Radar and ARPA for the safety of navigation",
            officialTOSItems = 442,
            description = "Radar plotting, CPA/TCPA calculation, true vs relative motion, ARPA trial manoeuvres, clutter suppression."
        ),
        CompetenceInfo(
            competenceId = "C4",
            functionId = "F1",
            functionName = "NAVIGATION AT THE OPERATIONAL LEVEL",
            track = Track.DECK_OIC_NW,
            code = "C4",
            title = "Use of ECDIS for the safety of navigation",
            officialTOSItems = 307,
            description = "Electronic navigational charts (ENC), safety contours, display categories, sensors integration, over-reliance prevention."
        ),
        CompetenceInfo(
            competenceId = "C5",
            functionId = "F1",
            functionName = "NAVIGATION AT THE OPERATIONAL LEVEL",
            track = Track.DECK_OIC_NW,
            code = "C5",
            title = "Respond to emergencies",
            officialTOSItems = 356,
            description = "Man overboard maneuvers (Williamson, Scharnow), search & rescue (IAMSAR), collision, grounding, damage control."
        ),
        CompetenceInfo(
            competenceId = "C7",
            functionId = "F1",
            functionName = "NAVIGATION AT THE OPERATIONAL LEVEL",
            track = Track.DECK_OIC_NW,
            code = "C7",
            title = "Use the IMO SMCP and use English in written and oral form",
            officialTOSItems = 533,
            description = "Transmit and receive information by visual signalling / Morse & Flags."
        ),
        CompetenceInfo(
            competenceId = "C9",
            functionId = "F1",
            functionName = "NAVIGATION AT THE OPERATIONAL LEVEL",
            track = Track.DECK_OIC_NW,
            code = "C9",
            title = "Manoeuvre the ship",
            officialTOSItems = 508,
            description = "Ship handling, pivot point, turning circles, squat and shallow water effects, bank suction, anchoring."
        ),
        CompetenceInfo(
            competenceId = "C10",
            functionId = "F2",
            functionName = "CARGO HANDLING AND STOWAGE AT THE OPERATIONAL LEVEL",
            track = Track.DECK_OIC_NW,
            code = "C10",
            title = "Monitor the loading, stowage, securing, care during the voyage and the unloading of cargoes",
            officialTOSItems = 610,
            description = "IMDG Code hazard classes, cargo securing manual (CSM), bulk grain regulations, dangerous goods segregation."
        ),
        CompetenceInfo(
            competenceId = "C11",
            functionId = "F2",
            functionName = "CARGO HANDLING AND STOWAGE AT THE OPERATIONAL LEVEL",
            track = Track.DECK_OIC_NW,
            code = "C11",
            title = "Inspect and report defects and damage to cargo spaces, hatch covers and ballast tanks",
            officialTOSItems = 421,
            description = "Ultrasonic hatch testing, corrosion detection, Enhanced Survey Programme (ESP), structural deformation."
        ),
        CompetenceInfo(
            competenceId = "C12",
            functionId = "F3",
            functionName = "CONTROLLING OF OPERATION OF THE SHIP AT THE OPERATIONAL LEVEL",
            track = Track.DECK_OIC_NW,
            code = "C12",
            title = "Ensure compliance with pollution-prevention requirements",
            officialTOSItems = 351,
            description = "MARPOL Annexes I to VI (Oily water separator 15ppm, sewage, garbage special areas, SOx/NOx ECA emissions)."
        ),
        CompetenceInfo(
            competenceId = "C13",
            functionId = "F3",
            functionName = "CONTROLLING OF OPERATION OF THE SHIP AT THE OPERATIONAL LEVEL",
            track = Track.DECK_OIC_NW,
            code = "C13",
            title = "Maintain seaworthiness of the ship",
            officialTOSItems = 888,
            description = "Ship stability, transverse metacentric height (GM), free surface effect, load lines, damage stability."
        ),
        CompetenceInfo(
            competenceId = "C17",
            functionId = "F3",
            functionName = "CONTROLLING OF OPERATION OF THE SHIP AT THE OPERATIONAL LEVEL",
            track = Track.DECK_OIC_NW,
            code = "C17",
            title = "Monitor compliance with legislative requirements",
            officialTOSItems = 628,
            description = "Maritime Labour Convention (MLC 2006) rest hours, STCW compliance, Port State Control inspections, ISM Code."
        )
    )

    val GMDSS_COMPETENCES = listOf(
        CompetenceInfo(
            competenceId = "C1",
            functionId = "GMDSS",
            functionName = "GMDSS Radio Operations",
            track = Track.GMDSS_GOC,
            code = "C1",
            title = "Transmit and receive information using GMDSS subsystems and equipment",
            officialTOSItems = OPERATIONAL_REVIEW_LIMIT_C1,
            description = "GMDSS Operational Review • $OPERATIONAL_REVIEW_LIMIT_C1 Questions (DSC, NAVTEX, EPIRB, SART, Inmarsat-C, VHF/MF/HF)"
        ),
        CompetenceInfo(
            competenceId = "C2",
            functionId = "GMDSS",
            functionName = "GMDSS Radio Operations",
            track = Track.GMDSS_GOC,
            code = "C2",
            title = "Provide radio services in emergencies",
            officialTOSItems = OPERATIONAL_REVIEW_LIMIT_C2,
            description = "GMDSS Operational Review • $OPERATIONAL_REVIEW_LIMIT_C2 Questions (Distress alerting, SAR on-scene communications)"
        )
    )

    fun getCompetencesForTrack(track: Track): List<CompetenceInfo> {
        return when (track) {
            Track.DECK_OIC_NW -> DECK_COMPETENCES
            Track.GMDSS_GOC -> GMDSS_COMPETENCES
            Track.ENGINE_OIC_EW -> ENGINE_OIC_EW_COMPETENCES
            Track.DECK_MGT -> DECK_MGT_COMPETENCES
            Track.ENGINE_MGT -> ENGINE_MGT_COMPETENCES
        }
    }

    fun getCompetenceById(id: String, track: Track? = null): CompetenceInfo? {
        val all = DECK_COMPETENCES + GMDSS_COMPETENCES + ENGINE_OIC_EW_COMPETENCES + DECK_MGT_COMPETENCES + ENGINE_MGT_COMPETENCES
        return if (track != null) {
            all.find { it.track == track && it.competenceId == id } ?: all.find { it.competenceId == id }
        } else {
            all.find { it.competenceId == id }
        }
    }

    fun getFunctionTitle(functionId: String): String {
        return when (functionId) {
            "F1" -> "NAVIGATION AT THE OPERATIONAL LEVEL"
            "F2" -> "CARGO HANDLING AND STOWAGE AT THE OPERATIONAL LEVEL"
            "F3" -> "CONTROLLING OF OPERATION OF THE SHIP AT THE OPERATIONAL LEVEL"
            "GMDSS" -> "GMDSS General Operator Certificate (GOC)"
            else -> functionId
        }
    }

    fun getFunctionTitle(functionId: String, track: Track): String {
        return when (track) {
            Track.ENGINE_OIC_EW -> ENGINE_OIC_EW_FUNCTION_TITLES[functionId] ?: functionId
            Track.DECK_MGT -> DECK_MGT_FUNCTION_TITLES[functionId] ?: functionId
            Track.ENGINE_MGT -> ENGINE_MGT_FUNCTION_TITLES[functionId] ?: functionId
            else -> getFunctionTitle(functionId)
        }
    }

    fun getPermitDistribution(track: Track): Map<String, Int> {
        return when (track) {
            Track.DECK_OIC_NW -> OFFICIAL_DECK_PERMIT_DISTRIBUTION
            Track.GMDSS_GOC -> OFFICIAL_GMDSS_PERMIT_DISTRIBUTION
            Track.ENGINE_OIC_EW -> OFFICIAL_ENGINE_OIC_EW_PERMIT_DISTRIBUTION
            Track.DECK_MGT -> OFFICIAL_DECK_MGT_PERMIT_DISTRIBUTION
            Track.ENGINE_MGT -> OFFICIAL_ENGINE_MGT_PERMIT_DISTRIBUTION
        }
    }

    val OFFICIAL_DECK_PERMIT_DISTRIBUTION = mapOf(
        "C1" to 35,
        "C2" to 20,
        "C3" to 15,
        "C4" to 10,
        "C5" to 10,
        "C7" to 20,
        "C9" to 15,
        "C10" to 15,
        "C11" to 15,
        "C12" to 15,
        "C13" to 15,
        "C17" to 15
    )

    /**
     * Restricted question distribution for Free-Tier MARINA CRP-Based Practice.
     * Total: exactly 2,065 items across 12 Deck OIC-NW competencies.
     */
    val CRP_DECK_DISTRIBUTION = mapOf(
        "C1" to 420,
        "C2" to 221,
        "C3" to 258,
        "C4" to 160,
        "C5" to 100,
        "C7" to 196,
        "C9" to 176,
        "C10" to 109,
        "C11" to 96,
        "C12" to 110,
        "C13" to 122,
        "C17" to 97
    )

    /**
     * Question distribution for GMDSS.
     * C1: 1,911 questions
     * C2: 349 questions
     * Total: 2,260 questions.
     */
    val CRP_GMDSS_DISTRIBUTION = mapOf(
        "C1" to 1000,
        "C2" to 150
    )

    val CRP_GMDSS_PARTS_DISTRIBUTION = mapOf(
        "C1" to mapOf(1 to 500, 2 to 500),
        "C2" to mapOf(1 to 200)
    )

    val OFFICIAL_GMDSS_PERMIT_DISTRIBUTION = mapOf(
        "C1" to 85,
        "C2" to 15
    )

    fun getPermitItemCount(competenceId: String, track: Track): Int {
        if (track == Track.ENGINE_OIC_EW || track == Track.DECK_MGT || track == Track.ENGINE_MGT) {
            return getPermitDistribution(track)[competenceId] ?: 10
        }
        return if (track == Track.DECK_OIC_NW) {
            OFFICIAL_DECK_PERMIT_DISTRIBUTION[competenceId] ?: 15
        } else {
            OFFICIAL_GMDSS_PERMIT_DISTRIBUTION[competenceId] ?: 50
        }
    }

    fun getPermitItemsForFunction(functionId: String, track: Track): Int {
        if (track == Track.ENGINE_OIC_EW || track == Track.DECK_MGT || track == Track.ENGINE_MGT) {
            val dist = getPermitDistribution(track)
            return getCompetencesForTrack(track)
                .filter { it.functionId == functionId }
                .sumOf { dist[it.competenceId] ?: 0 }
        }
        return if (track == Track.DECK_OIC_NW) {
            when (functionId) {
                "F1" -> 125 // 35+20+15+10+10+20+15
                "F2" -> 30  // 15+15
                "F3" -> 45  // 15+15+15
                else -> 0
            }
        } else {
            100
        }
    }

    fun getOfficialCrpQuestionCount(competenceId: String, track: Track): Int {
        if (track == Track.ENGINE_OIC_EW || track == Track.DECK_MGT || track == Track.ENGINE_MGT) {
            return 0
        }
        return if (track == Track.DECK_OIC_NW) {
            CRP_DECK_DISTRIBUTION[competenceId] ?: 100
        } else {
            CRP_GMDSS_DISTRIBUTION[competenceId] ?: 200
        }
    }

    fun getFormattedOfficialCrpQuestionCount(competenceId: String, track: Track): String {
        val count = getOfficialCrpQuestionCount(competenceId, track)
        return if (count >= 1000) "%,d".format(count) else count.toString()
    }

    fun getDynamicQuestionCount(competenceId: String, track: Track, isCrpMode: Boolean = false, isProUnlocked: Boolean = true, isOperationalReview: Boolean = false): Int {
        val parsedCount = MaritimeQuestionBank.getQuestionsByCompetence(
            competenceId,
            track,
            isProUnlocked = true
        ).size

        if (track == Track.GMDSS_GOC && isOperationalReview) {
            val limit = when (competenceId) {
                "C1" -> OPERATIONAL_REVIEW_LIMIT_C1
                "C2" -> OPERATIONAL_REVIEW_LIMIT_C2
                else -> OPERATIONAL_REVIEW_LIMIT_C1 + OPERATIONAL_REVIEW_LIMIT_C2
            }
            return minOf(parsedCount, limit)
        }
        if (isCrpMode && track == Track.DECK_OIC_NW) {
            return minOf(parsedCount, getOfficialCrpQuestionCount(competenceId, track))
        }

        // Counts are based on the fully parsed Question objects. Apply the same
        // free-tier selection size used by the reviewer when a free pool is requested.
        if (!isProUnlocked && track in listOf(Track.ENGINE_OIC_EW, Track.DECK_MGT, Track.ENGINE_MGT)) {
            return (parsedCount * 0.40).roundToInt().coerceAtLeast(if (parsedCount > 0) 1 else 0)
        }
        if (!isProUnlocked && track == Track.GMDSS_GOC) {
            return minOf(parsedCount, if (competenceId == "C1") OPERATIONAL_REVIEW_LIMIT_C1 else OPERATIONAL_REVIEW_LIMIT_C2)
        }
        if (!isProUnlocked && track == Track.DECK_OIC_NW) {
            return minOf(parsedCount, getOfficialCrpQuestionCount(competenceId, track))
        }
        return parsedCount
    }

    fun getFormattedQuestionCount(competenceId: String, track: Track, isCrpMode: Boolean = false, isProUnlocked: Boolean = true, isOperationalReview: Boolean = false): String {
        val count = getDynamicQuestionCount(competenceId, track, isCrpMode, isProUnlocked, isOperationalReview)
        return if (count >= 1000) "%,d".format(count) else count.toString()
    }

    fun getTotalTrackQuestionCount(track: Track, isCrpMode: Boolean = false, isProUnlocked: Boolean = true, isOperationalReview: Boolean = false): Int {
        if (!MaritimeQuestionBank.isLoaded) return 0
        if (isCrpMode) return getTotalStandardReviewQuestionCount(track, isProUnlocked = false)
        if (track == Track.GMDSS_GOC && isOperationalReview) {
            return getTotalStandardReviewQuestionCount(track, isProUnlocked = false)
        }

        // Full-bank dashboard and track-page totals always reflect the complete parsed bank,
        // regardless of the current user's license tier.
        return MaritimeQuestionBank.getQuestionsByTrack(track, isProUnlocked = true).size
    }

    fun getTotalStandardReviewQuestionCount(track: Track, isProUnlocked: Boolean): Int {
        if (!MaritimeQuestionBank.isLoaded) return 0
        if (track == Track.GMDSS_GOC && isProUnlocked) {
            return MaritimeQuestionBank.getQuestionsByTrack(track, isProUnlocked = true).size
        }
        return getCompetencesForTrack(track).sumOf { competence ->
            getDynamicQuestionCount(
                competence.competenceId,
                track,
                isCrpMode = track == Track.DECK_OIC_NW,
                isProUnlocked = isProUnlocked,
                isOperationalReview = track == Track.GMDSS_GOC
            )
        }
    }
}
