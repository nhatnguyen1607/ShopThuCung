package com.example.shopthucung.user.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.shopthucung.model.Order
import com.google.gson.Gson
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(navController: NavController, orderJson: String) {
    val decodedOrderJson = URLDecoder.decode(orderJson, "UTF-8")
    val order = Gson().fromJson(decodedOrderJson, Order::class.java)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val formattedBookingDate = order.bookingDate?.toDate()?.let { dateFormat.format(it) } ?: "Chưa xác định"
    val formattedDeliveryDate = order.deliveryDate?.toDate()?.let { dateFormat.format(it) } ?: "Chưa xác định"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chi tiết đơn hàng",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF212121)
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                },
                modifier = Modifier.shadow(4.dp)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hình ảnh sản phẩm
            AsyncImage(
                model = order.product?.anh_sp?.firstOrNull() ?: "",
                contentDescription = "Ảnh sản phẩm",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                error = painterResource(id = android.R.drawable.ic_menu_report_image)
            )

            // Card thông tin đơn hàng
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Thông tin đơn hàng",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    // Mã đơn hàng
                    InfoRow(label = "Mã đơn hàng:", value = order.orderId, valueFontSize = 12.sp)

                    // Sản phẩm
                    InfoRow(
                        label = "Sản phẩm:",
                        value = order.product?.ten_sp ?: "Không có thông tin"
                    )

                    // Số lượng
                    InfoRow(label = "Số lượng:", value = order.quantity.toString())

                    // Phương thức thanh toán
                    InfoRow(label = "Phương thức thanh toán:", value = order.paymentMethod)

                    // Trạng thái
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trạng thái:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF212121)
                        )
                        // Hiển thị trạng thái dưới dạng badge
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when (order.status) {
                                "Đang xử lí" -> Color(0xFFFFA726).copy(alpha = 0.1f)
                                "Đã xác nhận" -> Color(0xFF03A9F4).copy(alpha = 0.1f)
                                "Đang giao hàng" -> Color(0xFF00FFCC).copy(alpha = 0.1f)
                                "Giao thành công" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                "Đã hủy" -> Color(0xFFF44336).copy(alpha = 0.1f)
                                else -> Color(0xFF757575).copy(alpha = 0.1f)
                            }
                        ) {
                            Text(
                                text = order.status,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (order.status) {
                                    "Đang xử lí" -> Color(0xFFFFA726)
                                    "Đã xác nhận", "Đang giao hàng" -> Color(0xFF03A9F4)
                                    "Giao thành công" -> Color(0xFF4CAF50)
                                    "Đã hủy" -> Color(0xFFF44336)
                                    else -> Color(0xFF757575)
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Thời gian đặt hàng
                    InfoRow(label = "Thời gian đặt hàng:", value = formattedBookingDate)

                    // Thời gian nhận hàng
                    InfoRow(label = "Thời gian nhận hàng:", value = formattedDeliveryDate)
                }
            }

            // Tổng tiền
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tổng tiền:",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121)
                )
                Text(
                    text = "${order.totalPrice} VNĐ",
                    fontSize = 20.sp,
                    color = Color(0xFFE91E63),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Hàm tiện ích để hiển thị thông tin dạng hàng
@Composable
fun InfoRow(label: String, value: String, valueFontSize: TextUnit = 14.sp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF212121)
        )
        Text(
            text = value,
            fontSize = valueFontSize,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF616161),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
    }
}