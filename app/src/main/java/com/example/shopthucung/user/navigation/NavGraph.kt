package com.example.shopthucung.user.navigation

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.shopthucung.user.view.*
import com.example.shopthucung.user.viewmodel.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.shopthucung.model.Product
import com.example.shopthucung.model.CartItem
import org.json.JSONObject
import org.json.JSONArray
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val loginViewModel = LoginViewModel(firestore)

    val activity = LocalActivity.current
    val registerViewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModelFactory(firestore, activity)
    )

    val homeViewModel = HomeViewModel(firestore)
    val storeOwner = LocalViewModelStoreOwner.current!!
    val cartViewModel = viewModel<CartViewModel>(storeOwner)
    val orderViewModel = viewModel<OrderViewModel>(storeOwner)

    NavHost(
        navController = navController,
        startDestination = if (auth.currentUser != null) "home" else "login"
    ) {
        composable("login") {
            auth.signOut()
            loginViewModel.clearMessage()
            LoginScreen(navController = navController, viewModel = loginViewModel)
        }
        composable("register") {
            RegisterScreen(navController = navController, viewModel = registerViewModel)
        }
        composable("verification") {
            VerificationScreen(navController = navController, viewModel = registerViewModel)
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
                    val jsonObject = JSONObject(
                        URLDecoder.decode(
                            productJson,
                            StandardCharsets.UTF_8.toString()
                        )
                    )
                    Product(
                        id_sanpham = jsonObject.getInt("id_sanpham"),
                        ten_sp = jsonObject.getString("ten_sp"),
                        gia_sp = jsonObject.getLong("gia_sp"),
                        giam_gia = jsonObject.getInt("giam_gia"),
                        anh_sp = jsonObject.getJSONArray("anh_sp").let { jsonArray ->
                            (0 until jsonArray.length()).map { jsonArray.getString(it) }
                        },
                        mo_ta = jsonObject.getString("mo_ta"),
                        soluong = jsonObject.getInt("soluong"),
                        so_luong_ban = jsonObject.getInt("so_luong_ban"),
                        danh_gia = jsonObject.getDouble("danh_gia").toFloat(),
                        firestoreId = jsonObject.optString("firestoreId", "")
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
                    val jsonArray = JSONArray(
                        URLDecoder.decode(
                            cartItemsJson,
                            StandardCharsets.UTF_8.toString()
                        )
                    )
                    (0 until jsonArray.length()).map { index ->
                        val jsonObject = jsonArray.getJSONObject(index)
                        val productJson = jsonObject.getJSONObject("product")
                        CartItem(
                            userId = jsonObject.optString("userId", ""),
                            productId = jsonObject.optInt("productId", 0),
                            product = Product(
                                id_sanpham = productJson.getInt("id_sanpham"),
                                ten_sp = productJson.getString("ten_sp"),
                                gia_sp = productJson.getLong("gia_sp"),
                                giam_gia = productJson.getInt("giam_gia"),
                                anh_sp = productJson.getJSONArray("anh_sp").let { jsonArray ->
                                    (0 until jsonArray.length()).map { jsonArray.getString(it) }
                                },
                                mo_ta = productJson.getString("mo_ta"),
                                soluong = productJson.getInt("soluong"),
                                so_luong_ban = productJson.getInt("so_luong_ban"),
                                danh_gia = productJson.getDouble("danh_gia").toFloat(),
                                firestoreId = productJson.optString("firestoreId", "")
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
        composable(
            route = "order_detail/{orderJson}",
            arguments = listOf(navArgument("orderJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderJson = backStackEntry.arguments?.getString("orderJson") ?: ""
            OrderDetailScreen(navController = navController, orderJson = orderJson)
        }
        composable("rating/{orderJson}/{uid}") { backStackEntry ->
            val orderJson = backStackEntry.arguments?.getString("orderJson") ?: ""
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            RatingScreen(navController = navController, orderJson = orderJson, uid = uid)
        }
    }
}
