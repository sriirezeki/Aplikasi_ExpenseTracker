package com.example.pocketflow.data

data class Transaction(
    val id: String = "",
    val type: String = "",
    val amount: Long = 0,
    val category: String = "",
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)