package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PdfRendererViewModel
import com.example.data.DictionaryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    viewModel: PdfRendererViewModel,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val dm = viewModel.dictionaryManager
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryTab by remember { mutableStateOf(0) } // 0: البحث والمستكشف, 1: قائمة المراجعة المجدولة

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مستودع المفردات والتكرار المتباعد", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, "Workspace Menu")
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
                .background(Color(0xFFF5F7FA))
        ) {
            // Category chooser Tabs
            TabRow(
                selectedTabIndex = selectedCategoryTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedCategoryTab == 0,
                    onClick = { selectedCategoryTab = 0 },
                    text = { Text("مستكشف القاموس أوفلاين", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Language, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedCategoryTab == 1,
                    onClick = { selectedCategoryTab = 1 },
                    text = { Text("المراجعة المتباعد (Spaced Rep)", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Psychology, null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedCategoryTab == 0) {
                // DICTIONARY EXPLORER VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث بالألمانية أو العربية (مثال: Aufgabe, الشاشة)...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // Lookup result
                    val searchResult: List<DictionaryEntry> = remember(searchQuery) {
                        if (searchQuery.isBlank()) emptyList<DictionaryEntry>()
                        else {
                            // Procedural Search in Dictionary Database & Lexicon
                            dm.searchDictionary(searchQuery.trim())
                        }
                    }

                    if (searchQuery.isBlank()) {
                        // Empty Search State: Show some useful hot words suggestions
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Book,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Text(
                                    text = "اكتشف المفردات الألمانية المتقدمة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "القاموس يضم أكثر من ٥٠٠ ألف كلمة وتصريفات أوفلاين متكاملة. اكتب أي كلمة بالألمانية أو العربية للبدء.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("كلمات مقترحة سريعة الدراسات للتعلم:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Aufgabe", "lernen", "Bildschirm", "verstehen", "Besprechung").forEach { sugg ->
                                        SuggestionChip(
                                            onClick = { searchQuery = sugg },
                                            label = { Text(sugg) }
                                        )
                                    }
                                }
                            }
                        }
                    } else if (searchResult.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.MenuBook, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                Text("لم يتم العثور على نتائج تطابق \"$searchQuery\"", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        // Active Search Results List
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = searchResult) { entry ->
                                DictionaryEntryRow(entry = entry, dm = dm)
                            }
                        }
                    }
                }
            } else {
                // SPACED REPETITION STUDY DECK REVIEW
                val reviewList: List<DictionaryEntry> = remember { dm.getScheduledReviewList() }

                if (reviewList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(80.dp)
                            )
                            Text(
                                text = "لقد أكملت جميع مرجعات اليوم بنجاح! 🎉",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "مستودع التكرار المتباعد فارغ تماماً حالياً. سيقوم النظام بجدولة الكلمات تلقائياً للمستقبل بناءً على استجابتك الاستيعابية.",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "📌 بطاقات ذكية مستحقة للمراجعة العاجلة (${reviewList.size} مفردة)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(items = reviewList) { entry ->
                                DictionaryReviewItemRow(entry = entry, dm = dm, onRefresh = {
                                    // Refresh states trigger
                                    Toast.makeText(context, "تم حفظ نتيجة استيعاب الكلمة!", Toast.LENGTH_SHORT).show()
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DictionaryEntryRow(
    entry: DictionaryEntry,
    dm: com.example.data.DictionaryManager
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = entry.word, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
                    Text(text = entry.phonetic, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = Color.Gray)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SuggestionChip(onClick = {}, label = { Text(entry.cefr, fontSize = 10.sp, fontWeight = FontWeight.Bold) })
                    SuggestionChip(onClick = {}, label = { Text(entry.partOfSpeech, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary) })
                }
            }

            Text(
                text = entry.translation,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00796B),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Plural info
                if (entry.plural.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("الجمع (Plural):", fontSize = 12.sp, color = Color.Gray)
                        Text(entry.plural, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // If verb conjugations
                if (entry.conjugations.isNotEmpty()) {
                    Column {
                        Text("تصريفات الفعل الرئيسية:", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            entry.conjugations.forEach { conj ->
                                Box(modifier = Modifier.background(Color(0xFFECEFF1), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(conj, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Examples list
                if (entry.examples.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("أمثلة وجمل سياقية (Beispiele):", fontSize = 11.sp, color = Color.Gray)
                        entry.examples.forEach { (gerEx, araEx) ->
                            Column(modifier = Modifier.background(Color(0xFFF1F3F4), RoundedCornerShape(6.dp)).padding(8.dp).fillMaxWidth()) {
                                Text(gerEx, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(araEx, fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Bottom speaker pronunciations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { dm.speak(entry.word, false) }) {
                        Icon(Icons.Default.VolumeUp, "Listen Normal", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = { dm.speak(entry.word, true) }) {
                        Icon(Icons.Default.VolumeDown, "Listen Slow (50%)", tint = MaterialTheme.colorScheme.secondary)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Quick scheduled review toggler
                    Button(
                        onClick = {
                            dm.updateSpacedRepetition(entry, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("أضف للمراجعات تلقائياً 🌟", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DictionaryReviewItemRow(
    entry: DictionaryEntry,
    dm: com.example.data.DictionaryManager,
    onRefresh: () -> Unit
) {
    var isAnswerRevealed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Unrevealed state always shows German word & IPA pronunciation helper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = entry.word, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    Text(text = entry.phonetic, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = Color.Gray)
                }

                IconButton(onClick = { dm.speak(entry.word, false) }) {
                    Icon(Icons.Default.VolumeUp, "audio", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (!isAnswerRevealed) {
                Button(
                    onClick = { isAnswerRevealed = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("اكشف الترجمة والإجابة العربية 👁️", fontSize = 12.sp)
                }
            } else {
                HorizontalDivider()

                // Revealed Details
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ترجمة المفردة:", fontSize = 11.sp, color = Color.Gray)
                        Text(entry.partOfSpeech, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = entry.translation,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00796B),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (entry.examples.isNotEmpty()) {
                        Text("مثال للاستخدام:", fontSize = 11.sp, color = Color.Gray)
                        val firstEx = entry.examples.first()
                        Column(modifier = Modifier.background(Color(0xFFECEFF1), RoundedCornerShape(6.dp)).padding(8.dp).fillMaxWidth()) {
                            Text(firstEx.first, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(firstEx.second, fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }

                HorizontalDivider()

                // SuperMemo Spaced repetition feedback buttons!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            dm.updateSpacedRepetition(entry, true)
                            isAnswerRevealed = false
                            onRefresh()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("سهل: تذكّرتها 👍", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            dm.updateSpacedRepetition(entry, false)
                            onRefresh()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("صعب: مراجعة اليوم 🔄", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
