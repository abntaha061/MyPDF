package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
    
    var hasPermission by remember { mutableStateOf(hasAllStoragePermissionsGranted(context)) }
    
    // Automatic checking on app resume/active focus (such as when returning from Settings)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPermission = hasAllStoragePermissionsGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (hasPermission) "workspace" else "permissions"
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
    val bookmarkedFiles by viewModel.bookmarkedFiles.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.tab_bookmarks), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
            if (bookmarkedFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "No bookmarks",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.no_bookmarks),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
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
