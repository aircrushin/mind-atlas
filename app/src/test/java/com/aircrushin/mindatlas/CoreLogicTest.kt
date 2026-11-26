package com.aircrushin.mindatlas

import com.aircrushin.mindatlas.ai.AiClient
import com.aircrushin.mindatlas.ai.AiConfig
import com.aircrushin.mindatlas.ai.LocalBrain
import com.aircrushin.mindatlas.data.Card
import com.aircrushin.mindatlas.data.ChatMessage
import com.aircrushin.mindatlas.data.Link
import com.aircrushin.mindatlas.data.Note
import com.aircrushin.mindatlas.data.SampleLibrary
import com.aircrushin.mindatlas.data.WorkspaceState
import com.aircrushin.mindatlas.data.toJson
import com.aircrushin.mindatlas.data.workspaceFromJson
import com.aircrushin.mindatlas.ui.GraphLayout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {
    private fun cardsFromSamples(): List<Card> = SampleLibrary.docs.flatMap { d ->
        d.paragraphs.mapIndexed { i, p -> LocalBrain.makeCard("${d.id}-$i", p, d.id, i.toLong()) }
    }

    @Test fun offlineCardsHaveTitleSummaryKeywords() {
        val cards = cardsFromSamples()
        assertEquals(15, cards.size)
        cards.forEach {
            assertTrue(it.title.isNotBlank()); assertTrue(it.summary.isNotBlank()); assertTrue(it.keywords.isNotEmpty())
        }
    }

    @Test fun offlineLinkingConnectsEveryCard() {
        val cards = cardsFromSamples()
        val links = LocalBrain.link(cards)
        val linked = links.flatMap { listOf(it.from, it.to) }.toSet()
        assertTrue(cards.all { it.id in linked })
    }

    @Test fun layoutIsNormalisedAndDeterministic() {
        val cards = cardsFromSamples()
        val links = LocalBrain.link(cards)
        val a = GraphLayout.layout(cards, links)
        val b = GraphLayout.layout(cards, links)
        assertEquals(a, b)
        a.values.forEach { assertTrue(it.len() <= 1.0001f) }
    }

    @Test fun workspaceJsonRoundTrip() {
        val s = WorkspaceState(
            docs = SampleLibrary.docs, activeDocId = "sample-feynman",
            cards = cardsFromSamples().take(3),
            links = listOf(Link("sample-memory-0", "sample-memory-1", "因果", true)),
            notes = listOf(Note("n1", null, "想法", 1L)),
            chat = listOf(ChatMessage("user", "你好")),
        )
        val back = workspaceFromJson(JSONObject(s.toJson().toString()))
        assertEquals(s.copy(focusedCardId = null), back)
    }

    @Test fun parsesFencedJson() {
        val o = AiClient.parseJsonObject("```json\n{\"cards\":[{\"title\":\"t\"}]}\n```")
        assertEquals("t", o.getJSONArray("cards").getJSONObject(0).getString("title"))
    }

    @Test fun usableRequiresKeyOnDefaultOpenRouter() {
        val noKey = AiConfig("https://openrouter.ai/api/v1", "", "deepseek/deepseek-chat-v3-0324", false)
        assertFalse(noKey.usable)
        val trailing = AiConfig("https://openrouter.ai/api/v1/", "  ", "m", false)
        assertFalse(trailing.usable)
        val withKey = AiConfig("https://openrouter.ai/api/v1", "test-key", "m", false)
        assertTrue(withKey.usable)
        val offline = AiConfig("https://openrouter.ai/api/v1", "test-key", "m", true)
        assertFalse(offline.usable)
    }

    @Test fun usableAllowsKeylessCustomProxy() {
        val proxy = AiConfig("https://mindatlas-proxy.example.com/v1", "", "local-model", false)
        assertTrue(proxy.usable)
        val blankUrl = AiConfig("", "", "m", false)
        assertFalse(blankUrl.usable)
        val offlineProxy = AiConfig("https://mindatlas-proxy.example.com/v1", "", "m", true)
        assertFalse(offlineProxy.usable)
    }
}
