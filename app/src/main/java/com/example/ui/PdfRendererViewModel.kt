package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PdfRendererViewModel(application: Application) : AndroidViewModel(application) {
    private val pdfDao = AppDatabase.getDatabase(application).pdfDao()
    val repository = PdfRepository(pdfDao)
    val geminiService = GeminiService()
    val dictionaryManager = DictionaryManager(geminiService)

    // Flows
    val allPdfs: StateFlow<List<PdfFile>> = repository.allPdfs.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        emptyList()
    )
    val recentPdfs: StateFlow<List<PdfFile>> = repository.recentPdfs.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        emptyList()
    )
    val favoritePdfs: StateFlow<List<PdfFile>> = repository.favoritePdfs.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        emptyList()
    )
    val allFolders: StateFlow<List<Folder>> = repository.allFolders.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        emptyList()
    )

    // Active reading PDF
    private val _activePdf = MutableStateFlow<PdfFile?>(null)
    val activePdf: StateFlow<PdfFile?> = _activePdf.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _pageBitmap = MutableStateFlow<Bitmap?>(null)
    val pageBitmap: StateFlow<Bitmap?> = _pageBitmap.asStateFlow()

    private val _isLoadingPage = MutableStateFlow(false)
    val isLoadingPage: StateFlow<Boolean> = _isLoadingPage.asStateFlow()

    // Bookmarks for active PDF
    private val _activeBookmarks = MutableStateFlow<List<PageBookmark>>(emptyList())
    val activeBookmarks: StateFlow<List<PageBookmark>> = _activeBookmarks.asStateFlow()

    // Gemini states
    private val _geminiResponse = MutableStateFlow<String?>(null)
    val geminiResponse: StateFlow<String?> = _geminiResponse.asStateFlow()

    private val _isGeminiLoading = MutableStateFlow(false)
    val isGeminiLoading: StateFlow<Boolean> = _isGeminiLoading.asStateFlow()

    // Dictionary states
    private val _dictionaryEntry = MutableStateFlow<DictionaryEntry?>(null)
    val dictionaryEntry: StateFlow<DictionaryEntry?> = _dictionaryEntry.asStateFlow()

    private val _isDictLoading = MutableStateFlow(false)
    val isDictLoading: StateFlow<Boolean> = _isDictLoading.asStateFlow()

    // App Preferences (API Key, Theme, Language)
    private val sharedPref = application.getSharedPreferences("wps_pdf_prefs", Context.MODE_PRIVATE)

    private val _geminiApiKey = MutableStateFlow(sharedPref.getString("gemini_key", "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _appLanguage = MutableStateFlow(sharedPref.getString("app_lang", "en") ?: "en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _appTheme = MutableStateFlow(sharedPref.getString("app_theme", "slate") ?: "slate")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    // Internal renderer fields
    private var activePdfRenderer: PdfRenderer? = null
    private var activeFileDescriptor: ParcelFileDescriptor? = null

    init {
        // Collect bookmarks when active PDF changes
        viewModelScope.launch {
            _activePdf.collectLatest { pdf ->
                if (pdf != null) {
                    repository.getBookmarksForPdf(pdf.id).collect {
                        _activeBookmarks.value = it
                    }
                } else {
                    _activeBookmarks.value = emptyList()
                }
            }
        }
        preloadSamplePdfs()
    }

    private fun preloadSamplePdfs() {
        viewModelScope.launch {
            repository.allPdfs.first().let { currentList ->
                if (currentList.isEmpty()) {
                    val context = getApplication<Application>().applicationContext
                    
                    // Create default folder "WPS Guides & Manuals"
                    val sampleFolderId = "guide_folder"
                    repository.createFolder(Folder(id = sampleFolderId, name = "WPS Guides & Manuals"))

                    val samples = listOf(
                        Triple("WPS Office QuickStart Guide", "wps_guide.pdf", 10),
                        Triple("Deep Learning Academic Paper", "deep_learning.pdf", 5),
                        Triple("Business Annual Growth Report 2026", "annual_report.pdf", 12)
                    )

                    samples.forEach { (title, filename, pages) ->
                        val localFile = File(context.filesDir, filename)
                        if (!localFile.exists()) {
                            localFile.writeText("Preloaded SIMULATED PDF:\nTitle: $title\nTotal Pages: $pages")
                        }
                        
                        val pdf = PdfFile(
                            id = filename,
                            title = title,
                            filePath = localFile.absolutePath,
                            size = localFile.length(),
                            pageCount = pages,
                            folderId = sampleFolderId
                        )
                        repository.insertPdf(pdf)
                    }
                }
            }
        }
    }


    fun setGeminiKey(key: String) {
        sharedPref.edit().putString("gemini_key", key).apply()
        _geminiApiKey.value = key
    }

    fun setAppLanguage(lang: String) {
        sharedPref.edit().putString("app_lang", lang).apply()
        _appLanguage.value = lang
    }

    fun setAppTheme(theme: String) {
        sharedPref.edit().putString("app_theme", theme).apply()
        _appTheme.value = theme
    }

    // PDF Import & Select
    fun importPdfFromUri(uri: Uri, fileName: String, size: Long) {
        viewModelScope.launch {
            _isLoadingPage.value = true
            try {
                val context = getApplication<Application>().applicationContext
                val file = File(context.filesDir, "pdf_${UUID.randomUUID()}.pdf")
                
                // Copy stream to local private storage
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Retrieve page count using PdfRenderer temporarily
                var pages = 0
                try {
                    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fd)
                    pages = renderer.pageCount
                    renderer.close()
                    fd.close()
                } catch (e: Exception) {
                    Log.e("PdfRendererViewModel", "Failed counting pages: ${e.message}")
                }

                val pdfFile = PdfFile(
                    id = UUID.randomUUID().toString(),
                    title = fileName.removeSuffix(".pdf"),
                    filePath = file.absolutePath,
                    size = size,
                    pageCount = if (pages > 0) pages else 1
                )

                repository.insertPdf(pdfFile)
                openPdfForReading(pdfFile)
            } catch (e: Exception) {
                Log.e("PdfRendererViewModel", "Import failed: ${e.message}", e)
            } finally {
                _isLoadingPage.value = false
            }
        }
    }

    fun openPdfForReading(pdf: PdfFile) {
        viewModelScope.launch {
            closeActiveRenderer()
            _activePdf.value = pdf
            
            try {
                val file = File(pdf.filePath)
                if (file.exists()) {
                    activeFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    activePdfRenderer = PdfRenderer(activeFileDescriptor!!)
                    
                    // Restores last read page
                    val initialPage = if (pdf.lastReadPage in 0 until pdf.pageCount) pdf.lastReadPage else 0
                    _currentPage.value = initialPage
                    renderPage(initialPage)
                    
                    // Update recents database timestamp
                    repository.updateReadingProgress(pdf.id, initialPage)
                } else {
                    Log.e("PdfRendererViewModel", "File does not exist: ${pdf.filePath}")
                }
            } catch (e: Exception) {
                Log.e("PdfRendererViewModel", "Open PDF failed: ${e.message}", e)
                _pageBitmap.value = null
            }
        }
    }

    fun goToPage(pageIndex: Int) {
        val renderer = activePdfRenderer ?: return
        val pdf = _activePdf.value ?: return
        if (pageIndex in 0 until renderer.pageCount) {
            _currentPage.value = pageIndex
            renderPage(pageIndex)
            
            viewModelScope.launch {
                repository.updateReadingProgress(pdf.id, pageIndex)
            }
        }
    }

    private fun renderPage(pageIndex: Int) {
        val renderer = activePdfRenderer ?: return
        viewModelScope.launch {
            _isLoadingPage.value = true
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val page = renderer.openPage(pageIndex)
                    val width = page.width
                    val height = page.height
                    
                    val scale = 1200f / width.coerceAtLeast(1)
                    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
                    val targetHeight = (height * scale).toInt().coerceAtLeast(1)

                    val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bmp
                } catch (e: Exception) {
                    Log.e("PdfRendererViewModel", "Render page failed: ${e.message}", e)
                    null
                }
            }
            _pageBitmap.value = bitmap
            _isLoadingPage.value = false
        }
    }

    // Folders
    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(Folder(id = UUID.randomUUID().toString(), name = name))
        }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch {
            repository.deleteFolder(id)
        }
    }

    fun movePdfToFolder(pdfId: String, folderId: String?) {
        viewModelScope.launch {
            repository.movePdfToFolder(pdfId, folderId)
        }
    }

    fun toggleFavorite(pdfId: String, isFav: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(pdfId, isFav)
            // Update state flow if active
            _activePdf.value?.let { active ->
                if (active.id == pdfId) {
                    _activePdf.value = active.copy(isFavorite = isFav)
                }
            }
        }
    }

    // Bookmark
    fun toggleBookmarkCurrentPage(titleNote: String? = null) {
        val pdf = _activePdf.value ?: return
        val currentPageIndex = _currentPage.value
        val hasBookmark = _activeBookmarks.value.any { it.pageIndex == currentPageIndex }
        
        viewModelScope.launch {
            if (hasBookmark) {
                repository.deleteBookmark(pdf.id, currentPageIndex)
            } else {
                repository.addBookmark(pdf.id, currentPageIndex, titleNote)
            }
        }
    }

    // Gemini
    fun askGemini(customPrompt: String? = null, useSuggested: String? = null) {
        val pdf = _activePdf.value ?: return
        val currentPg = _currentPage.value
        
        viewModelScope.launch {
            _isGeminiLoading.value = true
            _geminiResponse.value = "Consulting Gemini AI..."
            
            val prompt = when {
                useSuggested == "summary" -> "Summarize the major points, layout context, or objectives of Page ${currentPg + 1} of the document titled '${pdf.title}'."
                useSuggested == "explain" -> "Examine and explain any sophisticated terminology typically found on Page ${currentPg + 1} of a document labeled '${pdf.title}'."
                useSuggested == "translate" -> "Translate the core elements of Page ${currentPg + 1} of the document '${pdf.title}' into the default user language (Arabic/German/English depending on context)."
                else -> "Document Title: '${pdf.title}', Active Page: ${currentPg + 1}.\nUser Question: $customPrompt"
            }

            val apiResponse = geminiService.getAiResponse(
                prompt = prompt,
                userCustomKey = _geminiApiKey.value.ifBlank { null }
            )
            _geminiResponse.value = apiResponse
            _isGeminiLoading.value = false
        }
    }

    // Dictionary
    fun lookupWord(word: String) {
        if (word.isBlank()) return
        viewModelScope.launch {
            _isDictLoading.value = true
            val entry = dictionaryManager.lookupWord(word, _geminiApiKey.value.ifBlank { null })
            _dictionaryEntry.value = entry
            _isDictLoading.value = false
        }
    }

    fun clearDictEntry() {
        _dictionaryEntry.value = null
    }

    private fun closeActiveRenderer() {
        try {
            activePdfRenderer?.close()
        } catch (e: Exception) {
            // Silence
        }
        try {
            activeFileDescriptor?.close()
        } catch (e: Exception) {
            // Silence
        }
        activePdfRenderer = null
        activeFileDescriptor = null
    }

    override fun onCleared() {
        super.onCleared()
        closeActiveRenderer()
    }
}
