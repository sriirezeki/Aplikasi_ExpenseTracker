package com.example.pocketflow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketflow.data.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var items: List<TransactionListItem>,
    private val onLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class TransactionListItem {
        data class Header(val label: String) : TransactionListItem()
        data class Item(val transaction: Transaction) : TransactionListItem()
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TRANSACTION = 1
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDateHeader: TextView = view.findViewById(R.id.tvDateHeader)
    }

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvIcon)
        val tvNote: TextView = view.findViewById(R.id.tvNote)
        val tvCategoryTime: TextView = view.findViewById(R.id.tvCategoryTime)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is TransactionListItem.Header -> TYPE_HEADER
        is TransactionListItem.Item -> TYPE_TRANSACTION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date_header, parent, false)
            )
        } else {
            TransactionViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_transaction, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TransactionListItem.Header -> (holder as HeaderViewHolder).tvDateHeader.text = item.label
            is TransactionListItem.Item -> {
                val t = item.transaction
                holder as TransactionViewHolder
                holder.tvIcon.text = categoryIcon(t.category)
                holder.tvNote.text = if (t.note.isNotEmpty()) t.note else t.category
                holder.tvCategoryTime.text = "${t.category} · ${formatTime(t.date)}"
                val amount = formatRupiah(t.amount)
                if (t.type == "INCOME") {
                    holder.tvAmount.text = "+$amount"
                    holder.tvAmount.setTextColor(0xFF4CAF50.toInt())
                } else {
                    holder.tvAmount.text = "-$amount"
                    holder.tvAmount.setTextColor(0xFFF44336.toInt())
                }
                holder.itemView.setOnLongClickListener {
                    onLongClick(t)
                    true
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<TransactionListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun categoryIcon(category: String) = when (category) {
        "Makanan" -> "🍔"
        "Transportasi" -> "🚗"
        "Belanja" -> "🛍️"
        "Tagihan" -> "💡"
        "Kesehatan" -> "💊"
        "Edukasi" -> "📚"
        "Hiburan" -> "🎮"
        "Gaji" -> "💼"
        "Bonus" -> "🎁"
        "Investasi" -> "📈"
        "Freelance" -> "💻"
        else -> "💰"
    }

    private fun formatTime(timestamp: Long): String =
        SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(timestamp))

    private fun formatRupiah(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount).replace("Rp", "Rp ")
    }
}
