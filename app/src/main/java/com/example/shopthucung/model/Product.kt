package com.example.shopthucung.model

data class Product(
    val anh_sp: List<String> = emptyList(),
    val danh_gia: Float = 0f,
    val gia_sp: Long = 0L,
    var id_sanpham: Int = 0,
    var firestoreId: String = "",
    val mo_ta: String = "",
    var so_luong_ban: Int = 0,
    var soluong: Int = 0,
    val ten_sp: String = "",
    val giam_gia: Int = 5,
    var id_category: Int = 0
)