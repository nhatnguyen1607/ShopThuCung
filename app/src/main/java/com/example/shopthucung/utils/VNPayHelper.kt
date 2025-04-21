package com.example.shopthucung.utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object VNPayHelper {
    private const val TmnCode = "GHHNT2HB" // Xác nhận lại
    private const val HashSecret = "BAGAOHAPRHKQZASKQZASVPRSAKPXNYXS" // Xác nhận lại
    private const val VNPayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
    private const val ReturnUrl = "https://yourapp.com/vnpay_return"

    fun createPaymentUrl(
        orderId: String,
        amount: Long,
        ipAddr: String,
        orderInfo: String = "Thanh toan don hang $orderId"
    ): String {
        val vnpParams = sortedMapOf<String, String>()
        vnpParams["vnp_Version"] = "2.1.0"
        vnpParams["vnp_Command"] = "pay"
        vnpParams["vnp_TmnCode"] = TmnCode
        vnpParams["vnp_Amount"] = (amount * 100).toString()
        vnpParams["vnp_CurrCode"] = "VND"
        vnpParams["vnp_TxnRef"] = orderId
        vnpParams["vnp_OrderInfo"] = orderInfo.replace(" ", "_") 
        vnpParams["vnp_OrderType"] = "250000"
        vnpParams["vnp_Locale"] = "vn"
        vnpParams["vnp_ReturnUrl"] = ReturnUrl
        vnpParams["vnp_IpAddr"] = ipAddr
        vnpParams["vnp_CreateDate"] = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        vnpParams["vnp_ExpireDate"] = SimpleDateFormat("yyyyMMddHHmmss").format(
            Date(System.currentTimeMillis() + 30 * 60 * 1000)
        )

        val signData = vnpParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        println("signData: $signData")
        val vnpSecureHash = hmacSHA512(HashSecret, signData)
        println("vnp_SecureHash: $vnpSecureHash")
        vnpParams["vnp_SecureHash"] = vnpSecureHash

        // Encode tham số khi tạo URL
        val queryString = vnpParams.entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8.name())}" }
        return "$VNPayUrl?$queryString"
    }

    private fun hmacSHA512(secretKey: String, data: String): String {
        val algorithm = "HmacSHA512"
        val mac = Mac.getInstance(algorithm)
        val keySpec = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), algorithm)
        mac.init(keySpec)
        val hash = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun getClientIp(): String = "192.168.1.1"
}