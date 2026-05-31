package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PdfFile
import com.example.ui.PdfRendererViewModel
import com.example.ui.components.DetailedFileInfoDialog
import com.example.ui.components.AdvancedFilePickerDialog
import com.example.ui.components.PdfThumbnailView
import com.example.ui.components.getSemanticTagColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    viewModel: PdfRendererViewModel,
    onBackClick: () -> Unit,
    onOpenFile: (PdfFile) -> Unit
) {
    val context = LocalContext.current
    val allFolders by viewModel.allFolders.collectAsState()
    val allFiles by viewModel.allFiles.collectAsState()

    // Screen View Configuration States
    var isGridView by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeCategoryFilter by remember { mutableStateOf("الكل") } // الكل, المفضلة, كتب, تقارير, اختبارات, مستندات
    var activeFolderFilter by remember { mutableStateOf<String?>(null) }
    var activeSortMode by remember { mutableStateOf("addedDate") } // addedDate, title, fileSize, currentPage

    // Dialog trackers
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    var showAdvancedFilePicker by remember { mutableStateOf(false) }
    var selectedFileForDetails by remember { mutableStateOf<PdfFile?>(null) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    // Scroll States
    val categoryScrollState = rememberScrollState()

    // SAF file picker trigger
    val systemSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importSelectedPdf(uri) { importedFile ->
                if (importedFile != null) {
                    Toast.makeText(context, "تم استيراد الملف بنجاح!", Toast.LENGTH_SHORT).show()
                    onOpenFile(importedFile)
                } else {
                    Toast.makeText(context, "فشل استيراد الملف", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Dynamic processing: Search, Category Filter, Folder Filter, and Sorted Streams
    val displayedFiles = remember(allFiles, searchQuery, activeCategoryFilter, activeFolderFilter, activeSortMode) {
        var list = allFiles

        // 1. Text Filter (Search name, content simulation, tags, folder)
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.tags.lowercase().contains(q) ||
                it.folderName.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
            }
        }

        // 2. Category Filter
        if (activeCategoryFilter != "الكل") {
            list = if (activeCategoryFilter == "المفضلة") {
                list.filter { it.isFavorite }
            } else {
                list.filter { it.category.equals(activeCategoryFilter, ignoreCase = true) }
            }
        }

        // 3. Folder Filter
        if (activeFolderFilter != null) {
            list = list.filter { it.folderName.equals(activeFolderFilter, ignoreCase = true) }
        }

        // 4. Sort calculations
        when (activeSortMode) {
            "title" -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            "fileSize" -> list.sortedByDescending { it.fileSize }
            "currentPage" -> list.sortedByDescending { it.currentPage } // Most opened / progressive
            else -> list.sortedByDescending { it.addedDate } // default newest date first
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = activeFolderFilter ?: "المكتبة وإدارة المستندات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeFolderFilter != null) {
                            activeFolderFilter = null
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Create Category Folder")
                    }
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Switch layout view"
                        )
                    }
                    IconButton(onClick = { showAdvancedFilePicker = true }) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = "Advanced Importer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
            // 1. Unified Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المستند، التصنيف، المجلد أو الأوسمة...", fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search query")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // 2. Drag & Drop Simulated Upload Zone
            Card(
                onClick = { showAdvancedFilePicker = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
                border = BorderStroke(1.5.dp, Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "سحب وإفلات ملف PDF السحابي والمحلي هنا للتحليل والفرز",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "انقر للتصفح من Google Drive, SD Card والتخزين الداخلي",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // 3. Horizontal Category Navigation Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(categoryScrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("الكل", "المفضلة", "كتب", "تقارير", "اختبارات", "مستندات").forEach { cat ->
                    val isSelected = activeCategoryFilter == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            activeCategoryFilter = cat
                            // Clear folder list filter back to showcase category level catalog
                            activeFolderFilter = null
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (cat == "المفضلة") {
                                    Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isSelected) Color.White else Color(0xFFFFB300))
                                }
                                Text(cat, fontSize = 12.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Show Custom Folders Section ONLY if no folder path is chosen
            if (activeFolderFilter == null && searchQuery.isEmpty() && activeCategoryFilter == "الكل") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.folders_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مجلد جديد", fontSize = 12.sp)
                        }
                    }

                    if (allFolders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "لا توجد مجلدات مخصصة مبنية بعد. أنشئ مجلداً لتجميع الكتب والمقررات.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Virtual folder: All
                            FolderHorizontalCard(
                                name = "كل المجلدات",
                                count = allFiles.size,
                                isSelected = activeFolderFilter == null,
                                onClick = { activeFolderFilter = null }
                            )

                            allFolders.forEach { folderName ->
                                val count = allFiles.count { it.folderName.equals(folderName, ignoreCase = true) }
                                FolderHorizontalCard(
                                    name = folderName,
                                    count = count,
                                    isSelected = activeFolderFilter == folderName,
                                    onClick = { activeFolderFilter = folderName }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 4. Sort options & Items counts row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المستندات (${displayedFiles.size} ملف)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Dropdown sort trigger
                Box {
                    TextButton(onClick = { isSortMenuExpanded = true }) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (activeSortMode) {
                                "title" -> "رتب: أبجديًا (أ-ي)"
                                "fileSize" -> "رتب: الحجم الأكبر"
                                "currentPage" -> "رتب: الأكثر فتحاً"
                                else -> "رتب: الأحدث تاريخاً"
                            },
                            fontSize = 12.sp
                        )
                    }

                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("الأحدث تاريخاً") },
                            onClick = {
                                activeSortMode = "addedDate"
                                isSortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("الاسم (أ-ي)") },
                            onClick = {
                                activeSortMode = "title"
                                isSortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("الحجم الأكبر") },
                            onClick = {
                                activeSortMode = "fileSize"
                                isSortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("الأكثر فتحاً وقراءة") },
                            onClick = {
                                activeSortMode = "currentPage"
                                isSortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 5. Document listings
            if (displayedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لم نجد أي مستند يطابق شروط الفرز والبحث الحالية.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(displayedFiles) { file ->
                            LibraryFileGridCard(
                                file = file,
                                viewModel = viewModel,
                                onClick = { onOpenFile(file) },
                                onInfoClick = { selectedFileForDetails = file }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(displayedFiles) { file ->
                            LibraryAdvancedFileRow(
                                file = file,
                                viewModel = viewModel,
                                onClick = { onOpenFile(file) },
                                onInfoClick = { selectedFileForDetails = file },
                                onRemoveFromFolder = {
                                    viewModel.moveDocumentToFolder(file.id, "")
                                    Toast.makeText(context, "تمت الإزالة من المجلد المبوب", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Add Folder dialog
        if (showCreateFolderDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFolderDialog = false },
                title = { Text(stringResource(R.string.dialog_create_folder)) },
                text = {
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        label = { Text(stringResource(R.string.dialog_folder_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val name = folderNameInput.trim()
                            if (name.isNotEmpty()) {
                                viewModel.createEmptyFolder(name)
                                activeFolderFilter = name
                                showCreateFolderDialog = false
                                folderNameInput = ""
                            }
                        }
                    ) {
                        Text(stringResource(R.string.btn_create))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolderDialog = false }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }

        // Detailed Metadata Dialog
        selectedFileForDetails?.let { file ->
            DetailedFileInfoDialog(
                file = file,
                viewModel = viewModel,
                onDismiss = { selectedFileForDetails = null }
            )
        }

        // Advanced Picker Hub dialog
        if (showAdvancedFilePicker) {
            AdvancedFilePickerDialog(
                viewModel = viewModel,
                onOpenFile = onOpenFile,
                onDismissRequest = { showAdvancedFilePicker = false }
            )
        }
    }
}

@Composable
fun FolderHorizontalCard(
    name: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.width(135.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFFFFB300),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count ملف",
                fontSize = 10.sp,
                color = if (isSelected) Color.White.copy(alpha = 0.7f) else Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryAdvancedFileRow(
    file: PdfFile,
    viewModel: PdfRendererViewModel,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    onRemoveFromFolder: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PdfThumbnailView(
                filePath = file.filePath,
                viewModel = viewModel,
                modifier = Modifier
                    .size(44.dp, 58.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (file.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred Favorite",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = file.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${file.formattedSize} • ${file.totalPages} صفحة • قسم: ${file.category}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                // Custom color tag-chips list inside row
                if (file.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        file.tags.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .take(3)
                            .forEach { tag ->
                                val chipColor = getSemanticTagColor(tag)
                                Box(
                                    modifier = Modifier
                                        .background(chipColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = chipColor
                                    )
                                }
                            }
                    }
                }
            }

            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Show item information",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.73f)
                )
            }

            if (file.folderName.isNotEmpty()) {
                IconButton(onClick = onRemoveFromFolder) {
                    Icon(
                        imageVector = Icons.Default.FolderDelete,
                        contentDescription = "Remove from folder",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryFileGridCard(
    file: PdfFile,
    viewModel: PdfRendererViewModel,
    onClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (file.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite star indicator",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(18.dp)
                    )
                }

                PdfThumbnailView(
                    filePath = file.filePath,
                    viewModel = viewModel,
                    modifier = Modifier
                        .size(85.dp, 110.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .align(Alignment.Center)
                )

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Detailed information",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = file.title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "${file.formattedSize} • ${file.totalPages} صفحة",
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Category tag indicator badge below name
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = file.category,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
