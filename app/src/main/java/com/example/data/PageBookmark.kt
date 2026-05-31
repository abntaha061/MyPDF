package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "page_bookmarks")
@Serializable
data class PageBookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pdfId: String,
    val pageIndex: Int,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
