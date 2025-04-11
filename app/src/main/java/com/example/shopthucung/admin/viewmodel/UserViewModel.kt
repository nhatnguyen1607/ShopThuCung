package com.example.shopthucung.admin.viewmodel

import androidx.lifecycle.ViewModel
import com.example.shopthucung.admin.repository.UserRepository
import com.example.shopthucung.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserViewModel(private val repository: UserRepository) : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        repository.getUsers { users ->
            _users.value = users
        }
    }

    suspend fun getUserById(userId: String): User? {
        return repository.getUserById(userId)
    }

    fun updateUser(user: User) {
        repository.updateUser(user)
    }
}