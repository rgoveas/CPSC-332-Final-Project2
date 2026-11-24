
package com.example.aerointelligence

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // PHYSICAL INSPECTION -> MainActivity2
        findViewById<Button>(R.id.btnOpenFragment_First).setOnClickListener {
            startActivity(Intent(this, MainActivity2::class.java))
        }

        // EXECUTIVE SUMMARY -> ExecutiveSummaryActivity
        findViewById<Button>(R.id.btnOpen).setOnClickListener {
            startActivity(Intent(this, ExecutiveSummaryActivity::class.java))
        }
    }
}