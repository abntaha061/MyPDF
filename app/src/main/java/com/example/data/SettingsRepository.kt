package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("reader_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_THEME = "theme"
        const val KEY_BRIGHTNESS = "screen_brightness"
        const val KEY_FONT = "font"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_LINE_SPACING = "line_spacing"
        const val KEY_SCROLL_DIR = "scroll_direction"
        const val KEY_PAGE_MODE = "page_mode"
        const val KEY_FIT_MODE = "fit_mode"
        const val KEY_AUTO_SCROLL = "auto_scroll_enabled"
        const val KEY_AUTO_SCROLL_SPEED = "auto_scroll_speed"
        const val KEY_SOURCE_LANG = "source_lang"
        const val KEY_TARGET_LANG = "target_lang"
        const val KEY_TTS_SPEED = "tts_speed"
        const val KEY_READER_VOICE = "reader_voice"
        const val KEY_HIGHLIGHT_COLOR = "highlight_color"
        const val KEY_PEN_THICKNESS = "pen_thickness"
        const val KEY_ANNOTATION_OPACITY = "annotation_opacity"
        const val KEY_HIGH_CONTRAST = "high_contrast"
        const val KEY_LARGE_TARGETS = "large_targets"
        const val KEY_TALKBACK_SIMULATION = "talkback_sim"
        const val KEY_REDUCE_MOTION = "reduce_motion"
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_APP_LOCK_PIN = "app_lock_pin"
        const val KEY_SCREEN_SECURITY = "screen_security"
    }

    fun getTheme(): String = prefs.getString(KEY_THEME, "system") ?: "system"
    fun setTheme(theme: String) = prefs.edit().putString(KEY_THEME, theme).apply()

    fun getBrightness(): Float = prefs.getFloat(KEY_BRIGHTNESS, 0.7f)
    fun setBrightness(brightness: Float) = prefs.edit().putFloat(KEY_BRIGHTNESS, brightness).apply()

    fun getFont(): String = prefs.getString(KEY_FONT, "Noto Kufi") ?: "Noto Kufi"
    fun setFont(font: String) = prefs.edit().putString(KEY_FONT, font).apply()

    fun getFontSize(): Float = prefs.getFloat(KEY_FONT_SIZE, 16f)
    fun setFontSize(size: Float) = prefs.edit().putFloat(KEY_FONT_SIZE, size).apply()

    fun getLineSpacing(): String = prefs.getString(KEY_LINE_SPACING, "normal") ?: "normal"
    fun setLineSpacing(spacing: String) = prefs.edit().putString(KEY_LINE_SPACING, spacing).apply()

    fun getScrollDirection(): String = prefs.getString(KEY_SCROLL_DIR, "vertical") ?: "vertical"
    fun setScrollDirection(dir: String) = prefs.edit().putString(KEY_SCROLL_DIR, dir).apply()

    fun getPageMode(): String = prefs.getString(KEY_PAGE_MODE, "continuous") ?: "continuous"
    fun setPageMode(mode: String) = prefs.edit().putString(KEY_PAGE_MODE, mode).apply()

    fun getFitMode(): String = prefs.getString(KEY_FIT_MODE, "fit_width") ?: "fit_width"
    fun setFitMode(mode: String) = prefs.edit().putString(KEY_FIT_MODE, mode).apply()

    fun isAutoScrollEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_SCROLL, false)
    fun setAutoScrollEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_SCROLL, enabled).apply()

    fun getAutoScrollSpeed(): Float = prefs.getFloat(KEY_AUTO_SCROLL_SPEED, 3f)
    fun setAutoScrollSpeed(speed: Float) = prefs.edit().putFloat(KEY_AUTO_SCROLL_SPEED, speed).apply()

    fun getSourceLang(): String = prefs.getString(KEY_SOURCE_LANG, "auto") ?: "auto"
    fun setSourceLang(lang: String) = prefs.edit().putString(KEY_SOURCE_LANG, lang).apply()

    fun getTargetLang(): String = prefs.getString(KEY_TARGET_LANG, "ar") ?: "ar"
    fun setTargetLang(lang: String) = prefs.edit().putString(KEY_TARGET_LANG, lang).apply()

    fun getTtsSpeed(): Float = prefs.getFloat(KEY_TTS_SPEED, 1.0f)
    fun setTtsSpeed(speed: Float) = prefs.edit().putFloat(KEY_TTS_SPEED, speed).apply()

    fun getReaderVoice(): String = prefs.getString(KEY_READER_VOICE, "female") ?: "female"
    fun setReaderVoice(voice: String) = prefs.edit().putString(KEY_READER_VOICE, voice).apply()

    fun getHighlightColor(): String = prefs.getString(KEY_HIGHLIGHT_COLOR, "#FFEE58") ?: "#FFEE58"
    fun setHighlightColor(colorHex: String) = prefs.edit().putString(KEY_HIGHLIGHT_COLOR, colorHex).apply()

    fun getPenThickness(): Float = prefs.getFloat(KEY_PEN_THICKNESS, 4f)
    fun setPenThickness(thickness: Float) = prefs.edit().putFloat(KEY_PEN_THICKNESS, thickness).apply()

    fun getAnnotationOpacity(): Float = prefs.getFloat(KEY_ANNOTATION_OPACITY, 0.6f)
    fun setAnnotationOpacity(opacity: Float) = prefs.edit().putFloat(KEY_ANNOTATION_OPACITY, opacity).apply()

    fun isHighContrastEnabled(): Boolean = prefs.getBoolean(KEY_HIGH_CONTRAST, false)
    fun setHighContrastEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply()

    fun isLargeTargetsEnabled(): Boolean = prefs.getBoolean(KEY_LARGE_TARGETS, false)
    fun setLargeTargetsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_LARGE_TARGETS, enabled).apply()

    fun isTalkbackSimEnabled(): Boolean = prefs.getBoolean(KEY_TALKBACK_SIMULATION, false)
    fun setTalkbackSimEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_TALKBACK_SIMULATION, enabled).apply()

    fun isReduceMotionEnabled(): Boolean = prefs.getBoolean(KEY_REDUCE_MOTION, false)
    fun setReduceMotionEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_REDUCE_MOTION, enabled).apply()

    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
    fun setAppLockEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()

    fun getAppLockPIN(): String = prefs.getString(KEY_APP_LOCK_PIN, "1234") ?: "1234"
    fun setAppLockPIN(pin: String) = prefs.edit().putString(KEY_APP_LOCK_PIN, pin).apply()

    fun isScreenSecurityEnabled(): Boolean = prefs.getBoolean(KEY_SCREEN_SECURITY, false)
    fun setScreenSecurityEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SCREEN_SECURITY, enabled).apply()
}
