package com.example.shopthucung.user.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopthucung.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) : ViewModel() {
    // State for product lists
    val trendingProducts = mutableStateOf<List<Product>>(emptyList())
    val newProducts = mutableStateOf<List<Product>>(emptyList())
    val topRatedProducts = mutableStateOf<List<Product>>(emptyList())

    // State for loading and error handling
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    init {
        fetchProducts()
    }

    fun fetchProducts() {
        isLoading.value = true
        errorMessage.value = null

        // Fetch trending products (based on sales)
        viewModelScope.launch {
            try {
                val trendingResult = db.collection("product")
                    .orderBy("so_luong_ban", Query.Direction.DESCENDING)
                    .limit(3)
                    .get()
                    .await()
                trendingProducts.value = trendingResult.toObjects(Product::class.java)
            } catch (e: Exception) {
                errorMessage.value = "Lỗi khi tải sản phẩm nổi bật: ${e.message}"
            }
        }

        // Fetch new products (based on product ID)
        viewModelScope.launch {
            try {
                val newResult = db.collection("product")
                    .orderBy("id_sanpham", Query.Direction.DESCENDING)
                    .limit(3)
                    .get()
                    .await()
                newProducts.value = newResult.toObjects(Product::class.java)
            } catch (e: Exception) {
                errorMessage.value = "Lỗi khi tải sản phẩm mới: ${e.message}"
            }
        }

        // Fetch top-rated products (based on rating)
        viewModelScope.launch {
            try {
                val topRatedResult = db.collection("product")
                    .orderBy("danh_gia", Query.Direction.DESCENDING)
                    .limit(3)
                    .get()
                    .await()
                topRatedProducts.value = topRatedResult.toObjects(Product::class.java)
            } catch (e: Exception) {
                errorMessage.value = "Lỗi khi tải sản phẩm xếp hạng cao: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    // Function to refresh data
    fun refresh() {
        fetchProducts()
    }
}