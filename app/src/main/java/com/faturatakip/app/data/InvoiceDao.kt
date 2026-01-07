package com.faturatakip.app.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.faturatakip.app.data.Invoice

@Dao
interface InvoiceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: Invoice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<Invoice>)

    @Update
    suspend fun update(invoice: Invoice)

    @Delete
    suspend fun delete(invoice: Invoice)

    @Query("SELECT * FROM invoices ORDER BY dueDate DESC")
    fun getAllInvoices(): LiveData<List<Invoice>>

    // YENİ EKLENDİ: Bir kategoriye ait tüm faturaları siler
    @Query("DELETE FROM invoices WHERE category = :categoryName")
    suspend fun deleteByCategory(categoryName: String)

    // YENİ EKLENDİ: Bir kategoriye ait tüm faturaların kategori adını günceller
    @Query("UPDATE invoices SET category = :newName WHERE category = :oldName")
    suspend fun updateCategoryName(oldName: String, newName: String)
}

