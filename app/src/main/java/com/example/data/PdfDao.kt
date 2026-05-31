package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_files ORDER BY addedDate DESC")
    fun getAllPdfs(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdf_files WHERE lastReadDate > 0 ORDER BY lastReadDate DESC")
    fun getRecentPdfs(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdf_files WHERE isFavorite = 1 ORDER BY addedDate DESC")
    fun getFavoritePdfs(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdf_files WHERE folderId = :folderId ORDER BY addedDate DESC")
    fun getPdfsInFolder(folderId: String): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdf_files WHERE id = :id")
    suspend fun getPdfById(id: String): PdfFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdfFile: PdfFile)

    @Update
    suspend fun updatePdf(pdfFile: PdfFile)

    @Query("DELETE FROM pdf_files WHERE id = :id")
    suspend fun deletePdfById(id: String)

    // Folder Queries
    @Query("SELECT * FROM folders ORDER BY createdDate DESC")
    fun getAllFolders(): Flow<List<Folder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String)

    @Query("UPDATE pdf_files SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolderIdInPdfs(folderId: String)

    // Bookmarks Queries
    @Query("SELECT * FROM page_bookmarks WHERE pdfId = :pdfId ORDER BY pageIndex ASC")
    fun getBookmarksForPdf(pdfId: String): Flow<List<PageBookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: PageBookmark)

    @Query("DELETE FROM page_bookmarks WHERE pdfId = :pdfId AND pageIndex = :pageIndex")
    suspend fun deleteBookmark(pdfId: String, pageIndex: Int)
}
