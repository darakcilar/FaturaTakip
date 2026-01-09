package com.faturatakip.faturatakip0.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.faturatakip.faturatakip0.R
import com.faturatakip.faturatakip0.data.Invoice
import com.faturatakip.faturatakip0.databinding.ListItemCategoryHeaderBinding
import com.faturatakip.faturatakip0.databinding.ListItemInvoiceBinding
import com.faturatakip.faturatakip0.util.formatCurrency
import com.faturatakip.faturatakip0.util.formatDate
import com.faturatakip.faturatakip0.util.isOverdue

sealed class ListItem {
    data class HeaderItem(val category: String, val invoiceCount: Int, var isExpanded: Boolean = false) : ListItem()
    data class InvoiceItem(val invoice: Invoice) : ListItem()
}

class InvoiceAdapter(
    private val onEditClick: (Invoice) -> Unit,
    private val onDeleteClick: (Invoice) -> Unit,
    private val onToggleStatusClick: (Invoice) -> Unit,
    private val onGeneratePdfClick: (Invoice) -> Unit,
    private val onCategoryClick: (ListItem.HeaderItem) -> Unit,
    private val onCategoryPdfClick: (String) -> Unit,
    private val onCategoryEditClick: (String) -> Unit,
    private val onCategoryDeleteClick: (String) -> Unit
) : ListAdapter<ListItem, RecyclerView.ViewHolder>(ListItemDiffCallback()) {

    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_INVOICE = 1

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.HeaderItem -> VIEW_TYPE_HEADER
            is ListItem.InvoiceItem -> VIEW_TYPE_INVOICE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = ListItemCategoryHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ListItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            InvoiceViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.HeaderItem -> (holder as HeaderViewHolder).bind(item)
            is ListItem.InvoiceItem -> (holder as InvoiceViewHolder).bind(item.invoice)
        }
    }

    inner class HeaderViewHolder(private val binding: ListItemCategoryHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.categoryMainLayout.setOnClickListener {
                if(adapterPosition != RecyclerView.NO_POSITION) {
                    val header = getItem(adapterPosition) as ListItem.HeaderItem
                    onCategoryClick(header)
                }
            }
            binding.categoryPdfButton.setOnClickListener {
                if(adapterPosition != RecyclerView.NO_POSITION) {
                    onCategoryPdfClick((getItem(adapterPosition) as ListItem.HeaderItem).category)
                }
            }
            binding.categoryEditButton.setOnClickListener {
                if(adapterPosition != RecyclerView.NO_POSITION) {
                    onCategoryEditClick((getItem(adapterPosition) as ListItem.HeaderItem).category)
                }
            }
            binding.categoryDeleteButton.setOnClickListener {
                if(adapterPosition != RecyclerView.NO_POSITION) {
                    onCategoryDeleteClick((getItem(adapterPosition) as ListItem.HeaderItem).category)
                }
            }
        }
        fun bind(header: ListItem.HeaderItem) {
            binding.categoryName.text = header.category
            binding.invoiceCount.text = header.invoiceCount.toString()
            binding.expandIcon.rotation = if (header.isExpanded) 0f else -180f
            binding.categoryActionsLayout.visibility = if(header.isExpanded) View.VISIBLE else View.GONE
        }
    }

    inner class InvoiceViewHolder(private val binding: ListItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.editButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onEditClick((getItem(adapterPosition) as ListItem.InvoiceItem).invoice)
                }
            }
            binding.deleteButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClick((getItem(adapterPosition) as ListItem.InvoiceItem).invoice)
                }
            }
            binding.statusToggleButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onToggleStatusClick((getItem(adapterPosition) as ListItem.InvoiceItem).invoice)
                }
            }
            binding.pdfButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onGeneratePdfClick((getItem(adapterPosition) as ListItem.InvoiceItem).invoice)
                }
            }
        }

        fun bind(invoice: Invoice) {
            val context = binding.root.context
            binding.invoiceName.text = invoice.name
            binding.invoiceAmount.text = invoice.amount.formatCurrency()
            binding.invoiceDueDate.text = context.getString(R.string.due_date_label, invoice.dueDate.formatDate())

            if (invoice.isPaid) {
                binding.statusText.text = context.getString(R.string.status_paid)
                binding.statusText.background.setTint(ContextCompat.getColor(context, R.color.paid_green_bg))
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.paid_green_text))
                binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.paid_green_card_bg))
                binding.statusToggleButton.setImageResource(R.drawable.ic_rotate_ccw)
            } else {
                if (invoice.dueDate.isOverdue()) {
                    binding.statusText.text = context.getString(R.string.status_overdue)
                    binding.statusText.background.setTint(ContextCompat.getColor(context, R.color.overdue_red_bg))
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.overdue_red_text))
                    binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.overdue_red_card_bg))
                } else {
                    binding.statusText.text = context.getString(R.string.status_unpaid)
                    binding.statusText.background.setTint(ContextCompat.getColor(context, R.color.unpaid_yellow_bg))
                    binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.unpaid_yellow_text))
                    binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                }
                binding.statusToggleButton.setImageResource(R.drawable.ic_check)
            }
        }
    }
}

class ListItemDiffCallback : DiffUtil.ItemCallback<ListItem>() {
    override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
        return if (oldItem is ListItem.HeaderItem && newItem is ListItem.HeaderItem) {
            oldItem.category == newItem.category
        } else if (oldItem is ListItem.InvoiceItem && newItem is ListItem.InvoiceItem) {
            oldItem.invoice.id == newItem.invoice.id
        } else {
            false
        }
    }

    override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
        return oldItem == newItem
    }
}