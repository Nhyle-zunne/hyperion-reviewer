# OIC-NW C1: first 10 explanation drafts

**Status: private editorial review only. Nothing in this batch is live in either app.**

Prepared 7 November 2026. Source: private question bank at commit `b881555be0b023e2bfb0f966a2c71b96b4aa5e62`. The encrypted bank, stems, choices, answer keys, question order and saved-data identities are unchanged. These drafts sit outside Android assets and PWA delivery.

4 items have a working explanation with supported or explicitly accepted interpretation; 6 need additional question/answer review. All 10 still require editorial approval. “Needs review” does not automatically mean the key is wrong: it can mean an unstated condition, overlapping answers, or missing precision.

## How wording is handled

Each draft stores the full original stem, all choices, original correct answer and an exact content fingerprint. A colon, underscore, punctuation, wording, choice-text or correct-answer change creates a different version. Similarity is only used to discover comparison candidates. Pure answer-choice reordering can retain the same fingerprint because correctness follows the answer text; explanations must not depend on answer letters or positions. The current row-based ID is shown for finding the source, not trusted as permanent identity.

The existing apps already have explanation fields. This batch does not implement the separate approval/CSV-update workflow yet, and uploading a CSV is not currently guaranteed to merge these records automatically. That integration is a later change after the pilot is reviewed.

## Batch summary

| Question | Subject | Editorial status | Main issue |
| --- | --- | --- | --- |
| D-F1-C1-001 | Morning cumulus | Needs review | Existing key A requires an unstated assumption of sufficient instability and moisture for deep convection; small morning cumulus alone does not establish later CB. |
| D-F1-C1-002 | Gyroscope apparent drift | Draft | The stem uses error broadly: apparent drift describes an apparent change relative to Earth; it does not by itself establish a fault in a compensated heading system. |
| D-F1-C1-003 | Compass accuracy at high latitude | Needs review | Compass type is unspecified; magnetic and gyrocompasses have different reasons for high-latitude degradation. |
| D-F1-C1-004 | Cold front | Draft | No substantive issue found. |
| D-F1-C1-005 | Chart courses and bearings | Needs review | Options 1 and 2 are overlapping descriptions for compass-observed bearings converted to true; the key cannot be justified by treating option 2 as simply false. |
| D-F1-C1-006 | Jet stream | Needs review | Options A and B both describe jet streams steering weather systems; official sources do not justify rejecting B. |
| D-F1-C1-007 | Current drift calculation | Draft — closest-listed interpretation accepted | Exact calculation is 2.16 knots; rounded to the options' one-decimal precision it is 2.2 knots, which is not offered. |
| D-F1-C1-008 | Polaris and north | Needs review | The stem confuses angular distance from the north celestial pole with azimuthal departure from due north. |
| D-F1-C1-009 | IALA Region B starboard mark | Draft | No substantive issue found. |
| D-F1-C1-010 | Aligned Sun, Moon and Earth | Needs review | Option A is the intended alignment-specific answer, but “stronger” should be understood as a larger tidal range, not a universal prediction of current speed or a doubling of gravitational attraction. |

## 1. Morning cumulus — D-F1-C1-001

**Original question:** If there are small cumulus clouds in the morning in summer, what kind of cloud cover would you expect and is the reasonable to forecast later in the day?

- **A.** CB cloud.
- **B.** Haze
- **C.** Clear skies.
- **D.** ST and drizzle

**Existing keyed answer (unchanged):** CB cloud.

**Status:** Needs review — do not publish automatically

**Draft explanation**

Cumulonimbus (CB) is a possible later development, rather than a certain forecast from small morning cumulus alone. Summer sunshine heats the surface, and rising warm air cools until water vapour condenses into cumulus. With enough moisture and an unstable atmosphere that permits continued ascent, the clouds can grow into towering cumulus and then cumulonimbus, bringing showers or thunderstorms. Small cumulus can also remain shallow in fair weather. The question does not state the conditions needed to choose CB confidently, so its existing answer needs review; haze, clear skies, and stratus with drizzle do not follow automatically from this observation either.

**Remember:** Cumulus can build into CB when moisture and instability let it keep rising.

**Official references**

- [Met Office — Low level clouds](https://weather.metoffice.gov.uk/learn-about/weather/types-of-weather/clouds/low-level-clouds) — Cumulus clouds: “How do cumulus clouds form?” and “What weather is associated with cumulus clouds?”; development is conditional, and small cumulus humilis indicates fair weather.
- [Met Office — What is convection?](https://weather.metoffice.gov.uk/learn-about/weather/how-weather-works/what-is-convection) — “How does convection affect the weather?” and “Can convection be seen?”; surface heating, rising air, cloud growth, and stronger convection sometimes producing cumulonimbus.
- [Met Office — Tornadoes](https://weather.metoffice.gov.uk/learn-about/weather/types-of-weather/tornadoes) — Explanation of thermals: shallow cumulus develops first; an unstable atmosphere allows much deeper ascent and deep cumulus/cumulonimbus.

**Editorial notes**

- Existing key A requires an unstated assumption of sufficient instability and moisture for deep convection; small morning cumulus alone does not establish later CB.
- Do not assert the three other options are impossible; the observation does not uniquely determine later cloud cover.
- Original question wording and answer key are preserved; no correction is authorized by this draft.

## 2. Gyroscope apparent drift — D-F1-C1-002

**Original question:** What do you call a gyroscope error that affects the heading system due to the rotation of the earth?

- **A.** Apparent drift
- **B.** System error
- **C.** Heading error
- **D.** Transport error

**Existing keyed answer (unchanged):** Apparent drift

**Status:** Draft — still requires editorial approval

**Draft explanation**

Apparent drift is the intended answer. A free gyroscope tends to keep its spin axis pointing in the same direction in space while the Earth rotates beneath it. To an observer on Earth, the axis therefore appears to change direction. Apparent precession is the broader term; its horizontal component is drift. Transport effects arise when the gyro moves over the Earth's surface, whereas this question specifically identifies the Earth's rotation. A working heading system must compensate for these effects.

**Remember:** Earth turns beneath the gyro: apparent drift. Moving the gyro across Earth adds transport effects.

**Official references**

- [NGA, The American Practical Navigator, Volume II](https://msi.nga.mil/api/publications/download?key=16693975%2FSFH00000%2FBowditch_Vol_2_LoRes.pdf&type=view) — Glossary entries: apparent precession (apparent gyro-axis direction change caused by Earth rotation); drift, noun sense 5 (horizontal component of real or apparent precession); drift axis (vertical for a horizontally mounted directional gyro). Exact definitions retrieved from indexed official NGA PDF text; full PDF download timed out/returned HTTP 503.

**Editorial notes**

- The stem uses error broadly: apparent drift describes an apparent change relative to Earth; it does not by itself establish a fault in a compensated heading system.

## 3. Compass accuracy at high latitude — D-F1-C1-003

**Original question:** In what area on the earth surface does the compass decreases its accuracy?

- **A.** 75 degree latitude
- **B.** 85 degree latitude
- **C.** 60 degree latitude
- **D.** 80 degree latitude

**Existing keyed answer (unchanged):** 75 degree latitude

**Status:** Needs review — do not publish automatically

**Draft explanation**

Compass performance can worsen in polar regions, but the question does not identify which compass it means. A magnetic compass becomes unreliable where the horizontal magnetic field is weak; those areas are not bounded by one fixed geographic latitude. A conventional gyrocompass also loses north-seeking strength toward the geographic poles, with practical limits depending on its design and operating conditions. These principles support caution at high latitudes, but they do not establish 75 degrees as the uniquely correct choice over the other listed latitudes.

**Remember:** Magnetic compass: weak horizontal field. Gyrocompass: weaker north-seeking action toward the poles.

**Official references**

- [NOAA NCEI, World Magnetic Model Accuracy, Limitations, and Error Model](https://www.ncei.noaa.gov/products/world-magnetic-model/accuracy-limitations-error-model) — Blackout Zone (BoZ) section: unreliable below horizontal intensity H = 2000 nT; caution zone from 2000 to below 6000 nT.
- [NGA, The American Practical Navigator, Volume I](https://msi.nga.mil/api/publications/download?key=16693975%2FSFH00000%2FBowditch_Vol_1_LoRes.pdf&type=view) — Chapter 9, Compasses, section 937, Using the Gyrocompass, printed page 185: directive force is greatest at the equator and decreases to zero at the poles.

**Editorial notes**

- Compass type is unspecified; magnetic and gyrocompasses have different reasons for high-latitude degradation.
- The official sources reviewed do not support a universal accuracy threshold at 75 degrees latitude.
- All listed choices describe high latitudes, so the stem lacks a condition that makes the keyed choice uniquely correct. Specify instrument type and a documented operating limit before release.

## 4. Cold front — D-F1-C1-004

**Original question:** What is your idea of a Cold Front?

- **A.** It is the line of discontinuity at the surface or a upper level along which an advancing cold air mass is undercutting a warmer air mass.
- **B.** It is the area of a depression occupied by cold air at the surface and comprises the whole depression after the occluding process has been completed
- **C.** It is a period of low temperatures below the average for the season and locality which last 24 hours or longer particularly during the cold season
- **D.** It is the the sudden line of demarcation separating the Gulf Stream from the Labrador current.

**Existing keyed answer (unchanged):** It is the line of discontinuity at the surface or a upper level along which an advancing cold air mass is undercutting a warmer air mass.

**Status:** Draft — still requires editorial approval

**Draft explanation**

A cold front is the boundary where a cooler, denser air mass advances into and replaces warmer air. The denser cold air moves beneath the warm air and can force it upward, which explains the undercutting described in the keyed answer. If the lifted air contains enough moisture, clouds and rain can form; thunderstorms also require instability. A cold front is a moving boundary, not the whole cold-air portion of a depression. A prolonged spell of unusually low temperatures describes cold weather, while a boundary between the Gulf Stream and Labrador Current concerns ocean water, rather than an atmospheric front.

**Remember:** Cold front: cold air advances, warm air rises.

**Official references**

- [National Weather Service Glossary — Cold Front](https://forecast.weather.gov/glossary.php?word=cold+front) — Entry “Cold Front”: a zone separating two air masses, with the cooler, denser mass advancing and replacing the warmer.
- [National Weather Service Louisville — A Basic Discussion on Pressure Systems, Fronts, Jet Streams, and Precipitation](https://www.weather.gov/lmk/basic-fronts) — Cold-front discussion and vertical cross-section: dense cold air advances into warm air; upward motion, moisture-dependent rain, instability-dependent thunderstorms, and fronts aloft.

## 5. Chart courses and bearings — D-F1-C1-005

**Original question:** How would you categorize the courses and the bearings which are laid down on the chart?

- **A.** All the courses and all the bearings which are laid down down on the chart are true.
- **B.** All the courses and all the bearings which are laid down down on the chart are corrected for compass error.
- **C.** All the courses and all the bearings which are laid down down on the chart are corrected for deviation.
- **D.** All the causes and all the bearings which are laid down down on the chart are relatively corrected.

**Existing keyed answer (unchanged):** All the courses and all the bearings which are laid down down on the chart are true.

**Status:** Needs review — do not publish automatically

**Draft explanation**

For ordinary nautical chart work, courses and bearings are normally expressed as true directions, measured from geographic north. A bearing observed with a magnetic compass must be converted before plotting it as true: allow for both deviation and variation, whose signed sum is the compass error. Correcting only for deviation produces a magnetic direction. However, the choices “true” and “corrected for compass error” overlap when discussing observed compass bearings: correcting their compass error makes them true. The intended concept is clear, but the wording does not provide a clean single-answer distinction.

**Remember:** Compass plus deviation gives magnetic; magnetic plus variation gives true. Apply east or west with the correct sign.

**Official references**

- [NGA, The American Practical Navigator, Volume II](https://msi.nga.mil/api/publications/download?key=16693975%2FSFH00000%2FBowditch_Vol_2_LoRes.pdf&type=view) — Chapter 4, Compass Error, section 400, Magnetic Compass Error, printed page 231. Official NGA indexed PDF text defines true direction by geographic meridian, deviation as magnetic-to-compass difference, and total compass error as the algebraic sum of variation and deviation. Full PDF download timed out/returned HTTP 503.

**Editorial notes**

- Options 1 and 2 are overlapping descriptions for compass-observed bearings converted to true; the key cannot be justified by treating option 2 as simply false.
- The word all is too absolute without specifying ordinary chart plotting and the direction reference; a course constructed on a chart was not necessarily observed with a compass.

## 6. Jet stream — D-F1-C1-006

**Original question:** Base on what you know, would you explain how the jet stream affect weather conditions around the world?

- **A.** The strong wind current of the jet stream pushes weather systems around the world
- **B.** The narrow meandering current of high speed winds near the tropopause redirect weather system around the world
- **C.** The strong wind current of the jet stream prevents weather systems from moving around the world
- **D.** The weak wind current of the jet stream redirects weather systems around the world

**Existing keyed answer (unchanged):** The strong wind current of the jet stream pushes weather systems around the world

**Status:** Needs review — do not publish automatically

**Draft explanation**

Jet streams are narrow bands of strong winds high in the atmosphere, usually near the tropopause. They help steer weather systems, so their position and changing path influence where storms travel and which areas become wetter, drier, warmer, or colder. This supports the keyed answer’s general idea that strong winds move weather systems. However, the choice beginning “The narrow meandering current” also describes the same process and adds valid details about the jet stream’s shape, speed, and altitude. The weak-wind option misdescribes a jet stream, and the prevention option contradicts its steering role. The existing single-answer key therefore does not distinguish two scientifically defensible choices.

**Remember:** Jet streams are strong winds aloft that steer storm tracks.

**Official references**

- [NOAA — Weather systems and patterns](https://www.noaa.gov/education/resource-collections/weather-atmosphere/weather-systems-patterns) — Section “Jet streams”: narrow bands of strong wind high in the atmosphere steer weather systems and transfer heat and moisture.
- [NOAA NESDIS — What Is the Jet Stream?](https://www.nesdis.noaa.gov/about/k-12-education/atmosphere/what-the-jet-stream) — Definition at line 25; “How Do Jet Streams Affect Weather?” and “How Does the Jet Stream Help Us Predict the Weather?” describe transport, north–south bends, and changing storm paths.
- [National Weather Service — Prevailing Winds](https://www.weather.gov/source/zhu/ZHU_Training_Page/winds/Wx_Terms/Flight_Environment.htm) — Section “JET STREAM”: narrow bands of high-speed winds, closely associated with the tropopause and polar front.

**Editorial notes**

- Options A and B both describe jet streams steering weather systems; official sources do not justify rejecting B.
- Single-answer item needs editorial review or an authorized option revision before publication.
- Original options and key remain unchanged.

## 7. Current drift calculation — D-F1-C1-007

**Original question:** At 1200H, you fix your position and change course to 315T. At 1225H, you again fix your position, and its 0.9 mile southwest of your DR position. Find the drift:

- **A.** 2.1 knots
- **B.** 2.3 knots
- **C.** 1.9 knots
- **D.** 1.7 knots

**Existing keyed answer (unchanged):** 2.1 knots

**Status:** Draft — exact-result/closest-listed interpretation accepted; full explanation still requires editorial approval

**Draft explanation**

Drift is the current's speed, while set is the direction toward which it carries the vessel. Assuming the stated mile is a nautical mile and the DR plot is otherwise correct, the current has displaced the vessel 0.9 nautical mile in 25 minutes. Convert the interval to hours: 25/60 hour. The drift is therefore 0.9 divided by 25/60, or 2.16 knots. The set is southwest. Among the choices, 2.1 knots is closest, but conventional rounding to one decimal place gives 2.2 knots, which is absent.

**Remember:** Drift = offset in nautical miles × 60 ÷ elapsed minutes; set points from the DR position to the fix.

**Official references**

- [NOAA Tides & Currents, Glossary](https://www.tidesandcurrents.noaa.gov/glossary.html) — Entry: drift (of current), defined as the speed of the current. Calculation uses the problem's distance and elapsed time.
- [NGA, The American Practical Navigator, Volume II](https://msi.nga.mil/api/publications/download?key=16693975%2FSFH00000%2FBowditch_Vol_2_LoRes.pdf&type=view) — Glossary entry: current. Distinguishes set, the direction toward which current flows, from drift, its speed.
- [NOAA National Weather Service, Marine Definitions](https://www.weather.gov/okx/marinedef) — Entry: Knot, a speed of one nautical mile per hour.

**Editorial notes**

- Exact calculation is 2.16 knots; rounded to the options' one-decimal precision it is 2.2 knots, which is not offered.
- The keyed 2.1 knots is the nearest offered value, but the stem does not ask for the nearest offered approximation or specify truncation.
- Mile should explicitly mean nautical mile; interpreting it as a statute mile changes the calculation.

**User decision:** Show 2.16 knots as the calculated result and 2.1 knots as the closest listed option. Keep the original question and key unchanged.

## 8. Polaris and north — D-F1-C1-008

**Original question:** Polaris always appears due north in the sky to a precision better than how many degree?

- **A.** one degree
- **B.** quarter of a degree
- **C.** tenth of a degree
- **D.** half degree

**Existing keyed answer (unchanged):** one degree

**Status:** Needs review — do not publish automatically

**Draft explanation**

Polaris is close to the north celestial pole, but closeness to that pole is different from a one-degree limit on its compass bearing. The 2026 Air Almanac gives Polaris a declination near 89 degrees 23 minutes north, placing it less than one degree from the pole. Its azimuth still changes with the observer's latitude and the star's position around the pole. The official azimuth table includes departures exceeding one degree at high northern latitudes. Therefore, one degree fits a polar-distance question, but the present claim that Polaris always appears due north is inaccurate.

**Remember:** Near the celestial pole does not mean exactly north on the horizon; use Polaris azimuth tables for a precise bearing.

**Official references**

- [USNO, The Air Almanac 2026](https://aa.usno.navy.mil/downloads/publications/aira26_all.pdf) — Page A157, Polaris (Pole Star) Table, 2026, and Azimuth of Polaris, 2026: declination N 89°22.6′; at LHA Aries 0°, azimuth is 1.1° at latitude 65°N and 1.4° at 70°N.
- [USNO, Computing Altitude and Azimuth from Greenwich Apparent Sidereal Time](https://aa.usno.navy.mil/faq/alt_az) — Definition of azimuth and equations using observer latitude, body declination, and local hour angle.

**Editorial notes**

- The stem confuses angular distance from the north celestial pole with azimuthal departure from due north.
- The word always makes the keyed one-degree claim false: the USNO 2026 table directly gives departures larger than one degree at high latitude.
- For a polar-distance question, specify the present era or a stated almanac year rather than making a timeless claim.

## 9. IALA Region B starboard mark — D-F1-C1-009

**Original question:** What is the color of the Starboard hand lateral marks used in IALA Region B?

- **A.** Red
- **B.** Green and white vertical stripes
- **C.** Black with one or more broad horizontal green bands
- **D.** Green

**Existing keyed answer (unchanged):** Red

**Status:** Draft — still requires editorial approval

**Draft explanation**

Starboard-hand lateral marks in IALA Region B are red. Their meaning is referenced to the conventional direction of buoyage, normally the direction taken when entering a harbour from seaward, or another direction established by the buoyage authority. Following that direction, keep these red marks on the vessel's starboard side. Their topmark, when fitted, is a cone pointing upward, and a fitted light is red. When travelling against the direction of buoyage, the side of the vessel on which the same mark is passed reverses.

**Remember:** Region B: red, right, returning—check the chart's direction of buoyage.

**Official references**

- [NGA/NOAA, U.S. Chart No. 1, Edition 13](https://msi.nga.mil/api/publications/download?key=16694005%2FSFH00000%2FChartNo1.pdf) — Appendix 1, IALA Maritime Buoyage System, Region B lateral-mark table, printed page 129; following Direction of Buoyage diagram: starboard marks red, conical topmarks, red light if fitted.
- [USCG, Aids to Navigation Manual—Administration, COMDTINST M16500.7A](https://www.navcen.uscg.gov/sites/default/files/pdf/waterways/shallow_draft/CIM_16500_7a%20%28Admin%20manual%20w%20CH1%20%26%20CH2%29.pdf) — Short Range Aid Marking Systems, C.1, The U.S. Marking System: identifies Region B and contrasts Region A's reversed colour significance.

## 10. Aligned Sun, Moon and Earth — D-F1-C1-010

**Original question:** What conclusion can you draw if the moon and sun align with the Earth?

- **A.** The tides will be stronger because the attraction of the moon and sun is combine
- **B.** The moon's gravitational pull will be twice as strong as the suns
- **C.** Their pulls partially cancel each other out, so the tides are weaker
- **D.** The moon's gravitational attraction has more effect on tides than the sun's

**Existing keyed answer (unchanged):** The tides will be stronger because the attraction of the moon and sun is combine

**Status:** Needs review — do not publish automatically

**Draft explanation**

When the Sun, Moon, and Earth line up near new or full moon, the solar and lunar tidal effects reinforce each other, producing spring tides with a larger difference between high and low water. This is the intended meaning of “stronger” in the keyed answer. Partial cancellation describes neap tides, when the Sun and Moon lie at right angles as viewed from Earth. The Moon produces a greater tide-raising effect than the Sun, but that does not mean its total gravitational pull is greater, and it is not a new consequence of alignment. Actual tidal heights also depend on local conditions.

**Remember:** Aligned Sun and Moon reinforce tidal range: spring tides.

**Official references**

- [NOAA National Ocean Service — Tidal Variations: The Influence of Position and Distance](https://oceanservice.noaa.gov/education/tutorial_tides/tides06_variations.html) — Opening paragraphs: solar tides are about half as large as lunar tides; alignment at new/full moon adds their effects, while right-angle geometry produces neap tides.
- [NOAA JetStream — Tides](https://www.noaa.gov/jetstream/ocean/tides) — Line 50 distinguishes stronger total solar gravitational attraction from the Moon’s larger tidal pull; lines 78 and 84–97 explain local influences, spring tidal range, and neap tides.

**Editorial notes**

- Option A is the intended alignment-specific answer, but “stronger” should be understood as a larger tidal range, not a universal prediction of current speed or a doubling of gravitational attraction.
- Option B confuses total gravitational pull with tide-raising effect; the roughly two-to-one comparison applies to tidal effects.
- Option D is broadly true about tidal influence but does not follow specifically from alignment; review this potentially confusing distractor before publication.
- Original question, options, and key remain unchanged.

## Punctuation and near-variant checks

- **D-F1-C1-460, D-F1-C1-814:** Colon versus blank with chart-scale formatting: both key coastal chart, but options differ. Punctuation alone neither proves equivalence nor proves a different answer.
- **D-F1-C1-009, D-F1-C1-033, D-F1-C1-369:** Region B versus Region A changes red/green; asking shape instead of colour changes the required fact.
- **D-F1-C1-008, D-F1-C1-725, D-F1-C1-731:** Azimuth from due north versus angular distance from the north celestial pole are not interchangeable claims.
- **D-F1-C1-004, D-F1-C1-184, D-F1-C1-364:** Cold front, cold sector and cold wall are different concepts despite similar stem structure.

The drift result is **0.9 ÷ (25/60) = 2.16 knots**. **2.1** is the closest listed option; conventional rounding to one decimal is **2.2**, which is missing. This batch preserves the key and reports the distinction instead of claiming the exact result is 2.1.

## Verification

- Exact original text, choice order and keys checked against the pinned source.
- All 10 content fingerprints recomputed; punctuation and answer-change invalidation and pure-shuffle stability checked.
- Encrypted source bytes remain identical; no app code, assets, entitlements or progress data changed.
- Two independent subject reviews used official sources. This is an editorial draft, not MARINA approval or a production release.
- Android/PWA builds were not needed for these authoring documents; no display/offline acceptance is claimed.
