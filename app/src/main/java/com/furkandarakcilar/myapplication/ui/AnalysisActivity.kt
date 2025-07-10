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
        val paid   = invoices.filter { it.isPaid  }.sumOf { it.amount.toDouble() }
        val unpaid = invoices.filter { !it.isPaid }.sumOf { it.amount.toDouble() }

        val series = BarGraphSeries(arrayOf(
            DataPoint(0.0, paid),
            DataPoint(1.0, unpaid)
        )).apply {
            spacing = 50
            isDrawValuesOnTop = true
        }

        graph.removeAllSeries()
        graph.addSeries(series)

        val labels = arrayOf("Ödenen", "Ödenmeyen")
        graph.gridLabelRenderer.apply {
            isHorizontalLabelsVisible = true
            isVerticalLabelsVisible   = true
            numHorizontalLabels = labels.size
            labelFormatter = StaticLabelsFormatter(graph).apply {
                setHorizontalLabels(labels)
            }
        }
    }

    private fun drawCategoryChart(graph: GraphView, invoices: List<Invoice>) {
        val legendCategory   = findViewById<LinearLayout>(R.id.legendCategory)
        val grouped = invoices
            .groupBy { it.category }
            .mapValues { it.value.sumOf { inv -> inv.amount.toDouble() } }

        // 2) Renk paleti
        val palette = listOf(
            Color.parseColor("#F44336"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#3F51B5"),
            Color.parseColor("#03A9F4"),
            Color.parseColor("#009688")
        )

        // 3) DataPoint’leri (0.5, 1.5, …) oluştur
        val points = grouped.entries
            .mapIndexed { idx, (_, total) -> DataPoint(idx + 0.5, total) }
            .toTypedArray()

        val count = points.size
        val maxY  = (grouped.values.maxOrNull() ?: 0.0) * 1.2

        graph.removeAllSeries()

        // 4) Yalnızca yatay grid çizgileri, renk set et
        graph.gridLabelRenderer.apply {
            gridStyle = GridLabelRenderer.GridStyle.HORIZONTAL
            setGridColor(Color.BLACK)
            setHorizontalLabelsVisible(false)
            setVerticalLabelsVisible(true)
            numVerticalLabels = 5
        }

        // 5) Sol eksen çizgisi (X = 0), kalınlık 4px
        LineGraphSeries(arrayOf(
            DataPoint(0.0,   0.0),
            DataPoint(0.0, maxY)
        )).apply {
            color     = Color.BLACK
            thickness = 4
        }.also { graph.addSeries(it) }


        // 7) Bar serisi – her bar farklı renk
        BarGraphSeries(points).apply {
            spacing           = 20
            isDrawValuesOnTop = true
            setValueDependentColor { p ->
                palette[p.x.toInt() % palette.size]
            }
        }.also { graph.addSeries(it) }

        // 8) Viewport sınırları
        graph.viewport.apply {
            isXAxisBoundsManual = true
            setMinX(0.0);    setMaxX(count.toDouble())
            isYAxisBoundsManual = true
            setMinY(0.0);    setMaxY(maxY)
        }

        // 9) Manuel legendCategory doldur
        legendCategory.removeAllViews()
        grouped.keys.forEachIndexed { idx, cat ->
            // renk kutucuğu
            val sizePx = (16 * resources.displayMetrics.density).toInt()
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
                    .also { it.setMargins(0, 0, sizePx/2, 0) }
                setBackgroundColor(palette[idx % palette.size])
            }.also { legendCategory.addView(it) }

            // kategori etiketi
            TextView(this).apply {
                text             = cat
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.DKGRAY)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 16, 0) }
            }.also { legendCategory.addView(it) }
        }
    }
}
