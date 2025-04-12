package com.example.shopthucung.admin.repository

import com.example.shopthucung.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getUsers(onResult: (List<User>) -> Unit) {
        db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                onResult(emptyList())
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.mapNotNull { it.toObject(User::class.java)?.copy(idUser = it.id) } ?: emptyList()
            onResult(users)
        }
    }

    suspend fun getUserById(userId: String): User? {
        val snapshot = db.collection("users").document(userId).get().await()
        return snapshot.toObject(User::class.java)?.copy(idUser = snapshot.id)
    }

    fun updateUser(user: User) {
        db.collection("users").document(user.idUser).set(user)
    }
}