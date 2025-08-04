package com.lurenjia534.skydrivex.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lurenjia534.skydrivex.ui.screens.FilesScreen
import com.lurenjia534.skydrivex.ui.screens.HomeScreen
import com.lurenjia534.skydrivex.ui.screens.ProfileScreen
import com.lurenjia534.skydrivex.viewmodel.MainViewModel

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
            val viewModel: MainViewModel = hiltViewModel()
            val uiState by viewModel.userState.collectAsState()
            ProfileScreen(uiState = uiState, onRefresh = viewModel::retry)
        }
        composable(NavDestination.Settings.route) {
            // 空白页面，点击导航时会启动SettingsActivity
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
