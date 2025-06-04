package com.example.shopthucung.user.view


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.shopthucung.model.Category
import com.example.shopthucung.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetScreen(navController: NavController) {
    val listState = rememberLazyListState()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("category")
                .whereGreaterThanOrEqualTo("id_category", 2)
                .get()
                .await()
            categories = snapshot.documents.mapNotNull { doc ->
                val idCategory = doc.getLong("id_category")?.toInt() ?: 0
                val tenDanhmuc = doc.getString("ten")
                if (idCategory > 0) Category(id_category = idCategory, ten = tenDanhmuc.toString()) else null
            }
        } catch (e: Exception) {
            println("Lỗi khi tải danh mục: $e")
            categories = emptyList()
        }
    }

    LaunchedEffect(selectedCategoryId) {
        try {
            val query = FirebaseFirestore.getInstance()
                .collection("product")
                .whereGreaterThanOrEqualTo("id_category", 2)

            val snapshot = if (selectedCategoryId != null) {
                query.whereEqualTo("id_category", selectedCategoryId).get().await()
            } else {
                query.get().await()
            }
            products = snapshot.documents.mapNotNull { doc ->
                val product = doc.toObject(Product::class.java)
                product?.copy(
                    id_sanpham = doc.getLong("id_sanpham")?.toInt() ?: 0,
                    firestoreId = doc.id
                )
            }
        } catch (e: Exception) {
            println("Lỗi khi tải sản phẩm: $e")
            products = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thú cưng", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFFA5D6A7)
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
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                if (categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Tất cả") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFA5D6A7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFE0E0E0),
                                labelColor = Color(0xFF424242)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategoryId == category.id_category,
                                onClick = { selectedCategoryId = category.id_category },
                                label = { Text(category.ten ?: "Không tên") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFA5D6A7),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFE0E0E0),
                                    labelColor = Color(0xFF424242)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
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
                                text = "Danh sách thú cưng",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF424242)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${products.size} sản phẩm)",
                                fontSize = 16.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }

                    if (products.isEmpty()) {
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
                        items(products) { product ->
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
            }
        }
    )
}