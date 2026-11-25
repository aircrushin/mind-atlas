package com.aircrushin.mindatlas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.aircrushin.mindatlas.data.Workspace
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TextField

private val QUICK = listOf(
    "总结当前文档的 3 个要点",
    "根据我的卡片出 3 道自测题",
    "这些概念之间有什么联系？",
    "用一个生活类比解释最难的概念",
)

@Composable
fun AssistantWindow(modifier: Modifier) {
    val s by Workspace.state.collectAsState()
    val busy by Actions.busy.collectAsState()
    var input by remember { mutableStateOf("") }
    val list = rememberLazyListState()
    LaunchedEffect(s.chat.size) { if (s.chat.isNotEmpty()) list.animateScrollToItem(s.chat.size - 1) }

    Column(modifier.fillMaxSize()) {
        WindowHeader("AI 助手", "了解你正在读的《${s.activeDoc?.title ?: "—"}》和 ${s.cards.size} 张卡片") {
            Button(
                onClick = { Workspace.clearChat() }, size = ButtonDefaults.Small,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.10f), contentColor = PicoTheme.colorScheme.labelPrimary),
            ) { Text("清空") }
        }
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 24.dp), state = list,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (s.chat.isEmpty()) item {
                Text("向我提问，或点下面的快捷指令。我会结合当前文档和你的知识卡片回答。", color = PicoTheme.colorScheme.labelSecondary)
            }
            items(s.chat) { m ->
                val mine = m.role == "user"
                Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
                    Box(
                        Modifier.widthIn(max = 480.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (mine) Color(0xFF5B8CFF).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.09f))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) { Text(m.content, fontSize = 16.sp, lineHeight = 26.sp) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QUICK.take(2).forEach { q -> QuickChip(q, busy == null) { Actions.ask(q) } }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK.drop(2).forEach { q -> QuickChip(q, busy == null) { Actions.ask(q) } }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("问点什么…") })
            Button(onClick = { Actions.ask(input); input = "" }, enabled = input.isNotBlank() && busy == null) { Text("发送") }
        }
        StatusLine(Modifier.padding(horizontal = 28.dp, vertical = 14.dp))
    }
}

@Composable
private fun QuickChip(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, size = ButtonDefaults.Small,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.10f), contentColor = PicoTheme.colorScheme.labelPrimary),
    ) { Text(text, fontSize = 13.sp) }
}
