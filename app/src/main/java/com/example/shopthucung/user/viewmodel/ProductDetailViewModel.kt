package com.example.shopthucung.user.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopthucung.model.Product
import com.example.shopthucung.model.Review
import com.example.shopthucung.model.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProductDetailViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _productState = MutableStateFlow<Product?>(null)
    val productState: StateFlow<Product?> = _productState.asStateFlow()

    private val _reviewsState = MutableStateFlow<List<Review>>(emptyList())
    val reviewsState: StateFlow<List<Review>> = _reviewsState.asStateFlow()

    private val _originalReviewsState = MutableStateFlow<List<Review>>(emptyList())

    private val _usersState = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersState: StateFlow<Map<String, User>> = _usersState.asStateFlow()

    private val _averageRating = MutableStateFlow(0f)
    val averageRating: StateFlow<Float> = _averageRating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedFilter = MutableStateFlow<Int?>(null)
    val selectedFilter: StateFlow<Int?> = _selectedFilter.asStateFlow()

    fun fetchProduct(productId: Int) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("product")
                    .whereEqualTo("id_sanpham", productId)
                    .get()
                    .await()
                if (!snapshot.isEmpty) {
                    val product = snapshot.documents[0].toObject(Product::class.java)
                    _productState.value = product
                } else {
                    _errorMessage.value = "Không tìm thấy sản phẩm với ID: $productId"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khi tải sản phẩm: ${e.message}"
            }
        }
    }

    fun fetchReviews(productId: Int) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("products_reviews")
                    .document(productId.toString())
                    .collection("reviews")
                    .get()
                    .await()

                val reviews = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Review::class.java)?.copy(id = doc.id)
                }
                _originalReviewsState.value = reviews
                _reviewsState.value = reviews

                if (reviews.isNotEmpty()) {
                    val totalRating = reviews.sumOf { it.rating }
                    _averageRating.value = totalRating.toFloat() / reviews.size
                } else {
                    _averageRating.value = 0f
                }

                val userIds = reviews.map { it.idUser }.distinct()
                val userMap = mutableMapOf<String, User>()
                for (userId in userIds) {
                    try {
                        val document = db.collection("user")
                            .document(userId)
                            .get()
                            .await()
                        if (document.exists()) {
                            val user = document.toObject(User::class.java)
                            if (user != null) {
                                userMap[userId] = user
                            }
                        }
                    } catch (e: Exception) {
                        _errorMessage.value = "Lỗi khi tải thông tin người dùng: ${e.message}"
                    }
                }
                _usersState.value = userMap
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi khi tải đánh giá: ${e.message}"
            }
        }
    }

    fun setFilter(stars: Int?) {
        _selectedFilter.value = stars
        _reviewsState.value = if (stars == null) {
            _originalReviewsState.value
        } else {
            _originalReviewsState.value.filter { it.rating == stars }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
    }
}