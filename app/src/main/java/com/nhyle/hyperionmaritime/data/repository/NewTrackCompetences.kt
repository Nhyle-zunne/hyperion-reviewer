package com.nhyle.hyperionmaritime.data.repository

import com.nhyle.hyperionmaritime.data.model.CompetenceInfo
import com.nhyle.hyperionmaritime.data.model.Track

// =============================================================================
// ENGINE_OIC_EW (Engine Operational Level)
// =============================================================================

val OFFICIAL_ENGINE_OIC_EW_PERMIT_DISTRIBUTION: Map<String, Int> = mapOf(
    "C1" to 14,
    "C2" to 10,
    "C3" to 10,
    "C4" to 30,
    "C5" to 10,
    "C6" to 20,
    "C7" to 15,
    "C8" to 24,
    "C9" to 15,
    "C10" to 10,
    "C11" to 10,
    "C15" to 10,
    "C16" to 22
)

val ENGINE_OIC_EW_FUNCTION_TITLES: Map<String, String> = mapOf(
    "F1" to "Marine engineering at the operational level",
    "F2" to "Electrical, electronic and control engineering at the operational level",
    "F3" to "Maintenance and repair at the operational level",
    "F4" to "Controlling the operation of the ship and care for persons on board at the operational level"
)

val ENGINE_OIC_EW_COMPETENCES: List<CompetenceInfo> = listOf(
    // F1 "Marine engineering at the operational level"
    CompetenceInfo(
        competenceId = "C1",
        functionId = "F1",
        functionName = "Marine engineering at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C1",
        title = "Maintain a safe engineering watch",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C2",
        functionId = "F1",
        functionName = "Marine engineering at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C2",
        title = "Use English in oral and written form",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C3",
        functionId = "F1",
        functionName = "Marine engineering at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C3",
        title = "Use internal communications systems",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C4",
        functionId = "F1",
        functionName = "Marine engineering at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C4",
        title = "Operate main and auxiliary machinery and associated control systems",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C5",
        functionId = "F1",
        functionName = "Marine engineering at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C5",
        title = "Operate fuel, lubrication, ballast and pumping systems and other associated control systems",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    // F2 "Electrical, electronic and control engineering at the operational level"
    CompetenceInfo(
        competenceId = "C6",
        functionId = "F2",
        functionName = "Electrical, electronic and control engineering at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C6",
        title = "Operate electrical, electronic and control systems",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C7",
        functionId = "F2",
        functionName = "Electrical, electronic and control engineering at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C7",
        title = "Maintenance and repair of electrical and electronic equipment",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    // F3 "Maintenance and repair at the operational level"
    CompetenceInfo(
        competenceId = "C8",
        functionId = "F3",
        functionName = "Maintenance and repair at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C8",
        title = "Appropriate use of hand tools, machine tools and measuring instruments for fabrication and repair on board",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C9",
        functionId = "F3",
        functionName = "Maintenance and repair at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C9",
        title = "Maintenance and repair of shipboard machinery and equipment",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    // F4 "Controlling the operation of the ship and care for persons on board at the operational level"
    CompetenceInfo(
        competenceId = "C10",
        functionId = "F4",
        functionName = "Controlling the operation of the ship and care for persons on board at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C10",
        title = "Ensure compliance with pollution-prevention requirements",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C11",
        functionId = "F4",
        functionName = "Controlling the operation of the ship and care for persons on board at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C11",
        title = "Maintain seaworthiness of the ship",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C15",
        functionId = "F4",
        functionName = "Controlling the operation of the ship and care for persons on board at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C15",
        title = "Monitor compliance with legislative requirements",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C16",
        functionId = "F4",
        functionName = "Controlling the operation of the ship and care for persons on board at the operational level",
        track = Track.ENGINE_OIC_EW,
        code = "C16",
        title = "Application of leadership and team working skills",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    )
)

// =============================================================================
// DECK_MGT (Deck Management Level)
// =============================================================================

val OFFICIAL_DECK_MGT_PERMIT_DISTRIBUTION: Map<String, Int> = mapOf(
    "C1" to 10,
    "C2" to 10,
    "C3" to 10,
    "C4" to 10,
    "C5" to 10,
    "C6" to 10,
    "C8" to 10,
    "C9" to 10,
    "C10" to 20,
    "C11" to 10,
    "C12" to 15,
    "C13" to 10,
    "C14" to 10,
    "C15" to 15,
    "C16" to 10,
    "C17" to 10,
    "C18" to 10,
    "C20" to 10
)

val DECK_MGT_FUNCTION_TITLES: Map<String, String> = mapOf(
    "F1" to "Navigation at the management level",
    "F2" to "Cargo handling and stowage at the management level",
    "F3" to "Controlling of operation of the ship and care for persons on board at the management level"
)

val DECK_MGT_COMPETENCES: List<CompetenceInfo> = listOf(
    // F1 "Navigation at the management level"
    CompetenceInfo(
        competenceId = "C1",
        functionId = "F1",
        functionName = "Navigation at the management level",
        track = Track.DECK_MGT,
        code = "C1",
        title = "Plan a voyage and conduct navigation",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C2",
        functionId = "F1",
        functionName = "Navigation at the management level",
        track = Track.DECK_MGT,
        code = "C2",
        title = "Determine position and the accuracy of resultant position fix by any means",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C3",
        functionId = "F1",
        functionName = "Navigation at the management level",
        track = Track.DECK_MGT,
        code = "C3",
        title = "Determine and allow for compass errors",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C4",
        functionId = "F1",
        functionName = "Navigation at the management level",
        track = Track.DECK_MGT,
        code = "C4",
        title = "Coordinate search and rescue operations",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C5",
        functionId = "F1",
        functionName = "Navigation at the management level",
        track = Track.DECK_MGT,
        code = "C5",
        title = "Establish watchkeeping arrangements and procedures",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C6",
        functionId = "F1",
        functionName = "Navigation at the management level",
        track = Track.DECK_MGT,
        code = "C6",
        title = "Maintain safety of navigation through the use of information from navigation equipment and systems to assist command decision making",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C8",
        functionId = "F1",
        functionName = "Navigation at the management level",
        track = Track.DECK_MGT,
        code = "C8",
        title = "Forecast weather and oceanographic conditions",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C9",
        functionId = "F1",
        functionName = "Navigation at the management level",
        track = Track.DECK_MGT,
        code = "C9",
        title = "Respond to navigational emergencies",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C10",
        functionId = "F1",
        functionName = "Navigation at the management level",
        track = Track.DECK_MGT,
        code = "C10",
        title = "Manoeuvre and handle a ship in all conditions",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C11",
        functionId = "F1",
        functionName = "Navigation at the management level",
        track = Track.DECK_MGT,
        code = "C11",
        title = "Operate remote controls of propulsion plant and engineering systems and services",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    // F2 "Cargo handling and stowage at the management level"
    CompetenceInfo(
        competenceId = "C12",
        functionId = "F2",
        functionName = "Cargo handling and stowage at the management level",
        track = Track.DECK_MGT,
        code = "C12",
        title = "Plan and ensure safe loading, stowage, securing, care during the voyage and the unloading of cargoes",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C13",
        functionId = "F2",
        functionName = "Cargo handling and stowage at the management level",
        track = Track.DECK_MGT,
        code = "C13",
        title = "Assess reported defects and damage to cargo spaces, hatch covers and ballast tanks and take appropriate action",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C14",
        functionId = "F2",
        functionName = "Cargo handling and stowage at the management level",
        track = Track.DECK_MGT,
        code = "C14",
        title = "Carriage of dangerous goods",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    // F3 "Controlling of operation of the ship and care for persons on board at the management level"
    CompetenceInfo(
        competenceId = "C15",
        functionId = "F3",
        functionName = "Controlling of operation of the ship and care for persons on board at the management level",
        track = Track.DECK_MGT,
        code = "C15",
        title = "Control trim, stability and stress",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C16",
        functionId = "F3",
        functionName = "Controlling of operation of the ship and care for persons on board at the management level",
        track = Track.DECK_MGT,
        code = "C16",
        title = "Monitor and control compliance with legislative requirements and measures to ensure safety of life at sea, security and protection of the marine environment",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C17",
        functionId = "F3",
        functionName = "Controlling of operation of the ship and care for persons on board at the management level",
        track = Track.DECK_MGT,
        code = "C17",
        title = "Maintain safety and security of the ship's crew and passengers and the operational condition of life-saving, firefighting and other safety systems",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C18",
        functionId = "F3",
        functionName = "Controlling of operation of the ship and care for persons on board at the management level",
        track = Track.DECK_MGT,
        code = "C18",
        title = "Develop emergency and damage control plans and handle emergency situations",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C20",
        functionId = "F3",
        functionName = "Controlling of operation of the ship and care for persons on board at the management level",
        track = Track.DECK_MGT,
        code = "C20",
        title = "Organize and manage the provision of medical care on board",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    )
)

// =============================================================================
// ENGINE_MGT (Engine Management Level)
// =============================================================================

val OFFICIAL_ENGINE_MGT_PERMIT_DISTRIBUTION: Map<String, Int> = mapOf(
    "C1" to 10,
    "C2" to 26,
    "C3" to 28,
    "C4" to 10,
    "C5" to 20,
    "C6" to 20,
    "C7" to 10,
    "C8" to 10,
    "C9" to 10,
    "C10" to 10,
    "C11" to 10,
    "C12" to 10,
    "C13" to 10,
    "C14" to 16
)

val ENGINE_MGT_FUNCTION_TITLES: Map<String, String> = mapOf(
    "F1" to "Marine engineering at the management level",
    "F2" to "Electrical, electronic and control engineering at the management level",
    "F3" to "Maintenance and repair at the management level",
    "F4" to "Controlling the operation of the ship and care for persons on board at the management level"
)

val ENGINE_MGT_COMPETENCES: List<CompetenceInfo> = listOf(
    // F1 "Marine engineering at the management level"
    CompetenceInfo(
        competenceId = "C1",
        functionId = "F1",
        functionName = "Marine engineering at the management level",
        track = Track.ENGINE_MGT,
        code = "C1",
        title = "Manage the operation of propulsion plant machinery",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C2",
        functionId = "F1",
        functionName = "Marine engineering at the management level",
        track = Track.ENGINE_MGT,
        code = "C2",
        title = "Plan and schedule operations",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C3",
        functionId = "F1",
        functionName = "Marine engineering at the management level",
        track = Track.ENGINE_MGT,
        code = "C3",
        title = "Operation, surveillance, performance assessment and maintaining safety of propulsion plant and auxiliary machinery",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C4",
        functionId = "F1",
        functionName = "Marine engineering at the management level",
        track = Track.ENGINE_MGT,
        code = "C4",
        title = "Manage fuel, lubrication and ballast operations",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    // F2 "Electrical, electronic and control engineering at the management level"
    CompetenceInfo(
        competenceId = "C5",
        functionId = "F2",
        functionName = "Electrical, electronic and control engineering at the management level",
        track = Track.ENGINE_MGT,
        code = "C5",
        title = "Manage the operation of electrical and electronic control equipment",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C6",
        functionId = "F2",
        functionName = "Electrical, electronic and control engineering at the management level",
        track = Track.ENGINE_MGT,
        code = "C6",
        title = "Manage trouble shooting, restoration of electrical and electronic control equipment to operating condition",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    // F3 "Maintenance and repair at the management level"
    CompetenceInfo(
        competenceId = "C7",
        functionId = "F3",
        functionName = "Maintenance and repair at the management level",
        track = Track.ENGINE_MGT,
        code = "C7",
        title = "Manage safe and effective maintenance and repair procedures",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C8",
        functionId = "F3",
        functionName = "Maintenance and repair at the management level",
        track = Track.ENGINE_MGT,
        code = "C8",
        title = "Detect and identify the cause of machinery malfunctions and correct faults",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C9",
        functionId = "F3",
        functionName = "Maintenance and repair at the management level",
        track = Track.ENGINE_MGT,
        code = "C9",
        title = "Ensure safe working practices",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    // F4 "Controlling the operation of the ship and care for persons on board at the management level"
    CompetenceInfo(
        competenceId = "C10",
        functionId = "F4",
        functionName = "Controlling the operation of the ship and care for persons on board at the management level",
        track = Track.ENGINE_MGT,
        code = "C10",
        title = "Control trim, stability and stress",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C11",
        functionId = "F4",
        functionName = "Controlling the operation of the ship and care for persons on board at the management level",
        track = Track.ENGINE_MGT,
        code = "C11",
        title = "Monitor and control compliance with legislative requirements and measures to ensure safety of life at sea, security and protection of the marine environment",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C12",
        functionId = "F4",
        functionName = "Controlling the operation of the ship and care for persons on board at the management level",
        track = Track.ENGINE_MGT,
        code = "C12",
        title = "Maintain safety and security of the vessel, crew and passengers and the operational condition of life-saving, fire-fighting and other safety systems",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C13",
        functionId = "F4",
        functionName = "Controlling the operation of the ship and care for persons on board at the management level",
        track = Track.ENGINE_MGT,
        code = "C13",
        title = "Develop emergency and damage control plans and handle emergency situations",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    ),
    CompetenceInfo(
        competenceId = "C14",
        functionId = "F4",
        functionName = "Controlling the operation of the ship and care for persons on board at the management level",
        track = Track.ENGINE_MGT,
        code = "C14",
        title = "Use of leadership and managerial skills",
        officialTOSItems = 1, // replace with real bank size when CSVs load
        description = ""
    )
)
