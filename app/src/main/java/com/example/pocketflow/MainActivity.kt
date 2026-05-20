package com.example.pocketflow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pocketflow.databinding.ActivityMainBinding
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGreeting()
        setupDummyBalance()
        setupClickListeners()
    }

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> "Selamat pagi,"
            hour < 17 -> "Selamat siang,"
            else      -> "Selamat malam,"
        }
    }

    private fun setupDummyBalance() {
        val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))
        binding.tvTotalBalance.text = "Rp ${fmt.format(4_250_000)}"
        binding.tvTotalIncome.text  = "Rp ${fmt.format(6_500_000)}"
        binding.tvTotalExpense.text = "Rp ${fmt.format(2_250_000)}"
    }

    private fun setupClickListeners() {
        binding.menuTambah.setOnClickListener {
            // TODO: buka halaman tambah transaksi
        }
        binding.tvSeeAll.setOnClickListener {
            // TODO: buka halaman semua transaksi
        }
        binding.bottomNav.setOnItemSelectedListener { true }
    }
}