package com.example.shopthucung.admin.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopthucung.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProductViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    init {
        fetchProducts()
    }

    private fun fetchProducts() {
        db.collection("product").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ProductViewModel", "Error fetching products: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val productList = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Product::class.java)?.copy(firestoreId = doc.id)
                    } catch (e: Exception) {
                        Log.e("ProductViewModel", "Error mapping product ${doc.id}: ${e.message}")
                        null
                    }
                }
                _products.value = productList
                Log.d("ProductViewModel", "Fetched ${productList.size} products")
            } else {
                Log.d("ProductViewModel", "No products found")
            }
        }
    }

    fun getProductFromListById(id: String): Product? {
        return products.value.find { it.firestoreId == id }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            try {
                if (product.firestoreId.isNotEmpty()) {
                    db.collection("product").document(product.firestoreId).set(product).await()
                    Log.d("ProductViewModel", "Product updated: ${product.firestoreId}")
                } else {
                    Log.e("ProductViewModel", "Cannot update product: firestoreId is empty")
                }
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error updating product: ${e.message}")
            }
        }
    }

    fun addProduct(product: Product) {
        viewModelScope.launch {
            try {
                // Kiểm tra xem ten_sp có trống không
                if (product.ten_sp.isNullOrEmpty()) {
                    Log.e("ProductViewModel", "Cannot add product: ten_sp is null or empty")
                    return@launch
                }

                // Lấy số lượng sản phẩm hiện có để tạo id_sanpham mới
                val snapshot = db.collection("product").get().await()
                val newId = snapshot.size() + 1

                // Sử dụng ten_sp làm tên document
                val newProduct = product.copy(id_sanpham = newId, firestoreId = product.ten_sp)

                // Kiểm tra xem document với ten_sp đã tồn tại chưa
                val existingDoc = db.collection("product").document(product.ten_sp).get().await()
                if (existingDoc.exists()) {
                    Log.e("ProductViewModel", "Product with ten_sp '${product.ten_sp}' already exists")
                    return@launch
                }

                // Thêm sản phẩm mới với ten_sp làm tên document
                db.collection("product").document(product.ten_sp).set(newProduct).await()
                Log.d("ProductViewModel", "Product added with ID: ${product.ten_sp}, id_sanpham: $newId")
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error adding product: ${e.message}")
            }
        }
    }
}