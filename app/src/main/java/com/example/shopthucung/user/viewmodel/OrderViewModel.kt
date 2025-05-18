package com.example.shopthucung.user.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopthucung.model.CartItem
import com.example.shopthucung.model.Order
import com.example.shopthucung.model.Product
import com.example.shopthucung.utils.VNPayHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OrderViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _pendingOrders = MutableStateFlow<List<Order>>(emptyList())
    val pendingOrders: StateFlow<List<Order>> = _pendingOrders.asStateFlow()

    private val _vnpayUrls = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // (orderId, url)
    val vnpayUrls: StateFlow<List<Pair<String, String>>> = _vnpayUrls.asStateFlow()

    fun confirmDirectOrder(product: Product, quantity: Int, paymentMethod: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _errorMessage.value = "Người dùng chưa đăng nhập!"
                    return@launch
                }

                if (paymentMethod.isBlank()) {
                    _errorMessage.value = "Phương thức thanh toán không hợp lệ!"
                    return@launch
                }

                // Kiểm tra số lượng tồn kho
                if (product.soluong < quantity) {
                    _errorMessage.value = "Số lượng sản phẩm ${product.ten_sp} không đủ! Chỉ còn ${product.soluong} sản phẩm."
                    return@launch
                }

                val price = if (product.giam_gia > 0) {
                    product.gia_sp - (product.gia_sp * product.giam_gia / 100)
                } else {
                    product.gia_sp
                }
                val totalPrice = price * quantity

                // Tạo orderId tạm thời
                val tempOrderId = "temp_${userId}_${product.id_sanpham}_${System.currentTimeMillis()}"

                val order = Order(
                    orderId = tempOrderId,
                    userId = userId,
                    productId = product.id_sanpham,
                    product = product,
                    quantity = quantity,
                    totalPrice = totalPrice,
                    paymentMethod = paymentMethod,
                    status = "Đang xử lí"
                )

                if (paymentMethod == "VNPay") {
                    // Tạo URL thanh toán VNPay nhưng chưa lưu đơn hàng
                    val vnpayUrl = VNPayHelper.createPaymentUrl(
                        orderId = tempOrderId,
                        amount = totalPrice,
                        ipAddr = VNPayHelper.getClientIp(),
                        orderInfo = "Thanh toan don hang $tempOrderId"
                    )
                    _vnpayUrls.value = listOf(tempOrderId to vnpayUrl)
                    _pendingOrders.value = listOf(order) // Lưu tạm trong pendingOrders
                } else {
                    // Lưu đơn hàng COD
                    // Tìm số thứ tự lớn nhất
                    val querySnapshot = db.collection("orders")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("productId", product.id_sanpham)
                        .get()
                        .await()

                    var maxIndex = 0
                    for (document in querySnapshot.documents) {
                        val docId = document.id
                        val index = docId.substringAfterLast("_").toIntOrNull() ?: 0
                        if (index > maxIndex) {
                            maxIndex = index
                        }
                    }

                    val newIndex = maxIndex + 1
                    val orderId = "${userId}_${product.id_sanpham}_$newIndex"
                    val finalOrder = order.copy(orderId = orderId)

                    db.collection("orders")
                        .document(orderId)
                        .set(finalOrder)
                        .await()

                    product.so_luong_ban += quantity
                    db.collection("product")
                        .document(product.ten_sp.toString())
                        .update("so_luong_ban", product.so_luong_ban)
                        .await()

                    _pendingOrders.value = emptyList()
                    _successMessage.value = "Thanh toán thành công với $paymentMethod!"
                }

            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error in confirmDirectOrder: ${e.message}", e)
                _errorMessage.value = "Lỗi khi tạo đơn hàng: ${e.message}"
            }
        }
    }

    fun confirmCartOrders(
        cartItems: List<CartItem>,
        paymentMethod: String,
        onComplete: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _errorMessage.value = "Người dùng chưa đăng nhập!"
                    return@launch
                }

                if (paymentMethod.isBlank()) {
                    _errorMessage.value = "Phương thức thanh toán không hợp lệ!"
                    return@launch
                }

                if (cartItems.isEmpty()) {
                    _errorMessage.value = "Giỏ hàng trống!"
                    return@launch
                }

                for (cartItem in cartItems) {
                    val product = cartItem.product ?: continue
                    if (product.soluong < cartItem.quantity) {
                        _errorMessage.value = "Số lượng sản phẩm ${product.ten_sp} không đủ! Chỉ còn ${product.soluong} sản phẩm."
                        return@launch
                    }
                }

                val orders = mutableListOf<Order>()
                val vnpayUrls = mutableListOf<Pair<String, String>>()
                cartItems.forEach { cartItem ->
                    val product = cartItem.product ?: return@forEach
                    val price = if (product.giam_gia > 0) {
                        product.gia_sp - (product.gia_sp * product.giam_gia / 100)
                    } else {
                        product.gia_sp
                    }
                    val totalPrice = price * cartItem.quantity

                    // Tạo orderId tạm thời
                    val tempOrderId = "temp_${userId}_${product.id_sanpham}_${System.currentTimeMillis()}"

                    val order = Order(
                        orderId = tempOrderId,
                        userId = userId,
                        productId = product.id_sanpham,
                        product = product,
                        quantity = cartItem.quantity,
                        totalPrice = totalPrice,
                        paymentMethod = paymentMethod,
                        status = "Đang xử lí"
                    )

                    if (paymentMethod == "VNPay") {
                        val vnpayUrl = VNPayHelper.createPaymentUrl(
                            orderId = tempOrderId,
                            amount = totalPrice,
                            ipAddr = VNPayHelper.getClientIp(),
                            orderInfo = "Thanh toan don hang $tempOrderId"
                        )
                        vnpayUrls.add(tempOrderId to vnpayUrl)
                        orders.add(order)
                    } else {
                        // Lưu đơn hàng COD
                        val querySnapshot = db.collection("orders")
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("productId", product.id_sanpham)
                            .get()
                            .await()

                        var maxIndex = 0
                        for (document in querySnapshot.documents) {
                            val docId = document.id
                            val index = docId.substringAfterLast("_").toIntOrNull() ?: 0
                            if (index > maxIndex) {
                                maxIndex = index
                            }
                        }

                        val newIndex = maxIndex + 1
                        val orderId = "${userId}_${product.id_sanpham}_$newIndex"
                        val finalOrder = order.copy(orderId = orderId)

                        db.collection("orders")
                            .document(orderId)
                            .set(finalOrder)
                            .await()

                        product.so_luong_ban += cartItem.quantity
                        db.collection("product")
                            .document(product.ten_sp.toString())
                            .update("so_luong_ban", product.so_luong_ban)
                            .await()

                        val docId = "${userId}_${cartItem.cartIndex}"
                        db.collection("cart")
                            .document(docId)
                            .delete()
                            .await()
                        Log.d("OrderViewModel", "Đã xóa mục giỏ hàng: $docId")
                    }
                }

                if (paymentMethod == "VNPay") {
                    _vnpayUrls.value = vnpayUrls
                    _pendingOrders.value = orders
                } else {
                    _pendingOrders.value = emptyList()
                    _successMessage.value = "Đã tạo ${orders.size} đơn hàng thành công với $paymentMethod!"
                    onComplete()
                }

            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error in confirmCartOrders: ${e.message}", e)
                _errorMessage.value = "Lỗi khi tạo đơn hàng: ${e.message}"
            }
        }
    }

    fun confirmVNPayPayment(orderId: String) {
        viewModelScope.launch {
            try {
                // Tìm đơn hàng trong pendingOrders
                val order = _pendingOrders.value.find { it.orderId == orderId }
                    ?: throw Exception("Không tìm thấy đơn hàng với ID: $orderId")

                // Tạo orderId chính thức
                val userId = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập!")
                val product = order.product ?: throw Exception("Không tìm thấy sản phẩm trong đơn hàng")
                val querySnapshot = db.collection("orders")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("productId", product.id_sanpham)
                    .get()
                    .await()

                var maxIndex = 0
                for (document in querySnapshot.documents) {
                    val docId = document.id
                    val index = docId.substringAfterLast("_").toIntOrNull() ?: 0
                    if (index > maxIndex) {
                        maxIndex = index
                    }
                }

                val newIndex = maxIndex + 1
                val finalOrderId = "${userId}_${product.id_sanpham}_$newIndex"
                val finalOrder = order.copy(orderId = finalOrderId)

                db.collection("orders")
                    .document(finalOrderId)
                    .set(finalOrder)
                    .await()

                // Cập nhật số lượng và số lượng bán
                if (product.soluong < order.quantity) {
                    throw Exception("Số lượng sản phẩm ${product.ten_sp} không đủ để hoàn thành đơn hàng")
                }
                product.so_luong_ban += order.quantity
                db.collection("product")
                    .document(product.ten_sp.toString())
                    .update("so_luong_ban", product.so_luong_ban)
                    .await()

                // Xóa mục giỏ hàng nếu có
                db.collection("cart")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                    .documents
                    .forEach { doc ->
                        doc.reference.delete().await()
                    }

                _vnpayUrls.value = emptyList()
                _pendingOrders.value = emptyList()
                _successMessage.value = "Thanh toán online thành công!"
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error in confirmVNPayPayment: ${e.message}", e)
                _errorMessage.value = "Lỗi khi xác nhận thanh toán: ${e.message}"
            }
        }
    }

    fun setPendingOrders(cartItems: List<CartItem>) {
        val orders = cartItems.mapNotNull { cartItem ->
            val product = cartItem.product ?: return@mapNotNull null
            val price = if (product.giam_gia > 0) {
                product.gia_sp - (product.gia_sp * product.giam_gia / 100)
            } else {
                product.gia_sp
            }
            val totalPrice = price * cartItem.quantity

            Order(
                orderId = "temp_${System.currentTimeMillis()}_${cartItem.cartIndex}",
                product = product,
                quantity = cartItem.quantity,
                totalPrice = totalPrice,
                paymentMethod = "",
                status = "Đang xử lí"
            )
        }
        _pendingOrders.value = orders
    }

    fun setPendingOrder(product: Product, quantity: Int) {
        val price = if (product.giam_gia > 0) {
            product.gia_sp - (product.gia_sp * product.giam_gia / 100)
        } else {
            product.gia_sp
        }
        val totalPrice = price * quantity

        val tempOrderId = "temp_${System.currentTimeMillis()}"
        val pendingOrder = Order(
            orderId = tempOrderId,
            product = product,
            quantity = quantity,
            totalPrice = totalPrice,
            paymentMethod = "",
            status = "Đang xử lí"
        )

        _pendingOrders.value = listOf(pendingOrder)
    }

    fun clearPendingOrders() {
        _pendingOrders.value = emptyList()
        _vnpayUrls.value = emptyList()
    }

    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }
}