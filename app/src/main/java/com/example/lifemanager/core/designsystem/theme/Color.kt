package com.example.lifemanager.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ========================================
// Coolors 配色主題
// https://coolors.co/404e4d-63595c-646881-62bec1-5ad2f4
// ========================================

// 主色調 - 青綠色系（來自 Coolors）
val Teal = Color(0xFF62BEC1)              // 主要強調色
val TealDark = Color(0xFF4FA8AB)          // 按下狀態
val TealLight = Color(0xFF7BCBCD)         // 懸停/次要

// 亮青藍 - 高亮/互動元素
val SkyBlue = Color(0xFF5AD2F4)           // 亮點強調
val SkyBlueDark = Color(0xFF3EC4E8)       // 按下狀態
val SkyBlueLight = Color(0xFF7DDDFA)      // 淺色版本

// 深色背景色系（來自 Coolors）
val DarkTealGray = Color(0xFF404E4D)      // 最深背景
val BrownGray = Color(0xFF63595C)         // 次要背景/邊框
val GrayPurple = Color(0xFF646881)        // 卡片/表面背景
val GrayPurpleLight = Color(0xFF7A7D99)   // 浮起元素背景

// 文字色系
val LightText = Color(0xFFF0F4F8)         // 主要文字（深色模式）
val LightTextSecondary = Color(0xFFB8C4CE) // 次要文字
val LightTextMuted = Color(0xFF8A9AA8)    // 禁用/提示文字
val BorderColor = Color(0xFF5A6672)       // 邊框/分隔線

// 淺色模式用色
val LightBackground = Color(0xFFF5F7FA)   // 淺色背景
val LightSurface = Color(0xFFFFFFFF)      // 淺色卡片
val LightSurfaceVariant = Color(0xFFE8ECF0) // 淺色次要表面
val DarkText = Color(0xFF2D3748)          // 深色文字
val DarkTextSecondary = Color(0xFF5A6672) // 深色次要文字

// 狀態色（調和主題）
val SuccessGreen = Color(0xFF62BEC1)      // 成功/完成（使用 Teal）
val WarningAmber = Color(0xFFE9B949)      // 警告（柔和的琥珀）
val ErrorRed = Color(0xFFD96666)          // 錯誤/放棄（柔和的紅）
val InfoBlue = Color(0xFF5AD2F4)          // 資訊（使用 SkyBlue）

// 任務預設顏色選項（與 Coolors 主題協調）
val TaskColorOptions = listOf(
    Color(0xFF62BEC1),  // 青綠（主題色）
    Color(0xFF5AD2F4),  // 亮青藍（主題色）
    Color(0xFF646881),  // 灰紫藍（主題色）
    Color(0xFF7C9CBF),  // 霧藍
    Color(0xFF8B7CB5),  // 淡紫
    Color(0xFF6BA38A),  // 森綠
    Color(0xFFE9B949),  // 琥珀
    Color(0xFFD4A5A5),  // 玫瑰灰
)

