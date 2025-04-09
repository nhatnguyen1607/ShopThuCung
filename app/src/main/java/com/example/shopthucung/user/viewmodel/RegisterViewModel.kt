package com.example.shopthucung.user.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    // Trạng thái gửi email xác nhận
    private val _verificationEmailSent = MutableStateFlow(false)
    val verificationEmailSent: StateFlow<Boolean> = _verificationEmailSent

    // Trạng thái đăng ký hoàn tất (email đã được xác minh)
    private val _registrationComplete = MutableStateFlow(false)
    val registrationComplete: StateFlow<Boolean> = _registrationComplete

    // Đăng ký và gửi email xác nhận
    fun registerAndSendVerificationEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // Đăng ký người dùng với Firebase Authentication
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw IllegalStateException("Không thể tạo người dùng!")

                // Gửi email xác minh
                user.sendEmailVerification().await()

                // Đăng xuất ngay sau khi gửi email xác minh
                auth.signOut()

                _verificationEmailSent.value = true
                onResult(true, "Email xác minh đã được gửi! Vui lòng kiểm tra hộp thư và nhấp vào liên kết để kích hoạt tài khoản.")
            } catch (e: Exception) {
                // Nếu có lỗi, xóa tài khoản đã tạo (nếu tồn tại)
                auth.currentUser?.delete()?.await()
                onResult(false, "Đăng ký thất bại: ${e.message}")
            }
        }
    }

    // Kiểm tra trạng thái xác minh email
    fun checkEmailVerification(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // Đăng nhập lại để kiểm tra trạng thái
                auth.signInWithEmailAndPassword(email, password).await()
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    onResult(false, "Không thể đăng nhập để kiểm tra! Vui lòng thử lại.")
                    return@launch
                }

                // Làm mới thông tin người dùng
                currentUser.reload().await()

                if (currentUser.isEmailVerified) {
                    _verificationEmailSent.value = false
                    _registrationComplete.value = true
                    onResult(true, "Đăng ký thành công! Tài khoản đã được kích hoạt.")
                } else {
                    auth.signOut() // Đăng xuất nếu chưa xác minh
                    onResult(false, "Email chưa được xác minh! Vui lòng kiểm tra hộp thư và nhấp vào liên kết.")
                }
            } catch (e: Exception) {
                onResult(false, "Kiểm tra thất bại: ${e.message}")
            }
        }
    }

    // Đăng xuất người dùng
    fun signOut() {
        auth.signOut()
        _verificationEmailSent.value = false
        _registrationComplete.value = false
    }
}