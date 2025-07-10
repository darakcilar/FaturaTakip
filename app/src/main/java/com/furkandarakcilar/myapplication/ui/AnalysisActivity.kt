package com.furkandarakcilar.myapplication.ui

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

        // Toolbar ve "Up" butonu
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_analysis)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // View'ları bağla
        val tvPaid          = findViewById<TextView>(R.id.tvTotalPaid)
        val tvUnpaid        = findViewById<TextView>(R.id.tvTotalUnpaid)
        val graphPaidUnpaid = findViewById<GraphView>(R.id.graphPaidUnpaid)
        val graphCategory   = findViewById<GraphView>(R.id.graphCategory)

        // Fatura verisini dinle
        viewModel.allInvoices.observe(this) { invoices ->
            updateSummary(tvPaid, tvUnpaid, invoices)
            drawPaidUnpaidChart(graphPaidUnpaid, invoices)
            drawCategoryChart(graphCategory, invoices)
        }
    }

    private fun updateSummary(
        tvPaid: TextView,
        tvUnpaid: TextView,
        invoices: List<Invoice>
    ) {
        // sumOf ambiguity kaldırıldı, Double dönüşümü kullanılıyor
        val totalPaid: Double = invoices
            .filter { it.isPaid }
            .sumOf   { it.amount.toDouble() }

        val totalUnpaid: Double = invoices
            .filter { !it.isPaid }
            .sumOf   { it.amount.toDouble() }

        tvPaid.text   = "Ödenmiş: ₺%,.2f".format(totalPaid)
        tvUnpaid.text = "Ödenmemiş: ₺%,.2f".format(totalUnpaid)
    }

    private fun drawPaidUnpaidChart(graph: GraphView, invoices: List<Invoice>) {
        val paid   = invoices.filter { it.isPaid  }.sumOf { it.amount.toDouble() }
        val unpaid = invoices.filter { !it.isPaid }.sumOf { it.amount.toDouble() }

        val series = BarGraphSeries(arrayOf(
            DataPoint(0.0, paid),
            DataPoint(1.0, unpaid)
        ))
        series.spacing = 50
        series.isDrawValuesOnTop = true
        // default paint size/color yeterli

        graph.removeAllSeries()
        graph.addSeries(series)

        // X ekseni etiketlerini ayarla
        val labels = arrayOf("Ödenen", "Ödenmeyen")
        graph.gridLabelRenderer.labelFormatter = StaticLabelsFormatter(graph).apply {
            setHorizontalLabels(labels)
        }
        graph.gridLabelRenderer.numHorizontalLabels = labels.size
    }

    private fun drawCategoryChart(graph: GraphView, invoices: List<Invoice>) {
        // Kategori bazlı toplam tutar
        val grouped = invoices
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() } }

        val dataPoints = grouped.entries.mapIndexed { idx, (cat, total) ->
            DataPoint(idx.toDouble(), total)
        }.toTypedArray()

        val series = BarGraphSeries(dataPoints)
        series.spacing = 30
        series.isDrawValuesOnTop = true

        graph.removeAllSeries()
        graph.addSeries(series)

        // Kategori etiketleri
        graph.gridLabelRenderer.labelFormatter = StaticLabelsFormatter(graph).apply {
            setHorizontalLabels(grouped.keys.toTypedArray())
        }
        graph.gridLabelRenderer.numHorizontalLabels = grouped.size
        graph.gridLabelRenderer.isVerticalLabelsVisible = false
    }
}
