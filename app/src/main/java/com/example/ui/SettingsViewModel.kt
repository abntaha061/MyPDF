package com.example.ui

import android.app.Activity
import android.app.Application
import android.view.WindowManager
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Mock file class for Privacy section
data class SecurableFile(
    val id: String,
    val name: String,
    val size: String,
    val isLocked: Boolean,
    val passwordHash: String = ""
)

// Language Pack item
data class LanguagePack(
    val code: String,
    val name: String,
    val size: String,
    val progress: Float, // 0.0 to 1.0, 1.0 means installed
    val isDownloading: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    // Flow states
    private val _theme = MutableStateFlow(repository.getTheme())
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _brightness = MutableStateFlow(repository.getBrightness())
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _font = MutableStateFlow(repository.getFont())
    val font: StateFlow<String> = _font.asStateFlow()

    private val _fontSize = MutableStateFlow(repository.getFontSize())
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _lineSpacing = MutableStateFlow(repository.getLineSpacing())
    val lineSpacing: StateFlow<String> = _lineSpacing.asStateFlow()

    private val _scrollDirection = MutableStateFlow(repository.getScrollDirection())
    val scrollDirection: StateFlow<String> = _scrollDirection.asStateFlow()

    private val _pageMode = MutableStateFlow(repository.getPageMode())
    val pageMode: StateFlow<String> = _pageMode.asStateFlow()

    private val _fitMode = MutableStateFlow(repository.getFitMode())
    val fitMode: StateFlow<String> = _fitMode.asStateFlow()

    private val _autoScrollEnabled = MutableStateFlow(repository.isAutoScrollEnabled())
    val autoScrollEnabled: StateFlow<Boolean> = _autoScrollEnabled.asStateFlow()

    private val _autoScrollSpeed = MutableStateFlow(repository.getAutoScrollSpeed())
    val autoScrollSpeed: StateFlow<Float> = _autoScrollSpeed.asStateFlow()

    private val _sourceLang = MutableStateFlow(repository.getSourceLang())
    val sourceLang: StateFlow<String> = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow(repository.getTargetLang())
    val targetLang: StateFlow<String> = _targetLang.asStateFlow()

    private val _ttsSpeed = MutableStateFlow(repository.getTtsSpeed())
    val ttsSpeed: StateFlow<Float> = _ttsSpeed.asStateFlow()

    private val _readerVoice = MutableStateFlow(repository.getReaderVoice())
    val readerVoice: StateFlow<String> = _readerVoice.asStateFlow()

    private val _highlightColor = MutableStateFlow(repository.getHighlightColor())
    val highlightColor: StateFlow<String> = _highlightColor.asStateFlow()

    private val _penThickness = MutableStateFlow(repository.getPenThickness())
    val penThickness: StateFlow<Float> = _penThickness.asStateFlow()

    private val _annotationOpacity = MutableStateFlow(repository.getAnnotationOpacity())
    val annotationOpacity: StateFlow<Float> = _annotationOpacity.asStateFlow()

    private val _highContrastEnabled = MutableStateFlow(repository.isHighContrastEnabled())
    val highContrastEnabled: StateFlow<Boolean> = _highContrastEnabled.asStateFlow()

    private val _largeTargetsEnabled = MutableStateFlow(repository.isLargeTargetsEnabled())
    val largeTargetsEnabled: StateFlow<Boolean> = _largeTargetsEnabled.asStateFlow()

    private val _talkbackSimEnabled = MutableStateFlow(repository.isTalkbackSimEnabled())
    val talkbackSimEnabled: StateFlow<Boolean> = _talkbackSimEnabled.asStateFlow()

    private val _reduceMotionEnabled = MutableStateFlow(repository.isReduceMotionEnabled())
    val reduceMotionEnabled: StateFlow<Boolean> = _reduceMotionEnabled.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(repository.isAppLockEnabled())
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _appLockPIN = MutableStateFlow(repository.getAppLockPIN())
    val appLockPIN: StateFlow<String> = _appLockPIN.asStateFlow()

    private val _screenSecurityEnabled = MutableStateFlow(repository.isScreenSecurityEnabled())
    val screenSecurityEnabled: StateFlow<Boolean> = _screenSecurityEnabled.asStateFlow()

    // Interactive custom state variables for simulation
    val filesList = mutableStateListOf<SecurableFile>(
        SecurableFile("1", "كتاب_تعليم_الالمانية_الذكي.pdf", "4.2 MB", false),
        SecurableFile("2", "تقرير_مبيعات_الشركة_2025.pdf", "18.5 MB", true, "1234"),
        SecurableFile("3", "رواية_عبر_الصحراء_مصححة.pdf", "2.1 MB", false)
    )

    val languagePacks = mutableStateListOf<LanguagePack>(
        LanguagePack("ar", "العربية (Offline Pack)", "38 MB", 1.0f),
        LanguagePack("en", "الإنجليزية (Offline Pack)", "42 MB", 1.0f),
        LanguagePack("de", "الألمانية (Offline Pack)", "51 MB", 0.0f),
        LanguagePack("fr", "الفرنسية (Offline Pack)", "45 MB", 0.0f)
    )

    private val _talkbackSimulationOutput = MutableStateFlow("")
    val talkbackSimulationOutput: StateFlow<String> = _talkbackSimulationOutput.asStateFlow()

    private var downloadJobs = mutableMapOf<String, Job>()

    // Apply Screen Security State and Screen Brightness back to Activity
    fun applyActivitySpecificSettings(activity: Activity) {
        val secure = _screenSecurityEnabled.value
        val bVal = _brightness.value

        activity.runOnUiThread {
            // Apply Secure Window (prevents screenshot & recent app view)
            if (secure) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            // Apply custom brightness
            val lp = activity.window.attributes
            lp.screenBrightness = bVal.coerceIn(0.01f, 1.0f)
            activity.window.attributes = lp
        }
    }

    // Interactive simulated Talkback Voice Over
    fun simulateTalkback(elementArabicDescription: String) {
        if (_talkbackSimEnabled.value) {
            _talkbackSimulationOutput.value = "القارئ التلقائي: تم الضغط على $elementArabicDescription"
            viewModelScope.launch {
                delay(2500)
                if (_talkbackSimulationOutput.value == "القارئ التلقائي: تم الضغط على $elementArabicDescription") {
                    _talkbackSimulationOutput.value = ""
                }
            }
        }
    }

    // Language pack simulated downloading
    fun downloadLanguagePack(code: String) {
        val index = languagePacks.indexOfFirst { it.code == code }
        if (index != -1 && !languagePacks[index].isDownloading) {
            val oldPack = languagePacks[index]
            languagePacks[index] = oldPack.copy(isDownloading = true, progress = 0.01f)

            val job = viewModelScope.launch {
                var currentProgress = 0.01f
                while (currentProgress < 1.0f) {
                    delay(300)
                    currentProgress += 0.15f
                    if (currentProgress >= 1.0f) {
                        currentProgress = 1.0f
                    }
                    val currentIdx = languagePacks.indexOfFirst { it.code == code }
                    if (currentIdx != -1) {
                        languagePacks[currentIdx] = languagePacks[currentIdx].copy(progress = currentProgress)
                    }
                }
                val finalIdx = languagePacks.indexOfFirst { it.code == code }
                if (finalIdx != -1) {
                    languagePacks[finalIdx] = languagePacks[finalIdx].copy(progress = 1.0f, isDownloading = false)
                }
                downloadJobs.remove(code)
            }
            downloadJobs[code] = job
        }
    }

    fun deleteLanguagePack(code: String) {
        val index = languagePacks.indexOfFirst { it.code == code }
        if (index != -1) {
            downloadJobs[code]?.cancel()
            downloadJobs.remove(code)
            languagePacks[index] = languagePacks[index].copy(progress = 0.0f, isDownloading = false)
        }
    }

    // Simulated file locking operations
    fun lockFile(id: String, pass: String) {
        val index = filesList.indexOfFirst { it.id == id }
        if (index != -1) {
            filesList[index] = filesList[index].copy(isLocked = true, passwordHash = pass)
        }
    }

    fun unlockFile(id: String, pass: String): Boolean {
        val index = filesList.indexOfFirst { it.id == id }
        if (index != -1) {
            val file = filesList[index]
            if (file.passwordHash == pass) {
                filesList[index] = file.copy(isLocked = false, passwordHash = "")
                return true
            }
        }
        return false
    }

    // Setters
    fun updateTheme(newTheme: String, activity: Activity) {
        _theme.value = newTheme
        repository.setTheme(newTheme)
        simulateTalkback("تغيير المظهر إلى ${getThemeNameArabic(newTheme)}")
    }

    fun updateBrightness(v: Float, activity: Activity) {
        _brightness.value = v
        repository.setBrightness(v)
        applyActivitySpecificSettings(activity)
    }

    fun updateFont(newFont: String) {
        _font.value = newFont
        repository.setFont(newFont)
        simulateTalkback("تغيير الخط لتطبيق: $newFont")
    }

    fun updateFontSize(newSize: Float) {
        _fontSize.value = newSize
        repository.setFontSize(newSize)
    }

    fun updateLineSpacing(spacing: String) {
        _lineSpacing.value = spacing
        repository.setLineSpacing(spacing)
        simulateTalkback("تغيير تباعد الأسطر إلى ${getLineSpacingNameArabic(spacing)}")
    }

    fun updateScrollDirection(dir: String) {
        _scrollDirection.value = dir
        repository.setScrollDirection(dir)
        simulateTalkback("تغيير اتجاه التمرير إلى ${if (dir == "vertical") "العمودي" else "الأفقي"}")
    }

    fun updatePageMode(mode: String) {
        _pageMode.value = mode
        repository.setPageMode(mode)
        simulateTalkback("تغيير وضع الصفحة إلى ${getPageModeNameArabic(mode)}")
    }

    fun updateFitMode(mode: String) {
        _fitMode.value = mode
        repository.setFitMode(mode)
        simulateTalkback("تغيير تكييف الحجم إلى ${getFitModeNameArabic(mode)}")
    }

    fun updateAutoScrollEnabled(enabled: Boolean) {
        _autoScrollEnabled.value = enabled
        repository.setAutoScrollEnabled(enabled)
        simulateTalkback("${if (enabled) "تفعيل" else "تعطيل"} التمرير التلقائي")
    }

    fun updateAutoScrollSpeed(speed: Float) {
        _autoScrollSpeed.value = speed
        repository.setAutoScrollSpeed(speed)
    }

    fun updateSourceLang(lang: String) {
        _sourceLang.value = lang
        repository.setSourceLang(lang)
        simulateTalkback("تغيير اللغة المصدر الافتراضية إلى ${getLangNameArabic(lang)}")
    }

    fun updateTargetLang(lang: String) {
        _targetLang.value = lang
        repository.setTargetLang(lang)
        simulateTalkback("تغيير لغة الترجمة المستهدفة إلى ${getLangNameArabic(lang)}")
    }

    fun updateTtsSpeed(speed: Float) {
        _ttsSpeed.value = speed
        repository.setTtsSpeed(speed)
    }

    fun updateReaderVoice(voice: String) {
        _readerVoice.value = voice
        repository.setReaderVoice(voice)
        simulateTalkback("اختيار صوت القارئ: ${if (voice == "male") "ذكر" else "أنثى"}")
    }

    fun updateHighlightColor(colorHex: String) {
        _highlightColor.value = colorHex
        repository.setHighlightColor(colorHex)
    }

    fun updatePenThickness(thick: Float) {
        _penThickness.value = thick
        repository.setPenThickness(thick)
    }

    fun updateAnnotationOpacity(opacity: Float) {
        _annotationOpacity.value = opacity
        repository.setAnnotationOpacity(opacity)
    }

    fun updateHighContrast(enabled: Boolean, activity: Activity) {
        _highContrastEnabled.value = enabled
        repository.setHighContrastEnabled(enabled)
        if (enabled) {
            updateTheme("high_contrast", activity)
        } else {
            updateTheme("system", activity)
        }
        simulateTalkback("${if (enabled) "تفعيل" else "تعطيل"} وضع التباين العالي")
    }

    fun updateLargeTargets(enabled: Boolean) {
        _largeTargetsEnabled.value = enabled
        repository.setLargeTargetsEnabled(enabled)
        simulateTalkback("${if (enabled) "تفعيل" else "تعطيل"} المؤشر والأزرار الكبيرة")
    }

    fun updateTalkbackSim(enabled: Boolean) {
        _talkbackSimEnabled.value = enabled
        repository.setTalkbackSimEnabled(enabled)
        _talkbackSimulationOutput.value = if (enabled) "تم تفعيل محاكي TalkBack! اضغط على أي زر لتشاهد نطق المساعد." else ""
    }

    fun updateReduceMotion(enabled: Boolean) {
        _reduceMotionEnabled.value = enabled
        repository.setReduceMotionEnabled(enabled)
        simulateTalkback("${if (enabled) "تفعيل" else "تعطيل"} وضع تقليل الحركة")
    }

    fun updateAppLockEnabled(enabled: Boolean, pin: String = "1234") {
        _appLockEnabled.value = enabled
        repository.setAppLockEnabled(enabled)
        repository.setAppLockPIN(pin)
        _appLockPIN.value = pin
        simulateTalkback("${if (enabled) "تفعيل" else "تعطيل"} قفل التطبيق برقم سري")
    }

    fun updateScreenSecurity(enabled: Boolean, activity: Activity) {
        _screenSecurityEnabled.value = enabled
        repository.setScreenSecurityEnabled(enabled)
        applyActivitySpecificSettings(activity)
        simulateTalkback("${if (enabled) "تفعيل" else "تعطيل"} حماية الشاشة من لقطات الشاشة وقائمة التطبيقات الأخيرة")
    }

    // Readable Names Converters
    private fun getThemeNameArabic(themeKey: String) = when (themeKey) {
        "light" -> "فاتح (نهاري)"
        "dark" -> "داكن (ليلي)"
        "sepia" -> "سيبيا (ألوان مريحة للعين)"
        "high_contrast" -> "تباين عالي"
        else -> "الوضع الافتراضي للنظام"
    }

    private fun getLineSpacingNameArabic(spacingKey: String) = when (spacingKey) {
        "tight" -> "ضيق"
        "wide" -> "واسع"
        else -> "عادي"
    }

    private fun getPageModeNameArabic(modeKey: String) = when (modeKey) {
        "single" -> "صفحة صفحة"
        "book" -> "وضع كتاب"
        else -> "مستمر"
    }

    private fun getFitModeNameArabic(fitKey: String) = when (fitKey) {
        "full_page" -> "الصفحة الكاملة"
        "custom" -> "حجم مخصص"
        else -> "ملاءمة العرض"
    }

    private fun getLangNameArabic(langKey: String) = when (langKey) {
        "auto" -> "تلقائي"
        "ar" -> "العربية"
        "de" -> "الألمانية"
        "en" -> "الإنجليزية"
        "fr" -> "الفرنسية"
        else -> langKey
    }
}
