package com.example.data.repository

import com.example.data.model.Difficulty
import com.example.data.model.Quadruple
import com.example.data.model.Question
import com.example.data.model.Track

object MaritimeQuestionBankDeckF1 {

    fun getQuestions(): List<Question> {
        val list = mutableListOf<Question>()

        // ==========================================
        // C1: Plan and conduct a passage and determine position (35 items)
        // ==========================================
        val c1Topics = listOf(
            "Terrestrial Navigation & Plotting",
            "Compass Errors & Corrections",
            "Passage Planning & Appraisal",
            "Tides & Tidal Stream Calculations",
            "Celestial Navigation & Sight Reduction",
            "Chart Projections & Mercator Scale",
            "Electronic Position Fixing & GNSS Errors",
            "Great Circle Sailing & Vertex Calculations"
        )
        for (i in 1..35) {
            val topic = c1Topics[(i - 1) % c1Topics.size]
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "When planning a passage across oceanic routes, what is the primary geometric difference between a Great Circle track and a Rhumb Line track on a standard Mercator projection chart?",
                    listOf(
                        "A Great Circle track appears as a straight line, while a Rhumb Line appears as a curve towards the equator.",
                        "A Great Circle track appears as a curved line convex to the nearer pole, while a Rhumb Line appears as a straight line intersecting meridians at a constant angle.",
                        "Both appear as identical straight lines at latitudes below 45 degrees.",
                        "A Great Circle track maintains a constant compass heading, whereas a Rhumb Line changes course continuously."
                    ),
                    1,
                    "On a Mercator projection chart, a Rhumb line is a straight line intersecting all meridians at an oblique constant angle, while a Great Circle track curves toward the pole. (Bowditch, Practical Navigator; MARINA TOS Competence 1)."
                )
                2 -> Quadruple(
                    "The ship's gyro compass indicates a heading of 086°. A transit bearing of two fixed leading lights known to be on true line 088° is observed on the gyro repeater as 087°. What is the gyro error?",
                    listOf("1° High (West)", "1° Low (East)", "2° Low (East)", "2° High (West)"),
                    1,
                    "Gyro Error = True Bearing (088°) - Gyro Bearing (087°) = +1° (1° Low or 1° East). Rule: 'Gyro is Low, Error is East; Gyro is High, Error is West.'"
                )
                3 -> Quadruple(
                    "According to IMO Resolution A.893(21) on Passage Planning, what are the four distinct operational stages required for every voyage plan?",
                    listOf(
                        "Assessment, Routing, Piloting, and Anchoring",
                        "Appraisal, Planning, Execution, and Monitoring",
                        "Inspection, Plotting, Waypointing, and Debriefing",
                        "Departure, Ocean Transit, Coastal Approach, and Mooring"
                    ),
                    1,
                    "IMO Resolution A.893(21) defines the 4 core stages of voyage planning as Appraisal, Planning, Execution, and Monitoring (APEM)."
                )
                4 -> Quadruple(
                    "Using the Rule of Twelfths for semi-diurnal tides with a range of 3.6 meters, what is the anticipated rise in tidal height during the third hour after Low Water?",
                    listOf("0.30 meters (1/12th)", "0.60 meters (2/12ths)", "0.90 meters (3/12ths)", "1.20 meters (4/12ths)"),
                    2,
                    "By the Rule of Twelfths: 1st hr = 1/12, 2nd hr = 2/12, 3rd hr = 3/12 (3/12 * 3.6 = 0.90m)."
                )
                5 -> Quadruple(
                    "When taking a meridian altitude observation of the Sun at upper transit, which correction must be applied to the sextant reading to obtain the True Altitude (Ho)?",
                    listOf(
                        "Index error, dip, refraction, semi-diameter, and parallax.",
                        "Index error and magnetic variation only.",
                        "Coriolis effect, speed log error, and precession.",
                        "Chart datum offset and draught correction."
                    ),
                    0,
                    "Sextant altitude (Hs) is corrected for Index Error -> Apparent Altitude (Ha) corrected for Dip -> Observed Altitude corrected for Refraction, Semi-diameter, and Parallax to yield True Altitude (Ho)."
                )
                6 -> Quadruple(
                    "What is the mathematical definition of Meridional Parts (DMP) on a Mercator chart projection?",
                    listOf(
                        "The length of the equator divided by the cosine of the mid-latitude.",
                        "The distance on a Mercator chart from the equator to any parallel of latitude, expressed in minutes of longitude.",
                        "The difference in minutes of arc between the prime meridian and local meridian.",
                        "The scale factor applied to the radar range ring spacing."
                    ),
                    1,
                    "Meridional Parts represent the distance along a meridian from the equator to a given latitude on a Mercator chart, expressed in minutes of arc of the equator."
                )
                7 -> Quadruple(
                    "Which GNSS error is caused by satellite signals bouncing off the ship's superstructure or sea surface before reaching the antenna?",
                    listOf("Ionospheric delay", "Tropospheric refraction", "Multipath interference", "Ephemeris orbital degradation"),
                    2,
                    "Multipath error occurs when signals reflect off surrounding metallic surfaces or water prior to arriving at the GNSS antenna, causing pseudo-range calculation errors."
                )
                8 -> Quadruple(
                    "What is the Vertex of a Great Circle track connecting two oceanic positions?",
                    listOf(
                        "The point where the Great Circle intersects the Equator.",
                        "The point of highest latitude reached along the Great Circle arc.",
                        "The initial departure waypoint with the steepest course angle.",
                        "The point where the track changes from Rhumb Line to composite sailing."
                    ),
                    1,
                    "The vertex is the point on a great circle arc where the latitude is highest (closest to the pole) and where the course is exactly 090° or 270° True."
                )
                9 -> Quadruple(
                    "How often must the magnetic compass error be determined and recorded in the compass observation book according to STCW requirements?",
                    listOf(
                        "Once every 24 hours at noon only.",
                        "At least once every watch, and whenever a major course alteration is executed.",
                        "Only during annual Class survey inspections.",
                        "When entering restricted waters only."
                    ),
                    1,
                    "STCW Code Section A-VIII/2 requires that magnetic compass error is determined and logged at least once a watch and after every substantial course change."
                )
                10 -> Quadruple(
                    "What is the primary purpose of parallel indexing (PI) techniques during coastal navigation?",
                    listOf(
                        "To verify radar magnetron transmission frequency.",
                        "To provide continuous real-time cross-track distance monitoring relative to radar-conspicuous landmarks.",
                        "To eliminate true motion sea clutter.",
                        "To calibrate the Doppler speed log over ground."
                    ),
                    1,
                    "Parallel indexing is a real-time radar monitoring technique that immediately detects cross-track displacement from the intended track without waiting for a fix."
                )
                else -> Quadruple(
                    "In Passage Planning & Terrestrial Navigation (Item $i): What is the required standard for calculating Under Keel Clearance (UKC) when approaching a shallow waterway?",
                    listOf(
                        "UKC = Charted Depth + Height of Tide - (Maximum Static Draught + Squat + Heel Allowance + Environmental Safety Margin).",
                        "UKC = Charted Depth minus Draught only, ignoring tidal variations.",
                        "UKC = Half the ship's beam in all circumstances.",
                        "UKC is determined solely by the harbor pilot after boarding."
                    ),
                    0,
                    "UKC calculations must account for Charted Depth, Height of Tide, Vessel Draught, Squat, list/heel allowance, and safety margin per MARINA & IMO guidelines."
                )
            }
            list.add(
                Question(
                    id = "D-F1-C1-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
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
        // C2: Maintain a safe navigational watch (20 items)
        // ==========================================
        val c2Topics = listOf("COLREGs Rule 1-19", "COLREGs Lights & Shapes", "Sound Signals & Restricted Visibility", "STCW Watchkeeping Procedures")
        for (i in 1..20) {
            val topic = c2Topics[(i - 1) % c2Topics.size]
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "Two power-driven vessels are meeting on reciprocal or nearly reciprocal courses so as to involve risk of collision. According to COLREGs Rule 14, what action must each vessel take?",
                    listOf(
                        "The vessel with the other on her starboard side shall keep out of the way.",
                        "Each shall alter her course to port so that each shall pass on the starboard side of the other.",
                        "Each shall alter her course to starboard so that each shall pass on the port side of the other.",
                        "The larger vessel maintains course and speed while the smaller vessel alters course to starboard."
                    ),
                    2,
                    "COLREGs Rule 14(a) mandates that when two power-driven vessels meet on reciprocal courses, each vessel shall alter course to starboard so that each passes on the port side of the other."
                )
                2 -> Quadruple(
                    "Under COLREGs Rule 10, when may a vessel navigate in an Inshore Traffic Zone (ITZ) rather than using the adjacent traffic lane?",
                    listOf(
                        "Whenever the adjacent traffic lane has heavy merchant vessel congestion.",
                        "A vessel of less than 20 meters in length, sailing vessels, and vessels engaged in fishing may use the inshore traffic zone.",
                        "Any vessel executing a Great Circle route to shorten passage time.",
                        "Power-driven vessels under 500 gross tonnage at any time."
                    ),
                    1,
                    "COLREGs Rule 10(d) states that a vessel of less than 20m in length, sailing vessels, and fishing vessels may use the ITZ."
                )
                3 -> Quadruple(
                    "Under STCW Section A-VIII/2 Part 4-1, under what specific operational condition may the Officer of the Watch (OIC-NW) be the sole lookout during daylight hours?",
                    listOf(
                        "Whenever the vessel is in calm sea conditions with visibility exceeding 6 nautical miles.",
                        "Only when the situation has been carefully assessed as safe, considering traffic, weather, visibility, proximity to hazards, and assistance is immediately available to be summoned to the bridge.",
                        "Whenever the autopilot and ECDIS track-control are fully engaged.",
                        "At any time outside of territorial waters and restricted waters."
                    ),
                    1,
                    "STCW Code A-VIII/2 permits the OIC-NW to be the sole lookout in daylight only if assessed as completely safe and assistance is immediately summonable."
                )
                4 -> Quadruple(
                    "Under COLREGs Rule 19, what action is strictly discouraged for a vessel detecting another forward of her beam solely by radar in restricted visibility?",
                    listOf(
                        "An alteration of course to port for a vessel forward of the beam, other than for a vessel being overtaken.",
                        "Stopping engines and navigating with extreme caution.",
                        "Making a substantial alteration of course to starboard.",
                        "Sounding one prolonged blast every 2 minutes."
                    ),
                    0,
                    "COLREGs Rule 19(d)(i) prohibits or strongly advises against an alteration of course to port for a vessel forward of the beam, other than for a vessel being overtaken."
                )
                5 -> Quadruple(
                    "Which lights are displayed by a vessel restricted in her ability to manoeuver (RAM) underway and making way through the water?",
                    listOf(
                        "Three all-round lights in a vertical line: Red-White-Red, plus masthead lights, sidelights, and sternlight.",
                        "Two all-round red lights in a vertical line with no sidelights.",
                        "Three all-round red lights with towing lights aft.",
                        "Two all-round green lights over a white masthead light."
                    ),
                    0,
                    "Rule 27(b): A vessel restricted in her ability to manoeuver displays three vertical all-round lights (Red-White-Red), and when making way, masthead light(s), sidelights, and sternlight."
                )
                6 -> Quadruple(
                    "According to COLREGs Rule 34, what is the mandatory sound signal to indicate doubt or failure to understand the intentions or actions of an approaching vessel?",
                    listOf(
                        "One prolonged blast followed by two short blasts.",
                        "At least five short and rapid blasts on the whistle.",
                        "Three prolonged blasts on the ship's horn.",
                        "Two short blasts followed by one prolonged blast."
                    ),
                    1,
                    "COLREGs Rule 34(d): When vessels in sight are approaching each other and either fails to understand the other's actions, she shall immediately indicate doubt by at least five short and rapid blasts."
                )
                7 -> Quadruple(
                    "What constitutes a 'safe speed' under COLREGs Rule 6?",
                    listOf(
                        "A constant speed not exceeding 12 knots in open waters.",
                        "A speed such that a vessel can take proper and effective action to avoid collision and be stopped within a distance appropriate to the prevailing circumstances.",
                        "The vessel's half-ahead service cruising speed.",
                        "The maximum speed allowed by the local harbor master."
                    ),
                    1,
                    "COLREGs Rule 6 defines safe speed as a speed allowing proper and effective action to avoid collision and stopping within an appropriate distance."
                )
                8 -> Quadruple(
                    "Under COLREGs Rule 13, when is a vessel deemed to be an overtaking vessel?",
                    listOf(
                        "When coming up with another vessel from a direction more than 22.5 degrees abaft her beam.",
                        "When approaching another vessel from within her 112.5 degree forward sector.",
                        "Whenever her speed is at least 3 knots faster than the other vessel.",
                        "When she is crossing the other vessel's port bow."
                    ),
                    0,
                    "Rule 13(b): A vessel is overtaking when coming up with another vessel from a direction more than 22.5° abaft her beam (so at night she would see only the sternlight)."
                )
                else -> Quadruple(
                    "In Watchkeeping & COLREGs (Item $i): Under Rule 8, how must any action to avoid collision be executed?",
                    listOf(
                        "Positive, made in ample time, and with due regard to the observance of good seamanship.",
                        "Gradual and small alterations of course so as not to disrupt engine speed.",
                        "Only after VHF radiotelephone agreement with the target vessel.",
                        "Exclusively by altering course to port if on open seas."
                    ),
                    0,
                    "Rule 8(a) mandates that any action to avoid collision shall be positive, made in ample time, and with due regard to good seamanship."
                )
            }
            list.add(
                Question(
                    id = "D-F1-C2-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C2",
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
        // C3: Use of Radar and ARPA (15 items)
        // ==========================================
        val c3Topics = listOf("ARPA Tracking & Vectors", "Radar Wave Propagation & Clutter", "CPA/TCPA Thresholds", "Trial Manoeuvre Simulation")
        for (i in 1..15) {
            val topic = c3Topics[(i - 1) % c3Topics.size]
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "When evaluating collision risk on an ARPA display, what does a Relative Vector of a tracked target pointed directly at your own ship's origin indicate?",
                    listOf(
                        "The target vessel is on a parallel heading at the same speed.",
                        "Collision risk exists; the target will pass with a Closest Point of Approach (CPA) of zero.",
                        "The target vessel is stopped dead in the water.",
                        "Your own vessel's speed log has lost Doppler ground-tracking."
                    ),
                    1,
                    "A Relative Vector pointing at own ship origin means the Closest Point of Approach (CPA) is zero, indicating collision if no action is taken."
                )
                2 -> Quadruple(
                    "Why is an S-band (10 cm / 3 GHz) navigational radar generally superior to an X-band (3 cm / 9 GHz) radar when navigating through heavy tropical squalls and torrential rain?",
                    listOf(
                        "S-band radar has a much narrower horizontal beam width than X-band.",
                        "The longer 10 cm wavelength of S-band experiences significantly less attenuation and sea/rain clutter backscatter.",
                        "S-band triggers Search and Rescue Radar Transponders (SART) with higher sensitivity.",
                        "S-band radar operates without a magnetron or solid-state transceiver."
                    ),
                    1,
                    "S-band (10 cm) waves experience significantly less atmospheric absorption and backscatter from rain droplets than 3 cm X-band."
                )
                3 -> Quadruple(
                    "What input error will cause an ARPA to display incorrect True Vectors and aspect for tracked targets while maintaining accurate CPA and TCPA?",
                    listOf(
                        "Gyro compass error.",
                        "Speed log error (speed through water vs speed over ground).",
                        "Incorrect radar pulse repetition frequency.",
                        "Misaligned radar scanner heading marker."
                    ),
                    1,
                    "Speed log errors distort true motion vectors and target aspect, but relative vectors, CPA, and TCPA remain geometrically unaffected because relative motion is derived directly from successive relative positions."
                )
                4 -> Quadruple(
                    "What is the function of the ARPA 'Trial Manoeuvre' facility on a modern bridge radar?",
                    listOf(
                        "To automatically steer the rudder during an emergency maneuver.",
                        "To simulate the effect of planned alterations of course and/or speed on all tracked targets before executing the maneuver.",
                        "To run automated self-diagnostic tests on the transceiver electronics.",
                        "To calibrate the heading sensor against GNSS track."
                    ),
                    1,
                    "The Trial Manoeuvre feature allows the OIC-NW to test a prospective course or speed alteration to verify that the CPA of all targets will remain safe before implementing it."
                )
                else -> Quadruple(
                    "In Radar & ARPA Operations (Item $i): What is the primary cause of target 'target swap' in automatic radar plotting aids?",
                    listOf(
                        "When two tracked targets pass very close to one another and the tracking gate transfers to the stronger echo.",
                        "Sudden fluctuations in the bridge mains AC power frequency.",
                        "Switching from North-Up to Head-Up display orientation.",
                        "Tuning the receiver local oscillator beyond band limits."
                    ),
                    0,
                    "Target swap occurs when echoes pass in close proximity and the ARPA tracking gate transfers lock from one target to another."
                )
            }
            list.add(
                Question(
                    id = "D-F1-C3-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C3",
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
        // C4: Use of ECDIS to maintain safety of navigation (10 items)
        // ==========================================
        for (i in 1..10) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "In an ECDIS system, if the user-input 'Safety Contour' depth does not exist exactly in the underlying Electronic Navigational Chart (ENC) database, how does ECDIS behave?",
                    listOf(
                        "ECDIS interpolates a theoretical safety line between existing soundings.",
                        "ECDIS defaults to the next deeper available depth contour present in the ENC cell.",
                        "ECDIS disables anti-grounding audible alarms until a valid contour is chosen.",
                        "ECDIS defaults to the shallow water 2-meter baseline."
                    ),
                    1,
                    "Per IMO MSC.232(82), if the operator inputs a safety contour value not available in the ENC vector dataset, ECDIS defaults to the next deeper available contour line."
                )
                2 -> Quadruple(
                    "Which ECDIS display category contains the minimum chart content that CANNOT be removed or turned off by the navigator at any time?",
                    listOf("Standard Display", "Display Base", "All Other Information", "Custom User Layer"),
                    1,
                    "Under IMO MSC.232(82), 'Display Base' cannot be removed from the ECDIS display at any time."
                )
                3 -> Quadruple(
                    "What is the operational purpose of the ECDIS Safety Frame (Look-Ahead Cone / Vector Guard)?",
                    listOf(
                        "To automatically adjust display brightness based on ambient bridge lighting.",
                        "To trigger audible and visual anti-grounding alarms when intersecting shallow water contours, wrecks, or isolated dangers within a pre-set time or distance.",
                        "To calculate fuel consumption along the active route.",
                        "To compress ENC vector cell data for rapid rendering."
                    ),
                    1,
                    "The safety frame / look-ahead vector scans ahead of own ship to warn the bridge team of impending dangers to navigation."
                )
                else -> Quadruple(
                    "In ECDIS Navigation (Item $i): What does the ECDIS indication 'ECDIS Operating in RCDS Mode' signify?",
                    listOf(
                        "The system is operating with Raster Navigational Charts (RNC) where vector chart interrogation and automatic anti-grounding safety contour alarms are not fully functional.",
                        "The ECDIS has lost differential GNSS satellite correction.",
                        "The chart data has not been updated within 30 days.",
                        "The system is running on emergency battery backup power."
                    ),
                    0,
                    "When using Raster Navigational Charts (RCDS mode), ECDIS operates like paper charts scanned digitally and cannot perform vector spatial interrogation or automatic contour alarms."
                )
            }
            list.add(
                Question(
                    id = "D-F1-C4-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C4",
                    topic = "ECDIS Safety & Performance Standards",
                    questionText = qText,
                    options = options,
                    correctAnswerIndex = correctIdx,
                    explanation = explanation,
                    difficulty = diff
                )
            )
        }

        // ==========================================
        // C5: Respond to emergencies (10 items)
        // ==========================================
        for (i in 1..10) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "Which standard emergency ship maneuver is specifically designed for a 'Delayed Action' or 'Missing Person' man overboard scenario to return the ship exactly to its opposite reciprocal track line?",
                    listOf("Anderson Turn", "Williamson Turn", "Scharnow Turn", "Round Turn"),
                    2,
                    "The Scharnow Turn returns the vessel back onto her reciprocal wake line well astern of her current position, optimal for delayed action/missing person scenarios."
                )
                2 -> Quadruple(
                    "According to the IAMSAR Manual Volume III, which search pattern is recommended for a single searching vessel when the datum position is known within close limits?",
                    listOf("Expanding Square Search (SS)", "Parallel Track Search (PS)", "Creeping Line Search (CS)", "Track Line Search (TS)"),
                    0,
                    "The Expanding Square Search pattern (SS) is most effective when the target position is known within close limits and the search area is compact."
                )
                3 -> Quadruple(
                    "In the event of an imminent collision, what is the immediate action required by the Officer on Watch before impact?",
                    listOf(
                        "Sound the General Emergency Alarm, maneuver to minimize angle of impact, close watertight doors, and notify the Master and Engine Room.",
                        "Engage full speed astern and abandon ship immediately.",
                        "Broadcast a MAYDAY on VHF Ch 16 before assessing damage.",
                        "Switch off radar scanners to prevent magnetron burnout."
                    ),
                    0,
                    "Standard bridge emergency procedures dictate sounding the general alarm, maneuvering to minimize the impact angle, closing watertight doors, and summoning all hands."
                )
                else -> Quadruple(
                    "In Emergency Response & SAR (Item $i): What is the first priority of damage control following a hull breach or grounding?",
                    listOf(
                        "Establish the extent of flooding, verify watertight integrity of adjacent boundary bulkheads, and calculate stability/reserve buoyancy.",
                        "Discharge all bunker fuel overboard immediately.",
                        "Cut anchor chains and tow lines.",
                        "Derrick off all deck containers."
                    ),
                    0,
                    "Immediate priorities are assessing flooding boundaries, sounding all compartments/tanks, and verifying damage stability."
                )
            }
            list.add(
                Question(
                    id = "D-F1-C5-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C5",
                    topic = "Emergency Ship Handling & SAR",
                    questionText = qText,
                    options = options,
                    correctAnswerIndex = correctIdx,
                    explanation = explanation,
                    difficulty = diff
                )
            )
        }

        // ==========================================
        // C7: Use the IMO SMCP and English in written and oral form (20 items)
        // ==========================================
        for (i in 1..20) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "Under the IMO Standard Marine Communication Phrases (SMCP), which Message Marker must be used when you intend to inform another vessel of an urgent danger such as a drifting unlit barge?",
                    listOf("INFORMATION", "INSTRUCTION", "WARNING", "ADVICE"),
                    2,
                    "IMO SMCP defines WARNING to inform other vessels of an immediate danger or navigational hazard."
                )
                2 -> Quadruple(
                    "In IMO SMCP communications, what is the standardized phrase to indicate that a vessel has her anchor lifted off the sea bottom and is no longer held by it?",
                    listOf("Anchor is clear of bottom.", "Anchor is aweigh.", "Anchor is up and free.", "Anchor is hoisted."),
                    1,
                    "Under IMO SMCP, 'Anchor is aweigh' indicates the anchor has broken ground and the ship is underway."
                )
                3 -> Quadruple(
                    "What is the correct SMCP standard phrase to indicate that your vessel is unable to change speed or heading due to machinery breakdown?",
                    listOf(
                        "I am not under command.",
                        "My vessel is broken and stopping.",
                        "Engine is dead, please look out.",
                        "I have steering difficulty."
                    ),
                    0,
                    "IMO SMCP uses 'I am not under command' (NUC) for complete loss of maneuvering capability."
                )
                4 -> Quadruple(
                    "Under IMO SMCP, which Message Marker is used when the shore-based Vessel Traffic Service (VTS) issues a legally binding directive to a ship?",
                    listOf("ADVICE", "INSTRUCTION", "WARNING", "INTENTION"),
                    1,
                    "The marker INSTRUCTION indicates a mandatory regulation or directive issued by an authorized VTS or coast authority."
                )
                else -> Quadruple(
                    "In IMO SMCP & Maritime English (Item $i): What is the standard response phrase when an instruction from VTS has been fully understood and will be complied with?",
                    listOf(
                        "Understood. I will comply.",
                        "Roger that, will do my best.",
                        "Message received, copy that.",
                        "Affirmative, we try."
                    ),
                    0,
                    "IMO SMCP establishes 'Understood. I will comply' as the precise phrase confirming comprehension and execution of instructions."
                )
            }
            list.add(
                Question(
                    id = "D-F1-C7-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C7",
                    topic = "IMO SMCP & Standard Communications",
                    questionText = qText,
                    options = options,
                    correctAnswerIndex = correctIdx,
                    explanation = explanation,
                    difficulty = diff
                )
            )
        }

        // ==========================================
        // C9: Manoeuver the ship (15 items)
        // ==========================================
        for (i in 1..15) {
            val diff = when (i % 3) {
                1 -> Difficulty.EASY
                2 -> Difficulty.MODERATE
                else -> Difficulty.DIFFICULT
            }
            val (qText, options, correctIdx, explanation) = when (i) {
                1 -> Quadruple(
                    "Where is the approximate location of a ship's Pivot Point when the vessel is moving ahead with steady speed in deep water?",
                    listOf(
                        "At the center of gravity (amidships).",
                        "Approximately 1/3 to 1/4 of the ship's length from the bow.",
                        "Approximately 1/4 of the ship's length from the stern.",
                        "At the propeller boss."
                    ),
                    1,
                    "Under steady headway, hydrodynamic bow resistance moves the pivot point forward to roughly 1/3 to 1/4 length from the stem."
                )
                2 -> Quadruple(
                    "When a vessel with a full hull form (Block Coefficient Cb > 0.70, such as a bulk carrier or tanker) enters shallow confined water at speed, how does 'Squat' typically manifest?",
                    listOf(
                        "The vessel will squat by the stern with increased trim aft.",
                        "The vessel will squat by the head with increased draft forward.",
                        "The vessel will rise bodily with reduced draft throughout.",
                        "The vessel's GM increases, eliminating rolling completely."
                    ),
                    1,
                    "Full-form vessels (Cb > 0.70) experience concentrated Bernoulli suction forward, squatting by the bow and reducing forward UKC."
                )
                3 -> Quadruple(
                    "What hydrodynamic phenomena occur when a ship navigates too close to a vertical canal bank (Bank Effect)?",
                    listOf(
                        "Bank suction at the bow and bank cushion at the stern.",
                        "Bank cushion (cushioning pressure) pushing the bow away from the bank, and bank suction drawing the stern towards the bank.",
                        "Uniform repulsion pushing the ship bodily sideways.",
                        "Increased propeller rpm with zero rudder response."
                    ),
                    1,
                    "Bank cushion creates positive pressure wedge at the bow pushing it towards the channel center, while low pressure water flow creates bank suction pulling the stern inward."
                )
                else -> Quadruple(
                    "In Ship Manoeuvring & Hydrodynamics (Item $i): What is the transverse thrust (paddle wheel effect) of a right-handed fixed-pitch propeller when engine is put Full Astern from stop?",
                    listOf(
                        "Pushes the stern to port, canting the bow to starboard.",
                        "Pushes the stern to starboard, canting the bow to port.",
                        "Has zero transverse effect when ship has no headway.",
                        "Pulls the ship bodily to the starboard side."
                    ),
                    0,
                    "A right-handed propeller running astern discharges upper water flow against the starboard quarter, kicking the stern to port and swinging the bow to starboard."
                )
            }
            list.add(
                Question(
                    id = "D-F1-C9-${String.format("%02d", i)}",
                    track = Track.DECK_OIC_NW,
                    functionId = "F1",
                    competenceId = "C9",
                    topic = "Ship Handling & Hydrodynamic Effects",
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
