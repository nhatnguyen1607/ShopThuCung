package com.example.shopthucung.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Review(
    val comment: String = "",
    val idUser: String = "",
    val id_sanpham: Int = 0,
    val rating: Int = 0,
    val timestamp: Timestamp? = null,
    @Exclude
    val id: String = ""
)