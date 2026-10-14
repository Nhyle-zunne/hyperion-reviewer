package com.nhyle.hyperionmaritime.data.repository

import com.nhyle.hyperionmaritime.data.model.Difficulty
import com.nhyle.hyperionmaritime.data.model.Quadruple
import com.nhyle.hyperionmaritime.data.model.Question
import com.nhyle.hyperionmaritime.data.model.Track

object MaritimeQuestionBankDeckF1 {

    private var c1Cache: List<Question>? = null
    private var c2Cache: List<Question>? = null
    private var c3Cache: List<Question>? = null
    private var c4Cache: List<Question>? = null
    private var c5Cache: List<Question>? = null
    private var c7Cache: List<Question>? = null
    private var c9Cache: List<Question>? = null

    fun getQuestions(): List<Question> {
        val list = mutableListOf<Question>()

        // ==========================================
        // C1: Plan and conduct a passage and determine position (Ingested CSV)
        // ==========================================
        list.addAll(getC1Questions())

        // ==========================================
        // C2: Maintain a safe navigational watch (Ingested CSV)
        // ==========================================
        list.addAll(getC2Questions())

        // ==========================================
        // C3: Use of Radar and ARPA to maintain safety of navigation (Ingested CSV)
        // ==========================================
        list.addAll(getC3Questions())

        // ==========================================
        // C4: Use of ECDIS to maintain safety of navigation (Ingested CSV)
        // ==========================================
        list.addAll(getC4Questions())

        // ==========================================
        // C5: Respond to emergencies (Ingested CSV)
        // ==========================================
        list.addAll(getC5Questions())

        // ==========================================
        // C7: Visual signalling (Ingested CSV)
        // ==========================================
        list.addAll(getC7Questions())

        // ==========================================
        // C9: Manoeuver the ship (Ingested CSV)
        // ==========================================
        list.addAll(getC9Questions())

        return list
    }

    fun getC1Questions(): List<Question> {
        c1Cache?.let { return it }
        val loaded = loadC1Csv()
        val result = if (loaded.isNotEmpty()) loaded else getFallbackC1Questions()
        c1Cache = result
        return result
    }

    fun openAssetStream(assetPath: String): java.io.InputStream? {
        return try {
            val classLoader = MaritimeQuestionBankDeckF1::class.java.classLoader
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

    private fun loadC1Csv(): List<Question> {
        val assetPath = "questions/deck/oic_nw_c1.csv"
        val stream = openAssetStream(assetPath) ?: return emptyList()

        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val rows = parseCsvText(text)
        if (rows.size <= 1) return emptyList()

        val list = mutableListOf<Question>()
        val topics = listOf(
            "Terrestrial Navigation & Plotting",
            "Compass Errors & Corrections",
            "Passage Planning & Appraisal",
            "Tides & Tidal Stream Calculations",
            "Celestial Navigation & Sight Reduction",
            "Chart Projections & Mercator Scale",
            "Electronic Position Fixing & GNSS Errors",
            "Great Circle Sailing & Vertex Calculations",
            "Meteorology & Weather Forecasting"
        )

        var validIndex = 1
        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.size >= 7) {
                val qNo = row[0].trim()
                val qText = row[1].trim()
                val optA = row[2].trim()
                val optB = row[3].trim()
                val optC = row[4].trim()
                val optD = row[5].trim()
                val ansStr = row[6].trim().uppercase()

                val qNoInt = qNo.toIntOrNull()
                val isHeader = qText.equals("Question", ignoreCase = true) || qText.isBlank()
                val isDummyOptions = optA == "A" && optB == "B" && optC == "C" && optD == "D"

                if (qNoInt != null && qNoInt > 0 && !isHeader && !isDummyOptions) {
                    val options = listOf(optA, optB, optC, optD).filter { it.isNotEmpty() }
                    if (qText.isNotEmpty() && options.size >= 2) {
                        val ansIdx = when (ansStr) {
                            "A", "1" -> 0
                            "B", "2" -> 1
                            "C", "3" -> 2
                            "D", "4" -> 3
                            else -> 0
                        }.coerceIn(0, options.size - 1)

                        val diff = when (validIndex % 3) {
                            1 -> Difficulty.EASY
                            2 -> Difficulty.MODERATE
                            else -> Difficulty.DIFFICULT
                        }
                        val topic = topics[(validIndex - 1) % topics.size]

                        list.add(
                            Question(
                                id = "D-F1-C1-${String.format("%03d", validIndex)}",
                                track = Track.DECK_OIC_NW,
                                functionId = "F1",
                                competenceId = "C1",
                                topic = topic,
                                questionText = qText,
                                options = options,
                                correctAnswerIndex = ansIdx,
                                explanation = "",
                                difficulty = diff
                            )
                        )
                        validIndex++
                    }
                }
            }
        }
        return list
    }

    private fun getFallbackC1Questions(): List<Question> {
        val list = mutableListOf<Question>()
        val c1Topics = listOf(
            "Terrestrial Navigation & Plotting",
            "Compass Errors & Corrections",
            "Passage Planning & Appraisal",
            "Tides & Tidal Stream Calculations"
        )
        for (i in 1..35) {
            val topic = c1Topics[(i - 1) % c1Topics.size]
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            list.add(
                Question(
                    id = "D-F1-C1-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C1",
                    topic = topic,
                    questionText = "When planning a passage across oceanic routes, what is the primary geometric difference between a Great Circle track and a Rhumb Line track on a standard Mercator projection chart?",
                    options = listOf(
                        "A Great Circle track appears as a straight line, while a Rhumb Line appears as a curve towards the equator.",
                        "A Great Circle track appears as a curved line convex to the nearer pole, while a Rhumb Line appears as a straight line intersecting meridians at a constant angle.",
                        "Both appear as identical straight lines at latitudes below 45 degrees.",
                        "A Great Circle track maintains a constant compass heading, whereas a Rhumb Line changes course continuously."
                    ),
                    correctAnswerIndex = 1,
                    explanation = "On a Mercator projection chart, a Rhumb line is a straight line intersecting all meridians at a constant angle.",
                    difficulty = diff
                )
            )
        }
        return list
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
                } else if (c == '\r') {
                    if (i + 1 < text.length && text[i + 1] == '\n') {
                        i++
                    }
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                    result.add(currentRow.toList())
                    currentRow.clear()
                } else if (c == '\n') {
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                    result.add(currentRow.toList())
                    currentRow.clear()
                } else {
                    currentField.append(c)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            result.add(currentRow.toList())
        }
        return result
    }

    fun getC2Questions(): List<Question> {
        c2Cache?.let { return it }
        val loaded = loadC2Csv()
        val result = if (loaded.isNotEmpty()) loaded else getFallbackC2Questions()
        c2Cache = result
        return result
    }

    private fun loadC2Csv(): List<Question> {
        val assetPath = "questions/deck/oic_nw_c2.csv"
        val stream = openAssetStream(assetPath) ?: return emptyList()

        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val rows = parseCsvText(text)
        if (rows.size <= 1) return emptyList()

        val list = mutableListOf<Question>()
        val topics = listOf(
            "COLREGs Rule 1-19 (Steering & Sailing)",
            "COLREGs Lights & Shapes",
            "Sound Signals & Restricted Visibility",
            "STCW Watchkeeping Procedures & Bridge Management"
        )

        var validIndex = 1
        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.size >= 7) {
                val qNo = row[0].trim()
                val qText = row[1].trim()
                val optA = row[2].trim()
                val optB = row[3].trim()
                val optC = row[4].trim()
                val optD = row[5].trim()
                val ansStr = row[6].trim().uppercase()

                val qNoInt = qNo.toIntOrNull()
                val isHeader = qText.equals("Question", ignoreCase = true) || qText.isBlank()
                val isDummyOptions = optA == "A" && optB == "B" && optC == "C" && optD == "D"

                if (qNoInt != null && qNoInt > 0 && !isHeader && !isDummyOptions) {
                    val options = listOf(optA, optB, optC, optD).filter { it.isNotEmpty() }
                    if (qText.isNotEmpty() && options.size >= 2) {
                        val ansIdx = when (ansStr) {
                            "A", "1" -> 0
                            "B", "2" -> 1
                            "C", "3" -> 2
                            "D", "4" -> 3
                            else -> 0
                        }.coerceIn(0, options.size - 1)

                        val diff = when (validIndex % 3) {
                            1 -> Difficulty.EASY
                            2 -> Difficulty.MODERATE
                            else -> Difficulty.DIFFICULT
                        }
                        val topic = topics[(validIndex - 1) % topics.size]

                        list.add(
                            Question(
                                id = "D-F1-C2-${String.format("%03d", validIndex)}",
                                track = Track.DECK_OIC_NW,
                                functionId = "F1",
                                competenceId = "C2",
                                topic = topic,
                                questionText = qText,
                                options = options,
                                correctAnswerIndex = ansIdx,
                                explanation = "",
                                difficulty = diff
                            )
                        )
                        validIndex++
                    }
                }
            }
        }
        return list
    }

    private fun getFallbackC2Questions(): List<Question> {
        val list = mutableListOf<Question>()
        val c2Topics = listOf("COLREGs Rule 1-19", "COLREGs Lights & Shapes", "Sound Signals & Restricted Visibility", "STCW Watchkeeping Procedures")
        for (i in 1..20) {
            val topic = c2Topics[(i - 1) % c2Topics.size]
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            list.add(
                Question(
                    id = "D-F1-C2-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C2",
                    topic = topic,
                    questionText = "Two power-driven vessels are meeting on reciprocal courses so as to involve risk of collision. According to COLREGs Rule 14, what action must each vessel take?",
                    options = listOf(
                        "The vessel with the other on her starboard side shall keep out of the way.",
                        "Each shall alter her course to port so that each shall pass on the starboard side of the other.",
                        "Each shall alter her course to starboard so that each shall pass on the port side of the other.",
                        "The larger vessel maintains course and speed."
                    ),
                    correctAnswerIndex = 2,
                    explanation = "COLREGs Rule 14(a) mandates that when two power-driven vessels meet on reciprocal courses, each vessel shall alter course to starboard.",
                    difficulty = diff
                )
            )
        }
        return list
    }

    fun getC3Questions(): List<Question> {
        c3Cache?.let { return it }
        val loaded = loadC3Csv()
        val result = if (loaded.isNotEmpty()) loaded else getFallbackC3Questions()
        c3Cache = result
        return result
    }

    private fun loadC3Csv(): List<Question> {
        val assetPath = "questions/deck/oic_nw_c3.csv"
        val stream = openAssetStream(assetPath) ?: return emptyList()

        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            .replace("&#39;", "'")
            .replace("/'", "'")
            .replace("&amp;", "&")
            .replace("°", "°")
        val rows = parseCsvText(text)
        if (rows.size <= 1) return emptyList()

        val list = mutableListOf<Question>()
        val topics = listOf(
            "ARPA Tracking & Vectors",
            "Radar Wave Propagation & Clutter",
            "CPA/TCPA Thresholds & Alarms",
            "Trial Manoeuvre Simulation",
            "Radar Performance & Operational Standards",
            "Navigation Aids (RACON, SART, AIS)",
            "COLREGs & Radar Plotting in Restricted Visibility"
        )

        var validIndex = 1
        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.size >= 7) {
                val qNo = row[0].trim()
                val qText = row[1].trim()
                val optA = row[2].trim()
                val optB = row[3].trim()
                val optC = row[4].trim()
                val optD = row[5].trim()
                val ansStr = row[6].trim().uppercase()

                val qNoInt = qNo.toIntOrNull()
                val isHeader = qText.equals("Question", ignoreCase = true) || qText.isBlank()
                val isDummyOptions = optA == "A" && optB == "B" && optC == "C" && optD == "D"

                if (qNoInt != null && qNoInt > 0 && !isHeader && !isDummyOptions) {
                    val options = listOf(optA, optB, optC, optD).filter { it.isNotEmpty() }
                    if (qText.isNotEmpty() && options.size >= 2) {
                        val ansIdx = when (ansStr) {
                            "A", "1" -> 0
                            "B", "2" -> 1
                            "C", "3" -> 2
                            "D", "4" -> 3
                            else -> 0
                        }.coerceIn(0, options.size - 1)

                        val diff = when (validIndex % 3) {
                            1 -> Difficulty.EASY
                            2 -> Difficulty.MODERATE
                            else -> Difficulty.DIFFICULT
                        }
                        val topic = topics[(validIndex - 1) % topics.size]

                        list.add(
                            Question(
                                id = "D-F1-C3-${String.format("%03d", validIndex)}",
                                track = Track.DECK_OIC_NW,
                                functionId = "F1",
                                competenceId = "C3",
                                topic = topic,
                                questionText = qText,
                                options = options,
                                correctAnswerIndex = ansIdx,
                                explanation = "",
                                difficulty = diff
                            )
                        )
                        validIndex++
                    }
                }
            }
        }
        return list
    }

    private fun getFallbackC3Questions(): List<Question> {
        val list = mutableListOf<Question>()
        val c3Topics = listOf("ARPA Tracking & Vectors", "Radar Wave Propagation & Clutter", "CPA/TCPA Thresholds", "Trial Manoeuvre Simulation")
        for (i in 1..15) {
            val topic = c3Topics[(i - 1) % c3Topics.size]
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            list.add(
                Question(
                    id = "D-F1-C3-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C3",
                    topic = topic,
                    questionText = "When evaluating collision risk on an ARPA display, what does a Relative Vector of a tracked target pointed directly at your own ship's origin indicate?",
                    options = listOf(
                        "The target vessel is on a parallel heading at the same speed.",
                        "Collision risk exists; the target will pass with a Closest Point of Approach (CPA) of zero.",
                        "The target vessel is stopped dead in the water.",
                        "Your own vessel's speed log has lost Doppler ground-tracking."
                    ),
                    correctAnswerIndex = 1,
                    explanation = "A Relative Vector pointing at own ship origin means the Closest Point of Approach (CPA) is zero, indicating collision if no action is taken.",
                    difficulty = diff
                )
            )
        }
        return list
    }

    fun getC4Questions(): List<Question> {
        c4Cache?.let { return it }
        val loaded = loadC4Csv()
        val result = if (loaded.isNotEmpty()) loaded else getFallbackC4Questions()
        c4Cache = result
        return result
    }

    private fun loadC4Csv(): List<Question> {
        val assetPath = "questions/deck/oic_nw_c4.csv"
        val stream = openAssetStream(assetPath) ?: return emptyList()

        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            .replace("&#39;", "'")
            .replace("/'", "'")
            .replace("&amp;", "&")
            .replace("°", "°")
        val rows = parseCsvText(text)
        if (rows.size <= 1) return emptyList()

        val list = mutableListOf<Question>()
        val topics = listOf(
            "ECDIS Safety & Performance Standards",
            "ENC & RNC Data Formats",
            "Route Planning & Route Monitoring",
            "Safety Contour & Depth Parameters",
            "ECDIS Alarms & Warnings",
            "Admiralty Information Overlay (AIO)",
            "IHO Standards & Data Protection (S-52, S-57, S-63)"
        )

        var validIndex = 1
        for (i in 1 until rows.size) {
            val row = rows[i]
            if (row.size >= 7) {
                val qNo = row[0].trim()
                val qText = row[1].trim()
                val optA = row[2].trim()
                val optB = row[3].trim()
                val optC = row[4].trim()
                val optD = row[5].trim()
                val ansStr = row[6].trim().uppercase()

                val qNoInt = qNo.toIntOrNull()
                val isHeader = qText.equals("Question", ignoreCase = true) || qText.isBlank()
                val isDummyOptions = optA == "A" && optB == "B" && optC == "C" && optD == "D"

                if (qNoInt != null && qNoInt > 0 && !isHeader && !isDummyOptions) {
                    val options = listOf(optA, optB, optC, optD).filter { it.isNotEmpty() }
                    if (qText.isNotEmpty() && options.size >= 2) {
                        val ansIdx = when (ansStr) {
                            "A", "1" -> 0
                            "B", "2" -> 1
                            "C", "3" -> 2
                            "D", "4" -> 3
                            else -> 0
                        }.coerceIn(0, options.size - 1)

                        val diff = when (validIndex % 3) {
                            1 -> Difficulty.EASY
                            2 -> Difficulty.MODERATE
                            else -> Difficulty.DIFFICULT
                        }
                        val topic = topics[(validIndex - 1) % topics.size]

                        list.add(
                            Question(
                                id = "D-F1-C4-${String.format("%03d", validIndex)}",
                                track = Track.DECK_OIC_NW,
                                functionId = "F1",
                                competenceId = "C4",
                                topic = topic,
                                questionText = qText,
                                options = options,
                                correctAnswerIndex = ansIdx,
                                explanation = "",
                                difficulty = diff
                            )
                        )
                        validIndex++
                    }
                }
            }
        }
        return list
    }

    private fun getFallbackC4Questions(): List<Question> {
        val list = mutableListOf<Question>()
        for (i in 1..10) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            list.add(
                Question(
                    id = "D-F1-C4-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C4",
                    topic = "ECDIS Safety & Performance Standards",
                    questionText = "In an ECDIS system, if the user-input 'Safety Contour' depth does not exist exactly in the underlying Electronic Navigational Chart (ENC) database, how does ECDIS behave?",
                    options = listOf(
                        "ECDIS interpolates a theoretical safety line between existing soundings.",
                        "ECDIS defaults to the next deeper available depth contour present in the ENC cell.",
                        "ECDIS disables anti-grounding audible alarms until a valid contour is chosen.",
                        "ECDIS defaults to the shallow water 2-meter baseline."
                    ),
                    correctAnswerIndex = 1,
                    explanation = "Per IMO MSC.232(82), if the operator inputs a safety contour value not available in the ENC vector dataset, ECDIS defaults to the next deeper available contour line.",
                    difficulty = diff
                )
            )
        }
        return list
    }

    fun getC5Questions(): List<Question> {
        c5Cache?.let { return it }
        val loaded = loadC5Csv()
        val result = if (loaded.isNotEmpty()) loaded else getFallbackC5Questions()
        c5Cache = result
        return result
    }

    private fun loadC5Csv(): List<Question> {
        val assetPath = "questions/deck/oic_nw_c5.csv"
        val stream = openAssetStream(assetPath) ?: return emptyList()

        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            .replace("&#39;", "'")
            .replace("/'", "'")
            .replace("&amp;", "&")
            .replace("°", "°")
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

        val list = mutableListOf<Question>()
        val topics = listOf(
            "Emergency Ship Handling & SAR",
            "Muster Lists & Emergency Drills",
            "Damage Control & Watertight Integrity",
            "Helicopter Evacuation Procedures",
            "Collision & Grounding Response",
            "Abandon Ship & Survival Craft Operations",
            "GMDSS Distress Alerting & Radio Silence"
        )

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
                if (qText.isNotEmpty() && options.size >= 2) {
                    val ansIdx = when (ansStr) {
                        "A", "1" -> 0
                        "B", "2" -> 1
                        "C", "3" -> 2
                        "D", "4" -> 3
                        else -> 0
                    }.coerceIn(0, options.size - 1)

                    val diff = when (i % 3) {
                        1 -> Difficulty.EASY
                        2 -> Difficulty.MODERATE
                        else -> Difficulty.DIFFICULT
                    }
                    val topic = topics[(i - (headerIndex + 1)) % topics.size]

                    list.add(
                        Question(
                            id = "O-F1-C5-${String.format("%03d", list.size + 1)}",
                            track = Track.DECK_OIC_NW,
                            functionId = "F1",
                            competenceId = "C5",
                            topic = topic,
                            questionText = qText,
                            options = options,
                            correctAnswerIndex = ansIdx,
                            explanation = "",
                            difficulty = diff
                        )
                    )
                }
            }
        }
        return list
    }

    private fun getFallbackC5Questions(): List<Question> {
        val list = mutableListOf<Question>()
        for (i in 1..10) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            list.add(
                Question(
                    id = "O-F1-C5-${String.format("%03d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C5",
                    topic = "Emergency Ship Handling & SAR",
                    questionText = "Which standard emergency ship maneuver is specifically designed for a 'Delayed Action' or 'Missing Person' man overboard scenario to return the ship exactly to its opposite reciprocal track line?",
                    options = listOf("Anderson Turn", "Williamson Turn", "Scharnow Turn", "Round Turn"),
                    correctAnswerIndex = 2,
                    explanation = "The Scharnow Turn returns the vessel back onto her reciprocal wake line well astern of her current position, optimal for delayed action/missing person scenarios.",
                    difficulty = diff
                )
            )
        }
        return list
    }

    fun getC7Questions(): List<Question> {
        c7Cache?.let { return it }
        val loaded = loadC7Csv()
        val result = if (loaded.isNotEmpty()) loaded else getFallbackC7Questions()
        c7Cache = result
        return result
    }

    private fun loadC7Csv(): List<Question> {
        val assetPath = "questions/deck/oic_nw_c7.csv"
        val stream = openAssetStream(assetPath) ?: return emptyList()

        val text = stream.bufferedReader(java.nio.charset.Charset.forName("Windows-1252")).use { it.readText() }
            .replace("&#39;", "'")
            .replace("/'", "'")
            .replace("&amp;", "&")
            .replace("°", "°")
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

        val fallbackPool = getFallbackC7Questions()
        val list = MutableList(533) { idx ->
            val i = idx + 1
            val baseQ = fallbackPool[idx % fallbackPool.size]
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            Question(
                id = "O-F1-C7-${String.format("%03d", i)}",
                track = Track.DECK_OIC_NW,
                functionId = "F1",
                competenceId = "C7",
                topic = baseQ.topic,
                questionText = baseQ.questionText,
                options = baseQ.options,
                correctAnswerIndex = baseQ.correctAnswerIndex,
                explanation = "",
                difficulty = diff
            )
        }

        val topics = listOf(
            "Phonetic Alphabet & Radiotelephony Procedures",
            "International Code of Signals (Flags & Single-letter codes)",
            "Urgency, Safety & Distress Message Markers (IMO SMCP)",
            "Visual Signalling (Flags, Morse Code, Flashing Lights)",
            "Vessel Traffic Services (VTS) & Port Communications",
            "Medical Assistance Requests & Medico/Radio Medical Messages",
            "Search and Rescue (SAR) Communications & Aircraft Interaction"
        )

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
                if (qNoInt != null && qNoInt in 1..533 && qText.isNotEmpty() && options.size >= 2) {
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
                        id = "O-F1-C7-${String.format("%03d", qNoInt)}",
                        track = Track.DECK_OIC_NW,
                        functionId = "F1",
                        competenceId = "C7",
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
        return list
    }

    private fun getFallbackC7Questions(): List<Question> {
        val list = mutableListOf<Question>()
        val baseFallbacks = listOf(
            Triple(
                "Under the IMO Standard Marine Communication Phrases (SMCP), which Message Marker must be used when you intend to inform another vessel of an urgent danger such as a drifting unlit barge?",
                listOf("INFORMATION", "INSTRUCTION", "WARNING", "ADVICE"),
                2
            ),
            Triple(
                "Under INTERCO, what is the meaning of single-letter flag 'A' (Alpha)?",
                listOf("I have a diver down; keep well clear at slow speed", "I am taking in dangerous cargo", "Affirmative", "I require a pilot"),
                0
            ),
            Triple(
                "Under INTERCO, what is the meaning of single-letter flag 'B' (Bravo)?",
                listOf("I am taking in, discharging, or carrying dangerous goods", "I have a diver down", "I require a tug", "Man overboard"),
                0
            ),
            Triple(
                "In visual signalling by daylight flashing lamp, what is the erase signal indicating a mistake in transmission?",
                listOf("A series of 8 dots (EEEEEEEE)", "A series of 4 dashes", "Sending 'SOS'", "Sending 'TTTT'"),
                0
            ),
            Triple(
                "Under IMO SMCP, what is the meaning of the standard wheel order 'MIDSHIPS'?",
                listOf("Rudder to be brought to zero amidships position on centerline", "Rudder 15 degrees to starboard", "Steer towards middle of channel", "Check swing immediately"),
                0
            )
        )
        for (i in 1..20) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val base = baseFallbacks[(i - 1) % baseFallbacks.size]
            list.add(
                Question(
                    id = "O-F1-C7-${String.format("%03d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C7",
                    topic = "Phonetic Alphabet & Radiotelephony Procedures",
                    questionText = base.first,
                    options = base.second,
                    correctAnswerIndex = base.third,
                    explanation = "IMO SMCP and INTERCO Visual Signalling standard specification.",
                    difficulty = diff
                )
            )
        }
        return list
    }

    fun getC9Questions(): List<Question> {
        c9Cache?.let { return it }
        val loaded = loadC9Csv()
        val result = if (loaded.isNotEmpty()) loaded else getFallbackC9Questions()
        c9Cache = result
        return result
    }

    private fun loadC9Csv(): List<Question> {
        val assetPath = "questions/deck/oic_nw_c9.csv"
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

        val fallbackPool = getFallbackC9Questions()
        val list = MutableList(508) { idx ->
            val i = idx + 1
            val baseQ = fallbackPool[idx % fallbackPool.size]
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            Question(
                id = "O-F1-C9-${String.format("%03d", i)}",
                track = Track.DECK_OIC_NW,
                functionId = "F1",
                competenceId = "C9",
                topic = baseQ.topic,
                questionText = baseQ.questionText,
                options = baseQ.options,
                correctAnswerIndex = baseQ.correctAnswerIndex,
                explanation = "",
                difficulty = diff
            )
        }

        val topics = listOf(
            "Ship Handling & Hydrodynamic Effects",
            "Pivot Point & Turning Circles",
            "Squat & Shallow Water Effects",
            "Bank Suction & Cushion Effects",
            "Anchoring & Mooring Operations",
            "Man Overboard Rescue Manoeuvres",
            "Heavy Weather Ship Handling"
        )

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
                if (qNoInt != null && qNoInt in 1..508 && qText.isNotEmpty() && options.size >= 2) {
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
                        id = "O-F1-C9-${String.format("%03d", qNoInt)}",
                        track = Track.DECK_OIC_NW,
                        functionId = "F1",
                        competenceId = "C9",
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
        return list
    }

    private fun getFallbackC9Questions(): List<Question> {
        val list = mutableListOf<Question>()
        val topics = listOf(
            "Ship Handling & Hydrodynamic Effects",
            "Pivot Point & Turning Circles",
            "Squat & Shallow Water Effects"
        )
        val data = listOf(
            Triple(
                "Where is the approximate location of a ship's Pivot Point when the vessel is moving ahead with steady speed in deep water?",
                listOf(
                    "At the center of gravity (amidships).",
                    "Approximately 1/3 to 1/4 of the ship's length from the bow.",
                    "Approximately 1/4 of the ship's length from the stern.",
                    "At the propeller boss."
                ),
                1
            ),
            Triple(
                "When a vessel with a full hull form (Block Coefficient Cb > 0.70, such as a bulk carrier or tanker) enters shallow confined water at speed, how does 'Squat' typically manifest?",
                listOf(
                    "The vessel will squat by the stern with increased trim aft.",
                    "The vessel will squat by the head with increased draft forward.",
                    "The vessel will rise bodily with reduced draft throughout.",
                    "The vessel's GM increases, eliminating rolling completely."
                ),
                1
            ),
            Triple(
                "What hydrodynamic phenomena occur when a ship navigates too close to a vertical canal bank (Bank Effect)?",
                listOf(
                    "Bank suction at the bow and bank cushion at the stern.",
                    "Bank cushion (cushioning pressure) pushing the bow away from the bank, and bank suction drawing the stern towards the bank.",
                    "Uniform repulsion pushing the ship bodily sideways.",
                    "Increased propeller rpm with zero rudder response."
                ),
                1
            )
        )
        for (i in 1..15) {
            val d = data[(i - 1) % data.size]
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            list.add(
                Question(
                    id = "O-F1-C9-${String.format("%03d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C9",
                    topic = topics[(i - 1) % topics.size],
                    questionText = d.first,
                    options = d.second,
                    correctAnswerIndex = d.third,
                    explanation = "Under steady headway, hydrodynamic effects dictate ship manoeuvrability and pivot point location.",
                    difficulty = diff
                )
            )
        }
        return list
    }
}
