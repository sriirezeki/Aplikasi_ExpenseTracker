package com.example.pocketflow

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketflow.data.Transaction
import com.example.pocketflow.viewmodel.TransactionViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

class TransactionActivity : AppCompatActivity() {

    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var rvTransactions: RecyclerView
    private var allTransactions = listOf<Transaction>()
    private var currentFilter = "ALL"
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)
        supportActionBar?.hide()

        tvEmpty = findViewById(R.id.tvEmpty)
        rvTransactions = findViewById(R.id.rvTransactions)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        adapter = TransactionAdapter(emptyList()) { transaction ->
            MaterialAlertDialogBuilder(this)
                .setTitle("Hapus Transaksi?")
                .setMessage("${transaction.category} - Rp ${transaction.amount}")
                .setPositiveButton("Hapus") { _, _ ->
                    viewModel.deleteTransaction(transaction.id) {}
                }
                .setNegativeButton("Batal", null)
                .show()
        }
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter

        val filterAll = findViewById<TextView>(R.id.filterAll)
        val filterIncome = findViewById<TextView>(R.id.filterIncome)
        val filterExpense = findViewById<TextView>(R.id.filterExpense)
        val filterMakanan = findViewById<TextView>(R.id.filterMakanan)
        val filterTransportasi = findViewById<TextView>(R.id.filterTransportasi)
        val filterBelanja = findViewById<TextView>(R.id.filterBelanja)
        val allChips = listOf(filterAll, filterIncome, filterExpense, filterMakanan, filterTransportasi, filterBelanja)

        fun setActive(active: TextView) {
            allChips.forEach { chip ->
                if (chip == active) {
                    chip.setBackgroundResource(R.drawable.filter_active)
                    chip.setTextColor(0xFFFFFFFF.toInt())
                } else {
                    chip.setBackgroundResource(R.drawable.filter_inactive)
                    chip.setTextColor(0xFF5B4FCF.toInt())
                }
            }
        }

        filterAll.setOnClickListener { currentFilter = "ALL"; setActive(filterAll); applyFilter() }
        filterIncome.setOnClickListener { currentFilter = "INCOME"; setActive(filterIncome); applyFilter() }
        filterExpense.setOnClickListener { currentFilter = "EXPENSE"; setActive(filterExpense); applyFilter() }
        filterMakanan.setOnClickListener { currentFilter = "Makanan"; setActive(filterMakanan); applyFilter() }
        filterTransportasi.setOnClickListener { currentFilter = "Transportasi"; setActive(filterTransportasi); applyFilter() }
        filterBelanja.setOnClickListener { currentFilter = "Belanja"; setActive(filterBelanja); applyFilter() }

        viewModel.transactions.observe(this) { list ->
            allTransactions = list
            applyFilter()
        }

        setupSearch()
        setupBottomNav()
    }

    private fun setupSearch() {
        val layoutNormal = findViewById<LinearLayout>(R.id.layoutHeaderNormal)
        val layoutSearch = findViewById<LinearLayout>(R.id.layoutHeaderSearch)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val btnSearch = findViewById<TextView>(R.id.btnSearch)
        val btnSearchClose = findViewById<TextView>(R.id.btnSearchClose)
        val btnSearchClear = findViewById<TextView>(R.id.btnSearchClear)

        fun openSearch() {
            layoutNormal.visibility = View.GONE
            layoutSearch.visibility = View.VISIBLE
            etSearch.requestFocus()
            val imm = getSystemService(InputMethodManager::class.java)
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        fun closeSearch() {
            layoutSearch.visibility = View.GONE
            layoutNormal.visibility = View.VISIBLE
            etSearch.text.clear()
            searchQuery = ""
            applyFilter()
            val imm = getSystemService(InputMethodManager::class.java)
            imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
        }

        btnSearch.setOnClickListener { openSearch() }
        btnSearchClose.setOnClickListener { closeSearch() }
        btnSearchClear.setOnClickListener { etSearch.text.clear() }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                applyFilter()
            }
        })
    }

    private fun applyFilter() {
        var filtered = when (currentFilter) {
            "INCOME" -> allTransactions.filter { it.type == "INCOME" }
            "EXPENSE" -> allTransactions.filter { it.type == "EXPENSE" }
            "Makanan", "Transportasi", "Belanja" -> allTransactions.filter { it.category == currentFilter }
            else -> allTransactions
        }
        if (searchQuery.isNotEmpty()) {
            val q = searchQuery.lowercase()
            filtered = filtered.filter {
                it.note.lowercase().contains(q) || it.category.lowercase().contains(q)
            }
        }
        val isEmpty = filtered.isEmpty()
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvTransactions.visibility = if (isEmpty) View.GONE else View.VISIBLE
        adapter.updateList(groupByDate(filtered))
    }

    private fun groupByDate(transactions: List<Transaction>): List<TransactionAdapter.TransactionListItem> {
        val result = mutableListOf<TransactionAdapter.TransactionListItem>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis
        val yesterdayStart = todayStart - 86400000L
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))

        val grouped = LinkedHashMap<Long, MutableList<Transaction>>()
        for (t in transactions.sortedByDescending { it.date }) {
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = t.date
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            grouped.getOrPut(dayStart) { mutableListOf() }.add(t)
        }

        for ((dayStart, list) in grouped) {
            val dateStr = dateFormat.format(Date(dayStart))
            val label = when (dayStart) {
                todayStart -> "Hari ini - $dateStr"
                yesterdayStart -> "Kemarin - $dateStr"
                else -> dateStr
            }
            result.add(TransactionAdapter.TransactionListItem.Header(label))
            list.forEach { result.add(TransactionAdapter.TransactionListItem.Item(it)) }
        }
        return result
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_transaction
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_transaction -> true
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
}
