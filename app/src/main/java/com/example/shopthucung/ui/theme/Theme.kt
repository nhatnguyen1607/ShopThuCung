package com.example.shopthucung.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Typography

// Định nghĩa ColorScheme cho chế độ sáng
private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF6200EE), // Màu chính
    secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6), // Màu phụ
    background = androidx.compose.ui.graphics.Color(0xFFFFFFFF), // Nền
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF), // Bề mặt
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF), // Chữ trên màu chính
    onSecondary = androidx.compose.ui.graphics.Color(0xFF000000), // Chữ trên màu phụ
    onBackground = androidx.compose.ui.graphics.Color(0xFF000000), // Chữ trên nền
    onSurface = androidx.compose.ui.graphics.Color(0xFF000000) // Chữ trên bề mặt
)

// Định nghĩa ColorScheme cho chế độ tối
private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFBB86FC), // Màu chính
    secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6), // Màu phụ
    background = androidx.compose.ui.graphics.Color(0xFF121212), // Nền
    surface = androidx.compose.ui.graphics.Color(0xFF121212), // Bề mặt
    onPrimary = androidx.compose.ui.graphics.Color(0xFF000000), // Chữ trên màu chính
    onSecondary = androidx.compose.ui.graphics.Color(0xFF000000), // Chữ trên màu phụ
    onBackground = androidx.compose.ui.graphics.Color(0xFFFFFFFF), // Chữ trên nền
    onSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF) // Chữ trên bề mặt
)

@Composable
fun ShopThuCungTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}