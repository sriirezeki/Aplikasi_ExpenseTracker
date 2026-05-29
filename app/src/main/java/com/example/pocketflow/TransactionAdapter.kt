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
    private var transactions: List<Transaction>,
    private val onLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvIcon)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvNote: TextView = view.findViewById(R.id.tvNote)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = transactions[position]

        holder.tvIcon.text = categoryIcon(t.category)
        holder.tvCategory.text = t.category
        holder.tvNote.text = if (t.note.isEmpty()) t.category else t.note
        holder.tvDate.text = formatDate(t.date)

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

    override fun getItemCount() = transactions.size

    fun updateList(newList: List<Transaction>) {
        transactions = newList
        notifyDataSetChanged()
    }

    private fun categoryIcon(category: String): String {
        return when (category) {
            "Makanan" -> "🍔"
            "Transportasi" -> "🚗"
            "Belanja" -> "🛍️"
            "Tagihan" -> "💡"
            "Kesehatan" -> "💊"
            "Edukasi" -> "📚"
            "Hiburan" -> "🎮"
            else -> "💰"
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    private fun formatRupiah(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount).replace("Rp", "Rp ")
    }
}