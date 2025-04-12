package com.example.shopthucung.admin.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.shopthucung.admin.viewmodel.ProductViewModel
import com.example.shopthucung.model.Product

@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    navController: NavController
) {
    val productList by viewModel.products.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Danh sách sản phẩm",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (productList.isEmpty()) {
            Text(
                text = "Không có sản phẩm nào",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        } else {
            LazyColumn {
                items(productList) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { navController.navigate("product_detail/${product.firestoreId}") }
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Sản phẩm: ${product.ten_sp}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Giá: ${product.gia_sp} VNĐ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Số lượng tồn kho: ${product.soluong}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Số lượng đã bán: ${product.so_luong_ban}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}