package com.furkandarakcilar.myapplication.ui

import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.furkandarakcilar.myapplication.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog to add a new Invoice with title, amount, due date and category.
 *
 * @param context Context
 * @param onAdd Callback invoked with (title, amount, dueDateMillis, category)
 */
class AddInvoiceDialog(
    private val context: Context,
    private val onAdd: (title: String, amount: Double, dueDate: Long, category: String) -> Unit
) {
    fun show() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_add_invoice, null)

        val etTitle   = view.findViewById<EditText>(R.id.etTitle)
        val etAmount  = view.findViewById<EditText>(R.id.etAmount)
        val tvDueDate = view.findViewById<TextView>(R.id.tvDueDate)
        val spinnerCategory   = view.findViewById<Spinner>(R.id.spinnerCategory)

        // Spinner’i kategori listesiyle doldur
        val categories = listOf("TELEFON", "SU", "DOGALGAZ", "ELEKTRIK", "DIGER")
        spinnerCategory.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            categories
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Tarih seçici ayarları
        val cal = Calendar.getInstance()
        var selectedDate = cal.timeInMillis
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        tvDueDate.text = fmt.format(cal.time)
        tvDueDate.setOnClickListener {
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    cal.set(y, m, d)
                    selectedDate = cal.timeInMillis
                    tvDueDate.text = fmt.format(cal.time)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        AlertDialog.Builder(context)
            .setTitle("Yeni Fatura Ekle")
            .setView(view)
            .setPositiveButton("Ekle") { _, _ ->
                val title    = etTitle.text.toString().trim()
                if (title.isEmpty()) return@setPositiveButton
                val amount   = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                val category = spinnerCategory.selectedItem as String
                onAdd(title, amount, selectedDate, category)
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
