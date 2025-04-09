package com.example.shopthucung.user.navigation

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.shopthucung.user.view.HomeScreen
import com.example.shopthucung.user.view.LoginScreen
import com.example.shopthucung.user.view.RegisterScreen
import com.example.shopthucung.user.view.UserScreen
import com.example.shopthucung.user.viewmodel.HomeViewModel
import com.example.shopthucung.user.viewmodel.LoginViewModel
import com.example.shopthucung.user.viewmodel.RegisterViewModel
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("ContextCastToActivity")
@Composable
fun NavGraph(navController: NavHostController) {
    val firestore = FirebaseFirestore.getInstance()
    val activity = LocalContext.current as Activity
    val loginViewModel = LoginViewModel(firestore, activity)
    val registerViewModel = RegisterViewModel(firestore, activity)
    val homeViewModel = HomeViewModel(firestore)
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
    }
}