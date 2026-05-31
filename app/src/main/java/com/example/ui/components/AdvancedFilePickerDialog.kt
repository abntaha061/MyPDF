package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PdfFile
import com.example.ui.PdfRendererViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilePickerDialog(
    viewModel: PdfRendererViewModel,
    onOpenFile: (PdfFile) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var activeSourceTab by remember { mutableStateOf("internal") } // internal, sdcard, cloud, saf

    // SAF default system picker integration
    val systemSafPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importSelectedPdf(uri) { importedFile ->
                if (importedFile != null) {
                    Toast.makeText(context, "تم استيراد الملف بنجاح وتحليله!", Toast.LENGTH_SHORT).show()
                    onOpenFile(importedFile)
                    onDismissRequest()
                } else {
                    Toast.makeText(context, "فشل استيراد الملف", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // List trackers
    val allFiles by viewModel.allFiles.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مستعرض ملفات WPS الذكي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close dialog")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Horizontal scroll selectors of directories
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PickerSourceTab(
                        label = "الهاتف",
                        selected = activeSourceTab == "internal",
                        icon = Icons.Default.PhoneAndroid,
                        modifier = Modifier.weight(1f),
                        onClick = { activeSourceTab = "internal" }
                    )
                    PickerSourceTab(
                        label = "ذاكرة SD",
                        selected = activeSourceTab == "sdcard",
                        icon = Icons.Default.SdCard,
                        modifier = Modifier.weight(1f),
                        onClick = { activeSourceTab = "sdcard" }
                    )
                    PickerSourceTab(
                        label = "السحابية",
                        selected = activeSourceTab == "cloud",
                        icon = Icons.Default.CloudQueue,
                        modifier = Modifier.weight(1f),
                        onClick = { activeSourceTab = "cloud" }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Scroll view container
                Box(modifier = Modifier.weight(1f)) {
                    when (activeSourceTab) {
                        "internal" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "مساحة التخزين الداخلية:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray
                                )

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // SAF button
                                    item {
                                        Button(
                                            onClick = { systemSafPicker.launch("application/pdf") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("اختيار ملف مخصص (SAF)", fontSize = 12.sp)
                                        }
                                    }

                                    // Local cached document items
                                    if (allFiles.isEmpty()) {
                                        item {
                                            EmptyPickerHint(msg = "لا توجد مستندات مستوردة محلياً بعد.")
                                        }
                                    } else {
                                        items(allFiles) { file ->
                                            PickerFileRow(
                                                title = file.title,
                                                info = "${file.formattedSize} • ${file.totalPages} صفحة",
                                                icon = Icons.Default.Description,
                                                onClick = {
                                                    onOpenFile(file)
                                                    onDismissRequest()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "sdcard" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "بطاقة الذاكرة الخارجية (MicroSD Partition):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray
                                )

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        PickerFileRow(
                                            title = "[SD_CARD] محاضرات الفصل الأول.pdf",
                                            info = "1.2 MB  •  24 صفحة",
                                            icon = Icons.Default.FolderOpen,
                                            onClick = {
                                                importMockPdfAndOpen(viewModel, "محاضرات الفصل الأول", 24, onOpenFile, onDismissRequest, context)
                                            }
                                        )
                                    }
                                    item {
                                        PickerFileRow(
                                            title = "[SD_CARD] كتاب الميكانيكا الهندسية.pdf",
                                            info = "14.5 MB  •  380 صفحة",
                                            icon = Icons.Default.FolderOpen,
                                            onClick = {
                                                importMockPdfAndOpen(viewModel, "كتاب الميكانيكا الهندسية", 380, onOpenFile, onDismissRequest, context)
                                            }
                                        )
                                    }
                                    item {
                                        PickerFileRow(
                                            title = "[SD_CARD] كشف حساب الربع السنوي.pdf",
                                            info = "412 KB  •  5 صفحة",
                                            icon = Icons.Default.FolderOpen,
                                            onClick = {
                                                importMockPdfAndOpen(viewModel, "كشف حساب الربع السنوي", 5, onOpenFile, onDismissRequest, context)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        "cloud" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "تكامل السحب (Google Drive, Dropbox, OneDrive):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray
                                )

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        PickerFileRow(
                                            title = "[Google Drive] ملخص دراسة فيزياء 101.pdf",
                                            info = "السحابة الإلكترونية • 18 صفحة",
                                            icon = Icons.Default.CloudDone,
                                            onClick = {
                                                importMockPdfAndOpen(viewModel, "ملخص دراسة فيزياء 101", 18, onOpenFile, onDismissRequest, context)
                                            }
                                        )
                                    }
                                    item {
                                        PickerFileRow(
                                            title = "[Dropbox] تقارير كفاءة المبيعات السنوية.pdf",
                                            info = "قراءة ومزامنة سحابية • 75 صفحة",
                                            icon = Icons.Default.CloudDownload,
                                            onClick = {
                                                importMockPdfAndOpen(viewModel, "تقارير كفاءة المبيعات السنوية", 75, onOpenFile, onDismissRequest, context)
                                            }
                                        )
                                    }
                                    item {
                                        PickerFileRow(
                                            title = "[OneDrive] اختبار تحديد مهارات البرمجة.pdf",
                                            info = "قراءة ومزامنة سحابية • 12 صفحة",
                                            icon = Icons.Default.CloudQueue,
                                            onClick = {
                                                importMockPdfAndOpen(viewModel, "اختبار تحديد مهارات البرمجة", 12, onOpenFile, onDismissRequest, context)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { systemSafPicker.launch("application/pdf") }
            ) {
                Text("فتح النظام لملفات أخرى")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

fun importMockPdfAndOpen(
    viewModel: PdfRendererViewModel,
    title: String,
    pages: Int,
    onOpenFile: (PdfFile) -> Unit,
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    viewModel.importMockDocumentForTesting(
        title = title,
        totalPages = pages
    ) { progressFile ->
        if (progressFile != null) {
            Toast.makeText(context, "تم تحميل المستند السحابي ومزامنته محلياً!", Toast.LENGTH_SHORT).show()
            onOpenFile(progressFile)
            onDismiss()
        }
    }
}

@Composable
fun PickerSourceTab(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}

@Composable
fun PickerFileRow(
    title: String,
    info: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = info,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronLeft, // Arabic chevron points left, or AutoMirrored ChevronLeft
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EmptyPickerHint(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = msg,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}
