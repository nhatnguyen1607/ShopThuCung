package com.example.shopthucung.admin.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
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
import androidx.navigation.NavController
import com.example.shopthucung.admin.viewmodel.ProductViewModel
import com.example.shopthucung.model.Product
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

@Composable
fun AddProductScreen(
    viewModel: ProductViewModel,
    navController: NavController
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
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val base64String = bitmapToBase64(bitmap) // Chuyển ảnh sang Base64
            imageBase64 = base64String
            inputStream?.close()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Thêm sản phẩm mới",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
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
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Chọn ảnh sản phẩm từ thư viện")
                }

                if (imageBase64 != null) {
                    Text("Ảnh đã được chọn", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (imageBase64 == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Vui lòng chọn ảnh sản phẩm")
                            }
                        } else {
                            isUploading = true
                            uploadImageToFirebaseStorage(imageBase64!!) { url ->
                                if (url != null) {
                                    val newProduct = Product(
                                        ten_sp = name,
                                        gia_sp = price.toLongOrNull() ?: 0L,
                                        mo_ta = description,
                                        anh_sp = url, // Truyền một URL duy nhất
                                        soluong = stockQuantity.toIntOrNull() ?: 0,
                                        so_luong_ban = soldQuantity.toIntOrNull() ?: 0,
                                        danh_gia = rating.toFloatOrNull() ?: 0f,
                                        giam_gia = discount.toIntOrNull() ?: 5,
                                        id_sanpham = 0,
                                        firestoreId = ""
                                    )
                                    viewModel.addProduct(newProduct)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Thêm sản phẩm thành công")
                                    }
                                    navController.popBackStack()
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Tải ảnh lên thất bại")
                                    }
                                }
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        Text("Đang tải ảnh...")
                    } else {
                        Text("Thêm sản phẩm")
                    }
                }
            }
        }
    )
}

private fun bitmapToBase64(bitmap: Bitmap?): String? {
    return try {
        bitmap?.let {
            val byteArrayOutputStream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun uploadImageToFirebaseStorage(base64String: String, onComplete: (String?) -> Unit) {
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference
    val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

    try {
        val data = Base64.decode(base64String, Base64.DEFAULT)
        imageRef.putBytes(data)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    onComplete(uri.toString())
                }.addOnFailureListener {
                    onComplete(null)
                }
            }
            .addOnFailureListener {
                onComplete(null)
            }
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(null)
    }
}