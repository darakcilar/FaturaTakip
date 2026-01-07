package com.faturatakip.app.ui

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.faturatakip.app.R
import com.faturatakip.app.data.Invoice
import com.faturatakip.app.databinding.ActivityMainBinding
import com.faturatakip.app.databinding.DialogRenameCategoryBinding
import com.faturatakip.app.ui.dialogs.AnalyticsDialogFragment
import com.faturatakip.app.ui.dialogs.InvoiceDialogFragment
import com.faturatakip.app.ui.dialogs.LoanCalculatorDialogFragment
import com.faturatakip.app.util.formatCurrency
import com.faturatakip.app.util.formatDate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itextpdf.kernel.colors.DeviceGray
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var invoiceAdapter: InvoiceAdapter

    private var pendingInvoicesForPdf: List<Invoice>? = null
    private var pendingPdfTitle: String? = null

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startSmartScan()
            } else {
                Toast.makeText(this, "Fatura okutmak için kamera izni gerekli.", Toast.LENGTH_LONG).show()
            }
        }

    private val scanActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val bundle = Bundle().apply {
                    putString("name", data?.getStringExtra("SCAN_NAME"))
                    putDouble("amount", data?.getDoubleExtra("SCAN_AMOUNT", 0.0) ?: 0.0)
                    putString("date", data?.getStringExtra("SCAN_DATE"))
                    putString("category", data?.getStringExtra("SCAN_CATEGORY"))
                }
                val dialog = InvoiceDialogFragment.newInstance(null)
                dialog.arguments = (dialog.arguments ?: Bundle()).apply { putAll(bundle) }
                dialog.show(supportFragmentManager, "InvoiceDialog")
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setupRecyclerView()
        setupEventListeners()
        observeViewModel()
    }

    private fun startSmartScan() {
        // Barkod seçeneği kaldırıldı, doğrudan Akıllı OCR'ı başlatıyoruz
        scanActivityResultLauncher.launch(Intent(this, TextScanActivity::class.java))
    }

    private fun checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startSmartScan()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupRecyclerView() {
        invoiceAdapter = InvoiceAdapter(
            onEditClick = { showInvoiceDialog(it) },
            onDeleteClick = { showDeleteConfirmation(it) },
            onToggleStatusClick = { viewModel.toggleInvoiceStatus(it) },
            onGeneratePdfClick = { generatePdfForInvoice(it) },
            onCategoryClick = { headerItem -> viewModel.toggleCategory(headerItem.category) },
            onCategoryPdfClick = { generatePdfForCategory(it) },
            onCategoryEditClick = { showRenameCategoryDialog(it) },
            onCategoryDeleteClick = { showDeleteCategoryConfirmation(it) }
        )

        binding.invoiceRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = invoiceAdapter
            (itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun setupEventListeners() {
        binding.addInvoiceFab.setOnClickListener { showInvoiceDialog(null) }
        binding.scanBillFab.setOnClickListener { checkCameraPermissionAndScan() }
        binding.analyticsButton.setOnClickListener {
            AnalyticsDialogFragment().show(supportFragmentManager, "AnalyticsDialog")
        }
        binding.loanCalculatorButton.setOnClickListener {
            LoanCalculatorDialogFragment().show(supportFragmentManager, "LoanCalculatorDialog")
        }

        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chip_paid -> MainViewModel.Filter.PAID
                R.id.chip_unpaid -> MainViewModel.Filter.UNPAID
                else -> MainViewModel.Filter.ALL
            }
            viewModel.setFilter(filter)
        }

        setupSortSpinner()
    }

    private fun setupSortSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.sort_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.sortSpinner.adapter = adapter
            binding.sortSpinner.setSelection(1, false)
        }

        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val sort = MainViewModel.Sort.values()[position]
                viewModel.setSort(sort)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeViewModel() {
        viewModel.groupedInvoices.observe(this) { items ->
            invoiceAdapter.submitList(items)
            binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.totalUnpaid.observe(this) { total ->
            binding.totalUnpaidTextView.text = "Toplam Ödenecek: ${total.formatCurrency()}"
        }
    }

    private fun showInvoiceDialog(invoice: Invoice?) {
        val dialog = InvoiceDialogFragment.newInstance(invoice)
        dialog.show(supportFragmentManager, "InvoiceDialog")
    }

    private fun showDeleteConfirmation(invoice: Invoice) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_delete_invoice)
            .setMessage(R.string.delete_confirmation)
            .setNegativeButton(R.string.button_cancel, null)
            .setPositiveButton(R.string.button_delete) { _, _ ->
                viewModel.deleteInvoice(invoice)
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun generatePdfForInvoice(invoice: Invoice) { generatePdf(listOf(invoice), "Fatura Detayı") }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun generatePdfForCategory(category: String) {
        val invoicesForCategory = viewModel.allInvoices.value?.filter { it.category == category }
        if (invoicesForCategory.isNullOrEmpty()) {
            Toast.makeText(this, "Kategoride fatura bulunmuyor.", Toast.LENGTH_SHORT).show()
            return
        }
        generatePdf(invoicesForCategory, "$category Faturaları Raporu")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun generatePdf(invoices: List<Invoice>, title: String) {
        pendingInvoicesForPdf = invoices
        pendingPdfTitle = title
        createAndSavePdf(invoices, title)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createAndSavePdf(invoices: List<Invoice>, title: String) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${title.replace(" ", "_")}_$timeStamp.pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri == null) {
            Toast.makeText(this, "PDF oluşturulamadı.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            resolver.openOutputStream(uri).use { outputStream ->
                if(outputStream == null) throw IOException("OutputStream oluşturulamadı.")

                val writer = PdfWriter(outputStream)
                val pdf = PdfDocument(writer)
                val document = Document(pdf)

                document.add(Paragraph(getString(R.string.app_name))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10f))

                document.add(Paragraph(title)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold().setFontSize(20f).setMarginBottom(20f))

                val table = Table(UnitValue.createPercentArray(floatArrayOf(3f, 2f, 2f, 2f))).useAllAvailableWidth()
                table.addHeaderCell(Paragraph("Fatura Adı").setBold())
                table.addHeaderCell(Paragraph("Son Tarih").setBold())
                table.addHeaderCell(Paragraph("Tutar").setBold())
                table.addHeaderCell(Paragraph("Durum").setBold())

                var totalAmount = 0.0
                invoices.sortedBy { it.dueDate }.forEach { invoice ->
                    table.addCell(invoice.name)
                    table.addCell(invoice.dueDate.formatDate())
                    table.addCell(invoice.amount.formatCurrency())
                    table.addCell(if (invoice.isPaid) "Ödendi" else "Ödenmedi")
                    if(!invoice.isPaid) totalAmount += invoice.amount
                }
                document.add(table)

                document.add(Paragraph("\nÖzet Bilgiler").setBold().setFontSize(14f))
                document.add(Paragraph("Toplam Fatura Sayısı: ${invoices.size}"))
                document.add(Paragraph("Listelenen Ödenmemiş Toplam Tutar: ${totalAmount.formatCurrency()}"))
                document.add(Paragraph("Rapor Tarihi: ${System.currentTimeMillis().formatDate()}"))

                document.close()
                Toast.makeText(this, "PDF İndirilenler klasörüne kaydedildi.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDeleteCategoryConfirmation(category: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("$category Kategorisini Sil")
            .setMessage("Kategoriyi ve içindeki faturaları silmek istediğinizden emin misiniz?")
            .setNegativeButton("İptal", null)
            .setPositiveButton("Sil") { _, _ ->
                viewModel.deleteCategory(category)
                Toast.makeText(this, "'$category' silindi.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showRenameCategoryDialog(oldCategory: String) {
        val dialogBinding = DialogRenameCategoryBinding.inflate(LayoutInflater.from(this))
        dialogBinding.newCategoryNameInput.setText(oldCategory)

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Kaydet") { _, _ ->
                val newCategory = dialogBinding.newCategoryNameInput.text.toString().trim()
                if (newCategory.isNotEmpty() && newCategory != oldCategory) {
                    viewModel.updateCategoryName(oldCategory, newCategory)
                }
            }
            .show()
    }
}