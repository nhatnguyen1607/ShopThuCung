package com.example.shopthucung.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.shopthucung.admin.repository.ProductRepository
import com.example.shopthucung.admin.nav.AdminNavGraph
import com.example.shopthucung.admin.viewmodel.ProductViewModel
import com.example.shopthucung.admin.repository.UserRepository
import com.example.shopthucung.admin.viewmodel.UserViewModel
import com.google.firebase.FirebaseApp

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Khởi tạo Firebase (có thể bỏ nếu đã cấu hình tự động qua plugin Google Services)
        FirebaseApp.initializeApp(this)
        setContent {
            MaterialTheme {
                Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    AdminNavGraph(
                        productViewModel = ProductViewModel(),
                        userViewModel = UserViewModel(UserRepository())
                    )
                }
            }
        }
    }
}