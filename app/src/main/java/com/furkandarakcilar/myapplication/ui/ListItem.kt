// app/src/main/java/com/furkandarakcilar/myapplication/ui/ListItem.kt
package com.furkandarakcilar.myapplication.ui

sealed class ListItem {
    data class Header(val category: String) : ListItem()
    data class InvoiceItem(val invoice: com.furkandarakcilar.myapplication.data.Invoice) : ListItem()
}
