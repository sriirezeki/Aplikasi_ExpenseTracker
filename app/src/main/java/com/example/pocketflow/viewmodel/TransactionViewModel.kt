package com.example.pocketflow.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.pocketflow.data.Transaction
import com.example.pocketflow.data.TransactionRepository

class TransactionViewModel : ViewModel() {

    private val repository = TransactionRepository()

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    val totalIncome: LiveData<Long> get() = _totalIncome
    private val _totalIncome = MutableLiveData<Long>(0L)

    val totalExpense: LiveData<Long> get() = _totalExpense
    private val _totalExpense = MutableLiveData<Long>(0L)

    // C++ native functions for calculations
    external fun calculateTotal(amounts: DoubleArray, size: Int): Double
    external fun calculateBudgetPercentage(totalExpense: Double, budgetLimit: Double): Double
    external fun calculateCategoryPercentage(categoryTotal: Double, grandTotal: Double): Double

    companion object {
        init { System.loadLibrary("pocketflow") }
    }

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        repository.getAllTransactions { list ->
            _transactions.postValue(list)

            val incomeList = list.filter { it.type == "INCOME" }.map { it.amount.toDouble() }
            val expenseList = list.filter { it.type == "EXPENSE" }.map { it.amount.toDouble() }

            // Use C++ to calculate totals
            val income = if (incomeList.isEmpty()) 0.0
            else calculateTotal(incomeList.toDoubleArray(), incomeList.size)
            val expense = if (expenseList.isEmpty()) 0.0
            else calculateTotal(expenseList.toDoubleArray(), expenseList.size)

            _totalIncome.postValue(income.toLong())
            _totalExpense.postValue(expense.toLong())
        }
    }

    fun addTransaction(transaction: Transaction, onResult: (Boolean) -> Unit) {
        repository.addTransaction(transaction, onResult)
    }

    fun deleteTransaction(id: String, onResult: (Boolean) -> Unit) {
        repository.deleteTransaction(id, onResult)
    }

    fun getByCategory(): Map<String, Long> {
        val list = _transactions.value ?: return emptyMap()
        return list.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry ->
                val amounts = entry.value.map { it.amount.toDouble() }
                calculateTotal(amounts.toDoubleArray(), amounts.size).toLong()
            }
    }
}