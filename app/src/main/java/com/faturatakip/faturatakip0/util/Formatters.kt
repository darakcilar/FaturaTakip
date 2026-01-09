package com.faturatakip.faturatakip0.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun Double.formatCurrency(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
    return format.format(this)
}

fun Long.formatDate(): String {
    val date = Date(this)
    val format = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
    return format.format(date)
}

fun Long.isOverdue(): Boolean {
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)
    return this < today.timeInMillis
}

