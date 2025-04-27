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
import androidx.navigation.NavController
import com.example.shopthucung.admin.viewmodel.ProductViewModel
import com.example.shopthucung.model.Product
import com.example.shopthucung.utils.CloudinaryUtils
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen(
    productId: String,
    viewModel: ProductViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var product by remember { mutableStateOf<Product?>(null) }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stockQuantity by remember { mutableStateOf("") }
    var soldQuantity by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // Lấy thông tin sản phẩm khi màn hình được tải
    LaunchedEffect(productId) {
        val prod = viewModel.getProductFromListById(productId)
        product = prod
        prod?.let {
            name = it.ten_sp
            price = it.gia_sp.toString()
            description = it.mo_ta
            stockQuantity = it.soluong.toString()
            soldQuantity = it.so_luong_ban.toString()
            rating = it.danh_gia.toString()
            discount = it.giam_gia.toString()
            imageUrl = it.anh_sp
        }
    }

    // Chọn ảnh từ thư viện
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
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
                    text = "Chi tiết sản phẩm",
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

                // Hiển thị URL ảnh hiện tại (nếu có)
                if (imageUrl != null && imageUrl!!.isNotEmpty()) {
                    Text(
                        text = "Ảnh hiện tại: $imageUrl",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Nút chọn ảnh mới từ thư viện
                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Chọn ảnh mới từ thư viện")
                }

                // Hiển thị thông báo nếu đã chọn ảnh mới
                if (imageUri != null) {
                    Text("Ảnh mới đã được chọn", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        product?.let { prod ->
                            // Nếu đã chọn ảnh mới, tải ảnh lên Cloudinary
                            if (imageUri != null) {
                                isUploading = true
                                scope.launch {
                                    val url = CloudinaryUtils.uploadToCloudinary(imageUri!!, context)
                                    if (url != null) {
                                        // Cập nhật imageUrl để hiển thị trên giao diện
                                        imageUrl = url
                                        val updatedProduct = prod.copy(
                                            ten_sp = name,
                                            gia_sp = price.toLongOrNull() ?: 0L,
                                            mo_ta = description,
                                            anh_sp = url, // Cập nhật URL mới
                                            soluong = stockQuantity.toIntOrNull() ?: 0,
                                            so_luong_ban = soldQuantity.toIntOrNull() ?: 0,
                                            danh_gia = rating.toFloatOrNull() ?: 0f,
                                            giam_gia = discount.toIntOrNull() ?: 5
                                        )
                                        viewModel.updateProduct(updatedProduct)
                                        snackbarHostState.showSnackbar("Cập nhật sản phẩm thành công")
                                        navController.popBackStack()
                                    } else {
                                        snackbarHostState.showSnackbar("Tải ảnh lên thất bại")
                                    }
                                    isUploading = false
                                }
                            } else {
                                // Nếu không chọn ảnh mới, giữ nguyên URL cũ
                                val updatedProduct = prod.copy(
                                    ten_sp = name,
                                    gia_sp = price.toLongOrNull() ?: 0L,
                                    mo_ta = description,
                                    anh_sp = prod.anh_sp, // Giữ nguyên URL cũ
                                    soluong = stockQuantity.toIntOrNull() ?: 0,
                                    so_luong_ban = soldQuantity.toIntOrNull() ?: 0,
                                    danh_gia = rating.toFloatOrNull() ?: 0f,
                                    giam_gia = discount.toIntOrNull() ?: 5
                                )
                                viewModel.updateProduct(updatedProduct)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Cập nhật sản phẩm thành công")
                                }
                                navController.popBackStack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        Text("Đang tải ảnh...")
                    } else {
                        Text("Lưu thay đổi")
                    }
                }
            }
        }
    )
}