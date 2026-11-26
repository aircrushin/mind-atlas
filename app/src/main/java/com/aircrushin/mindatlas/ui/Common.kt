package com.aircrushin.mindatlas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aircrushin.mindatlas.ai.AiSettings
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text

/** Colour per source document, so cards from the same reading cluster visually in every window. */
val Palette = listOf(
    Color(0xFF5B8CFF), Color(0xFFFF8A5B), Color(0xFF3CCB9A), Color(0xFFC77DFF),
    Color(0xFFFFC94D), Color(0xFF4DD0E1), Color(0xFFFF6B9A),
)

fun colorFor(key: String?): Color = Palette[((key ?: "none").hashCode() and 0x7fffffff) % Palette.size]

@Composable
fun WindowHeader(title: String, subtitle: String? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 32.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = PicoTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = PicoTheme.typography.bodySmall, color = PicoTheme.colorScheme.labelSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { trailing() }
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.22f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) { Text(text, fontSize = 13.sp, color = PicoTheme.colorScheme.labelPrimary) }
}

@Composable
fun Dot(color: Color, size: Int = 10) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(color))
}

/** Shows whether AI calls go online or the offline fallback is active, plus the busy status. */
@Composable
fun StatusLine(modifier: Modifier = Modifier) {
    val cfg by AiSettings.config.collectAsState()
    val busy by Actions.busy.collectAsState()
    val last by Actions.lastMessage.collectAsState()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val online = cfg.usable
        val statusText = when {
            busy != null -> busy!!
            last != null -> last!!
            online -> "AI 在线 · ${cfg.model}"
            cfg.offlineOnly -> "离线模式（本地整理）"
            else -> "AI 未配置（本地整理）"
        }
        Dot(if (busy != null) Color(0xFFFFC94D) else if (online) Color(0xFF3CCB9A) else Color(0xFF9E9E9E))
        Spacer(Modifier.width(8.dp))
        Text(
            statusText,
            style = PicoTheme.typography.bodySmall,
            color = PicoTheme.colorScheme.labelSecondary,
            maxLines = 1,
        )
    }
}
