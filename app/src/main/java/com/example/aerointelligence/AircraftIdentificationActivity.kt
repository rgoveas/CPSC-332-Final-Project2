package com.example.aerointelligence
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar


class AircraftIdentificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aircraft_identification)

        val tilRegistration = findViewById<TextInputLayout>(R.id.tilRegistration)
        val tilType = findViewById<TextInputLayout>(R.id.tilType)
        val tilAuditor = findViewById<TextInputLayout>(R.id.tilAuditor)
        val tilDate = findViewById<TextInputLayout>(R.id.tilDate)

        val etRegistration = findViewById<TextInputEditText>(R.id.etRegistration)
        val etType = findViewById<TextInputEditText>(R.id.etType)
        val etSerial = findViewById<TextInputEditText>(R.id.etSerial)
        val etOperator = findViewById<TextInputEditText>(R.id.etOperator)
        val etAuditor = findViewById<TextInputEditText>(R.id.etAuditor)
        val etDate = findViewById<TextInputEditText>(R.id.etDate)

        // Refill the form if a report was already saved
        with(ReportStore.aircraftId) {
            etRegistration.setText(registration)
            etType.setText(type)
            etSerial.setText(serial)
            etOperator.setText(operator)
            etAuditor.setText(auditor)
            etDate.setText(date)
        }

        etDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
                    tilDate.error = null
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            // listOf evaluates every check, so all missing fields show their error at once
            val allValid = listOf(
                checkRequired(tilRegistration, etRegistration, "Registration is required"),
                checkRequired(tilType, etType, "Aircraft type is required"),
                checkRequired(tilAuditor, etAuditor, "Auditor name is required"),
                checkRequired(tilDate, etDate, "Audit date is required")
            ).all { it }

            if (!allValid) return@setOnClickListener

            ReportStore.aircraftId = AircraftId(
                registration = etRegistration.value(),
                type = etType.value(),
                serial = etSerial.value(),
                operator = etOperator.value(),
                auditor = etAuditor.value(),
                date = etDate.value()
            )
            Toast.makeText(this, "Aircraft identification saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun TextInputEditText.value(): String = text.toString().trim()

    private fun checkRequired(
        layout: TextInputLayout,
        field: TextInputEditText,
        message: String
    ): Boolean {
        val empty = field.value().isEmpty()
        layout.error = if (empty) message else null
        return !empty
    }
}