package com.example.shopthucung.user.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.example.shopthucung.model.Banner
import com.example.shopthucung.model.Product
import com.example.shopthucung.user.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel) {
    val searchQuery = remember { mutableStateOf("") }
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var cartItemCount by remember { mutableStateOf(0) }
    var searchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
    val columnListState = rememberLazyListState()
    val rowListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var banners by remember { mutableStateOf<List<Banner>>(emptyList()) }
    var currentBannerIndex by remember { mutableStateOf(0) }

    // Tải danh sách banner từ Firestore
    LaunchedEffect(Unit) {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("banner")
                .get()
                .await()
            banners = snapshot.documents.mapNotNull { doc ->
                val banner = doc.toObject(Banner::class.java)
                banner
            }
            if (banners.isNotEmpty()) {
                while (true) {
                    delay(3000)
                    currentBannerIndex = (currentBannerIndex + 1) % banners.size
                    coroutineScope.launch {
                        rowListState.animateScrollToItem(currentBannerIndex)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error loading banners: $e")
            banners = emptyList()
        }
    }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("cart")
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()
                cartItemCount = snapshot.documents.size
            } catch (e: Exception) {
                cartItemCount = 0
            }
        }
    }

    LaunchedEffect(searchQuery.value) {
        if (searchQuery.value.isNotEmpty()) {
            try {
                val query = removeDiacritics(searchQuery.value.trim()).lowercase()
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("product")
                    .get()
                    .await()
                searchResults = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)
                }.filter { product ->
                    val tenSpLowercase = removeDiacritics(product.ten_sp).lowercase()
                    tenSpLowercase.contains(query)
                }
            } catch (e: Exception) {
                searchResults = emptyList()
            }
        } else {
            searchResults = emptyList()
        }
    }

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
                                .size(60.dp) // Giảm kích thước logo để cân đối
                                .padding(end = 12.dp)
                        )
                        OutlinedTextField(
                            value = searchQuery.value,
                            onValueChange = { searchQuery.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .shadow(4.dp, RoundedCornerShape(16.dp)), // Tăng shadow cho nổi bật
                            placeholder = { Text("Tìm kiếm sản phẩm...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Tìm kiếm",
                                    tint = Color(0xFFA5D6A7)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.value.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery.value = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Xóa tìm kiếm",
                                            tint = Color(0xFFA5D6A7)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp), // Bo góc lớn hơn
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
                    containerColor = Color(0xFFFAFAFA),
                    titleContentColor = Color(0xFF424242)
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFFFAFAFA),
                contentColor = Color(0xFFA5D6A7),
                modifier = Modifier.shadow(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Trang chủ",
                            tint = Color(0xFFA5D6A7),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFF44336),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = cartItemCount.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { navController.navigate("cart") }) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Giỏ hàng",
                                tint = Color(0xFFA5D6A7),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    IconButton(onClick = { navController.navigate("notifications") }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Thông báo",
                            tint = Color(0xFFA5D6A7),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        navController.navigate("user/$uid")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Người dùng",
                            tint = Color(0xFFA5D6A7),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFFAFAFA),
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (searchQuery.value.isNotEmpty()) {
                    LazyColumn(
                        state = columnListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kết quả tìm kiếm",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF424242)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${searchResults.size} sản phẩm)",
                                    fontSize = 16.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                        }
                        if (searchResults.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Không tìm thấy sản phẩm nào!",
                                        fontSize = 18.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }
                        } else {
                            items(searchResults) { product ->
                                SearchProductCard(
                                    price = product.gia_sp,
                                    name = product.ten_sp,
                                    imageUrls = product.anh_sp,
                                    isNew = product.so_luong_ban == 0,
                                    discount = product.giam_gia,
                                    soldQuantity = product.so_luong_ban,
                                    rating = product.danh_gia,
                                    productId = product.id_sanpham,
                                    navController = navController,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = columnListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Nút danh mục
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { navController.navigate("pet") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .padding(end = 8.dp)
                                        .shadow(6.dp, RoundedCornerShape(16.dp)),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFA5D6A7),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "Thú cưng",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Button(
                                    onClick = { navController.navigate("supplies") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .shadow(6.dp, RoundedCornerShape(16.dp)),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF8BBD0),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "Đồ dùng thú cưng",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Carousel banner
                        item {
                            if (banners.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp) // Tăng chiều cao banner
                                ) {
                                    LazyRow(
                                        state = rowListState,
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                                    ) {
                                        items(banners) { banner ->
                                            AsyncImage(
                                                model = banner.anh_banner,
                                                contentDescription = "Banner",
                                                modifier = Modifier
                                                    .fillParentMaxWidth()
                                                    .height(280.dp)
                                                    .clip(RoundedCornerShape(16.dp)), // Bo góc cho banner
                                                contentScale = ContentScale.Crop, // Cắt ảnh để vừa khung
                                                error = painterResource(R.drawable.placeholder_image)
                                            )
                                        }
                                    }

                                    // Chỉ báo (dots indicator)
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        banners.forEachIndexed { index, _ ->
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (index == currentBannerIndex) Color(0xFFA5D6A7)
                                                        else Color(0xFFB0BEC5)
                                                    )
                                                    .padding(horizontal = 4.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Sản phẩm nổi bật
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sản phẩm nổi bật",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF424242)
                                )
                            }
                        }

                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp) // Giảm khoảng cách
                            ) {
                                items(viewModel.trendingProducts.value) { product ->
                                    ProductCard(
                                        price = product.gia_sp,
                                        name = product.ten_sp,
                                        imageUrls = product.anh_sp,
                                        isNew = product.so_luong_ban == 0,
                                        discount = product.giam_gia,
                                        productId = product.id_sanpham,
                                        navController = navController
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Sản phẩm mới
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sản phẩm mới",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF424242)
                                )
                            }
                        }

                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(viewModel.newProducts.value) { product ->
                                    ProductCard(
                                        price = product.gia_sp,
                                        name = product.ten_sp,
                                        imageUrls = product.anh_sp,
                                        isNew = true,
                                        discount = product.giam_gia,
                                        productId = product.id_sanpham,
                                        navController = navController
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Xếp hạng cao
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Xếp hạng cao",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF424242)
                                )
                            }
                        }

                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(viewModel.topRatedProducts.value) { product ->
                                    ProductCard(
                                        price = product.gia_sp,
                                        name = product.ten_sp,
                                        imageUrls = product.anh_sp,
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

                ScrollToTopButton(
                    lazyListState = columnListState,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    )
}

@Composable
fun ScrollToTopButton(lazyListState: LazyListState, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()

    if (lazyListState.firstVisibleItemIndex > 2) {
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            },
            modifier = modifier.padding(16.dp),
            containerColor = Color(0xFFA5D6A7),
            contentColor = Color.White,
            shape = CircleShape // Bo tròn nút
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Quay lại đầu trang",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ProductCard(
    price: Long,
    name: String,
    imageUrls: List<String>,
    isNew: Boolean,
    discount: Int,
    productId: Int,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(150.dp) // Giảm chiều rộng để hiển thị nhiều sản phẩm hơn
            .padding(vertical = 4.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)), // Tăng shadow cho nổi bật
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate("productDetail/$productId") }
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp) // Giảm chiều cao ảnh
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0E0E0))
                ) {
                    AsyncImage(
                        model = imageUrls.firstOrNull() ?: "",
                        contentDescription = "Hình ảnh sản phẩm",
                        contentScale = ContentScale.Crop,
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
                                    Color(0xFFF44336), // Đổi màu nền thành đỏ để nổi bật
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    if (isNew) {
                        Text(
                            text = "Mới",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(
                                    Color(0xFFA5D6A7), // Đổi màu nền thành xanh lá
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = name,
                    fontSize = 14.sp, // Giảm kích thước chữ để vừa khung
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (discount > 0) {
                    Text(
                        text = "${price.formatWithComma()} đ",
                        fontSize = 12.sp,
                        color = Color(0xFF757575),
                        style = TextStyle(textDecoration = TextDecoration.LineThrough)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val discountedPrice = price - (price * discount / 100)
                    Text(
                        text = "${discountedPrice.formatWithComma()} đ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                } else {
                    Text(
                        text = "${price.formatWithComma()} đ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchProductCard(
    price: Long,
    name: String,
    imageUrls: List<String>,
    isNew: Boolean,
    discount: Int,
    soldQuantity: Int,
    rating: Float,
    productId: Int,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate("productDetail/$productId") }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp) // Giảm kích thước ảnh
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
                AsyncImage(
                    model = imageUrls.firstOrNull() ?: "",
                    contentDescription = "Hình ảnh sản phẩm",
                    contentScale = ContentScale.Crop,
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
                                Color(0xFFF44336),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                if (isNew) {
                    Text(
                        text = "Mới",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(
                                Color(0xFFA5D6A7),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (discount > 0) {
                        Text(
                            text = "${price.formatWithComma()} đ",
                            fontSize = 12.sp,
                            color = Color(0xFF757575),
                            style = TextStyle(textDecoration = TextDecoration.LineThrough)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    val finalPrice = if (discount > 0) {
                        price - (price * discount / 100)
                    } else {
                        price
                    }
                    Text(
                        text = "${finalPrice.formatWithComma()} đ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Đã bán: $soldQuantity",
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Đánh giá: $rating",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_star),
                        contentDescription = "Đánh giá",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun Long.formatWithComma(): String {
    return String.format("%,d", this).replace(",", ".")
}

fun removeDiacritics(input: String): String {
    return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
        .replace("\\p{M}".toRegex(), "")
        .lowercase()
}