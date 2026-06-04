package com.example.pocketflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketflow.viewmodel.TransactionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

class AiChatActivity : AppCompatActivity() {

    private val viewModel: TransactionViewModel by viewModels()
    private val chatMessages = mutableListOf<Pair<String, Boolean>>() // message, isUser
    private lateinit var chatAdapter: ChatAdapter
    private val apiKey = BuildConfig.GEMINI_API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_chat)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val rvChat = findViewById<RecyclerView>(R.id.rvChat)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<TextView>(R.id.btnSend)

        chatAdapter = ChatAdapter(chatMessages)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = chatAdapter

        // Welcome message
        addAiMessage("Halo! Saya Pocket AI. Tanyakan apapun tentang keuangan kamu — saya akan bantu menganalisis pengeluaran dan memberi saran. 💜")

        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isEmpty()) return@setOnClickListener

            // Check if data is loaded
            val income = viewModel.totalIncome.value ?: 0L
            val expense = viewModel.totalExpense.value ?: 0L

            etMessage.text.clear()
            addUserMessage(message)

            if (income == 0L && expense == 0L) {
                // Data might not be loaded yet, wait briefly
                viewModel.transactions.observe(this) { list ->
                    if (list.isNotEmpty() || income > 0 || expense > 0) {
                        sendToGemini(message, rvChat)
                    } else {
                        sendToGemini(message, rvChat)
                    }
                }
            } else {
                sendToGemini(message, rvChat)
            }
        }
    }

    private fun buildFinanceContext(): String {
        val income = viewModel.totalIncome.value ?: 0L
        val expense = viewModel.totalExpense.value ?: 0L
        val balance = income - expense
        val categoryMap = viewModel.getByCategory()

        val categoryText = categoryMap.entries
            .sortedByDescending { it.value }
            .joinToString("\n") { (cat, amount) ->
                "- $cat: ${formatRupiah(amount)}"
            }

        return """
            Data keuangan pengguna:
            - Total Pemasukan: ${formatRupiah(income)}
            - Total Pengeluaran: ${formatRupiah(expense)}
            - Saldo: ${formatRupiah(balance)}
            - Pengeluaran per kategori:
            $categoryText
            
            Kamu adalah asisten keuangan pribadi bernama Pocket AI.
            Jawab dalam Bahasa Indonesia, singkat dan helpful.
            Berikan saran berdasarkan data di atas jika relevan.
        """.trimIndent()
    }

    private fun sendToGemini(userMessage: String, rvChat: RecyclerView) {
        addAiMessage("...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val context = buildFinanceContext()
                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "$context\n\nPertanyaan user: $userMessage")
                                })
                            })
                        })
                    })
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(requestBody.toString())
                writer.flush()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val aiText = json
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    withContext(Dispatchers.Main) {
                        // Replace "..." with real response
                        chatMessages[chatMessages.size - 1] = Pair(aiText, false)
                        chatAdapter.notifyItemChanged(chatMessages.size - 1)
                        rvChat.scrollToPosition(chatMessages.size - 1)
                    }
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "no error body"
                    withContext(Dispatchers.Main) {
                        chatMessages[chatMessages.size - 1] = Pair("Error $responseCode: $errorBody", false)
                        chatAdapter.notifyItemChanged(chatMessages.size - 1)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatMessages[chatMessages.size - 1] = Pair("Tidak dapat terhubung. Periksa koneksi internet.", false)
                    chatAdapter.notifyItemChanged(chatMessages.size - 1)
                }
            }
        }
    }

    private fun addUserMessage(message: String) {
        chatMessages.add(Pair(message, true))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        findViewById<RecyclerView>(R.id.rvChat).scrollToPosition(chatMessages.size - 1)
    }

    private fun addAiMessage(message: String) {
        chatMessages.add(Pair(message, false))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        findViewById<RecyclerView>(R.id.rvChat).scrollToPosition(chatMessages.size - 1)
    }

    private fun formatRupiah(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount).replace("Rp", "Rp ")
    }

    // Chat Adapter
    inner class ChatAdapter(private val messages: List<Pair<String, Boolean>>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int) = if (messages[position].second) 1 else 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layout = if (viewType == 1) R.layout.item_chat_user else R.layout.item_chat_ai
            val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun getItemCount() = messages.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.findViewById<TextView>(R.id.tvMessage).text = messages[position].first
        }
    }
}