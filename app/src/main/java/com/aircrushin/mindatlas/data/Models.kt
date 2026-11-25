package com.aircrushin.mindatlas.data

import org.json.JSONArray
import org.json.JSONObject

data class Doc(val id: String, val title: String, val source: String, val paragraphs: List<String>)

data class Card(
    val id: String,
    val title: String,
    val summary: String,
    val keywords: List<String>,
    val docId: String?,
    val excerpt: String,
    val createdAt: Long,
    val byAi: Boolean,
)

data class Link(val from: String, val to: String, val relation: String, val byAi: Boolean)

data class Note(val id: String, val docId: String?, val text: String, val createdAt: Long)

data class ChatMessage(val role: String, val content: String)

data class WorkspaceState(
    val docs: List<Doc> = emptyList(),
    val activeDocId: String? = null,
    val cards: List<Card> = emptyList(),
    val links: List<Link> = emptyList(),
    val notes: List<Note> = emptyList(),
    val chat: List<ChatMessage> = emptyList(),
    val focusedCardId: String? = null,
) {
    val activeDoc: Doc? get() = docs.firstOrNull { it.id == activeDocId } ?: docs.firstOrNull()
    fun card(id: String?): Card? = cards.firstOrNull { it.id == id }
}

// --- JSON (org.json keeps the APK free of extra serialization plugins) ---

private fun List<String>.toJson() = JSONArray().also { a -> forEach { a.put(it) } }
private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { optString(it) }

fun WorkspaceState.toJson(): JSONObject = JSONObject().apply {
    put("version", 1)
    put("activeDocId", activeDocId)
    put("docs", JSONArray().also { a -> docs.forEach { d ->
        a.put(JSONObject().put("id", d.id).put("title", d.title).put("source", d.source)
            .put("paragraphs", d.paragraphs.toJson()))
    } })
    put("cards", JSONArray().also { a -> cards.forEach { c ->
        a.put(JSONObject().put("id", c.id).put("title", c.title).put("summary", c.summary)
            .put("keywords", c.keywords.toJson()).put("docId", c.docId).put("excerpt", c.excerpt)
            .put("createdAt", c.createdAt).put("byAi", c.byAi))
    } })
    put("links", JSONArray().also { a -> links.forEach { l ->
        a.put(JSONObject().put("from", l.from).put("to", l.to).put("relation", l.relation).put("byAi", l.byAi))
    } })
    put("notes", JSONArray().also { a -> notes.forEach { n ->
        a.put(JSONObject().put("id", n.id).put("docId", n.docId).put("text", n.text).put("createdAt", n.createdAt))
    } })
    put("chat", JSONArray().also { a -> chat.takeLast(60).forEach { m ->
        a.put(JSONObject().put("role", m.role).put("content", m.content))
    } })
}

fun workspaceFromJson(o: JSONObject): WorkspaceState {
    fun arr(name: String) = o.optJSONArray(name) ?: JSONArray()
    fun JSONArray.objs() = (0 until length()).mapNotNull { optJSONObject(it) }
    fun JSONObject.nstr(k: String): String? = if (isNull(k)) null else optString(k)
    return WorkspaceState(
        docs = arr("docs").objs().map {
            Doc(it.getString("id"), it.optString("title"), it.optString("source"), it.optJSONArray("paragraphs").strings())
        },
        activeDocId = o.nstr("activeDocId"),
        cards = arr("cards").objs().map {
            Card(it.getString("id"), it.optString("title"), it.optString("summary"),
                it.optJSONArray("keywords").strings(), it.nstr("docId"), it.optString("excerpt"),
                it.optLong("createdAt"), it.optBoolean("byAi"))
        },
        links = arr("links").objs().map {
            Link(it.getString("from"), it.getString("to"), it.optString("relation"), it.optBoolean("byAi"))
        },
        notes = arr("notes").objs().map {
            Note(it.getString("id"), it.nstr("docId"), it.optString("text"), it.optLong("createdAt"))
        },
        chat = arr("chat").objs().map { ChatMessage(it.optString("role"), it.optString("content")) },
    )
}
