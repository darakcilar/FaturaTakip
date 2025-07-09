package com.furkandarakcilar.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.furkandarakcilar.myapplication.data.Invoice
import com.furkandarakcilar.myapplication.data.InvoiceDatabase
import com.furkandarakcilar.myapplication.data.InvoiceRepository
import com.furkandarakcilar.myapplication.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = InvoiceRepository(
        InvoiceDatabase.getInstance(application).invoiceDao(),
        application
    )

    /** Oturum açan kullanıcıya ait faturaları döner */
    val allInvoices: LiveData<List<Invoice>> =
        repo.getInvoicesForCurrentUser()

    /**
     * Yeni fatura ekle
     *
     * @param title   Fatura başlığı
     * @param amount  Tutar
     * @param dueDate Vade zamanı (millis)
     * @param category Kategori (TELEFON, SU, ELEKTRIK, DOGALGAZ, DIGER)
     */
    fun insert(
        title: String,
        amount: Double,
        dueDate: Long,
        category: String
    ) {
        val owner = Prefs.getCurrentUser(getApplication())
            ?: throw IllegalStateException("Oturum açmış kullanıcı bulunamadı")

        val invoice = Invoice(
            title    = title,
            amount   = amount,
            dueDate  = dueDate,
            owner    = owner,
            category = category
        )

        viewModelScope.launch(Dispatchers.IO) {
            repo.insert(invoice)
        }
    }

    /** Seçili faturayı siler */
    fun delete(invoice: Invoice) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.delete(invoice)
        }
    }

    /** Faturayı güncelle (örn. ödendi durumu için) */
    fun update(invoice: Invoice) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.update(invoice)
        }
    }
}
