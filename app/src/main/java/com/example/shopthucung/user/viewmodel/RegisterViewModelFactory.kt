package com.example.shopthucung.user.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore
import android.app.Activity

class RegisterViewModelFactory(
    private val firestore: FirebaseFirestore,
    private val activity: Activity?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(firestore, activity!!) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}