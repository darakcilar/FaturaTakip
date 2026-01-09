package com.faturatakip.faturatakip0.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.faturatakip.faturatakip0.R
import com.faturatakip.faturatakip0.data.Invoice
import com.faturatakip.faturatakip0.databinding.ActivityMainBinding
import com.faturatakip.faturatakip0.ui.dialogs.*
import com.faturatakip.faturatakip0.util.NotificationWorker
import com.faturatakip.faturatakip0.util.PdfHelper
import com.faturatakip.faturatakip0.util.formatCurrency
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var invoiceAdapter: InvoiceAdapter

    private var invoicesToExport: List<Invoice> = emptyList()
    private var exportTitle: String = "Fatura Raporu"

    private val createPdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { PdfHelper.createInvoicePdf(this, it, invoicesToExport, exportTitle) }
    }

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bundle = Bundle().apply {
                putString("name", result.data?.getStringExtra("SCAN_NAME"))
                putDouble("amount", result.data?.getDoubleExtra("SCAN_AMOUNT", 0.0) ?: 0.0)
                putString("date", result.data?.getStringExtra("SCAN_DATE"))
                putString("category", result.data?.getStringExtra("SCAN_CATEGORY"))
            }
            InvoiceDialogFragment.newInstance(null).apply { arguments = bundle }.show(supportFragmentManager, "ScanResult")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startScanner()
        } else {
            Toast.makeText(this, "Kamera izni olmadan fatura tarayamazsınız.", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        setupReminderWorker()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        checkNotificationPermission()
        setupRecyclerView()
        setupSortSpinner()
        setupEventListeners()
        observeViewModel()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                setupReminderWorker()
            }
        } else {
            setupReminderWorker()
        }
    }

    private fun setupReminderWorker() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "FaturaHatirlatici",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun setupRecyclerView() {
        invoiceAdapter = InvoiceAdapter(
            onEditClick = { showInvoiceDialog(it) },
            onDeleteClick = { viewModel.deleteInvoice(it) },
            onToggleStatusClick = { viewModel.toggleInvoiceStatus(it) },
            onGeneratePdfClick = { inv ->
                invoicesToExport = listOf(inv)
                exportTitle = "${inv.name} Faturası"
                createPdfLauncher.launch("${inv.name}.pdf")
            },
            onCategoryClick = { h -> viewModel.toggleCategory(h.category) },
            onCategoryPdfClick = { cat ->
                invoicesToExport = viewModel.allInvoices.value?.filter { it.category == cat } ?: emptyList()
                exportTitle = "$cat Kategorisi Raporu"
                createPdfLauncher.launch("${cat}_rapor.pdf")
            },
            onCategoryEditClick = { showRenameDialog(it) },
            onCategoryDeleteClick = { showCategoryDeleteConfirm(it) }
        )
        binding.invoiceRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.invoiceRecyclerView.adapter = invoiceAdapter
    }

    private fun setupSortSpinner() {
        ArrayAdapter.createFromResource(this, R.array.sort_options, android.R.layout.simple_spinner_item).also { adp ->
            adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.sortSpinner.adapter = adp
        }
        binding.sortSpinner.setSelection(1) // Tarih: Eski -> Yeni
        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                viewModel.setSort(MainViewModel.Sort.values()[pos])
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun setupEventListeners() {
        binding.addInvoiceFab.setOnClickListener { showInvoiceDialog(null) }
        binding.scanBillFab.setOnClickListener {
            checkCameraPermissionAndScan()
        }
        binding.analyticsButton.setOnClickListener { AnalyticsDialogFragment().show(supportFragmentManager, "Analiz") }
        binding.loanCalculatorButton.setOnClickListener { LoanCalculatorDialogFragment().show(supportFragmentManager, "Kredi") }

        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chip_paid -> MainViewModel.Filter.PAID
                R.id.chip_unpaid -> MainViewModel.Filter.UNPAID
                else -> MainViewModel.Filter.ALL
            }
            viewModel.setFilter(filter)
        }
    }

    private fun checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanner()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScanner() {
        scanLauncher.launch(Intent(this, TextScanActivity::class.java))
    }

    private fun showRenameDialog(oldName: String) {
        val input = EditText(this).apply { setText(oldName); setSelection(oldName.length) }
        AlertDialog.Builder(this).setTitle("Kategoriyi Düzenle").setView(input)
            .setPositiveButton("Güncelle") { _, _ -> viewModel.updateCategoryName(oldName, input.text.toString()) }
            .setNegativeButton("İptal", null).show()
    }

    private fun showCategoryDeleteConfirm(cat: String) {
        AlertDialog.Builder(this).setTitle("Kategoriyi Sil").setMessage("$cat ve tüm içeriği silinsin mi?")
            .setPositiveButton("Sil") { _, _ -> viewModel.deleteByCategory(cat) }
            .setNegativeButton("Vazgeç", null).show()
    }

    private fun observeViewModel() {
        viewModel.groupedInvoices.observe(this) { invoiceAdapter.submitList(it) }
        viewModel.totalUnpaid.observe(this) { binding.totalUnpaidTextView.text = "Borç: ${it.formatCurrency()}" }
    }

    private fun showInvoiceDialog(invoice: Invoice?) {
        InvoiceDialogFragment.newInstance(invoice).show(supportFragmentManager, "Dialog")
    }
}