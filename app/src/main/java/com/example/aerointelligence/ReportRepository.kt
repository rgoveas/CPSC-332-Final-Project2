package com.example.aerointelligence

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException

object ReportRepository {

    const val FILE_NAME = "aircraft_reports.json"

    // Display label to JSON key
    val ITEMS = listOf(
        "Cargo Floor" to "cargoFloor",
        "Cargo Ceiling" to "cargoCeiling",
        "Cargo Bulkhead/Sidewalls" to "cargoSidewalls",
        "Cargo Smoke Detector" to "cargoSmokeDetector"
    )

    // All saved reports in saved order (oldest first). Empty if nothing saved yet.
    // May throw IOException / JSONException; callers handle them.
    fun loadAll(context: Context): JSONArray {
        return try {
            val text = context.openFileInput(FILE_NAME).bufferedReader().use { it.readText() }
            if (text.isBlank()) JSONArray() else JSONArray(text)
        } catch (e: FileNotFoundException) {
            JSONArray()
        }
    }

    fun findById(context: Context, id: Int): JSONObject? {
        val reports = loadAll(context)
        for (i in 0 until reports.length()) {
            val r = reports.getJSONObject(i)
            if (r.optInt("id") == id) return r
        }
        return null
    }
}
