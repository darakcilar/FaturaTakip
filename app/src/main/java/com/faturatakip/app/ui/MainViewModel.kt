package com.faturatakip.app.ui

import android.app.Application
import androidx.lifecycle.*
import com.faturatakip.app.data.AppDatabase
import com.faturatakip.app.data.db.InvoiceDao
import com.faturatakip.app.data.Invoice
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val invoiceDao: InvoiceDao
    val allInvoices: LiveData<List<Invoice>>

    private val _filter = MutableLiveData(Filter.ALL)
    private val _sort = MutableLiveData(Sort.DATE_ASC)
    private val _expandedCategories = MutableLiveData<Set<String>>(setOf())

    val groupedInvoices: LiveData<List<ListItem>>
    val categories: LiveData<List<String>>
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

        categories = MediatorLiveData<List<String>>().apply {
            addSource(allInvoices) { invoices ->
                value = invoices?.map { it.category }?.distinct()?.sorted() ?: emptyList()
            }
        }

        totalUnpaid = MediatorLiveData<Double>().apply {
            addSource(allInvoices) { invoices ->
                value = invoices?.filter { !it.isPaid }?.sumOf { it.amount } ?: 0.0
            }
        }
    }

    fun setFilter(filter: Filter) { _filter.value = filter }
    fun setSort(sort: Sort) { _sort.value = sort }

    fun toggleCategory(category: String) {
        val currentSet = _expandedCategories.value ?: setOf()
        val newSet = currentSet.toMutableSet()
        if (newSet.contains(category)) {
            newSet.remove(category)
        } else {
            newSet.add(category)
        }
        _expandedCategories.value = newSet
    }

    private fun createGroupedList(invoices: List<Invoice>?, expandedCategories: Set<String>?, filter: Filter?, sort: Sort?): List<ListItem> {
        val items = mutableListOf<ListItem>()

        val filteredInvoices = invoices?.filter {
            when (filter) {
                Filter.PAID -> it.isPaid
                Filter.UNPAID -> !it.isPaid
                else -> true
            }
        }

        val grouped = filteredInvoices?.groupBy { it.category }?.toSortedMap() ?: emptyMap()

        grouped.forEach { (category, invoiceList) ->
            val isExpanded = expandedCategories?.contains(category) ?: false
            items.add(ListItem.HeaderItem(category, invoiceList.size, isExpanded))
            if (isExpanded) {
                val sortedInvoiceList = when (sort) {
                    Sort.DATE_DESC -> invoiceList.sortedByDescending { it.dueDate }
                    Sort.AMOUNT_DESC -> invoiceList.sortedByDescending { it.amount }
                    Sort.AMOUNT_ASC -> invoiceList.sortedBy { it.amount }
                    Sort.NAME_ASC -> invoiceList.sortedBy { it.name }
                    else -> invoiceList.sortedBy { it.dueDate }
                }
                items.addAll(sortedInvoiceList.map { ListItem.InvoiceItem(it) })
            }
        }
        return items
    }

    fun insert(invoice: Invoice) = viewModelScope.launch { invoiceDao.insert(invoice) }

    fun insertAll(invoices: List<Invoice>) = viewModelScope.launch { invoiceDao.insertAll(invoices) }

    fun update(invoice: Invoice) = viewModelScope.launch { invoiceDao.update(invoice) }

    fun deleteInvoice(invoice: Invoice) = viewModelScope.launch { invoiceDao.delete(invoice) }

    fun toggleInvoiceStatus(invoice: Invoice) = viewModelScope.launch {
        val updatedInvoice = invoice.copy(isPaid = !invoice.isPaid)
        invoiceDao.update(updatedInvoice)
    }

    fun deleteCategory(category: String) = viewModelScope.launch {
        invoiceDao.deleteByCategory(category)
    }

    fun updateCategoryName(oldName: String, newName: String) = viewModelScope.launch {
        invoiceDao.updateCategoryName(oldName, newName)
    }

    enum class Filter { ALL, PAID, UNPAID }
    enum class Sort { DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, NAME_ASC }
}

