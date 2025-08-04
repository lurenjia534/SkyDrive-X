package com.lurenjia534.skydrivex.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavDestination(val route: String, val icon: ImageVector, val label: String) {
    object Home : NavDestination("home", Icons.Default.Home, "首页")
    object Files : NavDestination("files", Icons.Outlined.Folder, "文件")
    object Profile : NavDestination("profile", Icons.Default.Person, "个人")
    object Settings : NavDestination("settings", Icons.Default.Settings, "设置")

    companion object {
        val bottomNavItems = listOf(Home, Files, Profile, Settings)
    }
}
