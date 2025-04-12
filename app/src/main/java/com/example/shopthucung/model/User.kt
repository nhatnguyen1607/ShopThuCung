package com.example.shopthucung.model

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class User(
    @PropertyName("diaChi") val diaChi: String = "",
    @PropertyName("sdt") val sdt: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("hoVaTen") val hoVaTen: String = "",
    @PropertyName("idUser") val idUser: String = "",
    @PropertyName("matKhau") val matKhau: String = ""
)