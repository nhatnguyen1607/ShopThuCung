package com.example.shopthucung.admin.view

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shopthucung.model.Product
import com.example.shopthucung.utils.CloudinaryUtils
import kotlinx.coroutines.launch

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onAdd: (Product) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stockQuantity by remember { mutableStateOf("") }
    var soldQuantity by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("5") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(text = "Thêm sản phẩm mới", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                content = { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Tên sản phẩm") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Giá (VNĐ)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Mô tả") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = stockQuantity,
                            onValueChange = { stockQuantity = it },
                            label = { Text("Số lượng tồn kho") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = soldQuantity,
                            onValueChange = { soldQuantity = it },
                            label = { Text("Số lượng đã bán") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = rating,
                            onValueChange = { rating = it },
                            label = { Text("Đánh giá (0-5)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = discount,
                            onValueChange = { discount = it },
                            label = { Text("Giảm giá (%)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { pickImageLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Chọn ảnh sản phẩm từ thư viện")
                        }

                        if (imageUri != null) {
                            Text("Ảnh đã được chọn", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (imageUri == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Vui lòng chọn ảnh sản phẩm")
                        }
                    } else {
                        isUploading = true // Fixed typo: was "is unsigned copy"
                        scope.launch {
                            val url = CloudinaryUtils.uploadToCloudinary(imageUri!!, context)
                            if (url != null) {
                                val newProduct = Product(
                                    ten_sp = name,
                                    gia_sp = price.toLongOrNull() ?: 0L,
                                    mo_ta = description,
                                    anh_sp = url,
                                    soluong = stockQuantity.toIntOrNull() ?: 0,
                                    so_luong_ban = soldQuantity.toIntOrNull() ?: 0,
                                    danh_gia = rating.toFloatOrNull() ?: 0f,
                                    giam_gia = discount.toIntOrNull() ?: 5,
                                    id_sanpham = 0,
                                    firestoreId = ""
                                )
                                onAdd(newProduct)
                                snackbarHostState.showSnackbar("Thêm sản phẩm thành công")
                                onDismiss()
                            } else {
                                snackbarHostState.showSnackbar("Tải ảnh lên thất bại")
                            }
                            isUploading = false
                        }
                    }
                },
                enabled = !isUploading
            ) {
                Text(if (isUploading) "Đang tải ảnh..." else "Thêm sản phẩm")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Hủy")
            }
        }
    )
}