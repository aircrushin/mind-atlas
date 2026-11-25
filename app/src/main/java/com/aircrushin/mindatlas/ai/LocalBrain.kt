package com.aircrushin.mindatlas.ai

import com.aircrushin.mindatlas.data.Card
import com.aircrushin.mindatlas.data.Link

/**
 * Offline fallback so the core loop (excerpt -> card -> link -> graph) never breaks when the
 * headset has no network or no AI key. Uses character bigrams, which works for Chinese without
 * a word segmenter.
 */
object LocalBrain {
    private val stop = "的了是在和与就都而及着或一个这那我们你他她它其之为以于把被让对从到也又很更最并且如果因为所以但是".toSet()

    fun bigrams(text: String): Map<String, Int> {
        val clean = text.filter { it.isLetterOrDigit() }
        val m = HashMap<String, Int>()
        for (i in 0 until clean.length - 1) {
            val a = clean[i]; val b = clean[i + 1]
            if (a in stop || b in stop) continue
            val g = "$a$b"
            m[g] = (m[g] ?: 0) + 1
        }
        return m
    }

    fun keywords(text: String, n: Int = 4): List<String> =
        bigrams(text).entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(n).map { it.key }

    fun sentences(text: String): List<String> =
        text.split(Regex("(?<=[。！？!?；;])")).map { it.trim() }.filter { it.length > 4 }

    fun makeCard(id: String, text: String, docId: String?, now: Long): Card {
        val s = sentences(text)
        val first = s.firstOrNull() ?: text
        val kws = keywords(text)
        val title = (kws.take(2).joinToString("·").ifBlank { first.take(12) })
        val summary = s.take(2).joinToString("").take(90).ifBlank { text.take(90) }
        return Card(id, title, summary, kws, docId, text.take(240), now, byAi = false)
    }

    fun similarity(a: Card, b: Card): Double {
        val x = bigrams(a.title + a.summary + a.keywords.joinToString("")).keys
        val y = bigrams(b.title + b.summary + b.keywords.joinToString("")).keys
        if (x.isEmpty() || y.isEmpty()) return 0.0
        return (x intersect y).size.toDouble() / (x union y).size
    }

    fun link(cards: List<Card>, threshold: Double = 0.08): List<Link> {
        val out = ArrayList<Link>()
        for (i in cards.indices) for (j in i + 1 until cards.size) {
            val s = similarity(cards[i], cards[j])
            if (s >= threshold) {
                val shared = (cards[i].keywords intersect cards[j].keywords.toSet()).firstOrNull()
                out += Link(cards[i].id, cards[j].id, shared?.let { "共同概念：$it" } ?: "内容相近", byAi = false)
            }
        }
        // Keep every card reachable: connect isolated cards to their most similar neighbour.
        val linked = out.flatMap { listOf(it.from, it.to) }.toSet()
        cards.filter { it.id !in linked && cards.size > 1 }.forEach { c ->
            cards.filter { it.id != c.id }.maxByOrNull { similarity(c, it) }?.let {
                out += Link(c.id, it.id, "可能相关", byAi = false)
            }
        }
        return out
    }

    fun answer(question: String, cards: List<Card>): String {
        if (cards.isEmpty()) return "（离线模式）还没有知识卡片。先在阅读窗口选中段落，点“生成知识卡片”。"
        val q = Card("q", question, question, keywords(question), null, "", 0, false)
        val top = cards.sortedByDescending { similarity(q, it) }.take(3)
        return "（离线模式，基于你的卡片检索）与你的问题最相关的卡片：\n" +
            top.joinToString("\n") { "• ${it.title}：${it.summary}" }
    }
}
