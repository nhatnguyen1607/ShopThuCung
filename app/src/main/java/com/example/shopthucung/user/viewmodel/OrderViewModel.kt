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

    private val _vnpayUrls =
        MutableStateFlow<List<Pair<String, String>>>(emptyList()) // (orderId, url)
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

                val order = Order(
                    orderId = orderId,
                    userId = userId,
                    productId = product.id_sanpham,
                    product = product,
                    quantity = quantity,
                    totalPrice = totalPrice,
                    paymentMethod = paymentMethod,
                    status = "Đang xử lí"
                )

                if (paymentMethod == "VNPay") {
                    // Tạo URL thanh toán VNPay
                    val vnpayUrl = VNPayHelper.createPaymentUrl(
                        orderId = orderId,
                        amount = totalPrice,
                        ipAddr = VNPayHelper.getClientIp(),
                        orderInfo = "Thanh toan don hang $orderId"
                    )
                    _vnpayUrls.value = listOf(orderId to vnpayUrl)

                    // Lưu đơn hàng tạm thời
                    db.collection("orders")
                        .document(orderId)
                        .set(order)
                        .await()
                } else {
                    // Lưu đơn hàng COD
                    db.collection("orders")
                        .document(orderId)
                        .set(order)
                        .await()

                    // Cập nhật số lượng và số lượng bán
                    product.so_luong_ban += order.quantity
                    db.collection("product")
                        .document(product.ten_sp.toString())
                        .set(product)
                        .await()

                    _pendingOrders.value = emptyList()
                    _successMessage.value = "Thanh toán thành công với $paymentMethod!"
                }

            } catch (e: Exception) {
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

                // Kiểm tra số lượng tồn kho cho tất cả sản phẩm trong giỏ hàng
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
                    Log.d("OrderViewModel", "Tạo đơn hàng với ID: $orderId")

                    val order = Order(
                        orderId = orderId,
                        userId = userId,
                        productId = product.id_sanpham,
                        product = product,
                        quantity = cartItem.quantity,
                        totalPrice = totalPrice,
                        paymentMethod = paymentMethod,
                        status = "Đang xử lí"
                    )

                    if (paymentMethod == "VNPay") {
                        // Tạo URL thanh toán VNPay
                        val vnpayUrl = VNPayHelper.createPaymentUrl(
                            orderId = orderId,
                            amount = totalPrice,
                            ipAddr = VNPayHelper.getClientIp(),
                            orderInfo = "Thanh toan don hang $orderId"
                        )
                        vnpayUrls.add(orderId to vnpayUrl)
                        Log.d("OrderViewModel", "Tạo URL VNPay: $vnpayUrl")

                        // Lưu đơn hàng tạm thời
                        db.collection("orders")
                            .document(orderId)
                            .set(order)
                            .await()
                    } else {
                        // Lưu đơn hàng COD
                        db.collection("orders")
                            .document(orderId)
                            .set(order)
                            .await()

                        // Cập nhật số lượng và số lượng bán
                        product.so_luong_ban += order.quantity
                        db.collection("product")
                            .document(product.ten_sp.toString())
                            .set(product)
                            .await()

                        // Xóa mục trong giỏ hàng
                        val docId = "${userId}_${cartItem.cartIndex}"
                        db.collection("cart")
                            .document(docId)
                            .delete()
                            .await()
                        Log.d("OrderViewModel", "Đã xóa mục giỏ hàng: $docId")
                    }

                    orders.add(order)
                }

                if (paymentMethod == "VNPay") {
                    _vnpayUrls.value = vnpayUrls
                } else {
                    _pendingOrders.value = emptyList()
                    _successMessage.value =
                        "Đã tạo ${orders.size} đơn hàng thành công với $paymentMethod!"
                    onComplete()
                }

            } catch (e: Exception) {
                Log.e("OrderViewModel", "Lỗi khi tạo đơn hàng: ${e.message}", e)
                _errorMessage.value = "Lỗi khi tạo đơn hàng: ${e.message}"
            }
        }
    }

    fun confirmVNPayPayment(orderId: String) {
        viewModelScope.launch {
            try {
                // Lấy thông tin đơn hàng từ Firestore
                val orderSnapshot = db.collection("orders")
                    .document(orderId)
                    .get()
                    .await()

                val order = orderSnapshot.toObject(Order::class.java)
                    ?: throw Exception("Không tìm thấy đơn hàng với ID: $orderId")

                // Cập nhật trạng thái đơn hàng
                db.collection("orders")
                    .document(orderId)
                    .update("status", "Đã xác nhận")
                    .await()

                // Cập nhật số lượng và số lượng bán của sản phẩm
                val product = order.product ?: throw Exception("Không tìm thấy sản phẩm trong đơn hàng")
                if (product.soluong < order.quantity) {
                    throw Exception("Số lượng sản phẩm ${product.ten_sp} không đủ để hoàn thành đơn hàng")
                }
                product.so_luong_ban += order.quantity

                db.collection("product")
                    .document(product.ten_sp.toString())
                    .set(product)
                    .await()

                // Xóa mục giỏ hàng nếu có
                val userId = auth.currentUser?.uid ?: return@launch
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
                _successMessage.value = "Thanh toán VNPay thành công!"
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Lỗi khi xác nhận VNPay: ${e.message}", e)
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