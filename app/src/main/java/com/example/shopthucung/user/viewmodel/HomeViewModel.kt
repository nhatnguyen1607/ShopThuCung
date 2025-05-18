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
    val trendingProducts = mutableStateOf<List<Product>>(emptyList())
    val newProducts = mutableStateOf<List<Product>>(emptyList())
    val topRatedProducts = mutableStateOf<List<Product>>(emptyList())

    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    init {
        fetchProducts()
    }

    fun fetchProducts() {
        isLoading.value = true
        errorMessage.value = null

        db.collection("product")
            .orderBy("so_luong_ban", Query.Direction.DESCENDING)
            .orderBy("id_sanpham", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage.value = "Lỗi khi tải sản phẩm nổi bật: ${error.message}"
                    isLoading.value = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trendingProducts.value = snapshot.toObjects(Product::class.java)
                    isLoading.value = false
                }
            }

        viewModelScope.launch {
            try {
                val newResult = db.collection("product")
                    .orderBy("id_sanpham", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()
                newProducts.value = newResult.toObjects(Product::class.java)
            } catch (e: Exception) {
                errorMessage.value = "Lỗi khi tải sản phẩm mới: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }

        db.collection("product")
            .orderBy("danh_gia", Query.Direction.DESCENDING)
            .orderBy("id_sanpham", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage.value = "Lỗi khi tải sản phẩm xếp hạng cao: ${error.message}"
                    isLoading.value = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    topRatedProducts.value = snapshot.toObjects(Product::class.java)
                    isLoading.value = false
                }
            }
    }

    fun refresh() {
        fetchProducts()
    }

    override fun onCleared() {
        super.onCleared()
    }
}