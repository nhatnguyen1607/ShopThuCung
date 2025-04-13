package com.example.shopthucung.model

import com.google.firebase.Timestamp

data class Review(
    val idUser: String = "",
    val id_sanpham: Int = 0,
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Timestamp? = null
)
