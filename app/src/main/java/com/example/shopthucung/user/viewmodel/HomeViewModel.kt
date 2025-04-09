package com.example.shopthucung.user.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import com.example.shopthucung.user.model.Product

class HomeViewModel(private val db: FirebaseFirestore) : ViewModel() {
    // Danh sách sản phẩm cho các phần
    val trendingProducts = mutableStateOf<List<Product>>(emptyList())
    val newProducts = mutableStateOf<List<Product>>(emptyList())
    val topRatedProducts = mutableStateOf<List<Product>>(emptyList())

    init {
        fetchProducts()
    }

    private fun fetchProducts() {
        // Lấy sản phẩm nổi bật (dựa trên số lượng bán)
        viewModelScope.launch {
            db.collection("product")
                .orderBy("so_luong_ban", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener { result ->
                    val products = result.toObjects(Product::class.java)
                    trendingProducts.value = products
                }
                .addOnFailureListener { exception ->
                    // Xử lý lỗi (có thể hiển thị thông báo)
                }
        }

        // Lấy sản phẩm mới (dựa trên ID sản phẩm, giả sử ID mới nhất là mới nhất)
        viewModelScope.launch {
            db.collection("product")
                .orderBy("id_sanpham", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener { result ->
                    val products = result.toObjects(Product::class.java)
                    newProducts.value = products
                }
                .addOnFailureListener { exception ->
                    // Xử lý lỗi
                }
        }

        // Lấy sản phẩm xếp hạng cao (dựa trên đánh giá)
        viewModelScope.launch {
            db.collection("product")
                .orderBy("danh_gia", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener { result ->
                    val products = result.toObjects(Product::class.java)
                    topRatedProducts.value = products
                }
                .addOnFailureListener { exception ->
                    // Xử lý lỗi
                }
        }
    }
}