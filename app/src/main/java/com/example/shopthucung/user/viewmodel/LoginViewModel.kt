package com.example.shopthucung.user.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopthucung.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(
    private val firestore: FirebaseFirestore,
//    private val activity: Activity
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                println("LoginViewModel: Attempting to login with email: $email")
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val currentUser = result.user
                if (currentUser != null) {
                    val idUser = currentUser.uid
                    println("LoginViewModel: Login successful, user UID: $idUser")
                    val userDoc = firestore.collection("user").document(idUser).get().await()
                    if (!userDoc.exists()) {
                        println("LoginViewModel: Document does not exist, creating new document for UID: $idUser")
                        val userData = User(
                            diaChi = "",
                            sdt = "",
                            email = email,
                            hoVaTen = "",
                            idUser = idUser,
                            matKhau = password
                        )
                        firestore.collection("user").document(idUser).set(userData).await()
                        println("LoginViewModel: Document created successfully for UID: $idUser")
                    } else {
                        println("LoginViewModel: Document already exists for UID: $idUser")
                    }
                    _isLoggedIn.value = true
                    onResult(true, "Đăng nhập thành công")
                } else {
                    println("LoginViewModel: Login failed, no user found")
                    onResult(false, "Không thể đăng nhập")
                }
            } catch (e: Exception) {
                println("LoginViewModel: Login failed with exception: ${e.message}")
                _message.value = "Đăng nhập thất bại: ${e.message}"
                onResult(false, "Đăng nhập thất bại: ${e.message}")
            }
        }
    }

    fun logout() {
        auth.signOut()
        _isLoggedIn.value = false
    }

    fun clearMessage() {
        _message.value = null
    }
}