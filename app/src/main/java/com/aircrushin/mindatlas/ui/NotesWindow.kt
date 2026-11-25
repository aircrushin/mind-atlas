package com.aircrushin.mindatlas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircrushin.mindatlas.Windows
import com.aircrushin.mindatlas.data.Workspace
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TextField
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesWindow(modifier: Modifier) {
    val s by Workspace.state.collectAsState()
    val busy by Actions.busy.collectAsState()
    val nav = LocalSpatialNavigator.current
    var draft by remember { mutableStateOf("") }
    val fmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.CHINA) }

    Column(modifier.fillMaxSize()) {
        WindowHeader("笔记", "${s.notes.size} 条 · 当前阅读：${s.activeDoc?.title ?: "无"}")
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextField(
                value = draft, onValueChange = { draft = it }, singleLine = false, minLines = 3, maxLines = 6,
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("记下你的想法、疑问或联想…") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { Workspace.addNote(draft, s.activeDoc?.id); draft = "" }, enabled = draft.isNotBlank()) { Text("保存笔记") }
                Button(
                    onClick = { Actions.noteToCards(draft); Workspace.addNote(draft, s.activeDoc?.id); draft = ""; nav.openWindowContainer(Windows.GRAPH) },
                    enabled = draft.isNotBlank() && busy == null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = PicoTheme.colorScheme.labelPrimary),
                ) { Text("✦ 保存并生成卡片") }
            }
        }
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (s.notes.isEmpty()) item {
                Text("还没有笔记。在阅读窗口选中段落点“摘录到笔记”，或直接在上方输入。", color = PicoTheme.colorScheme.labelSecondary)
            }
            items(s.notes, key = { it.id }) { n ->
                val docTitle = s.docs.firstOrNull { it.id == n.docId }?.title
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.07f)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(colorFor(n.docId), 8)
                        Spacer(Modifier.padding(4.dp))
                        Text("${fmt.format(Date(n.createdAt))}${docTitle?.let { " · $it" } ?: ""}", fontSize = 12.sp, color = PicoTheme.colorScheme.labelTertiary)
                    }
                    Text(n.text, fontSize = 16.sp, lineHeight = 26.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { Actions.noteToCards(n.text); nav.openWindowContainer(Windows.GRAPH) }, enabled = busy == null, size = ButtonDefaults.Small) { Text("生成卡片") }
                        Button(
                            onClick = { Workspace.deleteNote(n.id) }, size = ButtonDefaults.Small,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.10f), contentColor = PicoTheme.colorScheme.labelPrimary),
                        ) { Text("删除") }
                    }
                }
            }
        }
        StatusLine(Modifier.padding(horizontal = 28.dp, vertical = 14.dp))
    }
}
