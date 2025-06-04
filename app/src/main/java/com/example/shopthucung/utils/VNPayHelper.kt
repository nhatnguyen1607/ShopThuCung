package com.example.shopthucung.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.math.RoundingMode

object VNPayHelper {
    private const val vnp_TmnCode = "9RZFRI0L"
    private const val vnp_HashSecret = "W3UIXY4EMGGNQ8Q284D4AXI0IXDJAJ4Q"
    private const val vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
    private const val vnp_ReturnUrl = "myapp://payment/verify"

    fun createPaymentUrl(context: Context, orderId: String, amount: Long, ipAddr: String, orderInfo: String): String {
        try {
            val cleanAmount = amount.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
            val vnp_Amount = (cleanAmount * 100).toLong()

            val vnp_Version = "2.1.0"
            val vnp_Command = "pay"
            val vnp_IpAddr = "127.0.0.1"
            val vnp_CreateDate = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
            val vnp_ExpireDate = SimpleDateFormat("yyyyMMddHHmmss").format(Date(System.currentTimeMillis() + 15 * 60 * 1000))
            val vnp_TxnRef = orderId

            val params = TreeMap<String, String>()
            params["vnp_Version"] = vnp_Version
            params["vnp_Command"] = vnp_Command
            params["vnp_TmnCode"] = vnp_TmnCode
            params["vnp_Amount"] = vnp_Amount.toString()
            params["vnp_CreateDate"] = vnp_CreateDate
            params["vnp_ExpireDate"] = vnp_ExpireDate
            params["vnp_CurrCode"] = "VND"
            params["vnp_IpAddr"] = vnp_IpAddr
            params["vnp_Locale"] = "vn"
            params["vnp_OrderInfo"] = orderInfo
            params["vnp_OrderType"] = "250000"
            params["vnp_ReturnUrl"] = vnp_ReturnUrl
            params["vnp_TxnRef"] = vnp_TxnRef

            val signData = params.entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
            val vnp_SecureHash = hmacSHA512(vnp_HashSecret, signData)
            params["vnp_SecureHash"] = vnp_SecureHash

            return vnp_Url + "?" + params.entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
        } catch (e: Exception) {
            Log.e("VNPayHelper", "Lỗi tạo URL thanh toán: ${e.message}", e)
            throw e
        }
    }

    fun launchPayment(context: Context, url: String, orderId: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.putExtra("orderId", orderId)
        context.startActivity(intent)
    }

    fun updateOrderStatusAfterPayment(orderId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        Firebase.firestore.collection("orders")
            .document(orderId)
            .update("status", "Đang xử lí")
            .addOnSuccessListener {
                Log.i("VNPayHelper", "Cập nhật đơn hàng thành công: $orderId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("VNPayHelper", "Lỗi cập nhật đơn hàng: ${e.message}")
                onError("Lỗi cập nhật đơn hàng: ${e.message}")
            }
    }

    fun updateOrderStatusAfterCancellation(orderId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        Firebase.firestore.collection("orders")
            .document(orderId)
            .update("status", "Đã hủy")
            .addOnSuccessListener {
                Log.i("VNPayHelper", "Hủy đơn hàng thành công: $orderId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("VNPayHelper", "Lỗi hủy đơn hàng: ${e.message}")
                onError("Lỗi hủy đơn hàng: ${e.message}")
            }
    }

    internal fun hmacSHA512(key: String, data: String): String {
        try {
            val algorithm = "HmacSHA512"
            val mac = Mac.getInstance(algorithm)
            val secretKeySpec = SecretKeySpec(key.toByteArray(), algorithm)
            mac.init(secretKeySpec)
            val bytes = mac.doFinal(data.toByteArray())
            return bytes.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            Log.e("VNPayHelper", "Lỗi tính hash: ${e.message}", e)
            throw e
        }
    }
//
//    fun getClientIp(): String {
//        return "127.0.0.1" // Giả lập IP, thay bằng logic lấy IP thực nếu cần
//    }
}