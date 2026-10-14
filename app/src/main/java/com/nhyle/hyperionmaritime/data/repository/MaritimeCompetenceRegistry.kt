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
            else -> emptyList()
        }
    }

    fun getCompetenceById(id: String, track: Track? = null): CompetenceInfo? {
        val all = DECK_COMPETENCES + GMDSS_COMPETENCES
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
        return if (track == Track.DECK_OIC_NW) {
            OFFICIAL_DECK_PERMIT_DISTRIBUTION[competenceId] ?: 15
        } else {
            OFFICIAL_GMDSS_PERMIT_DISTRIBUTION[competenceId] ?: 50
        }
    }

    fun getPermitItemsForFunction(functionId: String, track: Track): Int {
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
        if (track == Track.GMDSS_GOC) {
            return if (isOperationalReview) {
                when (competenceId) {
                    "C1" -> OPERATIONAL_REVIEW_LIMIT_C1
                    "C2" -> OPERATIONAL_REVIEW_LIMIT_C2
                    else -> OPERATIONAL_REVIEW_LIMIT_C1 + OPERATIONAL_REVIEW_LIMIT_C2
                }
            } else {
                when (competenceId) {
                    "C1" -> 1911
                    "C2" -> 349
                    else -> 2260
                }
            }
        }
        if (isCrpMode) {
            return getOfficialCrpQuestionCount(competenceId, track)
        }
        return when (competenceId) {
            "C1" -> 831
            "C2" -> 570
            "C3" -> 442
            "C4" -> 307
            "C5" -> 356
            "C7" -> 533
            "C9" -> 508
            "C10" -> 610
            "C11" -> 421
            "C12" -> 351
            "C13" -> 888
            "C17" -> 628
            else -> {
                val count = MaritimeQuestionBank.getQuestionsByCompetence(competenceId, track, isProUnlocked = true).size
                if (count > 0) count else (CRP_DECK_DISTRIBUTION[competenceId] ?: 100)
            }
        }
    }

    fun getFormattedQuestionCount(competenceId: String, track: Track, isCrpMode: Boolean = false, isProUnlocked: Boolean = true, isOperationalReview: Boolean = false): String {
        val count = getDynamicQuestionCount(competenceId, track, isCrpMode, isProUnlocked, isOperationalReview)
        return if (count >= 1000) "%,d".format(count) else count.toString()
    }

    fun getTotalTrackQuestionCount(track: Track, isCrpMode: Boolean = false, isProUnlocked: Boolean = true, isOperationalReview: Boolean = false): Int {
        if (track == Track.GMDSS_GOC) {
            if (isOperationalReview) {
                return OPERATIONAL_REVIEW_LIMIT_C1 + OPERATIONAL_REVIEW_LIMIT_C2
            }
            return if (isProUnlocked) 2260 else 1150
        }
        if (isCrpMode) {
            return if (track == Track.DECK_OIC_NW) 2065 else 1150
        }
        val count = MaritimeQuestionBank.getQuestionsByTrack(track, isProUnlocked = true).size
        return if (count > 0) count else 6444
    }
}
