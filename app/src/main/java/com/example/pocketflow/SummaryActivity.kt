package com.example.pocketflow

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketflow.data.Transaction
import com.example.pocketflow.viewmodel.TransactionViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class SummaryActivity : AppCompatActivity() {

    private val viewModel: TransactionViewModel by viewModels()
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)
    private var selectedMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var allTransactions = listOf<Transaction>()

    private lateinit var tvMonthYear: TextView
    private lateinit var tvHeaderExpense: TextView
    private lateinit var tvMonthIncome: TextView
    private lateinit var tvMonthExpense: TextView
    private lateinit var pieChart: PieChart
    private lateinit var legendContainer: LinearLayout
    private lateinit var rvCategories: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var layoutPieCard: View
    private lateinit var layoutCategoryCard: View

    private val pieColors = listOf(
        0xFFFF9800.toInt(), 0xFF2196F3.toInt(), 0xFFF44336.toInt(),
        0xFF4CAF50.toInt(), 0xFF795548.toInt(), 0xFF9C27B0.toInt(),
        0xFF9E9E9E.toInt(), 0xFFE91E63.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)
        supportActionBar?.hide()

        tvMonthYear = findViewById(R.id.tvMonthYear)
        tvHeaderExpense = findViewById(R.id.tvHeaderExpense)
        tvMonthIncome = findViewById(R.id.tvMonthIncome)
        tvMonthExpense = findViewById(R.id.tvMonthExpense)
        pieChart = findViewById(R.id.pieChart)
        legendContainer = findViewById(R.id.legendContainer)
        rvCategories = findViewById(R.id.rvCategories)
        tvEmpty = findViewById(R.id.tvEmpty)
        layoutPieCard = findViewById(R.id.layoutPieCard)
        layoutCategoryCard = findViewById(R.id.layoutCategoryCard)

        rvCategories.layoutManager = LinearLayoutManager(this)
        updateMonthHeader()

        findViewById<TextView>(R.id.btnPrevMonth).setOnClickListener {
            val cal = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
            cal.add(Calendar.MONTH, -1)
            selectedYear = cal.get(Calendar.YEAR)
            selectedMonth = cal.get(Calendar.MONTH)
            updateMonthHeader()
            updateUI()
        }

        findViewById<TextView>(R.id.btnNextMonth).setOnClickListener {
            val cal = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
            cal.add(Calendar.MONTH, 1)
            selectedYear = cal.get(Calendar.YEAR)
            selectedMonth = cal.get(Calendar.MONTH)
            updateMonthHeader()
            updateUI()
        }

        viewModel.transactions.observe(this) { list ->
            allTransactions = list
            updateUI()
        }

        setupBottomNav()
    }

    private fun updateMonthHeader() {
        val cal = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
        tvMonthYear.text = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(cal.time)
    }

    private fun updateUI() {
        val monthlyList = allTransactions.filter { t ->
            val cal = Calendar.getInstance().apply { timeInMillis = t.date }
            cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonth
        }

        val monthlyIncome = monthlyList.filter { it.type == "INCOME" }.sumOf { it.amount }
        val monthlyExpense = monthlyList.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        tvHeaderExpense.text = formatRupiah(monthlyExpense)
        tvMonthIncome.text = formatRupiah(monthlyIncome)
        tvMonthExpense.text = formatRupiah(monthlyExpense)

        val expenseList = monthlyList.filter { it.type == "EXPENSE" }
        val categoryMap = expenseList
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }

        if (categoryMap.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            layoutPieCard.visibility = View.GONE
            layoutCategoryCard.visibility = View.GONE
            pieChart.clear()
            pieChart.invalidate()
            legendContainer.removeAllViews()
            return
        }

        tvEmpty.visibility = View.GONE
        layoutPieCard.visibility = View.VISIBLE
        layoutCategoryCard.visibility = View.VISIBLE

        val totalExpense = categoryMap.sumOf { it.value }.toDouble()

        // Pie chart
        val entries = categoryMap.map { PieEntry(it.value.toFloat(), "") }
        val dataSet = PieDataSet(entries, "").apply {
            colors = categoryMap.indices.map { i -> pieColors[i % pieColors.size] }
            sliceSpace = 2f
            setDrawValues(false)
        }
        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 45f
        pieChart.setHoleColor(android.graphics.Color.WHITE)
        pieChart.setCenterText(formatRupiah(monthlyExpense))
        pieChart.setCenterTextSize(9f)
        pieChart.setCenterTextColor(0xFF1A1A2E.toInt())
        pieChart.animateY(600)
        pieChart.invalidate()

        // Legend beside pie chart
        legendContainer.removeAllViews()
        categoryMap.forEachIndexed { index, (cat, _) ->
            val item = LayoutInflater.from(this)
                .inflate(R.layout.item_pie_legend, legendContainer, false)
            item.findViewById<View>(R.id.vLegendDot).background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(pieColors[index % pieColors.size])
            }
            item.findViewById<TextView>(R.id.tvLegendName).text = cat
            legendContainer.addView(item)
        }

        // Category detail list
        val detailList = categoryMap.map { (cat, amount) ->
            val count = expenseList.count { it.category == cat }
            val percent = if (totalExpense > 0) amount.toDouble() / totalExpense * 100 else 0.0
            Triple(cat, amount, Pair(count, percent))
        }

        rvCategories.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_category, parent, false)
                ) {}

            override fun getItemCount() = detailList.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val (cat, amount, extra) = detailList[pos]
                val (count, percent) = extra
                val v = holder.itemView
                val icon = v.findViewById<TextView>(R.id.tvIcon)
                icon.text = categoryIcon(cat)
                icon.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 10 * resources.displayMetrics.density
                    setColor(categoryColor(cat))
                }
                v.findViewById<TextView>(R.id.tvCategory).text = cat
                v.findViewById<TextView>(R.id.tvCount).text = "$count transaksi"
                v.findViewById<TextView>(R.id.tvAmount).text = formatRupiah(amount)
                v.findViewById<TextView>(R.id.tvPercent).text = String.format("%.0f%%", percent)
            }
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_summary
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_transaction -> {
                    startActivity(Intent(this, TransactionActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_add -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    true
                }
                R.id.nav_summary -> true
                R.id.nav_ai -> {
                    startActivity(Intent(this, AiChatActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
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
        "Gaji" -> "💼"
        "Bonus" -> "🎁"
        "Investasi" -> "📈"
        "Freelance" -> "💻"
        else -> "💰"
    }

    private fun categoryColor(category: String): Int = when (category) {
        "Makanan" -> 0xFFFFE0B2.toInt()
        "Transportasi" -> 0xFFE3F2FD.toInt()
        "Belanja" -> 0xFFF3E5F5.toInt()
        "Tagihan" -> 0xFFFFF9C4.toInt()
        "Kesehatan" -> 0xFFE8F5E9.toInt()
        "Edukasi" -> 0xFFE1F5FE.toInt()
        "Hiburan" -> 0xFFFCE4EC.toInt()
        else -> 0xFFEEF0FF.toInt()
    }

    private fun formatRupiah(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount).replace("Rp", "Rp ")
    }
}
