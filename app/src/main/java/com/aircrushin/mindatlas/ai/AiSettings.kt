package com.aircrushin.mindatlas.ai

import android.content.Context
import android.content.SharedPreferences
import com.aircrushin.mindatlas.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * AI endpoint configuration. Any OpenAI-compatible endpoint works (OpenRouter by default, or a
 * small proxy that holds the key server-side). Values typed in the Settings window override the
 * build-time defaults from secrets.properties; nothing secret is committed to git.
 */
data class AiConfig(val baseUrl: String, val apiKey: String, val model: String, val offlineOnly: Boolean) {
    val usable: Boolean get() = !offlineOnly && baseUrl.isNotBlank()
}

object AiSettings {
    private lateinit var prefs: SharedPreferences
    private val _config = MutableStateFlow(AiConfig(BuildConfig.AI_BASE_URL, BuildConfig.AI_API_KEY, BuildConfig.AI_MODEL, false))
    val config: StateFlow<AiConfig> = _config

    fun init(context: Context) {
        prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
        _config.value = AiConfig(
            baseUrl = prefs.getString("baseUrl", null) ?: BuildConfig.AI_BASE_URL,
            apiKey = prefs.getString("apiKey", null) ?: BuildConfig.AI_API_KEY,
            model = prefs.getString("model", null) ?: BuildConfig.AI_MODEL,
            offlineOnly = prefs.getBoolean("offlineOnly", false),
        )
    }

    fun save(c: AiConfig) {
        _config.value = c
        prefs.edit().putString("baseUrl", c.baseUrl.trim()).putString("apiKey", c.apiKey.trim())
            .putString("model", c.model.trim()).putBoolean("offlineOnly", c.offlineOnly).apply()
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _config.value = AiConfig(BuildConfig.AI_BASE_URL, BuildConfig.AI_API_KEY, BuildConfig.AI_MODEL, false)
    }
}
