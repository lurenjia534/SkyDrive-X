package com.lurenjia534.skydrivex.ui.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.lurenjia534.skydrivex.R
import com.lurenjia534.skydrivex.ui.theme.SkyDriveXTheme
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark = viewModel.isDarkMode.collectAsState().value
            SkyDriveXTheme(darkTheme = isDark) {
                AboutScreen(
                    onBack = { finish() },
                    activity = this
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(
    onBack: () -> Unit,
    activity: ComponentActivity
) {
    // Internal constants to avoid adding strings.xml entries
    val RELEASE_NOTES_URL = "https://github.com/lurenjia534/SkyDrive-X"
    val WEBSITE_URL = "https://example.com"
    val FEEDBACK_URL = "https://example.com/feedback"
    val LICENSE_URL = "https://example.com/license"
    val CONTACT_MAILTO = "mailto:support@example.com?subject=SkyDrive%20X%20反馈"
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("关于 & 反馈") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            // 顶部应用卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        draggedElevation = 0.dp,
                        disabledElevation = 0.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "material design OneDrive 客户端",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AssistChip(
                                onClick = {},
                                label = { Text("v1.0") },
                                colors = AssistChipDefaults.assistChipColors()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 链接列表卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        draggedElevation = 0.dp,
                        disabledElevation = 0.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        LinkItem(
                            icon = { Icon(Icons.Outlined.RssFeed, contentDescription = null) },
                            title = "Github",
                            onClick = { openUrlSafely(activity, RELEASE_NOTES_URL) }
                        )
                        LinkItem(
                            icon = { Icon(Icons.Outlined.Public, contentDescription = null) },
                            title = "官方网站",
                            onClick = { openUrlSafely(activity, WEBSITE_URL) }
                        )
                        LinkItem(
                            icon = { Icon(Icons.Outlined.Feedback, contentDescription = null) },
                            title = "反馈",
                            onClick = { openUrlSafely(activity, FEEDBACK_URL) }
                        )
                        LinkItem(
                            icon = { Icon(Icons.Outlined.Copyright, contentDescription = null) },
                            title = "许可",
                            onClick = { openUrlSafely(activity, LICENSE_URL) }
                        )
                        LinkItem(
                            icon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            title = "联系我们",
                            onClick = { openEmailSafely(activity, CONTACT_MAILTO) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit
) {
    ListItem(
        leadingContent = icon,
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.CallMade, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .then(Modifier)
            .clickable { onClick() }
    )
}

private fun openUrlSafely(activity: ComponentActivity, url: String) {
    if (url.isBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    try {
        activity.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // ignore if no browser
    }
}

private fun openEmailSafely(activity: ComponentActivity, mailto: String) {
    if (mailto.isBlank()) return
    val intent = Intent(Intent.ACTION_SENDTO).apply { data = mailto.toUri() }
    try {
        activity.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // ignore if no mail app
    }
}
