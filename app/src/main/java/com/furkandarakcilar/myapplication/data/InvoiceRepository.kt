package com.furkandarakcilar.myapplication.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import com.furkandarakcilar.myapplication.util.Prefs

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val context: Context
) {

    /**
     * Oturum açmış kullanıcıya ait faturalar.
     * Kullanıcı yoksa boş liste.
     */
    fun getInvoicesForCurrentUser(): LiveData<List<Invoice>> {
        val currentUser = Prefs.getCurrentUser(context)
            ?: return MutableLiveData(emptyList())
        return invoiceDao.getByOwner(currentUser)
    }

    /** Yeni fatura ekle */
    fun insert(invoice: Invoice) {
        invoiceDao.insert(invoice)
    }

    /** Mevcut faturayı güncelle */
    fun update(invoice: Invoice) {
        invoiceDao.update(invoice)
    }

    /** Faturayı sil */
    fun delete(invoice: Invoice) {
        invoiceDao.delete(invoice)
    }
}
