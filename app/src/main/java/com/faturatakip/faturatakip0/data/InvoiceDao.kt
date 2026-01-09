package com.faturatakip.faturatakip0.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.faturatakip.faturatakip0.data.Invoice

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

    @Query("SELECT * FROM invoices ORDER BY dueDate ASC")
    fun getAllInvoices(): LiveData<List<Invoice>>
    @Query("SELECT * FROM invoices")
    suspend fun getAllInvoicesSync(): List<Invoice>

    @Query("UPDATE invoices SET category = :newName WHERE category = :oldName")
    suspend fun updateCategoryName(oldName: String, newName: String)

    @Query("DELETE FROM invoices WHERE category = :categoryName")
    suspend fun deleteByCategory(categoryName: String)
}