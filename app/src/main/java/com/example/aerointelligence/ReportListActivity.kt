package com.example.aerointelligence

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import java.io.IOException

class ReportListActivity : AppCompatActivity() {

    private data class Row(
        val id: Int,
        val timestamp: String = "",
        val good: Int = 0,
        val avg: Int = 0,
        val poor: Int = 0,
        val poorItems: List<String> = emptyList(),
        val comment: String = "",
        val message: String? = null      // used for error / empty states
    )

    private val green = Color.parseColor("#2E7D32")
    private val amber = Color.parseColor("#EF6C00")
    private val red = Color.parseColor("#C62828")

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "All Reports"

        val listView = ListView(this).apply {
            divider = null
            dividerHeight = 0
            setBackgroundColor(Color.parseColor("#F2F2F2"))
            setPadding(0, dp(6), 0, dp(6))
            clipToPadding = false
        }

        val btnSummary = Button(this).apply {
            text = "Back to Executive Summary"
            setOnClickListener { goToSummary() }
        }
        val btnMain = Button(this).apply {
            text = "Back to Main Menu"
            setOnClickListener { goToMain() }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(listView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(btnSummary, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(btnMain, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        setContentView(root)

        val rows = loadRows()

        listView.adapter = object : ArrayAdapter<Row>(
            this, android.R.layout.simple_list_item_1, rows
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                buildCard(getItem(position)!!)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val row = rows[position]
            if (row.message == null) {
                startActivity(
                    Intent(this, ReportDetailActivity::class.java).putExtra("reportId", row.id)
                )
            }
        }
    }

    private fun loadRows(): List<Row> {
        val rows = ArrayList<Row>()
        try {
            val reports = ReportRepository.loadAll(this)
            for (i in reports.length() - 1 downTo 0) {          // newest first
                val r = reports.optJSONObject(i) ?: continue
                var good = 0
                var avg = 0
                var poor = 0
                val poorItems = ArrayList<String>()
                for ((label, key) in ReportRepository.ITEMS) {
                    when (r.optString(key)) {
                        "Good" -> good++
                        "Average" -> avg++
                        "Poor" -> { poor++; poorItems.add(label) }
                    }
                }
                rows.add(
                    Row(
                        id = r.optInt("id"),
                        timestamp = r.optString("timestamp"),
                        good = good, avg = avg, poor = poor,
                        poorItems = poorItems,
                        comment = r.optString("comments").trim()
                    )
                )
            }
        } catch (e: IOException) {
            rows.add(Row(-1, message = "Could not read reports: ${e.message}"))
        } catch (e: JSONException) {
            rows.add(Row(-1, message = "Report file is not valid JSON: ${e.message}"))
        }
        if (rows.isEmpty()) rows.add(Row(-1, message = "No reports saved yet."))
        return rows
    }

    private fun buildCard(row: Row): View {
        val outer = LinearLayout(this).apply {
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }

        if (row.message != null) {
            outer.addView(TextView(this).apply {
                text = row.message
                textSize = 15f
                setTextColor(Color.DKGRAY)
                setPadding(dp(8), dp(16), dp(8), dp(16))
            })
            return outer
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            elevation = dp(2).toFloat()
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.WHITE)
            }
        }

        // Header: "Report #12"  [STATUS]
        val statusText: String
        val statusColor: Int
        when {
            row.poor > 0 -> { statusText = "ATTENTION"; statusColor = red }
            row.avg > 0 -> { statusText = "FAIR"; statusColor = amber }
            else -> { statusText = "ALL GOOD"; statusColor = green }
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(
            TextView(this).apply {
                text = "Report #${row.id}"
                textSize = 17f
                setTextColor(Color.BLACK)
                setTypeface(typeface, Typeface.BOLD)
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        header.addView(pill(statusText, statusColor))
        card.addView(header)

        // Timestamp
        card.addView(TextView(this).apply {
            text = row.timestamp
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(0, dp(2), 0, dp(8))
        })

        // Counts
        val counts = LinearLayout(this)
        counts.addView(countView("● ${row.good} Good", green))
        counts.addView(countView("● ${row.avg} Avg", amber))
        counts.addView(countView("● ${row.poor} Poor", red))
        card.addView(counts)

        // Items needing attention
        if (row.poorItems.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = "Needs attention: ${row.poorItems.joinToString(", ")}"
                textSize = 13f
                setTextColor(red)
                setPadding(0, dp(8), 0, 0)
            })
        }

        // Comment preview
        if (row.comment.isNotEmpty()) {
            card.addView(TextView(this).apply {
                text = row.comment
                textSize = 13f
                setTextColor(Color.DKGRAY)
                setTypeface(typeface, Typeface.ITALIC)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, dp(8), 0, 0)
            })
        }

        outer.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT))
        return outer
    }

    private fun pill(text: String, color: Int): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(10), dp(3), dp(10), dp(3))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(color)
            }
        }

    private fun countView(text: String, color: Int): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(color)
            setPadding(0, 0, dp(16), 0)
        }

    private fun goToSummary() {
        startActivity(
            Intent(this, ExecutiveSummaryActivity::class.java)
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
}