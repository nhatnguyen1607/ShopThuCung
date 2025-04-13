package com.example.shopthucung.admin.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.shopthucung.model.Product
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val db = FirebaseFirestore.getInstance()
    private val productsCollection = db.collection("products")

    // Lấy tất cả sản phẩm
    suspend fun getAllProducts(): List<Product> {
        return productsCollection.get().await().toObjects(Product::class.java)
    }

    // Lấy sản phẩm bán chạy
    suspend fun getTrendingProducts(): List<Product> {
        return productsCollection
            .orderBy("so_luong_ban", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()
            .toObjects(Product::class.java)
    }

    // Lấy sản phẩm mới
    suspend fun getNewProducts(): List<Product> {
        return productsCollection
            .orderBy("ngay_them", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()
            .toObjects(Product::class.java)
    }

    // Lấy sản phẩm đánh giá cao
    suspend fun getTopRatedProducts(): List<Product> {
        return productsCollection
            .orderBy("danh_gia", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()
            .toObjects(Product::class.java)
    }

    // Thêm sản phẩm
    suspend fun insertProduct(product: Product): String {
        val docRef = productsCollection.add(product).await()
        return docRef.id // Trả về ID của document vừa tạo
    }

    // Cập nhật sản phẩm
    suspend fun updateProduct(product: Product) {
        productsCollection.document(product.id_sanpham.toString()).set(product).await()
    }

    // Xóa sản phẩm
    suspend fun deleteProduct(productId: String) {
        productsCollection.document(productId).delete().await()
    }

    // Lấy sản phẩm theo ID
    suspend fun getProductById(id: String): Product? {
        return productsCollection.document(id).get().await().toObject(Product::class.java)
    }
}