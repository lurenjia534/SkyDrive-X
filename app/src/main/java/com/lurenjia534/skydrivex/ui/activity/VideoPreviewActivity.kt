package com.lurenjia534.skydrivex.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import com.lurenjia534.skydrivex.ui.screens.preview.VideoPreviewScreen
import com.lurenjia534.skydrivex.ui.theme.SkyDriveXTheme
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel

@AndroidEntryPoint
class VideoPreviewActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        val nameEncoded = intent.getStringExtra(EXTRA_NAME)

        setContent {
            val isDark by mainViewModel.isDarkMode.collectAsState(initial = false)
            SkyDriveXTheme(darkTheme = isDark) {
                VideoPreviewScreen(
                    itemId = itemId,
                    nameEncoded = nameEncoded,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_NAME = "extra_name"
    }
}

