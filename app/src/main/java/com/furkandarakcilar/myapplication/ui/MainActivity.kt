package com.furkandarakcilar.myapplication.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.furkandarakcilar.myapplication.R
import com.furkandarakcilar.myapplication.data.Invoice
import com.furkandarakcilar.myapplication.util.Prefs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity(), ActionMode.Callback {

    private val viewModel: InvoiceViewModel by viewModels()
    private lateinit var adapter: InvoiceAdapter
    private var actionMode: ActionMode? = null

    private var rawList: List<Invoice> = emptyList()
    private var sortType: SortType = SortType.DUE_DESC
    private var filterType: FilterType = FilterType.ALL

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        if (!Prefs.isLoggedIn(this)) {
            goLogin()
            return
        }

        setContentView(R.layout.activity_main)
        setupToolbar()       // ← Toolbar’ı tanımlayıp Up oku ekliyoruz
        applyWindowInsets()
        setupAdapter()
        setupFab()
        setupMenuButton()

        viewModel.allInvoices.observe(this) { list ->
            rawList = list
            applyFilterAndSort()
        }
    }

    private fun setupToolbar() {
        findViewById<MaterialToolbar>(R.id.toolbar).also { toolbar ->
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)

            // Up okuna tıklandığında Login ekranına dön
            toolbar.setNavigationOnClickListener {
                goLogin()
            }
        }
    }

    // Donanım veya sistem “up” tuşuna basıldığında da Login’e dönsün
    override fun onSupportNavigateUp(): Boolean {
        goLogin()
        return true
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, sys.top, 0, sys.bottom)
            insets
        }
    }

    private fun setupAdapter() {
        adapter = InvoiceAdapter(
            onClick = { inv -> handleInvoiceClick(inv) },
            onLongClick = { inv ->
                if (actionMode == null) actionMode = startSupportActionMode(this)
                adapter.toggleSelection(inv)
                updateActionTitle()
                true
            }
        )
        findViewById<RecyclerView>(R.id.invoiceRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun handleInvoiceClick(inv: Invoice) {
        if (actionMode != null) {
            adapter.toggleSelection(inv)
            updateActionTitle()
        } else {
            showPaymentConfirmation(inv)
        }
    }

    private fun showPaymentConfirmation(inv: Invoice) {
        val currentlyPaid = inv.isPaid
        val message = if (currentlyPaid)
            "Bu faturayı ödenmemiş olarak işaretlemek istediğinize emin misiniz?"
        else
            "Bu faturayı ödenmiş olarak işaretlemek istediğinize emin misiniz?"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(inv.title)
            .setMessage(message)
            .setNegativeButton("Hayır", null)
            .setPositiveButton("Evet") { _, _ ->
                viewModel.update(inv.copy(isPaid = !currentlyPaid))
            }
            .show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.colorSecondary))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.colorSecondary))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fabAdd)
            .setOnClickListener {
                AddInvoiceDialog(this) { title, amount, dueDate, category ->
                    viewModel.insert(title, amount, dueDate, category)
                }.show()
            }
    }

    private fun setupMenuButton() {
        findViewById<Button>(R.id.buttonBack).setOnClickListener {
            startActivity(Intent(this, AnalysisActivity::class.java))
        }
    }

    private fun applyFilterAndSort() {
        val now = System.currentTimeMillis()
        val filtered = when (filterType) {
            FilterType.ALL     -> rawList
            FilterType.PAID    -> rawList.filter { it.isPaid }
            FilterType.UNPAID  -> rawList.filter { !it.isPaid && it.dueDate >= now }
            FilterType.OVERDUE -> rawList.filter { !it.isPaid && it.dueDate < now }
        }
        val sorted = when (sortType) {
            SortType.DUE_ASC     -> filtered.sortedBy { it.dueDate }
            SortType.DUE_DESC    -> filtered.sortedByDescending { it.dueDate }
            SortType.AMOUNT_ASC  -> filtered.sortedBy { it.amount }
            SortType.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
        }
        adapter.submitInvoices(sorted)
    }

    private fun updateActionTitle() {
        val count = adapter.getSelectedItems().size
        if (count == 0) actionMode?.finish()
        else actionMode?.title = "$count seçili"
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        super.onBackPressed()
        actionMode?.let {
            it.finish()
            return
        }
        goLogin()
    }

    private fun goLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
        finish()
    }

    // --- ActionMode.Callback ---
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_selection, menu)
        return true
    }
    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
        if (item.itemId == R.id.action_delete) {
            adapter.getSelectedItems().forEach { viewModel.delete(it) }
            mode.finish()
            true
        } else false
    override fun onDestroyActionMode(mode: ActionMode) {
        adapter.clearSelection()
        actionMode = null
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_sort, menu)

        menu.findItem(R.id.menu_sort).subMenu
            ?.setGroupCheckable(R.id.group_sort, true, true)
        menu.findItem(R.id.menu_filter).subMenu
            ?.setGroupCheckable(R.id.group_filter, true, true)

        menu.findItem(R.id.action_sort_due_asc).isChecked = true
        menu.findItem(R.id.action_filter_all).isChecked  = true

        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // --- SIRALAMA SEÇENEKLERİ ---
            R.id.action_sort_due_asc -> {
                sortType = SortType.DUE_ASC
                item.isChecked = true
            }
            R.id.action_sort_due_desc -> {
                sortType = SortType.DUE_DESC
                item.isChecked = true
            }
            R.id.action_sort_amount_asc -> {
                sortType = SortType.AMOUNT_ASC
                item.isChecked = true
            }
            R.id.action_sort_amount_desc -> {
                sortType = SortType.AMOUNT_DESC
                item.isChecked = true
            }

            // --- FİLTRE SEÇENEKLERİ ---
            R.id.action_filter_all -> {
                filterType = FilterType.ALL
                item.isChecked = true
            }
            R.id.action_filter_paid -> {
                filterType = FilterType.PAID
                item.isChecked = true
            }
            R.id.action_filter_unpaid -> {
                filterType = FilterType.UNPAID
                item.isChecked = true
            }
            R.id.action_filter_overdue -> {
                filterType = FilterType.OVERDUE
                item.isChecked = true
            }
            else -> return super.onOptionsItemSelected(item)
        }

        applyFilterAndSort()
        return true
    }


}
