package com.faturatakip.faturatakip0.ui.dialogs

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.faturatakip.faturatakip0.data.Invoice
import com.faturatakip.faturatakip0.databinding.DialogLoanCalculatorBinding
import com.faturatakip.faturatakip0.ui.MainViewModel
import com.faturatakip.faturatakip0.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class LoanCalculatorDialogFragment : DialogFragment() {

    private var _binding: DialogLoanCalculatorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private var monthlyPaymentValue: Double = 0.0
    private var termInMonthsValue: Int = 0

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
        _binding = DialogLoanCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.loanAmountInput.doAfterTextChanged { calculateLoan() }
        binding.annualInterestRateInput.doAfterTextChanged { calculateLoan() }
        binding.loanTermInput.doAfterTextChanged { calculateLoan() }

        binding.firstPaymentDateInput.setOnClickListener {
            showDatePicker()
        }

        binding.createInstallmentsButton.setOnClickListener {
            createInstallmentsAsInvoices()
        }
    }

    private fun calculateLoan() {
        val loanAmountStr = binding.loanAmountInput.text.toString()
        val interestRateStr = binding.annualInterestRateInput.text.toString()
        val termInMonthsStr = binding.loanTermInput.text.toString()

        val p = loanAmountStr.toDoubleOrNull()
        val annualRate = interestRateStr.toDoubleOrNull()
        val n = termInMonthsStr.toIntOrNull()

        if (p != null && annualRate != null && n != null && p > 0 && annualRate > 0 && n > 0) {
            val r = (annualRate / 100) / 12
            val m = p * (r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1)
            val totalRepayment = m * n
            val totalInterest = totalRepayment - p

            monthlyPaymentValue = m
            termInMonthsValue = n

            binding.resultsGroup.visibility = View.VISIBLE
            binding.createInstallmentsGroup.visibility = View.VISIBLE
            binding.monthlyPayment.text = m.formatCurrency()
            binding.totalRepayment.text = totalRepayment.formatCurrency()
            binding.totalInterest.text = totalInterest.formatCurrency()
        } else {
            binding.resultsGroup.visibility = View.GONE
            binding.createInstallmentsGroup.visibility = View.GONE
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.firstPaymentDateInput.setText(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun createInstallmentsAsInvoices() {
        val title = binding.installmentTitleInput.text.toString().trim()
        val dateStr = binding.firstPaymentDateInput.text.toString().trim()

        if (title.isEmpty() || dateStr.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen kredi başlığı ve ilk ödeme tarihini girin.", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val firstPaymentDate: Date? = try {
            dateFormat.parse(dateStr)
        } catch (e: Exception) {
            Toast.makeText(context, "Geçersiz tarih formatı (GG/AA/YYYY)", Toast.LENGTH_SHORT).show()
            return
        }

        if (firstPaymentDate == null) return

        val calendar = Calendar.getInstance().apply { time = firstPaymentDate }
        val installments = mutableListOf<Invoice>()

        for (i in 1..termInMonthsValue) {
            val invoice = Invoice(
                name = "$i. Taksit",
                amount = monthlyPaymentValue,
                dueDate = calendar.timeInMillis,
                category = title.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                isPaid = false
            )
            installments.add(invoice)
            calendar.add(Calendar.MONTH, 1)
        }

        viewModel.insertAll(installments)
        Toast.makeText(requireContext(), "$termInMonthsValue adet taksit '$title' kategorisine eklendi.", Toast.LENGTH_LONG).show()
        dismiss()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

