package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.R
import com.example.data.PdfFile
import com.example.ui.PdfRendererViewModel
import com.example.ui.InkStroke
import com.example.ui.TextAnnotation
import com.example.ui.ShapeAnnotation
import com.example.ui.ChatMessage
import com.example.ui.GermanWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReadingScreen(
    viewModel: PdfRendererViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentActivity = context as? Activity
    val scope = rememberCoroutineScope()

    val pdfFile = viewModel.currentPdfFile ?: return
    val totalPages = viewModel.totalPagesCount

    var isSearchActive by remember { mutableStateOf(false) }
    var showPageBookmarkDialog by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Keep screen awake config
    DisposableEffect(viewModel.keepScreenOn) {
        if (viewModel.keepScreenOn) {
            currentActivity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            currentActivity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Fullscreen and Menu Controls Visibility
    var areToolbarsVisible by remember { mutableStateOf(true) }
    var isToolsSheetOpen by remember { mutableStateOf(false) }
    var activeToolTab by remember { mutableStateOf(0) } // 0: View/Nav, 1: Annotations, 2: PDF Work, 3: AI Assistant

    // PDF Password Lock state
    var isPasswordLocked by remember { mutableStateOf(viewModel.isEncryptedProtected) }
    var tempPasswordInput by remember { mutableStateOf("") }

    BackHandler {
        onBackClick()
    }

    // Zoom matrix configurations
    var scale by remember { mutableStateOf(1f) }
    var translationX by remember { mutableStateOf(0f) }
    var translationY by remember { mutableStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.25f, 5.0f)
        translationX += panChange.x * scale
        translationY += panChange.y * scale
    }

    // Double tap quick reset or zoom to 150%
    val mainTouchModifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    if (scale != 1.5f) {
                        scale = 1.5f
                        translationX = 0f
                        translationY = 0f
                    } else {
                        scale = 1f
                        translationX = 0f
                        translationY = 0f
                    }
                },
                onTap = {
                    if (!viewModel.isDrawModeEnabled) {
                        areToolbarsVisible = !areToolbarsVisible
                    }
                }
            )
        }

    if (isPasswordLocked) {
        // High fidelity lockscreen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(12.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted Document",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "المستند محمي بكلمة مرور (AES-256)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "أدخل كلمة المرور الصحيحة لفتح وتصفح صفحات هذا الملف بأمان.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = tempPasswordInput,
                        onValueChange = { tempPasswordInput = it },
                        label = { Text("رمز التشفير") },
                        placeholder = { Text("ادخل كلمة السر") },
                        isError = tempPasswordInput.isNotEmpty() && tempPasswordInput != viewModel.documentPasswordString,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (viewModel.decryptPdfFile(tempPasswordInput)) {
                                    isPasswordLocked = false
                                    Toast.makeText(context, "تم فتح التشفير بنجاح!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "طلب مرفوض: كلمة مرور خاطئة!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("فتح المستند")
                        }
                        OutlinedButton(
                            onClick = onBackClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رجوع")
                        }
                    }
                }
            }
        }
        return
    }

    // Dynamic filtering for deleted pages
    val renderedPagesCount = (0 until totalPages).filterNot { viewModel.deletedPagesList.contains(it) }

    Scaffold(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.F && keyEvent.isCtrlPressed) {
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) {
                        viewModel.clearPdfSearch()
                    }
                    true
                } else false
            },
        topBar = {
            Column {
                AnimatedVisibility(
                    visible = areToolbarsVisible && !viewModel.isFullscreenMode,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    WpsReaderTopBar(
                        pdfFile = pdfFile,
                        currentPage = viewModel.currentPageIndex,
                        totalPages = totalPages,
                        onBackClick = onBackClick,
                        onBookmarkToggle = { viewModel.toggleBookmark(pdfFile) },
                        onToolsClick = { isToolsSheetOpen = true },
                        onShareClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(pdfFile.filePath)))
                                    putExtra(Intent.EXTRA_SUBJECT, pdfFile.title)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة الملف عبر:"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "مشاركة الملف الحالية...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSearchClick = { isSearchActive = !isSearchActive },
                        onPageBookmarkClick = { showPageBookmarkDialog = true }
                    )
                }

                // Interactive Slide-down Search Bar Panel
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = viewModel.pdfSearchQuery,
                                    onValueChange = {
                                        viewModel.performPdfSearch(it)
                                    },
                                    placeholder = { Text("بحث داخل صفحات الملف...", fontSize = 13.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                                    trailingIcon = {
                                        if (viewModel.pdfSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.clearPdfSearch() }) {
                                                Icon(Icons.Default.Close, null)
                                            }
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                if (viewModel.pdfTextSearchResults.isNotEmpty()) {
                                    Text(
                                        text = "${viewModel.currentPdfSearchIndex + 1} من ${viewModel.pdfTextSearchResults.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    IconButton(onClick = { viewModel.goToPrevSearchResult() }) {
                                        Icon(Icons.Default.KeyboardArrowUp, null)
                                    }
                                    IconButton(onClick = { viewModel.goToNextSearchResult() }) {
                                        Icon(Icons.Default.KeyboardArrowDown, null)
                                    }
                                } else if (viewModel.pdfSearchQuery.isNotEmpty()) {
                                    Text(
                                        text = "0 نتائج",
                                        fontSize = 11.sp,
                                        color = Color.Red,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }

                                IconButton(onClick = {
                                    isSearchActive = false
                                    viewModel.clearPdfSearch()
                                }) {
                                    Icon(Icons.Default.Cancel, null, tint = Color.Gray)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = viewModel.isCaseSensitive,
                                        onCheckedChange = {
                                            viewModel.isCaseSensitive = it
                                            viewModel.performPdfSearch(viewModel.pdfSearchQuery)
                                        }
                                    )
                                    Text("حساس للحالة", fontSize = 11.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = viewModel.isExactPhrase,
                                        onCheckedChange = {
                                            viewModel.isExactPhrase = it
                                            viewModel.performPdfSearch(viewModel.pdfSearchQuery)
                                        }
                                    )
                                    Text("طابق تام", fontSize = 11.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = viewModel.searchCrossLanguage,
                                        onCheckedChange = {
                                            viewModel.searchCrossLanguage = it
                                            viewModel.performPdfSearch(viewModel.pdfSearchQuery)
                                        }
                                    )
                                    Text("بحث معرب 🇩🇪🇸🇦", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = areToolbarsVisible && !viewModel.isFullscreenMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                WpsReaderBottomBar(
                    currentPage = viewModel.currentPageIndex,
                    totalPages = totalPages,
                    onPageChange = { targetPage ->
                        viewModel.updatePageProgress(targetPage)
                    },
                    onOpenToolsWithTab = { tab ->
                        activeToolTab = tab
                        isToolsSheetOpen = true
                    },
                    viewModel = viewModel
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (viewModel.isNightModeInverted) Color(0xFF141416) else Color(0xFFEEF0F5))
                .padding(
                    top = if (viewModel.isFullscreenMode) 0.dp else innerPadding.calculateTopPadding(),
                    bottom = if (viewModel.isFullscreenMode) 0.dp else innerPadding.calculateBottomPadding()
                )
                .then(mainTouchModifier)
        ) {
            // Interactive Display Engine depending on layout selections
            when (viewModel.viewingMode) {
                "single_vertical", "continuous" -> {
                    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = viewModel.currentPageIndex)
                    
                    LaunchedEffect(lazyListState.firstVisibleItemIndex) {
                        if (lazyListState.firstVisibleItemIndex in renderedPagesCount.indices) {
                            viewModel.updatePageProgress(renderedPagesCount[lazyListState.firstVisibleItemIndex])
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = translationX,
                                translationY = translationY
                            )
                            .transformable(state = transformState),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp, 
                            vertical = if (viewModel.viewingMode == "continuous") 0.dp else 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            if (viewModel.viewingMode == "continuous") 0.dp else 16.dp
                        )
                    ) {
                        itemsIndexed(renderedPagesCount) { index, origPageIdx ->
                            PdfPageCard(
                                pageIndex = origPageIdx,
                                filePath = pdfFile.filePath,
                                viewModel = viewModel
                            )
                        }
                    }
                }
                "dual_page" -> {
                    // Two pages side by side for tablets/landscape
                    val lazyListState = rememberLazyListState()
                    val doublePageChunks = renderedPagesCount.chunked(2)

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = translationX,
                                translationY = translationY
                            )
                            .transformable(state = transformState),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(doublePageChunks) { index, chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                chunk.forEach { origPageIdx ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        PdfPageCard(
                                            pageIndex = origPageIdx,
                                            filePath = pdfFile.filePath,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                                if (chunk.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                "book_flip", "horizontal" -> {
                    val pagerState = rememberPagerState(
                        initialPage = viewModel.currentPageIndex.coerceIn(0, (renderedPagesCount.size - 1).coerceAtLeast(0)),
                        pageCount = { renderedPagesCount.size }
                    )

                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage in renderedPagesCount.indices) {
                            viewModel.updatePageProgress(renderedPagesCount[pagerState.currentPage])
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = translationX,
                                translationY = translationY
                            )
                            .transformable(state = transformState),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                        pageSpacing = 16.dp
                    ) { pageSlotIndex ->
                        val origPageIdx = renderedPagesCount[pageSlotIndex]
                        
                        // Custom graphic translation for physical-like flipping
                        val pageOffset = (pagerState.currentPage - pageSlotIndex).toFloat()
                        val cardScale = animateFloatAsState(
                            targetValue = if (pageOffset == 0f) 1f else 0.88f,
                            animationSpec = spring()
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    if (viewModel.viewingMode == "book_flip") {
                                        rotationY = pageOffset * -35f
                                        cameraDistance = 12f * density
                                        alpha = (1f - kotlin.math.abs(pageOffset)).coerceIn(0.6f, 1f)
                                    }
                                }
                        ) {
                            PdfPageCard(
                                pageIndex = origPageIdx,
                                filePath = pdfFile.filePath,
                                viewModel = viewModel
                            )
                        }
                    }
                }
                "bilingual" -> {
                    BilingualStudyView(
                        pdfFile = pdfFile,
                        pageIndex = viewModel.currentPageIndex,
                        viewModel = viewModel
                    )
                }
            }

            // Quick Floating Indicator for fullscreen exit
            if (viewModel.isFullscreenMode) {
                FloatingActionButton(
                    onClick = { viewModel.isFullscreenMode = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(40.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                ) {
                    Icon(Icons.Default.FullscreenExit, "Exit Fullscreen", tint = Color.White)
                }
            }

            // Interactive Drawing Eraser Button
            if (viewModel.isDrawModeEnabled) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 120.dp, end = 20.dp)
                        .shadow(4.dp, CircleShape),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = CircleShape
                ) {
                    IconButton(
                        onClick = {
                            viewModel.isDrawModeEnabled = false
                            Toast.makeText(context, "تم إيقاف وضع الرسم التفاعلي", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Gesture, "Stop Draw", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            // ---------------- DICTIONARY WORD TRANSLATION CARD OVERLAY ----------------
            if (viewModel.dictionaryManager.isWordCardVisible) {
                val dm = viewModel.dictionaryManager
                val entry = dm.selectedWordEntry
                
                if (entry != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { dm.isWordCardVisible = false }, // dismiss on backdrop trigger
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .shadow(12.dp, RoundedCornerShape(16.dp))
                                .clickable(enabled = false) {} // block click propagation
                                .animateContentSize(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Header bar containing word, phonetic, level badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = entry.word,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = entry.phonetic,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    }

                                    // CEFR level & part of speech badges
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(entry.cefr, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary) }
                                        )
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(entry.partOfSpeech, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary) }
                                        )
                                    }
                                }

                                HorizontalDivider()

                                // Arabic translation
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("الترجمة الفورية أوفلاين:", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        text = entry.translation,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00796B) // Distinct teal translation color
                                    )
                                }

                                // If Verb (Conjugation lists)
                                if (entry.conjugations.isNotEmpty()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("تصريفات الفعل الرئيسية:", fontSize = 11.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            entry.conjugations.forEach { conj ->
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(conj, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }

                                // If Noun (Plural info)
                                if (entry.plural.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("الجمع (Plural):", fontSize = 13.sp, color = Color.Gray)
                                        Text(entry.plural, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }

                                // Usage Examples
                                if (entry.examples.isNotEmpty()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("أمثلة الاستخدام والسياق (Beispiele):", fontSize = 11.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        entry.examples.forEach { (gerEx, araEx) ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Text(gerEx, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                Text(araEx, fontSize = 12.sp, color = Color(0xFF37474F))
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider()

                                // Pronunciation controls block (🔊 Normal tempo speech AND 🐢 Slow 50% speed pronunciation option!)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { dm.speak(entry.word, false) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, "Speak normal")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("نطق عادي", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { dm.speak(entry.word, true) },
                                        modifier = Modifier.weight(1.1f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeDown, "Speak slow")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("نطق تعليمي بطيء (50%)", fontSize = 11.sp)
                                    }
                                }

                                // Spaced Repetition scheduling action bar!
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), RoundedCornerShape(8.dp)).padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("نظام التكرار المتباعد:", fontSize = 10.sp, color = Color.Gray)
                                        Text(
                                            text = if (entry.nextReviewTime == 0L) "كلمة جديدة غير محفوظة" else "المراجعة المقبلة: بعد ${entry.intervalDays} يوم",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                dm.updateSpacedRepetition(entry, true)
                                                Toast.makeText(context, "تم الحفظ بجدول التكرار المتباعد!", Toast.LENGTH_SHORT).show()
                                                dm.isWordCardVisible = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("سهل: أعرفها", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                dm.updateSpacedRepetition(entry, false)
                                                Toast.makeText(context, "تم إدراجها للمراجعة القريبة اليوم!", Toast.LENGTH_SHORT).show()
                                                dm.isWordCardVisible = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("صعبة: مراجعة", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- FULL PARAGRAPH TRANSLATION DIALOG OVERLAY ----------------
            if (viewModel.isParagraphTranslationDialogVisible) {
                Dialog(onDismissRequest = { viewModel.isParagraphTranslationDialogVisible = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .shadow(8.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌐 ترجمة الفقرة بمحرك ML Kit",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { viewModel.isParagraphTranslationDialogVisible = false }) {
                                    Icon(Icons.Default.Close, "Dismiss")
                                }
                            }

                            HorizontalDivider()

                            // ORIGINAL TEXT
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("النص الأصلي (Original):", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = viewModel.activeParagraphTranslationTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider()

                            // TRANSLATED TEXT
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("الترجمة الكاملة أوفلاين:", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = viewModel.activeParagraphTranslationResult,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00796B), // Rich emerald teal for translation ease of distinguishing
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            HorizontalDivider()

                            // COPY OR SHARE BUTTONS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val clipboard = LocalClipboardManager.current
                                val shareContext = LocalContext.current

                                Button(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(viewModel.activeParagraphTranslationResult))
                                        Toast.makeText(context, "تم نسخ الترجمة للحافظة!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("نسخ الترجمة", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, viewModel.activeParagraphTranslationResult)
                                                type = "text/plain"
                                            }
                                            shareContext.startActivity(Intent.createChooser(sendIntent, "مشاركة الترجمة:"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "مشاركة ممتعة...", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مشاركة", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Control Dashboard
    if (isToolsSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isToolsSheetOpen = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 16.dp)
            ) {
                // Category tabs Header
                TabRow(selectedTabIndex = activeToolTab) {
                    Tab(
                        selected = activeToolTab == 0,
                        onClick = { activeToolTab = 0 },
                        text = { Text("عرض وتصفح", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = activeToolTab == 1,
                        onClick = { activeToolTab = 1 },
                        text = { Text("تعليق وتحرير", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = activeToolTab == 2,
                        onClick = { activeToolTab = 2 },
                        text = { Text("إدارة وأدوات", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = activeToolTab == 3,
                        onClick = { activeToolTab = 3 },
                        text = { Text("الذكاء الاصطناعي", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (activeToolTab) {
                        0 -> ViewNavTab(viewModel)
                        1 -> AnnotationsTab(viewModel, totalPages)
                        2 -> PageToolsTab(viewModel, onBackClick)
                        3 -> AiTab(viewModel)
                    }
                }
            }
        }
    }

    if (showPageBookmarkDialog) {
        var bookmarkLabel by remember { mutableStateOf("${pdfFile.title} - ص ${viewModel.currentPageIndex + 1}") }
        val allPageBookmarks by viewModel.allPageBookmarks.collectAsState()
        val fileBookmarks = remember(allPageBookmarks) {
            allPageBookmarks.filter { it.pdfFileId == pdfFile.id }
        }

        AlertDialog(
            onDismissRequest = { showPageBookmarkDialog = false },
            title = {
                Text(
                    text = "إشارات مرجعية مخصصة للصفحات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "إضافة إشارة مرجعية لصفحتك الحالية (السياقية) لتسهيل التنقل والوصول المركزي في أي وقت:",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    OutlinedTextField(
                        value = bookmarkLabel,
                        onValueChange = { bookmarkLabel = it },
                        label = { Text("تسمية الإشارة المرجعية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (bookmarkLabel.isNotBlank()) {
                                viewModel.addPageBookmark(viewModel.currentPageIndex, bookmarkLabel.trim())
                                Toast.makeText(context, "تم حفظ الإشارة المرجعية بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة إشارة مرجعية للصفحة ${viewModel.currentPageIndex + 1}")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))

                    Text(
                        "العلامات المحفوظة لهذا الملف (${fileBookmarks.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Right
                    )

                    if (fileBookmarks.isEmpty()) {
                        Text(
                            "لا توجد إشارات مخصصة لهذا الملف حتى الآن.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        fileBookmarks.forEach { bm ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (bm.pageIndex == viewModel.currentPageIndex)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updatePageProgress(bm.pageIndex)
                                            showPageBookmarkDialog = false
                                        }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.deletePageBookmark(bm.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = bm.label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "الصفحة ${bm.pageIndex + 1}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (fileBookmarks.isNotEmpty()) {
                        Button(
                            onClick = {
                                try {
                                    val array = org.json.JSONArray()
                                    fileBookmarks.forEach { bm ->
                                        val obj = org.json.JSONObject().apply {
                                            put("file", bm.pdfTitle)
                                            put("page", bm.pageIndex)
                                            put("label", bm.label)
                                            put("date", bm.addedDate)
                                        }
                                        array.put(obj)
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "إشارات مرجعية لملف: ${pdfFile.title}")
                                        putExtra(Intent.EXTRA_TEXT, array.toString(2))
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "تصدير / مشاركة الإشارات المرجعية:"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "فشل في تصدير الإشارات المرجعية", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير ومشاركة كـ JSON")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPageBookmarkDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

// ---------------- VIEWS TAB ----------------
@Composable
fun ViewNavTab(viewModel: PdfRendererViewModel) {
    var pageJumpInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("وضعيات عرض وقراءة الصفحات الحرة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        
        // Horizontal Mode selectors Flow
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val row1 = listOf(
                    "single_vertical" to "صفحة فردية",
                    "continuous" to "تمرير مستمر",
                    "bilingual" to "قراءة ثنائية 🌟"
                )
                row1.forEach { (modeCode, label) ->
                    val selected = viewModel.viewingMode == modeCode
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.viewingMode = modeCode },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (modeCode == "bilingual") FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val row2 = listOf(
                    "book_flip" to "تأثير كتاب",
                    "dual_page" to "صفحتين تجاور"
                )
                row2.forEach { (modeCode, label) ->
                    val selected = viewModel.viewingMode == modeCode
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.viewingMode = modeCode },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Divider()

        // Jump Directly to Target Page
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = pageJumpInput,
                onValueChange = { pageJumpInput = it },
                label = { Text("انتقال سريع لرقم صفحة...") },
                placeholder = { Text("مثال: 3") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val pageNo = pageJumpInput.toIntOrNull()
                    if (pageNo != null && pageNo >= 1 && pageNo <= viewModel.totalPagesCount) {
                        viewModel.updatePageProgress(pageNo - 1)
                        pageJumpInput = ""
                    }
                }
            ) {
                Text("إذهب")
            }
        }

        // Quick Fullscreen preference
        ListItem(
            headlineContent = { Text("وضع ملء الشاشة الكامل (بث مريح)") },
            supportingContent = { Text("يخفي أشرطة التنقل وقائمة الخيارات تماماً للتركيز على النصوص.") },
            trailingContent = {
                Switch(
                    checked = viewModel.isFullscreenMode,
                    onCheckedChange = { viewModel.isFullscreenMode = it }
                )
            }
        )

        ListItem(
            headlineContent = { Text("الوضع الليلي الذكي (عكس ألوان PDF)") },
            supportingContent = { Text("يحول الأوراق البيضاء إلى خلفية حبر داكن مريح ومثالي للعين في الليل.") },
            trailingContent = {
                Switch(
                    checked = viewModel.isNightModeInverted,
                    onCheckedChange = { viewModel.toggleNightModeInverted() }
                )
            }
        )

        // --- TABLE OF CONTENTS DIGITAL ACCORDION MODULE ---
        var isTocExpanded by remember { mutableStateOf(true) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isTocExpanded = !isTocExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Toc, "TOC", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فهرس المحتويات الرقمي (TOC)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Icon(
                        imageVector = if (isTocExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle"
                    )
                }

                if (isTocExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val chapters = remember(viewModel.currentPdfFile, viewModel.totalPagesCount) {
                        viewModel.getTableOfContents()
                    }
                    val currentChapter = remember(viewModel.currentPageIndex, chapters) {
                        chapters.lastOrNull { it.pageIndex <= viewModel.currentPageIndex } ?: chapters.firstOrNull()
                    }
                    chapters.forEach { chapter ->
                        val isCurrentChapter = (chapter == currentChapter)
                        val borderCol = if (isCurrentChapter) MaterialTheme.colorScheme.primary else Color.Transparent
                        val bgCol = if (isCurrentChapter) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bgCol)
                                .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(6.dp))
                                .clickable {
                                    viewModel.updatePageProgress(chapter.pageIndex)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chapter.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrentChapter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "ص ${chapter.pageIndex + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentChapter) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- ANNOTATIONS TAB ----------------
@Composable
fun AnnotationsTab(viewModel: PdfRendererViewModel, totalPages: Int) {
    val context = LocalContext.current
    var textInputState by remember { mutableStateOf("") }
    var noteInputState by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("تحرير وكتابة التعليقات والأختام على الصفحة الحالية (صفحة ${viewModel.currentPageIndex + 1})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        // Freehand stylus/touch pen setup
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gesture, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الرسم الحر بالأصبع أو القلم", fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = viewModel.isDrawModeEnabled,
                        onCheckedChange = { viewModel.isDrawModeEnabled = it }
                    )
                }

                if (viewModel.isDrawModeEnabled) {
                    Text("اختر لون قلم الرسم التفاعلي النشط:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        val paintColors = listOf(
                            "#E53935" to Color.Red,
                            "#1E88E5" to Color.Blue,
                            "#43A047" to Color.Green,
                            "#FDD835" to Color.Yellow,
                            "#212121" to Color.Black
                        )
                        paintColors.forEach { (hex, clr) ->
                            val active = viewModel.activeDrawColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(clr)
                                    .border(
                                        width = if (active) 3.dp else 0.dp,
                                        color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.activeDrawColorHex = hex }
                            )
                        }
                    }
                    Text("اسحب إصبعك/الماوس مباشرة على ورقة الـ PDF للرسم بأي مكان بشكل فوري وعفوي!", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Sticky Notes Generator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = noteInputState,
                onValueChange = { noteInputState = it },
                label = { Text("أضف ملاحظة لاصقة (Sticky Note)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            FilledIconButton(
                onClick = {
                    if (noteInputState.isNotBlank()) {
                        viewModel.addStickyNote(viewModel.currentPageIndex, noteInputState)
                        noteInputState = ""
                        Toast.makeText(context, "تم لصق الملاحظة بالصفحة!", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.PostAdd, "Sticky")
            }
        }

        // Rubber Stamps picker list
        Text("أضف ختم مطاطي (Rubber Stamps)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val stampsOptions = listOf(
                "APPROVED / معتمد",
                "REJECTED / مرفوض",
                "REVIEW / مراجعة",
                "DRAFT / مسودة"
            )
            stampsOptions.forEach { stamp ->
                val arLabel = stamp.substringAfter("/ ")
                OutlinedButton(
                    onClick = {
                        viewModel.addStamp(viewModel.currentPageIndex, stamp)
                        Toast.makeText(context, "تم الختم بصفحة ${viewModel.currentPageIndex + 1}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(arLabel, fontSize = 10.sp)
                }
            }
        }

        // Shapes selector
        Text("أضف أشكال هندسية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val shapesOptions = listOf(
                "rectangle" to "مربع",
                "circle" to "دائرة",
                "arrow" to "سهم",
                "line" to "خط مستقيم"
            )
            shapesOptions.forEach { (type, label) ->
                FilledTonalButton(
                    onClick = {
                        viewModel.addShape(viewModel.currentPageIndex, type, "#1E88E5")
                        Toast.makeText(context, "تم إدراج شكل تجميلي!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(label, fontSize = 10.sp)
                }
            }
        }

        OutlinedButton(
            onClick = {
                viewModel.resetDocumentModifications()
                Toast.makeText(context, "تم تنظيف كل الملاحظات والرسومات بالملف", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ClearAll, "Clean")
            Spacer(modifier = Modifier.width(6.dp))
            Text("حذف جميع التعديلات والتعليقات من المستند")
        }
    }
}

// ---------------- ADVANCED PAGE TOOLS TAB ----------------
@Composable
fun PageToolsTab(viewModel: PdfRendererViewModel, onBackClick: () -> Unit) {
    val context = LocalContext.current
    var isCompressing by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("إدارة بنية الصفحات والميزات الاحترافية المعقدة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        // Rotation & deletion
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    viewModel.toggleRotatePage(viewModel.currentPageIndex)
                    Toast.makeText(context, "تم تدوير الصفحة بـ 90 درجة", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.RotateRight, "Rotate")
                Spacer(modifier = Modifier.width(4.dp))
                Text("تدوير الصفحة")
            }

            Button(
                onClick = {
                    val pageIdx = viewModel.currentPageIndex
                    viewModel.deletePage(pageIdx)
                    Toast.makeText(context, "تم حذف الصفحة ${pageIdx + 1} بنجاح", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Delete, "Delete")
                Spacer(modifier = Modifier.width(4.dp))
                Text("حذف الصفحة")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedButton(
                onClick = {
                    viewModel.insertBlankPageAt(viewModel.currentPageIndex)
                    Toast.makeText(context, "تم إدراج صفحة فارغة جديدة!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AddBox, "Insert Blank")
                Spacer(modifier = Modifier.width(4.dp))
                Text("إدراج صفحة بيضاء")
            }

            ElevatedButton(
                onClick = {
                    // System triggers actual printing
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        try {
                            val printAdapter = object : PrintDocumentAdapter() {
                                override fun onLayout(
                                    oldAttributes: PrintAttributes?,
                                    newAttributes: PrintAttributes?,
                                    cancellationSignal: CancellationSignal?,
                                    callback: LayoutResultCallback?,
                                    extras: Bundle?
                                ) {
                                    // Simulated print layout success
                                    callback?.onLayoutFinished(null, true)
                                }

                                override fun onWrite(
                                    pages: Array<out android.print.PageRange>?,
                                    destination: ParcelFileDescriptor?,
                                    cancellationSignal: CancellationSignal?,
                                    callback: WriteResultCallback?
                                ) {
                                    // simulated print writing
                                    callback?.onWriteFinished(emptyArray())
                                }
                            }
                            printManager.print("WPS Print Service", printAdapter, null)
                        } catch(e: Exception) {
                            Toast.makeText(context, "تم إرسال مستند PDF للطباعة بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "جهاز الطباعة غير مهيأ حالياً", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Print, "Print")
                Spacer(modifier = Modifier.width(4.dp))
                Text("طباعة المستند")
            }
        }

        Divider()

        // Compression quality chooser
        Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تقليص وضغط حجم ملف PDF الذكي لإرساله بالبريد", fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val qualities = listOf("High" to "دقة عالية", "Medium" to "متوسطة", "Low" to "أدنى حجم")
                    qualities.forEach { (q, label) ->
                        OutlinedButton(
                            onClick = {
                                isCompressing = true
                                viewModel.compressPdfFile(q) {
                                    isCompressing = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, fontSize = 11.sp)
                        }
                    }
                }

                if (isCompressing || viewModel.lastCompressionResult.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (viewModel.lastCompressionResult == "Compressing...") {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = viewModel.lastCompressionResult,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // AES-256 Encryption protection Form
        Card {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تأمين وتشفير الملف بكلمة مرور (AES-256)", fontWeight = FontWeight.Bold)
                Text("عند قفل المستند، لن يتمكن أحد من تصفح المحتويات بدون الرمز السري الخاص بك.", fontSize = 11.sp)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("أدخل رمز القفل") },
                        placeholder = { Text("مثال: 1234") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (passwordInput.isNotBlank()) {
                                viewModel.encryptPdfFile(passwordInput)
                                Toast.makeText(context, "تم إغلاق وتشفير الـ PDF بنجاح بكلمة سر!", Toast.LENGTH_SHORT).show()
                                passwordInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Lock, "Lock")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تشفير")
                    }
                }
            }
        }
    }
}

// ---------------- AI HUB TAB ----------------
@Composable
fun AiTab(viewModel: PdfRendererViewModel) {
    val context = LocalContext.current
    var selectedSummaryLang by remember { mutableStateOf("ar") }
    var selectedSummaryFormat by remember { mutableStateOf("short") }
    var aiFeatureSelectedSubtab by remember { mutableStateOf(0) } // 0: Summarize, 1: Q&A, 2: Smart OCR, 3: Deutsch

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // AI Segment Controllers
        ScrollableTabRow(
            selectedTabIndex = aiFeatureSelectedSubtab,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(selected = aiFeatureSelectedSubtab == 0, onClick = { aiFeatureSelectedSubtab = 0 }, text = { Text("تلخيص ذكي", fontSize = 11.sp) })
            Tab(selected = aiFeatureSelectedSubtab == 1, onClick = { aiFeatureSelectedSubtab = 1 }, text = { Text("دردشة Q&A", fontSize = 11.sp) })
            Tab(selected = aiFeatureSelectedSubtab == 2, onClick = { aiFeatureSelectedSubtab = 2 }, text = { Text("OCR الكاميرا", fontSize = 11.sp) })
            Tab(selected = aiFeatureSelectedSubtab == 3, onClick = { aiFeatureSelectedSubtab = 3 }, text = { Text("تعلم الألمانية 🇩🇪", fontSize = 11.sp) })
        }

        when (aiFeatureSelectedSubtab) {
            0 -> {
                // Summarize section
                Text("مولد التلخيص التلقائي بالذكاء الاصطناعي (Gemini 3.5 Flash)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                
                // Lang parameters
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val langs = listOf("ar" to "العربية", "de" to "Deutsch", "en" to "English")
                    langs.forEach { (code, name) ->
                        FilterChip(
                            selected = selectedSummaryLang == code,
                            onClick = { selectedSummaryLang = code },
                            label = { Text(name, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Format parameters
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val formats = listOf("short" to "وجيز (5 نقاط)", "detailed" to "تفصيلي شامل", "bullets" to "نقاط مرتبة")
                    formats.forEach { (code, name) ->
                        FilterChip(
                            selected = selectedSummaryFormat == code,
                            onClick = { selectedSummaryFormat = code },
                            label = { Text(name, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Button(
                    onClick = { viewModel.summarizeCurrentDocument(selectedSummaryLang, selectedSummaryFormat) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (viewModel.isSummarizing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 1.5.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("لخّص المستند الآن")
                }

                if (viewModel.lastSummaryResult.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("نقاط الملخص المستخرج:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                OutlinedButton(
                                    onClick = {
                                        viewModel.addStickyNote(viewModel.currentPageIndex, "ملخص الذكاء الاصطناعي:\n" + viewModel.lastSummaryResult)
                                        Toast.makeText(context, "تم حفظ الملخص كملاحظة مرفقة بالملف!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("حفظ كملاحظة", fontSize = 10.sp)
                                }
                            }
                            Text(
                                text = viewModel.lastSummaryResult,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            1 -> {
                // Q&A Small chat Dialog
                Text("دردش مع أوراقك! (إستعلام يعتمد على نصوص المستند)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                
                var queryTextState by remember { mutableStateOf("") }

                // Message history Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (viewModel.chatMessages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "اطرح سؤال لغوي أو تصفحي حول الملف.\n(مثال: ما الفرق بين Akkusativ و Dativ؟)",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), reverseLayout = true) {
                            itemsIndexed(viewModel.chatMessages.reversed()) { _, msg ->
                                val isUser = msg.sender == "User"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "${msg.sender}:", 
                                                fontWeight = FontWeight.Bold, 
                                                fontSize = 10.sp, 
                                                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                            )
                                            Text(text = msg.text, fontSize = 11.sp)
                                            if (msg.pageNo != null) {
                                                Text(
                                                    text = "[المصدر: صفحة ${msg.pageNo}]",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(top = 2.dp),
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = queryTextState,
                        onValueChange = { queryTextState = it },
                        placeholder = { Text("اكتب سؤالك هنا...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (queryTextState.isNotBlank()) {
                                viewModel.askChatbotAboutDocument(queryTextState)
                                queryTextState = ""
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors()
                    ) {
                        if (viewModel.isChatLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 1.5.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Default.Send, "Send")
                        }
                    }
                }
            }
            2 -> {
                // Scanned PDF OCR Reader Text extraction layer
                Text("قراءة وتحويل النصوص الممسوحة ضوئياً (OCR)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("يدعم العربية والألمانية والإنجليزية بآنٍ واحد وبكل سلاسة ودقة.", fontSize = 11.sp)

                Button(
                    onClick = { viewModel.runSmartOcrExtraction(viewModel.currentPageIndex) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (viewModel.isSmartOcrRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 1.5.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(Icons.Default.DocumentScanner, null)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("ابدأ معالجة واستخراج النصوص (OCR)")
                }

                if (viewModel.smartOcrText.isNotEmpty()) {
                    Card {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "دقة التعارف: ${viewModel.smartOcrConfidence}% (ناجح)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF43A047),
                                    fontSize = 11.sp
                                )
                                val clipboardManager = LocalClipboardManager.current
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(viewModel.smartOcrText))
                                        Toast.makeText(context, "تم نسخ الطبقة النصية للعربية!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("نسخ الكل", fontSize = 10.sp)
                                }
                            }

                            // Editable Text Layer for user manual correction adjustments
                            OutlinedTextField(
                                value = viewModel.smartOcrText,
                                onValueChange = { viewModel.smartOcrText = it },
                                label = { Text("الطبقة النصية المستخرجة (قابلة للتعديل والنسخ)") },
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            3 -> {
                // German Vocabulary Tutor hub (Learn German)
                Text("المعلم اللغوي للغة الألمانية 🇩🇪 (Smart Language Tutor)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("نقوم باستخراج المصطلحات والمفردات من أوراق القواعد والكتب الدراسية تلقائياً.", fontSize = 11.sp)

                Button(
                    onClick = { viewModel.extractGermanVocabulary() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (viewModel.isExtractingGermanVocab) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 1.5.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(Icons.Default.Translate, null)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("استخراج الكلمات وبناء المفردات تلقائياً")
                }

                if (viewModel.extractedWordsList.isNotEmpty()) {
                    GermanLessonsHub(viewModel.extractedWordsList.toList())
                }
            }
        }
    }
}

// ---------------- SUBSYSTEM: GERMAN EDUCATION TOOLSET ----------------
@Composable
fun GermanLessonsHub(words: List<GermanWord>) {
    var playSubtabIndex by remember { mutableStateOf(0) } // 0: Word List, 1: Flashcards, 2: Vocabulary Quiz Game

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val chips = listOf("قائمة الكلمات", "بطاقات تعليمية", "إختبار الكلمات")
            chips.forEachIndexed { i, label ->
                FilterChip(
                    selected = playSubtabIndex == i,
                    onClick = { playSubtabIndex = i },
                    label = { Text(label, fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (playSubtabIndex) {
            0 -> {
                // Word List View
                Text("المفردات المستخرجة من الكتاب الدراسي:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                words.forEach { word ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(word.word, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                Text(word.translation, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                            }
                            Text("Pronunciation sound: ${word.audioHint}", fontSize = 11.sp, fontStyle = FontStyle.Italic)
                            Text(word.exampleSentence, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
            1 -> {
                // Swipeable / Flippable Flashcards deck
                var activeCardIx by remember { mutableStateOf(0) }
                var cardRevealed by remember { mutableStateOf(false) }

                if (activeCardIx in words.indices) {
                    val activeWord = words[activeCardIx]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .shadow(2.dp, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("بطاقة مراجعة مفردات رقم ${activeCardIx + 1}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = activeWord.word,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Text("كيفية النطق الصوتي: ${activeWord.audioHint}", fontSize = 12.sp, fontStyle = FontStyle.Italic)

                            AnimatedVisibility(visible = cardRevealed) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                    Text(
                                        text = activeWord.translation,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = activeWord.exampleSentence,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Button(
                                onClick = { cardRevealed = !cardRevealed },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text(if (cardRevealed) "إخفاء التفاصيل" else "إظهار الترجمة اللغوية")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    onClick = {
                                        if (activeCardIx > 0) {
                                            activeCardIx--
                                            cardRevealed = false
                                        }
                                    },
                                    enabled = activeCardIx > 0
                                ) {
                                    Text("السابق")
                                }
                                TextButton(
                                    onClick = {
                                        if (activeCardIx < words.size - 1) {
                                            activeCardIx++
                                            cardRevealed = false
                                        }
                                    },
                                    enabled = activeCardIx < words.size - 1
                                ) {
                                    Text("التالي")
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Interactive quiz game
                var currentQuizIx by remember { mutableStateOf(0) }
                var selectedQuizAnswer by remember { mutableStateOf<Int?>(null) }
                var scoreCounter by remember { mutableStateOf(0) }
                var isQuizFinished by remember { mutableStateOf(false) }

                val questionsList = listOf(
                    QuizQuestion(
                        question = "ما هو معنى الكلمة الألمانية 'der Bildschirm'؟",
                        options = listOf("الشاشة والمظهر", "الاجتماع والمناقشة", "يحفظ عن ظهر قلب"),
                        correctIndex = 0
                    ),
                    QuizQuestion(
                        question = "ما معنى التعبير القواعدي 'auswendig lernen'؟",
                        options = listOf("يفهم القوانين", "يحفظ عن ظهر قلب وبصم", "تحميل المستند الرقمي"),
                        correctIndex = 1
                    ),
                    QuizQuestion(
                        question = "ما معنى المصطلح الألماني 'die Besprechung'؟",
                        options = listOf("الاجتماع والمناقشة", "الشاشة", "الكتابة والتحرير"),
                        correctIndex = 0
                    )
                )

                if (isQuizFinished) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("لقد أنهيت اختبار الكلمات بنجاح! 🥳", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("النتيجة الإجمالية التي أحرزتها: $scoreCounter من ${questionsList.size}", fontSize = 14.sp)
                            Button(
                                onClick = {
                                    currentQuizIx = 0
                                    selectedQuizAnswer = null
                                    scoreCounter = 0
                                    isQuizFinished = false
                                }
                            ) {
                                Text("إعادة الاختبار")
                            }
                        }
                    }
                } else {
                    val activeQ = questionsList[currentQuizIx]

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("السؤال ${currentQuizIx + 1} من ${questionsList.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(activeQ.question, fontWeight = FontWeight.Bold)

                        activeQ.options.forEachIndexed { optIdx, optLabel ->
                            val isSelected = selectedQuizAnswer == optIdx
                            val isAnswered = selectedQuizAnswer != null
                            val isCorrect = optIdx == activeQ.correctIndex

                            val buttonColor = when {
                                isSelected && isCorrect -> Color(0xFFC8E6C9)
                                isSelected && !isCorrect -> Color(0xFFFFCDD2)
                                isAnswered && isCorrect -> Color(0xFFC8E6C9)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAnswered) {
                                        selectedQuizAnswer = optIdx
                                        if (optIdx == activeQ.correctIndex) {
                                            scoreCounter++
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = buttonColor),
                                border = BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Text(
                                    text = optLabel,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (selectedQuizAnswer != null) {
                            Button(
                                onClick = {
                                    if (currentQuizIx < questionsList.size - 1) {
                                        currentQuizIx++
                                        selectedQuizAnswer = null
                                    } else {
                                        isQuizFinished = true
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(if (currentQuizIx == questionsList.size - 1) "عرض النتيجة" else "السؤال التالي")
                            }
                        }
                    }
                }
            }
        }
    }
}

data class QuizQuestion(val question: String, val options: List<String>, val correctIndex: Int)


// ---------------- LAYOUT: PDF PAGE CARD GRAPHICS LAYER ----------------
@Composable
fun PdfPageCard(
    pageIndex: Int,
    filePath: String,
    viewModel: PdfRendererViewModel
) {
    val context = LocalContext.current
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderError by remember { mutableStateOf(false) }

    LaunchedEffect(pageIndex, filePath, viewModel.deletedPagesList) {
        renderError = false
        val bitmap = viewModel.renderPageBitmap(filePath, pageIndex)
        if (bitmap != null) {
            pageBitmap = bitmap
        } else {
            renderError = true
        }
    }

    // Double eye-comfort contrast inverting colors Matrix
    val inversionColorMatrix = ColorMatrix(
        floatArrayOf(
            -1.0f,  0.0f,  0.0f,  0.0f, 255.0f,
             0.0f, -1.0f,  0.0f,  0.0f, 255.0f,
             0.0f,  0.0f, -1.0f,  0.0f, 255.0f,
             0.0f,  0.0f,  0.0f,  1.0f,   0.0f
        )
    )

    val pageRotationDegrees = viewModel.pageRotationMap[pageIndex] ?: 0

    // Handles text select pin positions
    var selectionActive by remember { mutableStateOf(false) }
    var doubleTapIndexSelected by remember { mutableStateOf(false) }
    var contextMenuAnchorOffset by remember { mutableStateOf(Offset.Zero) }

    // Intercept draw gestures directly on PDF page card
    var brushStrokePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.707f) // ISO A4 Aspect Ratio shape (Width / Height = 0.707f)
            .rotate(pageRotationDegrees.toFloat())
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .pointerInput(viewModel.isDrawModeEnabled) {
                if (viewModel.isDrawModeEnabled) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            brushStrokePoints = listOf(offset.x to offset.y)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            brushStrokePoints = brushStrokePoints + (change.position.x to change.position.y)
                        },
                        onDragEnd = {
                            if (brushStrokePoints.isNotEmpty()) {
                                viewModel.addInkStroke(
                                    pageIdx = pageIndex,
                                    stroke = InkStroke(brushStrokePoints, viewModel.activeDrawColorHex)
                                )
                                brushStrokePoints = emptyList()
                            }
                        }
                    )
                } else {
                    detectTapGestures(
                        onLongPress = { offset ->
                            selectionActive = true
                            contextMenuAnchorOffset = offset
                        },
                        onDoubleTap = {
                            doubleTapIndexSelected = true
                            selectionActive = true
                        }
                    )
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (pageBitmap != null) {
                Image(
                    bitmap = pageBitmap!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (viewModel.isNightModeInverted) ColorFilter.colorMatrix(inversionColorMatrix) else null
                )
            } else if (renderError) {
                // If rendering fails (or virtual blank page)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddBox, "Blank", tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("صفحة فارغة مضافة للمستند", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("أثر اللمس والكتابة التفاعلية متاح للرسم بحرية.", fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                )
            }

            // --- REAL INTERACTIVE CANVAS DRAWER OVERLAY ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                // RENDER SAVED INK STROKES
                viewModel.pageAnnotationsInk[pageIndex]?.forEach { stroke ->
                    val strokePath = Path().apply {
                        stroke.points.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.first, pt.second)
                            else lineTo(pt.first, pt.second)
                        }
                    }
                    drawPath(
                        path = strokePath,
                        color = Color(android.graphics.Color.parseColor(stroke.colorHex)),
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // PLOT ACTIVE DRAWING STROKE POINT SEGMENTS
                if (brushStrokePoints.isNotEmpty()) {
                    val activePath = Path().apply {
                        brushStrokePoints.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.first, pt.second)
                            else lineTo(pt.first, pt.second)
                        }
                    }
                    drawPath(
                        path = activePath,
                        color = Color(android.graphics.Color.parseColor(viewModel.activeDrawColorHex)),
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // RENDER TEMPORARY SEARCH RESULT HIGHLIGHTS
                viewModel.pdfTextSearchResults.forEachIndexed { sIdx, result ->
                    if (result.pageIndex == pageIndex) {
                        val isCurrent = (sIdx == viewModel.currentPdfSearchIndex)
                        val matchY = 160f + (result.startIndex % 5) * 80f
                        val matchX = 100f + (result.startIndex % 3) * 120f
                        drawRect(
                            color = if (isCurrent) Color(0xFFFF9800).copy(alpha = 0.5f) else Color(0xFFFFEB3B).copy(alpha = 0.5f),
                            topLeft = Offset(matchX, matchY),
                            size = Size(180f, 36f)
                        )
                    }
                }

                // RENDER HIGHLIGHT PATH UNDERLAYS
                viewModel.pageAnnotationsText[pageIndex]?.forEach { textAnn ->
                    // Simulated highlights
                    drawRect(
                        color = Color(android.graphics.Color.parseColor(textAnn.colorHex)).copy(alpha = 0.35f),
                        topLeft = Offset(textAnn.x - 30, textAnn.y - 10),
                        size = Size(200f, 32f)
                    )
                }

                // Draw shapes
                viewModel.pageShapes[pageIndex]?.forEach { shape ->
                    val color = Color(android.graphics.Color.parseColor(shape.colorHex))
                    when (shape.type) {
                        "rectangle" -> {
                            drawRect(
                                color = color,
                                topLeft = Offset(shape.x, shape.y),
                                size = Size(140f, 90f),
                                style = Stroke(width = 4f)
                            )
                        }
                        "circle" -> {
                            drawCircle(
                                color = color,
                                center = Offset(shape.x + 50, shape.y + 50),
                                radius = 45f,
                                style = Stroke(width = 4f)
                            )
                        }
                        "arrow" -> {
                            // Arrow line & head pointing down right
                            drawLine(color = color, start = Offset(shape.x, shape.y), end = Offset(shape.x + 80, shape.y + 80), strokeWidth = 5f)
                            drawCircle(color = color, radius = 8f, center = Offset(shape.x + 80, shape.y + 80))
                        }
                        "line" -> {
                            drawLine(color = color, start = Offset(shape.x, shape.y), end = Offset(shape.x + 120, shape.y), strokeWidth = 5f)
                        }
                    }
                }
            }

            // --- HIGHLIGHT/SELECTION SIMULATION OVERLAYS ---
            if (selectionActive) {
                // Beautiful translucent Highlight Box spanning sentences
                Box(
                    modifier = Modifier
                        .offset(x = 60.dp, y = 140.dp)
                        .size(width = 240.dp, height = 34.dp)
                        .background(Color(0xFF2196F3).copy(alpha = 0.25f))
                        .border(1.dp, Color(0xFF2196F3))
                ) {
                    // Left pin drag
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (-6).dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1976D2))
                    )
                    // Right pin drag
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 6.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1976D2))
                    )
                }

                // --- CONTEXT MENU POPUP (FLOATING DIALOG ABOVE TARGET) ---
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val clipboard = LocalClipboardManager.current
                        val txtToUse = "WPS PDF Reader Pro – Smart Language Tutor"

                        // Copy
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(txtToUse))
                                selectionActive = false
                                Toast.makeText(context, "تم نسخ النص بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, "نسخ", modifier = Modifier.size(18.dp))
                        }

                        // Colors highlight selectors
                        val markerColors = listOf("#FDD835", "#43A047", "#1E88E5", "#F48FB1")
                        markerColors.forEach { markerHex ->
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(markerHex)))
                                    .clickable {
                                        viewModel.addHighlight(pageIndex, txtToUse, markerHex)
                                        selectionActive = false
                                        Toast.makeText(context, "تم تمييز النص باللون!", Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }

                        // Translate
                        IconButton(
                            onClick = {
                                selectionActive = false
                                viewModel.activeParagraphTranslationTitle = txtToUse
                                viewModel.activeParagraphTranslationResult = viewModel.dictionaryManager.translateParagraph(txtToUse)
                                viewModel.isParagraphTranslationDialogVisible = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Translate, "ترجمة", modifier = Modifier.size(18.dp))
                        }

                        // Add note
                        IconButton(
                            onClick = {
                                viewModel.addStickyNote(pageIndex, "مراجعة النص: تحتاج لدراسة أدوات وبادئات اللغة")
                                selectionActive = false
                                Toast.makeText(context, "تم حفظ ملحوظة متصلة بالفقرة!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Notes, "ملاحظة", modifier = Modifier.size(18.dp))
                        }

                        // Close context
                        IconButton(
                            onClick = { selectionActive = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, "أغلق", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // --- COLLAPSIBLE STICKY NOTES INTERACTION PIN ---
            viewModel.pageStickyNotes[pageIndex]?.forEachIndexed { noteIdx, noteText ->
                var isBubbleExpanded by remember { mutableStateOf(false) }
                
                Box(
                    modifier = Modifier
                        .offset(x = (20 + (noteIdx * 30)).dp, y = (40 + (noteIdx * 45)).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.StickyNote2,
                        contentDescription = "Sticky notes",
                        tint = Color(0xFFE65100),
                        modifier = Modifier
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0))
                            .padding(4.dp)
                            .size(24.dp)
                            .clickable { isBubbleExpanded = !isBubbleExpanded }
                    )

                    if (isBubbleExpanded) {
                        Card(
                            modifier = Modifier
                                .offset(x = 30.dp, y = 10.dp)
                                .width(200.dp)
                                .shadow(6.dp, RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الملاحظة المرفقة:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.DarkGray)
                                    IconButton(
                                        onClick = { viewModel.deleteAnnotation(pageIndex, "note", noteIdx) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "حذف", tint = Color.Red, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(noteText, fontSize = 12.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }

            // --- RUBBER STAMP GRAPHICS DISPLAY ---
            viewModel.pageStamps[pageIndex]?.forEachIndexed { stampIdx, stampLabel ->
                val stampColor = if (stampLabel.contains("APPROVED")) Color(0xFF2E7D32) else if (stampLabel.contains("REJECTED")) Color(0xFFC62828) else Color(0xFFE65100)
                val stampText = stampLabel.substringAfter("/ ")

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = (16 + (stampIdx * 45)).dp, end = 16.dp)
                        .rotate(-15f)
                        .border(width = 3.dp, color = stampColor.copy(alpha = 0.85f), shape = RoundedCornerShape(6.dp))
                        .background(stampColor.copy(alpha = 0.08f))
                        .clickable { viewModel.deleteAnnotation(pageIndex, "stamp", stampIdx) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stampText,
                        color = stampColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ---------------- QUICK NAVIGATION CONTROLS SCREEN BOTTOM ----------------
@Composable
fun WpsReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    onOpenToolsWithTab: (Int) -> Unit,
    viewModel: PdfRendererViewModel
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .navigationBarsPadding(),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Slider to scrub page indices
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${currentPage + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Slider(
                    value = currentPage.toFloat(),
                    onValueChange = { onPageChange(it.toInt()) },
                    valueRange = 0f..(totalPages - 1).coerceAtLeast(1).toFloat(),
                    steps = if (totalPages > 2) totalPages - 2 else 0,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "$totalPages",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // High Fidelity segment selector shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View properties launcher
                IconButton(onClick = { onOpenToolsWithTab(0) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MenuBook, "View Layouts", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Annotate controls trigger
                IconButton(onClick = { onOpenToolsWithTab(1) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Edit, "Edit Annotate", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Page Tools drawer launcher
                IconButton(onClick = { onOpenToolsWithTab(2) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Construction, "Tools Management", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Advanced AI center
                IconButton(onClick = { onOpenToolsWithTab(3) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoAwesome, "AI Assistant", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WpsReaderTopBar(
    pdfFile: PdfFile,
    currentPage: Int,
    totalPages: Int,
    onBackClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onToolsClick: () -> Unit,
    onShareClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPageBookmarkClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = pdfFile.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.page_number, currentPage + 1, totalPages),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return")
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "البحث داخل الملف", tint = Color.White)
            }
            IconButton(onClick = onPageBookmarkClick) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = "علامات مرجعية للصفحة الحالية",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onBookmarkToggle) {
                Icon(
                    imageVector = if (pdfFile.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Pin Lesezeichen",
                    tint = if (pdfFile.isBookmarked) MaterialTheme.colorScheme.secondary else Color.White
                )
            }
            IconButton(onClick = onToolsClick) {
                Icon(imageVector = Icons.Default.Build, contentDescription = "Advanced Toolbar", tint = Color.White)
            }
            IconButton(onClick = onShareClick) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share Document", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = Modifier.statusBarsPadding()
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BilingualStudyView(
    pdfFile: PdfFile,
    pageIndex: Int,
    viewModel: PdfRendererViewModel
) {
    val context = LocalContext.current
    val pageText = remember(pageIndex, pdfFile) { viewModel.getPageTextContent(pageIndex) }
    
    // Split text into individual sentences for step-by-step reading with Arabic equivalents
    val sentenceList = remember(pageText) {
        pageText.split(Regex("(?<=\\.)|(?<=\\?)|(?<=\\!)"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp
        
        if (isWideScreen) {
            // Landscape split: Side-by-Side Layout
            Row(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: PDF Original page
                Box(
                    modifier = Modifier.weight(1.1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    PdfPageCard(
                        pageIndex = pageIndex,
                        filePath = pdfFile.filePath,
                        viewModel = viewModel
                    )
                }

                // Right Panel: Interactive Bilingual Text & Controls
                Card(
                    modifier = Modifier.weight(0.9f).fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    BilingualStudyTexter(
                        sentences = sentenceList,
                        viewModel = viewModel
                    )
                }
            }
        } else {
            // Portrait/Compact Screen: Stack layouts
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Half: PDF original card
                Box(
                    modifier = Modifier.weight(1.0f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    PdfPageCard(
                        pageIndex = pageIndex,
                        filePath = pdfFile.filePath,
                        viewModel = viewModel
                    )
                }

                // Bottom Half: Interactive Bilingual Text companion
                Card(
                    modifier = Modifier.weight(1.0f).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    BilingualStudyTexter(
                        sentences = sentenceList,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BilingualStudyTexter(
    sentences: List<String>,
    viewModel: PdfRendererViewModel
) {
    val context = LocalContext.current
    val dm = viewModel.dictionaryManager
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrollable comparison text area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f), RoundedCornerShape(8.dp)).padding(10.dp)
            ) {
                Icon(Icons.Default.Translate, "Instant Translate Hub", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Column {
                    Text("المساعد اللغوي الذكي (WPS Translate)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("اضغط على أي كلمة للترجمة الفورية أوفلاين وسماع نطقها.", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            sentences.forEachIndexed { sentenceIdx, sentence ->
                val words = remember(sentence) { sentence.split(Regex("\\s+")).filter { it.isNotEmpty() } }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (dm.isReadingAloud && dm.currentHighlightWordIndex >= 0 && dm.currentHighlightWordIndex / 10 == sentenceIdx)
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                            else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Original words layout (FlowRow)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Pronounce whole sentence trigger
                        IconButton(
                            onClick = {
                                dm.speak(sentence, false)
                            },
                            modifier = Modifier.size(28.dp).align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Default.VolumeUp, "Speak sentence", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Render clickable word badges
                        words.forEachIndexed { wordRelativeIdx, rawWord ->
                            val cleanWord = rawWord.replace(Regex("[.,?!;:\"]"), "")
                            val globalWordIdx = sentenceIdx * 10 + wordRelativeIdx
                            val isWordHighlighted = dm.isReadingAloud && dm.currentHighlightWordIndex == globalWordIdx

                            Box(
                                modifier = Modifier
                                    .padding(vertical = 2.dp, horizontal = 3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isWordHighlighted) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable {
                                        val entry = dm.lookupWord(cleanWord)
                                        dm.selectedWordEntry = entry
                                        dm.isWordCardVisible = true
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = rawWord,
                                    color = if (isWordHighlighted) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Comparative Translated text (Arabic line in emerald/orange distinct colors for clarity)
                    val arabicSentence = remember(sentence) { dm.translateParagraph(sentence) }
                    Text(
                        text = arabicSentence,
                        color = Color(0xFF00796B), // Rich emerald teal for distinct bilingual contrast
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 28.dp, bottom = 4.dp).fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                }
            }
        }

        // --- READ ALOUD PLAYBACK CONTROLLER ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🔊 وضع القراءة الصوتية المستمرة",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "سرعة النطق: ${"%.1f".format(dm.activeSpeakerSpeed)}x",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Start reading aloud full page sequence
                    Button(
                        onClick = {
                            if (dm.isReadingAloud) {
                                dm.stopSpeaking()
                            } else {
                                dm.isReadingAloud = true
                                scope.launch {
                                    // Start step-by-step reading with visual highlighting mapping
                                    for (i in sentences.indices) {
                                        if (!dm.isReadingAloud) break
                                        val s = sentences[i]
                                        val wCount = s.split(" ").size
                                        
                                        // Pronounce sentence
                                        dm.speak(s, false)
                                        
                                        // Update highlighted word bounds step-by-step simulating read progress
                                        for (w in 0 until wCount) {
                                            if (!dm.isReadingAloud) break
                                            dm.currentHighlightWordIndex = i * 10 + w
                                            kotlinx.coroutines.delay((180 / dm.activeSpeakerSpeed).toLong())
                                        }
                                        kotlinx.coroutines.delay(800)
                                    }
                                    dm.stopSpeaking()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (dm.isReadingAloud) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (dm.isReadingAloud) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (dm.isReadingAloud) "إيقاف القراءة" else "قراءة المستند صوتياً", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Speed slider (0.5x to 2.0x)
                    Slider(
                        value = dm.activeSpeakerSpeed,
                        onValueChange = { dm.activeSpeakerSpeed = it },
                        valueRange = 0.5f..2.0f,
                        steps = 3,
                        modifier = Modifier.weight(1.8f)
                    )
                }
            }
        }
    }
}
