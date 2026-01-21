package com.example.lifemanager.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ========================================
// 深色主題 - Coolors Palette
// https://coolors.co/404e4d-63595c-646881-62bec1-5ad2f4
// ========================================
private val DarkColorScheme = darkColorScheme(
    // 主色調 - 青綠
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealDark,
    onPrimaryContainer = LightText,
    
    // 次要色調 - 亮青藍
    secondary = SkyBlue,
    onSecondary = DarkTealGray,
    secondaryContainer = GrayPurple,
    onSecondaryContainer = SkyBlueLight,
    
    // 第三色調 - 灰紫藍
    tertiary = GrayPurpleLight,
    onTertiary = Color.White,
    tertiaryContainer = BrownGray,
    onTertiaryContainer = LightText,
    
    // 背景 - 深青灰
    background = DarkTealGray,
    onBackground = LightText,
    
    // 表面 - 灰紫藍
    surface = GrayPurple,
    onSurface = LightText,
    surfaceVariant = BrownGray,
    onSurfaceVariant = LightTextSecondary,
    
    // 輪廓/邊框
    outline = BorderColor,
    outlineVariant = LightTextMuted,
    
    // 錯誤
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF8B3D3D),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // 其他
    inverseSurface = LightSurface,
    inverseOnSurface = DarkText,
    inversePrimary = TealDark,
    scrim = Color.Black
)

// ========================================
// 淺色主題 - Coolors Palette Light
// ========================================
private val LightColorScheme = lightColorScheme(
    // 主色調 - 青綠
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F1F2),
    onPrimaryContainer = TealDark,
    
    // 次要色調 - 亮青藍
    secondary = SkyBlueDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F4FC),
    onSecondaryContainer = Color(0xFF2A7A8C),
    
    // 第三色調 - 灰紫藍
    tertiary = GrayPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE4E5ED),
    onTertiaryContainer = Color(0xFF4A4D66),
    
    // 背景
    background = LightBackground,
    onBackground = DarkText,
    
    // 表面
    surface = LightSurface,
    onSurface = DarkText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    
    // 輪廓/邊框
    outline = Color(0xFF8A9AA8),
    outlineVariant = Color(0xFFCED6DE),
    
    // 錯誤
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF8B3D3D),
    
    // 其他
    inverseSurface = GrayPurple,
    inverseOnSurface = LightText,
    inversePrimary = TealLight,
    scrim = Color.Black
)

@Composable
fun LifeManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 關閉動態顏色以使用 Coolors 主題
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

