package com.example.shopthucung.user.view

import android.annotation.SuppressLint
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.shopthucung.R
import com.example.shopthucung.model.CartItem
import com.example.shopthucung.model.Order
import com.example.shopthucung.model.Product
import com.example.shopthucung.user.viewmodel.CartViewModel
import com.example.shopthucung.user.viewmodel.OrderViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

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
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (vnpayUrls.value.isNotEmpty()) {
                    // Hiển thị mã QR
                    vnpayUrls.value.forEachIndexed { index, (orderId, _) ->
                        val order = orders.value.getOrNull(index)
                        val productName = order?.product?.ten_sp ?: "Sản phẩm không xác định"
                        Text(
                            text = "Sản phẩm: $productName",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        QRCodeImage()
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                // Giả lập xác nhận thanh toán
                                orderViewModel.confirmVNPayPayment(orderId)
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(48.dp),
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
                            Text("Thanh toán online", fontSize = 16.sp, color = Color(0xFF424242))
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
                            .fillMaxWidth(0.8f)
                            .height(48.dp),
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
                Spacer(modifier = Modifier.height(16.dp))
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
    val productName = product.ten_sp
    val productImages = product.anh_sp

    Log.d("OrderItemCard", "Displaying product: $productName")

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
                    model = productImages.firstOrNull() ?: R.drawable.placeholder_image,
                    contentDescription = productName,
                    modifier = Modifier.fillMaxSize(),
                    error = painterResource(R.drawable.placeholder_image)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = productName,
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
fun QRCodeImage() {
    Image(
        painter = painterResource(id = R.drawable.qr),
        contentDescription = "QR Code thanh toán VNPay",
        modifier = Modifier
            .size(500.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(12.dp)
    )
}

