package com.example.shopthucung.user.view

import android.R.id.message
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.shopthucung.model.User
import com.example.shopthucung.user.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(navController: NavController, uid: String) {
    val db = FirebaseFirestore.getInstance()
    val viewModel: UserViewModel = viewModel { UserViewModel(db, uid) }
    val user by viewModel.user.collectAsState()
    val message by viewModel.message.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Thông tin cá nhân", "Cài đặt tài khoản")

    val snackbarHostState = remember { SnackbarHostState() }

    // Làm mới dữ liệu mỗi khi UserScreen được hiển thị
    LaunchedEffect(Unit) {
        viewModel.refreshUser()
    }

    // Chuyển hướng về màn hình đăng nhập nếu không tìm thấy dữ liệu
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            if (it.contains("Không tìm thấy thông tin người dùng")) {
                FirebaseAuth.getInstance().signOut()
                navController.navigate("login") {
                    popUpTo("user/$uid") { inclusive = true }
                }
            }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFA5D6A7))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
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

                when (selectedTab) {
                    0 -> PersonalInfoTab(user = user)
                    1 -> AccountSettingsTab(
                        user = user,
                        onUpdateUser = { updatedUser: User ->
                            viewModel.updateUser(updatedUser)
                        },
                        onChangePassword = { newPassword: String ->
                            viewModel.changePassword(newPassword)
                        },
                        onDeleteAccount = { password: String ->
                            viewModel.deleteAccount(password) {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("login") {
                                    popUpTo("user/$uid") { inclusive = true }
                                }
                            }
                        },
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("login") {
                                popUpTo("user/$uid") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalInfoTab(user: User?) {
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
                value = user?.hoVaTen ?: "",
                onValueChange = { /* Không cho phép chỉnh sửa */ },
                label = { Text("Họ tên") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    disabledBorderColor = Color(0xFFE0E0E0),
                    disabledTextColor = Color(0xFF757575)
                )
            )
        }

        item {
            OutlinedTextField(
                value = user?.email ?: "",
                onValueChange = { /* Không cho phép chỉnh sửa */ },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
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
                    value = user?.sdt ?: "",
                    onValueChange = { /* Không cho phép chỉnh sửa */ },
                    label = { Text("Số điện thoại") },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA5D6A7),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        disabledBorderColor = Color(0xFFE0E0E0),
                        disabledTextColor = Color(0xFF757575)
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
                value = user?.diaChi ?: "",
                onValueChange = { /* Không cho phép chỉnh sửa */ },
                label = { Text("Địa chỉ giao hàng") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    disabledBorderColor = Color(0xFFE0E0E0),
                    disabledTextColor = Color(0xFF757575)
                )
            )
        }
    }
}

@Composable
fun AccountSettingsTab(
    user: User?,
    onUpdateUser: (User) -> Unit,
    onChangePassword: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onLogout: () -> Unit
) {
    var hoTen by remember(user) { mutableStateOf(user?.hoVaTen ?: "") }
    var soDienThoai by remember(user) { mutableStateOf(user?.sdt ?: "") }
    var diaChi by remember(user) { mutableStateOf(user?.diaChi ?: "") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var deletePassword by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let {
            hoTen = it.hoVaTen
            soDienThoai = it.sdt
            diaChi = it.diaChi
        }
    }

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
                        isLoading = true
                        val updatedUser = it.copy(
                            hoVaTen = hoTen,
                            sdt = soDienThoai,
                            diaChi = diaChi
                        )
                        onUpdateUser(updatedUser)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && user != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA5D6A7),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Lưu thay đổi", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
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
            OutlinedTextField(
                value = deletePassword,
                onValueChange = { deletePassword = it },
                label = { Text("Nhập mật khẩu để xóa tài khoản") },
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
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = deletePassword.isNotEmpty()
            ) {
                Text("Xóa tài khoản", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Text(
                text = "Đăng xuất",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
        }

        item {
            Button(
                onClick = { onLogout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFA726),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Đăng xuất", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                        onDeleteAccount(deletePassword)
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

    LaunchedEffect(message) {
        if (message != null) {
            isLoading = false
        }
    }
}