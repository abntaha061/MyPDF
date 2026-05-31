package com.example

import android.net.Uri
import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.PdfFile
import com.example.ui.PdfRendererViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReadingScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.DictionaryScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PdfRendererViewModel = viewModel()
            
            // Dynamic application theme selection
            MyApplicationTheme(themeMode = viewModel.appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WpsAppNavigation(viewModel)
                }
            }
        }
    }
}

fun hasAllStoragePermissionsGranted(context: android.content.Context): Boolean {
    val readOk = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    
    val writeOk = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        android.os.Environment.isExternalStorageManager()
    } else {
        readOk && writeOk
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WpsAppNavigation(viewModel: PdfRendererViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val startDest = remember {
        if (hasAllStoragePermissionsGranted(context)) "workspace" else "permissions"
    }

    var hasPermission by remember { mutableStateOf(hasAllStoragePermissionsGranted(context)) }
    
    // Automatic checking on app resume/active focus (such as when returning from Settings)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val currentPermission = hasAllStoragePermissionsGranted(context)
                if (currentPermission && !hasPermission) {
                    hasPermission = true
                    navController.navigate("workspace") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable("permissions") {
            com.example.ui.screens.PermissionsOnboardingScreen(
                onPermissionsGranted = {
                    hasPermission = true
                    navController.navigate("workspace") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }
        composable("workspace") {
            WpsWorkspaceTabs(
                viewModel = viewModel,
                onOpenFile = { file ->
                    viewModel.openPdfFile(file)
                    navController.navigate("reading")
                },
                onNavigateToLibrary = {
                    navController.navigate("library")
                }
            )
        }
        composable("library") {
            LibraryScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onOpenFile = { file ->
                    viewModel.openPdfFile(file)
                    navController.navigate("reading")
                }
            )
        }
        composable("reading") {
            ReadingScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WpsWorkspaceTabs(
    viewModel: PdfRendererViewModel,
    onOpenFile: (PdfFile) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Active Tab state
    var selectedTab by remember { mutableStateOf(0) }

    // File Picker integration inside drawer
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importSelectedPdf(uri) { importedFile ->
                if (importedFile != null) {
                    Toast.makeText(context, context.getString(R.string.success_import), Toast.LENGTH_SHORT).show()
                    onOpenFile(importedFile)
                } else {
                    Toast.makeText(context, context.getString(R.string.error_loading_pdf), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Header Brand
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(24.dp)
                            .statusBarsPadding()
                    ) {
                        Column {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.drawer_title),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "WPS Office Document Hub",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Drawer Action Items
                    NavigationDrawerItem(
                        icon = { Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = null) },
                        label = { Text(stringResource(R.string.drawer_all_files)) },
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null) },
                        label = { Text(stringResource(R.string.drawer_folders)) },
                        selected = false,
                        onClick = {
                            onNavigateToLibrary()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null) },
                        label = { Text(stringResource(R.string.drawer_import)) },
                        selected = false,
                        onClick = {
                            filePicker.launch("application/pdf")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    NavigationDrawerItem(
                        icon = { Icon(imageVector = Icons.Default.Translate, contentDescription = null) },
                        label = { Text("القاموس والمفردات") },
                        selected = selectedTab == 3,
                        onClick = {
                            selectedTab = 3
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_settings)) },
                        selected = selectedTab == 4,
                        onClick = {
                            selectedTab = 4
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(imageVector = Icons.Default.History, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_recent)) },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(imageVector = Icons.Default.Bookmark, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_bookmarks)) },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_search)) },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    NavigationBarItem(
                        icon = { Icon(imageVector = Icons.Default.Translate, contentDescription = null) },
                        label = { Text("المفردات") },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                    NavigationBarItem(
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_settings)) },
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onOpenFile = onOpenFile,
                        onNavigateToSearch = { selectedTab = 2 },
                        onNavigateToLibrary = onNavigateToLibrary
                    )
                    1 -> BookmarksTabScreen(
                        viewModel = viewModel,
                        onOpenFile = onOpenFile,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                    2 -> SearchScreen(
                        viewModel = viewModel,
                        onOpenFile = onOpenFile
                    )
                    3 -> DictionaryScreen(
                        viewModel = viewModel,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                    4 -> SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksTabScreen(
    viewModel: PdfRendererViewModel,
    onOpenFile: (PdfFile) -> Unit,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val bookmarkedFiles by viewModel.bookmarkedFiles.collectAsState()
    val allPageBookmarks by viewModel.allPageBookmarks.collectAsState()
    val allFiles by viewModel.allFiles.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Pages Bookmarks, 1 = Starred Files
    var sortByDate by remember { mutableStateOf(true) } // true = Date, false = Page Index

    val sortedPageBookmarks = remember(allPageBookmarks, sortByDate) {
        if (sortByDate) {
            allPageBookmarks.sortedByDescending { it.addedDate }
        } else {
            allPageBookmarks.sortedWith(compareBy({ it.pdfTitle }, { it.pageIndex }))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "الإشارات المرجعية والملفات", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Drawer icon")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Segmented Tab Selector
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("إشارات الصفحات المخصصة", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.BookmarkBorder, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("الملفات المفضلة", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.StarBorder, null) }
                )
            }

            if (selectedTab == 0) {
                // Pages Bookmarks Segment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sorting controls using FilterChip
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = sortByDate,
                            onClick = { sortByDate = true },
                            label = { Text("تاريخ الإضافة", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = !sortByDate,
                            onClick = { sortByDate = false },
                            label = { Text("رقم الصفحة", fontSize = 11.sp) }
                        )
                    }

                    // Export central JSON Button
                    if (sortedPageBookmarks.isNotEmpty()) {
                        IconButton(onClick = {
                            try {
                                val array = org.json.JSONArray()
                                sortedPageBookmarks.forEach { bm ->
                                    val obj = org.json.JSONObject().apply {
                                        put("id", bm.id)
                                        put("fileId", bm.pdfFileId)
                                        put("pdfTitle", bm.pdfTitle)
                                        put("pageIndex", bm.pageIndex)
                                        put("label", bm.label)
                                        put("addedDate", bm.addedDate)
                                    }
                                    array.put(obj)
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "تصدير الإشارات المرجعية المركزية لقارئ PDF")
                                    putExtra(Intent.EXTRA_TEXT, array.toString(2))
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "تصدير / مشاركة الإشارات كـ JSON:"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "فشل تصدير الإشارات المرجعية", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Share, "تصدير الإشارات", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (sortedPageBookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "No bookmarks",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد علامات مرجعية للصفحات مضافة حالياً.",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "يمكنك إضافة علامات مخصصة مباشرة أثناء قراءة أي ملف PDF بالضغط على زر الحفظ أعلى شريط القراءة.",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(sortedPageBookmarks) { bm ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val coreFile = allFiles.find { it.id == bm.pdfFileId }
                                            if (coreFile != null) {
                                                val targetFile = coreFile.copy(currentPage = bm.pageIndex)
                                                onOpenFile(targetFile)
                                            } else {
                                                Toast.makeText(context, "الملف غير متوفر حالياً.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.deletePageBookmark(bm.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteForever, "حذف", tint = Color.Red.copy(alpha = 0.8f))
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = bm.label,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "الصفحة ${bm.pageIndex + 1}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "•",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = bm.pdfTitle,
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Starred Files Segment (Original files bookmark)
                if (bookmarkedFiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "No bookmarked files",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد ملفات تفضيل مميزة بنجمة.",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(bookmarkedFiles) { file ->
                            com.example.ui.screens.PdfListItem(
                                file = file,
                                viewModel = viewModel,
                                onClick = { onOpenFile(file) },
                                onMenuClick = { viewModel.toggleBookmark(file) }
                            )
                        }
                    }
                }
            }
        }
    }
}
