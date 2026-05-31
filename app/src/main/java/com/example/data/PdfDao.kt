package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_files ORDER BY addedDate DESC")
    fun getAllFiles(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdf_files WHERE lastReadDate > 0 ORDER BY lastReadDate DESC")
    fun getRecentFiles(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdf_files WHERE isBookmarked = 1 ORDER BY lastReadDate DESC, addedDate DESC")
    fun getBookmarkedFiles(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdf_files WHERE folderName = :folderName ORDER BY addedDate DESC")
    fun getFilesByFolder(folderName: String): Flow<List<PdfFile>>

    @Query("SELECT DISTINCT folderName FROM pdf_files WHERE folderName != '' AND folderName IS NOT NULL ORDER BY folderName ASC")
    fun getAllFolders(): Flow<List<String>>

    @Query("SELECT * FROM pdf_files WHERE filePath = :path LIMIT 1")
    suspend fun getFileByPath(path: String): PdfFile?

    @Query("SELECT * FROM pdf_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Int): PdfFile?

    @Query("SELECT * FROM pdf_files WHERE title LIKE '%' || :query || '%' ORDER BY lastReadDate DESC")
    fun searchFiles(query: String): Flow<List<PdfFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(pdfFile: PdfFile): Long

    @Update
    suspend fun updateFile(pdfFile: PdfFile)

    @Delete
    suspend fun deleteFile(pdfFile: PdfFile)

    // --- PageBookmark Queries ---
    @Query("SELECT * FROM page_bookmarks ORDER BY addedDate DESC")
    fun getAllPageBookmarks(): Flow<List<PageBookmark>>

    @Query("SELECT * FROM page_bookmarks WHERE pdfFileId = :pdfFileId ORDER BY pageIndex ASC")
    fun getPageBookmarksForFile(pdfFileId: Int): Flow<List<PageBookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageBookmark(pageBookmark: PageBookmark): Long

    @Query("DELETE FROM page_bookmarks WHERE id = :id")
    suspend fun deletePageBookmark(id: Int)

    // --- ReadingHistory Queries ---
    @Query("SELECT * FROM reading_history ORDER BY timestamp DESC")
    fun getAllReadingHistory(): Flow<List<ReadingHistory>>

    @Query("SELECT * FROM reading_history WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getReadingHistorySince(sinceTimestamp: Long): Flow<List<ReadingHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(readingHistory: ReadingHistory): Long

    @Query("SELECT * FROM reading_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastHistoryEntry(): ReadingHistory?

    @Query("DELETE FROM reading_history")
    suspend fun clearAllHistory()
}
