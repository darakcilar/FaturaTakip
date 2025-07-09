package com.furkandarakcilar.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_table")
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val amount: Double,
    val dueDate: Long,
    // Yeni eklenen kategori alanı, var olan kayıtlar için 'DIGER' default
    val category: String = "DIGER",
    val owner: String,

    // Yeni ödenme durumu
    val isPaid: Boolean = false
)
