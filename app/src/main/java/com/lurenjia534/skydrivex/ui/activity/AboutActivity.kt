package com.lurenjia534.skydrivex.ui.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
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
    val scaffoldColor = if (isSystemInDarkTheme())
        MaterialTheme.colorScheme.surface
    else
        MaterialTheme.colorScheme.surfaceContainer

    Scaffold(
        containerColor = scaffoldColor,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            // 顶部大标题样式 + 返回按钮（参照系统设置）
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                }
                Text(
                    text = "关于 & 反馈",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
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
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val context = LocalContext.current
                        val appIconBitmap = remember(context) {
                            val drawable = context.packageManager.getApplicationIcon(context.packageName)
                            val size = when {
                                drawable.intrinsicWidth > 0 -> drawable.intrinsicWidth
                                drawable.intrinsicHeight > 0 -> drawable.intrinsicHeight
                                else -> 192
                            }
                            drawable
                                .toBitmap(width = size, height = size)
                                .asImageBitmap()
                        }
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
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
                            Spacer(modifier = Modifier.height(6.dp))
                            AssistChip(
                                onClick = {},
                                label = { Text("v${stringResource(id = R.string.app_version_name)}") },
                                colors = AssistChipDefaults.assistChipColors()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 独立瓷块样式的链接项
            item { LinkTile(icon = { Icon(Icons.Outlined.RssFeed, contentDescription = null) }, title = "Github") { openUrlSafely(activity, RELEASE_NOTES_URL) } }
            item { Spacer(Modifier.height(1.dp)) }
            item { LinkTile(icon = { Icon(Icons.Outlined.Public, contentDescription = null) }, title = "官方网站") { openUrlSafely(activity, WEBSITE_URL) } }
            item { Spacer(Modifier.height(1.dp)) }
            item { LinkTile(icon = { Icon(Icons.Outlined.Feedback, contentDescription = null) }, title = "反馈") { openUrlSafely(activity, FEEDBACK_URL) } }
            item { Spacer(Modifier.height(1.dp)) }
            item { LinkTile(icon = { Icon(Icons.Outlined.Copyright, contentDescription = null) }, title = "许可") { openUrlSafely(activity, LICENSE_URL) } }
            item { Spacer(Modifier.height(1.dp)) }
            item { LinkTile(icon = { Icon(Icons.Outlined.Email, contentDescription = null) }, title = "联系我们") { openEmailSafely(activity, CONTACT_MAILTO) } }
        }
    }
}

 

@Composable
private fun LinkTile(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit
) {
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
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        ListItem(
            leadingContent = icon,
            headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
            trailingContent = { Icon(Icons.AutoMirrored.Outlined.CallMade, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        )
    }
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
