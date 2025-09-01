package com.lurenjia534.skydrivex.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lurenjia534.skydrivex.ui.screens.FilesScreen
import com.lurenjia534.skydrivex.ui.screens.HomeScreen
import com.lurenjia534.skydrivex.ui.screens.ProfileScreen
import com.lurenjia534.skydrivex.ui.screens.preview.ImagePreviewScreen
import com.lurenjia534.skydrivex.ui.screens.preview.VideoPreviewScreen
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = NavDestination.Files.route,
        modifier = modifier
    ) {
        composable(NavDestination.Home.route) {
            HomeScreen()
        }
        composable(NavDestination.Files.route) {
            val token by viewModel.token.collectAsState()
            FilesScreen(token = token, navController = navController)
        }
        composable(NavDestination.Profile.route) {
            val uiState by viewModel.userState.collectAsState()
            val driveState by viewModel.driveState.collectAsState()
            ProfileScreen(
                uiState = uiState,
                driveState = driveState,
                onRefresh = viewModel::retry,
            )
        }
        composable(NavDestination.Settings.route) {
            // 空白页面，点击导航时会启动SettingsActivity
            Box(modifier = Modifier.fillMaxSize())
        }
        // 图片预览页：参数 itemId 与 name（URL 编码）
        composable(NavDestination.ImagePreview.route) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            val nameEnc = backStackEntry.arguments?.getString("name")
            ImagePreviewScreen(itemId = itemId, nameEncoded = nameEnc, onBack = { navController.popBackStack() })
        }
        // 视频预览页：参数 itemId 与 name（URL 编码）
        composable(NavDestination.VideoPreview.route) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            val nameEnc = backStackEntry.arguments?.getString("name")
            VideoPreviewScreen(itemId = itemId, nameEncoded = nameEnc, onBack = { navController.popBackStack() })
        }
    }
}
