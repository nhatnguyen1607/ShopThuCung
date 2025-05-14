package com.example.shopthucung.user.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.shopthucung.R
import com.example.shopthucung.model.CartItem
import com.example.shopthucung.model.Order
import com.example.shopthucung.model.Product
import com.example.shopthucung.user.viewmodel.CartViewModel
import com.example.shopthucung.user.viewmodel.OrderViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    orderViewModel: OrderViewModel,
    cartViewModel: CartViewModel,
    product: Product? = null,
    quantity: Int = 1,
    cartItems: List<CartItem>? = null
) {
    val orders = orderViewModel.pendingOrders.collectAsState()
    val vnpayUrls = orderViewModel.vnpayUrls.collectAsState()
    val errorMessage = orderViewModel.errorMessage.collectAsState()
    val successMessage = orderViewModel.successMessage.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedPaymentMethod by remember { mutableStateOf("") }

    // Đặt đơn hàng tạm thời
    LaunchedEffect(product, quantity, cartItems) {
        when {
            cartItems != null && cartItems.isNotEmpty() -> {
                orderViewModel.setPendingOrders(cartItems)
            }
            product != null -> {
                orderViewModel.setPendingOrder(product, quantity)
            }
            else -> {
                orderViewModel.clearPendingOrders()
            }
        }
    }

    // Log trạng thái
    LaunchedEffect(orders.value, vnpayUrls.value) {
        Log.d("CheckoutScreen", "Orders: ${orders.value}, VNPay URLs: ${vnpayUrls.value}")
    }

    LaunchedEffect(Unit) {
        orderViewModel.successMessage.collectLatest { success ->
            if (success != null) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = success,
                        duration = SnackbarDuration.Short
                    )
                    cartViewModel.fetchCartItems()
                    navController.popBackStack()
                    orderViewModel.clearMessages()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đặt hàng") },
                navigationIcon = {
                    IconButton(onClick = {
                        orderViewModel.clearPendingOrders()
                        orderViewModel.clearMessages()
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAFAFA),
                    titleContentColor = Color(0xFF424242)
                )
            )
        },
        containerColor = Color(0xFFFAFAFA),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = if (data.visuals.message.contains("Lỗi")) {
                            Color(0xFFF44336)
                        } else {
                            Color(0xFF4CAF50)
                        },
                        contentColor = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = data.visuals.message,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (orders.value.isEmpty() && vnpayUrls.value.isEmpty()) {
            Log.d("CheckoutScreen", "Displaying empty state")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không có sản phẩm để thanh toán",
                    color = Color(0xFF757575),
                    fontSize = 16.sp
                )
            }
        } else {
            Log.d("CheckoutScreen", "Displaying orders: ${orders.value.size}, VNPay URLs: ${vnpayUrls.value.size}")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (vnpayUrls.value.isNotEmpty()) {
                    // Hiển thị mã QR
                    vnpayUrls.value.forEach { (orderId, url) ->
                        Text(
                            text = "Đơn hàng: $orderId",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        QRCodeImage(url = url)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                // Giả lập xác nhận thanh toán
                                orderViewModel.confirmVNPayPayment(orderId)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Xác nhận đã thanh toán", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    // Hiển thị danh sách đơn hàng
                    orders.value.forEach { order ->
                        OrderItemCard(order = order)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = "Phương thức thanh toán",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == "COD",
                                onClick = { selectedPaymentMethod = "COD" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFFA5D6A7),
                                    unselectedColor = Color(0xFF757575)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("COD", fontSize = 16.sp, color = Color(0xFF424242))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == "VNPay",
                                onClick = { selectedPaymentMethod = "VNPay" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFFF44336),
                                    unselectedColor = Color(0xFF757575)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VNPay", fontSize = 16.sp, color = Color(0xFF424242))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (selectedPaymentMethod.isEmpty()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Vui lòng chọn phương thức đặt hàng",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else {
                                if (cartItems != null && cartItems.isNotEmpty()) {
                                    orderViewModel.confirmCartOrders(cartItems, selectedPaymentMethod) {
                                        cartViewModel.fetchCartItems()
                                    }
                                } else if (product != null) {
                                    orderViewModel.confirmDirectOrder(product, quantity, selectedPaymentMethod)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = orders.value.isNotEmpty()
                    ) {
                        Text("Đặt hàng", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                errorMessage.value?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Lỗi: $error",
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(order: Order) {
    val product = order.product ?: run {
        Log.d("OrderItemCard", "Product is null for order: ${order.orderId}")
        return
    }
    Log.d("OrderItemCard", "Displaying product: ${product.ten_sp}")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
                AsyncImage(
                    model = product.anh_sp.firstOrNull() ?: R.drawable.placeholder_image, // Lấy ảnh đầu tiên từ danh sách
                    contentDescription = product.ten_sp,
                    modifier = Modifier.fillMaxSize(),
                    error = painterResource(R.drawable.placeholder_image)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.ten_sp,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Số lượng: ${order.quantity}",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tổng tiền: ${order.totalPrice.formatWithComma()} đ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun QRCodeImage(url: String) {
    if (url.isEmpty()) {
        Log.e("QRCodeImage", "URL is empty")
        Text(
            text = "URL thanh toán không hợp lệ",
            color = Color.Red,
            fontSize = 14.sp,
            modifier = Modifier.padding(8.dp)
        )
        return
    }

    val context = LocalContext.current
    val qrCodeState = remember(url) {
        try {
            val width = 300
            val height = 300
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, width, height)
            val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            Log.d("QRCodeImage", "ZXing Bitmap dimensions: ${bmp.width}x${bmp.height}")

            // Lưu Bitmap để kiểm tra
            saveBitmapToFile(context, bmp, "qr_code_zxing.png")

            Result.success(bmp)
        } catch (e: Exception) {
            Log.e("QRCodeImage", "Error generating QR Code: ${e.message}")
            Result.failure(e)
        }
    }

    Log.d("QRCodeImage", "Recomposing QRCodeImage")
    Log.d("QRCodeImage", "qrCodeState success: ${qrCodeState.isSuccess}, value: ${qrCodeState.getOrNull()}")
    when {
        qrCodeState.isSuccess -> {
            Log.d("QRCodeImage", "Rendering QR Image")
            Image(
                bitmap = qrCodeState.getOrNull()!!.asImageBitmap(),
                contentDescription = "QR Code thanh toán VNPay",
                modifier = Modifier
                    .size(300.dp)
                    .background(Color.White)
                    .padding(8.dp)
            )
        }
        qrCodeState.isFailure -> {
            Log.e("QRCodeImage", "QR Code failed: ${qrCodeState.exceptionOrNull()?.message}")
            Text(
                text = "Lỗi khi tạo mã QR: ${qrCodeState.exceptionOrNull()?.message}",
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

fun saveBitmapToFile(context: Context, bitmap: android.graphics.Bitmap, fileName: String) {
    try {
        val file = File(context.getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { out: FileOutputStream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        Log.d("QRCodeImage", "Saved QR code to: ${file.absolutePath}")
    } catch (e: Exception) {
        Log.e("QRCodeImage", "Error saving QR code: ${e.message}")
    }
}