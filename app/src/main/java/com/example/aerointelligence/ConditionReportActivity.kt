package com.example.aerointelligence

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ConditionReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_condition_report)

        val aircraft = ReportStore.aircraftId

        findViewById<TextView>(R.id.tvAircraftInfo).text =
            """
            Registration: ${aircraft.registration}
            Aircraft Type: ${aircraft.type}
            Serial Number: ${aircraft.serial}
            Operator: ${aircraft.operator}
            Auditor: ${aircraft.auditor}
            Audit Date: ${aircraft.date}
            """.trimIndent()
    }
}