package com.example.shopthucung.model

import com.google.firebase.Timestamp

data class Order(
    val orderId: String = "", // ID của đơn hàng, ví dụ: "${userId}_${timestamp}"
    val userId: String = "",  // ID của người dùng
    val productId: Int = 0,   // ID của sản phẩm
    val product: Product? = null, // Thông tin sản phẩm
    val quantity: Int = 0,    // Số lượng
    val totalPrice: Long = 0,  // Tổng giá (sản phẩm x số lượng, sau khi áp dụng giảm giá)
    val paymentMethod: String = "", // Phương thức thanh toán: "COD" hoặc "VNPay"
    val status: String = "Pending", // Trạng thái đơn hàng: "Pending", "Confirmed", "Delivered", "Cancelled"
    val timestamp: Timestamp = Timestamp.now() // Thời gian tạo đơn hàng
)