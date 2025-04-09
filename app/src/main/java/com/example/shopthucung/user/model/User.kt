package com.example.shopthucung.user.model

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

@Keep
data class User(
    @PropertyName("Dia_chi") val diaChi: String = "",
    @PropertyName("SDT") val sdt: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("ho_va_ten") val hoVaTen: String = "",
    @PropertyName("id_user") val idUser: Long = 0L,
    @PropertyName("mat_khau") val matKhau: String = ""
)