package com.example.pocketflow

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketflow.viewmodel.TransactionViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvBalance = findViewById<TextView>(R.id.tvTotalBalance)
        val tvIncome = findViewById<TextView>(R.id.tvTotalIncome)
        val tvExpense = findViewById<TextView>(R.id.tvTotalExpense)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val rvTransactions = findViewById<RecyclerView>(R.id.rvTransactions)
        val tvSeeAll = findViewById<TextView>(R.id.tvSeeAll)

        // Quick menu buttons
        findViewById<LinearLayout>(R.id.menuTambah).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.menuLaporan).setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.menuKategori).setOnClickListener {
            startActivity(Intent(this, TransactionActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.menuBudget).setOnClickListener {
            // coming soon
        }

        // See all transactions
        tvSeeAll.setOnClickListener {
            startActivity(Intent(this, TransactionActivity::class.java))
        }

        // RecyclerView setup
        rvTransactions.layoutManager = LinearLayoutManager(this)

        // Observe transactions
        viewModel.transactions.observe(this) { list ->
            if (list.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvTransactions.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvTransactions.visibility = View.VISIBLE
                // Adapter will be added later
            }
        }

        // Observe income
        viewModel.totalIncome.observe(this) {
            tvIncome.text = formatRupiah(it)
            updateBalance(tvBalance)
        }

        // Observe expense
        viewModel.totalExpense.observe(this) {
            tvExpense.text = formatRupiah(it)
            updateBalance(tvBalance)
        }

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_transaction -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    true
                }
                R.id.nav_report -> {
                    startActivity(Intent(this, SummaryActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, AiChatActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun updateBalance(tvBalance: TextView) {
        val income = viewModel.totalIncome.value ?: 0L
        val expense = viewModel.totalExpense.value ?: 0L
        tvBalance.text = formatRupiah(income - expense)
    }

    private fun formatRupiah(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount).replace("Rp", "Rp ")
    }
}