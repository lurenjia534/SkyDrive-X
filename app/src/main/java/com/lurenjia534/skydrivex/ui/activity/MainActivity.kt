package com.lurenjia534.skydrivex.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lurenjia534.skydrivex.ui.components.BottomNavBar
import com.lurenjia534.skydrivex.ui.navigation.NavGraph
import com.lurenjia534.skydrivex.ui.theme.SkyDriveXTheme
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                /* lightScrim = */ Color.Transparent.toArgb(),   // 浅色图标时铺的颜色
                /* darkScrim  = */ Color.Transparent.toArgb()    // 深色图标时铺的颜色
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.Transparent.toArgb(),
                darkScrim  = Color.Transparent.toArgb()
            )
        )
        super.onCreate(savedInstanceState)
        viewModel.acquireTokenSilent()
        setContent {
            SkyDriveXAppContent(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.acquireTokenSilent()
    }
}

@Composable
fun SkyDriveXAppContent(viewModel: MainViewModel = hiltViewModel()) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    SkyDriveXTheme(darkTheme = isDarkMode) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        // 仅在底栏页面显示 BottomNavBar；预览页为全屏（无底栏）
        val showBottomBar = when {
            currentRoute == null -> true
            currentRoute.startsWith("preview/") -> false
            currentRoute.startsWith("video/") -> false
            else -> true
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = { if (showBottomBar) BottomNavBar(navController = navController) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                NavGraph(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SkyDriveXAppPreview() {
    SkyDriveXTheme {
        SkyDriveXAppContent()
    }
}
