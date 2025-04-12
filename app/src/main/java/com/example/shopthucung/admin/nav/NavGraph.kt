package com.example.shopthucung.admin.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shopthucung.admin.view.*
import com.example.shopthucung.admin.viewmodel.ProductViewModel
import com.example.shopthucung.admin.viewmodel.UserViewModel

@Composable
fun AdminNavGraph(
    productViewModel: ProductViewModel,
    userViewModel: UserViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            AdminDashboardScreen(
                navController = navController,
                productViewModel = productViewModel,
                userViewModel = userViewModel
            )
        }
        composable("product_list") {
            ProductListScreen(
                viewModel = productViewModel,
                navController = navController
            )
        }
        composable("product_detail/{firestoreId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("firestoreId") ?: ""
            ProductDetailScreen(
                productId = productId, // Đây là firestoreId
                viewModel = productViewModel,
                navController = navController
            )
        }
        composable("add_product") {
            AddProductScreen(
                viewModel = productViewModel,
                navController = navController
            )
        }
        composable("user_list") {
            UserListScreen(
                viewModel = userViewModel,
                navController = navController
            )
        }
        composable("user_detail/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserDetailScreen(
                userId = userId,
                viewModel = userViewModel,
                navController = navController
            )
        }
    }
}