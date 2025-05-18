package com.example.shopthucung.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Notifications(
    @PropertyName("content") val content: String = "",
    @PropertyName("idNotification") var idNotification: Int = 0,
    @PropertyName("orderId") var orderId: String = "",
    @PropertyName("idUser") var idUser: String = "",
    @PropertyName("notdate") var notdate: com.google.firebase.Timestamp? = null
)
