package com.example.pocketflow

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketflow.data.Transaction
import com.example.pocketflow.viewmodel.TransactionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TransactionActivity : AppCompatActivity() {

    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter
    private var allTransactions = listOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        // RecyclerView
        val rv = findViewById<RecyclerView>(R.id.rvTransactions)
        adapter = TransactionAdapter(emptyList()) { transaction ->
            // Long press to delete
            MaterialAlertDialogBuilder(this)
                .setTitle("Hapus Transaksi?")
                .setMessage("${transaction.category} - Rp ${transaction.amount}")
                .setPositiveButton("Hapus") { _, _ ->
                    viewModel.deleteTransaction(transaction.id) {}
                }
                .setNegativeButton("Batal", null)
                .show()
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        // Filter buttons
        val filterAll = findViewById<TextView>(R.id.filterAll)
        val filterIncome = findViewById<TextView>(R.id.filterIncome)
        val filterExpense = findViewById<TextView>(R.id.filterExpense)
        val filterMakanan = findViewById<TextView>(R.id.filterMakanan)
        val filterTransportasi = findViewById<TextView>(R.id.filterTransportasi)

        fun setActiveFilter(active: TextView, others: List<TextView>) {
            active.setBackgroundResource(R.drawable.filter_active)
            active.setTextColor(0xFFFFFFFF.toInt())
            others.forEach {
                it.setBackgroundResource(R.drawable.filter_inactive)
                it.setTextColor(0xFF5B4FCF.toInt())
            }
        }

        filterAll.setOnClickListener {
            setActiveFilter(filterAll, listOf(filterIncome, filterExpense, filterMakanan, filterTransportasi))
            adapter.updateList(allTransactions)
        }
        filterIncome.setOnClickListener {
            setActiveFilter(filterIncome, listOf(filterAll, filterExpense, filterMakanan, filterTransportasi))
            adapter.updateList(allTransactions.filter { it.type == "INCOME" })
        }
        filterExpense.setOnClickListener {
            setActiveFilter(filterExpense, listOf(filterAll, filterIncome, filterMakanan, filterTransportasi))
            adapter.updateList(allTransactions.filter { it.type == "EXPENSE" })
        }
        filterMakanan.setOnClickListener {
            setActiveFilter(filterMakanan, listOf(filterAll, filterIncome, filterExpense, filterTransportasi))
            adapter.updateList(allTransactions.filter { it.category == "Makanan" })
        }
        filterTransportasi.setOnClickListener {
            setActiveFilter(filterTransportasi, listOf(filterAll, filterIncome, filterExpense, filterMakanan))
            adapter.updateList(allTransactions.filter { it.category == "Transportasi" })
        }

        // Observe data
        viewModel.transactions.observe(this) { list ->
            allTransactions = list
            adapter.updateList(list)
        }
    }
}