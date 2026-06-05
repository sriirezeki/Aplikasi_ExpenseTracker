package com.example.pocketflow

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.pocketflow.data.Transaction
import com.example.pocketflow.viewmodel.TransactionViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private val viewModel: TransactionViewModel by viewModels()
    private var selectedType = "INCOME"
    private var selectedDate = System.currentTimeMillis()
    private var selectedCategory = ""
    private lateinit var tvCategoryDisplay: TextView
    private lateinit var tvDate: TextView

    private val incomeCategories = listOf(
        "💼" to "Gaji",
        "🎁" to "Bonus",
        "📈" to "Investasi",
        "💻" to "Freelance",
        "💰" to "Lainnya"
    )

    private val expenseCategories = listOf(
        "🍔" to "Makanan",
        "🚗" to "Transportasi",
        "🛍️" to "Belanja",
        "💡" to "Tagihan",
        "💊" to "Kesehatan",
        "📚" to "Edukasi",
        "🎮" to "Hiburan",
        "💰" to "Lainnya"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)
        supportActionBar?.hide()

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val btnIncome = findViewById<TextView>(R.id.btnIncome)
        val btnExpense = findViewById<TextView>(R.id.btnExpense)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val rowCategory = findViewById<LinearLayout>(R.id.rowCategory)
        val rowDate = findViewById<LinearLayout>(R.id.rowDate)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnSave = findViewById<Button>(R.id.btnSave)
        tvCategoryDisplay = findViewById(R.id.tvCategoryDisplay)
        tvDate = findViewById(R.id.tvDate)

        btnBack.setOnClickListener { finish() }
        tvDate.text = formatDate(selectedDate)

        fun setType(type: String) {
            selectedType = type
            selectedCategory = ""
            tvCategoryDisplay.text = "Pilih Kategori"
            tvCategoryDisplay.setTextColor(0xFFAAAAAA.toInt())
            if (type == "INCOME") {
                btnIncome.setBackgroundResource(R.drawable.bg_toggle_selected)
                btnIncome.setTextColor(0xFFFFFFFF.toInt())
                btnIncome.setTypeface(null, android.graphics.Typeface.BOLD)
                btnExpense.setBackgroundResource(R.drawable.bg_toggle_unselected)
                btnExpense.setTextColor(0xFF5B4FCF.toInt())
                btnExpense.setTypeface(null, android.graphics.Typeface.NORMAL)
            } else {
                btnExpense.setBackgroundResource(R.drawable.bg_toggle_selected)
                btnExpense.setTextColor(0xFFFFFFFF.toInt())
                btnExpense.setTypeface(null, android.graphics.Typeface.BOLD)
                btnIncome.setBackgroundResource(R.drawable.bg_toggle_unselected)
                btnIncome.setTextColor(0xFF5B4FCF.toInt())
                btnIncome.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
            updateCategoryGrid()
        }

        btnIncome.setOnClickListener { setType("INCOME") }
        btnExpense.setOnClickListener { setType("EXPENSE") }

        rowCategory.setOnClickListener { showCategoryDialog() }

        rowDate.setOnClickListener {
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

        btnSave.setOnClickListener {
            val amountText = etAmount.text.toString().trim()
            if (amountText.isEmpty() || amountText == "0") {
                Toast.makeText(this, "Masukkan nominal!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedCategory.isEmpty()) {
                Toast.makeText(this, "Pilih kategori terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val transaction = Transaction(
                type = selectedType,
                amount = amountText.toLong(),
                category = selectedCategory,
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

        updateCategoryGrid()
        setupBottomNav()
    }

    private fun showCategoryDialog() {
        val categories = if (selectedType == "INCOME") incomeCategories else expenseCategories
        val labels = categories.map { "${it.first}  ${it.second}" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Kategori")
            .setItems(labels) { _, which ->
                val (emoji, name) = categories[which]
                selectedCategory = name
                tvCategoryDisplay.text = "$emoji  $name"
                tvCategoryDisplay.setTextColor(0xFF1A1A2E.toInt())
                updateCategoryGrid()
            }
            .show()
    }

    private fun updateCategoryGrid() {
        val grid = findViewById<LinearLayout>(R.id.gridCategories)
        grid.removeAllViews()
        val categories = if (selectedType == "INCOME") incomeCategories else expenseCategories
        val dm = resources.displayMetrics
        val dp4 = (4 * dm.density).toInt()
        val dp8 = (8 * dm.density).toInt()
        val dp12 = (12 * dm.density).toInt()

        var rowLayout: LinearLayout? = null
        categories.forEachIndexed { index, (emoji, name) ->
            if (index % 4 == 0) {
                rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = dp8 }
                }
                grid.addView(rowLayout)
            }

            val isSelected = name == selectedCategory
            val chip = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = ContextCompat.getDrawable(
                    this@AddTransactionActivity,
                    if (isSelected) R.drawable.bg_category_quick_selected else R.drawable.bg_category_quick
                )
                setPadding(dp4, dp12, dp4, dp12)
            }
            chip.addView(TextView(this).apply {
                text = emoji
                textSize = 20f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            chip.addView(TextView(this).apply {
                text = name
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFF5B4FCF.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            chip.setOnClickListener {
                selectedCategory = name
                tvCategoryDisplay.text = "$emoji  $name"
                tvCategoryDisplay.setTextColor(0xFF1A1A2E.toInt())
                updateCategoryGrid()
            }
            rowLayout?.addView(chip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.setMargins(dp4, 0, dp4, 0)
            })
        }

        // Fill remaining cells in the last row so items are aligned
        val remainder = categories.size % 4
        if (remainder != 0) {
            repeat(4 - remainder) {
                rowLayout?.addView(View(this), LinearLayout.LayoutParams(0, 0, 1f))
            }
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_add
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
                R.id.nav_add -> true
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

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date(timestamp))
}
