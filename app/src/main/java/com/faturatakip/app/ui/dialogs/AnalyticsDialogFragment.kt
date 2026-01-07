package com.faturatakip.app.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.faturatakip.app.databinding.DialogAnalyticsBinding
import com.faturatakip.app.ui.ListItem
import com.faturatakip.app.ui.MainViewModel
import com.faturatakip.app.util.formatCurrency

class AnalyticsDialogFragment : DialogFragment() {

    private var _binding: DialogAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEventListeners()
        observeViewModel()
    }

    private fun setupEventListeners() {
        binding.closeAnalyticsBtn.setOnClickListener {
            dismiss()
        }
    }

    private fun observeViewModel() {
        // HATA DÜZELTMESİ: 'groupedInvoices' dinleniyor ve içinden faturalar ayıklanıyor.
        viewModel.groupedInvoices.observe(viewLifecycleOwner) { items ->
            val invoices = items.filterIsInstance<ListItem.InvoiceItem>().map { it.invoice }

            val paidInvoices = invoices.filter { it.isPaid }
            val unpaidInvoices = invoices.filter { !it.isPaid }

            val totalAmount = invoices.sumOf { it.amount }
            val paidAmount = paidInvoices.sumOf { it.amount }

            binding.totalCount.text = invoices.size.toString()
            binding.paidCount.text = paidInvoices.size.toString()
            binding.unpaidCount.text = unpaidInvoices.size.toString()
            binding.totalAmount.text = totalAmount.formatCurrency()
            binding.paidAmount.text = paidAmount.formatCurrency()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

