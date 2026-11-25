package com.aircrushin.mindatlas.ui

import com.aircrushin.mindatlas.ai.Brain
import com.aircrushin.mindatlas.data.ChatMessage
import com.aircrushin.mindatlas.data.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** User intents shared by all windows. Long-running AI work runs in the app-wide scope. */
object Actions {
    val busy = MutableStateFlow<String?>(null)
    val lastMessage = MutableStateFlow<String?>(null)

    private fun run(label: String, block: suspend () -> String?) {
        if (busy.value != null) return
        busy.value = label
        Workspace.scope.launch {
            try {
                lastMessage.value = block()
            } catch (e: Exception) {
                lastMessage.value = "出错了：${e.message}"
            } finally {
                busy.value = null
            }
        }
    }

    fun makeCards(text: String) = run("AI 正在提炼知识卡片…") {
        val s = Workspace.state.value
        val r = Brain.makeCards(text, s.activeDoc)
        Workspace.addCards(r.value)
        // Re-link the whole graph so the new cards find their place immediately.
        val all = Workspace.state.value.cards
        val links = Brain.autoLink(all)
        Workspace.mergeLinks(links.value)
        Workspace.focusCard(r.value.firstOrNull()?.id)
        val how = if (r.usedAi) "AI" else "离线整理"
        "已生成 ${r.value.size} 张卡片（$how），并更新了 ${links.value.size} 条关联"
    }

    fun relinkAll() = run("AI 正在分析卡片之间的关系…") {
        val r = Brain.autoLink(Workspace.state.value.cards)
        Workspace.mergeLinks(r.value)
        "关联已更新：${r.value.size} 条（${if (r.usedAi) "AI" else "离线"}）"
    }

    fun noteToCards(text: String) = makeCards(text)

    fun ask(question: String) {
        if (question.isBlank()) return
        val s = Workspace.state.value
        Workspace.appendChat(ChatMessage("user", question))
        run("AI 正在思考…") {
            val r = Brain.ask(question, s.chat, s.activeDoc, s.cards)
            Workspace.appendChat(ChatMessage("assistant", r.value))
            if (r.usedAi) null else "AI 不可用，已用离线检索回答"
        }
    }
}
