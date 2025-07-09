package com.furkandarakcilar.myapplication.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.furkandarakcilar.myapplication.R
import com.furkandarakcilar.myapplication.data.Invoice
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class InvoiceAdapter(
    private val onClick: (Invoice) -> Unit,
    private val onLongClick: (Invoice) -> Boolean
) : ListAdapter<ListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM   = 1
        private val CATEGORY_ORDER = listOf(
            "TELEFON", "SU", "ELEKTRIK", "DOGALGAZ", "DIGER"
        )
    }

    private val selected = mutableSetOf<Invoice>()
    private val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun submitInvoices(invoices: List<Invoice>) {
        val byCat = invoices.groupBy { it.category }
        val list = mutableListOf<ListItem>()
        for (cat in CATEGORY_ORDER) {
            byCat[cat]?.let { items ->
                list += ListItem.Header(cat)
                list += items.map { ListItem.InvoiceItem(it) }
            }
        }
        submitList(list)
    }

    override fun getItemViewType(position: Int) =
        when (getItem(position)) {
            is ListItem.Header      -> TYPE_HEADER
            is ListItem.InvoiceItem -> TYPE_ITEM
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : RecyclerView.ViewHolder
    {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_category_header, parent, false))
        } else {
            InvoiceVH(inflater.inflate(R.layout.invoice_item, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        when (val item = getItem(pos)) {
            is ListItem.Header ->
                (holder as HeaderVH).bind(item.category)
            is ListItem.InvoiceItem ->
                (holder as InvoiceVH).bind(item.invoice,
                    selected.contains(item.invoice))
        }
    }

    fun toggleSelection(invoice: Invoice) {
        if (!selected.remove(invoice)) selected.add(invoice)
        val idx = currentList.indexOfFirst {
            it is ListItem.InvoiceItem && it.invoice.id == invoice.id
        }
        if (idx >= 0) notifyItemChanged(idx)
    }

    fun clearSelection() {
        val old = selected.toList()
        selected.clear()
        old.forEach { inv ->
            val idx = currentList.indexOfFirst {
                it is ListItem.InvoiceItem && it.invoice.id == inv.id
            }
            if (idx >= 0) notifyItemChanged(idx)
        }
    }

    fun getSelectedItems(): List<Invoice> = selected.toList()

    private inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = view.findViewById(R.id.tvCategoryHeader)
        fun bind(cat: String) {
            tv.text = when (cat) {
                "TELEFON"  -> "Telefon Faturaları"
                "SU"        -> "Su Faturaları"
                "ELEKTRIK"  -> "Elektrik Faturaları"
                "DOGALGAZ"  -> "Doğalgaz Faturaları"
                else        -> "Diğer Faturalar"
            }
        }
    }

    private inner class InvoiceVH(view: View) : RecyclerView.ViewHolder(view) {
        private val card      : MaterialCardView = view.findViewById(R.id.card)
        private val tvTitle   : TextView         = view.findViewById(R.id.tvTitle)
        private val tvAmount  : TextView         = view.findViewById(R.id.tvAmount)
        private val tvDueDate : TextView         = view.findViewById(R.id.tvDueDate)

        fun bind(invoice: Invoice, activated: Boolean) {
            tvTitle.text   = invoice.title
            tvAmount.text  = String.format(Locale.getDefault(), "%.2f ₺", invoice.amount)
            tvDueDate.text = dateFmt.format(Date(invoice.dueDate))

            // Filtre renkleri
            val filterColor = when {
                invoice.isPaid                               -> R.color.bg_paid_green
                invoice.dueDate < System.currentTimeMillis() -> R.color.bg_overdue_orange
                else                                         -> R.color.bg_paid_white
            }

            // Seçili ise invoice_selected_bg kullan
            val bgColorRes = if (activated) {
                R.color.invoice_selected_bg
            } else {
                filterColor
            }

            card.setCardBackgroundColor(
                ContextCompat.getColor(itemView.context, bgColorRes)
            )

            itemView.setOnClickListener    { onClick(invoice) }
            itemView.setOnLongClickListener { onLongClick(invoice) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(old: ListItem, new: ListItem): Boolean =
            when {
                old is ListItem.Header && new is ListItem.Header ->
                    old.category == new.category
                old is ListItem.InvoiceItem && new is ListItem.InvoiceItem ->
                    old.invoice.id == new.invoice.id
                else -> false
            }

        override fun areContentsTheSame(old: ListItem, new: ListItem): Boolean =
            old == new
    }
}
