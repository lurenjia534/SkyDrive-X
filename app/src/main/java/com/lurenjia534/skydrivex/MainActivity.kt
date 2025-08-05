package com.lurenjia534.skydrivex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.lurenjia534.skydrivex.ui.components.BottomNavBar
import com.lurenjia534.skydrivex.ui.navigation.NavGraph
import com.lurenjia534.skydrivex.ui.theme.SkyDriveXTheme
import com.lurenjia534.skydrivex.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.acquireTokenSilent()
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
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = { BottomNavBar(navController = navController) }
        ) { innerPadding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {
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
