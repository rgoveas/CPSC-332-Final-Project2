package com.example.aerointelligence

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity2 : AppCompatActivity() {

    private val fileName = "aircraft_reports.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2) // use your actual layout file name

        findViewById<Button>(R.id.button_first).setOnClickListener {
            saveReport()
        }
    }

    // True if a radio button is selected in this group
    private fun isSelected(group: RadioGroup): Boolean =
        group.checkedRadioButtonId != -1

    // Only call after isSelected() is true: returns "Good", "Average" or "Poor"
    private fun ratingOf(group: RadioGroup): String =
        findViewById<RadioButton>(group.checkedRadioButtonId).text.toString()

    // Loads the existing array, or returns an empty one if the file doesn't exist yet
    private fun loadReports(): JSONArray {
        return try {
            val text = openFileInput(fileName).bufferedReader().use { it.readText() }
            if (text.isBlank()) JSONArray() else JSONArray(text)
        } catch (e: FileNotFoundException) {
            JSONArray()
        }
    }

    // Uses highest existing id + 1 so ids stay unique even if entries are deleted later
    private fun nextId(reports: JSONArray): Int {
        var maxId = 0
        for (i in 0 until reports.length()) {
            maxId = maxOf(maxId, reports.getJSONObject(i).optInt("id", 0))
        }
        return maxId + 1
    }

    private fun saveReport() {
        val rgFloor = findViewById<RadioGroup>(R.id.rgCargoFloor)
        val rgCeiling = findViewById<RadioGroup>(R.id.rgCargoCeiling)
        val rgSidewalls = findViewById<RadioGroup>(R.id.rgCargoSidewalls)
        val rgSmoke = findViewById<RadioGroup>(R.id.rgCargoSmokeDetector)

        // Validation: don't save unless every item has a selection
        if (!isSelected(rgFloor) || !isSelected(rgCeiling) ||
            !isSelected(rgSidewalls) || !isSelected(rgSmoke)
        ) {
            Toast.makeText(
                this,
                "Please select a rating for every item before saving",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val comments = findViewById<EditText>(R.id.editTextComments).text.toString().trim()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        try {
            val reports = loadReports()

            val report = JSONObject().apply {
                put("id", nextId(reports))
                put("timestamp", timestamp)
                put("cargoFloor", ratingOf(rgFloor))
                put("cargoCeiling", ratingOf(rgCeiling))
                put("cargoSidewalls", ratingOf(rgSidewalls))
                put("cargoSmokeDetector", ratingOf(rgSmoke))
                put("comments", comments)
            }
            reports.put(report)

            // Rewrites the whole file with the updated array (2 = indent for readability)
            openFileOutput(fileName, MODE_PRIVATE).use {
                it.write(reports.toString(2).toByteArray())
            }

            Toast.makeText(this, "Report saved", Toast.LENGTH_SHORT).show()

            // Pass the saved values to the summary screen
//            val intent = Intent(this, ExecutiveSummaryActivity::class.java).apply {
//                putExtra("id", report.getInt("id"))
//                putExtra("timestamp", report.getString("timestamp"))
//                putExtra("cargoFloor", report.getString("cargoFloor"))
//                putExtra("cargoCeiling", report.getString("cargoCeiling"))
//                putExtra("cargoSidewalls", report.getString("cargoSidewalls"))
//                putExtra("cargoSmokeDetector", report.getString("cargoSmokeDetector"))
//                putExtra("comments", report.getString("comments"))
//            }
//            startActivity(intent)
            finish();
        } catch (e: IOException) {
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: JSONException) {
            Toast.makeText(this, "Data error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    private fun readReport() {
        try {
            val reports = loadReports()
            Log.d("AircraftReport", "Total reports: ${reports.length()}")
            // Print line by line to avoid Logcat's per-message length limit
            reports.toString(2).lines().forEach { Log.d("AircraftReport", it) }
        } catch (e: IOException) {
            Log.e("AircraftReport", "Read failed: ${e.message}")
        } catch (e: JSONException) {
            Log.e("AircraftReport", "File is not valid JSON: ${e.message}")
        }
    }
}

