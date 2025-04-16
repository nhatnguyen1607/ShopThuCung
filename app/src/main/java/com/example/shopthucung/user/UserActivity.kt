package com.example.shopthucung.user

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.shopthucung.ui.theme.ShopThuCungTheme
import com.example.shopthucung.user.navigation.NavGraph
import com.example.shopthucung.user.viewmodel.CartViewModel
import com.example.shopthucung.user.viewmodel.OrderViewModel
import com.google.firebase.FirebaseApp

// Định nghĩa CompositionLocal để cung cấp OrderViewModel và CartViewModel
val LocalOrderViewModel = compositionLocalOf<OrderViewModel> { error("No OrderViewModel provided") }
val LocalCartViewModel = compositionLocalOf<CartViewModel> { error("No CartViewModel provided") }

class UserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo Firebase
        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()
        setContent {
            ShopThuCungTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    App(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun App(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    // Khởi tạo OrderViewModel và CartViewModel tại scope của Activity
    val orderViewModel: OrderViewModel = viewModel()
    val cartViewModel: CartViewModel = viewModel()

    // Cung cấp ViewModel thông qua CompositionLocal
    CompositionLocalProvider(
        LocalOrderViewModel provides orderViewModel,
        LocalCartViewModel provides cartViewModel
    ) {
        NavGraph(navController = navController)
    }
}