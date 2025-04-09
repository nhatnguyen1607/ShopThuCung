package com.example.shopthucung.user.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.shopthucung.user.model.User
import com.example.shopthucung.user.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(navController: NavController, userId: String) {
    val db = FirebaseFirestore.getInstance()
    // Khởi tạo UserViewModel trực tiếp
    val viewModel: UserViewModel = viewModel { UserViewModel(db, userId) }
    val user by viewModel.user.collectAsState()
    val message by viewModel.message.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Thông tin cá nhân", "Cài đặt tài khoản")

    // Hiển thị thông báo bằng Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tài khoản",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAFAFA),
                    titleContentColor = Color(0xFF424242)
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFFA5D6A7)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Sử dụng snackbarHost thay vì scaffoldState
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TabRow
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFFAFAFA),
                contentColor = Color(0xFFA5D6A7)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) Color(0xFFA5D6A7) else Color(0xFF757575)
                            )
                        }
                    )
                }
            }

            // Nội dung của tab
            when (selectedTab) {
                0 -> PersonalInfoTab(user = user, onUpdateUser = { updatedUser: User ->
                    viewModel.updateUser(updatedUser)
                })
                1 -> AccountSettingsTab(
                    user = user,
                    onUpdateUser = { updatedUser: User ->
                        viewModel.updateUser(updatedUser)
                    },
                    onChangePassword = { newPassword: String ->
                        viewModel.changePassword(newPassword)
                    },
                    onDeleteAccount = {
                        viewModel.deleteAccount { navController.popBackStack() }
                    }
                )
            }
        }
    }
}

// Tab 1: Thông tin cá nhân
@Composable
fun PersonalInfoTab(user: User?, onUpdateUser: (User) -> Unit) {
    var hoTen by remember { mutableStateOf(user?.hoVaTen ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var soDienThoai by remember { mutableStateOf(user?.sdt ?: "") }
    var diaChi by remember { mutableStateOf(user?.diaChi ?: "") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Thông tin cá nhân",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
        }

        item {
            OutlinedTextField(
                value = hoTen,
                onValueChange = { hoTen = it },
                label = { Text("Họ tên") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        item {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false, // Email đã xác thực, không cho chỉnh sửa
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    disabledBorderColor = Color(0xFFE0E0E0),
                    disabledTextColor = Color(0xFF757575)
                )
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = soDienThoai,
                    onValueChange = { soDienThoai = it },
                    label = { Text("Số điện thoại") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA5D6A7),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { /* TODO: Gửi OTP để xác thực */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF8BBD0),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Xác thực OTP")
                }
            }
        }

        item {
            OutlinedTextField(
                value = diaChi,
                onValueChange = { diaChi = it },
                label = { Text("Địa chỉ giao hàng") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        item {
            Button(
                onClick = {
                    user?.let {
                        val updatedUser = it.copy(
                            hoVaTen = hoTen,
                            sdt = soDienThoai,
                            diaChi = diaChi
                        )
                        onUpdateUser(updatedUser)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA5D6A7),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Lưu thay đổi", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Tab 2: Cài đặt tài khoản
@Composable
fun AccountSettingsTab(
    user: User?,
    onUpdateUser: (User) -> Unit,
    onChangePassword: (String) -> Unit,
    onDeleteAccount: () -> Unit
) {
    var hoTen by remember { mutableStateOf(user?.hoVaTen ?: "") }
    var soDienThoai by remember { mutableStateOf(user?.sdt ?: "") }
    var diaChi by remember { mutableStateOf(user?.diaChi ?: "") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Thay đổi thông tin cá nhân",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
        }

        item {
            OutlinedTextField(
                value = hoTen,
                onValueChange = { hoTen = it },
                label = { Text("Họ tên") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        item {
            OutlinedTextField(
                value = soDienThoai,
                onValueChange = { soDienThoai = it },
                label = { Text("Số điện thoại") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        item {
            OutlinedTextField(
                value = diaChi,
                onValueChange = { diaChi = it },
                label = { Text("Địa chỉ giao hàng") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        item {
            Button(
                onClick = {
                    user?.let {
                        val updatedUser = it.copy(
                            hoVaTen = hoTen,
                            sdt = soDienThoai,
                            diaChi = diaChi
                        )
                        onUpdateUser(updatedUser)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA5D6A7),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Lưu thay đổi", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Text(
                text = "Đổi mật khẩu",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
        }

        item {
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Mật khẩu hiện tại") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        item {
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Mật khẩu mới") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        item {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Xác nhận mật khẩu mới") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }

        item {
            Button(
                onClick = {
                    if (newPassword == confirmPassword && newPassword.isNotEmpty()) {
                        onChangePassword(newPassword)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA5D6A7),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Đổi mật khẩu", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Text(
                text = "Xóa tài khoản",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
        }

        item {
            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Xóa tài khoản", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa tài khoản") },
            text = { Text("Bạn có chắc chắn muốn xóa tài khoản? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAccount()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF757575)
                    )
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}