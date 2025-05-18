package com.example.shopthucung.user.viewmodel

import androidx.lifecycle.ViewModel
import com.example.shopthucung.model.Notifications
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationViewModel : ViewModel() {
    private val _notifications = MutableStateFlow<List<Notifications>>(emptyList())
    val notifications: StateFlow<List<Notifications>> = _notifications

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        setupAuthStateListener()
    }

    private fun setupAuthStateListener() {
        val auth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUserId = firebaseAuth.currentUser?.uid ?: ""
            println("Trạng thái đăng nhập thay đổi - UID hiện tại: $currentUserId")
            if (currentUserId.isNotEmpty()) {
                fetchNotifications(currentUserId)
            } else {
                println("Không có UID, người dùng chưa đăng nhập")
                _notifications.value = emptyList()
            }
        }
        auth.addAuthStateListener(authStateListener!!)
    }

    private fun fetchNotifications(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("notifications")
            .whereEqualTo("idUser", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Lỗi khi lắng nghe thông báo: ${e.message}")
                    return@addSnapshotListener
                }

                val notificationList = snapshot?.toObjects(Notifications::class.java) ?: emptyList()
                println("Danh sách thông báo mới cho user $userId: $notificationList")
                _notifications.value = notificationList
            }
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let {
            FirebaseAuth.getInstance().removeAuthStateListener(it)
        }
    }
}