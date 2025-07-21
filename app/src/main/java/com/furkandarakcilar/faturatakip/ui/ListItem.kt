// app/src/main/java/com/furkandarakcilar/myapplication/ui/ListItem.kt
package com.furkandarakcilar.faturatakip.ui

sealed class ListItem {
    data class Header(val category: String) : ListItem()
    data class InvoiceItem(val invoice: com.furkandarakcilar.faturatakip.data.Invoice) : ListItem()
}
