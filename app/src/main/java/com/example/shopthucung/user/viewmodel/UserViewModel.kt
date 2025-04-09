package com.example.shopthucung.user.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopthucung.user.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val db: FirebaseFirestore, private val userId: String) : ViewModel() {

    // Trạng thái người dùng
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    // Trạng thái thông báo (thành công, lỗi, v.v.)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        fetchUser()
    }

    // Lấy thông tin người dùng từ Firestore
    private fun fetchUser() {
        viewModelScope.launch {
            db.collection("user").document(userId).get()
                .addOnSuccessListener { document ->
                    _user.value = document.toObject(User::class.java)
                }
                .addOnFailureListener { exception ->
                    _message.value = "Lỗi khi lấy thông tin người dùng: ${exception.message}"
                }
        }
    }

    // Cập nhật thông tin người dùng
    fun updateUser(updatedUser: User) {
        viewModelScope.launch {
            db.collection("user").document(userId).set(updatedUser)
                .addOnSuccessListener {
                    _user.value = updatedUser
                    _message.value = "Cập nhật thông tin thành công"
                }
                .addOnFailureListener { exception ->
                    _message.value = "Lỗi khi cập nhật thông tin: ${exception.message}"
                }
        }
    }

    // Đổi mật khẩu
    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            val currentUser = _user.value ?: return@launch
            val updatedUser = currentUser.copy(matKhau = newPassword) // TODO: Mã hóa mật khẩu trước khi lưu
            db.collection("user").document(userId).set(updatedUser)
                .addOnSuccessListener {
                    _user.value = updatedUser
                    _message.value = "Đổi mật khẩu thành công"
                }
                .addOnFailureListener { exception ->
                    _message.value = "Lỗi khi đổi mật khẩu: ${exception.message}"
                }
        }
    }

    // Xóa tài khoản
    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            db.collection("user").document(userId).delete()
                .addOnSuccessListener {
                    _message.value = "Xóa tài khoản thành công"
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    _message.value = "Lỗi khi xóa tài khoản: ${exception.message}"
                }
        }
    }

    // Xóa thông báo
    fun clearMessage() {
        _message.value = null
    }
}