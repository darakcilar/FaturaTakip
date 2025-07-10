package com.furkandarakcilar.myapplication.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.furkandarakcilar.myapplication.R
import com.furkandarakcilar.myapplication.data.Invoice
import com.google.android.material.appbar.MaterialToolbar
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.helper.StaticLabelsFormatter
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint

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
            numHorizontalLabels = labels.size
            labelFormatter = StaticLabelsFormatter(graph).apply {
                setHorizontalLabels(labels)
            }
        }
    }

    private fun drawCategoryChart(graph: GraphView, invoices: List<Invoice>) {
        val grouped = invoices
            .groupBy { it.category }
            .mapValues { it.value.sumOf { inv -> inv.amount.toDouble() } }

        val dataPoints = grouped.entries.mapIndexed { idx, (cat, total) ->
            DataPoint(idx.toDouble(), total)
        }.toTypedArray()

        val series = BarGraphSeries(dataPoints).apply {
            spacing = 30
            isDrawValuesOnTop = true
        }

        graph.removeAllSeries()
        graph.addSeries(series)

        graph.gridLabelRenderer.apply {
            numHorizontalLabels = grouped.size
            labelFormatter = StaticLabelsFormatter(graph).apply {
                setHorizontalLabels(grouped.keys.toTypedArray())
            }
            isVerticalLabelsVisible = false
        }
    }
}
