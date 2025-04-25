package com.example.shopthucung.user.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopthucung.model.Order
import com.example.shopthucung.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.max

class UserViewModel(private val db: FirebaseFirestore, private val uid: String) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val auth = FirebaseAuth.getInstance()

    init {
        fetchUser()
        fetchOrders()
    }

    fun fetchUser() {
        viewModelScope.launch {
            _isLoading.value = true
            println("UserViewModel: Fetching user data for uid: $uid")
            db.collection("user").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userData = document.toObject(User::class.java)
                        val email = auth.currentUser?.email ?: ""
                        _user.value = userData?.copy(email = email)
                        println("UserViewModel: User data fetched successfully: ${_user.value}")
                    } else {
                        _message.value = "Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại."
                        _user.value = null
                        println("UserViewModel: No document found for uid: $uid")
                    }
                    _isLoading.value = false
                }
                .addOnFailureListener { exception ->
                    _message.value = "Lỗi khi lấy thông tin người dùng: ${exception.message}"
                    _isLoading.value = false
                    println("UserViewModel: Failed to fetch user data: ${exception.message}")
                }
        }
    }

    fun fetchOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            db.collection("orders")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { documents ->
                    val orderList = documents.mapNotNull { doc ->
                        doc.toObject(Order::class.java).copy(orderId = doc.id)
                    }
                    _orders.value = orderList
                    _isLoading.value = false
                    println("UserViewModel: Orders fetched successfully: $orderList")
                }
                .addOnFailureListener { exception ->
                    _message.value = "Lỗi khi lấy đơn hàng: ${exception.message}"
                    _isLoading.value = false
                    println("UserViewModel: Failed to fetch orders: ${exception.message}")
                }
        }
    }

    fun refreshUser() {
        fetchUser()
    }

    fun refreshOrders() {
        fetchOrders()
    }

    fun updateUser(updatedUser: User) {
        viewModelScope.launch {
            try {
                db.collection("user").document(uid).update(
                    mapOf(
                        "diaChi" to updatedUser.diaChi,
                        "sdt" to updatedUser.sdt,
                        "hoVaTen" to updatedUser.hoVaTen,
                    )
                ).addOnSuccessListener {
                    _user.value = updatedUser
                    _message.value = "Cập nhật thông tin thành công"
                }.addOnFailureListener { exception ->
                    _message.value = "Lỗi khi cập nhật thông tin: ${exception.message}"
                }
            } catch (e: Exception) {
                _message.value = "Lỗi khi cập nhật thông tin: ${e.message}"
            }
        }
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            val userData = _user.value

            if (currentUser == null || userData == null) {
                _message.value = "Không tìm thấy người dùng"
                return@launch
            }

            currentUser.updatePassword(newPassword)
                .addOnSuccessListener {
                    val updatedUser = userData.copy(matKhau = newPassword)
                    db.collection("user").document(uid).set(updatedUser)
                        .addOnSuccessListener {
                            _user.value = updatedUser
                            _message.value = "Đổi mật khẩu thành công"
                        }
                        .addOnFailureListener { exception ->
                            _message.value = "Đổi mật khẩu trong Firestore thất bại: ${exception.message}"
                        }
                }
                .addOnFailureListener { exception ->
                    _message.value = "Đổi mật khẩu trong Authentication thất bại: ${exception.message}"
                }
        }
    }

    fun deleteAccount(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _message.value = "Không tìm thấy người dùng để xóa"
                    return@launch
                }

                val email = currentUser.email ?: ""
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
                currentUser.reauthenticate(credential)
                    .addOnSuccessListener {
                        currentUser.delete()
                            .addOnSuccessListener {
                                db.collection("user").document(uid).delete()
                                    .addOnSuccessListener {
                                        _message.value = "Xóa tài khoản thành công"
                                        onSuccess()
                                    }
                                    .addOnFailureListener { exception ->
                                        _message.value = "Lỗi khi xóa dữ liệu trong Firestore: ${exception.message}"
                                    }
                            }
                            .addOnFailureListener { exception ->
                                _message.value = "Lỗi khi xóa tài khoản trong Authentication: ${exception.message}"
                            }
                    }
                    .addOnFailureListener { exception ->
                        _message.value = "Xác thực thất bại: ${exception.message}"
                    }
            } catch (e: Exception) {
                _message.value = "Lỗi khi xóa tài khoản: ${e.message}"
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Lấy thông tin đơn hàng từ Firestore
                val orderSnapshot = db.collection("orders")
                    .document(orderId)
                    .get()
                    .await()

                val order = orderSnapshot.toObject(Order::class.java)
                    ?: throw Exception("Không tìm thấy đơn hàng với ID: $orderId")

                // Kiểm tra trạng thái hiện tại để tránh cập nhật trùng lặp
                if (newStatus == "Đã hủy" && order.status == "Đã hủy") {
                    _message.value = "Đơn hàng đã được hủy trước đó"
                    return@launch
                }

                // Kiểm tra trạng thái hợp lệ để hủy
                if (newStatus == "Đã hủy" && order.status !in listOf("Đang xử lí", "Đã xác nhận")) {
                    throw Exception("Không thể hủy đơn hàng ở trạng thái ${order.status}")
                }

                // Cập nhật trạng thái đơn hàng
                db.collection("orders")
                    .document(orderId)
                    .update("status", newStatus)
                    .await()

                // Nếu trạng thái là "Đã hủy", trả lại số lượng và giảm số lượng đã bán
                if (newStatus == "Đã hủy") {
                    val product = order.product ?: throw Exception("Không tìm thấy sản phẩm trong đơn hàng")
                    // Sử dụng giao dịch để đảm bảo cập nhật nguyên tử
                    db.runTransaction { transaction ->
                        val productRef = db.collection("product").document(product.ten_sp.toString())
                        val productSnapshot = transaction.get(productRef)
                        val currentStock = productSnapshot.getLong("soluong")?.toInt() ?: 0
                        val currentSold = productSnapshot.getLong("so_luong_ban")?.toInt() ?: 0
                        transaction.update(productRef, mapOf(
                            "soluong" to currentStock + order.quantity,
                            "so_luong_ban" to max(0, currentSold - order.quantity)
                        ))
                    }.await()
                }

                _message.value = "Cập nhật trạng thái đơn hàng thành công"
                refreshOrders()
            } catch (e: Exception) {
                _message.value = "Lỗi khi cập nhật trạng thái đơn hàng: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}