package com.example.shopthucung.user.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopthucung.user.model.Product
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

    // Function to add product to cart (assuming cart is stored in Firestore)
    fun addToCart(product: Product) {
        viewModelScope.launch {
            val userId = "current_user_id" // Replace with actual user ID from Firebase Auth
            val cartItem = hashMapOf(
                "userId" to userId,
                "productId" to product.id_sanpham,
                "quantity" to 1,
                "product" to product
            )
            db.collection("cart")
                .document("${userId}_${product.id_sanpham}")
                .set(cartItem)
                .addOnFailureListener { e ->
                    errorMessage.value = "Lỗi khi thêm vào giỏ hàng: ${e.message}"
                }
        }
    }
}