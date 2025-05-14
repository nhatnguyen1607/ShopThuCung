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

class RegisterViewModel(
    private val firestore: FirebaseFirestore,
    private val activity: Activity
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _pendingUserData = MutableStateFlow<User?>(null)
    private val _pendingUserId = MutableStateFlow<String?>(null)

    fun registerUser(
        email: String,
        password: String,
        hoVaTen: String,
        diaChi: String,
        sdt: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                println("RegisterViewModel: Attempting to register with email: $email")
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val currentUser = result.user
                if (currentUser != null) {
                    val idUser = currentUser.uid
                    println("RegisterViewModel: Registration successful, user UID: $idUser")

                    // Lưu dữ liệu người dùng tạm thời
                    val userData = User(
                        diaChi = diaChi,
                        sdt = sdt,
                        email = email,
                        hoVaTen = hoVaTen,
                        idUser = idUser,
                        matKhau = password,
                        active = true,
                        role = "user"
                    )
                    _pendingUserData.value = userData
                    _pendingUserId.value = idUser

                    // Gửi email xác minh
                    currentUser.sendEmailVerification().await()
                    println("RegisterViewModel: Verification email sent to $email")
                    onResult(true, "Vui lòng kiểm tra email $email để xác minh tài khoản")
                } else {
                    println("RegisterViewModel: Registration failed, no user found")
                    _message.value = "Không thể đăng ký"
                    onResult(false, "Không thể đăng ký")
                }
            } catch (e: Exception) {
                println("RegisterViewModel: Registration failed with exception: ${e.message}")
                _message.value = "Đăng ký thất bại: ${e.message}"
                onResult(false, "Đăng ký thất bại: ${e.message}")
            }
        }
    }

    fun verifyEmail(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Làm mới trạng thái người dùng để kiểm tra xác minh email
                    currentUser.reload().await()
                    if (currentUser.isEmailVerified) {
                        println("RegisterViewModel: Email verification successful")
                        val userData = _pendingUserData.value
                        val idUser = _pendingUserId.value
                        if (userData != null && idUser != null) {
                            // Lưu dữ liệu người dùng vào Firestore
                            firestore.collection("user").document(idUser).set(userData).await()
                            println("RegisterViewModel: Document created successfully for UID: $idUser")
                            _isRegistered.value = true
                            _pendingUserData.value = null
                            _pendingUserId.value = null
                            onResult(true, "Đăng ký thành công, vui lòng đăng nhập")
                        } else {
                            println("RegisterViewModel: No pending user data found")
                            _message.value = "Dữ liệu người dùng không hợp lệ"
                            onResult(false, "Dữ liệu người dùng không hợp lệ")
                        }
                    } else {
                        println("RegisterViewModel: Email not verified")
                        _message.value = "Vui lòng xác minh email trước khi tiếp tục"
                        onResult(false, "Vui lòng xác minh email trước khi tiếp tục")
                    }
                } else {
                    println("RegisterViewModel: No current user found")
                    _message.value = "Không tìm thấy người dùng"
                    onResult(false, "Không tìm thấy người dùng")
                }
            } catch (e: Exception) {
                println("RegisterViewModel: Email verification failed with exception: ${e.message}")
                _message.value = "Xác minh thất bại: ${e.message}"
                onResult(false, "Xác minh thất bại: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}