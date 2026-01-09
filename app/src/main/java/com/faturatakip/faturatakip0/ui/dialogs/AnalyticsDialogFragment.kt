package com.faturatakip.faturatakip0.ui.dialogs

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.faturatakip.faturatakip0.databinding.DialogAnalyticsBinding
import com.faturatakip.faturatakip0.ui.MainViewModel
import com.faturatakip.faturatakip0.util.formatCurrency

class AnalyticsDialogFragment : DialogFragment() {

    private var _binding: DialogAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.closeAnalyticsBtn.setOnClickListener { dismiss() }

        viewModel.allInvoices.observe(viewLifecycleOwner) { invoices ->
            if (invoices.isNullOrEmpty()) {
                binding.customPieChart.setData(emptyMap())
                return@observe
            }

            val paidTotal = invoices.filter { it.isPaid }.sumOf { it.amount }
            val unpaidTotal = invoices.filter { !it.isPaid }.sumOf { it.amount }

            binding.totalCount.text = invoices.size.toString()
            binding.paidAmount.text = paidTotal.formatCurrency()
            binding.unpaidAmount.text = unpaidTotal.formatCurrency()

            val categoryMap = invoices.groupBy { it.category }
                .mapValues { it.value.sumOf { inv -> inv.amount }.toFloat() }

            binding.customPieChart.setData(categoryMap)
            setupLegend(categoryMap)
        }
    }

    private fun setupLegend(data: Map<String, Float>) {
        binding.legendContainer.removeAllViews()
        val total = data.values.sum()
        val colors = binding.customPieChart.colors

        data.entries.forEachIndexed { index, entry ->
            val percentage = (entry.value / total * 100).toInt()

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
                gravity = Gravity.CENTER_VERTICAL
            }

            val colorBox = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(40, 40).apply { setMargins(0, 0, 24, 0) }
                setBackgroundColor(colors[index % colors.size])
            }

            val label = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = entry.key
                setTextColor(Color.BLACK)
            }

            val value = TextView(context).apply {
                text = "%$percentage (${entry.value.toDouble().formatCurrency()})"
                setTypeface(null, Typeface.BOLD)
            }

            row.addView(colorBox)
            row.addView(label)
            row.addView(value)
            binding.legendContainer.addView(row)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}