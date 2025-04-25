package com.example.shopthucung.user.view

import android.R.id.message
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import coil.compose.AsyncImage
import com.example.shopthucung.model.Order
import com.example.shopthucung.model.Product
import com.example.shopthucung.model.User
import com.example.shopthucung.user.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Timestamp
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(navController: NavController, uid: String) {
    val db = FirebaseFirestore.getInstance()
    val viewModel: UserViewModel = viewModel { UserViewModel(db, uid) }
    val user by viewModel.user.collectAsState()
    val message by viewModel.message.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val orders by viewModel.orders.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Thông tin cá nhân", "Đơn hàng của tôi", "Cài đặt tài khoản")

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refreshUser()
        viewModel.refreshOrders()
    }

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
                    1 -> MyOrdersTab(
                        orders = orders,
                        navController = navController,
                        viewModel = viewModel,
                        uid = uid
                    )
                    2 -> AccountSettingsTab(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersTab(
    orders: List<Order>,
    navController: NavController,
    viewModel: UserViewModel,
    uid: String
) {
    val displayStatuses =
        listOf("Tất cả", "Đang xử lí", "Đã xác nhận", "Đang giao hàng", "Giao thành công", "Đã hủy")
    val statusMapping = mapOf(
        "Đang xử lí" to listOf("Đang xử lí"),
        "Đã xác nhận" to listOf("Đã xác nhận"),
        "Đang giao hàng" to listOf("Đang giao hàng"),
        "Giao thành công" to listOf("Giao thành công"),
        "Đã hủy" to listOf("Đã hủy")
    )
    var selectedStatus by remember { mutableStateOf("Tất cả") }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedStatus,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Trạng thái đơn hàng") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA5D6A7),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    displayStatuses.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status) },
                            onClick = {
                                selectedStatus = status
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        val filteredOrders = if (selectedStatus == "Tất cả") {
            orders
        } else {
            val mappedStatuses = statusMapping[selectedStatus] ?: emptyList()
            orders.filter { order -> mappedStatuses.contains(order.status) }
        }

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không có đơn hàng nào trong trạng thái này",
                    fontSize = 16.sp,
                    color = Color(0xFF757575)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredOrders) { order ->
                    OrderItem(
                        order = order,
                        onClick = {
                            val orderJson = Gson().toJson(order)
                            navController.navigate("order_detail/$orderJson")
                        },
                        onReceivedClick = {
                            if (order.status == "Đang giao hàng") {
                                viewModel.updateOrderStatus(order.orderId, "Giao thành công")
                            } else if (order.status == "Đang xử lí" || order.status == "Đã xác nhận") {
                                viewModel.updateOrderStatus(order.orderId, "Đã hủy")
                            }
                        },
                        navController = navController, // Truyền navController
                        uid = uid // Truyền uid
                    )
                }
            }
        }
    }
}

@Composable
fun OrderItem(
    order: Order,
    onClick: () -> Unit,
    onReceivedClick: () -> Unit,
    navController: NavController,
    uid: String
) {
    // Thêm trạng thái để kiểm soát việc hủy đơn
    var isCancelling by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Sản phẩm: ${order.product?.ten_sp ?: "Không có thông tin"}",
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Số lượng: ${order.quantity}",
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tổng tiền: ${order.totalPrice} VNĐ",
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trạng thái:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF424242)
                        )
                        when (order.status) {
                            "Đang xử lí", "Đã xác nhận" -> {
                                Text(
                                    text = order.status,
                                    fontSize = 14.sp,
                                    color = Color(0xFFFFFFFF),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            color = Color(0xFF2196F3),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                )
                            }

                            "Đang giao hàng" -> {
                                Text(
                                    text = order.status,
                                    fontSize = 14.sp,
                                    color = Color(0xFFFFFFFF),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            color = Color(0xFF00FFCC),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                )
                            }

                            "Giao thành công" -> {
                                Text(
                                    text = order.status,
                                    fontSize = 14.sp,
                                    color = Color(0xFFFFFFFF),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            color = Color(0xFF00FF00),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                )
                            }

                            "Đã hủy" -> {
                                Text(
                                    text = order.status,
                                    fontSize = 14.sp,
                                    color = Color(0xFFFFFFFF),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            color = Color(0xFFEE0000),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                )
                            }

                            else -> {
                                Text(
                                    text = order.status,
                                    fontSize = 14.sp,
                                    color = Color(0xFF757575),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                AsyncImage(
                    model = order.product?.anh_sp ?: "",
                    contentDescription = "Ảnh sản phẩm",
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Gray, RoundedCornerShape(8.dp)),
                    alignment = Alignment.Center
                )
            }

            if (order.status == "Đang giao hàng") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (!isCancelling) {
                                isCancelling = true
                                onReceivedClick()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FFCC),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp),
                        enabled = !isCancelling
                    ) {
                        Text(
                            text = "Đã nhận hàng",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (order.status == "Đang xử lí" || order.status == "Đã xác nhận") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (!isCancelling) {
                                isCancelling = true
                                onReceivedClick()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEE0000),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp),
                        enabled = !isCancelling
                    ) {
                        Text(
                            text = "Hủy đơn hàng",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (order.status == "Giao thành công") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val orderJson = Gson().toJson(order)
                            navController.navigate("rating/$orderJson/$uid")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9900),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                    ) {
                        Text(
                            text = "Đánh giá",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Reset isCancelling khi trạng thái đơn hàng thay đổi
    LaunchedEffect(order.status) {
        if (order.status != "Đang xử lí" && order.status != "Đã xác nhận") {
            isCancelling = false
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