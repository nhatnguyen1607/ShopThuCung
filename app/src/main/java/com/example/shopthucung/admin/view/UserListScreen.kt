package com.example.shopthucung.admin.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.shopthucung.admin.viewmodel.UserViewModel
import com.example.shopthucung.model.User

@Composable
fun UserListScreen(
    viewModel: UserViewModel,
    navController: NavController
) {
    val userList by viewModel.users.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Danh sách người dùng",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(userList) { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { navController.navigate("user_detail/${user.idUser}") }
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Text("Người dùng: ${user.hoVaTen}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}