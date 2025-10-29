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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lurenjia534.skydrivex.auth.AuthManager
import com.lurenjia534.skydrivex.data.repository.AuthConfigRepository
import com.lurenjia534.skydrivex.ui.activity.OobeMode.INITIAL
import com.lurenjia534.skydrivex.ui.components.BottomNavBar
import com.lurenjia534.skydrivex.ui.navigation.NavGraph
import com.lurenjia534.skydrivex.ui.theme.SkyDriveXTheme
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var isUiInitialized = false
    private var skipTokenCheck: Boolean = false
    private var shouldRequestSignIn: Boolean = false

    @Inject lateinit var authConfigRepository: AuthConfigRepository
    @Inject lateinit var authManager: AuthManager

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
        skipTokenCheck = intent?.getBooleanExtra(EXTRA_SKIP_TOKEN_CHECK, false) ?: false
        shouldRequestSignIn = intent?.getBooleanExtra(EXTRA_REQUEST_SIGN_IN, false) ?: false
        lifecycleScope.launch {
            val hasConfig = authConfigRepository.hasConfig()
            if (!hasConfig) {
                startOobeAndFinish()
                return@launch
            }
            if (!skipTokenCheck) {
                val hasToken = authManager.hasCachedAccount()
                if (!hasToken) {
                    startOobeAndFinish()
                    return@launch
                }
            }
            initializeUi()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isUiInitialized) {
            viewModel.acquireTokenSilent()
            maybeTriggerSignIn()
        }
    }

    private fun initializeUi() {
        if (isUiInitialized) return
        isUiInitialized = true

        viewModel.acquireTokenSilent()
        setContent {
            SkyDriveXAppContent(viewModel)
        }
        maybeTriggerSignIn()
    }

    private fun startOobeAndFinish() {
        startActivity(
            Intent(this@MainActivity, OobeActivity::class.java).apply {
                putExtra(OobeActivity.EXTRA_MODE, INITIAL.name)
            }
        )
        finish()
    }

    private fun maybeTriggerSignIn() {
        if (!shouldRequestSignIn) return
        shouldRequestSignIn = false
        lifecycleScope.launch {
            if (authManager.awaitInitialization()) {
                viewModel.signIn(this@MainActivity)
            }
        }
    }

    companion object {
        const val EXTRA_SKIP_TOKEN_CHECK = "skip_token_check"
        const val EXTRA_REQUEST_SIGN_IN = "request_sign_in"
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
