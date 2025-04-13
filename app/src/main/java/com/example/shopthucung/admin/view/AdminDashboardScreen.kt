package com.example.shopthucung.admin.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.shopthucung.admin.viewmodel.ProductViewModel
import com.example.shopthucung.admin.viewmodel.UserViewModel
import com.example.shopthucung.model.Product

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    productViewModel: ProductViewModel,
    userViewModel: UserViewModel
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bảng điều khiển Admin",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { navController.navigate("product_list") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quản lý sản phẩm")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Thêm sản phẩm mới")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { navController.navigate("user_list") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quản lý người dùng")
        }

        if (showDialog) {
            AddProductDialog(
                onDismiss = { showDialog = false },
                onAdd = { product ->
                    productViewModel.addProduct(product)
                    showDialog = false
                }
            )
        }
    }
}
