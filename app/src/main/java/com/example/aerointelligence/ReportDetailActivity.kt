package com.example.aerointelligence

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class ReportDetailActivity : AppCompatActivity() {

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Report"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)   // arrow in the top bar

        val id = intent.getIntExtra("reportId", -1)

        val body = try {
            val reports = ReportRepository.loadAll(this)
            var found: JSONObject? = null
            for (i in 0 until reports.length()) {
                val r = reports.optJSONObject(i) ?: continue
                if (r.optInt("id") == id) {
                    found = r
                    break
                }
            }
            if (found == null) "Report #$id not found." else format(found)
        } catch (e: IOException) {
            "Could not read reports: ${e.message}"
        } catch (e: JSONException) {
            "Report file is not valid JSON: ${e.message}"
        }

        // Logo at the top (centered, rounded corners)
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aero_logo)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundResource(R.drawable.rounded_logo_bg)
            clipToOutline = true
            contentDescription = "Aero Intelligence logo"
        }

        // Report text, scrollable
        val tv = TextView(this).apply {
            text = body
            textSize = 16f
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val scroll = ScrollView(this).apply { addView(tv) }

        val btnBack = Button(this).apply {
            text = "Back to Report List"
            setOnClickListener { goToList() }
        }
        val btnMain = Button(this).apply {
            text = "Back to Main Menu"
            setOnClickListener { goToMain() }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                logo,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(80)
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dp(8)
                    bottomMargin = dp(8)
                }
            )
            addView(scroll, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(btnBack, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(btnMain, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        setContentView(root)
    }

    private fun format(r: JSONObject): String {
        val sb = StringBuilder()
        sb.append("Report #${r.optInt("id")}\n")
        sb.append("${r.optString("timestamp")}\n\n")
        for ((label, key) in ReportRepository.ITEMS) {
            sb.append("$label: ${r.optString(key, "N/A")}\n")
        }
        val comments = r.optString("comments").trim()
        sb.append("\nComments:\n")
        sb.append(if (comments.isEmpty()) "None" else comments)
        return sb.toString()
    }

    private fun goToList() {
        startActivity(
            Intent(this, ReportListActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    private fun goToMain() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            goToList()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}