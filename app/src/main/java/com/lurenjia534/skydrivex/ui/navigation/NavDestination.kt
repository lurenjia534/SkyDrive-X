package com.lurenjia534.skydrivex.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavDestination(val route: String, val icon: ImageVector, val label: String) {
    object Home : NavDestination("home", Icons.Outlined.SwapVert, "传输管理器")
    object Files : NavDestination("files", Icons.Outlined.Folder, "文件")
    object Settings : NavDestination("settings", Icons.Outlined.Settings, "设置")
    // 非底部栏页面：图片预览（带参数占位）
    object ImagePreview : NavDestination("preview/{itemId}/{name}", Icons.Outlined.Image, "预览")
    // 非底部栏页面：视频预览（带参数占位）
    object VideoPreview : NavDestination("video/{itemId}/{name}", Icons.Outlined.PlayCircle, "视频预览")
    // 非底部栏页面：音频预览（带参数占位）
    object AudioPreview : NavDestination("audio/{itemId}/{name}", Icons.Outlined.MusicNote, "音频预览")

    companion object {
        // 顺序：文件 → 传输管理器 → 设置
        val bottomNavItems = listOf(Files, Home, Settings)
    }
}
