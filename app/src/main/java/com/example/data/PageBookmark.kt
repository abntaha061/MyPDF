package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "page_bookmarks")
data class PageBookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pdfFileId: Int,
    val pdfTitle: String,
    val pageIndex: Int,
    val label: String,
    val addedDate: Long = System.currentTimeMillis()
)
