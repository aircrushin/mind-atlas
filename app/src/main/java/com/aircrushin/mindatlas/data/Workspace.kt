package com.aircrushin.mindatlas.data

import android.content.Context
import android.util.Log
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Single source of truth shared by every spatial window. All WindowContainers run in the same
 * process, so a StateFlow singleton keeps the reader, notes, assistant and graph in sync live.
 * State is persisted atomically to app-private storage (no data loss across restarts).
 */
object Workspace {
    private const val TAG = "MindAtlas"
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(WorkspaceState())
    val state: StateFlow<WorkspaceState> = _state
    private var file: File? = null

    fun newId(): String = UUID.randomUUID().toString().take(8)

    @OptIn(FlowPreview::class)
    fun init(context: Context) {
        val f = File(context.filesDir, "workspace.json")
        file = f
        val loaded = runCatching { if (f.exists()) workspaceFromJson(JSONObject(f.readText())) else null }
            .onFailure { Log.w(TAG, "load failed, starting fresh", it) }
            .getOrNull()
        _state.value = if (loaded == null || loaded.docs.isEmpty()) {
            WorkspaceState(docs = SampleLibrary.docs, activeDocId = SampleLibrary.docs.first().id)
        } else loaded
        scope.launch(Dispatchers.IO) {
            _state.drop(1).debounce(400).collect { save(it) }
        }
    }

    private fun save(s: WorkspaceState) {
        val f = file ?: return
        runCatching {
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(s.toJson().toString())
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        }.onFailure { Log.e(TAG, "save failed", it) }
    }

    fun mutate(block: (WorkspaceState) -> WorkspaceState) = _state.update(block)

    // --- Docs ---
    fun openDoc(id: String) = mutate { it.copy(activeDocId = id) }

    fun addDoc(title: String, text: String): String {
        val paras = text.split(Regex("\\n\\s*\\n|\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        val doc = Doc(newId(), title.ifBlank { paras.firstOrNull()?.take(16) ?: "未命名文档" }, "我的导入", paras)
        mutate { it.copy(docs = it.docs + doc, activeDocId = doc.id) }
        return doc.id
    }

    // --- Cards & links ---
    fun addCards(cards: List<Card>) = mutate { it.copy(cards = it.cards + cards) }

    fun updateCard(card: Card) = mutate { s -> s.copy(cards = s.cards.map { if (it.id == card.id) card else it }) }

    fun deleteCard(id: String) = mutate { s ->
        s.copy(cards = s.cards.filterNot { it.id == id }, links = s.links.filterNot { it.from == id || it.to == id },
            focusedCardId = if (s.focusedCardId == id) null else s.focusedCardId)
    }

    fun focusCard(id: String?) = mutate { it.copy(focusedCardId = id) }

    /** Merge links, de-duplicating undirected pairs; newer relation text wins. */
    fun mergeLinks(newLinks: List<Link>) = mutate { s ->
        val ids = s.cards.map { it.id }.toSet()
        val map = LinkedHashMap<Pair<String, String>, Link>()
        fun key(l: Link) = if (l.from < l.to) l.from to l.to else l.to to l.from
        (s.links + newLinks).filter { it.from in ids && it.to in ids && it.from != it.to }
            .forEach { map[key(it)] = it }
        s.copy(links = map.values.toList())
    }

    // --- Notes ---
    fun addNote(text: String, docId: String?) {
        if (text.isBlank()) return
        mutate { it.copy(notes = listOf(Note(newId(), docId, text.trim(), System.currentTimeMillis())) + it.notes) }
    }

    fun deleteNote(id: String) = mutate { s -> s.copy(notes = s.notes.filterNot { it.id == id }) }

    // --- Chat ---
    fun appendChat(m: ChatMessage) = mutate { it.copy(chat = it.chat + m) }
    fun clearChat() = mutate { it.copy(chat = emptyList()) }

    fun resetAll() = mutate { WorkspaceState(docs = SampleLibrary.docs, activeDocId = SampleLibrary.docs.first().id) }
}
