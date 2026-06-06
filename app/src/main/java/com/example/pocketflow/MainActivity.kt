package com.example.pocketflow

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketflow.viewmodel.TransactionViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        val tvBalance = findViewById<TextView>(R.id.tvTotalBalance)
        val tvIncome = findViewById<TextView>(R.id.tvTotalIncome)
        val tvExpense = findViewById<TextView>(R.id.tvTotalExpense)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val rvTransactions = findViewById<RecyclerView>(R.id.rvTransactions)
        val tvSeeAll = findViewById<TextView>(R.id.tvSeeAll)
        val tvBudgetEdit = findViewById<TextView>(R.id.tvBudgetEdit)
        val tvBudgetUsed = findViewById<TextView>(R.id.tvBudgetUsed)
        val tvBudgetLimit = findViewById<TextView>(R.id.tvBudgetLimit)
        val tvBudgetTitle = findViewById<TextView>(R.id.tvBudgetTitle)
        val viewBudgetProgress = findViewById<View>(R.id.viewBudgetProgress)
        val frameProgress = findViewById<View>(R.id.frameProgress)
        val tvBudgetPercent = findViewById<TextView>(R.id.tvBudgetPercent)
        val tvAiInsight = findViewById<TextView>(R.id.tvAiInsight)
        val barChart = findViewById<BarChart>(R.id.barChartDaily)

        // Greeting
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        findViewById<TextView>(R.id.tvGreeting).text = when {
            hour < 12 -> "Selamat pagi ☀️"
            hour < 15 -> "Selamat siang 🌤️"
            hour < 18 -> "Selamat sore 🌆"
            else -> "Selamat malam 🌙"
        }

        // Budget
        val prefs = getSharedPreferences("pocketflow_prefs", MODE_PRIVATE)
        var budgetLimit = prefs.getLong("budget_limit", 0L)

        fun updateBudgetUI(expense: Long) {
            val monthName = java.text.SimpleDateFormat(
                "MMMM yyyy", Locale("id", "ID")
            ).format(Date())
            tvBudgetTitle.text = "Budget $monthName"
            tvBudgetUsed.text = formatRupiah(expense)
            tvBudgetLimit.text = "/ ${formatRupiah(budgetLimit)}"

            if (budgetLimit > 0) {
                val percent = (expense.toFloat() / budgetLimit.toFloat()).coerceIn(0f, 1f)
                frameProgress.post {
                    val parentWidth = frameProgress.width
                    val params = viewBudgetProgress.layoutParams
                    params.width = (parentWidth * percent).toInt()
                    viewBudgetProgress.layoutParams = params
                }
                val pct = (percent * 100).toInt()
                tvBudgetPercent.text = "$pct% terpakai"
                tvBudgetPercent.setTextColor(
                    if (pct >= 90) 0xFFE53935.toInt() else 0xFF888888.toInt()
                )
            } else {
                tvBudgetPercent.text = "Budget belum diatur"
            }
        }

        tvBudgetEdit.setOnClickListener {
            val dialogView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(60, 40, 60, 20)
            }

            val titleIcon = TextView(this).apply {
                text = "💰"
                textSize = 36f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 8 }
            }

            val subtitle = TextView(this).apply {
                text = "Atur batas pengeluaran bulanmu"
                textSize = 13f
                setTextColor(0xFF888888.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 24 }
            }

            val input = EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                hint = "Contoh: 3000000"
                textSize = 18f
                setTextColor(0xFF1A1A2E.toInt())
                gravity = android.view.Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFFEEF0FF.toInt())
                    cornerRadius = 32f
                }
                setPadding(40, 28, 40, 28)
                if (budgetLimit > 0) setText(budgetLimit.toString())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 8 }
            }

            val hint = TextView(this).apply {
                text = "Rp per bulan"
                textSize = 11f
                setTextColor(0xFFAAAAAA.toInt())
                gravity = android.view.Gravity.CENTER
            }

            dialogView.addView(titleIcon)
            dialogView.addView(subtitle)
            dialogView.addView(input)
            dialogView.addView(hint)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Budget Bulanan")
                .setView(dialogView)
                .setPositiveButton("Simpan") { _, _ ->
                    val value = input.text.toString().toLongOrNull() ?: 0L
                    budgetLimit = value
                    prefs.edit().putLong("budget_limit", value).apply()
                    val expense = viewModel.totalExpense.value ?: 0L
                    updateBudgetUI(expense)
                }
                .setNegativeButton("Batal", null)
                .create()

            dialog.show()

            // Color the buttons
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(0xFF5B4FCF.toInt())
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(0xFF888888.toInt())
        }

        tvSeeAll.setOnClickListener {
            startActivity(Intent(this, TransactionActivity::class.java))
        }

        rvTransactions.layoutManager = LinearLayoutManager(this)

        viewModel.transactions.observe(this) { list ->
            if (list.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvTransactions.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvTransactions.visibility = View.VISIBLE
                rvTransactions.adapter = TransactionAdapter(
                    list.take(5).map { TransactionAdapter.TransactionListItem.Item(it) }
                ) {}
            }

            setupDailyChart(barChart, list)

            // AI insight
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val yesterdayStart = todayStart - 86400000L

            val expenseToday = list.filter {
                it.type == "EXPENSE" && it.date >= todayStart
            }.sumOf { it.amount }

            val expenseYesterday = list.filter {
                it.type == "EXPENSE" && it.date >= yesterdayStart && it.date < todayStart
            }.sumOf { it.amount }

            tvAiInsight.text = when {
                expenseToday == 0L -> "Belum ada pengeluaran hari ini. Bagus! 🎉"
                expenseYesterday == 0L -> "Pengeluaran hari ini: ${formatRupiah(expenseToday)}"
                expenseToday < expenseYesterday -> {
                    val pct = ((expenseYesterday - expenseToday).toDouble() / expenseYesterday * 100).toInt()
                    "Pengeluaran hari ini lebih hemat $pct% dari kemarin 💚"
                }
                else -> {
                    val pct = ((expenseToday - expenseYesterday).toDouble() / expenseYesterday * 100).toInt()
                    "Pengeluaran hari ini naik $pct% dari kemarin ⚠️"
                }
            }
        }

        viewModel.totalIncome.observe(this) { income ->
            tvIncome.text = formatRupiah(income)
            val expense = viewModel.totalExpense.value ?: 0L
            tvBalance.text = formatRupiah(income - expense)
        }

        viewModel.totalExpense.observe(this) { expense ->
            tvExpense.text = formatRupiah(expense)
            val income = viewModel.totalIncome.value ?: 0L
            tvBalance.text = formatRupiah(income - expense)
            updateBudgetUI(expense)
        }

        setupBottomNav()
    }

    private fun setupDailyChart(
        chart: BarChart,
        transactions: List<com.example.pocketflow.data.Transaction>
    ) {
        val dayLabels = mutableListOf<String>()
        val entries = mutableListOf<BarEntry>()
        val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 86399999L

            val total = transactions.filter {
                it.type == "EXPENSE" && it.date in dayStart..dayEnd
            }.sumOf { it.amount }

            val index = (6 - i).toFloat()
            entries.add(BarEntry(index, total.toFloat()))
            dayLabels.add(dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1])
        }

        val dataSet = BarDataSet(entries, "")
        dataSet.color = 0xFF5B4FCF.toInt()
        dataSet.setDrawValues(false)

        chart.data = BarData(dataSet)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.textColor = 0xFF888888.toInt()
        chart.axisLeft.textSize = 10f
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)
        chart.xAxis.textColor = 0xFF888888.toInt()
        chart.xAxis.textSize = 10f
        chart.xAxis.granularity = 1f
        chart.setTouchEnabled(false)
        chart.animateY(600)
        chart.invalidate()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_transaction -> {
                    startActivity(Intent(this, TransactionActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_add -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    true
                }
                R.id.nav_summary -> {
                    startActivity(Intent(this, SummaryActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_ai -> {
                    startActivity(Intent(this, AiChatActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun formatRupiah(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount).replace("Rp", "Rp ")
    }
}