package com.furkandarakcilar.myapplication.ui

import android.R.attr.numColumns
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.furkandarakcilar.myapplication.R
import com.furkandarakcilar.myapplication.data.Invoice
import com.google.android.material.appbar.MaterialToolbar
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.LegendRenderer
import com.jjoe64.graphview.helper.StaticLabelsFormatter
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlin.math.roundToInt

class AnalysisActivity : AppCompatActivity() {

    private val viewModel: InvoiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.analysis_activity)

        // Toolbar & Up ok tuşu
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_analysis)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(Color.WHITE)
        toolbar.setNavigationOnClickListener {
            goMain()
        }

        // View’ları bağla
        val tvPaid          = findViewById<TextView>(R.id.tvTotalPaid)
        val tvUnpaid        = findViewById<TextView>(R.id.tvTotalUnpaid)
        val graphPaidUnpaid = findViewById<GraphView>(R.id.graphPaidUnpaid)
        val graphCategory   = findViewById<GraphView>(R.id.graphCategory)

        // Veriyi dinle
        viewModel.allInvoices.observe(this) { invoices ->
            updateSummary(tvPaid, tvUnpaid, invoices)
            drawPaidUnpaidChart(graphPaidUnpaid, invoices)
            drawCategoryChart(graphCategory, invoices)
        }
    }

    // goMain: MainActivity’ye dön ve animasyon uygula
    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
        finish()
    }

    // Donanım “up” tuşuna basıldığında da goMain() çalışsın
    override fun onSupportNavigateUp(): Boolean {
        goMain()
        return true
    }

    // Cihaz “geri” tuşuna basıldığında da goMain() çalışsın
    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        super.onBackPressed()
        goMain()
    }

    private fun updateSummary(
        tvPaid: TextView,
        tvUnpaid: TextView,
        invoices: List<Invoice>
    ) {
        val totalPaid: Double = invoices
            .filter { it.isPaid }
            .sumOf { it.amount.toDouble() }

        val totalUnpaid: Double = invoices
            .filter { !it.isPaid }
            .sumOf { it.amount.toDouble() }

        tvPaid.text   = "Ödenmiş: ₺%,.2f".format(totalPaid)
        tvUnpaid.text = "Ödenmemiş: ₺%,.2f".format(totalUnpaid)
    }
    private fun drawPaidUnpaidChart(graph: GraphView, invoices: List<Invoice>) {
        val paid = invoices.filter { it.isPaid }.sumOf { it.amount.toDouble() }
        val unpaid = invoices.filter { !it.isPaid }.sumOf { it.amount.toDouble() }

        val series = BarGraphSeries(arrayOf(
            DataPoint(0.0, paid),
            DataPoint(1.0, unpaid)
        )).apply {
            spacing = 50
            isDrawValuesOnTop = true
            setOnDataPointTapListener { _, dataPoint ->
                Toast.makeText(
                    graph.context,
                    if (dataPoint.x == 0.0) "Ödenen: ${dataPoint.y.toInt()}" else "Ödenmeyen: ${dataPoint.y.toInt()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        graph.apply {
            removeAllSeries()
            addSeries(series)
            gridLabelRenderer.apply {
                isHorizontalLabelsVisible = true
                isVerticalLabelsVisible = true
                numHorizontalLabels = 2
                labelFormatter = StaticLabelsFormatter(graph).apply {
                    setHorizontalLabels(arrayOf("Ödenen", "Ödenmeyen"))
                }
            }
        }
    }

    private fun drawCategoryChart(graph: GraphView, invoices: List<Invoice>) {
        val legendCategory = findViewById<LinearLayout>(R.id.legendCategory)

        val grouped = invoices.groupBy { it.category }.mapValues { group ->
            group.value.sumOf { it.amount.toDouble() }
        }

        val palette = generateDynamicPalette(grouped.size)

        val points = grouped.entries.mapIndexed { index, entry ->
            DataPoint(index + 0.5, entry.value)
        }.toTypedArray()

        val maxY = (grouped.values.maxOrNull() ?: 0.0) * 1.2

        graph.apply {
            removeAllSeries()

            addSeries(LineGraphSeries(arrayOf(
                DataPoint(0.0, 0.0),
                DataPoint(0.0, maxY)
            )).apply {
                color = Color.BLACK
                thickness = 4
            })

            addSeries(BarGraphSeries(points).apply {
                spacing = 20
                isDrawValuesOnTop = true
                setValueDependentColor { dp ->
                    palette[dp.x.toInt() % palette.size]
                }
                setOnDataPointTapListener { _, dataPoint ->
                    val category = grouped.keys.elementAt(dataPoint.x.toInt())
                    Toast.makeText(
                        graph.context,
                        "$category: ${dataPoint.y.toInt()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

            gridLabelRenderer.apply {
                gridStyle = GridLabelRenderer.GridStyle.HORIZONTAL
                setGridColor(Color.BLACK)
                setHorizontalLabelsVisible(false)
                setVerticalLabelsVisible(true)
                numVerticalLabels = 5
            }

            viewport.apply {
                isXAxisBoundsManual = true
                setMinX(0.0)
                setMaxX(points.size.toDouble())
                isYAxisBoundsManual = true
                setMinY(0.0)
                setMaxY(maxY)
            }
        }

        legendCategory.removeAllViews()
        grouped.keys.forEachIndexed { index, category ->
            val colorView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (16 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt()
                ).apply { setMargins(0, 0, 8, 0) }
                setBackgroundColor(palette[index % palette.size])
            }

            val labelView = TextView(this).apply {
                text = category
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.DKGRAY)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 16, 0) }
            }

            legendCategory.addView(colorView)
            legendCategory.addView(labelView)
        }
    }

    private fun generateDynamicPalette(size: Int): List<Int> {
        val baseColors = listOf(
            Color.parseColor("#F44336"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#3F51B5"),
            Color.parseColor("#03A9F4"),
            Color.parseColor("#009688"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#795548"),
            Color.parseColor("#607D8B")
        )
        return List(size) { index -> baseColors[index % baseColors.size] }
    }


}
