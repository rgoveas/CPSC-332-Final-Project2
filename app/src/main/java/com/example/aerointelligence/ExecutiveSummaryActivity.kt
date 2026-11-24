package com.example.aerointelligence

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class ExecutiveSummaryActivity : AppCompatActivity() {

    private val maxPrevious = 4   // previous reports listed (keeps the page to one screen)
    private val items = ReportRepository.ITEMS   // List<Pair<String, String>> (label, jsonKey)

    private val green = Color.parseColor("#2E7D32")
    private val amber = Color.parseColor("#EF6C00")
    private val red = Color.parseColor("#C62828")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_executive_summary)

        val tvTotal = findViewById<TextView>(R.id.tvSummaryTotal)
        val btnLatest = findViewById<Button>(R.id.btnViewLatest)
        val btnAll = findViewById<Button>(R.id.btnViewAll)

        btnAll.setOnClickListener {
            startActivity(Intent(this, ReportListActivity::class.java))
        }

        findViewById<Button>(R.id.btnMainMenu).setOnClickListener { goToMain() }

        val reports = try {
            ReportRepository.loadAll(this)
        } catch (e: IOException) {
            tvTotal.text = "Could not read reports: ${e.message}"
            btnLatest.isEnabled = false
            return
        } catch (e: JSONException) {
            tvTotal.text = "Report file is not valid JSON: ${e.message}"
            btnLatest.isEnabled = false
            return
        }

        if (reports.length() == 0) {
            tvTotal.text = "No reports saved yet."
            btnLatest.isEnabled = false
            btnAll.isEnabled = false
            return
        }

        val latest = reports.optJSONObject(reports.length() - 1)
        if (latest == null) {
            tvTotal.text = "Latest report is unreadable."
            btnLatest.isEnabled = false
            return
        }

        btnLatest.setOnClickListener { openReport(latest.optInt("id")) }

        showTotals(reports)
        showRatings(reports, latest)
        showAttention(latest)
        showPreviousReports(reports)
        showLatestComment(reports)
    }

    private fun openReport(id: Int) {
        startActivity(
            Intent(this, ReportDetailActivity::class.java).putExtra("reportId", id)
        )
    }

    private fun goToMain() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun showTotals(reports: JSONArray) {
        val first = reports.optJSONObject(0)?.optString("timestamp", "?") ?: "?"
        val last = reports.optJSONObject(reports.length() - 1)?.optString("timestamp", "?") ?: "?"

        findViewById<TextView>(R.id.tvSummaryTotal).text =
            "Total reports: ${reports.length()}"
        findViewById<TextView>(R.id.tvSummaryPeriod).text =
            if (reports.length() == 1) "Date: $last" else "Period: $first to $last"
    }

    // One table: item | latest rating | Good / Average / Poor counts across all reports
    private fun showRatings(reports: JSONArray, latest: JSONObject) {
        val table = findViewById<TableLayout>(R.id.tableCounts)
        table.removeAllViews()

        table.addView(makeRow("Item", "Latest", "Good", "Avg", "Poor", bold = true))

        for ((label, key) in items) {
            var good = 0
            var average = 0
            var poor = 0
            for (i in 0 until reports.length()) {
                val r = reports.optJSONObject(i) ?: continue
                when (r.optString(key)) {
                    "Good" -> good++
                    "Average" -> average++
                    "Poor" -> poor++
                }
            }
            val latestRating = latest.optString(key, "N/A")
            table.addView(
                makeRow(
                    label, latestRating, good.toString(), average.toString(), poor.toString(),
                    latestPoor = latestRating == "Poor",
                    poorCountRed = poor > 0
                )
            )
        }
    }

    private fun makeRow(
        label: String,
        latest: String,
        good: String,
        average: String,
        poor: String,
        bold: Boolean = false,
        latestPoor: Boolean = false,
        poorCountRed: Boolean = false
    ): TableRow {
        val row = TableRow(this)
        val pad = dp(3)

        fun cell(text: String, center: Boolean, red: Boolean = false): TextView =
            TextView(this).apply {
                this.text = text
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(pad, pad, pad, pad)
                if (center) gravity = Gravity.CENTER
                if (bold) setTypeface(typeface, Typeface.BOLD)
                if (red) setTextColor(Color.RED)
            }

        row.addView(cell(label, center = false))
        row.addView(cell(latest, center = true, red = latestPoor))
        row.addView(cell(good, center = true))
        row.addView(cell(average, center = true))
        row.addView(cell(poor, center = true, red = poorCountRed))
        return row
    }

    private fun showAttention(latest: JSONObject) {
        val poorItems = items.filter { latest.optString(it.second) == "Poor" }.map { it.first }
        val tv = findViewById<TextView>(R.id.tvSummaryAttention)

        if (poorItems.isEmpty()) {
            tv.text = "No items rated Poor in the latest report."
            tv.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            tv.text = "Needs attention: ${poorItems.joinToString(", ")}"
            tv.setTextColor(Color.RED)
        }
    }

    // Up to maxPrevious older reports, newest first, one tappable card each
    private fun showPreviousReports(reports: JSONArray) {
        val container = findViewById<LinearLayout>(R.id.containerPrevious)
        container.removeAllViews()

        val olderCount = reports.length() - 1
        if (olderCount <= 0) {
            container.addView(smallText("No previous reports."))
            return
        }

        val shown = minOf(olderCount, maxPrevious)
        for (n in 0 until shown) {
            val r = reports.optJSONObject(reports.length() - 2 - n) ?: continue
            val id = r.optInt("id")

            var good = 0
            var avg = 0
            var poor = 0
            val poorItems = ArrayList<String>()
            for ((label, key) in items) {
                when (r.optString(key)) {
                    "Good" -> good++
                    "Average" -> avg++
                    "Poor" -> { poor++; poorItems.add(label) }
                }
            }

            val statusText: String
            val statusColor: Int
            when {
                poor > 0 -> { statusText = "ATTENTION"; statusColor = red }
                avg > 0 -> { statusText = "FAIR"; statusColor = amber }
                else -> { statusText = "ALL GOOD"; statusColor = green }
            }

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(Color.parseColor("#F5F5F5"))
                    setStroke(dp(1), Color.parseColor("#DDDDDD"))
                }
                isClickable = true
                setOnClickListener { openReport(id) }
            }

            // Row 1: "#12 · date"  [STATUS]
            val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            header.addView(
                TextView(this).apply {
                    text = "#$id · ${r.optString("timestamp")}"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTextColor(Color.BLACK)
                    setTypeface(typeface, Typeface.BOLD)
                },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            header.addView(TextView(this).apply {
                text = statusText
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(statusColor)
                }
            })
            card.addView(header)

            // Row 2: counts
            card.addView(TextView(this).apply {
                text = "$good Good  ·  $avg Avg  ·  $poor Poor"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(Color.DKGRAY)
                setPadding(0, dp(2), 0, 0)
            })

            // Row 3: poor items (only when there are any)
            if (poorItems.isNotEmpty()) {
                card.addView(TextView(this).apply {
                    text = "Needs attention: ${poorItems.joinToString(", ")}"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTextColor(red)
                    setPadding(0, dp(2), 0, 0)
                })
            }

            container.addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(6) }
            )
        }

        val remaining = olderCount - shown
        if (remaining > 0) {
            container.addView(
                smallText("+ $remaining older report${if (remaining == 1) "" else "s"} (tap to view all)").apply {
                    setTextColor(Color.parseColor("#1565C0"))
                    setPadding(0, dp(6), 0, dp(6))
                    setOnClickListener {
                        startActivity(Intent(this@ExecutiveSummaryActivity, ReportListActivity::class.java))
                    }
                }
            )
        }
    }

    private fun smallText(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(2), 0, dp(2))
        }

    // Only the most recent non-blank comment (the layout caps it at 3 lines)
    private fun showLatestComment(reports: JSONArray) {
        val tv = findViewById<TextView>(R.id.tvSummaryComments)
        for (i in reports.length() - 1 downTo 0) {
            val r = reports.optJSONObject(i) ?: continue
            val comment = r.optString("comments").trim()
            if (comment.isNotEmpty()) {
                tv.text = "${r.optString("timestamp")} (#${r.optInt("id")}): $comment"
                return
            }
        }
        tv.text = "No comments"
    }
}