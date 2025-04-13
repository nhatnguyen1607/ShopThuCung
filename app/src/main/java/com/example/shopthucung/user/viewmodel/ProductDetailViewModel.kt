package com.example.shopthucung.user.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopthucung.user.model.Product
import com.example.shopthucung.user.model.Review
import com.example.shopthucung.user.model.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductDetailViewModel : ViewModel() {
    private val db = Firebase.firestore

    // Trạng thái cho sản phẩm
    private val _productState = MutableStateFlow<Product?>(null)
    val productState: StateFlow<Product?> = _productState.asStateFlow()

    // Trạng thái cho danh sách đánh giá
    private val _reviewsState = MutableStateFlow<List<Review>>(emptyList())
    val reviewsState: StateFlow<List<Review>> = _reviewsState.asStateFlow()

    // Trạng thái cho thông tin người dùng
    private val _usersState = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersState: StateFlow<Map<String, User>> = _usersState.asStateFlow()

    // Trạng thái cho thông báo lỗi
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchProduct(productId: Int) {
        viewModelScope.launch {
            db.collection("product")
                .whereEqualTo("id_sanpham", productId)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val productData = result.documents[0].toObject(Product::class.java)
                        _productState.value = productData
                    } else {
                        _errorMessage.value = "Không tìm thấy sản phẩm với ID: $productId"
                    }
                }
                .addOnFailureListener { e ->
                    _errorMessage.value = "Lỗi khi tải sản phẩm: ${e.message}"
                }
        }
    }

    fun fetchReviews(productId: Int) {
        viewModelScope.launch {
            db.collection("review")
                .whereEqualTo("id_sanpham", productId)
                .get()
                .addOnSuccessListener { result ->
                    val reviews = result.toObjects(Review::class.java)
                    _reviewsState.value = reviews

                    // Fetch user data for each review
                    val userIds = reviews.map { it.idUser }.distinct()
                    val userMap = mutableMapOf<String, User>()
                    userIds.forEach { userId ->
                        db.collection("user")
                            .document(userId)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val user = document.toObject(User::class.java)
                                    if (user != null) {
                                        userMap[userId] = user
                                    }
                                }
                                _usersState.value = userMap
                            }
                            .addOnFailureListener { e ->
                                _errorMessage.value = "Lỗi khi tải thông tin người dùng: ${e.message}"
                            }
                    }
                }
                .addOnFailureListener { e ->
                    _errorMessage.value = "Lỗi khi tải đánh giá: ${e.message}"
                }
        }
    }
}