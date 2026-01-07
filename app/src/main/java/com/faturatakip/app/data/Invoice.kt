package com.faturatakip.app.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize


@Parcelize
@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val dueDate: Long,
    val category: String, // YENİ EKLENDİ: Kategori alanı
    val isPaid: Boolean = false
) : Parcelable

