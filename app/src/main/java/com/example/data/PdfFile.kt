package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_files")
data class PdfFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val title: String,
    val fileSize: Long,
    val addedDate: Long,
    val lastReadDate: Long = 0,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isBookmarked: Boolean = false,
    val folderName: String = "",
    val isSample: Boolean = false,
    val isFavorite: Boolean = false,
    val category: String = "مستندات", // Documents, Books, Reports, Tests/Quizzes
    val tags: String = "", // Comma-separated tags
    val commentCount: Int = 0,
    val bookmarkCount: Int = 0
) {
    val formattedSize: String
        get() {
            val kb = fileSize / 1024.0
            val mb = kb / 1024.0
            return if (mb > 1.0) {
                String.format("%.2f MB", mb)
            } else {
                String.format("%.2f KB", kb)
            }
        }
}
