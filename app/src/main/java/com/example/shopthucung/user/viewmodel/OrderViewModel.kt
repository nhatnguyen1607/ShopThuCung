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
import com.google.firebase.Timestamp
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
                val totalPrice = (price * quantity).toLong()

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
                    status = "Đang xử lí",
                    bookingDate = Timestamp.now(),
                    deliveryDate = Timestamp.now()
                )

                if (paymentMethod == "VNPay") {
                    db.collection("orders")
                        .document(orderId)
                        .set(order)
                        .await()

                    val combinedOrder = hashMapOf(
                        "orderId" to orderId,
                        "userId" to userId,
                        "productId" to product.id_sanpham,
                        "product" to product,
                        "quantity" to quantity,
                        "totalPrice" to totalPrice,
                        "paymentMethod" to paymentMethod,
                        "status" to "Đang xử lí",
                        "bookingDate" to Timestamp.now(),
                        "deliveryDate" to Timestamp.now(),
                        "items" to emptyList<CartItem>()
                    )
                    db.collection("combined_orders")
                        .document(orderId)
                        .set(combinedOrder)
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
                    Log.d("OrderViewModel", "Added to orders and combined_orders: $orderId")
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

                    product.soluong -= quantity
                    product.so_luong_ban += quantity
                    productRef.update(
                        "soluong", product.soluong,
                        "so_luong_ban", product.so_luong_ban
                    ).await()

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

                if (paymentMethod == "VNPay") {
                    if (cartItems.size == 1) {
                        val cartItem = cartItems[0]
                        val product = cartItem.product!!
                        val price = if (product.giam_gia > 0) {
                            product.gia_sp - (product.gia_sp * product.giam_gia / 100)
                        } else {
                            product.gia_sp
                        }
                        val totalPrice = (price * cartItem.quantity).toLong()

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
                            paymentMethod = paymentMethod,
                            status = "Đang xử lí",
                            bookingDate = Timestamp.now(),
                            deliveryDate = Timestamp.now()
                        )

                        db.collection("orders")
                            .document(orderId)
                            .set(order)
                            .await()

                        val combinedOrder = hashMapOf(
                            "orderId" to orderId,
                            "userId" to userId,
                            "productId" to product.id_sanpham,
                            "product" to product,
                            "quantity" to cartItem.quantity,
                            "totalPrice" to totalPrice,
                            "paymentMethod" to paymentMethod,
                            "status" to "Đang xử lí",
                            "bookingDate" to Timestamp.now(),
                            "deliveryDate" to Timestamp.now(),
                            "items" to emptyList<CartItem>()
                        )
                        db.collection("combined_orders")
                            .document(orderId)
                            .set(combinedOrder)
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
                        Log.d("OrderViewModel", "Added single item order to orders and combined_orders: $orderId")
                        VNPayHelper.launchPayment(context, vnpayUrl, orderId)
                    } else {
                        val totalPrice = cartItems.sumOf { cartItem ->
                            val product = cartItem.product!!
                            val price = if (product.giam_gia > 0) {
                                product.gia_sp - (product.gia_sp * product.giam_gia / 100)
                            } else {
                                product.gia_sp
                            }
                            (price * cartItem.quantity).toLong()
                        }

                        val tempOrderId = "temp_${userId}_${System.currentTimeMillis()}"

                        val combinedOrder = hashMapOf(
                            "orderId" to tempOrderId,
                            "userId" to userId,
                            "productId" to -1,
                            "product" to null,
                            "quantity" to cartItems.sumOf { it.quantity },
                            "totalPrice" to totalPrice,
                            "paymentMethod" to paymentMethod,
                            "status" to "Đang xử lí",
                            "bookingDate" to Timestamp.now(),
                            "deliveryDate" to Timestamp.now(),
                            "items" to cartItems
                        )

                        db.collection("combined_orders")
                            .document(tempOrderId)
                            .set(combinedOrder)
                            .await()

                        val vnpayUrl = VNPayHelper.createPaymentUrl(
                            context = context,
                            orderId = tempOrderId,
                            amount = totalPrice,
                            ipAddr = "127.0.0.1",
                            orderInfo = "Thanh toan gio hang $tempOrderId"
                        )
                        val orderForPending = Order(
                            orderId = tempOrderId,
                            userId = userId,
                            productId = -1,
                            product = null,
                            quantity = cartItems.sumOf { it.quantity },
                            totalPrice = totalPrice,
                            paymentMethod = paymentMethod,
                            status = "Đang xử lí",
                            bookingDate = Timestamp.now(),
                            deliveryDate = Timestamp.now()
                        )
                        _vnpayUrls.value = listOf(tempOrderId to vnpayUrl)
                        _pendingOrders.value = listOf(orderForPending)
                        savePendingOrders(context, listOf(orderForPending))
                        Log.d("OrderViewModel", "Created combined order: $tempOrderId")
                        VNPayHelper.launchPayment(context, vnpayUrl, tempOrderId)
                    }
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
            val totalPrice = (price * cartItem.quantity).toLong()

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
                status = "Đang xử lí",
                bookingDate = Timestamp.now(),
                deliveryDate = Timestamp.now()
            )

            db.collection("orders")
                .document(orderId)
                .set(order)
                .await()

            val productRef = db.collection("product").document(product.ten_sp)
            val productSnapshot = productRef.get().await()
            if (productSnapshot.exists()) {
                product.soluong -= cartItem.quantity
                product.so_luong_ban += cartItem.quantity
                productRef.update(
                    "soluong", product.soluong,
                    "so_luong_ban", product.so_luong_ban
                ).await()
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
                val combinedOrderSnapshot = db.collection("combined_orders")
                    .document(orderId)
                    .get()
                    .await()
                var cartItems: List<CartItem> = emptyList()

                if (!combinedOrderSnapshot.exists()) {
                    throw Exception("Không tìm thấy đơn hàng với ID: $orderId trong combined_orders")
                }

                if (order == null) {
                    val savedOrders = loadPendingOrders(context)
                    order = savedOrders.find { it.orderId == orderId }
                    if (order == null) {
                        order = combinedOrderSnapshot.toObject(Order::class.java)
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

                if (order.productId == -1) {
                    // Đơn hàng gộp (từ giỏ hàng)
                    // Lấy cartItems từ combined_orders
                    val items = combinedOrderSnapshot.get("items") as? List<Map<String, Any>> ?: emptyList()
                    cartItems = items.mapNotNull { item ->
                        try {
                            val productMap = item["product"] as? Map<String, Any>
                            val product = productMap?.let {
                                Product(
                                    id_sanpham = (it["id_sanpham"] as? Long)?.toInt() ?: 0,
                                    ten_sp = it["ten_sp"] as? String ?: "",
                                    gia_sp = (it["gia_sp"] as? Long)?.toLong() ?: 0,
                                    giam_gia = (it["giam_gia"] as? Long)?.toInt() ?: 0,
                                    soluong = (it["soluong"] as? Long)?.toInt() ?: 0,
                                    so_luong_ban = (it["so_luong_ban"] as? Long)?.toInt() ?: 0,
                                    anh_sp = (it["anh_sp"] as? List<String>) ?: emptyList()
                                )
                            }
                            CartItem(
                                userId = item["userId"] as? String ?: "",
                                productId = (item["productId"] as? Long)?.toInt() ?: 0,
                                quantity = (item["quantity"] as? Long)?.toInt() ?: 0,
                                product = product,
                                cartIndex = (item["cartIndex"] as? Long)?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            Log.e("OrderViewModel", "Error parsing CartItem: ${e.message}")
                            null
                        }
                    }

                    // Kiểm tra số lượng sản phẩm
                    cartItems.forEach { cartItem ->
                        val product = cartItem.product ?: throw Exception("Sản phẩm không hợp lệ!")
                        val productRef = db.collection("product").document(product.ten_sp)
                        val productSnapshot = productRef.get().await()
                        if (!productSnapshot.exists()) {
                            throw Exception("Sản phẩm ${product.ten_sp} không tồn tại trong kho!")
                        }
                        val productSoluong = productSnapshot.getLong("soluong")?.toInt() ?: 0
                        if (productSoluong == 0) {
                            throw Exception("Hết sản phẩm ${product.ten_sp}!")
                        }
                        if (productSoluong < cartItem.quantity) {
                            throw Exception("Số lượng sản phẩm ${product.ten_sp} không đủ! Chỉ còn $productSoluong sản phẩm.")
                        }
                    }

                    // Tách đơn hàng gộp thành các đơn hàng riêng lẻ
                    val newOrderIds = mutableListOf<String>()
                    cartItems.forEach { cartItem ->
                        val product = cartItem.product!!
                        val price = if (product.giam_gia > 0) {
                            product.gia_sp - (product.gia_sp * product.giam_gia / 100)
                        } else {
                            product.gia_sp
                        }
                        val totalPrice = (price * cartItem.quantity).toLong()

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
                        val newOrderId = "${userId}_${product.id_sanpham}_$newIndex"
                        val newOrder = Order(
                            orderId = newOrderId,
                            userId = userId,
                            productId = product.id_sanpham,
                            product = product,
                            quantity = cartItem.quantity,
                            totalPrice = totalPrice,
                            paymentMethod = "VNPay",
                            status = "Đang xử lí",
                            bookingDate = Timestamp.now(),
                            deliveryDate = Timestamp.now()
                        )

                        db.collection("orders")
                            .document(newOrderId)
                            .set(newOrder)
                            .await()

                        val productRef = db.collection("product").document(product.ten_sp)
                        val productSnapshot = productRef.get().await()
                        if (productSnapshot.exists()) {
                            product.soluong -= cartItem.quantity
                            product.so_luong_ban += cartItem.quantity
                            productRef.update(
                                "soluong", product.soluong,
                                "so_luong_ban", product.so_luong_ban
                            ).await()
                        }

                        db.collection("cart")
                            .document("${userId}_${cartItem.cartIndex}")
                            .delete()
                            .await()
                        newOrderIds.add(newOrderId)
                        Log.d("OrderViewModel", "Created separate order: $newOrderId")
                    }

                    // Xóa toàn bộ giỏ hàng của user sau khi thanh toán thành công
                    val cartSnapshot = db.collection("cart")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                    cartSnapshot.documents.forEach { document ->
                        db.collection("cart")
                            .document(document.id)
                            .delete()
                            .await()
                        Log.d("OrderViewModel", "Deleted cart item: ${document.id}")
                    }

                    // Xóa đơn hàng gộp sau khi tách
                    db.collection("combined_orders")
                        .document(orderId)
                        .delete()
                        .await()
                    Log.d("OrderViewModel", "Deleted combined order: $orderId")

                    _vnpayUrls.value = emptyList()
                    _pendingOrders.value = emptyList()
                    clearPendingOrdersFromPrefs(context)
                    _successMessage.value = "Thanh toán online thành công!"
                    onSuccess()
                } else {
                    // Đơn hàng đơn lẻ (productId != -1)
                    VNPayHelper.updateOrderStatusAfterPayment(
                        orderId = orderId,
                        onSuccess = {
                            viewModelScope.launch {
                                val product = order.product ?: throw Exception("Sản phẩm không hợp lệ!")
                                val productRef = db.collection("product").document(product.ten_sp)
                                val productSnapshot = productRef.get().await()
                                if (!productSnapshot.exists()) {
                                    throw Exception("Sản phẩm ${product.ten_sp} không tồn tại trong kho!")
                                }
                                val productSoluong = productSnapshot.getLong("soluong")?.toInt() ?: 0
                                if (productSoluong == 0) {
                                    throw Exception("Hết sản phẩm ${product.ten_sp}!")
                                }
                                if (productSoluong < order.quantity) {
                                    throw Exception("Số lượng sản phẩm ${product.ten_sp} không đủ! Chỉ còn $productSoluong sản phẩm.")
                                }

                                product.soluong -= order.quantity
                                product.so_luong_ban += order.quantity
                                productRef.update(
                                    "soluong", product.soluong,
                                    "so_luong_ban", product.so_luong_ban
                                ).await()

                                // Xóa đơn hàng trong combined_orders
                                db.collection("combined_orders")
                                    .document(orderId)
                                    .delete()
                                    .await()
                                Log.d("OrderViewModel", "Deleted combined order: $orderId")

                                _vnpayUrls.value = emptyList()
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
                val combinedOrderSnapshot = db.collection("combined_orders")
                    .document(orderId)
                    .get()
                    .await()

                if (order == null) {
                    val savedOrders = loadPendingOrders(context)
                    order = savedOrders.find { it.orderId == orderId }
                    if (order == null) {
                        if (!combinedOrderSnapshot.exists()) {
                            throw Exception("Không tìm thấy đơn hàng với ID: $orderId")
                        }
                        order = combinedOrderSnapshot.toObject(Order::class.java)
                        if (order == null) {
                            throw Exception("Không tìm thấy đơn hàng với ID: $orderId")
                        }
                        _pendingOrders.value = listOf(order)
                        Log.d("OrderViewModel", "Restored order from Firestore for cancellation: $orderId")
                    } else {
                        _pendingOrders.value = listOf(order)
                        Log.d("OrderViewModel", "Restored order from SharedPreferences for cancellation: $orderId")
                    }
                }

                val items = combinedOrderSnapshot.get("items") as? List<Map<String, Any>> ?: emptyList()

                val finalOrder = hashMapOf(
                    "orderId" to order.orderId,
                    "userId" to order.userId,
                    "productId" to order.productId,
                    "product" to order.product,
                    "quantity" to order.quantity,
                    "totalPrice" to order.totalPrice,
                    "paymentMethod" to order.paymentMethod,
                    "status" to "Đã hủy",
                    "bookingDate" to order.bookingDate,
                    "deliveryDate" to order.deliveryDate,
                    "items" to items
                )

                db.collection("combined_orders")
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
            val product = cartItem.product ?: return@sumOf 0L
            val price = if (product.giam_gia > 0) {
                product.gia_sp - (product.gia_sp * product.giam_gia / 100)
            } else {
                product.gia_sp
            }
            (price * cartItem.quantity).toLong()
        }

        viewModelScope.launch {
            try {
                val querySnapshot = db.collection("combined_orders")
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
                    status = "Đang xử lí",
                    bookingDate = Timestamp.now(),
                    deliveryDate = Timestamp.now()
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
        val totalPrice = (price * quantity).toLong()

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
                    status = "Đang xử lí",
                    bookingDate = Timestamp.now(),
                    deliveryDate = Timestamp.now()
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
        return try {
            gson.fromJson(ordersJson, Array<Order>::class.java).toList()
        } catch (e: Exception) {
            Log.e("OrderViewModel", "Error parsing pending orders: ${e.message}")
            emptyList()
        }
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