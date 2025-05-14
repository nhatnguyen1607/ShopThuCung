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
    private val firestore: FirebaseFirestore
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val currentUser = result.user
                if (currentUser != null) {
                    val idUser = currentUser.uid
                    val userDoc = firestore.collection("user").document(idUser).get().await()
                    if (!userDoc.exists()) {
                        val userData = User(
                            diaChi = "",
                            sdt = "",
                            email = email,
                            hoVaTen = "",
                            idUser = idUser,
                            matKhau = password,
                            active = true,
                            role = "user"
                        )
                        firestore.collection("user").document(idUser).set(userData).await()
                        val newUserDoc = firestore.collection("user").document(idUser).get().await()
                        val role = newUserDoc.getString("role") ?: "user"
                        val isActive = newUserDoc.getBoolean("active") ?: true

                        if (role == "user" && isActive == true) {
                            _isLoggedIn.value = true
                            onResult(true, "Đăng nhập thành công")
                        } else if (isActive == false) {
                            auth.signOut()
                            _isLoggedIn.value = false
                            _message.value = "Tài khoản đã bị khóa, vui lòng liên hệ admin để mở khóa"
                            onResult(false, "Tài khoản đã bị khóa, vui lòng liên hệ admin để mở khóa")
                        } else {
                            auth.signOut()
                            _isLoggedIn.value = false
                            _message.value = "Chỉ người dùng có vai trò 'user' mới được phép đăng nhập"
                            onResult(false, "Chỉ người dùng có vai trò 'user' mới được phép đăng nhập")
                        }
                    } else {
                        val role = userDoc.getString("role") ?: "user"
                        val isActive = userDoc.getBoolean("active") ?: true

                        if (role == "user" && isActive == true) {
                            _isLoggedIn.value = true
                            onResult(true, "Đăng nhập thành công")
                        } else if (isActive == false) {
                            auth.signOut()
                            _isLoggedIn.value = false
                            _message.value = "Tài khoản đã bị khóa, vui lòng liên hệ admin để mở khóa"
                            onResult(false, "Tài khoản đã bị khóa, vui lòng liên hệ admin để mở khóa")
                        } else {
                            auth.signOut()
                            _isLoggedIn.value = false
                            _message.value = "Chỉ người dùng có vai trò 'user' mới được phép đăng nhập"
                            onResult(false, "Chỉ người dùng có vai trò 'user' mới được phép đăng nhập")
                        }
                    }
                } else {
                    _message.value = "Không thể đăng nhập"
                    onResult(false, "Không thể đăng nhập")
                }
            } catch (e: Exception) {
                println("LoginViewModel: Login failed with exception: ${e.message}")
                _message.value = "Đăng nhập thất bại: ${e.message}"
                onResult(false, "Đăng nhập thất bại: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
    fun setMessage(msg: String) {
        _message.value = msg
    }
}
