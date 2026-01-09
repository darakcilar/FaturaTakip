package com.faturatakip.faturatakip0.ui

import android.app.Application
import androidx.lifecycle.*
import com.faturatakip.faturatakip0.data.AppDatabase
import com.faturatakip.faturatakip0.data.Invoice
import com.faturatakip.faturatakip0.data.db.InvoiceDao
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val invoiceDao: InvoiceDao
    val allInvoices: LiveData<List<Invoice>>

    private val _filter = MutableLiveData(Filter.ALL)
    private val _sort = MutableLiveData(Sort.DATE_ASC)
    private val _expandedCategories = MutableLiveData<Set<String>>(setOf())

    val groupedInvoices: LiveData<List<ListItem>>
    val totalUnpaid: LiveData<Double>

    init {
        val database = AppDatabase.getDatabase(application)
        invoiceDao = database.invoiceDao()
        allInvoices = invoiceDao.getAllInvoices()

        groupedInvoices = MediatorLiveData<List<ListItem>>().apply {
            fun update() { value = createGroupedList(allInvoices.value, _expandedCategories.value, _filter.value, _sort.value) }
            addSource(allInvoices) { update() }
            addSource(_expandedCategories) { update() }
            addSource(_filter) { update() }
            addSource(_sort) { update() }
        }

        totalUnpaid = allInvoices.map { invoices ->
            invoices.filter { !it.isPaid }.sumOf { it.amount }
        }
    }


    fun insert(invoice: Invoice) = viewModelScope.launch { invoiceDao.insert(invoice) }
    fun insertAll(invoices: List<Invoice>) = viewModelScope.launch { invoiceDao.insertAll(invoices) }
    fun update(invoice: Invoice) = viewModelScope.launch { invoiceDao.update(invoice) }
    fun deleteInvoice(invoice: Invoice) = viewModelScope.launch { invoiceDao.delete(invoice) }

    fun updateCategoryName(oldName: String, newName: String) = viewModelScope.launch {
        invoiceDao.updateCategoryName(oldName, newName)
    }

    fun deleteByCategory(categoryName: String) = viewModelScope.launch {
        invoiceDao.deleteByCategory(categoryName)
    }

    fun toggleInvoiceStatus(invoice: Invoice) = viewModelScope.launch {
        invoiceDao.update(invoice.copy(isPaid = !invoice.isPaid))
    }

    fun setFilter(f: Filter) { _filter.value = f }
    fun setSort(s: Sort) { _sort.value = s }
    fun toggleCategory(category: String) {
        val current = _expandedCategories.value ?: setOf()
        _expandedCategories.value = if (current.contains(category)) current - category else current + category
    }

    private fun createGroupedList(invoices: List<Invoice>?, expanded: Set<String>?, filter: Filter?, sort: Sort?): List<ListItem> {
        val items = mutableListOf<ListItem>()
        val filtered = invoices?.filter {
            when (filter) {
                Filter.PAID -> it.isPaid
                Filter.UNPAID -> !it.isPaid
                else -> true
            }
        } ?: emptyList()

        val grouped = filtered.groupBy { it.category }.toSortedMap()
        grouped.forEach { (category, list) ->
            val isExp = expanded?.contains(category) ?: false
            items.add(ListItem.HeaderItem(category, list.size, isExp))
            if (isExp) {
                val sorted = when (sort) {
                    Sort.DATE_DESC -> list.sortedByDescending { it.dueDate }
                    Sort.DATE_ASC -> list.sortedBy { it.dueDate }
                    Sort.AMOUNT_DESC -> list.sortedByDescending { it.amount }
                    Sort.AMOUNT_ASC -> list.sortedBy { it.amount }
                    Sort.NAME_ASC -> list.sortedBy { it.name }
                    else -> list.sortedBy { it.dueDate }
                }
                items.addAll(sorted.map { ListItem.InvoiceItem(it) })
            }
        }
        return items
    }

    enum class Filter { ALL, PAID, UNPAID }
    enum class Sort { DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, NAME_ASC }
}