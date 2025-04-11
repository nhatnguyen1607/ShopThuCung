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
        db.collection("products").addSnapshotListener { snapshot, error ->
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

    suspend fun getProductById(firestoreId: String): Product? {
        return try {
            val snapshot = db.collection("products").document(firestoreId).get().await()
            snapshot.toObject(Product::class.java)?.copy(firestoreId = snapshot.id)
        } catch (e: Exception) {
            Log.e("ProductViewModel", "Error getting product $firestoreId: ${e.message}")
            null
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            try {
                if (product.firestoreId.isNotEmpty()) {
                    db.collection("products").document(product.firestoreId).set(product).await()
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
                val snapshot = db.collection("products").get().await()
                val newId = snapshot.size() + 1
                val newProduct = product.copy(id_sanpham = newId)
                val docRef = db.collection("products").add(newProduct).await()
                val updatedProduct = newProduct.copy(firestoreId = docRef.id)
                db.collection("products").document(docRef.id).set(updatedProduct).await()
                Log.d("ProductViewModel", "Product added with ID: ${docRef.id}, id_sanpham: $newId")
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error adding product: ${e.message}")
            }
        }
    }
}