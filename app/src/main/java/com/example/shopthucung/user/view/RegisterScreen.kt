package com.example.shopthucung.user.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.shopthucung.R
import com.example.shopthucung.user.viewmodel.RegisterViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, viewModel: RegisterViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Theo dõi trạng thái gửi email xác minh và đăng ký hoàn tất
    val verificationEmailSent by viewModel.verificationEmailSent.collectAsState()
    val registrationComplete by viewModel.registrationComplete.collectAsState()

    // Tự động chuyển hướng khi đăng ký hoàn tất
    LaunchedEffect(registrationComplete) {
        if (registrationComplete) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Đăng ký thành công! Đang chuyển hướng...")
                navController.navigate("login") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.pet_background),
                contentDescription = "Ảnh nền thú cưng",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Đăng ký",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A90E2),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (!verificationEmailSent) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Xác nhận mật khẩu") },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                } else if (!registrationComplete) {
                    Text(
                        text = "Email xác minh đã được gửi! Vui lòng kiểm tra hộp thư (bao gồm thư rác) và nhấp vào liên kết để kích hoạt tài khoản.",
                        fontSize = 16.sp,
                        color = Color.Black,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    Text(
                        text = "Đăng ký thành công! Đang chuyển hướng đến màn hình đăng nhập...",
                        fontSize = 16.sp,
                        color = Color.Black,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                if (!registrationComplete) {
                    Button(
                        onClick = {
                            if (isLoading) return@Button

                            if (verificationEmailSent) {
                                // Kiểm tra trạng thái xác minh email
                                isLoading = true
                                viewModel.checkEmailVerification(email, password) { success, message ->
                                    isLoading = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(message ?: if (success) "Đăng ký thành công!" else "Xác minh thất bại!")
                                    }
                                }
                            } else {
                                if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Vui lòng nhập đầy đủ thông tin!")
                                    }
                                    return@Button
                                }
                                if (password != confirmPassword) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Mật khẩu không khớp!")
                                    }
                                    return@Button
                                }
                                isLoading = true
                                viewModel.registerAndSendVerificationEmail(email, password) { success, message ->
                                    isLoading = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(message ?: if (success) "Đã gửi email xác minh!" else "Đăng ký thất bại!")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                        enabled = !isLoading // Chỉ vô hiệu hóa khi đang xử lý
                    ) {
                        Text(
                            if (isLoading) "Đang xử lý..."
                            else if (verificationEmailSent) "Kiểm tra xác minh"
                            else "Đăng ký",
                            fontSize = 16.sp
                        )
                    }
                }

                if (!verificationEmailSent) {
                    TextButton(onClick = { if (!isLoading) navController.navigate("login") }) {
                        Text("Đã có tài khoản? Đăng nhập")
                    }
                }
            }
        }
    }
}