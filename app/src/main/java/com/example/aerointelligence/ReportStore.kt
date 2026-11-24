package com.example.aerointelligence

data class AircraftId(
    val registration: String = "",
    val type: String = "",
    val serial: String = "",
    val operator: String = "",
    val auditor: String = "",
    val date: String = ""
)

object ReportStore {
    var aircraftId = AircraftId()
}