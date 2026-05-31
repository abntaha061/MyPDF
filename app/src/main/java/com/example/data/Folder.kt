package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "folders")
@Serializable
data class Folder(
    @PrimaryKey val id: String,
    val name: String,
    val createdDate: Long = System.currentTimeMillis()
)
