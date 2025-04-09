package com.example.shopthucung.user.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopthucung.user.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel(
    private val firestore: FirebaseFirestore,
    private val activity: Activity
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun registerUser(email: String, password: String, hoVaTen: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                println("RegisterViewModel: Attempting to register with email: $email")
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val currentUser = result.user
                if (currentUser != null) {
                    val idUser = currentUser.uid
                    println("RegisterViewModel: Registration successful, user UID: $idUser")
                    val userData = User(
                        diaChi = "",
                        sdt = "",
                        email = email,
                        hoVaTen = hoVaTen,
                        idUser = idUser,
                        matKhau = password
                    )
                    // Sử dụng UID làm ID của document trong Firestore
                    firestore.collection("user").document(idUser).set(userData).await()
                    println("RegisterViewModel: Document created successfully for UID: $idUser")
                    _isRegistered.value = true
                    onResult(true, "Đăng ký thành công, vui lòng đăng nhập")
                } else {
                    println("RegisterViewModel: Registration failed, no user found")
                    onResult(false, "Không thể đăng ký")
                }
            } catch (e: Exception) {
                println("RegisterViewModel: Registration failed with exception: ${e.message}")
                _message.value = "Đăng ký thất bại: ${e.message}"
                onResult(false, "Đăng ký thất bại: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}