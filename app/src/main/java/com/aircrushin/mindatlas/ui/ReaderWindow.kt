package com.aircrushin.mindatlas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.pico.spatial.ui.design.TextField
import com.pico.spatial.ui.design.windows.Toolbar
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator

@Composable
fun ReaderWindow(modifier: Modifier) {
    val s by Workspace.state.collectAsState()
    val busy by Actions.busy.collectAsState()
    val nav = LocalSpatialNavigator.current
    var selected by remember(s.activeDoc?.id) { mutableStateOf(setOf<Int>()) }
    var importing by remember { mutableStateOf(false) }
    val doc = s.activeDoc

    Box(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            // --- Library sidebar ---
            Column(
                Modifier.width(280.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.04f))
                    .padding(top = 28.dp, start = 20.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("空间研读台", style = PicoTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("我的书架", style = PicoTheme.typography.bodySmall, color = PicoTheme.colorScheme.labelSecondary)
                Spacer(Modifier.height(4.dp))
                s.docs.forEach { d ->
                    val active = d.id == doc?.id
                    val count = s.cards.count { it.docId == d.id }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (active) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                            .spatialHoverEffect().clickable { Workspace.openDoc(d.id); importing = false }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Dot(colorFor(d.id))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.title, maxLines = 2, fontSize = 15.sp, fontWeight = if (active) FontWeight.SemiBold else null)
                            Text("${d.source} · $count 张卡片", fontSize = 12.sp, color = PicoTheme.colorScheme.labelTertiary)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { importing = !importing }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (importing) "返回阅读" else "＋ 导入文本")
                }
            }

            // --- Reading pane ---
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (importing) {
                    ImportPane(onDone = { importing = false })
                } else if (doc != null) {
                    WindowHeader(doc.title, "点选段落（可多选）→ 让 AI 提炼成知识卡片，它们会出现在空间知识星图里") {}
                    LazyColumn(
                        Modifier.weight(1f).padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(doc.paragraphs.size) { i ->
                            val on = i in selected
                            val used = s.cards.any { it.docId == doc.id && it.excerpt.isNotEmpty() && doc.paragraphs[i].startsWith(it.excerpt.take(20)) }
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(
                                        when {
                                            on -> colorFor(doc.id).copy(alpha = 0.28f)
                                            used -> Color.White.copy(alpha = 0.07f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .spatialHoverEffect()
                                    .clickable { selected = if (on) selected - i else selected + i }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(doc.paragraphs[i], fontSize = 19.sp, lineHeight = 32.sp)
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatusLine(Modifier.weight(1f))
                        val text = selected.sorted().joinToString("\n") { doc.paragraphs[it] }
                        Button(
                            onClick = { Workspace.addNote(text, doc.id); selected = emptySet(); nav.openWindowContainer(Windows.NOTES) },
                            enabled = selected.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = PicoTheme.colorScheme.labelPrimary),
                        ) { Text("摘录到笔记") }
                        Button(
                            onClick = {
                                Actions.makeCards(if (selected.isEmpty()) doc.paragraphs.joinToString("\n") else text)
                                selected = emptySet()
                                nav.openWindowContainer(Windows.GRAPH)
                            },
                            enabled = busy == null,
                        ) { Text(if (selected.isEmpty()) "✦ 全文生成知识卡片" else "✦ 生成知识卡片（${selected.size}）") }
                    }
                }
            }
        }

        // Ornament toolbar under the window: every other panel is its own spatial window.
        Toolbar {
            ToolbarAction("📝 笔记") { nav.openWindowContainer(Windows.NOTES) }
            ToolbarAction("🤖 AI 助手") { nav.openWindowContainer(Windows.ASSISTANT) }
            ToolbarAction("✨ 知识星图") { nav.openWindowContainer(Windows.GRAPH) }
            ToolbarAction("⚙ 设置") { nav.openWindowContainer(Windows.SETTINGS) }
        }
    }
}

@Composable
fun ToolbarAction(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = PicoTheme.colorScheme.labelPrimary),
    ) { Text(label) }
}

@Composable
private fun ImportPane(onDone: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WindowHeader("导入文本", "粘贴论文摘要、课程讲义或文章，每段一行")
        TextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("标题（可选）") })
        TextField(
            value = body, onValueChange = { body = it }, singleLine = false, minLines = 8,
            modifier = Modifier.fillMaxWidth().weight(1f), placeholder = { Text("在这里粘贴正文…") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { if (body.isNotBlank()) { Workspace.addDoc(title, body); onDone() } }, enabled = body.isNotBlank()) {
                Text("加入书架")
            }
        }
    }
}
