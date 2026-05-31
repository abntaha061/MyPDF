package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PdfFile
import com.example.ui.PdfRendererViewModel
import com.example.ui.components.PdfThumbnailView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PdfRendererViewModel,
    onMenuClick: () -> Unit,
    onOpenFile: (PdfFile) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val allFiles by viewModel.allFiles.collectAsState()

    var isGridView by remember { mutableStateOf(false) }
    var selectedFileForMenu by remember { mutableStateOf<PdfFile?>(null) }
    var showMoveFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }
    
    // File picker launcher
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importSelectedPdf(uri) { importedFile ->
                if (importedFile != null) {
                    Toast.makeText(context, context.getString(R.string.success_import), Toast.LENGTH_LONG).show()
                    onOpenFile(importedFile)
                } else {
                    Toast.makeText(context, context.getString(R.string.error_loading_pdf), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Sidebar drawer")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search PDF")
                    }
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Change visual style"
                        )
                    }
                    IconButton(onClick = onNavigateToLibrary) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Document library")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePicker.launch("application/pdf") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("import_pdf_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.btn_import_pdf))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Executive Welcome Dashboard Header
            WpsDashboardHeader(allFilesCount = allFiles.size, recentCount = recentFiles.size, onImportClick = {
                filePicker.launch("application/pdf")
            })

            var activeHomeTab by remember { mutableStateOf(0) } // 0 = Recent Files, 1 = Reading Statistics & Logs

            TabRow(selectedTabIndex = activeHomeTab) {
                Tab(
                    selected = activeHomeTab == 0,
                    onClick = { activeHomeTab = 0 },
                    text = { Text("الملفات الأخيرة", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.RecentActors, null) }
                )
                Tab(
                    selected = activeHomeTab == 1,
                    onClick = { activeHomeTab = 1 },
                    text = { Text("نشاط وإحصائيات القراءة", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Analytics, null) }
                )
            }

            if (activeHomeTab == 0) {
                Text(
                    text = stringResource(R.string.tab_recent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                if (recentFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                contentDescription = "Empty PDFs",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_recent_files),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { filePicker.launch("application/pdf") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.btn_import_pdf))
                            }
                        }
                    }
                } else {
                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(recentFiles) { file ->
                                PdfGridItem(
                                    file = file,
                                    viewModel = viewModel,
                                    onClick = { onOpenFile(file) },
                                    onMenuClick = { selectedFileForMenu = file }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(recentFiles) { file ->
                                PdfListItem(
                                    file = file,
                                    viewModel = viewModel,
                                    onClick = { onOpenFile(file) },
                                    onMenuClick = { selectedFileForMenu = file }
                                )
                            }
                        }
                    }
                }
            } else {
                // GORGEOUS ANALYTICS TAB CONTENT
                val allHistory by viewModel.allReadingHistory.collectAsState()
                val todayStart = remember {
                    Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                }
                val weekAgoStart = remember {
                    Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                    }.timeInMillis
                }

                val todayCount = remember(allHistory) {
                    allHistory.count { it.timestamp >= todayStart }
                }
                val weekCount = remember(allHistory) {
                    allHistory.count { it.timestamp >= weekAgoStart }
                }
                val estimatedMinutes = remember(todayCount) {
                    (todayCount * 1.5).roundToInt().coerceAtLeast(3)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Stat Cards Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Daily Study Time card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Timer, "Time", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("وقت القراءة اليومي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$estimatedMinutes دقيقة",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Weekly Page Count card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.MenuBook, "Pages", tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("صفحات هذا الأسبوع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$weekCount صفحة",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

                    // Activity Graph Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    "رسم بياني لنشاط القراءة الأسبوعي (Activity Graph)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Days Arabic list & mapping
                                val daysArabic = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
                                val pageReadsPerDay = remember(allHistory) {
                                    val counts = IntArray(7)
                                    val cal = Calendar.getInstance()
                                    allHistory.forEach { h ->
                                        cal.timeInMillis = h.timestamp
                                        val idx = cal.get(Calendar.DAY_OF_WEEK) - 1
                                        if (idx in 0..6) counts[idx]++
                                    }
                                    counts
                                }
                                val maxPages = remember(pageReadsPerDay) {
                                    pageReadsPerDay.maxOrNull()?.coerceAtLeast(1) ?: 1
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    for (i in 0..6) {
                                        val count = pageReadsPerDay[i]
                                        val barHeightPercent = count.toFloat() / maxPages.toFloat()

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (count > 0) {
                                                Text(
                                                    text = "$count",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                     color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .width(16.dp)
                                                    .fillMaxHeight(0.85f * barHeightPercent.coerceAtLeast(0.06f))
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(
                                                        if (count > 0) MaterialTheme.colorScheme.primary 
                                                        else Color.Gray.copy(alpha = 0.2f)
                                                    )
                                            )
                                             Spacer(modifier = Modifier.height(4.dp))
                                             Text(
                                                 text = daysArabic[i].take(2),
                                                 fontSize = 9.sp,
                                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                                             )
                                         }
                                     }
                                 }
                             }
                         }
                     }

                     // Section header for logged events
                     item {
                         Row(
                             modifier = Modifier.fillMaxWidth(),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             if (allHistory.isNotEmpty()) {
                                 TextButton(onClick = { viewModel.clearAllReadingHistory() }) {
                                     Text("تفريغ السجل الكامل", color = Color.Red, fontSize = 12.sp)
                                 }
                             }
                             Spacer(modifier = Modifier.weight(1f))
                             Text(
                                 "سجل تواريخ فتح الملفات والتقدم",
                                 fontWeight = FontWeight.Bold,
                                 fontSize = 14.sp,
                                 color = MaterialTheme.colorScheme.onBackground
                             )
                         }
                     }

                     // Reading logged items
                     if (allHistory.isEmpty()) {
                         item {
                             Box(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(24.dp),
                                 contentAlignment = Alignment.Center
                             ) {
                                 Text(
                                     "لا توجد عمليات قراءة مسجلة حتى الآن. ابدأ بفتح أي ملف لبناء سجلك التعليمي!",
                                     fontSize = 12.sp,
                                     color = Color.Gray,
                                     textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                 )
                             }
                         }
                     } else {
                         items(allHistory) { log ->
                             val fileProgress = remember(allFiles) {
                                 allFiles.find { it.id == log.pdfFileId }
                             }
                             val progressPercent = remember(fileProgress) {
                                 if (fileProgress != null && fileProgress.totalPages > 0) {
                                     ((fileProgress.currentPage + 1) * 100) / fileProgress.totalPages
                                 } else 0
                             }

                             Card(
                                 modifier = Modifier.fillMaxWidth(),
                                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                             ) {
                                 Row(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .padding(12.dp)
                                         .clickable {
                                             val coreFile = allFiles.find { it.id == log.pdfFileId }
                                             if (coreFile != null) {
                                                 val targetFile = coreFile.copy(currentPage = log.pageIndex)
                                                 onOpenFile(targetFile)
                                             } else {
                                                 Toast.makeText(context, "الذى تحاول فتحه غير متوفر.", Toast.LENGTH_SHORT).show()
                                             }
                                         },
                                     verticalAlignment = Alignment.CenterVertically,
                                     horizontalArrangement = Arrangement.SpaceBetween
                                 ) {
                                     val dateText = remember(log.timestamp) {
                                         SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                                     }
                                     Text(
                                         text = dateText,
                                         fontSize = 10.sp,
                                         color = Color.Gray
                                     )

                                     Column(
                                         horizontalAlignment = Alignment.End,
                                         modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                     ) {
                                         Text(
                                             text = log.pdfTitle,
                                             fontSize = 12.sp,
                                             fontWeight = FontWeight.Bold,
                                             maxLines = 1,
                                             overflow = TextOverflow.Ellipsis
                                         )
                                         Spacer(modifier = Modifier.height(2.dp))
                                         Row(
                                             horizontalArrangement = Arrangement.spacedBy(6.dp),
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             Text(
                                                 text = "التقدم التقريبي: $progressPercent%",
                                                 fontSize = 10.sp,
                                                 color = MaterialTheme.colorScheme.primary,
                                                 fontWeight = FontWeight.SemiBold
                                             )
                                             Text("•", fontSize = 10.sp, color = Color.Gray)
                                             Text(
                                                 text = "الصفحةالمفتوحة: ${log.pageIndex + 1}",
                                                 fontSize = 10.sp,
                                                 color = Color.Gray
                                             )
                                         }
                                     }
                                 }
                             }
                         }
                     }
                 }
            }
        }

        // More options dialog sheet
        selectedFileForMenu?.let { file ->
            AlertDialog(
                onDismissRequest = { selectedFileForMenu = null },
                title = {
                    Text(
                        text = file.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Info line
                        Text(
                            text = "Size: ${file.formattedSize}  •  Pages: ${file.totalPages}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Bookmark action item
                        ListItem(
                            headlineContent = { Text(if (file.isBookmarked) "Remove Bookmark" else "Add Bookmark") },
                            leadingContent = { 
                                Icon(
                                    imageVector = if (file.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (file.isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.toggleBookmark(file)
                                selectedFileForMenu = null
                            }
                        )

                        // Move Folder action item
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.btn_move)) },
                            leadingContent = { Icon(imageVector = Icons.Default.DriveFileMove, contentDescription = null) },
                            modifier = Modifier.clickable {
                                showMoveFolderDialog = true
                            }
                        )

                        // Delete action item
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.btn_delete), color = Color.Red) },
                            leadingContent = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                            modifier = Modifier.clickable {
                                viewModel.deleteDocument(file)
                                selectedFileForMenu = null
                                Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedFileForMenu = null }) {
                        Text(stringResource(R.string.btn_close))
                    }
                }
            )
        }

        // Dialog for categorization folders
        if (showMoveFolderDialog && selectedFileForMenu != null) {
            AlertDialog(
                onDismissRequest = { showMoveFolderDialog = false },
                title = { Text(stringResource(R.string.dialog_create_folder)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Current category: ${selectedFileForMenu?.folderName?.ifBlank { "Unorganized" }}")
                        OutlinedTextField(
                            value = newFolderNameInput,
                            onValueChange = { newFolderNameInput = it },
                            label = { Text(stringResource(R.string.dialog_folder_name_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.moveDocumentToFolder(selectedFileForMenu!!.id, newFolderNameInput)
                            showMoveFolderDialog = false
                            selectedFileForMenu = null
                            newFolderNameInput = ""
                            Toast.makeText(context, "Moved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(stringResource(R.string.btn_move))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMoveFolderDialog = false }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun WpsDashboardHeader(
    allFilesCount: Int,
    recentCount: Int,
    onImportClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WPS Office PDF Workspace",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Professional PDF Viewing & Management Engine",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = onImportClick) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload Document",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Quick Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(imageVector = Icons.Default.LibraryBooks, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Total Files", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text(text = "$allFilesCount", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Recent Views", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text(text = "$recentCount", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfListItem(
    file: PdfFile,
    viewModel: PdfRendererViewModel,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onMenuClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PDF cover thumbnail integration
            PdfThumbnailView(
                filePath = file.filePath,
                viewModel = viewModel,
                modifier = Modifier
                    .size(54.dp, 68.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${file.formattedSize} • ${file.totalPages} pgs",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (file.folderName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = file.folderName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Options Button
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options icon",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfGridItem(
    file: PdfFile,
    viewModel: PdfRendererViewModel,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onMenuClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Pin favorite icon at the top corner
                if (file.isBookmarked) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Bookmarked",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(18.dp)
                    )
                }

                PdfThumbnailView(
                    filePath = file.filePath,
                    viewModel = viewModel,
                    modifier = Modifier
                        .size(90.dp, 115.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = file.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = file.formattedSize,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
