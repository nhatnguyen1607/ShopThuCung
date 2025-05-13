package com.example.shopthucung.user.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopthucung.model.Order
import com.example.shopthucung.model.Product
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
            try {
                _isLoading.value = true
                println("UserViewModel: Fetching user data for uid: $uid")
                val document = db.collection("user").document(uid).get().await()
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
            } catch (e: Exception) {
                _message.value = "Lỗi khi lấy thông tin người dùng: ${e.message}"
                println("UserViewModel: Failed to fetch user data: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchOrders() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                println("UserViewModel: Fetching orders for uid: $uid")
                val documents = db.collection("orders")
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()

                val orderList = documents.mapNotNull { doc ->
                    try {
                        // Tự ánh xạ dữ liệu để xử lý trường anh_sp
                        val orderData = doc.data
                        val productData = orderData["product"] as? Map<String, Any> ?: return@mapNotNull null
                        val anhSpRaw = productData["anh_sp"]

                        // Xử lý trường anh_sp linh hoạt
                        val anhSp: List<String> = when (anhSpRaw) {
                            is List<*> -> anhSpRaw.filterIsInstance<String>()
                            is String -> listOf(anhSpRaw)
                            else -> emptyList()
                        }

                        // Tạo đối tượng Product
                        val product = Product(
                            id_sanpham = (productData["id_sanpham"] as? Number)?.toInt() ?: 0,
                            ten_sp = productData["ten_sp"] as? String ?: "",
                            gia_sp = (productData["gia_sp"] as? Number)?.toLong() ?: 0L,
                            giam_gia = (productData["giam_gia"] as? Number)?.toInt() ?: 0,
                            anh_sp = anhSp,
                            mo_ta = productData["mo_ta"] as? String ?: "",
                            soluong = (productData["soluong"] as? Number)?.toInt() ?: 0,
                            so_luong_ban = (productData["so_luong_ban"] as? Number)?.toInt() ?: 0,
                            danh_gia = (productData["danh_gia"] as? Number)?.toFloat() ?: 0f,
                            firestoreId = productData["firestoreId"] as? String ?: ""
                        )

                        // Tạo đối tượng Order
                        Order(
                            orderId = doc.id,
                            userId = orderData["userId"] as? String ?: "",
                            product = product,
                            quantity = (orderData["quantity"] as? Number)?.toInt() ?: 1,
                            totalPrice = (orderData["totalPrice"] as? Number)?.toLong() ?: 0L,
                            status = orderData["status"] as? String ?: "Đang xử lí",
                            bookingDate = orderData["bookingDate"] as? com.google.firebase.Timestamp,
                            deliveryDate = orderData["deliveryDate"] as? com.google.firebase.Timestamp,
                            paymentMethod = orderData["paymentMethod"] as? String ?: "COD"
                        )
                    } catch (e: Exception) {
                        println("UserViewModel: Error deserializing order ${doc.id}: ${e.message}")
                        null
                    }
                }

                _orders.value = orderList
                println("UserViewModel: Orders fetched successfully: $orderList")
            } catch (e: Exception) {
                _message.value = "Lỗi khi lấy đơn hàng: ${e.message}"
                println("UserViewModel: Failed to fetch orders: ${e.message}")
            } finally {
                _isLoading.value = false
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
                _isLoading.value = true
                db.collection("user").document(uid).update(
                    mapOf(
                        "diaChi" to updatedUser.diaChi,
                        "sdt" to updatedUser.sdt,
                        "hoVaTen" to updatedUser.hoVaTen,
                    )
                ).await()
                _user.value = updatedUser
                _message.value = "Cập nhật thông tin thành công"
            } catch (e: Exception) {
                _message.value = "Lỗi khi cập nhật thông tin: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUser = auth.currentUser
                val userData = _user.value

                if (currentUser == null || userData == null) {
                    _message.value = "Không tìm thấy người dùng"
                    return@launch
                }

                currentUser.updatePassword(newPassword).await()
                val updatedUser = userData.copy(matKhau = newPassword)
                db.collection("user").document(uid).set(updatedUser).await()
                _user.value = updatedUser
                _message.value = "Đổi mật khẩu thành công"
            } catch (e: Exception) {
                _message.value = "Đổi mật khẩu thất bại: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAccount(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _message.value = "Không tìm thấy người dùng để xóa"
                    return@launch
                }

                val email = currentUser.email ?: ""
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
                currentUser.reauthenticate(credential).await()
                currentUser.delete().await()
                db.collection("user").document(uid).delete().await()
                _message.value = "Xóa tài khoản thành công"
                onSuccess()
            } catch (e: Exception) {
                _message.value = "Lỗi khi xóa tài khoản: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
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