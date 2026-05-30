package com.example.pocketflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketflow.viewmodel.TransactionViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class SummaryActivity : AppCompatActivity() {

    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        // Set current month
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        findViewById<TextView>(R.id.tvMonth).text = sdf.format(Date())

        val pieChart = findViewById<PieChart>(R.id.pieChart)
        val rvCategories = findViewById<RecyclerView>(R.id.rvCategories)
        val tvTotalExpense = findViewById<TextView>(R.id.tvTotalExpense)
        val tvTotalExpense2 = findViewById<TextView>(R.id.tvTotalExpense2)
        val tvTotalIncome = findViewById<TextView>(R.id.tvTotalIncome)

        rvCategories.layoutManager = LinearLayoutManager(this)

        // Observe data
        viewModel.totalIncome.observe(this) {
            tvTotalIncome.text = formatRupiah(it)
        }

        viewModel.totalExpense.observe(this) {
            tvTotalExpense.text = formatRupiah(it)
            tvTotalExpense2.text = formatRupiah(it)
        }

        viewModel.transactions.observe(this) { list ->
            val expenseList = list.filter { it.type == "EXPENSE" }
            val categoryMap = viewModel.getByCategory()

            if (categoryMap.isEmpty()) return@observe

            // Pie chart
            val entries = categoryMap.map { (cat, amount) ->
                PieEntry(amount.toFloat(), cat)
            }
            val totalExpense = categoryMap.values.sum().toDouble()
            val dataSet = PieDataSet(entries, "")
            dataSet.colors = listOf(
                0xFF5B4FCF.toInt(), 0xFF9C88FF.toInt(), 0xFF4CAF50.toInt(),
                0xFFF44336.toInt(), 0xFFFF9800.toInt(), 0xFF2196F3.toInt(),
                0xFFE91E63.toInt(), 0xFF009688.toInt()
            )
            dataSet.valueTextSize = 11f
            dataSet.valueTextColor = 0xFFFFFFFF.toInt()
            dataSet.sliceSpace = 2f

            pieChart.data = PieData(dataSet)
            pieChart.description.isEnabled = false
            pieChart.isDrawHoleEnabled = true
            pieChart.holeRadius = 40f
            pieChart.setHoleColor(android.graphics.Color.WHITE)
            pieChart.legend.isEnabled = false
            pieChart.animateY(800)
            pieChart.invalidate()

            // Category list
            val categoryList = categoryMap.entries
                .sortedByDescending { it.value }
                .map { (cat, amount) ->
                    val count = expenseList.count { it.category == cat }
                    val percent = if (totalExpense > 0)
                        viewModel.calculateCategoryPercentage(amount.toDouble(), totalExpense)
                    else 0.0
                    Triple(cat, amount, Pair(count, percent))
                }

            rvCategories.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                    object : RecyclerView.ViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_category, parent, false)
                    ) {}

                override fun getItemCount() = categoryList.size

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val (cat, amount, extra) = categoryList[position]
                    val (count, percent) = extra
                    holder.itemView.findViewById<TextView>(R.id.tvIcon).text = categoryIcon(cat)
                    holder.itemView.findViewById<TextView>(R.id.tvCategory).text = cat
                    holder.itemView.findViewById<TextView>(R.id.tvCount).text = "$count transaksi"
                    holder.itemView.findViewById<TextView>(R.id.tvAmount).text = formatRupiah(amount)
                    holder.itemView.findViewById<TextView>(R.id.tvPercent).text =
                        String.format("%.0f%%", percent)
                }
            }
        }
    }

    private fun categoryIcon(category: String) = when (category) {
        "Makanan" -> "🍔"
        "Transportasi" -> "🚗"
        "Belanja" -> "🛍️"
        "Tagihan" -> "💡"
        "Kesehatan" -> "💊"
        "Edukasi" -> "📚"
        "Hiburan" -> "🎮"
        else -> "💰"
    }

    private fun formatRupiah(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount).replace("Rp", "Rp ")
    }
}