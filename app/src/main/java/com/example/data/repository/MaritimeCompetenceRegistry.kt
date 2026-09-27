package com.example.data.repository

import com.example.data.model.CompetenceInfo
import com.example.data.model.Track

object MaritimeCompetenceRegistry {

    val DECK_COMPETENCES = listOf(
        CompetenceInfo(
            competenceId = "C1",
            functionId = "F1",
            functionName = "Function 1: Navigation at Operational Level",
            track = Track.DECK_OIC_NW,
            code = "C1",
            title = "Plan and conduct a passage and determine position",
            officialTOSItems = 35,
            description = "Terrestrial & celestial navigation, voyage planning, chart work, gyro & magnetic compass errors, tides & currents."
        ),
        CompetenceInfo(
            competenceId = "C2",
            functionId = "F1",
            functionName = "Function 1: Navigation at Operational Level",
            track = Track.DECK_OIC_NW,
            code = "C2",
            title = "Maintain a safe navigational watch",
            officialTOSItems = 20,
            description = "COLREGs 1972 (Rules 1-38), STCW Watchkeeping Section A-VIII/2, bridge procedures, lookout duties."
        ),
        CompetenceInfo(
            competenceId = "C3",
            functionId = "F1",
            functionName = "Function 1: Navigation at Operational Level",
            track = Track.DECK_OIC_NW,
            code = "C3",
            title = "Use of Radar and ARPA to maintain safety of navigation",
            officialTOSItems = 15,
            description = "Radar plotting, CPA/TCPA calculation, true vs relative motion, ARPA trial manoeuvres, clutter suppression."
        ),
        CompetenceInfo(
            competenceId = "C4",
            functionId = "F1",
            functionName = "Function 1: Navigation at Operational Level",
            track = Track.DECK_OIC_NW,
            code = "C4",
            title = "Use of ECDIS to maintain safety of navigation",
            officialTOSItems = 10,
            description = "Electronic navigational charts (ENC), safety contours, display categories, sensors integration, over-reliance prevention."
        ),
        CompetenceInfo(
            competenceId = "C5",
            functionId = "F1",
            functionName = "Function 1: Navigation at Operational Level",
            track = Track.DECK_OIC_NW,
            code = "C5",
            title = "Respond to emergencies",
            officialTOSItems = 10,
            description = "Man overboard maneuvers (Williamson, Scharnow), search & rescue (IAMSAR), collision, grounding, damage control."
        ),
        CompetenceInfo(
            competenceId = "C7",
            functionId = "F1",
            functionName = "Function 1: Navigation at Operational Level",
            track = Track.DECK_OIC_NW,
            code = "C7",
            title = "Use the IMO SMCP and use English in written and oral form",
            officialTOSItems = 20,
            description = "IMO Standard Marine Communication Phrases, message markers, VHF communications, navigational warnings."
        ),
        CompetenceInfo(
            competenceId = "C9",
            functionId = "F1",
            functionName = "Function 1: Navigation at Operational Level",
            track = Track.DECK_OIC_NW,
            code = "C9",
            title = "Manoeuver the ship",
            officialTOSItems = 15,
            description = "Ship handling, pivot point, turning circles, squat and shallow water effects, bank suction, anchoring."
        ),
        CompetenceInfo(
            competenceId = "C10",
            functionId = "F2",
            functionName = "Function 2: Cargo Handling & Stowage",
            track = Track.DECK_OIC_NW,
            code = "C10",
            title = "Monitor loading, stowage, securing, and care during voyage and unloading",
            officialTOSItems = 15,
            description = "IMDG Code hazard classes, cargo securing manual (CSM), bulk grain regulations, dangerous goods segregation."
        ),
        CompetenceInfo(
            competenceId = "C11",
            functionId = "F2",
            functionName = "Function 2: Cargo Handling & Stowage",
            track = Track.DECK_OIC_NW,
            code = "C11",
            title = "Inspect and report defect and damage to cargo spaces, hatch covers, ballast tanks",
            officialTOSItems = 15,
            description = "Ultrasonic hatch testing, corrosion detection, Enhanced Survey Programme (ESP), structural deformation."
        ),
        CompetenceInfo(
            competenceId = "C12",
            functionId = "F3",
            functionName = "Function 3: Controlling Ship Operations & Care for Persons",
            track = Track.DECK_OIC_NW,
            code = "C12",
            title = "Ensure compliance with pollution-prevention requirements",
            officialTOSItems = 15,
            description = "MARPOL Annexes I to VI (Oily water separator 15ppm, sewage, garbage special areas, SOx/NOx ECA emissions)."
        ),
        CompetenceInfo(
            competenceId = "C13",
            functionId = "F3",
            functionName = "Function 3: Controlling Ship Operations & Care for Persons",
            track = Track.DECK_OIC_NW,
            code = "C13",
            title = "Maintain seaworthiness of the ship",
            officialTOSItems = 15,
            description = "Ship stability, transverse metacentric height (GM), free surface effect, load lines, damage stability."
        ),
        CompetenceInfo(
            competenceId = "C17",
            functionId = "F3",
            functionName = "Function 3: Controlling Ship Operations & Care for Persons",
            track = Track.DECK_OIC_NW,
            code = "C17",
            title = "Monitor compliance with legislative requirements",
            officialTOSItems = 15,
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
            officialTOSItems = 85,
            description = "Digital Selective Calling (DSC), NAVTEX (518 kHz), EPIRB (406 MHz), SART (9 GHz), Inmarsat-C, VHF/MF/HF."
        ),
        CompetenceInfo(
            competenceId = "C2",
            functionId = "GMDSS",
            functionName = "GMDSS Radio Operations",
            track = Track.GMDSS_GOC,
            code = "C2",
            title = "Provide radio services in emergencies",
            officialTOSItems = 15,
            description = "Distress alerting protocols (MAYDAY, PAN PAN, SECURITE), false distress cancellations, SAR on-scene communications."
        )
    )

    fun getCompetencesForTrack(track: Track): List<CompetenceInfo> {
        return when (track) {
            Track.DECK_OIC_NW -> DECK_COMPETENCES
            Track.GMDSS_GOC -> GMDSS_COMPETENCES
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
            "F1" -> "Function 1: Navigation at Operational Level"
            "F2" -> "Function 2: Cargo Handling & Stowage"
            "F3" -> "Function 3: Controlling Ship Operations & Care for Persons"
            "GMDSS" -> "GMDSS General Operator Certificate (GOC)"
            else -> functionId
        }
    }
}
