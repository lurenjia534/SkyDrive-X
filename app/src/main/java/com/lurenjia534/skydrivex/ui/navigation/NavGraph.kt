package com.lurenjia534.skydrivex.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lurenjia534.skydrivex.ui.screens.FilesScreen
import com.lurenjia534.skydrivex.ui.screens.HomeScreen
import com.lurenjia534.skydrivex.ui.screens.ProfileScreen
import com.lurenjia534.skydrivex.ui.screens.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavDestination.Home.route,
        modifier = modifier
    ) {
        composable(NavDestination.Home.route) {
            HomeScreen()
        }
        composable(NavDestination.Files.route) {
            FilesScreen()
        }
        composable(NavDestination.Profile.route) {
            ProfileScreen()
        }
        composable(NavDestination.Settings.route) {
            SettingsScreen()
        }
    }
}
