package com.example.data.repository

import com.example.data.model.Difficulty
import com.example.data.model.Quadruple
import com.example.data.model.Question
import com.example.data.model.Track

object MaritimeQuestionBankGmdss {

    fun getQuestions(): List<Question> {
        val list = mutableListOf<Question>()

        // ==========================================
        // GMDSS C1: Transmit and receive information using GMDSS subsystems and equipment (85 items)
        // Sub-topics: DSC, NAVTEX, EPIRB & SART, Inmarsat-C, VHF/MF/HF Radiotelephony, Distress Frequencies, General Regulations
        // ==========================================
        val c1Topics = listOf(
            "VHF & MF/HF Digital Selective Calling (DSC)",
            "NAVTEX & Maritime Safety Information (MSI)",
            "COSPAS-SARSAT Satellite EPIRBs (406 MHz)",
            "Search and Rescue Radar Transponder (SART 9 GHz)",
            "Inmarsat-C & Enhanced Group Calling (EGC)",
            "GMDSS Sea Areas A1 to A4 & Equipment Carriage",
            "MMSI Numbering & ITU Radio Regulations",
            "Power Supplies & Reserve Source of Energy"
        )

        for (i in 1..85) {
            val topic = c1Topics[(i - 1) % c1Topics.size]
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "What is the dedicated, internationally designated VHF frequency and channel exclusively used for GMDSS Digital Selective Calling (DSC) distress and safety alerting?",
                    listOf(
                        "VHF Channel 16 (156.800 MHz)",
                        "VHF Channel 70 (156.525 MHz)",
                        "VHF Channel 06 (156.300 MHz)",
                        "VHF Channel 13 (156.650 MHz)"
                    ),
                    1,
                    "VHF Channel 70 (156.525 MHz) is strictly dedicated to Digital Selective Calling (DSC). Voice communication is strictly prohibited on Ch 70. (ITU Radio Regs / SOLAS IV)."
                )
                2 -> Quadruple(
                    "Which frequency is the internationally designated primary frequency for standard English-language International NAVTEX transmissions worldwide?",
                    listOf("490 kHz", "518 kHz", "4209.5 kHz", "2182 kHz"),
                    1,
                    "518 kHz is the international NAVTEX frequency broadcasting navigational warnings, meteorological forecasts, and search and rescue alerts exclusively in English. 490 kHz is for national languages."
                )
                3 -> Quadruple(
                    "What dual transmission frequencies are utilized by standard satellite EPIRBs complying with GMDSS carriage requirements?",
                    listOf(
                        "406.0 - 406.1 MHz for satellite alert & positioning, plus 121.5 MHz for on-scene homing.",
                        "1.6 GHz for Inmarsat geostationary link and 9 GHz for radar detection.",
                        "2182 kHz for voice alert and 156.8 MHz for VHF alert.",
                        "1575.42 MHz for GPS reception only."
                    ),
                    0,
                    "Standard GMDSS satellite EPIRBs transmit a 5-watt digital burst on 406.0–406.1 MHz to COSPAS-SARSAT satellites and 121.5 MHz for on-scene aircraft homing."
                )
                4 -> Quadruple(
                    "How does an activated 9 GHz Search and Rescue Transponder (SART) appear on the display of an interrogating X-band marine radar?",
                    listOf(
                        "As an illuminated red flashing triangle at the center of the screen.",
                        "As a series of 12 equally spaced dots or blips extending radially outward from the SART position.",
                        "As an audible morse code 'SOS' tone on the radar audio channel.",
                        "As a continuous sweeping arc of 360 degrees."
                    ),
                    1,
                    "When triggered by 9 GHz X-band radar, a SART displays a series of 12 equidistant dots radiating outward along its line of bearing. In close range, dots broaden into arcs."
                )
                5 -> Quadruple(
                    "Which Medium Frequency (MF) is assigned exclusively for DSC distress and safety calls?",
                    listOf("2182.0 kHz", "2187.5 kHz", "2174.5 kHz", "4125.0 kHz"),
                    1,
                    "2187.5 kHz is the dedicated MF DSC distress and safety frequency. 2182.0 kHz is the radiotelephone voice frequency."
                )
                6 -> Quadruple(
                    "How is GMDSS Sea Area A2 defined under SOLAS Chapter IV Regulation 2?",
                    listOf(
                        "An area within the radiotelephone coverage of at least one VHF coast station in which continuous DSC alerting is available.",
                        "An area, excluding sea area A1, within the radiotelephone coverage of at least one MF coast station in which continuous DSC alerting is available.",
                        "An area within the coverage of an Inmarsat geostationary satellite (roughly between 70°N and 70°S).",
                        "The polar regions beyond 76° North or South latitude."
                    ),
                    1,
                    "SOLAS IV/2 defines Sea Area A2 as an area outside Sea Area A1 within MF coast station radiotelephone/DSC coverage (roughly 100-150 NM)."
                )
                7 -> Quadruple(
                    "A ship's Maritime Mobile Service Identity (MMSI) is a unique 9-digit number. What do the first three digits (MID) signify?",
                    listOf(
                        "The ship's gross tonnage classification category.",
                        "The Maritime Identification Digit (MID) indicating the nationality / flag state of the vessel.",
                        "The manufacturer code of the DSC terminal.",
                        "The satellite constellation assigned to the ship."
                    ),
                    1,
                    "The first 3 digits of a ship station MMSI represent the Maritime Identification Digit (MID) designating country of registry (e.g., Philippines = 548)."
                )
                8 -> Quadruple(
                    "Under SOLAS Chapter IV Regulation 13, what is the minimum duration of emergency operation required for the radio installations from the reserve source of electrical energy?",
                    listOf(
                        "At least 1 hour if the emergency generator complies with SOLAS II-1/42 or 43, otherwise at least 6 hours.",
                        "At least 12 hours under all conditions.",
                        "At least 24 hours.",
                        "At least 30 minutes."
                    ),
                    0,
                    "SOLAS IV/13 mandates at least 1 hour if the emergency generator powers the radio load, or at least 6 hours if the reserve battery is the sole backup."
                )
                9 -> Quadruple(
                    "Which HF frequencies are designated for GMDSS DSC Distress, Urgency, and Safety alerting across the international maritime bands?",
                    listOf(
                        "4207.5 kHz, 6312.0 kHz, 8414.5 kHz, 12577.0 kHz, and 16804.5 kHz.",
                        "4125.0 kHz, 6215.0 kHz, 8291.0 kHz, 12290.0 kHz, and 16420.0 kHz.",
                        "490 kHz, 518 kHz, 4209.5 kHz, 500 kHz.",
                        "156.8 MHz, 156.525 MHz, 156.3 MHz."
                    ),
                    0,
                    "The 5 HF DSC distress frequencies are 4207.5, 6312.0, 8414.5, 12577.0, and 16804.5 kHz. (8414.5 kHz must be continuously guarded by coast stations)."
                )
                10 -> Quadruple(
                    "In Inmarsat-C satellite communications, what system is utilized for broadcasting Maritime Safety Information (MSI) such as NAVAREA warnings and weather bulletins to vessels outside NAVTEX range?",
                    listOf("SafetyNET (Enhanced Group Call - EGC)", "FleetBroadband voice loop", "COSPAS return link", "DSC HF Telex"),
                    0,
                    "SafetyNET is the international service for broadcasting MSI via the Inmarsat Enhanced Group Call (EGC) system."
                )
                else -> Quadruple(
                    "In GMDSS Subsystems & Equipment (Item $i): What emission designation is assigned to single-sideband (SSB) radiotelephony on maritime HF distress frequencies such as 8291 kHz?",
                    listOf("J3E (suppressed carrier single sideband voice)", "F1B (frequency shift digital telex)", "G3E (frequency modulated voice)", "A1A (unmodulated continuous wave)"),
                    0,
                    "J3E denotes single sideband with suppressed carrier for maritime radiotelephone voice communications."
                )
            }
            list.add(
                Question(
                    id = "G-C1-${String.format("%02d", i)}",
                    track = Track.GMDSS_GOC,
                    functionId = "GMDSS",
                    competenceId = "C1",
                    topic = topic,
                    questionText = qText,
                    options = options,
                    correctAnswerIndex = correctIdx,
                    explanation = explanation,
                    difficulty = diff
                )
            )
        }

        // ==========================================
        // GMDSS C2: Provide radio services in emergencies (15 items)
        // ==========================================
        for (i in 1..15) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "According to ITU Radio Regulations, what is the strict, legally established order of priority for all radiocommunications in the maritime mobile service?",
                    listOf(
                        "1. Urgency, 2. Distress, 3. Safety, 4. Routine",
                        "1. Distress (MAYDAY), 2. Urgency (PAN PAN), 3. Safety (SECURITE), 4. Other communications",
                        "1. Port Operations, 2. Distress, 3. Meteorological, 4. Commercial",
                        "1. Safety, 2. Urgency, 3. Distress, 4. Routine"
                    ),
                    1,
                    "ITU Radio Regs Article 53 mandates the hierarchy: 1. Distress (MAYDAY), 2. Urgency (PAN PAN), 3. Safety (SECURITE), 4. Other communications."
                )
                2 -> Quadruple(
                    "If a ship accidentally transmits a false VHF DSC Distress Alert on Channel 70, what is the mandatory immediate procedure under IMO Resolution A.814(19)?",
                    listOf(
                        "Power off the transmitter immediately and say nothing to avoid penalties.",
                        "Switch immediately to VHF Channel 16 and broadcast an 'All Stations' voice cancellation message providing ship name, call sign, MMSI, position, and stating that the distress alert was transmitted in error.",
                        "Transmit a DSC Urgency alert on Channel 70 to override the distress alert.",
                        "Wait 15 minutes to see if an RCC contacts you before responding."
                    ),
                    1,
                    "Per IMO Res. A.814(19), to cancel a false VHF DSC distress alert: immediately switch to Ch 16 and broadcast an 'All Stations' voice cancellation giving ship details and position."
                )
                3 -> Quadruple(
                    "When a shipboard GMDSS operator receives a DSC Distress Alert from another vessel on VHF Ch 70 or MF 2187.5 kHz, why must the ship NOT immediately acknowledge the call by DSC?",
                    listOf(
                        "Ship stations are not technically capable of sending DSC acknowledgments.",
                        "To allow a Coast Radio Station (CRS) or RCC the opportunity to acknowledge the alert first; a ship DSC acknowledgment would silence other repeats before CRS hears it.",
                        "Because DSC acknowledgments require prior authorization from the ship's Master.",
                        "Because acknowledging on DSC will cause all surrounding GPS units to reset."
                    ),
                    1,
                    "ITU-R M.541 establishes that only Coast Stations should acknowledge DSC distress calls by DSC. Ships should listen on associated voice channels (Ch 16 / 2182 kHz) and inform CRS."
                )
                4 -> Quadruple(
                    "Under ITU Radio Regulations, what is the spoken distress signal indicating a ship is threatened by grave and imminent danger and requests immediate assistance?",
                    listOf("MAYDAY (spoken three times)", "PAN PAN (spoken three times)", "SECURITE (spoken three times)", "SOS (spoken three times)"),
                    0,
                    "The radiotelephone distress signal is MAYDAY (from French 'm'aider'), spoken three times, signaling grave and imminent danger."
                )
                5 -> Quadruple(
                    "What procedure should be taken by a ship station that has intercepted an HF DSC Distress Alert addressed to all stations?",
                    listOf(
                        "Acknowledge immediately on all HF frequencies simultaneously.",
                        "Monitor the associated radiotelephone distress frequency for 5 minutes. If no Coast Station acknowledgment is heard, relay the distress alert to a Coast Station by radiotelephone.",
                        "Transmit an 'All Ships' DSC acknowledgment on 8414.5 kHz.",
                        "Silence radio receivers to prevent interference."
                    ),
                    1,
                    "On HF, ships must never acknowledge by DSC unless directed by an RCC; ships monitor for 5 minutes and relay to shore if no coast station acknowledges."
                )
                else -> Quadruple(
                    "In GMDSS Emergency Operations (Item $i): What phrase is used by the station controlling distress traffic to impose radio silence on all stations causing interference?",
                    listOf("SEELONCE MAYDAY", "SILENCE PLEASE", "STOP TRANSMITTING", "HOLD ALL SIGNALS"),
                    0,
                    "The station controlling distress communications or on-scene operations uses the standard phrase 'SEELONCE MAYDAY' (or 'SEELONCE DISTRESS') to impose radio silence on interfering stations."
                )
            }
            list.add(
                Question(
                    id = "G-C2-${String.format("%02d", i)}",
                    track = Track.GMDSS_GOC,
                    functionId = "GMDSS",
                    competenceId = "C2",
                    topic = "Emergency Radio Communications & SAR",
                    questionText = qText,
                    options = options,
                    correctAnswerIndex = correctIdx,
                    explanation = explanation,
                    difficulty = diff
                )
            )
        }

        return list
    }
}
