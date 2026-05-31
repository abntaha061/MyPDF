package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "pdf_files")
@Serializable
data class PdfFile(
    @PrimaryKey val id: String,
    val title: String,
    val filePath: String,
    val size: Long,
    val pageCount: Int,
    val addedDate: Long = System.currentTimeMillis(),
    val lastReadDate: Long = 0L,
    val lastReadPage: Int = 0,
    val isFavorite: Boolean = false,
    val folderId: String? = null
)
