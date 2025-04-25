package com.example.shopthucung.user.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.shopthucung.R
import com.example.shopthucung.model.CartItem
import com.example.shopthucung.model.Product
import com.example.shopthucung.user.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import android.util.Log

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
    LaunchedEffect(errorMessage.value) {
        if (errorMessage.value != null) {
            Log.d("CartScreen", "Hiển thị lỗi: ${errorMessage.value}")
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

    // Hiển thị thông báo thành công với khóa duy nhất
    LaunchedEffect(successMessage.value) {
        if (successMessage.value != null) {
            Log.d("CartScreen", "Hiển thị thông báo thành công: ${successMessage.value}")
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
        Log.d("CartScreen", "Tải giỏ hàng khi vào màn hình")
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
                            onRemoveItem = { cartViewModel.removeCartItem(cartItem) },
                            onQuantityChanged = { newQuantity ->
                                cartViewModel.updateQuantity(cartItem, newQuantity)
                            }
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
    onRemoveItem: () -> Unit,
    onQuantityChanged: (Int) -> Unit
) {
    val product = cartItem.product
    // Đồng bộ quantityText với cartItem.quantity
    var quantityText by remember(cartItem.quantity) { mutableStateOf(cartItem.quantity.toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var hasFocus by remember { mutableStateOf(false) }

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
                val discountedPrice =
                    product?.let { it?.gia_sp?.minus((product.gia_sp * product.giam_gia / 100))}
                Text(
                    text = "${discountedPrice?.formatWithComma()} đ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Nút tăng/giảm số lượng và TextField
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onDecreaseQuantity()
                            quantityText = cartItem.quantity.toString()
                            errorText = null
                        },
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

                    Spacer(modifier = Modifier.width(8.dp))

                    TextField(
                        value = quantityText,
                        onValueChange = { newValue ->
                            quantityText = newValue
                            errorText = null
                        },
                        modifier = Modifier
                            .width(80.dp)
                            .height(50.dp)
                            .onFocusChanged { focusState ->
                                hasFocus = focusState.isFocused
                                if (!hasFocus && quantityText.isEmpty()) {
                                    quantityText = "1"
                                    onQuantityChanged(1)
                                }
                            },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        isError = errorText != null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            onIncreaseQuantity()
                            quantityText = cartItem.quantity.toString() // Cập nhật TextField
                            errorText = null // Xóa lỗi nếu có
                        },
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

                // Hiển thị thông báo lỗi nếu có
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = Color(0xFFF44336),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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

        // Xử lý khi TextField mất tiêu điểm hoặc nhấn Enter
        LaunchedEffect(quantityText) {
            if (quantityText.isNotEmpty()) {
                try {
                    val newQuantity = quantityText.toInt()
                    val availableQuantity = product?.let { it.soluong - it.so_luong_ban } ?: 0
                    when {
                        newQuantity <= 0 -> {
                            errorText = "Số lượng phải lớn hơn 0!"
                            quantityText = cartItem.quantity.toString() // Khôi phục giá trị cũ
                        }
                        newQuantity > availableQuantity -> {
                            errorText = "Số lượng vượt quá tồn kho ($availableQuantity)!"
                            quantityText = cartItem.quantity.toString() // Khôi phục giá trị cũ
                        }
                        newQuantity != cartItem.quantity -> {
                            onQuantityChanged(newQuantity)
                            errorText = null
                        }
                    }
                } catch (e: NumberFormatException) {
                    errorText = "Vui lòng nhập số hợp lệ!"
                    quantityText = cartItem.quantity.toString() // Khôi phục giá trị cũ
                }
            }
        }

        // Đồng bộ quantityText khi cartItem.quantity thay đổi (do nút + hoặc -)
        LaunchedEffect(cartItem.quantity) {
            Log.d("CartItemRow", "Đồng bộ quantityText: ${cartItem.quantity}")
            quantityText = cartItem.quantity.toString()
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
