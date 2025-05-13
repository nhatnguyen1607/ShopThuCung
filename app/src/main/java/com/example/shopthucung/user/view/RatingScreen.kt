package com.example.shopthucung.user.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.shopthucung.model.Order
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.net.URLDecoder
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingScreen(navController: NavController, orderJson: String, uid: String) {
    // Giải mã orderJson
    val decodedOrderJson = URLDecoder.decode(orderJson, "UTF-8")
    val order = Gson().fromJson(decodedOrderJson, Order::class.java)
    val product = order.product ?: return

    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val db = FirebaseFirestore.getInstance()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Đánh giá sản phẩm",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = order.product?.anh_sp?.firstOrNull() ?: "",
                    contentDescription = "Ảnh sản phẩm",
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Gray, RoundedCornerShape(8.dp)),
                    alignment = Alignment.Center
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = product.ten_sp ?: "Không có thông tin",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đánh giá: ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF424242)
                )
                (1..5).forEach { star ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Sao $star",
                        tint = if (star <= rating) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { rating = star }
                    )
                }
            }

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Nhận xét của bạn") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA5D6A7),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )

            Button(
                onClick = {
                    isSubmitting = true
                    val reviewData = hashMapOf(
                        "comment" to comment,
                        "idUser" to uid,
                        "id_sanpham" to product.id_sanpham,
                        "rating" to rating,
                        "timestamp" to Timestamp.now()
                    )

                    // Thêm đánh giá vào sub-collection reviews
                    db.collection("products_reviews")
                        .document(product.id_sanpham.toString())
                        .collection("reviews")
                        .add(reviewData)
                        .addOnSuccessListener {
                            // Sau khi thêm đánh giá thành công, tính trung bình rating và cập nhật danh_gia
                            db.collection("products_reviews")
                                .document(product.id_sanpham.toString())
                                .collection("reviews")
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    val reviews = snapshot.documents
                                    if (reviews.isNotEmpty()) {
                                        // Tính trung bình rating
                                        val totalRating = reviews.sumOf { it.getLong("rating")?.toInt() ?: 0 }
                                        val averageRating = totalRating.toDouble() / reviews.size

                                        // Cập nhật danh_gia trong collection product
                                        db.collection("product")
                                            .document(product.ten_sp)
                                            .update("danh_gia", averageRating.toInt())
                                            .addOnSuccessListener {
                                                isSubmitting = false
                                                navController.popBackStack()
                                            }
                                            .addOnFailureListener { e ->
                                                isSubmitting = false
                                                errorMessage = "Lỗi khi cập nhật đánh giá: ${e.message}"
                                            }
                                    } else {
                                        // Nếu không có đánh giá nào, đặt danh_gia = rating vừa thêm
                                        db.collection("product")
                                            .document(product.ten_sp)
                                            .update("danh_gia", rating)
                                            .addOnSuccessListener {
                                                isSubmitting = false
                                                navController.popBackStack()
                                            }
                                            .addOnFailureListener { e ->
                                                isSubmitting = false
                                                errorMessage = "Lỗi khi cập nhật đánh giá: ${e.message}"
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isSubmitting = false
                                    errorMessage = "Lỗi khi lấy danh sách đánh giá: ${e.message}"
                                }
                        }
                        .addOnFailureListener { e ->
                            isSubmitting = false
                            errorMessage = "Lỗi khi gửi đánh giá: ${e.message}"
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isSubmitting && rating > 0 && comment.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA5D6A7),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Gửi đánh giá", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}