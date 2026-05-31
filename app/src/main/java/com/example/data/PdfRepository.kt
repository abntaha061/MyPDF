package com.example.data

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PdfRepository(private val context: Context, private val pdfDao: PdfDao) {

    val allFiles: Flow<List<PdfFile>> = pdfDao.getAllFiles()
    val recentFiles: Flow<List<PdfFile>> = pdfDao.getRecentFiles()
    val bookmarkedFiles: Flow<List<PdfFile>> = pdfDao.getBookmarkedFiles()
    val allFolders: Flow<List<String>> = pdfDao.getAllFolders()

    // --- NEW: Bookmarks and Reading History Flows & Helpers ---
    val allPageBookmarks: Flow<List<PageBookmark>> = pdfDao.getAllPageBookmarks()
    val allReadingHistory: Flow<List<ReadingHistory>> = pdfDao.getAllReadingHistory()

    fun getPageBookmarksForFile(pdfFileId: Int): Flow<List<PageBookmark>> = pdfDao.getPageBookmarksForFile(pdfFileId)
    
    suspend fun insertPageBookmark(bookmark: PageBookmark): Long = withContext(Dispatchers.IO) {
        pdfDao.insertPageBookmark(bookmark)
    }

    suspend fun deletePageBookmark(id: Int) = withContext(Dispatchers.IO) {
        pdfDao.deletePageBookmark(id)
    }

    fun getReadingHistorySince(sinceTimestamp: Long): Flow<List<ReadingHistory>> = pdfDao.getReadingHistorySince(sinceTimestamp)

    suspend fun insertHistoryEntry(pdfFileId: Int, pdfTitle: String, pageIndex: Int) = withContext(Dispatchers.IO) {
        val entry = ReadingHistory(pdfFileId = pdfFileId, pdfTitle = pdfTitle, pageIndex = pageIndex)
        pdfDao.insertHistoryEntry(entry)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        pdfDao.clearAllHistory()
    }

    fun getFilesByFolder(folder: String): Flow<List<PdfFile>> = pdfDao.getFilesByFolder(folder)
    fun searchFiles(query: String): Flow<List<PdfFile>> = pdfDao.searchFiles(query)

    suspend fun getFileById(id: Int): PdfFile? = pdfDao.getFileById(id)

    suspend fun insertFile(pdfFile: PdfFile): Long = withContext(Dispatchers.IO) {
        pdfDao.insertFile(pdfFile)
    }

    suspend fun updateFile(pdfFile: PdfFile) = withContext(Dispatchers.IO) {
        pdfDao.updateFile(pdfFile)
    }

    suspend fun toggleFavorite(pdfId: Int, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        pdfDao.getFileById(pdfId)?.let {
            pdfDao.updateFile(it.copy(isFavorite = isFavorite))
        }
    }

    suspend fun updateTags(pdfId: Int, tags: String) = withContext(Dispatchers.IO) {
        pdfDao.getFileById(pdfId)?.let {
            pdfDao.updateFile(it.copy(tags = tags))
        }
    }

    suspend fun updateCategory(pdfId: Int, category: String) = withContext(Dispatchers.IO) {
        pdfDao.getFileById(pdfId)?.let {
            pdfDao.updateFile(it.copy(category = category))
        }
    }

    suspend fun incrementCommentCount(pdfId: Int) = withContext(Dispatchers.IO) {
        pdfDao.getFileById(pdfId)?.let {
            pdfDao.updateFile(it.copy(commentCount = it.commentCount + 1))
        }
    }

    fun autoCategorize(title: String): String {
        val t = title.lowercase()
        return when {
            t.contains("book") || t.contains("novel") || t.contains("كتاب") || t.contains("رواية") || t.contains("guide") || t.contains("دليل") -> "كتب"
            t.contains("report") || t.contains("تقارير") || t.contains("تقرير") || t.contains("statis") || t.contains("تحليل") -> "تقارير"
            t.contains("test") || t.contains("exam") || t.contains("quiz") || t.contains("امتحان") || t.contains("اختبار") || t.contains("tips") -> "اختبارات"
            else -> "مستندات"
        }
    }

    suspend fun deleteFile(pdfFile: PdfFile) = withContext(Dispatchers.IO) {
        try {
            val file = File(pdfFile.filePath)
            if (file.exists() && pdfFile.filePath.startsWith(context.filesDir.absolutePath)) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("PdfRepository", "Error deleting physical file: ${e.message}")
        }
        pdfDao.deleteFile(pdfFile)
    }

    suspend fun updateBookmark(pdfId: Int, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        pdfDao.getFileById(pdfId)?.let {
            pdfDao.updateFile(it.copy(isBookmarked = isBookmarked))
        }
    }

    suspend fun moveFileToFolder(pdfId: Int, folderName: String) = withContext(Dispatchers.IO) {
        pdfDao.getFileById(pdfId)?.let {
            pdfDao.updateFile(it.copy(folderName = folderName))
        }
    }

    suspend fun updateReadingProgress(pdfId: Int, currentPage: Int) = withContext(Dispatchers.IO) {
        pdfDao.getFileById(pdfId)?.let {
            pdfDao.updateFile(it.copy(
                currentPage = currentPage,
                lastReadDate = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Copy an external PDF file picked by the user into internal files storage
     * and save its metadata in the database.
     */
    suspend fun importPdfFile(uri: Uri): PdfFile? = withContext(Dispatchers.IO) {
        try {
            var fileName = "imported_${System.currentTimeMillis()}.pdf"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            // Ensure unique filename inside app files dir
            var destinationFile = File(context.filesDir, fileName)
            var count = 1
            while (destinationFile.exists()) {
                val nameWithoutExt = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".", "pdf")
                destinationFile = File(context.filesDir, "$nameWithoutExt($count).$ext")
                count++
            }

            // Copy file stream
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                FileOutputStream(destinationFile).use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
                inputStream.close()
            } else {
                return@withContext null
            }

            if (fileSize == 0L) {
                fileSize = destinationFile.length()
            }

            // Read total pages using system PdfRenderer safely
            var totalPages = 1
            try {
                val parcelFileDescriptor = android.os.ParcelFileDescriptor.open(
                    destinationFile,
                    android.os.ParcelFileDescriptor.MODE_READ_ONLY
                )
                val renderer = android.graphics.pdf.PdfRenderer(parcelFileDescriptor)
                totalPages = renderer.pageCount
                renderer.close()
                parcelFileDescriptor.close()
            } catch (e: Exception) {
                Log.e("PdfRepository", "Could not count pages of imported file: ${e.message}")
            }

            val titleText = destinationFile.name.substringBeforeLast(".")
            val newPdf = PdfFile(
                filePath = destinationFile.absolutePath,
                title = titleText,
                fileSize = fileSize,
                addedDate = System.currentTimeMillis(),
                lastReadDate = 0L,
                currentPage = 0,
                totalPages = totalPages,
                isBookmarked = false,
                folderName = "Imports", // Default category folder
                category = autoCategorize(titleText)
            )

            val id = pdfDao.insertFile(newPdf)
            return@withContext newPdf.copy(id = id.toInt())
        } catch (e: Exception) {
            Log.e("PdfRepository", "Failed to import file: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Pre-populates default sample PDFs inside internal storage if empty
     * using the built-in Android PdfDocument graphics engine, then seeds database.
     */
    suspend fun initializeSamplesIfNeeded() = withContext(Dispatchers.IO) {
        val count = pdfDao.getAllFiles().first().size
        if (count > 0) return@withContext

        Log.d("PdfRepository", "Initializing sample documents on default load...")

        // Save samples to files directory
        val enGuidePath = generateSystemSamplePdf(
            fileName = "WPS_PDF_Reader_Guide_EN.pdf",
            title = "WPS PDF Reader - English Guide",
            lang = "EN"
        )
        val arGuidePath = generateSystemSamplePdf(
            fileName = "WPS_PDF_Reader_Guide_AR.pdf",
            title = "WPS PDF - قارئ المستندات دليل البدء",
            lang = "AR"
        )
        val tipsPath = generateSystemSamplePdf(
            fileName = "Android_Productivity_Tips.pdf",
            title = "Android Productivity Suite",
            lang = "DE"
        )

        // Seed DB with English Sample
        enGuidePath?.let {
            pdfDao.insertFile(PdfFile(
                filePath = it.absolutePath,
                title = "WPS Reader QuickStart Guide",
                fileSize = it.length(),
                addedDate = System.currentTimeMillis() - 3600000, // 1 hour ago
                lastReadDate = System.currentTimeMillis() - 1800000, // active
                currentPage = 0,
                totalPages = 3,
                isBookmarked = true,
                folderName = "Guides",
                isSample = true
            ))
        }

        // Seed DB with Arabic Sample
        arGuidePath?.let {
            pdfDao.insertFile(PdfFile(
                filePath = it.absolutePath,
                title = "دليل البدء السريع بالعربية",
                fileSize = it.length(),
                addedDate = System.currentTimeMillis() - 1800000,
                lastReadDate = System.currentTimeMillis() - 900000,
                currentPage = 0,
                totalPages = 3,
                isBookmarked = false,
                folderName = "المستندات_العربية",
                isSample = true
            ))
        }

        // Seed DB with Tips Sample
        tipsPath?.let {
            pdfDao.insertFile(PdfFile(
                filePath = it.absolutePath,
                title = "Android Productivity Tips",
                fileSize = it.length(),
                addedDate = System.currentTimeMillis(),
                lastReadDate = 0L,
                currentPage = 0,
                totalPages = 3,
                isBookmarked = false,
                folderName = "Library",
                isSample = true
            ))
        }
    }

    private fun generateSystemSamplePdf(fileName: String, title: String, lang: String): File? {
        val file = File(context.filesDir, fileName)
        if (file.exists()) return file

        val document = PdfDocument()

        for (pageIdx in 1..3) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIdx).create() // A4 Size 595x842
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Paint standard white sheet
            canvas.drawColor(Color.WHITE)

            val paint = Paint().apply {
                isAntiAlias = true
            }

            // WPS blue header ribbon
            paint.color = Color.parseColor("#1565C0")
            canvas.drawRect(0f, 0f, 595f, 55f, paint)

            paint.color = Color.WHITE
            paint.textSize = 16f
            paint.isFakeBoldText = true

            if (lang == "AR") {
                canvas.drawText(title, 40f, 35f, paint)
                canvas.drawText("صفحة $pageIdx من 3", 470f, 35f, paint)
            } else {
                canvas.drawText(title, 40f, 35f, paint)
                canvas.drawText("Page $pageIdx of 3", 470f, 35f, paint)
            }

            // Document body layout title
            paint.color = Color.parseColor("#1E1E1E")
            paint.textSize = 22f
            paint.isFakeBoldText = true
            canvas.drawText(if (pageIdx == 1) "WPS PDF READER" else "Section Detail $pageIdx", 40f, 105f, paint)

            // Divider strip
            paint.color = Color.parseColor("#D5D5D5")
            canvas.drawRect(40f, 120f, 555f, 122f, paint)

            paint.textSize = 13f
            paint.isFakeBoldText = false
            paint.color = Color.BLACK

            var y = 160f

            val sentences = when (lang) {
                "AR" -> when (pageIdx) {
                    1 -> listOf(
                        "مرحبًا بك في تطبيق قارئ ملفات PDF من طراز WPS.",
                        "هذا المستند عبارة عن ملف PDF حقيقي تم إنشاؤه مباشرة على جهازك.",
                        "يقرأه التطبيق بانتظام باستخدام مصنف صفحات نظام Android.",
                        "",
                        "دليل مميزات الواجهة:",
                        "• شريط الأدوات العلوي: يحتوي على أيقونة القائمة الجانبية وإجراءات سريعة.",
                        "• التبويبات السفلية: لتسهيل تقسيم الوقت والأماكن وملفات البحث.",
                        "• تجربة تمرير رائعة: يدعم التمرير الرأسي والأفقي السلس.",
                        "• قاعدة بيانات لحفظ التقدم: يتذكر التطبيق آخر صفحة توقفت عندها."
                    )
                    2 -> listOf(
                        "ترتيب الملفات بالخدمة المحلية والمصنفات:",
                        "• شاشة المكتبة: تتيح لك ترتيب الملفات داخل تصنيفات ومجلدات مستقلة.",
                        "• التصفية والبحث الفوري: يتم تحديث النتائج ديناميكيًا حسب الحروف والكلمات.",
                        "• المشاركة السريعة: إمكانية مشاركة الملفات عبر البريد أو تطبيقات التواصل.",
                        "",
                        "خيارات العرض والقراءة الذكية:",
                        "• يدعم التطبيق قلب الألوان ليلاً لحظر الإجهاد البصري تماماً.",
                        "• تمكين عدم إيقاف الشاشة أثناء تصفح المستند لتعزيز تجربة الدراسة.",
                        "• التبديل السهل بين لغات الواجهة بنقرة واحدة."
                    )
                    else -> listOf(
                        "تفاصيل المحرك والأداء البرمجي للبلاتفورم:",
                        "• تم بناء العارض باستخدام Coroutines و Room Database للحفاظ على استهلاك البطارية.",
                        "• توافق كامل مع تصاميم Material 3 والزوايا المريحة للمكونات الدائرية.",
                        "• دعم التجاوب مع مقاسات الهواتف المتنوعة والتابلت والوضع العرضي.",
                        "",
                        "شكرًا جزيلًا لقراءة هذا الاختبار التشغيلي!",
                        "نتمنى لك وقتًا مثمرًا وقراءة مريحة للمستندات."
                    )
                }
                "DE" -> when (pageIdx) {
                    1 -> listOf(
                        "Willkommen beim WPS-konformen PDF-Produkt-Reader.",
                        "Dies ist ein echtes, zur Laufzeit generiertes PDF-Dokument.",
                        "Der native Android-Dienst (PdfRenderer) berechnet alle Vektoren.",
                        "",
                        "Hauptfunktionen auf einen Blick:",
                        "• Intelligentes Lese-Erlebnis mit vertikalem Scrollen.",
                        "• Invertierter Nachtmodus: Schont Ihre Augen in dunkler Umgebung.",
                        "• Einfache Lesezeichenverwaltung für wichtige Buchseiten.",
                        "• Kategorien-Ordner in der Bibliothek erstellen und befüllen.",
                        "• Vollständige RTL-Unterstützung für Arabisch und LTR für Englisch/Deutsch."
                    )
                    2 -> listOf(
                        "Bibliotheksstruktur und Dateiorganisation:",
                        "• Erstellen Sie personalisierte Ordner zur Sortierung Ihrer Berichte.",
                        "• Schneller Import echter PDFs directly von Ihrem internen Speicher.",
                        "• Vollständige Suche über Dateinamen im Echtzeit-Datenstrom.",
                        "",
                        "Erweiterte Lese-Einstellungen im Sidebar-Menü:",
                        "• Wählen Sie Ihre bevorzugte Ausrichtung (Anbreite, Anpassung).",
                        "• Nutzen Sie den integrierten System-Share zur Weiterleitung von PDFs.",
                        "• Verfolgen Sie Ihren Lesefortschritt automatisch (Fortschrittsanzeige)."
                    )
                    else -> listOf(
                        "Technische Implementierungsdaten im Detail:",
                        "• Asynchrone Datenbankvorgänge schützen den Rendering-Prozess im UI.",
                        "• Vollständige Einbettung sicherer Notch- und Safe-Drawing Insets.",
                        "• Entwickelt unter Einhaltung des modernen MVVM-Architekturmusters.",
                        "",
                        "Vielen Dank, dass Sie den WPS PDF-Reader testen!",
                        "Entwickelt für Stabilität und herausragende Performance."
                    )
                }
                else -> when (pageIdx) {
                    1 -> listOf(
                        "Welcome to the WPS-compliant PDF workspace guide.",
                        "This is a formal, system-generated PDF document.",
                        "Pages are rendered on demand by Android's internal PdfRenderer.",
                        "",
                        "Key Interface Features:",
                        "• Superior Toolbar Actions: Access Drawer, File Properties, and Sharing.",
                        "• Navigation Convenience: Smooth tab swapping or sidebar jumps.",
                        "• Dynamic Reader progress saving: The app remembers your last page.",
                        "• Invert Canvas Filter: Switch colors to soft eye-comfort dark canvas.",
                        "• Multi-lingual interface: Built-in localizations for Arabic, English, and German."
                    )
                    2 -> listOf(
                        "WPS Workspace Organization & Categories:",
                        "• Library view lets you create workspace folders like Work, Study, and receipts.",
                        "• Filter instantly with search triggers, sorting keys, and bookmarks filters.",
                        "• Single tap anywhere on the reading view to show/hide context controls.",
                        "",
                        "Responsive design across sizes:",
                        "• Fully optimized for multi-window folding modes and tablets.",
                        "• Standard touch gestures supported for fast paging."
                    )
                    else -> listOf(
                        "Architectural & Optimization Guidelines:",
                        "• Developed natively under Material Design 3 regulations with curved corners.",
                        "• Supported with full-bleed Edge-to-Edge window structures.",
                        "• SQLite Room database storage keeps reading indices persistent permanently.",
                        "",
                        "Thank you for evaluating the WPS PDF Reader!",
                        "Created with meticulous attention to mobile productivity."
                    )
                }
            }

            for (s in sentences) {
                canvas.drawText(s, 40f, y, paint)
                y += 24f
            }

            // Footer indicator
            paint.color = Color.parseColor("#999999")
            paint.textSize = 10f
            if (lang == "AR") {
                canvas.drawText("قارئ مستندات WPS • نظام تشغيل أندرويد المستقر", 40f, 800f, paint)
            } else {
                canvas.drawText("WPS PDF Workspace • Verified Android System Output", 40f, 800f, paint)
            }

            document.finishPage(page)
        }

        try {
            val out = FileOutputStream(file)
            document.writeTo(out)
            out.close()
            return file
        } catch (e: Exception) {
            Log.e("PdfRepository", "Error writing sample document: ${e.message}")
            return null
        } finally {
            document.close()
        }
    }
}
