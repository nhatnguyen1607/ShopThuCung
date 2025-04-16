package com.example.shopthucung.user.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.shopthucung.R
import com.example.shopthucung.model.CartItem
import com.example.shopthucung.user.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    cartViewModel: CartViewModel = viewModel()
) {
    val cartItems by cartViewModel.cartItemsState.collectAsState()
    val errorMessage = cartViewModel.errorMessage.collectAsState()
    val successMessage = cartViewModel.successMessage.collectAsState()

    // Tính tổng tiền
    val totalPrice = cartItems.sumOf { item ->
        val product = item.product ?: return@sumOf 0L
        val price = if (product.giam_gia > 0) {
            product.gia_sp - (product.gia_sp * product.giam_gia / 100)
        } else {
            product.gia_sp
        }
        price * item.quantity
    }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Hiển thị thông báo lỗi
    LaunchedEffect(errorMessage) {
        if (errorMessage.value != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMessage.value!!,
                    actionLabel = null,
                    withDismissAction = false,
                    duration = SnackbarDuration.Short
                )
                cartViewModel.clearMessages()
            }
        }
    }

    // Hiển thị thông báo thành công
    LaunchedEffect(successMessage) {
        if (successMessage.value != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = successMessage.value!!,
                    actionLabel = null,
                    withDismissAction = false,
                    duration = SnackbarDuration.Short
                )
                cartViewModel.clearMessages()
            }
        }
    }

    // Gọi fetchCartItems khi vào màn hình
    LaunchedEffect(Unit) {
        cartViewModel.fetchCartItems()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giỏ hàng") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tổng tiền:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = "${totalPrice.formatWithComma()} đ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            // Mã hóa danh sách cartItems thành JSON
                            val cartItemsJson = cartItems.toJson()
                            val encodedCartItems = URLEncoder.encode(
                                cartItemsJson,
                                StandardCharsets.UTF_8.toString()
                            )
                            navController.navigate("checkout?cartItems=$encodedCartItems")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Thanh toán (${cartItems.size} sản phẩm)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Giỏ hàng của bạn đang trống!",
                        fontSize = 18.sp,
                        color = Color(0xFF757575),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(cartItems) { cartItem ->
                        CartItemRow(
                            cartItem = cartItem,
                            onIncreaseQuantity = {
                                cartViewModel.updateQuantity(cartItem, cartItem.quantity + 1)
                            },
                            onDecreaseQuantity = {
                                cartViewModel.updateQuantity(cartItem, cartItem.quantity - 1)
                            },
                            onRemoveItem = { cartViewModel.removeCartItem(cartItem) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onIncreaseQuantity: () -> Unit,
    onDecreaseQuantity: () -> Unit,
    onRemoveItem: () -> Unit
) {
    val product = cartItem.product
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hình ảnh sản phẩm
            AsyncImage(
                model = product?.anh_sp,
                contentDescription = product?.ten_sp,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                error = painterResource(R.drawable.placeholder_image)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Thông tin sản phẩm
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = product?.ten_sp ?: "Không xác định",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${product?.gia_sp?.formatWithComma()} đ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Nút tăng/giảm số lượng
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDecreaseQuantity,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFE0E0E0), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Giảm số lượng",
                            tint = Color(0xFF424242)
                        )
                    }
                    Text(
                        text = "${cartItem.quantity}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 12.dp),
                        color = Color(0xFF424242)
                    )
                    IconButton(
                        onClick = onIncreaseQuantity,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFE0E0E0), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tăng số lượng",
                            tint = Color(0xFF424242)
                        )
                    }
                }
            }

            // Nút xóa sản phẩm
            IconButton(
                onClick = onRemoveItem,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFFE0E0), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa sản phẩm",
                    tint = Color(0xFFF44336)
                )
            }
        }
    }
}

// Hàm tiện ích để mã hóa cartItems thành JSON
fun List<CartItem>.toJson(): String {
    val items = this.joinToString(",") { cartItem ->
        val product = cartItem.product ?: return@joinToString "{}"
        """{
            "product": {
                "id_sanpham": "${product.id_sanpham}",
                "ten_sp": "${product.ten_sp.replace("\"", "\\\"")}",
                "gia_sp": ${product.gia_sp},
                "giam_gia": ${product.giam_gia},
                "anh_sp": "${product.anh_sp}",
                "mo_ta": "${product.mo_ta.replace("\"", "\\\"")}",
                "soluong": ${product.soluong},
                "so_luong_ban": ${product.so_luong_ban},
                "danh_gia": ${product.danh_gia}
            },
            "quantity": ${cartItem.quantity},
            "cartIndex": ${cartItem.cartIndex}
        }"""
    }
    return "[$items]"
}