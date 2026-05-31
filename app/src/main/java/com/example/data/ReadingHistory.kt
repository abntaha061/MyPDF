package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_history")
data class ReadingHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pdfFileId: Int,
    val pdfTitle: String,
    val pageIndex: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0 // Optional tracking
)
