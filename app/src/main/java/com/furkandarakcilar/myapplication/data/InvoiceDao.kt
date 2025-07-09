package com.furkandarakcilar.myapplication.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface InvoiceDao {

    @Query(
        """
      SELECT * FROM invoice_table
      WHERE owner = :owner
      ORDER BY dueDate DESC
      """
    )
    fun getByOwner(owner: String): LiveData<List<Invoice>>

    @Insert
    fun insert(inv: Invoice)

    @Update       // ← Güncelleme desteği
    fun update(inv: Invoice)

    @Delete
    fun delete(inv: Invoice)
}
