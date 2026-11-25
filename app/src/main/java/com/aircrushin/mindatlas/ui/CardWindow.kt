package com.aircrushin.mindatlas.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircrushin.mindatlas.Windows
import com.aircrushin.mindatlas.data.Workspace
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import com.pico.spatial.ui.platform.containers.SpatialNavigator

fun SpatialNavigator.openCard(id: String) =
    openWindowContainer(Windows.CARD, id, Bundle().apply { putString(Windows.ARG_CARD_ID, id) })

/** A single knowledge card in its own spatial window: pin several side by side to compare. */
@Composable
fun CardWindow(cardId: String?, modifier: Modifier) {
    val s by Workspace.state.collectAsState()
    val nav = LocalSpatialNavigator.current
    val card = s.card(cardId)
    Column(modifier.fillMaxSize()) {
        if (card == null) {
            WindowHeader("卡片已删除", "可以关闭这个窗口")
            return@Column
        }
        val color = colorFor(card.docId)
        val doc = s.docs.firstOrNull { it.id == card.docId }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(color, 14)
                Spacer(Modifier.width(10.dp))
                Text(if (card.byAi) "AI 卡片" else "离线卡片", fontSize = 13.sp, color = PicoTheme.colorScheme.labelTertiary)
            }
            Text(card.title, style = PicoTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(card.summary, fontSize = 18.sp, lineHeight = 30.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { card.keywords.forEach { Pill(it, color) } }
            if (card.excerpt.isNotBlank()) {
                Text("原文摘录${doc?.let { " · 《${it.title}》" } ?: ""}", fontSize = 13.sp, color = PicoTheme.colorScheme.labelTertiary)
                Text(
                    card.excerpt, fontSize = 15.sp, lineHeight = 24.sp, color = PicoTheme.colorScheme.labelSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.06f)).padding(12.dp),
                )
            }
            val related = s.links.filter { it.from == card.id || it.to == card.id }
            if (related.isNotEmpty()) {
                Text("关联卡片", fontSize = 13.sp, color = PicoTheme.colorScheme.labelTertiary)
                related.forEach { l ->
                    val other = s.card(if (l.from == card.id) l.to else l.from) ?: return@forEach
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.06f))
                            .spatialHoverEffect().clickable { Workspace.focusCard(other.id); nav.openCard(other.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Dot(colorFor(other.docId), 8)
                        Spacer(Modifier.width(8.dp))
                        Text(other.title, Modifier.weight(1f), fontSize = 15.sp)
                        Text(l.relation, fontSize = 13.sp, color = PicoTheme.colorScheme.labelSecondary)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Row(Modifier.padding(horizontal = 28.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (doc != null) Button(onClick = { Workspace.openDoc(doc.id) }, size = ButtonDefaults.Small) { Text("回到原文") }
            Button(
                onClick = { Workspace.deleteCard(card.id); nav.closeWindowContainer(Windows.CARD, card.id) }, size = ButtonDefaults.Small,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.10f), contentColor = PicoTheme.colorScheme.labelPrimary),
            ) { Text("删除卡片") }
        }
    }
}
