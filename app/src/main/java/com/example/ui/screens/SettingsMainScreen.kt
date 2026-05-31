package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LanguagePack
import com.example.ui.SecurableFile
import com.example.ui.SettingsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    viewModel: SettingsViewModel,
    activity: Activity
) {
    val theme by viewModel.theme.collectAsState()
    val highContrast by viewModel.highContrastEnabled.collectAsState()
    val largeTargets by viewModel.largeTargetsEnabled.collectAsState()
    val talkbackSim by viewModel.talkbackSimEnabled.collectAsState()
    val talkbackSimOutput by viewModel.talkbackSimulationOutput.collectAsState()
    val reduceMotion by viewModel.reduceMotionEnabled.collectAsState()

    // Screen brightness controller injection trigger
    val brightness by viewModel.brightness.collectAsState()
    LaunchedEffect(brightness) {
        viewModel.applyActivitySpecificSettings(activity)
    }

    // Dynamic scale helper based on "largeTargets" accessibility mode
    val elementPadding = if (largeTargets) 20.dp else 12.dp
    val buttonMinHeight = if (largeTargets) 56.dp else 44.dp
    val textBaseSize = if (largeTargets) 18.sp else 14.sp

    var activeCategory by remember { mutableStateOf("display") }

    // Navigation and Categories lists in Arabic
    val categories = listOf(
        CategoryItem("display", "العرض والخطوط", Icons.Default.DisplaySettings),
        CategoryItem("scroll", "وضع التمرير والصفحات", Icons.Default.MenuBook),
        CategoryItem("voice", "الترجمة والنطق الصوتي", Icons.Default.Translate),
        CategoryItem("annotations", "التعليقات والتحشية", Icons.Default.Create),
        CategoryItem("accessibility", "إمكانية الوصول", Icons.Default.AccessibilityNew),
        CategoryItem("privacy", "الخصوصية والأمان الحصين", Icons.Default.Security)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "إعدادات القارئ الشاملة - WPS PRO",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(2.dp)
            )
        },
        bottomBar = {
            // Simulated Talkback floating text reader overlay
            if (talkbackSim && talkbackSimOutput.isNotEmpty()) {
                Surface(
                    color = Color(0xFFFFFF00),
                    contentColor = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = "Hearing",
                            tint = Color.Black
                        )
                        Text(
                            text = talkbackSimOutput,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sidebar for large screens (e.g. tablet mode landscape is standard on our emulator)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth > 650.dp
                if (isWide) {
                    // Split screen layout
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left/Right side navigational sidebar
                        NavigationSidebar(
                            categories = categories,
                            activeCategory = activeCategory,
                            onCategorySelect = {
                                activeCategory = it
                                viewModel.simulateTalkback("تبويب ${categories.find { c -> c.id == it }?.title}")
                            },
                            modifier = Modifier
                                .width(220.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(0.dp)
                                )
                        )
                        
                        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

                        // Active screen settings viewport
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(16.dp)
                        ) {
                            AnimatedContent(
                                targetState = activeCategory,
                                transitionSpec = {
                                    if (reduceMotion) {
                                        fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                                    } else {
                                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                            slideOutVertically { height -> -height } + fadeOut())
                                    }
                                },
                                label = "CategoryTransitionWide"
                            ) { target ->
                                SettingsContentPanel(
                                    categoryId = target,
                                    viewModel = viewModel,
                                    activity = activity,
                                    paddingSize = elementPadding,
                                    minHeight = buttonMinHeight,
                                    textBase = textBaseSize
                                )
                            }
                        }
                        
                        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

                        // Live Document Preview Container (Always present in wide view!)
                        Box(
                            modifier = Modifier
                                .width(280.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                                .padding(16.dp)
                        ) {
                            LiveDocumentPreview(viewModel = viewModel, reduceMotion = reduceMotion)
                        }
                    }
                } else {
                    // Portrait Mobile layout
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Bottom Navigation row or top horizontal tabs
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        ) {
                            items(categories) { cat ->
                                val selected = activeCategory == cat.id
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        activeCategory = cat.id
                                        viewModel.simulateTalkback("تبويب ${cat.title}")
                                    },
                                    label = { Text(cat.title, fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = cat.icon,
                                            contentDescription = cat.title,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("chip_${cat.id}")
                                )
                            }
                        }

                        // Collapsible preview block at top for mobile
                        var showPreviewOnMobile by remember { mutableStateOf(false) }
                        Surface(
                            onClick = { showPreviewOnMobile = !showPreviewOnMobile },
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.FindInPage, contentDescription = "Preview")
                                    Text("معاينة المستند المباشرة (اضغط للعرض/الإخفاء)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Icon(
                                    imageVector = if (showPreviewOnMobile) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand"
                                )
                            }
                        }

                        AnimatedVisibility(visible = showPreviewOnMobile) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                LiveDocumentPreview(viewModel = viewModel, reduceMotion = reduceMotion)
                            }
                        }

                        // Category Panel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            AnimatedContent(
                                targetState = activeCategory,
                                transitionSpec = {
                                    if (reduceMotion) {
                                        fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                                    } else {
                                        fadeIn().togetherWith(fadeOut())
                                    }
                                },
                                label = "CategoryTransitionMobile"
                            ) { target ->
                                SettingsContentPanel(
                                    categoryId = target,
                                    viewModel = viewModel,
                                    activity = activity,
                                    paddingSize = elementPadding,
                                    minHeight = buttonMinHeight,
                                    textBase = textBaseSize
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sidebar Navigation Composable
@Composable
fun NavigationSidebar(
    categories: List<CategoryItem>,
    activeCategory: String,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = activeCategory == cat.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else Color.Transparent
                    )
                    .clickable { onCategorySelect(cat.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("sidebar_${cat.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = cat.icon,
                    contentDescription = cat.title,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = cat.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Render selected panel view
@Composable
fun SettingsContentPanel(
    categoryId: String,
    viewModel: SettingsViewModel,
    activity: Activity,
    paddingSize: Dp,
    minHeight: Dp,
    textBase: androidx.compose.ui.unit.TextUnit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("content_panel"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        when (categoryId) {
            "display" -> {
                item { DisplaySettingsView(viewModel, activity, paddingSize, minHeight, textBase) }
            }
            "scroll" -> {
                item { ScrollSettingsView(viewModel, paddingSize, minHeight, textBase) }
            }
            "voice" -> {
                item { TranslationVoiceSettingsView(viewModel, paddingSize, minHeight, textBase) }
            }
            "annotations" -> {
                item { AnnotationSettingsView(viewModel, paddingSize, minHeight, textBase) }
            }
            "accessibility" -> {
                item { AccessibilitySettingsView(viewModel, activity, paddingSize, minHeight, textBase) }
            }
            "privacy" -> {
                item { PrivacySettingsView(viewModel, activity, paddingSize, minHeight, textBase) }
            }
        }
    }
}

// 1. --- إعدادات العرض (DISPLAY SETTINGS) ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisplaySettingsView(
    viewModel: SettingsViewModel,
    activity: Activity,
    paddingSize: Dp,
    minHeight: Dp,
    textBase: androidx.compose.ui.unit.TextUnit
) {
    val theme by viewModel.theme.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val fontName by viewModel.font.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val lineSpacing by viewModel.lineSpacing.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("إعدادات مظهر القراءة والمحاذاة والخطوط", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            // Theme selection Row
            Text("المظهر العام (ثيم التطبيق):", fontWeight = FontWeight.Bold, fontSize = textBase)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val themes = listOf(
                    Triple("light", "فاتح", Color(0xFFFAF6EE)),
                    Triple("dark", "داكن", Color(0xFF1E1E1E)),
                    Triple("sepia", "سيبيا", Color(0xFFF4ECD8)),
                    Triple("system", "تلقائي", Color(0xFF90A4AE))
                )
                themes.forEach { t ->
                    val isSelected = theme == t.first
                    Button(
                        onClick = { viewModel.updateTheme(t.first, activity) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else t.third,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (t.first == "light" || t.first == "sepia") Color(0xFF424242) else Color.White
                        ),
                        modifier = Modifier
                            .height(minHeight)
                            .border(
                                1.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .testTag("theme_card_${t.first}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = when (t.first) {
                                    "light" -> Icons.Default.LightMode
                                    "dark" -> Icons.Default.DarkMode
                                    "sepia" -> Icons.Default.MenuBook
                                    else -> Icons.Default.SettingsSystemDaydream
                                },
                                contentDescription = t.second,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(t.second, fontSize = 12.sp)
                        }
                    }
                }
            }

            Divider()

            // Brightness slider
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.BrightnessMedium, contentDescription = "Brightness")
                Text("سطوع شاشة التطبيق الداخلي المستقل:", fontWeight = FontWeight.Bold, fontSize = textBase)
                Spacer(modifier = Modifier.weight(1f))
                Text("${(brightness * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = brightness,
                onValueChange = { viewModel.updateBrightness(it, activity) },
                valueRange = 0.1f..1.0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("brightness_slider")
            )

            Divider()

            // Font choice
            Text("الخط الإفتراضي لقراءة المستندات النصوص العربي/الإنجليزي:", fontWeight = FontWeight.Bold, fontSize = textBase)
            val fontsList = listOf("Noto Kufi", "Cairo", "Amiri", "Scheherazade", "Tajawal", "Almarai", "El Messiri", "IBM Plex")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(fontsList) { name ->
                    val isSelected = fontName == name
                    ElevatedAssistChip(
                        onClick = { viewModel.updateFont(name) },
                        label = { Text(name, fontFamily = getFontFamilySimulation(name)) },
                        colors = AssistChipDefaults.elevatedAssistChipColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            androidx.compose.foundation.BorderStroke(1.dp, Color.Transparent)
                        },
                        modifier = Modifier.testTag("font_chip_$name")
                    )
                }
            }

            Divider()

            // Font size slider with dynamic label
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.FormatSize, contentDescription = "Font Size")
                Text("حجم خط القراءة ونموذج النص:", fontWeight = FontWeight.Bold, fontSize = textBase)
                Spacer(modifier = Modifier.weight(1f))
                Text("${fontSize.toInt()}pt", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = fontSize,
                onValueChange = { viewModel.updateFontSize(it) },
                valueRange = 8f..32f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("font_size_slider")
            )

            // Dynamic Font size live preview box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "أبجد هوز حطي كلمن (معاينة فورية لحجم الخط)",
                    fontFamily = getFontFamilySimulation(fontName),
                    fontSize = fontSize.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Divider()

            // Line spacing
            Text("تباعد الأسطر بين نصوص المستندات:", fontWeight = FontWeight.Bold, fontSize = textBase)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val spacings = listOf("tight" to "ضيق (1.0x)", "normal" to "عادي (1.5x)", "wide" to "واسع (2.0x)")
                spacings.forEach { sp ->
                    val isSelected = lineSpacing == sp.first
                    OutlinedButton(
                        onClick = { viewModel.updateLineSpacing(sp.first) },
                        modifier = Modifier
                            .weight(1f)
                            .height(minHeight),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(sp.second, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

// 2. --- إعدادات التمرير والصفحات (SCROLL SETTINGS) ---
@Composable
fun ScrollSettingsView(
    viewModel: SettingsViewModel,
    paddingSize: Dp,
    minHeight: Dp,
    textBase: androidx.compose.ui.unit.TextUnit
) {
    val scrollDir by viewModel.scrollDirection.collectAsState()
    val pageMode by viewModel.pageMode.collectAsState()
    val fitMode by viewModel.fitMode.collectAsState()
    val autoScroll by viewModel.autoScrollEnabled.collectAsState()
    val autoScrollSpeed by viewModel.autoScrollSpeed.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("تخصيص وضع التمرير وتنسيق الصفحات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            // Scroll orientation
            Text("اتجاه تمرير الصفحات:", fontWeight = FontWeight.Bold, fontSize = textBase)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (scrollDir == "vertical") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .clickable { viewModel.updateScrollDirection("vertical") }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Vertical", tint = if (scrollDir == "vertical") MaterialTheme.colorScheme.primary else Color.Gray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("عمودي (سحب للأعلى)", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (scrollDir == "horizontal") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .clickable { viewModel.updateScrollDirection("horizontal") }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Horizontal", tint = if (scrollDir == "horizontal") MaterialTheme.colorScheme.primary else Color.Gray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("أفقي (سحب جانبي)", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            }

            Divider()

            // Page mode
            Text("وضع تنسيق العرض:", fontWeight = FontWeight.Bold, fontSize = textBase)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                val modes = listOf("continuous" to "مستمر", "single" to "صفحة صفحة", "book" to "وضع كتاب")
                modes.forEach { md ->
                    val isSelected = pageMode == md.first
                    OutlinedButton(
                        onClick = { viewModel.updatePageMode(md.first) },
                        modifier = Modifier
                            .weight(1f)
                            .height(minHeight),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Text(md.second, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            Divider()

            // Fit Mode
            Text("ملاءمة وتكيف الحجم الافتراضي:", fontWeight = FontWeight.Bold, fontSize = textBase)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                val fits = listOf("fit_width" to "ملاءمة العرض", "full_page" to "الصفحة الكاملة", "custom" to "حجم مخصص")
                fits.forEach { ft ->
                    val isSelected = fitMode == ft.first
                    OutlinedButton(
                        onClick = { viewModel.updateFitMode(ft.first) },
                        modifier = Modifier
                            .weight(1f)
                            .height(minHeight),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Text(ft.second, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            Divider()

            // Auto scroll controller with dynamic demonstration!
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Auto Scroll", tint = MaterialTheme.colorScheme.primary)
                        Text("ميزة التمرير التلقائي لذوي الاحتياجات والقراء:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = autoScroll,
                            onCheckedChange = { viewModel.updateAutoScrollEnabled(it) },
                            modifier = Modifier.testTag("auto_scroll_toggle")
                        )
                    }

                    if (autoScroll) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("سرعة التمرير الحالية:", fontSize = 11.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("${autoScrollSpeed.toInt()}x", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        Slider(
                            value = autoScrollSpeed,
                            onValueChange = { viewModel.updateAutoScrollSpeed(it) },
                            valueRange = 1f..10f,
                            modifier = Modifier.testTag("auto_scroll_speed_slider")
                        )

                        // Visual simulated auto scrolling demonstration!
                        Text("السرعة تفاعلية (سر الآن):", fontSize = 10.sp, fontStyle = FontStyle.Italic)
                        SimulatedScrollPreviewBox(autoScrollSpeed)
                    }
                }
            }
        }
    }
}

// Visual auto scrolling simulator
@Composable
fun SimulatedScrollPreviewBox(speedFactor: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScrollTransition")
    
    // Animate the offset of the simulated lines
    val offsetAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -120f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (12000 / speedFactor).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScrollOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray.copy(alpha = 0.8f))
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .offset(y = offsetAnimation.dp)
        ) {
            val lines = listOf(
                "--- السطر التوضيحي 1 لتجربة سرعة الحركة ---",
                "يمكنك الملاحظة كيف تتحرك نصوص الكتاب لأعلى",
                "بدون لمس الشاشه لتتيح لك تجربة خالية من المجهود",
                "سرعة تمريرك الحالية سريعة وملائمة تماماً لسرعتك",
                "--- السطر التوضيحي الأخير المستمر ---"
            )
            // Double the list to make seamless scrolling
            (lines + lines).forEach { line ->
                Text(
                    text = line,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// 3. --- إعدادات الترجمة والصوت (TRANSLATION & VOICE SETTINGS) ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranslationVoiceSettingsView(
    viewModel: SettingsViewModel,
    paddingSize: Dp,
    minHeight: Dp,
    textBase: androidx.compose.ui.unit.TextUnit
) {
    val context = LocalContext.current
    val sourceLang by viewModel.sourceLang.collectAsState()
    val targetLang by viewModel.targetLang.collectAsState()
    val ttsSpeed by viewModel.ttsSpeed.collectAsState()
    val readerVoice by viewModel.readerVoice.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("إعدادات محرك الترجمة ومحاكي النطق والملفات الصوتية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            // Source Translate Language
            Text("اللغة المصدر الافتراضية لقراءة الصفحات:", fontWeight = FontWeight.Bold, fontSize = textBase)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val languages = listOf("auto" to "تلقائي ✨", "ar" to "العربية 🇸🇦", "de" to "الألمانية 🇩🇪", "en" to "الإنجليزية 🇺🇸")
                languages.forEach { lang ->
                    FilterChip(
                        selected = sourceLang == lang.first,
                        onClick = { viewModel.updateSourceLang(lang.first) },
                        label = { Text(lang.second, fontSize = 11.sp) }
                    )
                }
            }

            Divider()

            // Target Translate Language
            Text("اللغة المستهدفة للترجمة الفورية الفورية والتحشية:", fontWeight = FontWeight.Bold, fontSize = textBase)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val targets = listOf("ar" to "العربية 🇸🇦", "de" to "الألمانية 🇩🇪", "en" to "الإنجليزية 🇺🇸", "fr" to "الفرنسية 🇫🇷")
                targets.forEach { lang ->
                    FilterChip(
                        selected = targetLang == lang.first,
                        onClick = { viewModel.updateTargetLang(lang.first) },
                        label = { Text(lang.second, fontSize = 11.sp) }
                    )
                }
            }

            Divider()

            // TTS Speed
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "TTS Speed")
                Text("سرعة النطق التلقائي التراكمي (TTS):", fontWeight = FontWeight.Bold, fontSize = textBase)
                Spacer(modifier = Modifier.weight(1f))
                Text("${ttsSpeed}x", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = ttsSpeed,
                onValueChange = { viewModel.updateTtsSpeed(it) },
                valueRange = 0.5f..2.0f,
                steps = 5,
                modifier = Modifier.testTag("tts_speed_slider")
            )

            // Reader Voice
            Text("جنس صوت القارئ:", fontWeight = FontWeight.Bold, fontSize = textBase)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.updateReaderVoice("male") },
                    modifier = Modifier
                        .weight(1f)
                        .height(minHeight)
                        .testTag("voice_male_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (readerVoice == "male") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Male, contentDescription = "Male Voice")
                        Text("صوت ذكر ♂", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = { viewModel.updateReaderVoice("female") },
                    modifier = Modifier
                        .weight(1f)
                        .height(minHeight)
                        .testTag("voice_female_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (readerVoice == "female") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Female, contentDescription = "Female Voice")
                        Text("صوت أنثى ♀", fontSize = 12.sp)
                    }
                }
            }

            // Interactive TTS Preview Button
            var isPlayingSoundwave by remember { mutableStateOf(false) }
            LaunchedEffect(isPlayingSoundwave) {
                if (isPlayingSoundwave) {
                    delay(3000)
                    isPlayingSoundwave = false
                }
            }

            Button(
                onClick = {
                    isPlayingSoundwave = true
                    viewModel.simulateTalkback("زر تجربة نطق الصوت بسرعة $ttsSpeed")
                    Toast.makeText(context, "محاكاة صوتية: 'مرحباً بك في إعدادات قارئ نصوص الألمانية والعربية الذكي.'", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Hearing, contentDescription = "Listen")
                    Text("تجربة نطق الصوت وسماع النغمة 🔊")
                }
            }

            AnimatedVisibility(visible = isPlayingSoundwave) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("جاري تشغيل الصوت الإرشادي الذكي: ", fontSize = 11.sp, fontStyle = FontStyle.Italic)
                    // Animated Sound Wave Representation
                    val infiniteTransition = rememberInfiniteTransition(label = "WaveTransition")
                    val h1 by infiniteTransition.animateFloat(10f, 40f, infiniteRepeatable(tween(200 / ttsSpeed.coerceIn(0.5f, 2.0f).toInt()), RepeatMode.Reverse), "w1")
                    val h2 by infiniteTransition.animateFloat(5f, 30f, infiniteRepeatable(tween(250 / ttsSpeed.coerceIn(0.5f, 2.0f).toInt()), RepeatMode.Reverse), "w2")
                    val h3 by infiniteTransition.animateFloat(8f, 45f, infiniteRepeatable(tween(180 / ttsSpeed.coerceIn(0.5f, 2.0f).toInt()), RepeatMode.Reverse), "w3")
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(4.dp).height(h1.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                        Box(Modifier.width(4.dp).height(h2.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                        Box(Modifier.width(4.dp).height(h3.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                    }
                }
            }

            Divider()

            // Language Pack offline storage manager
            Text("تحميل حزم اللغات للعمل أوفلاين دون إنترنت:", fontWeight = FontWeight.Bold, fontSize = textBase)
            viewModel.languagePacks.forEach { pack ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pack.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("الحجم: ${pack.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        
                        if (pack.isDownloading || (pack.progress > 0f && pack.progress < 1.0f)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = pack.progress,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (pack.progress >= 1.0f) {
                        Text("مُثبتة ✓", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.deleteLanguagePack(pack.code) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Pack", tint = Color.Red)
                        }
                    } else if (pack.isDownloading) {
                        Text("تنزيل ${(pack.progress * 100).toInt()}%", fontSize = 11.sp, fontStyle = FontStyle.Italic)
                    } else {
                        Button(
                            onClick = { viewModel.downloadLanguagePack(pack.code) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("تنزيل", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

// 4. --- إعدادات التعليقات والمخطوطات (ANNOTATION SETTINGS) ---
@Composable
fun AnnotationSettingsView(
    viewModel: SettingsViewModel,
    paddingSize: Dp,
    minHeight: Dp,
    textBase: androidx.compose.ui.unit.TextUnit
) {
    val highlightColorHex by viewModel.highlightColor.collectAsState()
    val penThickness by viewModel.penThickness.collectAsState()
    val opacity by viewModel.annotationOpacity.collectAsState()

    val colorsMap = listOf(
        "#FFEE58" to Color(0xFFFFEE58), // Yellow
        "#81C784" to Color(0xFF81C784), // Green
        "#64B5F6" to Color(0xFF64B5F6), // Blue
        "#F06292" to Color(0xFFF06292), // Pink
        "#FFB74D" to Color(0xFFFFB74D)  // Orange
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("إعدادات التعليقات والتحشية والتظليل اليدوي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            // Highlighter Color
            Text("اللون الافتراضي لتمييز النصوص والقلم:", fontWeight = FontWeight.Bold, fontSize = textBase)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                colorsMap.forEach { cm ->
                    val isSelected = highlightColorHex == cm.first
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 40.dp else 34.dp)
                            .clip(CircleShape)
                            .background(cm.second)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { viewModel.updateHighlightColor(cm.first) }
                            .testTag("color_${cm.first}")
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = Color.Black,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(18.dp)
                            )
                        }
                    }
                }
            }

            Divider()

            // Pen Thickness
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Gesture, contentDescription = "Pen Thickness")
                Text("سُمك قلم الرسم الحر التعليقي المساعد:", fontWeight = FontWeight.Bold, fontSize = textBase)
                Spacer(modifier = Modifier.weight(1f))
                Text("${penThickness.toInt()}dp", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = penThickness,
                onValueChange = { viewModel.updatePenThickness(it) },
                valueRange = 1f..15f,
                modifier = Modifier.testTag("pen_thickness_slider")
            )

            Divider()

            // Annotation opacity
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Opacity, contentDescription = "Opacity")
                Text("مستوى شفافية التحشية (Opacity):", fontWeight = FontWeight.Bold, fontSize = textBase)
                Spacer(modifier = Modifier.weight(1f))
                Text("${(opacity * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = opacity,
                onValueChange = { viewModel.updateAnnotationOpacity(it) },
                valueRange = 0.1f..1.0f,
                modifier = Modifier.testTag("opacity_slider")
            )

            Divider()

            // Live drawing panel simulation!
            Text("لوحة محاكاة الرسم والشخبطة المباشرة (جرب الرسم هنا بأصبعك!):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            InteractiveGestureDrawingCanvas(
                strokeColor = colorsMap.find { it.first == highlightColorHex }?.second ?: Color.Yellow,
                strokeWidth = penThickness,
                opacity = opacity
            )
        }
    }
}

// Finger drawing canvas simulator
@Composable
fun InteractiveGestureDrawingCanvas(
    strokeColor: Color,
    strokeWidth: Float,
    opacity: Float
) {
    val drawPaths = remember { mutableStateListOf<Pair<Path, Float>>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(2.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            .pointerInput(strokeColor, strokeWidth, opacity) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val path = Path().apply {
                            moveTo(offset.x, offset.y)
                        }
                        currentPath = path
                        drawPaths.add(path to strokeWidth)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentPath?.let { path ->
                            // Draw continuous lines
                            val lastPos = change.position
                            path.lineTo(lastPos.x, lastPos.y)
                            // Re-add to trigger recomposition
                            val lastIdx = drawPaths.size - 1
                            if (lastIdx >= 0) {
                                drawPaths[lastIdx] = path to strokeWidth
                            }
                        }
                    },
                    onDragEnd = {
                        currentPath = null
                    }
                )
            }
    ) {
        // Draw real user strokes
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPaths.forEach { (path, width) ->
                drawPath(
                    path = path,
                    color = strokeColor.copy(alpha = opacity),
                    style = Stroke(width = width, cap = StrokeCap.Round)
                )
            }
        }
        
        if (drawPaths.isEmpty()) {
            Text(
                text = "اسحب إصبعك هنا للرسم الحر بالقلم واللون المختار",
                color = Color.LightGray,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            IconButton(
                onClick = { drawPaths.clear() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear Scribble", tint = Color.Red)
            }
        }
    }
}

// 5. --- إمكانية الوصول (ACCESSIBILITY SCREEN) ---
@Composable
fun AccessibilitySettingsView(
    viewModel: SettingsViewModel,
    activity: Activity,
    paddingSize: Dp,
    minHeight: Dp,
    textBase: androidx.compose.ui.unit.TextUnit
) {
    val highContrast by viewModel.highContrastEnabled.collectAsState()
    val largeTargets by viewModel.largeTargetsEnabled.collectAsState()
    val talkbackSim by viewModel.talkbackSimEnabled.collectAsState()
    val reduceMotion by viewModel.reduceMotionEnabled.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("أدوات تيسير إمكانية الوصول وذوي الهمم", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            // High Contrast Mode
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Adjust, contentDescription = "Contrast")
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("وضع ألوان عالي التباين (Contrast Boost):", fontWeight = FontWeight.Bold, fontSize = textBase)
                    Text("يفيد في القراءة تحت ضوء الشمس القوي", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                }
                Switch(
                    checked = highContrast,
                    onCheckedChange = { viewModel.updateHighContrast(it, activity) },
                    modifier = Modifier.testTag("high_contrast_switch")
                )
            }

            Divider()

            // Large Touch Targets & Pointer Enlargement
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AspectRatio, contentDescription = "Large bounds")
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("تكبير حجم الأزرار ومسافات اللمس:", fontWeight = FontWeight.Bold, fontSize = textBase)
                    Text("توسيع حقل الضغط إلى 56dp كيسّيري للمس", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                }
                Switch(
                    checked = largeTargets,
                    onCheckedChange = { viewModel.updateLargeTargets(it) },
                    modifier = Modifier.testTag("large_targets_switch")
                )
            }

            Divider()

            // Screen Reader Simulated TalkBack Helper
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = "TalkBack Simulator")
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("محاكاة قارئ الشاشة التلقائي (TalkBack):", fontWeight = FontWeight.Bold, fontSize = textBase)
                    Text("يعرض تعليق منطوق مرئي عند النقر على أي عنصر", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                }
                Switch(
                    checked = talkbackSim,
                    onCheckedChange = { viewModel.updateTalkbackSim(it) }
                )
            }

            Divider()

            // Reduce Motion Animation Toggle
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.MotionPhotosOff, contentDescription = "Reduce Motion")
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("تقليل أنيمشنز الحركة (Reduce Motion):", fontWeight = FontWeight.Bold, fontSize = textBase)
                    Text("يمنع الغثيان والحساسية الناتجة عن الحركات", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                }
                Switch(
                    checked = reduceMotion,
                    onCheckedChange = { viewModel.updateReduceMotion(it) }
                )
            }

            Divider()

            // Keyboard Shortcuts list references
            Text("قائمة اختصارات لوحة المفاتيح الكاملة للقارئ المادي:", fontWeight = FontWeight.Bold, fontSize = textBase)
            val shortcuts = listOf(
                "Ctrl + سهم أيمن / أيسر" to "الصفحة التالية / السابقة",
                "Ctrl + L" to "تشغيل النطق التلقائي للمستند (TTS)",
                "Ctrl + F" to "البحث الفوري عن كلمة في الورقة",
                "Alt + K" to "تغيير الخط لتطبيق السيبيا أو الداكن الصديق للهاتف"
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(10.dp)
            ) {
                shortcuts.forEach { (key, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(key, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Text(desc, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// 6. --- إعدادات الخصوصية والأمن (PRIVACY & SECURITY SCREEN) ---
@Composable
fun PrivacySettingsView(
    viewModel: SettingsViewModel,
    activity: Activity,
    paddingSize: Dp,
    minHeight: Dp,
    textBase: androidx.compose.ui.unit.TextUnit
) {
    val appLock by viewModel.appLockEnabled.collectAsState()
    val pinCode by viewModel.appLockPIN.collectAsState()
    val screenSecurity by viewModel.screenSecurityEnabled.collectAsState()

    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ميزات خصوصية الوثيقة وأمان القفل البيومتري والثنائي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            // App Lock PIN Code Lock
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Lock, contentDescription = "App Lock")
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("قفل تطبيق القارئ برقم PIN سري:", fontWeight = FontWeight.Bold, fontSize = textBase)
                    Text("طلب PIN عند فتح التطبيق من جديد لحماية خصوصيتك", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                }
                Switch(
                    checked = appLock,
                    onCheckedChange = { viewModel.updateAppLockEnabled(it, pinCode) },
                    modifier = Modifier.testTag("app_lock_switch")
                )
            }

            if (appLock) {
                var tempPIN by remember { mutableStateOf(pinCode) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = tempPIN,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                tempPIN = it
                            }
                        },
                        label = { Text("أدخل 4 أرقام سرية لقفل التطبيق") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (tempPIN.length == 4) {
                                viewModel.updateAppLockEnabled(true, tempPIN)
                                Toast.makeText(context, "تم تحديث الرقم السري بنجاح: $tempPIN", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "الرقم السري يجب أن يتكون من 4 أرقام!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.height(minHeight)
                    ) {
                        Text("حفظ")
                    }
                }
            }

            Divider()

            // Window Screen Security layout settings (FLAG_SECURE)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.HideImage, contentDescription = "Screen security")
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("إخفاء محتوى التطبيق وتأمين اللقطات:", fontWeight = FontWeight.Bold, fontSize = textBase)
                    Text("يمنع تصوير الشاشة (Screenshot) ويخفيه في التطبيقات الأخيرة", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                }
                Switch(
                    checked = screenSecurity,
                    onCheckedChange = { viewModel.updateScreenSecurity(it, activity) },
                    modifier = Modifier.testTag("screen_security_switch")
                )
            }

            Divider()

            // File Locker tool (Lock specific items with password)
            Text("قفل وتشفير ملفات PDF معينة بكلمة مرور مخصصة:", fontWeight = FontWeight.Bold, fontSize = textBase)
            viewModel.filesList.forEach { securable ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (securable.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Status",
                            tint = if (securable.isLocked) Color.Red else Color.Green
                        )
                        Column {
                            Text(securable.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("محمي: ${if (securable.isLocked) "نعم 🔒" else "مفتوح للكل 🔓"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    var passInput by remember { mutableStateOf("") }
                    var showDialogInput by remember { mutableStateOf(false) }

                    if (showDialogInput) {
                        AlertDialog(
                            onDismissRequest = { showDialogInput = false },
                            title = { Text("قفل/فك القفل") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("أدخل كلمة المرور لمعالجة ملف: ${securable.name}")
                                    TextField(
                                        value = passInput,
                                        onValueChange = { passInput = it },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        label = { Text("رمز المرور الثاني") }
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDialogInput = false
                                        if (securable.isLocked) {
                                            val success = viewModel.unlockFile(securable.id, passInput)
                                            if (success) {
                                                Toast.makeText(context, "تم إلغاء قفل الملف بنجاح!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "كلمة المرور خاطئة!", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            if (passInput.isNotEmpty()) {
                                                viewModel.lockFile(securable.id, passInput)
                                                Toast.makeText(context, "تم تشفير وقفل الملف الآن!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "الرجاء كتابة رمز مرور!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        passInput = ""
                                    }
                                ) {
                                    Text("تأكيد")
                                }
                            }
                        )
                    }

                    Button(
                        onClick = { showDialogInput = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (securable.isLocked) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    ) {
                        Text(if (securable.isLocked) "إلغاء التشفير" else "قفل بالرمز", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// Interactive Real-time Document Live Preview (THE ABSOLUTE CRAFT!)
@Composable
fun LiveDocumentPreview(
    viewModel: SettingsViewModel,
    reduceMotion: Boolean
) {
    val themeSetting by viewModel.theme.collectAsState()
    val fontName by viewModel.font.collectAsState()
    val fontSizeScaled by viewModel.fontSize.collectAsState()
    val lineSpacing by viewModel.lineSpacing.collectAsState()
    val scrollDir by viewModel.scrollDirection.collectAsState()
    val targetLang by viewModel.targetLang.collectAsState()
    val highlightColorHex by viewModel.highlightColor.collectAsState()
    val penThickness by viewModel.penThickness.collectAsState()
    val annotationOpacity by viewModel.annotationOpacity.collectAsState()

    // Map color theme settings to local preview background
    val previewBgColor = when (themeSetting) {
        "dark" -> Color(0xFF1E1E1E)
        "sepia" -> Color(0xFFF4ECD8)
        "high_contrast" -> Color.Black
        else -> Color(0xFFFAF6EE)
    }

    val previewTextColor = when (themeSetting) {
        "dark" -> Color.White
        "sepia" -> Color(0xFF4A3B32)
        "high_contrast" -> Color.White
        else -> Color(0xFF1E293B)
    }

    val previewParagraphSpacing = when (lineSpacing) {
        "tight" -> 4.dp
        "wide" -> 16.dp
        else -> 10.dp
    }

    val highlightColorParsed = when (highlightColorHex) {
        "#81C784" -> Color(0xFF81C784)
        "#64B5F6" -> Color(0xFF64B5F6)
        "#F06292" -> Color(0xFFF06292)
        "#FFB74D" -> Color(0xFFFFB74D)
        else -> Color(0xFFFFEE58) // Yellow default
    }

    Surface(
        color = previewBgColor,
        contentColor = previewTextColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 2.dp,
                color = if (themeSetting == "high_contrast") Color.Yellow else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .shadow(4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(previewParagraphSpacing)
        ) {
            // Header simulated indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "صفحة المستند 12 من 450",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = previewTextColor.copy(alpha = 0.6f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(6.dp).background(if (scrollDir == "vertical") Color.Green else Color.LightGray, CircleShape))
                    Box(Modifier.size(6.dp).background(previewTextColor.copy(alpha = 0.4f), CircleShape))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Highlighted Title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Drawing highlighted annotation line
                        drawRect(
                            color = highlightColorParsed.copy(alpha = annotationOpacity),
                            size = size.copy(height = size.height * 0.8f, width = size.width),
                            topLeft = Offset(0f, size.height * 0.1f)
                        )
                        // Mock hand scribbled pen line below
                        drawLine(
                            color = highlightColorParsed.copy(alpha = 0.8f),
                            start = Offset(0f, size.height - 2f),
                            end = Offset(size.width * 0.9f, size.height - 2f),
                            strokeWidth = penThickness,
                            cap = StrokeCap.Round
                        )
                    }
            ) {
                Text(
                    text = "الفصل الأول: تاريخ اللغات الحية",
                    fontFamily = getFontFamilySimulation(fontName),
                    fontSize = (fontSizeScaled * 0.8f).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Paragraph text showing live updates
            Text(
                text = "مرحباً بك في لوحة معاينة المعالج. عند تعديل أي خيار من إعدادات العرض أو المحاذاة أو تباعد الأسطر والخطوط فإن هذا المستند المباشر يتغير في التو واللحظة ليعكس ما تراه في المستند الحقيقي.",
                fontFamily = getFontFamilySimulation(fontName),
                fontSize = (fontSizeScaled * 0.6f).sp,
                lineHeight = (fontSizeScaled * 1.0f).sp,
                color = previewTextColor,
                modifier = Modifier.fillMaxWidth()
            )

            // Translation interactive segment
            Surface(
                color = previewTextColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text(
                        text = "ترجمة الفقرة الحالية إلى (${if (targetLang=="ar") "العربية" else if (targetLang=="de") "الألمانية" else "الإنجليزية"}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = previewTextColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = when (targetLang) {
                            "de" -> "Willkommen im Dokument-Vorschaubereich. Jede Änderung, die Sie vornehmen, wird sofort angezeigt."
                            "en" -> "Welcome to the document preview pane. Any change you make will immediately reflect here."
                            "fr" -> "Bienvenue dans le volet d'aperçu du document. Tout changement s'affiche immédiatement."
                            else -> "مرحباً بك في جزء معاينة المستند المباشر. أي تغيير تقوم به ينعكس فوراً هنا."
                        },
                        fontFamily = getFontFamilySimulation(fontName),
                        fontSize = (fontSizeScaled * 0.5f).sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// Maps name to standard platform families to avoid loading failures
fun getFontFamilySimulation(fontName: String): FontFamily {
    return when (fontName) {
        "Amiri", "Scheherazade" -> FontFamily.Serif
        "Cairo", "Noto Kufi", "Tajawal", "Almarai" -> FontFamily.SansSerif
        "IBM Plex" -> FontFamily.Monospace
        else -> FontFamily.Default
    }
}

data class CategoryItem(
    val id: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
