package com.example.shopthucung.user.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopthucung.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.shopthucung.model.CartItem

class CartViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // Trạng thái cho giỏ hàng
    private val _cartItemsState = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItemsState: StateFlow<List<CartItem>> = _cartItemsState.asStateFlow()

    // Trạng thái cho thông báo lỗi
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Trạng thái cho thông báo thành công
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun addToCart(product: Product) {
        Log.d("CartViewModel", "Bắt đầu addToCart cho sản phẩm: ${product.ten_sp}")
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.w("CartViewModel", "Người dùng chưa đăng nhập!")
                    _errorMessage.value = "Người dùng chưa đăng nhập!"
                    return@launch
                }

                val existingItem = db.collection("cart")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("productId", product.id_sanpham)
                    .get()
                    .await()

                if (!existingItem.isEmpty) {
                    // Nếu sản phẩm đã tồn tại, tăng quantity
                    val doc = existingItem.documents[0]
                    val currentQuantity = doc.getLong("quantity")?.toInt() ?: 1
                    doc.reference.update("quantity", currentQuantity + 1).await()
                    _successMessage.value = "Đã tăng số lượng sản phẩm trong giỏ hàng!"
                    Log.d("CartViewModel", "Đã cập nhật số lượng: ${_successMessage.value}")
                    // Cập nhật cục bộ
                    _cartItemsState.value = _cartItemsState.value.map {
                        if (it.productId == product.id_sanpham) {
                            it.copy(quantity = currentQuantity + 1)
                        } else {
                            it
                        }
                    }
                    fetchCartItems() // Đồng bộ lại từ Firestore
                } else {
                    // Nếu sản phẩm chưa tồn tại, thêm mới
                    Log.d("CartViewModel", "Thêm sản phẩm mới vào giỏ hàng")
                    val querySnapshot = db.collection("cart")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()

                    var maxIndex = 0
                    for (document in querySnapshot.documents) {
                        val docId = document.id
                        val index = docId.substringAfterLast("_").toIntOrNull() ?: 0
                        if (index > maxIndex) {
                            maxIndex = index
                        }
                    }

                    val newIndex = maxIndex + 1
                    val newDocId = "${userId}_$newIndex"
                    Log.d("CartViewModel", "Tạo document mới với ID: $newDocId")

                    val cartItem = hashMapOf(
                        "userId" to userId,
                        "productId" to product.id_sanpham,
                        "quantity" to 1,
                        "product" to product,
                        "cartIndex" to newIndex
                    )

                    db.collection("cart")
                        .document(newDocId)
                        .set(cartItem)
                        .await()
                    Log.d("CartViewModel", "Đã thêm sản phẩm vào giỏ hàng với ID: $newDocId")
                    _successMessage.value = "Thêm vào giỏ hàng thành công!"
                    Log.d("CartViewModel", "Đã thêm vào giỏ hàng: ${_successMessage.value}")
                    // Cập nhật cục bộ
                    _cartItemsState.value = _cartItemsState.value + CartItem(
                        userId = userId,
                        productId = product.id_sanpham,
                        quantity = 1,
                        product = product,
                        cartIndex = newIndex
                    )
                    fetchCartItems() // Đồng bộ lại từ Firestore
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Lỗi trong addToCart: ${e.message}", e)
                _errorMessage.value = "Lỗi khi xử lý giỏ hàng: ${e.message}"
            }
        }
    }

    fun fetchCartItems() {
        Log.d("CartViewModel", "Bắt đầu fetchCartItems")
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.w("CartViewModel", "Người dùng chưa đăng nhập trong fetchCartItems")
                    _errorMessage.value = "Người dùng chưa đăng nhập!"
                    return@launch
                }

                val result = db.collection("cart")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val cartItems = result.documents.mapNotNull { doc ->
                    doc.toObject(CartItem::class.java)?.apply {
                        Log.d("CartViewModel", "Lấy được mục giỏ hàng: ${this.product?.ten_sp}, quantity: ${this.quantity}")
                    }
                }
                _cartItemsState.value = cartItems
                Log.d("CartViewModel", "Đã tải ${cartItems.size} mục trong giỏ hàng")
            } catch (e: Exception) {
                Log.e("CartViewModel", "Lỗi trong fetchCartItems: ${e.message}", e)
                _errorMessage.value = "Lỗi khi tải giỏ hàng: ${e.message}"
            }
        }
    }

    fun updateQuantity(cartItem: CartItem, newQuantity: Int) {
        Log.d("CartViewModel", "Bắt đầu updateQuantity cho sản phẩm: ${cartItem.product?.ten_sp}, số lượng mới: $newQuantity")
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.w("CartViewModel", "Người dùng chưa đăng nhập trong updateQuantity")
                    _errorMessage.value = "Người dùng chưa đăng nhập!"
                    return@launch
                }

                if (newQuantity <= 0) {
                    removeCartItem(cartItem)
                    return@launch
                }

                val docId = "${userId}_${cartItem.cartIndex}"
                val docSnapshot = db.collection("cart")
                    .document(docId)
                    .get()
                    .await()

                if (docSnapshot.exists()) {
                    db.collection("cart")
                        .document(docId)
                        .update("quantity", newQuantity)
                        .await()
                    _successMessage.value = if (newQuantity > cartItem.quantity) {
                        "Đã tăng số lượng sản phẩm!"
                    } else {
                        "Đã giảm số lượng sản phẩm!"
                    }
                    Log.d("CartViewModel", "Đã cập nhật số lượng: ${_successMessage.value}")
                    // Cập nhật cục bộ
                    _cartItemsState.value = _cartItemsState.value.map {
                        if (it.cartIndex == cartItem.cartIndex && it.userId == userId) {
                            it.copy(quantity = newQuantity)
                        } else {
                            it
                        }
                    }
                    fetchCartItems() // Đồng bộ lại từ Firestore
                } else {
                    Log.w("CartViewModel", "Không tìm thấy sản phẩm để cập nhật: $docId")
                    _errorMessage.value = "Không tìm thấy sản phẩm trong giỏ hàng để cập nhật!"
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Lỗi trong updateQuantity: ${e.message}", e)
                _errorMessage.value = "Lỗi khi cập nhật số lượng: ${e.message}"
            }
        }
    }

    fun removeCartItem(cartItem: CartItem) {
        Log.d("CartViewModel", "Bắt đầu removeCartItem cho sản phẩm: ${cartItem.product?.ten_sp}")
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.w("CartViewModel", "Người dùng chưa đăng nhập trong removeCartItem")
                    _errorMessage.value = "Người dùng chưa đăng nhập!"
                    return@launch
                }

                val docId = "${userId}_${cartItem.cartIndex}"
                val docSnapshot = db.collection("cart")
                    .document(docId)
                    .get()
                    .await()

                if (docSnapshot.exists()) {
                    db.collection("cart")
                        .document(docId)
                        .delete()
                        .await()
                    Log.d("CartViewModel", "Đã xóa sản phẩm: $docId")
                    _successMessage.value = "Đã xóa sản phẩm khỏi giỏ hàng!"
                    Log.d("CartViewModel", "Đã xóa sản phẩm: ${_successMessage.value}")
                    // Cập nhật cục bộ
                    _cartItemsState.value = _cartItemsState.value.filterNot {
                        it.cartIndex == cartItem.cartIndex && it.userId == userId
                    }
                    fetchCartItems() // Đồng bộ lại từ Firestore
                } else {
                    Log.w("CartViewModel", "Không tìm thấy sản phẩm để xóa: $docId")
                    _errorMessage.value = "Không tìm thấy sản phẩm trong giỏ hàng để xóa!"
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Lỗi trong removeCartItem: ${e.message}", e)
                _errorMessage.value = "Lỗi khi xóa sản phẩm: ${e.message}"
            }
        }
    }

    // Hàm để reset thông báo sau khi hiển thị
    fun clearMessages() {
        Log.d("CartViewModel", "Clear messages: successMessage=${_successMessage.value}, errorMessage=${_errorMessage.value}")
        _successMessage.value = null
        _errorMessage.value = null
    }
}