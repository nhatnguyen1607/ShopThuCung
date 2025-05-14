package com.example.shopthucung.model

import com.google.firebase.firestore.PropertyName

data class Banner(
    @PropertyName("id_banner") var id_banner: Int = 0,
    @PropertyName("anh_banner") var anh_banner: String = "",
    @PropertyName("status") var status: String = "Bật"
)
