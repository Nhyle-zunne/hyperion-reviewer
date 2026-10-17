package com.nhyle.hyperionmaritime.data.repository

import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.Quadruple
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track

object MaritimeQuestionBankDeckF2F3 {

    private var c10Cache: List<Question>? = null
    private var c11Cache: List<Question>? = null
    private var c12Cache: List<Question>? = null
    private var c13Cache: List<Question>? = null
    private var c17Cache: List<Question>? = null

    fun getQuestions(): List<Question> {
        val list = mutableListOf<Question>()

        list.addAll(getC10Questions())
        list.addAll(getC11Questions())
        list.addAll(getC12Questions())
        list.addAll(getC13Questions())
        list.addAll(getC17Questions())

        return list
    }

    fun getC10Questions(): List<Question> {
        c10Cache?.let { return it }
        val loaded = loadF2F3Csv("questions/deck/oic_nw_c10.csv", "C10", "F2", 610, getFallbackC10Questions(), "Cargo handling, stowage and securing")
        c10Cache = loaded
        return loaded
    }

    fun getC11Questions(): List<Question> {
        c11Cache?.let { return it }
        val loaded = loadF2F3Csv("questions/deck/oic_nw_c11.csv", "C11", "F2", 421, getFallbackC11Questions(), "Inspect and report defects and damage to cargo spaces, hatch covers and ballast tanks")
        c11Cache = loaded
        return loaded
    }

    fun getC12Questions(): List<Question> {
        c12Cache?.let { return it }
        val loaded = loadF2F3Csv("questions/deck/oic_nw_c12.csv", "C12", "F3", 351, getFallbackC12Questions(), "Compliance with pollution prevention requirements")
        c12Cache = loaded
        return loaded
    }

    fun getC13Questions(): List<Question> {
        c13Cache?.let { return it }
        val loaded = loadF2F3Csv("questions/deck/oic_nw_c13.csv", "C13", "F3", 888, getFallbackC13Questions(), "Maintain seaworthiness of the ship")
        c13Cache = loaded
        return loaded
    }

    fun getC17Questions(): List<Question> {
        c17Cache?.let { return it }
        val loaded = loadF2F3Csv("questions/deck/oic_nw_c17.csv", "C17", "F3", 628, getFallbackC17Questions(), "Application of leadership and teamworking skills")
        c17Cache = loaded
        return loaded
    }

    fun openAssetStream(assetPath: String): java.io.InputStream? {
        val encrypted = openRawAssetStream("$assetPath.enc")
        if (encrypted != null) {
            return try {
                com.nhyle.hyperionmaritime.data.util.AssetCrypto.decryptStream(encrypted)
            } catch (_: Exception) {
                null
            }
        }
        return openRawAssetStream(assetPath)
    }

    private fun openRawAssetStream(assetPath: String): java.io.InputStream? {
        return try {
            val classLoader = MaritimeQuestionBankDeckF2F3::class.java.classLoader
            classLoader?.getResourceAsStream(assetPath)
                ?: classLoader?.getResourceAsStream("assets/$assetPath")
                ?: MaritimeQuestionBank.appContext?.assets?.open(assetPath)
                ?: java.io.File("app/src/main/assets/$assetPath").takeIf { it.exists() }?.inputStream()
                ?: java.io.File("src/main/assets/$assetPath").takeIf { it.exists() }?.inputStream()
                ?: java.io.File("/app/src/main/assets/$assetPath").takeIf { it.exists() }?.inputStream()
                ?: java.io.File("/app/applet/app/src/main/assets/$assetPath").takeIf { it.exists() }?.inputStream()
        } catch (_: Exception) {
            null
        }
    }

    private fun loadF2F3Csv(
        assetPath: String,
        competenceId: String,
        functionId: String,
        totalItems: Int,
        fallbackPool: List<Question>,
        compTitle: String
    ): List<Question> {
        val stream = openAssetStream(assetPath) ?: return emptyList()

        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            .replace("&#39;", "'")
            .replace("/'", "'")
            .replace("&amp;", "&")
            .replace("\uFFFD", "'")
        val rows = parseCsvText(text)
        if (rows.size <= 4) return emptyList()

        // Find the index of the header row "No."
        var headerIndex = -1
        for (idx in rows.indices) {
            val r = rows[idx]
            if (r.isNotEmpty() && r[0].trim().equals("No.", ignoreCase = true)) {
                headerIndex = idx
                break
            }
        }
        if (headerIndex == -1) return emptyList()

        val list = MutableList(totalItems) { idx ->
            val i = idx + 1
            val baseQ = fallbackPool[idx % fallbackPool.size]
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            Question(
                id = "FALLBACK-$functionId-$competenceId-${String.format("%03d", i)}",
                track = Track.DECK_OIC_NW,
                functionId = functionId,
                competenceId = competenceId,
                topic = baseQ.topic,
                questionText = baseQ.questionText,
                options = baseQ.options,
                correctAnswerIndex = baseQ.correctAnswerIndex,
                explanation = "",
                difficulty = diff
            )
        }

        val topics = when (competenceId) {
            "C10" -> listOf(
                "Cargo Stowage, IMDG & Bulk Grain",
                "Hazardous Cargoes & IMDG Code",
                "Cargo Securing & Lashing",
                "Bulk Cargo Liquefaction & IMSBC",
                "Cargo Care during Voyage",
                "Draft Survey & Displacement Calculations",
                "Ventilation & Condensation Control"
            )
            "C11" -> listOf(
                "Structural Inspection, Hatch Integrity & ESP",
                "Enhanced Survey Programme (ESP)",
                "Hatch Cover Tightness & Maintenance",
                "Enclosed Space Entry Safety",
                "Corrosion Detection & Prevention",
                "Structural Damage & Defects Reporting",
                "Ballast Tank Inspection Protocols"
            )
            "C12" -> listOf(
                "MARPOL Annexes I-VI & BWM",
                "Oily Water Separator & Bilge Discharge",
                "Garbage Disposal & Plastics Prohibition",
                "Air Pollution & ECA Compliance",
                "Ballast Water Management Standard D-2",
                "Sewage Treatment & Discharge Limits",
                "SOx & NOx Emission Control"
            )
            "C13" -> listOf(
                "Transverse Stability, GM & Free Surface",
                "Intact & Damage Stability Criteria",
                "Free Surface Effect & Slack Tanks",
                "Hogging, Sagging & Hull Stress",
                "Load Lines & Draft Marks",
                "Drydocking Stability & Calculations",
                "Angle of Loll & Corrective Actions"
            )
            "C17" -> listOf(
                "MLC 2006, ISM, ISPS & Port State Control",
                "Work and Rest Hours Compliance",
                "Safety Management System & DPA",
                "ISPS Ship Security Plans",
                "Port State Control Inspections",
                "Leadership, Teamwork & Resource Management",
                "International Maritime Conventions"
            )
            else -> listOf("General Maritime Operations")
        }

        for (i in (headerIndex + 1) until rows.size) {
            val row = rows[i]
            if (row.size >= 7) {
                val qNo = row[0].trim()
                val qText = row[1].trim()
                val optA = row[2].trim()
                val optB = row[3].trim()
                val optC = row[4].trim()
                val optD = row[5].trim()
                val ansStr = row[6].trim().uppercase()

                val options = listOf(optA, optB, optC, optD).filter { it.isNotEmpty() }
                val qNoInt = qNo.toIntOrNull()
                if (qNoInt != null && qNoInt in 1..totalItems && qText.isNotEmpty() && options.size >= 2) {
                    val ansIdx = when (ansStr) {
                        "A", "1" -> 0
                        "B", "2" -> 1
                        "C", "3" -> 2
                        "D", "4" -> 3
                        else -> 0
                    }.coerceIn(0, options.size - 1)

                    val diff = when (qNoInt % 3) {
                        1 -> Difficulty.EASY
                        2 -> Difficulty.MODERATE
                        else -> Difficulty.DIFFICULT
                    }
                    val topic = topics[(qNoInt - 1) % topics.size]

                    list[qNoInt - 1] = Question(
                        id = "O-$functionId-$competenceId-${String.format("%03d", qNoInt)}",
                        track = Track.DECK_OIC_NW,
                        functionId = functionId,
                        competenceId = competenceId,
                        topic = topic,
                        questionText = qText,
                        options = options,
                        correctAnswerIndex = ansIdx,
                        explanation = "",
                        difficulty = diff
                    )
                }
            }
        }
        return list.filterNot { it.id.startsWith("FALLBACK-") }
    }

    private fun parseCsvText(text: String): List<List<String>> {
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
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                } else if (c == '\n' || c == '\r') {
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                    if (currentRow.any { it.isNotEmpty() }) {
                        result.add(currentRow.toList())
                    }
                    currentRow.clear()
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') {
                        i++
                    }
                } else {
                    currentField.append(c)
                }
            }
            i++
        }
        if (currentRow.isNotEmpty() || currentField.isNotEmpty()) {
            currentRow.add(currentField.toString())
            if (currentRow.any { it.isNotEmpty() }) {
                result.add(currentRow.toList())
            }
        }
        return result
    }

    private fun getFallbackC10Questions(): List<Question> {
        val list = mutableListOf<Question>()
        for (i in 1..15) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "According to the IMDG Code segregation table, what does Segregation Level 4 ('Separated longitudinally by an intervening complete compartment or hold from') require between two incompatible dangerous goods packages?",
                    listOf(
                        "At least 3 meters horizontal separation on open deck.",
                        "Separation by a complete vertical fire-and-liquid-tight hold or bulkhead intervening between the compartments.",
                        "Placing them in adjacent container bays as long as both are weather-tight.",
                        "Separation by at least one pallet row of inert general cargo."
                    ),
                    1,
                    "IMDG Code Section 7.2 defines Segregation 4 as requiring an intervening complete compartment or hold separating incompatible cargo packages."
                )
                2 -> Quadruple(
                    "Under the International Grain Code (SOLAS Ch. VI Part C), what is the maximum allowable angle of heel due to the assumed shift of grain for a cargo vessel?",
                    listOf(
                        "Not greater than 12 degrees (or angle of deck edge immersion if less).",
                        "Not greater than 18 degrees.",
                        "Not greater than 25 degrees.",
                        "Not greater than 5 degrees."
                    ),
                    0,
                    "The International Grain Code mandates that the calculated angle of heel due to assumed shift of grain shall not exceed 12° or the deck edge immersion angle."
                )
                3 -> Quadruple(
                    "In a draft survey calculation, what is the 'Mean of Means' (Quarter Mean) formula used to calculate the true displacement draft corrected for hull hog or sag?",
                    listOf(
                        "(Draft Fwd + Draft Aft) / 2",
                        "(Draft Fwd + 6*Draft Mid + Draft Aft) / 8",
                        "(Draft Fwd + 4*Draft Mid + Draft Aft) / 6",
                        "(Draft Fwd + 2*Draft Mid + Draft Aft) / 4"
                    ),
                    1,
                    "Quarter Mean (Mean of Means) gives appropriate weight to midship deflection: Mean Draft = (Draft Fwd + 6*Draft Mid + Draft Aft) / 8."
                )
                4 -> Quadruple(
                    "What is the function of the Cargo Securing Manual (CSM) required on board under SOLAS Chapter VI and VII?",
                    listOf(
                        "To document port crane maintenance certificates.",
                        "To provide precise instructions on the arrangement, number, and strength of securing devices for all cargo units carried on board.",
                        "To list customs tariff classifications.",
                        "To record hourly reefer container temperatures."
                    ),
                    1,
                    "SOLAS requires an approved Cargo Securing Manual (CSM) detailing securing devices and calculations for safe stowage during transit."
                )
                else -> Quadruple(
                    "In Cargo Handling & Stowage (Item $i): What hazard is associated with loading solid bulk cargoes with a high moisture content exceeding their Transportable Moisture Limit (TML)?",
                    listOf(
                        "Cargo liquefaction causing sudden catastrophic loss of transverse stability and rapid ship capsize.",
                        "Spontaneous combustion inside the hold.",
                        "Over-pressurization of the hatch cover hydraulic seals.",
                        "Chemical degradation of ship bilge strainers."
                    ),
                    0,
                    "Per the IMSBC Code, bulk cargoes containing moisture above TML can liquefy due to ship motion and vibrations, resulting in cargo shift and capsizing."
                )
            }
            list.add(
                Question(
                    id = "D-F2-C10-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F2",
                    competenceId = "C10",
                    topic = "Cargo Stowage, IMDG & Bulk Grain",
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

    private fun getFallbackC11Questions(): List<Question> {
        val list = mutableListOf<Question>()
        for (i in 1..15) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "Which non-destructive testing method is recognized as the most accurate and modern technique for testing the weathertightness of cargo hatch covers and coamings without risking water damage?",
                    listOf(
                        "Chalk testing using industrial chalk stones.",
                        "Ultrasonic tight-testing with a calibrated sound transmitter inside the closed hold.",
                        "High-pressure fire hose testing at 2 bar from 1 meter distance.",
                        "Kerosene penetration dye testing along compression bars."
                    ),
                    1,
                    "Ultrasonic testing is the internationally approved standard for hatch cover integrity, transmitting high-frequency sound waves inside the hold and detecting seal leakage."
                )
                2 -> Quadruple(
                    "The Enhanced Survey Programme (ESP) under SOLAS Chapter XI-1 applies mandatory rigorous close-up structural hull inspections primarily to which types of ships?",
                    listOf(
                        "Container ships and Ro-Ro passenger ferries.",
                        "Bulk carriers and Oil tankers.",
                        "Refrigerated cargo carriers and offshore supply vessels.",
                        "Chemical parcel tankers under 5,000 DWT."
                    ),
                    1,
                    "IMO Resolution A.1049(27) (ESP Code) mandates the Enhanced Survey Programme specifically for bulk carriers and oil tankers to detect corrosion and fatigue in ballast tanks and holds."
                )
                3 -> Quadruple(
                    "What safety procedure must strictly precede entering an enclosed cargo hold or ballast tank for inspection?",
                    listOf(
                        "Spray the space with fresh water.",
                        "Ventilate thoroughly, verify atmospheric oxygen (minimum 20.9%), flammable gases (<1% LFL), toxic gases, and obtain an Enclosed Space Entry Permit.",
                        "Enter in pairs with flashlight only.",
                        "Keep hatch covers partially closed to avoid rain ingress."
                    ),
                    1,
                    "IMO Resolution A.1050(27) strictly enforces testing atmosphere (O2 >= 20.9%, flammable gas <1% LFL, toxic levels within threshold) and securing an entry permit."
                )
                else -> Quadruple(
                    "In Hull & Tank Inspection (Item $i): What type of corrosion typically affects double bottom water ballast tanks where protective coatings have broken down?",
                    listOf(
                        "Pitting corrosion and accelerated galvanic wastage of structural frames and longitudinals.",
                        "Stress corrosion cracking from fresh water wash.",
                        "Bacterial oxidation of hydraulic fluids.",
                        "Thermal expansion fatigue along hatch coamings."
                    ),
                    0,
                    "Ballast tanks suffer severe electrolytic pitting corrosion when protective sacrificial zinc anodes or epoxy coatings deteriorate."
                )
            }
            list.add(
                Question(
                    id = "D-F2-C11-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F2",
                    competenceId = "C11",
                    topic = "Structural Inspection, Hatch Integrity & ESP",
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

    private fun getFallbackC12Questions(): List<Question> {
        val list = mutableListOf<Question>()
        for (i in 1..15) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "Under MARPOL Annex I, what is the maximum oil content allowable in effluent discharged overboard from machinery space bilges through an Oily Water Separator (OWS) while en route?",
                    listOf("50 parts per million (ppm)", "15 parts per million (ppm)", "5 parts per million (ppm)", "0 parts per million (zero discharge always)"),
                    1,
                    "MARPOL Annex I Regulation 14/15 allows discharge of bilge effluent only when oil content does not exceed 15 ppm and 15 ppm bilge alarm/auto-stop is operational."
                )
                2 -> Quadruple(
                    "Under MARPOL Annex V, which category of garbage is strictly PROHIBITED from being discharged into the sea ANYWHERE in the world, including outside special areas?",
                    listOf(
                        "Comminuted food waste passing through a 25 mm screen beyond 12 NM.",
                        "All plastics, including synthetic ropes, plastic garbage bags, and incinerator ashes from plastic products.",
                        "Cargo residues non-harmful to the marine environment beyond 12 NM.",
                        "Clean deck wash water."
                    ),
                    1,
                    "MARPOL Annex V completely prohibits the disposal into the sea of all plastics, including fishing gear, plastic packaging, and incinerator ashes."
                )
                3 -> Quadruple(
                    "Under MARPOL Annex VI, what is the maximum allowable sulfur content of marine fuel oil used on board ships operating inside designated Emission Control Areas (ECAs)?",
                    listOf("0.50% m/m", "0.10% m/m", "1.00% m/m", "0.05% m/m"),
                    1,
                    "MARPOL Annex VI Regulation 14 mandates 0.10% m/m sulfur limit in ECAs, compared to 0.50% m/m outside ECAs."
                )
                4 -> Quadruple(
                    "Under the International Ballast Water Management Convention (BWM Convention), what is the standard D-2 biological performance standard?",
                    listOf(
                        "Conducting open ocean ballast exchange at least 200 NM from nearest land in 200m depth.",
                        "Treating ballast water using an approved Ballast Water Management System (BWMS) to discharge fewer than 10 viable organisms per cubic meter >= 50 micrometers in size.",
                        "Flushing all ballast tanks with fresh drinking water prior to departure.",
                        "Retention of all ballast water on board indefinitely."
                    ),
                    1,
                    "BWM Convention Regulation D-2 defines discharge standards based on organism concentration achieved via an approved Ballast Water Management System."
                )
                else -> Quadruple(
                    "In Environmental Compliance & MARPOL (Item $i): What is the required entry procedure when discharging treated sewage under MARPOL Annex IV?",
                    listOf(
                        "Discharge using an approved sewage treatment plant certified by the Administration, or comminuted/disinfected sewage at a distance of >3 NM, or untreated sewage at >12 NM en route at >= 4 knots.",
                        "Sewage may be discharged at any distance as long as it is during night time.",
                        "Discharge is strictly prohibited within 200 NM of all coastlines.",
                        "Sewage must always be retained until reaching drydock."
                    ),
                    0,
                    "MARPOL Annex IV regulates sewage discharge rates based on distance (>3 NM comminuted, >12 NM untreated, or anytime through certified Sewage Treatment Plant)."
                )
            }
            list.add(
                Question(
                    id = "D-F3-C12-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F3",
                    competenceId = "C12",
                    topic = "MARPOL Annexes I-VI & BWM",
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

    private fun getFallbackC13Questions(): List<Question> {
        val list = mutableListOf<Question>()
        for (i in 1..15) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "What does a vessel possess when its Center of Gravity (G) is located directly above its Metacenter (M) (i.e., GM is negative)?",
                    listOf(
                        "Positive initial stability with a stiff rolling period.",
                        "Unstable equilibrium, causing the ship to take an angle of loll to port or starboard.",
                        "Neutral equilibrium where the ship remains at any angle to which it is inclined.",
                        "Maximum righting lever GZ."
                    ),
                    1,
                    "Negative GM causes unstable equilibrium where any small perturbing force creates an upsetting moment until the ship comes to rest at an Angle of Loll."
                )
                2 -> Quadruple(
                    "How does the Free Surface Effect (FSE) caused by partially filled slack ballast or fuel tanks impact a ship's stability?",
                    listOf(
                        "It lowers the transverse metacenter (KM), increasing GM.",
                        "It causes a virtual rise in the ship's center of gravity (G), thereby reducing the effective metacentric height (GM).",
                        "It shifts the center of buoyancy downward, decreasing the draft.",
                        "It only affects longitudinal trim, having no impact on transverse stability."
                    ),
                    1,
                    "Surging liquid in slack tanks creates an upsetting moment mathematically equivalent to a virtual elevation of the center of gravity (GG1 = i/V), reducing GM."
                )
                3 -> Quadruple(
                    "What is the definition of a 'stiff' ship versus a 'tender' ship?",
                    listOf(
                        "A stiff ship has a large GM and violent, rapid rolling period, whereas a tender ship has a small GM and sluggish, gentle rolling period.",
                        "A stiff ship has negative GM, while a tender ship has positive GM.",
                        "A stiff ship is hogged, while a tender ship is sagged.",
                        "A stiff ship has excess ballast, while a tender ship is in light condition."
                    ),
                    0,
                    "A large GM creates excessive righting moments and rapid, violent rolling (stiff), while small GM produces sluggish, slow roll periods (tender)."
                )
                else -> Quadruple(
                    "In Ship Stability & Seaworthiness (Item $i): According to the IMO Intact Stability Code 2008 (IS Code), what is the minimum required initial metacentric height (GM0) for cargo ships?",
                    listOf(
                        "Not less than 0.15 meters in all loading conditions.",
                        "Not less than 1.00 meters.",
                        "Not less than 0.50 meters.",
                        "Not less than 0.05 meters."
                    ),
                    0,
                    "The IMO Intact Stability Code 2008 requires an initial transverse metacentric height GM0 of not less than 0.15 m for cargo ships."
                )
            }
            list.add(
                Question(
                    id = "D-F3-C13-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F3",
                    competenceId = "C13",
                    topic = "Transverse Stability, GM & Free Surface",
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

    private fun getFallbackC17Questions(): List<Question> {
        val list = mutableListOf<Question>()
        for (i in 1..15) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "Under the Maritime Labour Convention (MLC 2006) and STCW 2010 Manila Amendments, what is the mandatory minimum number of hours of rest required for any seafarer in any 24-hour period?",
                    listOf("At least 8 hours", "At least 10 hours", "At least 12 hours", "At least 6 hours"),
                    1,
                    "MLC 2006 Standard A2.3 and STCW A-VIII/1 mandate at least 10 hours of rest in any 24-hour period, and 77 hours in any 7-day period."
                )
                2 -> Quadruple(
                    "When a Port State Control (PSC) officer identifies an objective failure to properly implement the ship's Safety Management System (SMS) that poses serious danger to ship safety, which action is standard?",
                    listOf(
                        "Issue a verbal warning with a 30-day grace period.",
                        "Code 30 Detention of the vessel until the serious deficiency is rectified and verified by Class/Flag.",
                        "Impose an immediate administrative fine on the Chief Officer only.",
                        "Downgrade the vessel's Gross Tonnage rating."
                    ),
                    1,
                    "Under Port State Control MOUs, serious safety non-conformities warrant a Code 30 detention until resolved."
                )
                3 -> Quadruple(
                    "What is the primary role of the Designated Person Ashore (DPA) under the International Safety Management (ISM) Code?",
                    listOf(
                        "To negotiate cargo freight rates with charterers.",
                        "To provide a direct link between the ship and highest level of management, monitoring safety and pollution prevention aspects of ship operations.",
                        "To conduct customs clearance in foreign ports.",
                        "To oversee crew payroll and tax deductions."
                    ),
                    1,
                    "ISM Code Section 4 requires a Designated Person Ashore (DPA) with direct access to top management to monitor shipboard safety and pollution prevention."
                )
                else -> Quadruple(
                    "In Maritime Conventions & Legislation (Item $i): What does the International Ship and Port Facility Security (ISPS) Code Security Level 2 signify?",
                    listOf(
                        "Normal operational security measures.",
                        "The level applying for a period of time when there is a heightened risk of a security incident, requiring additional protective security measures.",
                        "An exceptional level when a security incident is probable or imminent.",
                        "Lockdown of all communications equipment."
                    ),
                    1,
                    "ISPS Code defines Level 2 as heightened risk requiring additional protective measures beyond normal baseline Level 1."
                )
            }
            list.add(
                Question(
                    id = "D-F3-C17-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F3",
                    competenceId = "C17",
                    topic = "MLC 2006, ISM, ISPS & Port State Control",
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
