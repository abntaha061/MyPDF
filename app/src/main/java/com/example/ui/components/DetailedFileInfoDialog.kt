package com.example.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PdfFile
import com.example.ui.PdfRendererViewModel
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailedFileInfoDialog(
    file: PdfFile,
    viewModel: PdfRendererViewModel,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val categoryScrollState = rememberScrollState()

    // Temporary editor states
    var customTagsInput by remember { mutableStateOf(file.tags) }
    var selectedCategory by remember { mutableStateOf(file.category) }
    var isFavorite by remember { mutableStateOf(file.isFavorite) }
    var activeFolderInput by remember { mutableStateOf(file.folderName) }

    // Categories available
    val categoriesAvailable = listOf("مستندات", "كتب", "تقارير", "اختبارات")

    // Dates formatted helper
    fun formatLongDate(time: Long): String {
        if (time == 0L) return "لم يُقرأ بعد"
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.timeInMillis = time
        return DateFormat.format("dd MMMM yyyy hh:mm a", cal).toString()
    }

    // Calculating precise progress indicators
    val percentProgress = if (file.totalPages > 0) {
        (file.currentPage + 1).toFloat() / file.totalPages.toFloat()
    } else {
        0f
    }
    val completionPercentText = (percentProgress * 100).toInt().coerceIn(0, 100)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تفاصيل المستند",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                // Favorite Toggle Star Button
                IconButton(onClick = { isFavorite = !isFavorite }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite status toggle",
                        tint = if (isFavorite) Color(0xFFFFB300) else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Document Heading
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (selectedCategory) {
                                "كتب" -> Icons.Default.MenuBook
                                "تقارير" -> Icons.Default.Assessment
                                "اختبارات" -> Icons.Default.AssignmentTurnedIn
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = file.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "القسم: $selectedCategory",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Stats Metrics Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "تفاصيل ومعلومات وصفيّة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoStatCard(
                            label = "حجم الملف",
                            value = file.formattedSize,
                            icon = Icons.Default.Cloud,
                            modifier = Modifier.weight(1f)
                        )
                        InfoStatCard(
                            label = "عدد الصفحات",
                            value = "${file.totalPages} صفحة",
                            icon = Icons.Default.AutoStories,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoStatCard(
                            label = "الإشارات والصفحات",
                            value = "${file.currentPage + 1} / ${file.totalPages}",
                            icon = Icons.Default.Bookmark,
                            modifier = Modifier.weight(1f)
                        )
                        InfoStatCard(
                            label = "التفاعلات والتعليقات",
                            value = "${file.commentCount} تعليق",
                            icon = Icons.Default.Comment,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Progress Bar Display Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معدل التقدم المحرز في القراءة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$completionPercentText%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { percentProgress.coerceIn(0f, 1f) },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }

                // Date added/modified metadata list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetaLabelValue(label = "تاريخ الإضافة:", value = formatLongDate(file.addedDate))
                    MetaLabelValue(label = "آخر تشغيل:", value = formatLongDate(file.lastReadDate))
                }

                HorizontalDivider()

                // Interactivity: Category Reassignment Segment
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "تصنيف المستند التلقائي:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(categoryScrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoriesAvailable.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Custom Folder input
                OutlinedTextField(
                    value = activeFolderInput,
                    onValueChange = { activeFolderInput = it },
                    label = { Text("المجلد المخصص (Folder)") },
                    placeholder = { Text("انقل المستند لمجلد مخصص") },
                    singleLine = true,
                    leadingIcon = { Icon(imageVector = Icons.Default.Folder, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Interactivity: Tags Editor
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = customTagsInput,
                        onValueChange = { customTagsInput = it },
                        label = { Text("علامات التمييز الكودية (بدلالة الفاصلة ,)") },
                        placeholder = { Text("مثال: فيزياء, دراسة, عاجل") },
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tags chips visual previews
                    if (customTagsInput.isNotBlank()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            customTagsInput.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .forEach { tag ->
                                    val chipColor = getSemanticTagColor(tag)
                                    Box(
                                        modifier = Modifier
                                            .background(chipColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .border(BorderStroke(1.dp, chipColor.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = chipColor
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Update Database values securely through ViewModels
                    viewModel.updateCategory(file, selectedCategory)
                    viewModel.moveDocumentToFolder(file.id, activeFolderInput)
                    viewModel.updateTags(file, customTagsInput)
                    if (isFavorite != file.isFavorite) {
                        viewModel.toggleFavorite(file)
                    }
                    onDismiss()
                }
            ) {
                Text("حفظ التعديلات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// Generates beautiful responsive tag background colors based on naming strings
fun getSemanticTagColor(tagName: String): Color {
    val seed = tagName.hashCode()
    return when (Math.abs(seed) % 6) {
        0 -> Color(0xFFE53935) // Red Accent
        1 -> Color(0xFF1E88E5) // Blue Accent
        2 -> Color(0xFF43A047) // Green Accent
        3 -> Color(0xFF8E24AA) // Purple Accent
        4 -> Color(0xFFD81B60) // Magenta Accent
        else -> Color(0xFFF4511E) // Orange Accent
    }
}

@Composable
fun InfoStatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, size = 16.dp, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MetaLabelValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

// Icon helper function to specify custom sizes easily
@Composable
private fun Icon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp,
    tint: Color
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size)
    )
}
