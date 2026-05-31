package com.example.data

import kotlinx.coroutines.flow.Flow

class PdfRepository(private val pdfDao: PdfDao) {
    val allPdfs: Flow<List<PdfFile>> = pdfDao.getAllPdfs()
    val recentPdfs: Flow<List<PdfFile>> = pdfDao.getRecentPdfs()
    val favoritePdfs: Flow<List<PdfFile>> = pdfDao.getFavoritePdfs()
    val allFolders: Flow<List<Folder>> = pdfDao.getAllFolders()

    fun getPdfsInFolder(folderId: String): Flow<List<PdfFile>> = pdfDao.getPdfsInFolder(folderId)
    fun getBookmarksForPdf(pdfId: String): Flow<List<PageBookmark>> = pdfDao.getBookmarksForPdf(pdfId)

    suspend fun getPdfById(id: String): PdfFile? = pdfDao.getPdfById(id)

    suspend fun insertPdf(pdfFile: PdfFile) {
        pdfDao.insertPdf(pdfFile)
    }

    suspend fun updatePdf(pdfFile: PdfFile) {
        pdfDao.updatePdf(pdfFile)
    }

    suspend fun deletePdf(id: String) {
        pdfDao.deletePdfById(id)
    }

    suspend fun createFolder(folder: Folder) {
        pdfDao.insertFolder(folder)
    }

    suspend fun deleteFolder(folderId: String) {
        pdfDao.clearFolderIdInPdfs(folderId)
        pdfDao.deleteFolderById(folderId)
    }

    suspend fun movePdfToFolder(pdfId: String, folderId: String?) {
        pdfDao.getPdfById(pdfId)?.let { pdf ->
            pdfDao.insertPdf(pdf.copy(folderId = folderId))
        }
    }

    suspend fun setFavorite(pdfId: String, isFavorite: Boolean) {
        pdfDao.getPdfById(pdfId)?.let { pdf ->
            pdfDao.insertPdf(pdf.copy(isFavorite = isFavorite))
        }
    }

    suspend fun updateReadingProgress(pdfId: String, page: Int) {
        pdfDao.getPdfById(pdfId)?.let { pdf ->
            pdfDao.updatePdf(pdf.copy(
                lastReadPage = page,
                lastReadDate = System.currentTimeMillis()
            ))
        }
    }

    suspend fun addBookmark(pdfId: String, pageIndex: Int, note: String?) {
        pdfDao.insertBookmark(PageBookmark(pdfId = pdfId, pageIndex = pageIndex, note = note))
    }

    suspend fun deleteBookmark(pdfId: String, pageIndex: Int) {
        pdfDao.deleteBookmark(pdfId, pageIndex)
    }
}
