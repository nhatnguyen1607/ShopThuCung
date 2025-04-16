package com.example.shopthucung.user.navigation

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.shopthucung.user.view.CartScreen
import com.example.shopthucung.user.view.CheckoutScreen
import com.example.shopthucung.user.view.HomeScreen
import com.example.shopthucung.user.view.LoginScreen
import com.example.shopthucung.user.view.ProductDetailScreen
import com.example.shopthucung.user.view.RegisterScreen
import com.example.shopthucung.user.view.UserScreen
import com.example.shopthucung.user.viewmodel.CartViewModel
import com.example.shopthucung.user.viewmodel.HomeViewModel
import com.example.shopthucung.user.viewmodel.LoginViewModel
import com.example.shopthucung.user.viewmodel.OrderViewModel
import com.example.shopthucung.user.viewmodel.ProductDetailViewModel
import com.example.shopthucung.user.viewmodel.RegisterViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.example.shopthucung.model.Product
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import com.example.shopthucung.model.CartItem
import org.json.JSONArray


@SuppressLint("ContextCastToActivity")
@Composable
fun NavGraph(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val loginViewModel = LoginViewModel(firestore)
    val registerViewModel = RegisterViewModel(firestore)
    val homeViewModel = HomeViewModel(firestore)

    val storeOwner = LocalViewModelStoreOwner.current!!
    val cartViewModel = viewModel<CartViewModel>(storeOwner)
    val orderViewModel = viewModel<OrderViewModel>(storeOwner)


    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController = navController, viewModel = loginViewModel)
        }
        composable("register") {
            RegisterScreen(navController = navController, viewModel = registerViewModel)
        }
        composable(
            route = "home",
            deepLinks = listOf(
                navDeepLink { uriPattern = "android-app://android.navigation/HomeScreen" }
            )
        ) {
            HomeScreen(navController = navController, viewModel = homeViewModel)
        }
        composable("user/{uid}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            UserScreen(navController = navController, uid = uid)
        }
        composable("productDetail/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")?.toIntOrNull() ?: 0
            ProductDetailScreen(
                navController = navController,
                productId = productId,
                viewModel = cartViewModel,
                orderViewModel = orderViewModel,
                productDetailViewModel = viewModel<ProductDetailViewModel>()
            )
        }
        composable(route = "cart") {
            CartScreen(
                navController = navController,
                cartViewModel = cartViewModel
            )
        }


                composable(
                    route = "checkout?product={product}&quantity={quantity}&cartItems={cartItems}",
                    arguments = listOf(
                        navArgument("product") { type = NavType.StringType; defaultValue = "" },
                        navArgument("quantity") { type = NavType.IntType; defaultValue = 1 },
                        navArgument("cartItems") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val productJson = backStackEntry.arguments?.getString("product") ?: ""
                    val quantity = backStackEntry.arguments?.getInt("quantity") ?: 1
                    val cartItemsJson = backStackEntry.arguments?.getString("cartItems") ?: ""

                    val product = try {
                        if (productJson.isNotEmpty()) {
                            val jsonObject = JSONObject(URLDecoder.decode(productJson, StandardCharsets.UTF_8.toString()))
                            Product(
                                id_sanpham = jsonObject.getInt("id_sanpham"),
                                ten_sp = jsonObject.getString("ten_sp"),
                                gia_sp = jsonObject.getLong("gia_sp"),
                                giam_gia = jsonObject.getInt("giam_gia"),
                                anh_sp = jsonObject.getString("anh_sp"),
                                mo_ta = jsonObject.getString("mo_ta"),
                                soluong = jsonObject.getInt("soluong"),
                                so_luong_ban = jsonObject.getInt("so_luong_ban"),
                                danh_gia = jsonObject.getDouble("danh_gia").toFloat()
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("CheckoutScreen", "Lỗi giải mã product: ${e.message}")
                        null
                    }

                    val cartItems = try {
                        if (cartItemsJson.isNotEmpty()) {
                            val jsonArray = JSONArray(URLDecoder.decode(cartItemsJson, StandardCharsets.UTF_8.toString()))
                            (0 until jsonArray.length()).map { index ->
                                val jsonObject = jsonArray.getJSONObject(index)
                                val productJson = jsonObject.getJSONObject("product")
                                CartItem(
                                    product = Product(
                                        id_sanpham = productJson.getInt("id_sanpham"),
                                        ten_sp = productJson.getString("ten_sp"),
                                        gia_sp = productJson.getLong("gia_sp"),
                                        giam_gia = productJson.getInt("giam_gia"),
                                        anh_sp = productJson.getString("anh_sp"),
                                        mo_ta = productJson.getString("mo_ta"),
                                        soluong = productJson.getInt("soluong"),
                                        so_luong_ban = productJson.getInt("so_luong_ban"),
                                        danh_gia = productJson.getDouble("danh_gia").toFloat()
                                    ),
                                    quantity = jsonObject.getInt("quantity"),
                                    cartIndex = jsonObject.getInt("cartIndex")
                                )
                            }
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("CheckoutScreen", "Lỗi giải mã cartItems: ${e.message}")
                        null
                    }

                    CheckoutScreen(
                        navController = navController,
                        orderViewModel = orderViewModel,
                        cartViewModel = cartViewModel,
                        product = product,
                        quantity = quantity,
                        cartItems = cartItems
                    )
                }
    }
}
