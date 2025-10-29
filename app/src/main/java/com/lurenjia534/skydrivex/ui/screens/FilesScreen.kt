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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.ui.Alignment
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import com.lurenjia534.skydrivex.data.local.DownloadLocationMode
import com.lurenjia534.skydrivex.data.local.DownloadLocationPreference
import com.lurenjia534.skydrivex.data.model.driveitem.DriveItemDto
import com.lurenjia534.skydrivex.ui.activity.preview.AudioPreviewActivity
import com.lurenjia534.skydrivex.ui.activity.preview.ImagePreviewActivity
import com.lurenjia534.skydrivex.ui.activity.preview.VideoPreviewActivity
import com.lurenjia534.skydrivex.ui.components.CopyItemSheet
import com.lurenjia534.skydrivex.ui.components.DeleteConfirmDialog
import com.lurenjia534.skydrivex.ui.components.MoveItemSheet
import com.lurenjia534.skydrivex.ui.components.FabActionSheet
import com.lurenjia534.skydrivex.ui.components.RenameItemDialog
import com.lurenjia534.skydrivex.ui.components.ShareLinkDialog
import com.lurenjia534.skydrivex.ui.components.PropertiesDialog
import com.lurenjia534.skydrivex.ui.components.EmptyState
import com.lurenjia534.skydrivex.ui.notification.DownloadRegistry
import com.lurenjia534.skydrivex.ui.notification.TransferTracker
import com.lurenjia534.skydrivex.ui.notification.createDownloadChannel
import com.lurenjia534.skydrivex.ui.notification.replaceWithCompletion
import com.lurenjia534.skydrivex.ui.notification.showOrUpdateProgress
import com.lurenjia534.skydrivex.ui.notification.startSystemDownloadWithNotification
import com.lurenjia534.skydrivex.ui.service.TransferService
import com.lurenjia534.skydrivex.ui.state.Breadcrumb
import com.lurenjia534.skydrivex.ui.util.saveToTree
import com.lurenjia534.skydrivex.ui.viewmodel.FilesViewModel
import com.lurenjia534.skydrivex.ui.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Screen that displays files and folders from the user's drive.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    var propsTarget by remember { mutableStateOf<Pair<String, String?>?>(null) }
    var showPropsDialog by remember { mutableStateOf(false) }
    var propsDetails by remember { mutableStateOf<DriveItemDto?>(null) }
    var deleteTarget by remember { mutableStateOf<Triple<String, String?, Boolean>?>(null) } // id, name, isFolder
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<Pair<String, String?>?>(null) } // id, currentName
    var showMoveSheet by remember { mutableStateOf(false) }

    // Listen for upload completion broadcast to refresh current folder
    DisposableEffect(token) {
        if (token == null) return@DisposableEffect onDispose { }
        val tokenSnapshot = token
        val filter = IntentFilter(TransferService.ACTION_UPLOAD_COMPLETED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                val parentId = i?.getStringExtra(TransferService.EXTRA_PARENT_ID) ?: "root"
                viewModel.refreshFolder(tokenSnapshot, parentId)
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

    // Debounce server-side search based on query
    LaunchedEffect(searchQuery, token) {
        val t = token
        if (t.isNullOrEmpty()) {
            viewModel.clearSearch()
        } else {
            if (searchQuery.isBlank()) {
                viewModel.clearSearch()
            } else {
                kotlinx.coroutines.delay(350)
                // Default: search whole drive (includes shared items)
                viewModel.searchInDrive(t, searchQuery)
            }
        }
    }

    Scaffold(
        topBar = {},
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.selectionMode) {
                FloatingActionButton(onClick = { showSheet = true }) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "添加")
                }
            }
        }
    ) { padding ->
        val contentModifier = modifier.fillMaxSize().padding(padding)
        val listState = rememberLazyListState()
        // Pull-to-refresh (downward) using nested scroll + M3 LoadingIndicator
        val density = LocalDensity.current
        var pullOffset by remember { mutableFloatStateOf(0f) }
        var isPulling by remember { mutableStateOf(false) }
        var refreshing by remember { mutableStateOf(false) }
        val triggerPx = with(density) { 72.dp.toPx() }

        val nested = remember(listState, token) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.Drag && available.y > 0f &&
                        listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 &&
                        !refreshing
                    ) {
                        // consume downward drag at top to reveal indicator
                        val consumedY = available.y
                        pullOffset = (pullOffset + consumedY).coerceAtMost(triggerPx * 1.5f)
                        isPulling = true
                        return Offset(0f, consumedY)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (isPulling) {
                        val shouldRefresh = pullOffset >= triggerPx
                        pullOffset = 0f
                        isPulling = false
                        if (shouldRefresh && token != null) {
                            refreshing = true
                            try {
                                viewModel.refreshCurrent(token)
                            } catch (_: Exception) {
                            } finally {
                                refreshing = false
                            }
                        }
                    }
                    return Velocity.Zero
                }
            }
        }
        val hideSearch by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
        val selectionMode = uiState.selectionMode
        val selectedCount = uiState.selectedIds.size
        val selectedItems = remember(uiState.selectedIds, uiState.items, uiState.searchResults) {
            if (uiState.selectedIds.isEmpty()) emptyList<DriveItemDto>() else {
                val pool = mutableMapOf<String, DriveItemDto>()
                uiState.items.orEmpty().forEach { item ->
                    item.id?.let { pool[it] = item }
                }
                uiState.searchResults.orEmpty().forEach { item ->
                    item.id?.let { pool[it] = item }
                }
                uiState.selectedIds.mapNotNull { pool[it] }
            }
        }
        val downloadableSelection = remember(selectedItems) {
            selectedItems.filter { !it.id.isNullOrEmpty() && it.file != null }
        }
        val hasNonDownloadables = remember(selectedItems) {
            selectedItems.any { it.file == null || it.id.isNullOrEmpty() }
        }
        val itemsToShow = if (searchQuery.isBlank()) uiState.items else uiState.searchResults
        val filteredItems = itemsToShow.orEmpty()
        val isBusy = if (searchQuery.isBlank()) uiState.isLoading else uiState.isSearching
        val errorNow = if (searchQuery.isBlank()) uiState.error else uiState.searchError
        // 统一用 Column 包裹，顶部放搜索/标题/面包屑，底部权重占满显示列表
        Column(modifier = contentModifier.nestedScroll(nested)) {
            // Pull-to-refresh indicator (M3 expressive)
            AnimatedVisibility(visible = refreshing || isPulling) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(modifier = Modifier.size(36.dp))
                }
            }
            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.exitSelectionMode() }) {
                        Icon(Icons.Filled.Close, contentDescription = "退出选择")
                    }
                    Text(
                        text = "已选 $selectedCount 项",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.selectAllCurrent() }) {
                        Text("全选")
                    }
                    TextButton(onClick = { viewModel.invertSelection() }) {
                        Text("反选")
                    }
                    IconButton(
                        onClick = {
                            if (token == null) {
                                scope.launch { snackbarHostState.showSnackbar("未登录，无法下载") }
                                return@IconButton
                            }
                            if (downloadPref.mode == DownloadLocationMode.CUSTOM_TREE && downloadPref.treeUri.isNullOrEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar("未选择自定义下载目录") }
                                return@IconButton
                            }
                            val items = downloadableSelection
                            if (items.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar("所选项目无法下载") }
                                return@IconButton
                            }
                            scope.launch {
                                if (hasNonDownloadables) {
                                    launch { snackbarHostState.showSnackbar("已跳过部分不可下载项目") }
                                }
                                for (entry in items) {
                                    startDownloadForItem(
                                        context = context,
                                        viewModel = viewModel,
                                        scope = this,
                                        snackbarHostState = snackbarHostState,
                                        downloadPref = downloadPref,
                                        token = token,
                                        item = entry
                                    )
                                }
                            }
                        },
                        enabled = downloadableSelection.isNotEmpty() && token != null
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "下载所选"
                        )
                    }
                    IconButton(
                        onClick = { showBulkDeleteDialog = true },
                        enabled = selectedCount > 0
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "删除所选",
                            tint = if (selectedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
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
                            onSearch = { /* debounced search handled via LaunchedEffect */ },
                            active = false,
                            onActiveChange = { /* keep collapsed to avoid large empty panel */ },
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
                        ) { }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                            if (uiState.canGoBack) {
                                IconButton(onClick = { token?.let { viewModel.goBack(it) } }, enabled = token != null) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上一级")
                                }
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
                // 初始进入或搜索/加载中，或数据尚未到达（itemsToShow==null）时，统一显示骨架屏
                isBusy || itemsToShow == null -> {
                    FilesLoadingPlaceholder(modifier = Modifier.weight(1f).fillMaxWidth())
                }

                errorNow != null -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = errorNow)
                    }
                }

                filteredItems.isEmpty() -> {
                    if (searchQuery.isNotBlank()) {
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = "未找到与\"$searchQuery\"匹配的内容",
                            subtitle = "请尝试更换关键字或清除搜索",
                            primaryText = "清除搜索",
                            onPrimary = {
                                searchQuery = ""
                                viewModel.clearSearch()
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.Folder,
                            title = "这里空空如也",
                            subtitle = "上传文件或新建文件夹开始使用",
                            primaryText = "上传文件",
                            onPrimary = { pickFilesLauncher.launch(arrayOf("*/*")) },
                            secondaryText = "新建文件夹",
                            onSecondary = {
                                newFolderName = ""
                                showNewFolderDialog = true
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(filteredItems) { item ->
                            val itemId = item.id
                            val isFolder = item.folder != null
                            var expanded by remember { mutableStateOf(false) }
                            val selected = itemId != null && selectionMode && uiState.selectedIds.contains(itemId)
                            if (selectionMode && expanded) {
                                expanded = false
                            }

                            Box {
                                ListItem(
                                    leadingContent = {
                                    if (selectionMode && itemId != null) {
                                        Checkbox(
                                            checked = selected,
                                            onCheckedChange = { viewModel.toggleSelect(itemId) }
                                        )
                                    } else if (isFolder) {
                                        Icon(
                                            imageVector = Icons.Outlined.Folder,
                                            contentDescription = null,
                                        )
                                    } else {
                                        val thumbUrl = try {
                                            val sets = item.thumbnails
                                            val first = sets?.firstOrNull()
                                            first?.smallSquare?.url
                                                ?: first?.small?.url
                                                ?: first?.mediumSquare?.url
                                                ?: first?.medium?.url
                                                ?: first?.largeSquare?.url
                                                ?: first?.large?.url
                                        } catch (_: Exception) { null }
                                        if (thumbUrl.isNullOrBlank()) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                                contentDescription = null,
                                            )
                                        } else {
                                            // 使用 Coil 显示缩略图
                                            androidx.compose.foundation.Image(
                                                painter = coil3.compose.rememberAsyncImagePainter(thumbUrl),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(40.dp),
                                            )
                                        }
                                    }
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
                                    if (!selectionMode) {
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
                                                text = { Text("属性", fontWeight = FontWeight.Bold) },
                                                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                                                onClick = {
                                                    val id = item.id
                                                    if (id != null) {
                                                        propsTarget = id to item.name
                                                        showPropsDialog = true
                                                        // 触发加载详情
                                                        if (token != null) {
                                                            propsDetails = null
                                                            scope.launch {
                                                                try {
                                                                    propsDetails = viewModel.getItemDetails(id, token)
                                                                } catch (_: Exception) { }
                                                            }
                                                        }
                                                    } else {
                                                        scope.launch { snackbarHostState.showSnackbar("无法获取属性：缺少条目ID") }
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
                                                        if (token != null) {
                                                            scope.launch {
                                                                startDownloadForItem(
                                                                    context = context,
                                                                    viewModel = viewModel,
                                                                    scope = this,
                                                                    snackbarHostState = snackbarHostState,
                                                                    downloadPref = downloadPref,
                                                                    token = token,
                                                                    item = item
                                                                )
                                                            }
                                                        } else {
                                                            scope.launch { snackbarHostState.showSnackbar("未登录，无法下载") }
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
                                                                val intent = Intent(context, ImagePreviewActivity::class.java)
                                                                intent.putExtra(ImagePreviewActivity.EXTRA_ITEM_ID, id)
                                                                intent.putExtra(ImagePreviewActivity.EXTRA_NAME, encodedName)
                                                                context.startActivity(intent)
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
                                                                    val intent = Intent(context, VideoPreviewActivity::class.java)
                                                                    intent.putExtra(
                                                                        VideoPreviewActivity.EXTRA_ITEM_ID, id)
                                                                    intent.putExtra(
                                                                        VideoPreviewActivity.EXTRA_NAME, encodedName)
                                                                    context.startActivity(intent)
                                                                } else {
                                                                    scope.launch { snackbarHostState.showSnackbar("无法预览：缺少条目ID") }
                                                                }
                                                                expanded = false
                                                            },
                                                            leadingIcon = { Icon(Icons.Outlined.PlayCircle, contentDescription = null) }
                                                        )
                                                    }
                                                }
                                                // 预览音频（包含 flac 等，按 MIME 或扩展名判断）
                                                run {
                                                    val amime = item.file?.mimeType
                                                    val lower = (item.name ?: "").lowercase()
                                                    val isAudioByExt = lower.endsWith(".mp3") || lower.endsWith(".m4a") ||
                                                            lower.endsWith(".aac") || lower.endsWith(".flac") ||
                                                            lower.endsWith(".wav") || lower.endsWith(".ogg") ||
                                                            lower.endsWith(".opus")
                                                    if ((amime != null && amime.startsWith("audio/")) || isAudioByExt) {
                                                        DropdownMenuItem(
                                                            text = { Text("预览音频", fontWeight = FontWeight.Bold) },
                                                            onClick = {
                                                                val id = item.id
                                                                if (id != null) {
                                                                    val encodedName = java.net.URLEncoder.encode(item.name ?: "", "UTF-8")
                                                                    val intent = Intent(context, AudioPreviewActivity::class.java)
                                                                    intent.putExtra(
                                                                        AudioPreviewActivity.EXTRA_ITEM_ID, id)
                                                                    intent.putExtra(
                                                                        AudioPreviewActivity.EXTRA_NAME, encodedName)
                                                                    context.startActivity(intent)
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
                                }},
                                modifier = Modifier
                                    .combinedClickable(
                                        enabled = itemId != null,
                                        onClick = {
                                            val id = itemId ?: return@combinedClickable
                                            if (selectionMode) {
                                                viewModel.toggleSelect(id)
                                                return@combinedClickable
                                            }
                                            if (isFolder) {
                                                if (token != null) {
                                                    viewModel.loadChildren(id, token, item.name ?: "")
                                                }
                                            } else {
                                                // 非文件夹：图片/视频直接进入预览
                                                val mime = item.file?.mimeType
                                                val nameLower = (item.name ?: "").lowercase()
                                                val encodedName = java.net.URLEncoder.encode(item.name ?: "", "UTF-8")
                                                val isImageByExt = nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg") ||
                                                        nameLower.endsWith(".png") || nameLower.endsWith(".gif") ||
                                                        nameLower.endsWith(".webp") || nameLower.endsWith(".bmp") ||
                                                        nameLower.endsWith(".heic") || nameLower.endsWith(".heif")
                                                val isVideoByExt = nameLower.endsWith(".mp4") || nameLower.endsWith(".mkv") ||
                                                        nameLower.endsWith(".webm") || nameLower.endsWith(".avi") ||
                                                        nameLower.endsWith(".mov") || nameLower.endsWith(".m4v") ||
                                                        nameLower.endsWith(".3gp") || nameLower.endsWith(".3gpp")
                                                val isAudioByExt = nameLower.endsWith(".mp3") || nameLower.endsWith(".m4a") ||
                                                        nameLower.endsWith(".aac") || nameLower.endsWith(".flac") ||
                                                        nameLower.endsWith(".wav") || nameLower.endsWith(".ogg") ||
                                                        nameLower.endsWith(".opus")
                                                when {
                                                    (mime != null && mime.startsWith("image/")) || isImageByExt -> {
                                                        val intent = Intent(context, ImagePreviewActivity::class.java)
                                                        intent.putExtra(ImagePreviewActivity.EXTRA_ITEM_ID, id)
                                                        intent.putExtra(ImagePreviewActivity.EXTRA_NAME, encodedName)
                                                        context.startActivity(intent)
                                                    }
                                                    (mime != null && mime.startsWith("video/")) || isVideoByExt -> {
                                                        val intent = Intent(context, VideoPreviewActivity::class.java)
                                                        intent.putExtra(VideoPreviewActivity.EXTRA_ITEM_ID, id)
                                                        intent.putExtra(VideoPreviewActivity.EXTRA_NAME, encodedName)
                                                        context.startActivity(intent)
                                                    }
                                                    (mime != null && mime.startsWith("audio/")) || isAudioByExt -> {
                                                        val intent = Intent(context, AudioPreviewActivity::class.java)
                                                        intent.putExtra(AudioPreviewActivity.EXTRA_ITEM_ID, id)
                                                        intent.putExtra(AudioPreviewActivity.EXTRA_NAME, encodedName)
                                                        context.startActivity(intent)
                                                    }
                                                    else -> {
                                                        // MIME 缺失或非图/视频：保持现状（不做跳转）。
                                                    }
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            val id = itemId ?: return@combinedClickable
                                            if (!selectionMode) {
                                                viewModel.enterSelectionMode(id)
                                            } else {
                                                viewModel.toggleSelect(id)
                                            }
                                        }
                                    ),
                                colors = if (selected) {
                                    ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        headlineColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        supportingColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    ListItemDefaults.colors()
                                }
                            )
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

    if (showBulkDeleteDialog) {
        val selected = uiState.selectedIds.size
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("批量删除") },
            text = {
                Text(
                    if (selected > 0) "确认将 $selected 项移到回收站？" else "没有选中任何项目。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkDeleteDialog = false
                        val t = token
                        if (t == null) {
                            scope.launch { snackbarHostState.showSnackbar("未登录，无法删除") }
                        } else if (selected > 0) {
                            viewModel.deleteSelected(t)
                            scope.launch { snackbarHostState.showSnackbar("正在删除 $selected 项") }
                        }
                    },
                    enabled = selected > 0
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) {
                    Text("取消")
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

    // 属性详情对话框
    if (showPropsDialog) {
        val target = propsTarget
        PropertiesDialog(
            name = target?.second,
            details = propsDetails,
            onDismiss = {
                showPropsDialog = false
                propsDetails = null
            }
        )
    }
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
private fun FilesLoadingPlaceholder(
    modifier: Modifier = Modifier,
    rows: Int = 12
) {
    val shimmer = PlaceholderHighlight.shimmer()
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧缩略图/图标圆角方块（更贴近真实列表）
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .placeholder(
                            visible = true,
                            highlight = shimmer,
                            shape = MaterialTheme.shapes.medium
                        )
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // 文件名行
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(16.dp)
                            .placeholder(
                                visible = true,
                                highlight = shimmer,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // 次要信息行（大小/时间）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(12.dp)
                            .placeholder(
                                visible = true,
                                highlight = shimmer,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
                Spacer(Modifier.width(12.dp))
                // 右侧更多菜单占位
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .placeholder(
                            visible = true,
                            highlight = shimmer,
                            shape = MaterialTheme.shapes.extraSmall
                        )
                )
            }
        }
    }
}

// 文件保存工具已提取至 ui.util.FileSave.saveToTree

private suspend fun startDownloadForItem(
    context: Context,
    viewModel: FilesViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    downloadPref: DownloadLocationPreference,
    token: String,
    item: DriveItemDto
) {
    if (item.file == null) {
        scope.launch { snackbarHostState.showSnackbar("无法下载：所选项目不是文件") }
        return
    }
    val itemId = item.id
    if (itemId.isNullOrEmpty()) {
        scope.launch { snackbarHostState.showSnackbar("无法下载：缺少条目ID") }
        return
    }
    val fileName = item.name ?: "download"
    val totalBytes = item.size
    val url = try {
        viewModel.getDownloadUrl(itemId, token)
    } catch (e: Exception) {
        scope.launch { snackbarHostState.showSnackbar(e.message ?: "下载失败") }
        null
    }
    if (url.isNullOrEmpty()) {
        scope.launch { snackbarHostState.showSnackbar("无法获取下载链接") }
        return
    }
    when (downloadPref.mode) {
        DownloadLocationMode.SYSTEM_DOWNLOADS -> {
            startSystemDownloadWithNotification(context, url, fileName)
            scope.launch { snackbarHostState.showSnackbar("已开始下载：$fileName") }
        }
        DownloadLocationMode.CUSTOM_TREE -> {
            val tree = downloadPref.treeUri
            if (tree.isNullOrEmpty()) {
                scope.launch { snackbarHostState.showSnackbar("未选择自定义下载目录") }
                return
            }
            val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            val cancelFlag = java.util.concurrent.atomic.AtomicBoolean(false)
            DownloadRegistry.registerCustom(notificationId, cancelFlag)
            TransferTracker.start(
                notificationId = notificationId,
                title = fileName,
                type = TransferTracker.TransferType.DOWNLOAD_CUSTOM,
                allowCancel = true,
                indeterminate = totalBytes == null
            )
            createDownloadChannel(context)
            if (totalBytes == null) {
                showOrUpdateProgress(context, notificationId, fileName, null, null, true, withCancelAction = true)
                TransferTracker.updateProgress(notificationId, progress = null, indeterminate = true)
            } else {
                showOrUpdateProgress(context, notificationId, fileName, 0, 100, false, withCancelAction = true)
                TransferTracker.updateProgress(notificationId, progress = 0, indeterminate = false)
            }
            scope.launch { snackbarHostState.showSnackbar("已开始下载：$fileName") }
            var saved = false
            var failureMessage: String? = null
            try {
                saved = withContext(Dispatchers.IO) {
                    saveToTree(
                        context = context,
                        treeUriString = tree,
                        fileName = fileName,
                        downloadUrl = url,
                        totalBytes = totalBytes,
                        onProgress = { downloaded, total ->
                            val percent = if (total > 0L) {
                                ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                            } else null
                            if (percent != null) {
                                showOrUpdateProgress(context, notificationId, fileName, percent, 100, false, withCancelAction = true)
                                TransferTracker.updateProgress(notificationId, percent, indeterminate = false)
                            } else {
                                showOrUpdateProgress(context, notificationId, fileName, null, null, true, withCancelAction = true)
                                TransferTracker.updateProgress(notificationId, null, indeterminate = true)
                            }
                        },
                        cancelFlag = cancelFlag
                    )
                }
            } catch (e: Exception) {
                failureMessage = e.message
            }
            val cancelled = cancelFlag.get()
            val success = saved && !cancelled
            replaceWithCompletion(context, notificationId, fileName, success)
            when {
                success -> {
                    TransferTracker.markSuccess(notificationId)
                    scope.launch { snackbarHostState.showSnackbar("已保存到自定义目录：$fileName") }
                }
                cancelled -> {
                    TransferTracker.markCancelled(notificationId, "已取消")
                    scope.launch { snackbarHostState.showSnackbar("已取消") }
                }
                else -> {
                    TransferTracker.markFailed(notificationId, failureMessage)
                    scope.launch { snackbarHostState.showSnackbar(failureMessage ?: "下载失败") }
                }
            }
            DownloadRegistry.cleanup(notificationId)
        }
    }
}
