package com.example.shopthucung.user.viewmodel

import android.content.Context
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
import com.google.gson.Gson
import kotlinx.coroutines.delay
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

    private val _vnpayUrls = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val vnpayUrls: StateFlow<List<Pair<String, String>>> = _vnpayUrls.asStateFlow()

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    fun confirmDirectOrder(product: Product, quantity: Int, paymentMethod: String, context: Context) {
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

                if (product.soluong == 0) {
                    _errorMessage.value = "Hết sản phẩm ${product.ten_sp}!"
                    return@launch
                }
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

                // Lấy index lớn nhất từ Firestore
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
                    db.collection("orders")
                        .document(orderId)
                        .set(order)
                        .await()

                    val vnpayUrl = VNPayHelper.createPaymentUrl(
                        context = context,
                        orderId = orderId,
                        amount = totalPrice,
                        ipAddr = "127.0.0.1",
                        orderInfo = "Thanh toan don hang $orderId"
                    )
                    _vnpayUrls.value = listOf(orderId to vnpayUrl)
                    _pendingOrders.value = listOf(order)
                    savePendingOrders(context, listOf(order))
                    Log.d("OrderViewModel", "Added to orders: $orderId")
                    VNPayHelper.launchPayment(context, vnpayUrl, orderId)
                } else {
                    db.collection("orders")
                        .document(orderId)
                        .set(order)
                        .await()

                    val productRef = db.collection("product").document(product.ten_sp)
                    val productSnapshot = productRef.get().await()
                    if (!productSnapshot.exists()) {
                        _errorMessage.value = "Sản phẩm ${product.ten_sp} không tồn tại trong kho!"
                        return@launch
                    }

                    product.so_luong_ban += quantity
                    productRef.update("so_luong_ban", product.so_luong_ban).await()

                    _pendingOrders.value = emptyList()
                    _successMessage.value = "Thanh toán thành công với $paymentMethod!"
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Lỗi xác nhận đơn hàng: ${e.message}", e)
                _errorMessage.value = "Lỗi khi tạo đơn hàng: ${e.message}"
            }
        }
    }

    fun confirmCartOrders(
        cartItems: List<CartItem>,
        paymentMethod: String,
        context: Context,
        onComplete: suspend (List<String>) -> Unit
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

                // Kiểm tra số lượng sản phẩm
                cartItems.forEach { cartItem ->
                    val product = cartItem.product ?: return@forEach
                    if (product.soluong == 0) {
                        _errorMessage.value = "Hết sản phẩm ${product.ten_sp}!"
                        return@launch
                    }
                    if (product.soluong < cartItem.quantity) {
                        _errorMessage.value = "Số lượng sản phẩm ${product.ten_sp} không đủ! Chỉ còn ${product.soluong} sản phẩm."
                        return@launch
                    }
                }

                // Tính tổng tiền
                val totalPrice = cartItems.sumOf { cartItem ->
                    val product = cartItem.product!!
                    val price = if (product.giam_gia > 0) {
                        product.gia_sp - (product.gia_sp * product.giam_gia / 100)
                    } else {
                        product.gia_sp
                    }
                    price * cartItem.quantity
                }

                // Vì là đơn hàng gộp, productId = -1, lấy index từ Firestore
                val querySnapshot = db.collection("orders")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("productId", -1)
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
                val orderId = "${userId}_-1_$newIndex"

                val order = Order(
                    orderId = orderId,
                    userId = userId,
                    productId = -1,
                    product = null,
                    quantity = cartItems.sumOf { it.quantity },
                    totalPrice = totalPrice,
                    paymentMethod = paymentMethod,
                    status = "Đang xử lí"
                )

                if (paymentMethod == "VNPay") {
                    db.collection("orders")
                        .document(orderId)
                        .set(order)
                        .await()

                    val vnpayUrl = VNPayHelper.createPaymentUrl(
                        context = context,
                        orderId = orderId,
                        amount = totalPrice,
                        ipAddr = "127.0.0.1",
                        orderInfo = "Thanh toan gio hang $orderId"
                    )
                    _vnpayUrls.value = listOf(orderId to vnpayUrl)
                    _pendingOrders.value = listOf(order)
                    savePendingOrders(context, listOf(order))
                    Log.d("OrderViewModel", "Added combined order to orders: $orderId")
                    VNPayHelper.launchPayment(context, vnpayUrl, orderId)
                } else {
                    splitOrders(cartItems, userId, context)
                    _successMessage.value = "Đã tạo đơn hàng thành công với $paymentMethod!"
                    _pendingOrders.value = emptyList()
                    onComplete(emptyList())
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error in confirmCartOrders: ${e.message}", e)
                _errorMessage.value = "Lỗi khi tạo đơn hàng: ${e.message}"
            }
        }
    }

    private suspend fun splitOrders(cartItems: List<CartItem>, userId: String, context: Context) {
        val orderIds = mutableListOf<String>()
        cartItems.forEach { cartItem ->
            val product = cartItem.product ?: return@forEach
            val price = if (product.giam_gia > 0) {
                product.gia_sp - (product.gia_sp * product.giam_gia / 100)
            } else {
                product.gia_sp
            }
            val totalPrice = price * cartItem.quantity

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
                quantity = cartItem.quantity,
                totalPrice = totalPrice,
                paymentMethod = "COD",
                status = "Đang xử lí"
            )

            db.collection("orders")
                .document(orderId)
                .set(order)
                .await()

            val productRef = db.collection("product").document(product.ten_sp)
            val productSnapshot = productRef.get().await()
            if (productSnapshot.exists()) {
                product.so_luong_ban += cartItem.quantity
                productRef.update("so_luong_ban", product.so_luong_ban).await()
            }

            orderIds.add(orderId)
            db.collection("cart")
                .document("${userId}_${cartItem.cartIndex}")
                .delete()
                .await()
            Log.d("OrderViewModel", "Đã tạo đơn hàng riêng: $orderId")
        }
    }

    fun confirmVNPayPayment(
        orderId: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("OrderViewModel", "Current pending orders: ${_pendingOrders.value.map { it.orderId }}")
                var order = _pendingOrders.value.find { it.orderId == orderId }
                if (order == null) {
                    val savedOrders = loadPendingOrders(context)
                    order = savedOrders.find { it.orderId == orderId }
                    if (order == null) {
                        val snapshot = db.collection("orders")
                            .document(orderId)
                            .get()
                            .await()
                        order = snapshot.toObject(Order::class.java)
                        if (order == null) {
                            throw Exception("Không tìm thấy đơn hàng với ID: $orderId")
                        }
                        _pendingOrders.value = listOf(order)
                        Log.d("OrderViewModel", "Restored order from Firestore: $orderId")
                    } else {
                        _pendingOrders.value = listOf(order)
                        Log.d("OrderViewModel", "Restored order from SharedPreferences: $orderId")
                    }
                }

                val userId = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập!")

                if (order.status == "Đang xử lí" && order.totalPrice > 0) {
                    VNPayHelper.updateOrderStatusAfterPayment(
                        orderId = orderId,
                        onSuccess = {
                            viewModelScope.launch {
                                // Cập nhật trạng thái đơn hàng trong orders
                                db.collection("orders")
                                    .document(orderId)
                                    .update("status", "Đang xử lí")
                                    .await()
                                _vnpayUrls.value = _vnpayUrls.value.filter { it.first != orderId }
                                _pendingOrders.value = emptyList()
                                clearPendingOrdersFromPrefs(context)
                                _successMessage.value = "Thanh toán online thành công!"
                                onSuccess()
                            }
                        },
                        onError = { error ->
                            _errorMessage.value = error
                            onError(error)
                        }
                    )
                } else {
                    throw Exception("Trạng thái đơn hàng không hợp lệ hoặc tổng tiền không đúng")
                }
            } catch (e: Exception) {
                if (e.message?.contains("UNAVAILABLE") == true) {
                    delay(2000)
                    confirmVNPayPayment(orderId, context, onSuccess, onError)
                } else {
                    Log.e("OrderViewModel", "Lỗi xác nhận thanh toán: ${e.message}", e)
                    _errorMessage.value = "Lỗi khi xác nhận thanh toán: ${e.message}"
                    onError("Lỗi khi xác nhận thanh toán: ${e.message}")
                }
            }
        }
    }

    fun cancelVNPayPayment(
        orderId: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("OrderViewModel", "Attempting to cancel order: $orderId")
                var order = _pendingOrders.value.find { it.orderId == orderId }
                if (order == null) {
                    val savedOrders = loadPendingOrders(context)
                    order = savedOrders.find { it.orderId == orderId }
                    if (order == null) {
                        val snapshot = db.collection("orders")
                            .document(orderId)
                            .get()
                            .await()
                        order = snapshot.toObject(Order::class.java)
                        if (order == null) {
                            throw Exception("Không tìm thấy đơn hàng với ID: $orderId")
                        }
                        Log.d("OrderViewModel", "Restored order from Firestore for cancellation: $orderId")
                    } else {
                        _pendingOrders.value = listOf(order)
                        Log.d("OrderViewModel", "Restored order from SharedPreferences for cancellation: $orderId")
                    }
                }

                val finalOrder = order.copy(status = "Đã hủy")
                db.collection("orders")
                    .document(orderId)
                    .set(finalOrder)
                    .await()

                _vnpayUrls.value = _vnpayUrls.value.filter { it.first != orderId }
                _pendingOrders.value = _pendingOrders.value.filter { it.orderId != orderId }
                clearPendingOrdersFromPrefs(context)
                _successMessage.value = "Đơn hàng đã được hủy"
                onSuccess()
            } catch (e: Exception) {
                if (e.message?.contains("UNAVAILABLE") == true) {
                    delay(2000)
                    cancelVNPayPayment(orderId, context, onSuccess, onError)
                } else {
                    Log.e("OrderViewModel", "Lỗi hủy thanh toán: ${e.message}", e)
                    _errorMessage.value = "Lỗi khi hủy thanh toán: ${e.message}"
                    onError("Lỗi khi hủy thanh toán: ${e.message}")
                }
            }
        }
    }

    fun setPendingOrders(cartItems: List<CartItem>, context: Context) {
        val userId = auth.currentUser?.uid ?: return
        val totalPrice = cartItems.sumOf { cartItem ->
            val product = cartItem.product!!
            val price = if (product.giam_gia > 0) {
                product.gia_sp - (product.gia_sp * product.giam_gia / 100)
            } else {
                product.gia_sp
            }
            price * cartItem.quantity
        }

        // Lấy index từ Firestore cho productId = -1
        viewModelScope.launch {
            try {
                val querySnapshot = db.collection("orders")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("productId", -1)
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
                val orderId = "${userId}_-1_$newIndex"
                val order = Order(
                    orderId = orderId,
                    userId = userId,
                    productId = -1,
                    product = null,
                    quantity = cartItems.sumOf { it.quantity },
                    totalPrice = totalPrice,
                    paymentMethod = "",
                    status = "Đang xử lí"
                )
                _pendingOrders.value = listOf(order)
                savePendingOrders(context, listOf(order))
                Log.d("OrderViewModel", "Set pending order: $orderId")
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Lỗi setPendingOrders: ${e.message}", e)
            }
        }
    }

    fun setPendingOrder(product: Product, quantity: Int, context: Context) {
        val userId = auth.currentUser?.uid ?: return
        val price = if (product.giam_gia > 0) {
            product.gia_sp - (product.gia_sp * product.giam_gia / 100)
        } else {
            product.gia_sp
        }
        val totalPrice = price * quantity

        // Lấy index từ Firestore
        viewModelScope.launch {
            try {
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
                    paymentMethod = "",
                    status = "Đang xử lí"
                )
                _pendingOrders.value = listOf(order)
                savePendingOrders(context, listOf(order))
                Log.d("OrderViewModel", "Set pending order: $orderId")
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Lỗi setPendingOrder: ${e.message}", e)
            }
        }
    }

    fun clearPendingOrders() {
        _pendingOrders.value = emptyList()
        _vnpayUrls.value = emptyList()
        Log.d("OrderViewModel", "Cleared pending orders")
    }

    fun savePendingOrders(context: Context, orders: List<Order>) {
        val prefs = context.getSharedPreferences("ShopThucungPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val gson = Gson()
        val ordersJson = gson.toJson(orders)
        editor.putString("pending_orders", ordersJson)
        editor.apply()
    }

    fun loadPendingOrders(context: Context): List<Order> {
        val prefs = context.getSharedPreferences("ShopThucungPrefs", Context.MODE_PRIVATE)
        val ordersJson = prefs.getString("pending_orders", "[]") ?: "[]"
        val gson = Gson()
        return gson.fromJson(ordersJson, Array<Order>::class.java).toList()
    }

    fun clearPendingOrdersFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences("ShopThucungPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove("pending_orders")
        editor.apply()
    }

    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }
}