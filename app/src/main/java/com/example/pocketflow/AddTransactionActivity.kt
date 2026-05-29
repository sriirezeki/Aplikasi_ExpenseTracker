package com.example.pocketflow

import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.pocketflow.data.Transaction
import com.example.pocketflow.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private val viewModel: TransactionViewModel by viewModels()
    private var selectedType = "INCOME"
    private var selectedDate = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val btnIncome = findViewById<Button>(R.id.btnIncome)
        val btnExpense = findViewById<Button>(R.id.btnExpense)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Back button
        btnBack.setOnClickListener { finish() }

        // Set today's date
        tvDate.text = formatDate(selectedDate)
        tvDate.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(
                this,
                { _, year, month, day ->
                    cal.set(year, month, day)
                    selectedDate = cal.timeInMillis
                    tvDate.text = formatDate(selectedDate)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Category spinner
        val categories = listOf(
            "Makanan", "Transportasi", "Belanja",
            "Tagihan", "Kesehatan", "Edukasi",
            "Hiburan", "Lainnya"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Type toggle
        btnIncome.setOnClickListener {
            selectedType = "INCOME"
            btnIncome.backgroundTintList = getColorStateList(R.color.purple_primary)
            btnIncome.setTextColor(getColor(android.R.color.white))
            btnExpense.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFE0E0E0.toInt())
            btnExpense.setTextColor(0xFF888888.toInt())
        }

        btnExpense.setOnClickListener {
            selectedType = "EXPENSE"
            btnExpense.backgroundTintList = getColorStateList(R.color.purple_primary)
            btnExpense.setTextColor(getColor(android.R.color.white))
            btnIncome.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFE0E0E0.toInt())
            btnIncome.setTextColor(0xFF888888.toInt())
        }

        // Save transaction
        btnSave.setOnClickListener {
            val amountText = etAmount.text.toString().trim()
            if (amountText.isEmpty()) {
                Toast.makeText(this, "Masukkan nominal!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val transaction = Transaction(
                type = selectedType,
                amount = amountText.toLong(),
                category = spinnerCategory.selectedItem.toString(),
                note = etNote.text.toString().trim(),
                date = selectedDate
            )

            btnSave.isEnabled = false
            viewModel.addTransaction(transaction) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Transaksi disimpan! ✓", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        btnSave.isEnabled = true
                        Toast.makeText(this, "Gagal menyimpan, coba lagi.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }
}