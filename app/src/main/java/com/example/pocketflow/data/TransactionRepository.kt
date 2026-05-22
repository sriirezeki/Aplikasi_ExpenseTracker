package com.example.pocketflow.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TransactionRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("transactions")

    fun addTransaction(transaction: Transaction, onResult: (Boolean) -> Unit) {
        val doc = collection.document()
        val data = transaction.copy(id = doc.id)
        doc.set(data)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun deleteTransaction(id: String, onResult: (Boolean) -> Unit) {
        collection.document(id)
            .delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun getAllTransactions(onResult: (List<Transaction>) -> Unit) {
        collection
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(Transaction::class.java)
                } ?: emptyList()
                onResult(list)
            }
    }
}