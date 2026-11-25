package com.aircrushin.mindatlas.ai

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AiException(msg: String, cause: Throwable? = null) : Exception(msg, cause)

/** Minimal OpenAI-compatible chat client (no extra dependencies). */
object AiClient {
    suspend fun chat(
        messages: List<Pair<String, String>>,
        json: Boolean = false,
        maxTokens: Int = 1200,
        config: AiConfig = AiSettings.config.value,
    ): String = withContext(Dispatchers.IO) {
        if (!config.usable) throw AiException("AI 未配置")
        val url = URL(config.baseUrl.trimEnd('/') + "/chat/completions")
        val body = JSONObject().apply {
            put("model", config.model)
            put("temperature", 0.3)
            put("max_tokens", maxTokens)
            put("messages", JSONArray().also { a ->
                messages.forEach { (role, content) -> a.put(JSONObject().put("role", role).put("content", content)) }
            })
            if (json) put("response_format", JSONObject().put("type", "json_object"))
        }
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (config.apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            // OpenRouter attribution headers (harmless for other providers).
            setRequestProperty("HTTP-Referer", "https://github.com/aircrushin/mind-atlas")
            setRequestProperty("X-Title", "MindAtlas PICO")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText() ?: ""
            if (code !in 200..299) throw AiException("AI 服务返回 $code: ${text.take(200)}")
            val content = JSONObject(text).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").optString("content")
            if (content.isBlank()) throw AiException("AI 返回为空")
            content
        } catch (e: AiException) {
            throw e
        } catch (e: Exception) {
            throw AiException("网络错误：${e.message}", e)
        } finally {
            conn.disconnect()
        }
    }

    /** Extract the first JSON object from a model reply (tolerates ```json fences). */
    fun parseJsonObject(raw: String): JSONObject {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) throw AiException("AI 返回的不是 JSON")
        return JSONObject(raw.substring(start, end + 1))
    }
}
