package com.aircrushin.mindatlas.ai

import android.util.Log
import com.aircrushin.mindatlas.data.Card
import com.aircrushin.mindatlas.data.ChatMessage
import com.aircrushin.mindatlas.data.Doc
import com.aircrushin.mindatlas.data.Link
import com.aircrushin.mindatlas.data.Workspace

/** Outcome of an AI action: [usedAi] false means the offline fallback produced the result. */
data class BrainResult<T>(val value: T, val usedAi: Boolean, val error: String? = null)

/** High-level AI operations. Always returns something useful: falls back to [LocalBrain]. */
object Brain {
    private const val TAG = "MindAtlasBrain"

    private const val SYSTEM = "你是“空间研读台”里的 AI 学习助手，帮助用户把阅读材料整理成知识卡片并建立知识之间的联系。" +
        "回答用简体中文，准确、简洁，不编造原文没有的事实。"

    suspend fun makeCards(text: String, doc: Doc?): BrainResult<List<Card>> {
        val now = System.currentTimeMillis()
        return try {
            val prompt = """
                阅读下面的摘录（来自《${doc?.title ?: "未命名"}》），提炼 1 到 3 张知识卡片。
                每张卡片只表达一个独立概念。返回 JSON：
                {"cards":[{"title":"不超过12字的概念名","summary":"不超过80字的解释，用自己的话","keywords":["2到4个关键词"]}]}
                摘录：
                $text
            """.trimIndent()
            val o = AiClient.parseJsonObject(AiClient.chat(listOf("system" to SYSTEM, "user" to prompt), json = true))
            val arr = o.getJSONArray("cards")
            val cards = (0 until arr.length()).map { i ->
                val c = arr.getJSONObject(i)
                val kws = c.optJSONArray("keywords")
                Card(
                    id = Workspace.newId(),
                    title = c.optString("title").take(24),
                    summary = c.optString("summary").take(200),
                    keywords = if (kws == null) emptyList() else (0 until kws.length()).map { kws.optString(it) }.take(5),
                    docId = doc?.id,
                    excerpt = text.take(240),
                    createdAt = now + i,
                    byAi = true,
                )
            }.filter { it.title.isNotBlank() }
            if (cards.isEmpty()) throw AiException("AI 未生成卡片")
            BrainResult(cards, usedAi = true)
        } catch (e: Exception) {
            Log.w(TAG, "makeCards fallback", e)
            BrainResult(listOf(LocalBrain.makeCard(Workspace.newId(), text, doc?.id, now)), false, e.message)
        }
    }

    suspend fun autoLink(cards: List<Card>): BrainResult<List<Link>> {
        if (cards.size < 2) return BrainResult(emptyList(), usedAi = false)
        return try {
            val list = cards.joinToString("\n") { "${it.id} | ${it.title} | ${it.summary}" }
            val prompt = """
                下面是知识卡片列表（id | 标题 | 摘要）。找出它们之间有意义的关系（因果、包含、对比、应用、类比等），
                每张卡片至少与一张相连，总关系数不超过 ${cards.size * 2}。返回 JSON：
                {"links":[{"from":"id","to":"id","relation":"不超过8字的关系描述"}]}
                卡片：
                $list
            """.trimIndent()
            val o = AiClient.parseJsonObject(AiClient.chat(listOf("system" to SYSTEM, "user" to prompt), json = true, maxTokens = 1500))
            val arr = o.getJSONArray("links")
            val ids = cards.map { it.id }.toSet()
            val links = (0 until arr.length()).map { arr.getJSONObject(it) }
                .map { Link(it.optString("from"), it.optString("to"), it.optString("relation").take(16), byAi = true) }
                .filter { it.from in ids && it.to in ids && it.from != it.to }
            if (links.isEmpty()) throw AiException("AI 未找到关联")
            BrainResult(links, usedAi = true)
        } catch (e: Exception) {
            Log.w(TAG, "autoLink fallback", e)
            BrainResult(LocalBrain.link(cards), false, e.message)
        }
    }

    suspend fun ask(question: String, history: List<ChatMessage>, doc: Doc?, cards: List<Card>): BrainResult<String> {
        return try {
            val context = buildString {
                doc?.let { append("当前阅读：《${it.title}》\n").append(it.paragraphs.joinToString("\n").take(3000)).append("\n\n") }
                if (cards.isNotEmpty()) {
                    append("用户的知识卡片：\n")
                    cards.takeLast(30).forEach { append("- ${it.title}：${it.summary}\n") }
                }
            }
            val msgs = mutableListOf("system" to "$SYSTEM\n\n$context")
            history.takeLast(10).forEach { msgs += it.role to it.content }
            msgs += "user" to question
            BrainResult(AiClient.chat(msgs, maxTokens = 800).trim(), usedAi = true)
        } catch (e: Exception) {
            Log.w(TAG, "ask fallback", e)
            BrainResult(LocalBrain.answer(question, cards), false, e.message)
        }
    }

    /** Lightweight connectivity check for the Settings window. */
    suspend fun ping(config: AiConfig): String = try {
        val r = AiClient.chat(listOf("user" to "只回复两个字：连通"), maxTokens = 10, config = config)
        "连接成功：$r"
    } catch (e: Exception) {
        "连接失败：${e.message}"
    }
}
