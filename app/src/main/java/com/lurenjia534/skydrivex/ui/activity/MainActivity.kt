package com.lurenjia534.skydrivex.ui.activity

import android.content.Intent
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lurenjia534.skydrivex.ui.components.BottomNavBar
import com.lurenjia534.skydrivex.ui.navigation.NavGraph
import com.lurenjia534.skydrivex.ui.theme.SkyDriveXTheme
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import com.lurenjia534.skydrivex.data.repository.AuthConfigRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var hasPromptedLogin = false
    private var isUiInitialized = false

    @Inject lateinit var authConfigRepository: AuthConfigRepository

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
        lifecycleScope.launch {
            val hasConfig = authConfigRepository.hasConfig()
            if (!hasConfig) {
                startActivity(Intent(this@MainActivity, OobeActivity::class.java))
                finish()
                return@launch
            }
            initializeUi()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isUiInitialized) {
            viewModel.acquireTokenSilent()
        }
    }

    private fun initializeUi() {
        if (isUiInitialized) return
        isUiInitialized = true

        viewModel.acquireTokenSilent()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.isAccountInitialized,
                    viewModel.account
                ) { initialized, account -> initialized to account }
                    .collect { (initialized, account) ->
                        if (!initialized) return@collect
                        if (account == null) {
                            if (!hasPromptedLogin) {
                                hasPromptedLogin = true
                                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            }
                        } else {
                            hasPromptedLogin = false
                        }
                    }
            }
        }
        setContent {
            SkyDriveXAppContent(viewModel)
        }
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
