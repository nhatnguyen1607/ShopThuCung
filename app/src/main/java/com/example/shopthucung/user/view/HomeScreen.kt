package com.example.shopthucung.user.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.shopthucung.R
import com.example.shopthucung.user.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel) {
    val searchQuery = remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_petshop),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(80.dp)
                                .padding(end = 12.dp)
                        )
                        OutlinedTextField(
                            value = searchQuery.value,
                            onValueChange = { searchQuery.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .shadow(2.dp, RoundedCornerShape(12.dp)),
                            placeholder = { Text("Tìm kiếm thú cưng, đồ dùng...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Tìm kiếm",
                                    tint = Color(0xFFA5D6A7) // Xanh lá nhạt
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFA5D6A7),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAFAFA), // Nền trắng nhạt
                    titleContentColor = Color(0xFF424242) // Màu chữ xám đậm
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFFFAFAFA),
                contentColor = Color(0xFFA5D6A7)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* TODO: Xử lý khi nhấn Trang chủ */ }) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Trang chủ",
                            tint = Color(0xFFA5D6A7)
                        )
                    }
                    IconButton(onClick = { navController.navigate("cart") }) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Giỏ hàng",
                            tint = Color(0xFFA5D6A7)
                        )
                    }
                    IconButton(onClick = { navController.navigate("notifications") }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Thông báo",
                            tint = Color(0xFFA5D6A7)
                        )
                    }
                    IconButton(onClick = {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        navController.navigate("user/$uid")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Người dùng",
                            tint = Color(0xFFA5D6A7)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFFAFAFA), // Nền tổng thể
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Nút danh mục
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { navController.navigate("category/pets") },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .padding(end = 8.dp)
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA5D6A7), // Xanh lá nhạt
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Thú cưng",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = { navController.navigate("category/accessories") },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF8BBD0), // Hồng nhạt
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Đồ dùng thú cưng",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Sản phẩm nổi bật
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sản phẩm nổi bật",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF424242)
                            )
                            TextButton(onClick = { navController.navigate("trending/all") }) {
                                Text(
                                    text = "Xem tất cả",
                                    color = Color(0xFFF44336), // Đỏ
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(viewModel.trendingProducts.value) { product ->
                                ProductCard(
                                    price = product.gia_sp,
                                    name = product.ten_sp,
                                    imageUrl = product.anh_sp,
                                    isNew = product.so_luong_ban == 0,
                                    discount = product.giam_gia,
                                    productId = product.id_sanpham,
                                    navController = navController
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Sản phẩm mới
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sản phẩm mới",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF424242)
                            )
                            TextButton(onClick = { navController.navigate("new/all") }) {
                                Text(
                                    text = "Xem tất cả",
                                    color = Color(0xFFF44336),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(viewModel.newProducts.value) { product ->
                                ProductCard(
                                    price = product.gia_sp,
                                    name = product.ten_sp,
                                    imageUrl = product.anh_sp,
                                    isNew = true,
                                    discount = product.giam_gia,
                                    productId = product.id_sanpham,
                                    navController = navController
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Xếp hạng cao
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Xếp hạng cao",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF424242)
                            )
                            TextButton(onClick = { navController.navigate("toprated/all") }) {
                                Text(
                                    text = "Xem tất cả",
                                    color = Color(0xFFF44336),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(viewModel.topRatedProducts.value) { product ->
                                ProductCard(
                                    price = product.gia_sp,
                                    name = product.ten_sp,
                                    imageUrl = product.anh_sp,
                                    isNew = false,
                                    discount = product.giam_gia,
                                    productId = product.id_sanpham,
                                    navController = navController
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    )
}

@Composable
fun ProductCard(
    price: Long,
    name: String,
    imageUrl: String,
    isNew: Boolean,
    discount: Int,
    productId: Int,
    navController: NavController
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .padding(vertical = 8.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clickable { navController.navigate("productDetail/$productId") },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0E0E0))
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Hình ảnh sản phẩm",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        error = painterResource(R.drawable.placeholder_image)
                    )

                    if (discount > 0) {
                        Text(
                            text = "-$discount%",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .background(
                                    Color(0xFFFFF59D),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${price.formatWithComma()} đ",
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    style = TextStyle(textDecoration = TextDecoration.LineThrough)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (discount > 0) {
                    val discountedPrice = price - (price * discount / 100)
                    Text(
                        text = "${discountedPrice.formatWithComma()} đ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                } else {
                    Text(
                        text = "${price.formatWithComma()} đ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }

            if (isNew) {
                Text(
                    text = "Mới",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color(0xFFF8BBD0), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

fun Long.formatWithComma(): String {
    return String.format("%,d", this).replace(",", ".")
}