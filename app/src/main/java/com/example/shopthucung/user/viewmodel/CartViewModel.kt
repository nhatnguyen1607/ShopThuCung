
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
                    Log.d("CartViewModel", "Người dùng chưa đăng nhập")
                    _errorMessage.value = "Người dùng chưa đăng nhập!"
                    return@launch
                }
                Log.d("CartViewModel", "User ID: $userId")

                // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
                Log.d("CartViewModel", "Kiểm tra sản phẩm trong giỏ hàng với productId: ${product.id_sanpham}")
                val existingItem = db.collection("cart")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("productId", product.id_sanpham)
                    .get()
                    .await()
                Log.d("CartViewModel", "Kết quả kiểm tra: ${existingItem.documents.size} mục")

                if (!existingItem.isEmpty) {
                    // Nếu sản phẩm đã tồn tại, tăng quantity
                    val doc = existingItem.documents[0]
                    val currentQuantity = doc.getLong("quantity")?.toInt() ?: 1
                    Log.d("CartViewModel", "Sản phẩm đã tồn tại, quantity hiện tại: $currentQuantity")
                    doc.reference.update("quantity", currentQuantity + 1).await()
                    Log.d("CartViewModel", "Đã cập nhật quantity thành: ${currentQuantity + 1}")
                    _successMessage.value = "Đã tăng số lượng sản phẩm trong giỏ hàng!"
                    fetchCartItems()
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
                    fetchCartItems()
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Lỗi trong addToCart: ${e.message}", e)
                _errorMessage.value = "Lỗi khi xử lý giỏ hàng: ${e.message}"
            }
        }
    }

    fun fetchCartItems() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.d("CartViewModel", "Người dùng chưa đăng nhập trong fetchCartItems")
                    _errorMessage.value = "Người dùng chưa đăng nhập!"
                    return@launch
                }

                val result = db.collection("cart")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val cartItems = result.documents.mapNotNull { doc ->
                    doc.toObject(CartItem::class.java)
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
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.d("CartViewModel", "Người dùng chưa đăng nhập trong updateQuantity")
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
                    fetchCartItems()
                } else {
                    Log.d("CartViewModel", "Không tìm thấy sản phẩm để cập nhật: $docId")
                    _errorMessage.value = "Không tìm thấy sản phẩm trong giỏ hàng để cập nhật!"
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Lỗi trong updateQuantity: ${e.message}", e)
                _errorMessage.value = "Lỗi khi cập nhật số lượng: ${e.message}"
            }
        }
    }

    fun removeCartItem(cartItem: CartItem) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.d("CartViewModel", "Người dùng chưa đăng nhập trong removeCartItem")
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
                    fetchCartItems()
                } else {
                    Log.d("CartViewModel", "Không tìm thấy sản phẩm để xóa: $docId")
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
        Log.d("CartViewModel", "Clear messages")
        _successMessage.value = null
        _errorMessage.value = null
    }
}
