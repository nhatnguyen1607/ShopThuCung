package com.example.shopthucung.user

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.shopthucung.ui.theme.ShopThuCungTheme
import com.example.shopthucung.user.navigation.NavGraph
import com.example.shopthucung.user.viewmodel.CartViewModel
import com.example.shopthucung.user.viewmodel.OrderViewModel
import com.example.shopthucung.utils.VNPayHelper
import com.google.firebase.FirebaseApp
import java.net.URLEncoder
import java.util.TreeMap

val LocalOrderViewModel = compositionLocalOf<OrderViewModel> { error("No OrderViewModel provided") }
val LocalCartViewModel = compositionLocalOf<CartViewModel> { error("No CartViewModel provided") }

class UserActivity : ComponentActivity() {
    private val orderViewModel: OrderViewModel by viewModels()
    private var navController: androidx.navigation.NavController? = null
    private var isDeepLinkHandled = false
    private var pendingOrderIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        handleDeepLink(intent)
        setContent {
            ShopThuCungTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    App(
                        modifier = Modifier.padding(innerPadding),
                        onNavControllerReady = { navController = it }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("UserActivity", "onNewIntent called with intent: $intent")
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (isDeepLinkHandled) {
            Log.d("DeepLink", "Deep link already handled, skipping")
            return
        }
        intent?.data?.toString()?.let { url ->
            Log.d("DeepLink", "Received URL: $url")
            if (url.startsWith("myapp://payment/verify")) {
                isDeepLinkHandled = true
                handlePaymentResult(url)
            } else {
                Log.w("DeepLink", "Unknown deep link: $url")
                navigateToCheckout()
            }
        } ?: Log.w("DeepLink", "No deep link data in intent")
    }

    private fun handlePaymentResult(result: String) {
        Log.d("PaymentResult", "Received result: $result")
        if (result.isEmpty() || !result.startsWith("myapp://payment/verify")) {
            Log.e("PaymentResult", "Invalid or empty result URL: $result")
            Toast.makeText(this, "Lỗi xử lý thanh toán: URL không hợp lệ", Toast.LENGTH_LONG).show()
            navigateToCheckout()
            return
        }

        val uri = Uri.parse(result)
        val vnp_SecureHash = uri.getQueryParameter("vnp_SecureHash")
        val txnRef = uri.getQueryParameter("vnp_TxnRef")
        val transactionStatus = uri.getQueryParameter("vnp_TransactionStatus")
        Log.d("PaymentResult", "txnRef: $txnRef, vnp_SecureHash: $vnp_SecureHash, transactionStatus: $transactionStatus")

        if (vnp_SecureHash == null || txnRef == null || transactionStatus == null) {
            Log.e("PaymentResult", "Missing required parameters in URL")
            Toast.makeText(this, "Lỗi xử lý thanh toán: Thiếu tham số", Toast.LENGTH_LONG).show()
            navigateToCheckout()
            return
        }

        val params = TreeMap<String, String>()
        uri.queryParameterNames.forEach { key ->
            if (key != "vnp_SecureHash") {
                params[key] = uri.getQueryParameter(key) ?: ""
            }
        }
        val signData = params.entries.joinToString("&") { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
        val computedHash = VNPayHelper.hmacSHA512("W3UIXY4EMGGNQ8Q284D4AXI0IXDJAJ4Q", signData) // Thay bằng Hash Secret từ VNPay
        Log.d("PaymentResult", "Computed hash: $computedHash, Expected hash: $vnp_SecureHash")

        if (computedHash == vnp_SecureHash) {
            if (transactionStatus == "00") {
                orderViewModel.confirmVNPayPayment(txnRef, this, onSuccess = {
                    Log.d("PaymentResult", "Payment successful, pending orders after: ${orderViewModel.pendingOrders.value.map { it.orderId }}")
                    Toast.makeText(this, "Thanh toán thành công", Toast.LENGTH_SHORT).show()
                    pendingOrderIds.remove(txnRef)
                    navigateToCheckout()
                }, onError = { error ->
                    Log.e("PaymentResult", "Error updating order: $error")
                    Toast.makeText(this, "Lỗi cập nhật đơn hàng: $error", Toast.LENGTH_LONG).show()
                    navigateToCheckout()
                })
            } else {
                Log.d("PaymentResult", "Payment failed or canceled, status: $transactionStatus")
                Toast.makeText(this, "Thanh toán thất bại hoặc bị hủy", Toast.LENGTH_LONG).show()
                orderViewModel.cancelVNPayPayment(txnRef, this, onSuccess = {
                    Log.d("PaymentResult", "Order status updated to canceled")
                    pendingOrderIds.remove(txnRef)
                    navigateToCheckout()
                }, onError = { error ->
                    Log.e("PaymentResult", "Error updating order status to canceled: $error")
                    Toast.makeText(this, "Lỗi cập nhật trạng thái đơn hàng: $error", Toast.LENGTH_LONG).show()
                    navigateToCheckout()
                })
            }
        } else {
            Log.e("PaymentResult", "Invalid secure hash")
            Toast.makeText(this, "Kết quả thanh toán không hợp lệ", Toast.LENGTH_LONG).show()
            navigateToCheckout()
        }
    }

    private fun navigateToCheckout() {
        navController?.navigate("checkout") {
            popUpTo(navController!!.graph.startDestinationId) { inclusive = false }
            launchSingleTop = true
        } ?: run {
            val intent = Intent(this, UserActivity::class.java)
            intent.putExtra("navigate_to_checkout", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    @Composable
    fun App(modifier: Modifier = Modifier, onNavControllerReady: (androidx.navigation.NavController) -> Unit) {
        val navController = rememberNavController()
        LaunchedEffect(navController) {
            onNavControllerReady(navController)
        }
        val cartViewModel: CartViewModel = viewModel()

        CompositionLocalProvider(
            LocalOrderViewModel provides orderViewModel,
            LocalCartViewModel provides cartViewModel
        ) {
            NavGraph(navController = navController, modifier = modifier)
        }
    }
}