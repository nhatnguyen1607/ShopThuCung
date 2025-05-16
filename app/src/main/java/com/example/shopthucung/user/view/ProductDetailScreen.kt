package com.example.shopthucung.user.view

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.shopthucung.R
import com.example.shopthucung.model.Product
import com.example.shopthucung.model.Review
import com.example.shopthucung.model.User
import com.example.shopthucung.user.viewmodel.CartViewModel
import com.example.shopthucung.user.viewmodel.ProductDetailViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: Int,
    viewModel: CartViewModel,
    productDetailViewModel: ProductDetailViewModel
) {
    val productState = productDetailViewModel.productState.collectAsState()
    val reviewsState = productDetailViewModel.reviewsState.collectAsState()
    val usersState = productDetailViewModel.usersState.collectAsState()
    val averageRating = productDetailViewModel.averageRating.collectAsState()
    val errorMessage = productDetailViewModel.errorMessage.collectAsState()
    val successMessage = viewModel.successMessage.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Hiển thị thông báo lỗi
    LaunchedEffect(errorMessage) {
        if (errorMessage.value != null) {
            Log.d("ProductDetailScreen", "Hiển thị lỗi: ${errorMessage.value}")
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMessage.value!!,
                    actionLabel = null,
                    withDismissAction = false,
                    duration = SnackbarDuration.Short
                )
                productDetailViewModel.clearMessages()
            }
        }
    }

    // Hiển thị thông báo thành công với khóa duy nhất
    LaunchedEffect(successMessage.value) {
        if (successMessage.value != null) {
            Log.d("ProductDetailScreen", "Hiển thị thông báo thành công: ${successMessage.value}")
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = successMessage.value!!,
                    actionLabel = null,
                    withDismissAction = false,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearMessages()
            }
        }
    }

    LaunchedEffect(productId) {
        productDetailViewModel.fetchProduct(productId)
        productDetailViewModel.fetchReviews(productId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết sản phẩm") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        when {
            productState.value == null && errorMessage.value == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage.value != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage.value ?: "Đã xảy ra lỗi",
                        color = Color.Red,
                        fontSize = 16.sp
                    )
                }
            }

            else -> {
                val product = productState.value!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Product Image Carousel
                    val pageCount = product.anh_sp.size.coerceAtLeast(1)
                    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE0E0E0))
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = product.anh_sp.getOrNull(page) ?: R.drawable.placeholder_image,
                                contentDescription = "${product.ten_sp} - Ảnh $page",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = painterResource(R.drawable.placeholder_image)
                            )
                        }
                        // Indicators
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(pageCount) { iteration ->
                                val color = if (pagerState.currentPage == iteration) Color.White else Color.Gray
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(8.dp)
                                )
                            }
                        }
                        if (product.giam_gia > 0) {
                            Text(
                                text = "-${product.giam_gia}%",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .background(Color(0xFFFFF59D), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Product Name
                    Text(
                        text = product.ten_sp,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pricing
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (product.giam_gia > 0) {
                            val discountedPrice =
                                product.gia_sp - (product.gia_sp * product.giam_gia / 100)
                            Text(
                                text = "${product.gia_sp.formatWithComma()} đ",
                                fontSize = 16.sp,
                                color = Color(0xFF757575),
                                style = TextStyle(textDecoration = TextDecoration.LineThrough)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${discountedPrice.formatWithComma()} đ",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                        } else {
                            Text(
                                text = "${product.gia_sp.formatWithComma()} đ",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quantity Available
                    Text(
                        text = "Số lượng còn: ${product.soluong - product.so_luong_ban}",
                        fontSize = 16.sp,
                        color = Color(0xFF424242)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sold Quantity
                    Text(
                        text = "Đã bán: ${product.so_luong_ban}",
                        fontSize = 16.sp,
                        color = Color(0xFF424242)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                viewModel.addToCart(product)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .padding(end = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFA5D6A7),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Giỏ hàng"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Thêm vào giỏ")
                            }
                        }
                        Button(
                            onClick = {
                                // Chỉ điều hướng đến CheckoutScreen, không tạo đơn hàng
                                val productJson = product.toJson()
                                val encodedProduct = URLEncoder.encode(productJson, StandardCharsets.UTF_8.toString())
                                navController.navigate("checkout?product=$encodedProduct&quantity=1")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Mua ngay")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Description
                    Text(
                        text = "Mô tả sản phẩm",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.mo_ta,
                        fontSize = 16.sp,
                        color = Color(0xFF757575)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Rating Section
                    RatingSection(
                        averageRating = averageRating.value,
                        reviews = reviewsState.value,
                        users = usersState.value,
                        selectedFilter = productDetailViewModel.selectedFilter.collectAsState().value,
                        onFilterSelected = { filter -> productDetailViewModel.setFilter(filter) }
                    )
                }
            }
        }
    }
}

fun Product.toJson(): String {
    val anhSpJson = anh_sp.joinToString(
        separator = ",",
        prefix = "[",
        postfix = "]"
    ) { "\"${it.replace("\"", "\\\"")}\"" }
    return """{
        "id_sanpham": "${id_sanpham}",
        "ten_sp": "${ten_sp}",
        "gia_sp": ${gia_sp},
        "giam_gia": ${giam_gia},
        "anh_sp": $anhSpJson,
        "mo_ta": "${mo_ta.replace("\"", "\\\"")}",
        "soluong": ${soluong},
        "so_luong_ban": ${so_luong_ban},
        "danh_gia": ${danh_gia},
        "id_category": ${id_category}
    }""".trimIndent()
}

@Composable
fun RatingSection(
    averageRating: Float,
    reviews: List<Review>,
    users: Map<String, User>,
    selectedFilter: Int?,
    onFilterSelected: (Int?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "ĐÁNH GIÁ",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF44336)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%.1f/5", averageRating),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row {
                repeat(5) { index ->
                    Icon(
                        painter = painterResource(R.drawable.ic_star),
                        contentDescription = "Sao",
                        tint = if (index < averageRating.toInt()) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(${reviews.size} lượt đánh giá)",
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val ratingDistribution = IntArray(5) { 0 }
        reviews.forEach { review ->
            if (review.rating in 1..5) {
                ratingDistribution[review.rating - 1]++
            }
        }
        val totalReviews = reviews.size
        for (stars in 5 downTo 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$stars",
                    fontSize = 14.sp,
                    color = Color(0xFF424242),
                    modifier = Modifier.width(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                if (totalReviews > 0) ratingDistribution[stars - 1].toFloat() / totalReviews else 0f
                            )
                            .height(8.dp)
                            .background(Color(0xFF2196F3), RoundedCornerShape(4.dp))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${ratingDistribution[stars - 1]}",
                    fontSize = 14.sp,
                    color = Color(0xFF424242),
                    modifier = Modifier.width(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FilterButton(
                text = "ALL",
                color = Color(0xFF2196F3),
                isSelected = selectedFilter == null,
                onClick = { onFilterSelected(null) }
            )
            FilterButton(
                text = "5",
                color = Color(0xFF4CAF50),
                isSelected = selectedFilter == 5,
                onClick = { onFilterSelected(5) }
            )
            FilterButton(
                text = "4",
                color = Color(0xFF4CAF50),
                isSelected = selectedFilter == 4,
                onClick = { onFilterSelected(4) }
            )
            FilterButton(
                text = "3",
                color = Color(0xFFFFC107),
                isSelected = selectedFilter == 3,
                onClick = { onFilterSelected(3) }
            )
            FilterButton(
                text = "2",
                color = Color(0xFFFF9800),
                isSelected = selectedFilter == 2,
                onClick = { onFilterSelected(2) }
            )
            FilterButton(
                text = "1",
                color = Color(0xFFF44336),
                isSelected = selectedFilter == 1,
                onClick = { onFilterSelected(1) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (reviews.isNotEmpty()) {
            reviews.forEach { review ->
                val user = users[review.idUser]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AsyncImage(
                        model = user?.avatar ?: R.drawable.placeholder_image,
                        contentDescription = "Ảnh đại diện của ${user?.hoVaTen ?: "Người dùng"}",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0)),
                        error = painterResource(R.drawable.placeholder_image)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = user?.hoVaTen ?: "Người dùng không xác định",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            repeat(5) { index ->
                                Icon(
                                    painter = painterResource(R.drawable.ic_star),
                                    contentDescription = "Sao",
                                    tint = if (index < review.rating) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = review.comment,
                            fontSize = 14.sp,
                            color = Color(0xFF424242)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatTimestamp(review.timestamp),
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Chưa có đánh giá nào.",
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

@Composable
fun FilterButton(
    text: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else Color(0xFFE0E0E0),
            contentColor = if (isSelected) Color.White else Color(0xFF424242)
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatTimestamp(timestamp: Timestamp?): String {
    return try {
        timestamp?.toDate()?.let { date ->
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
        } ?: "Không có thời gian"
    } catch (e: Exception) {
        "Lỗi định dạng thời gian: ${e.message}"
    }
}

fun Int.formatWithComma(): String {
    return String.format("%,d", this).replace(",", ".")
}

