package com.faturatakip.faturatakip0.ui.dialogs

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
import com.faturatakip.faturatakip0.data.Invoice
import com.faturatakip.faturatakip0.databinding.DialogAddEditInvoiceBinding
import com.faturatakip.faturatakip0.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class InvoiceDialogFragment : DialogFragment() {

    private var _binding: DialogAddEditInvoiceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private var existingInvoice: Invoice? = null

    private val defaultCategories = listOf("Su", "Doğalgaz", "Elektrik", "İnternet", "Kira", "Market", "Diğer")

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.92).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddEditInvoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        existingInvoice = arguments?.getParcelable(ARG_INVOICE)

        setupUI()
        setupCategoryAutocomplete()
        setupListeners()
    }

    private fun setupUI() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        if (existingInvoice != null) {
            binding.dialogTitle.text = "Faturayı Düzenle"
            binding.invoiceNameInput.setText(existingInvoice?.name)
            binding.invoiceAmountInput.setText(existingInvoice?.amount.toString())
            binding.categoryInput.setText(existingInvoice?.category)
            binding.dueDateInput.setText(sdf.format(Date(existingInvoice!!.dueDate)))
            binding.saveButton.text = "Güncelle"
        } else {
            arguments?.let { b ->
                binding.invoiceNameInput.setText(b.getString("name", ""))
                val amt = b.getDouble("amount", 0.0)
                if (amt > 0) binding.invoiceAmountInput.setText(amt.toString())
                binding.dueDateInput.setText(b.getString("date", ""))
                binding.categoryInput.setText(b.getString("category", "Diğer"))
            }
        }
    }

    private fun setupCategoryAutocomplete() {
        viewModel.allInvoices.observe(viewLifecycleOwner) { invoices ->
            val dbCategories = invoices?.map { it.category }?.distinct() ?: emptyList()

            val combinedCategories = (defaultCategories + dbCategories).distinct().sorted()

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                combinedCategories
            )
            binding.categoryInput.setAdapter(adapter)

            binding.categoryInput.setOnTouchListener { _, _ ->
                binding.categoryInput.showDropDown()
                false
            }
        }
    }

    private fun setupListeners() {
        binding.dueDateInput.setOnClickListener { showDatePicker() }
        binding.saveButton.setOnClickListener { validateAndSave() }
        binding.cancelButton.setOnClickListener { dismiss() }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        if (existingInvoice != null) c.timeInMillis = existingInvoice!!.dueDate

        DatePickerDialog(requireContext(), { _, y, m, d ->
            val sel = Calendar.getInstance().apply { set(y, m, d) }
            binding.dueDateInput.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(sel.time))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun validateAndSave() {
        val name = binding.invoiceNameInput.text.toString().trim()
        val amt = binding.invoiceAmountInput.text.toString().toDoubleOrNull() ?: 0.0
        val cat = binding.categoryInput.text.toString().trim().ifEmpty { "Diğer" }
        val dateStr = binding.dueDateInput.text.toString()

        if (name.isEmpty() || amt <= 0 || dateStr.isEmpty()) {
            Toast.makeText(context, "Lütfen geçerli bilgiler girin!", Toast.LENGTH_SHORT).show()
            return
        }

        val date = try { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr)?.time ?: System.currentTimeMillis() }
        catch (e: Exception) { System.currentTimeMillis() }

        if (existingInvoice == null) {
            viewModel.insert(Invoice(name = name, amount = amt, dueDate = date, category = cat))
        } else {
            viewModel.update(existingInvoice!!.copy(name = name, amount = amt, dueDate = date, category = cat))
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_INVOICE = "arg_invoice"
        fun newInstance(inv: Invoice?) = InvoiceDialogFragment().apply {
            arguments = bundleOf(ARG_INVOICE to inv)
        }
    }
}