package com.lurenjia534.skydrivex.ui.screens

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import com.lurenjia534.skydrivex.ui.components.CopyItemSheet
import com.lurenjia534.skydrivex.ui.components.DeleteConfirmDialog
import com.lurenjia534.skydrivex.ui.components.MoveItemSheet
import com.lurenjia534.skydrivex.ui.components.FabActionSheet
import com.lurenjia534.skydrivex.ui.components.RenameItemDialog
import com.lurenjia534.skydrivex.ui.components.ShareLinkDialog
import com.lurenjia534.skydrivex.ui.notification.DownloadRegistry
import com.lurenjia534.skydrivex.ui.notification.createDownloadChannel
import com.lurenjia534.skydrivex.ui.notification.replaceWithCompletion
import com.lurenjia534.skydrivex.ui.notification.showOrUpdateProgress
import com.lurenjia534.skydrivex.ui.notification.startSystemDownloadWithNotification
import com.lurenjia534.skydrivex.ui.service.TransferService
import com.lurenjia534.skydrivex.ui.state.Breadcrumb
import com.lurenjia534.skydrivex.ui.util.saveToTree
import com.lurenjia534.skydrivex.ui.viewmodel.FilesViewModel
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Screen that displays files and folders from the user's drive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    token: String?,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: FilesViewModel = hiltViewModel<FilesViewModel>(),
    mainViewModel: MainViewModel = hiltViewModel<MainViewModel>(),
) {
    val uiState by viewModel.filesState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadPref by mainViewModel.downloadPreference.collectAsState()
    val driveState by mainViewModel.driveState.collectAsState()
    // 移除与 TopAppBar 的嵌套滚动耦合，避免顶部高度计算异常
    val clipboard = LocalClipboard.current
    var shareTarget by remember { mutableStateOf<Pair<String, String?>?>(null) } // itemId to name
    var showShareDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Triple<String, String?, Boolean>?>(null) } // id, name, isFolder
    var moveTarget by remember { mutableStateOf<Pair<String, String?>?>(null) } // id, currentName
    var showMoveSheet by remember { mutableStateOf(false) }

    // Listen for upload completion broadcast to refresh current folder
    DisposableEffect(token) {
        if (token == null) return@DisposableEffect onDispose { }
        val tokenSnapshot = token
        val filter = IntentFilter(TransferService.ACTION_UPLOAD_COMPLETED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                viewModel.refreshCurrent(tokenSnapshot)
            }
        }
        try {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (_: Exception) {}
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    // System photo picker to select images for upload
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val count = uris.size
        scope.launch { snackbarHostState.showSnackbar(if (count > 0) "已选择 $count 项，开始上传" else "未选择内容") }
        if (token != null && uris.isNotEmpty()) {
            val parentId = viewModel.currentFolderId()
            val cr = context.contentResolver
            for (uri in uris) {
                val name = runCatching {
                    var displayName: String? = null
                    cr.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val idx = c.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                            if (idx >= 0) displayName = c.getString(idx)
                        }
                    }
                    displayName ?: ("IMG_" + System.currentTimeMillis() + ".jpg")
                }.getOrDefault("IMG_" + System.currentTimeMillis() + ".jpg")
                val mime = cr.getType(uri) ?: "image/jpeg"
                startUploadSmallService(context, token, parentId, uri, name, mime)
            }
        }
    }

    // System document picker to select files for upload (multiple)
    val pickFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val count = uris.size
        if (token != null && uris.isNotEmpty()) {
            val cr = context.contentResolver
            val parentId = viewModel.currentFolderId()
            for (uri in uris) {
                val name = runCatching {
                    var displayName: String? = null
                    cr.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val idx = c.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                            if (idx >= 0) displayName = c.getString(idx)
                        }
                    }
                    displayName ?: ("FILE_" + System.currentTimeMillis())
                }.getOrDefault("FILE_" + System.currentTimeMillis())
                val mime = cr.getType(uri) ?: "application/octet-stream"
                val size = runCatching {
                    cr.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.SIZE), null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val idx = c.getColumnIndex(android.provider.MediaStore.MediaColumns.SIZE)
                            if (idx >= 0) c.getLong(idx) else null
                        } else null
                    }
                }.getOrNull() ?: -1L
                val threshold = 10L * 1024L * 1024L // 10 MiB
                if (size >= 0 && size > threshold) {
                    startUploadLargeService(context, token, parentId, uri, name, size)
                } else {
                    startUploadSmallService(context, token, parentId, uri, name, mime)
                }
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar(if (count > 0) "未登录" else "未选择内容") }
        }
    }

    // Bottom sheet for actions
    var showSheet by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showCopySheet by remember { mutableStateOf(false) }
    var copyTarget by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Pair<String, String?>?>(null) }

    LaunchedEffect(key1 = token) {
        token?.let { viewModel.loadRoot(it) }
    }

    LaunchedEffect(uiState.items, uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
        } ?: uiState.items?.let {
            snackbarHostState.showSnackbar("加载成功")
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {},
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSheet = true }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "添加")
            }
        }
    ) { padding ->
        val contentModifier = modifier.fillMaxSize().padding(padding)
        val listState = rememberLazyListState()
        val hideSearch by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
        val filteredItems = uiState.items.orEmpty().let { list ->
            if (searchQuery.isBlank()) list
            else list.filter { (it.name ?: "").contains(searchQuery, ignoreCase = true) }
        }
        // 统一用 Column 包裹，顶部放搜索/标题/面包屑，底部权重占满显示列表
        Column(modifier = contentModifier) {
            AnimatedVisibility(
                visible = !hideSearch,
                enter =
                    slideInVertically(
                        initialOffsetY = { -it / 2 },
                        animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(150, easing = LinearOutSlowInEasing)),
                exit =
                    slideOutVertically(
                        targetOffsetY = { -it / 2 },
                        animationSpec = tween(durationMillis = 100, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(90, easing = FastOutLinearInEasing))
            ) {
                Column {
                    DockedSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { searchActive = false },
                        // 始终保持收起形态，避免显示空的“建议”面板
                        active = false,
                        onActiveChange = {},
                        placeholder = { Text("搜索文件和文件夹") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "清除")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {}

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                        if (uiState.canGoBack) {
                            IconButton(onClick = { token?.let { viewModel.goBack(it) } }, enabled = token != null) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上一级")
                            }
                        }
                    }
                }
            }

            BreadcrumbBar(
                path = uiState.path,
                onNavigate = { index -> if (token != null) viewModel.navigateTo(index, token) }
            )

            when {
                uiState.isLoading -> {
                    FilesLoadingPlaceholder(modifier = Modifier.weight(1f).fillMaxWidth())
                }

                uiState.error != null -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.error ?: "加载失败")
                    }
                }

                uiState.items == null -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "暂无文件")
                    }
                }

                filteredItems.isEmpty() -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = if (searchQuery.isNotBlank()) "无匹配结果" else "暂无文件")
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(filteredItems) { item ->
                            val isFolder = item.folder != null
                            var expanded by remember { mutableStateOf(false) }

                        Box {
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        imageVector = if (isFolder) Icons.Outlined.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile,
                                        contentDescription = null,
                                    )
                                },
                                headlineContent = { Text(text = item.name ?: "") },
                                supportingContent = {
                                    if (isFolder) {
                                        val count = item.folder.childCount ?: 0
                                        Text("$count 项")
                                    } else {
                                        val sizeText = item.size?.let { formatBytes(it) } ?: ""
                                        if (sizeText.isNotEmpty()) Text(sizeText)
                                    }
                                },
                                trailingContent = {
                                    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                                        IconButton(onClick = { expanded = true }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "更多操作"
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            offset = DpOffset(x = 0.dp, y = 0.dp) // 如需可微调
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("重命名", fontWeight = FontWeight.Bold) },
                                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                                onClick = {
                                                    val id = item.id
                                                    if (id != null) {
                                                        renameTarget = id to item.name
                                                        showRenameDialog = true
                                                    } else {
                                                        scope.launch { snackbarHostState.showSnackbar("无法重命名：缺少条目ID") }
                                                    }
                                                    expanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("复制到", fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    val id = item.id
                                                    if (id != null) {
                                                        copyTarget = id to item.name
                                                        showCopySheet = true
                                                    } else {
                                                        scope.launch { snackbarHostState.showSnackbar("无法复制：缺少条目ID") }
                                                    }
                                                    expanded = false
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("移动到", fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    val id = item.id
                                                    if (id != null) {
                                                        moveTarget = id to item.name
                                                        showMoveSheet = true
                                                    } else {
                                                        scope.launch { snackbarHostState.showSnackbar("无法移动：缺少条目ID") }
                                                    }
                                                    expanded = false
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) }
                                            )
                                            if (!isFolder) {
                                                DropdownMenuItem(
                                                    text = { Text("下载", fontWeight = FontWeight.Bold) },
                                                    onClick = {
                                                        val itemId = item.id
                                                        val fileName = item.name ?: "download"
                                                        val totalBytes = item.size
                                                        if (itemId != null && token != null) {
                                                            scope.launch {
                                                                try {
                                                                    val url = viewModel.getDownloadUrl(itemId, token)
                                                                    if (url.isNullOrEmpty()) {
                                                                        snackbarHostState.showSnackbar("无法获取下载链接")
                                                                    } else {
                                                                        if (downloadPref.mode.name == "SYSTEM_DOWNLOADS") {
                                                                            startSystemDownloadWithNotification(context, url, fileName)
                                                                            scope.launch { snackbarHostState.showSnackbar("已开始下载：$fileName") }
                                                                        } else {
                                                                            val tree = downloadPref.treeUri
                                                                            if (tree.isNullOrEmpty()) {
                                                                                snackbarHostState.showSnackbar("未选择自定义下载目录")
                                                                            } else {
                                                                                val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                                                                                createDownloadChannel(context)
                                                                                if (totalBytes == null) {
                                                                                    showOrUpdateProgress(context, notificationId, fileName, null, null, true, withCancelAction = true)
                                                                                } else {
                                                                                    showOrUpdateProgress(context, notificationId, fileName, 0, 100, false, withCancelAction = true)
                                                                                }
                                                                                val cancelFlag = java.util.concurrent.atomic.AtomicBoolean(false)
                                                                                DownloadRegistry.registerCustom(notificationId, cancelFlag)
                                                                                // immediate user feedback without blocking
                                                                                scope.launch { snackbarHostState.showSnackbar("已开始下载：$fileName") }
                                                                                val saved = withContext(Dispatchers.IO) {
                                                                                    saveToTree(
                                                                                        context,
                                                                                        tree,
                                                                                        fileName,
                                                                                        url,
                                                                                        totalBytes,
                                                                                        { downloaded, total ->
                                                                                            val max = 100
                                                                                            val progress = if (total > 0L) {
                                                                                                ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                                                                                            } else null
                                                                                            if (progress != null) {
                                                                                                showOrUpdateProgress(context, notificationId, fileName, progress, max, false, withCancelAction = true)
                                                                                            } else {
                                                                                                showOrUpdateProgress(context, notificationId, fileName, null, null, true, withCancelAction = true)
                                                                                            }
                                                                                        },
                                                                                        cancelFlag
                                                                                    )
                                                                                }
                                                                                replaceWithCompletion(context, notificationId, fileName, saved)
                                                                                if (saved) {
                                                                                    snackbarHostState.showSnackbar("已保存到自定义目录：$fileName")
                                                                                } else {
                                                                                    if (cancelFlag.get()) {
                                                                                        snackbarHostState.showSnackbar("已取消")
                                                                                    } else {
                                                                                        snackbarHostState.showSnackbar("下载失败")
                                                                                    }
                                                                                }
                                                                                DownloadRegistry.cleanup(notificationId)
                                                                            }
                                                                        }
                                                                    }
                                                                } catch (e: Exception) {
                                                                    snackbarHostState.showSnackbar(e.message ?: "下载出错")
                                                                }
                                                            }
                                                        }
                                                        expanded = false
                                                    },
                                                    leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) }
                                                )
                                                // 预览（仅图片类型）
                                                val mime = item.file?.mimeType
                                                if (mime != null && mime.startsWith("image/")) {
                                                    DropdownMenuItem(
                                                        text = { Text("预览", fontWeight = FontWeight.Bold) },
                                                        onClick = {
                                                            val id = item.id
                                                            if (id != null) {
                                                                val encodedName = java.net.URLEncoder.encode(item.name ?: "", "UTF-8")
                                                                navController.navigate("preview/${id}/${encodedName}")
                                                            } else {
                                                                scope.launch { snackbarHostState.showSnackbar("无法预览：缺少条目ID") }
                                                            }
                                                            expanded = false
                                                        },
                                                        leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) }
                                                    )
                                                }
                                                // 预览视频（仅视频文件）
                                                run {
                                                    val vmime = item.file?.mimeType
                                                    if (vmime != null && vmime.startsWith("video/")) {
                                                        DropdownMenuItem(
                                                            text = { Text("预览视频", fontWeight = FontWeight.Bold) },
                                                            onClick = {
                                                                val id = item.id
                                                                if (id != null) {
                                                                    val encodedName = java.net.URLEncoder.encode(item.name ?: "", "UTF-8")
                                                                    navController.navigate("video/${id}/${encodedName}")
                                                                } else {
                                                                    scope.launch { snackbarHostState.showSnackbar("无法预览：缺少条目ID") }
                                                                }
                                                                expanded = false
                                                            },
                                                            leadingIcon = { Icon(Icons.Outlined.PlayCircle, contentDescription = null) }
                                                        )
                                                    }
                                                }
                                                // 预览音频（仅音频文件）
                                                run {
                                                    val amime = item.file?.mimeType
                                                    if (amime != null && amime.startsWith("audio/")) {
                                                        DropdownMenuItem(
                                                            text = { Text("预览音频", fontWeight = FontWeight.Bold) },
                                                            onClick = {
                                                                val id = item.id
                                                                if (id != null) {
                                                                    val encodedName = java.net.URLEncoder.encode(item.name ?: "", "UTF-8")
                                                                    navController.navigate("audio/${id}/${encodedName}")
                                                                } else {
                                                                    scope.launch { snackbarHostState.showSnackbar("无法预览：缺少条目ID") }
                                                                }
                                                                expanded = false
                                                            },
                                                            leadingIcon = { Icon(Icons.Outlined.LibraryMusic, contentDescription = null) }
                                                        )
                                                    }
                                                }
                                            }
                                            DropdownMenuItem(
                                                text = { Text("分享", fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    val itemId = item.id
                                                    if (itemId != null) {
                                                        shareTarget = itemId to (item.name)
                                                        showShareDialog = true
                                                    } else {
                                                        scope.launch { snackbarHostState.showSnackbar("无法分享：缺少条目ID") }
                                                    }
                                                    expanded = false
                                                },
                                                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("删除", fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    val id = item.id
                                                    if (id != null) {
                                                        deleteTarget = Triple(id, item.name, isFolder)
                                                    } else {
                                                        scope.launch { snackbarHostState.showSnackbar("无法删除：缺少条目ID") }
                                                    }
                                                    expanded = false
                                                },
                                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                                colors = MenuDefaults.itemColors(
                                                    textColor = MaterialTheme.colorScheme.error,
                                                    leadingIconColor = MaterialTheme.colorScheme.error
                                                )
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.clickable(enabled = isFolder && item.id != null) {
                                    if (isFolder && item.id != null && token != null) {
                                        viewModel.loadChildren(item.id, token, item.name ?: "")
                                    }
                                },
                            )
                        }
                    }
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        val target = shareTarget
        if (target != null) {
            val isBusiness = driveState.data?.driveType?.lowercase(Locale.getDefault()) == "business"
            ShareLinkDialog(
                isBusiness = isBusiness,
                fileName = target.second,
                onDismiss = { showShareDialog = false },
                onCreate = { type, scopeSel, expirationDays, password ->
                    val itemId = target.first
                    if (token == null) {
                        scope.launch { snackbarHostState.showSnackbar("未登录，无法分享") }
                        showShareDialog = false
                        return@ShareLinkDialog
                    }
                    scope.launch {
                        try {
                            val expiration = expirationDays?.let { days ->
                                val millis = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L
                                java.time.Instant.ofEpochMilli(millis).toString() // RFC3339 UTC
                            }
                            val webUrl = viewModel.createShareLink(
                                itemId = itemId,
                                token = token,
                                type = type,
                                scope = scopeSel,
                                password = password,
                                expirationDateTime = expiration
                            )
                            if (webUrl.isNullOrEmpty()) {
                                snackbarHostState.showSnackbar("创建分享链接失败")
                            } else {
                                    val clip = ClipData.newPlainText("SkyDriveX link", webUrl).toClipEntry()
                                    clipboard.setClipEntry(clip)  // suspend
                                snackbarHostState.showSnackbar("已复制分享链接")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(e.message ?: "分享失败")
                        } finally {
                            showShareDialog = false
                        }
                    }
                }
            )
        }
    }

    // Delete confirmation dialog
    deleteTarget?.let { (targetId, targetName, isFolder) ->
        DeleteConfirmDialog(
            name = targetName,
            isFolder = isFolder,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                val t = token
                if (t == null) {
                    scope.launch { snackbarHostState.showSnackbar("未登录，无法删除") }
                    deleteTarget = null
                } else {
                    // 先关闭对话框，再异步执行删除并提示
                    deleteTarget = null
                    scope.launch {
                        try {
                            viewModel.deleteFile(targetId, t)
                            snackbarHostState.showSnackbar("已删除")
                        } catch (_: Exception) {
                            snackbarHostState.showSnackbar("删除失败")
                        }
                    }
                }
            }
        )
    }

    // Copy sheet
    if (showCopySheet && token != null && copyTarget != null) {
        CopyItemSheet(
            token = token,
            visible = true,
            initialName = copyTarget!!.second,
            onDismiss = { showCopySheet = false },
            onConfirm = { targetFolderId, newName ->
                val itemId = copyTarget!!.first
                scope.launch {
                    snackbarHostState.showSnackbar("正在复制…")
                    runCatching {
                        viewModel.copyItem(itemId = itemId, token = token, newParentId = targetFolderId, newName = newName)
                    }.onSuccess {
                        snackbarHostState.showSnackbar("复制完成")
                    }.onFailure {
                        snackbarHostState.showSnackbar(it.message ?: "复制失败")
                    }
                }
            }
        )
    }

    if (showRenameDialog && token != null && renameTarget != null) {
        RenameItemDialog(
            currentName = renameTarget!!.second,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                val id = renameTarget!!.first
                showRenameDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("正在重命名…")
                    runCatching { viewModel.renameItem(id, token, newName) }
                        .onSuccess { snackbarHostState.showSnackbar("已重命名为：$newName") }
                        .onFailure { snackbarHostState.showSnackbar(it.message ?: "重命名失败") }
                }
            }
        )
    }

    // Move bottom sheet
    val tgt = moveTarget
    if (showMoveSheet && tgt != null && token != null) {
        MoveItemSheet(
            token = token,
            visible = showMoveSheet,
            initialName = tgt.second,
            onDismiss = { showMoveSheet = false },
            onConfirm = { folderId, newName ->
                scope.launch {
                    try {
                        viewModel.moveItem(
                            itemId = tgt.first,
                            token = token,
                            newParentId = folderId,
                            newName = newName
                        )
                        snackbarHostState.showSnackbar("已移动")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(e.message ?: "移动失败")
                    } finally {
                        showMoveSheet = false
                    }
                }
            }
        )
    }

    // Modal bottom sheet content
    FabActionSheet(
        visible = showSheet,
        onDismiss = { showSheet = false },
        onUploadPhoto = {
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onUploadFiles = {
            pickFilesLauncher.launch(arrayOf("*/*"))
        },
        onNewFolder = {
            newFolderName = ""
            showNewFolderDialog = true
        }
    )

    // New folder name dialog
    if (showNewFolderDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("新建文件夹") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newFolderName, onValueChange = { newFolderName = it }, singleLine = true, label = { Text("名称") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val folderName = newFolderName.trim()
                    // 立即关闭对话框，避免等待网络完成才消失
                    showNewFolderDialog = false
                    if (folderName.isNotEmpty() && token != null) {
                        scope.launch {
                            try {
                                viewModel.createFolderInCurrent(token = token, name = folderName)
                                snackbarHostState.showSnackbar("已创建：$folderName")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(e.message ?: "创建失败")
                            }
                        }
                    }
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showNewFolderDialog = false }) { Text("取消") } }
        )
    }
}

private fun startUploadSmallService(
    context: Context,
    token: String,
    parentId: String,
    uri: android.net.Uri,
    name: String,
    mime: String
) {
    val intent = Intent(context, TransferService::class.java).apply {
        action = TransferService.ACTION_UPLOAD_SMALL
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(TransferService.EXTRA_TOKEN, token)
        putExtra(TransferService.EXTRA_PARENT_ID, parentId)
        putExtra(TransferService.EXTRA_URI, uri)
        putExtra(TransferService.EXTRA_FILE_NAME, name)
        putExtra(TransferService.EXTRA_MIME, mime)
    }
    ContextCompat.startForegroundService(context, intent)
}

private fun startUploadLargeService(
    context: Context,
    token: String,
    parentId: String,
    uri: android.net.Uri,
    name: String,
    totalBytes: Long
) {
    val intent = Intent(context, TransferService::class.java).apply {
        action = TransferService.ACTION_UPLOAD_LARGE
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(TransferService.EXTRA_TOKEN, token)
        putExtra(TransferService.EXTRA_PARENT_ID, parentId)
        putExtra(TransferService.EXTRA_URI, uri)
        putExtra(TransferService.EXTRA_FILE_NAME, name)
        putExtra(TransferService.EXTRA_TOTAL_BYTES, totalBytes)
    }
    ContextCompat.startForegroundService(context, intent)
}

private fun formatBytes(bytes: Long): String {
    val kb = 1024L
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> "%.2f GB".format(Locale.US, bytes.toDouble() / gb)
        bytes >= mb -> "%.2f MB".format(Locale.US, bytes.toDouble() / mb)
        bytes >= kb -> "%.2f KB".format(Locale.US, bytes.toDouble() / kb)
        else -> "$bytes B"
    }
}

@Composable
private fun BreadcrumbBar(
    path: List<Breadcrumb>,
    onNavigate: (index: Int) -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(scroll),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        path.forEachIndexed { index, crumb ->
            val isLast = index == path.lastIndex
            val color = if (isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = crumb.name,
                color = color,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .then(if (!isLast) Modifier.clickable { onNavigate(index) } else Modifier)
            )
            if (!isLast) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FilesLoadingPlaceholder(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(10) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标占位
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp)
                        .placeholder(
                            visible = true,
                            highlight = PlaceholderHighlight.shimmer(),
                            shape = MaterialTheme.shapes.small
                        )
                )

                Column(modifier = Modifier.weight(1f)) {
                    // 文件名占位
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(18.dp)
                            .placeholder(
                                visible = true,
                                highlight = PlaceholderHighlight.shimmer(),
                                shape = MaterialTheme.shapes.small
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 次要信息占位（如大小/时间）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(14.dp)
                            .placeholder(
                                visible = true,
                                highlight = PlaceholderHighlight.shimmer(),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }
        }
    }
}

// 文件保存工具已提取至 ui.util.FileSave.saveToTree
