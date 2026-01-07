package com.faturatakip.app.ui.dialogs

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.faturatakip.app.R
import com.faturatakip.app.data.Invoice
import com.faturatakip.app.databinding.DialogAddEditInvoiceBinding
import com.faturatakip.app.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class InvoiceDialogFragment : DialogFragment() {

    private var _binding: DialogAddEditInvoiceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var existingInvoice: Invoice? = null

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
        _binding = DialogAddEditInvoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        existingInvoice = arguments?.getParcelable(ARG_INVOICE)
        setupUI()
        setupEventListeners()
        observeCategories()
    }

    // Bu fonksiyon, mevcut kategorileri ViewModel'den alıp öneri listesini oluşturur.
    private fun observeCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
            binding.categoryInput.setAdapter(adapter)
        }
    }

    private fun setupUI() {
        if (existingInvoice != null) {
            binding.dialogTitle.text = getString(R.string.dialog_title_edit)
            binding.categoryInput.setText(existingInvoice?.category)
            binding.invoiceNameInput.setText(existingInvoice?.name)
            binding.invoiceAmountInput.setText(existingInvoice?.amount.toString())

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.dueDateInput.setText(dateFormat.format(Date(existingInvoice!!.dueDate)))

            binding.saveButton.text = getString(R.string.button_update)
        } else {
            binding.dialogTitle.text = getString(R.string.dialog_title_add)
            binding.saveButton.text = getString(R.string.button_save)

            arguments?.let {
                val scannedAmount = it.getDouble("amount", 0.0)
                val scannedDate = it.getString("date")
                if (scannedAmount > 0.0) {
                    binding.invoiceAmountInput.setText(scannedAmount.toString())
                }
                if (scannedDate != null) {
                    binding.dueDateInput.setText(scannedDate)
                }
            }
        }
    }

    private fun setupEventListeners() {
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.saveButton.setOnClickListener { saveOrUpdateInvoice() }
        binding.dueDateInput.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dateStr = binding.dueDateInput.text.toString()
        if (dateStr.isNotEmpty()) {
            try {
                calendar.time = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr) ?: Date()
            } catch (e: Exception) { /* Hata varsa bugünün tarihini kullan */ }
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.dueDateInput.setText(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun saveOrUpdateInvoice() {
        // İYİLEŞTİRME: Kategori ismini alırken ilk harfini büyük yap.
        val category = binding.categoryInput.text.toString().trim().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val name = binding.invoiceNameInput.text.toString().trim()
        val amountStr = binding.invoiceAmountInput.text.toString().trim()
        val dateStr = binding.dueDateInput.text.toString().trim()

        if (category.isEmpty() || name.isEmpty() || amountStr.isEmpty() || dateStr.isEmpty()) {
            Toast.makeText(context, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(context, "Geçersiz tutar girdiniz", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dueDate: Date? = try {
            dateFormat.parse(dateStr)
        } catch (e: Exception) {
            Toast.makeText(context, "Geçersiz tarih formatı (GG/AA/YYYY)", Toast.LENGTH_SHORT).show()
            return
        }
        if (dueDate == null) return
        val dueDateAsLong = dueDate.time

        if (existingInvoice == null) {
            val newInvoice = Invoice(
                name = name,
                amount = amount,
                dueDate = dueDateAsLong,
                category = category,
                isPaid = false
            )
            viewModel.insert(newInvoice)
        } else {
            val updatedInvoice = existingInvoice!!.copy(
                name = name,
                amount = amount,
                dueDate = dueDateAsLong,
                category = category
            )
            viewModel.update(updatedInvoice)
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_INVOICE = "arg_invoice"

        fun newInstance(invoice: Invoice?): InvoiceDialogFragment {
            return InvoiceDialogFragment().apply {
                arguments = bundleOf(ARG_INVOICE to invoice)
            }
        }
    }
}

