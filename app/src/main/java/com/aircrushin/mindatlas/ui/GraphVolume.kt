package com.aircrushin.mindatlas.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aircrushin.mindatlas.GRAPH_D
import com.aircrushin.mindatlas.GRAPH_H
import com.aircrushin.mindatlas.GRAPH_W
import com.aircrushin.mindatlas.data.Workspace
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.windows.Toolbar
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.foundation.layout.zOffset
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import kotlin.math.roundToInt

private const val EDGE_DOTS = 7

/**
 * The knowledge graph as a volumetric window: every card is a node floating at its own depth
 * (Modifier.zOffset), links are drawn as 3D dotted lines whose dots are interpolated in depth too.
 * Drag to rotate the constellation; tap a node to focus it and see its neighbours.
 */
@Composable
fun GraphVolume(modifier: Modifier) {
    val s by Workspace.state.collectAsState()
    val busy by Actions.busy.collectAsState()
    val nav = LocalSpatialNavigator.current
    val density = LocalDensity.current

    val layout = remember(s.cards.map { it.id }, s.links) { GraphLayout.layout(s.cards, s.links) }
    var yaw by remember { mutableFloatStateOf(20f) }
    var pitch by remember { mutableFloatStateOf(-12f) }
    var spin by remember { mutableStateOf(true) }
    val transition = rememberInfiniteTransition(label = "spin")
    val autoYaw by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "yaw",
    )
    val totalYaw = yaw + if (spin) autoYaw else 0f

    val focused = s.card(s.focusedCardId)
    val neighbours = focused?.let { f -> s.links.filter { it.from == f.id || it.to == f.id }.flatMap { listOf(it.from, it.to) }.toSet() } ?: emptySet()

    val halfW = with(density) { GRAPH_W.toPx() } * 0.36f
    val halfH = with(density) { GRAPH_H.toPx() } * 0.30f
    val halfD = with(density) { GRAPH_D.toPx() } * 0.40f
    val cxPx = with(density) { GRAPH_W.toPx() } / 2f
    val cyPx = with(density) { GRAPH_H.toPx() } * 0.44f

    fun project(p: P3): Triple<Float, Float, Float> {
        val r = p.rotate(totalYaw, pitch)
        val persp = 1f + r.z * 0.12f
        return Triple(cxPx + r.x * halfW * persp, cyPx + r.y * halfH * persp, r.z * halfD)
    }

    Box(
        modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures { _, d ->
                yaw += d.x * 0.25f
                pitch = (pitch - d.y * 0.2f).coerceIn(-60f, 60f)
            }
        }
    ) {
        if (s.cards.isEmpty()) {
            EmptyGraphPlaceholder(
                modifier = Modifier.fillMaxSize(),
                project = { project(it) },
                halfD = halfD,
            )
        }

        // Edges: dotted lines with per-dot depth.
        s.links.forEach { l ->
            val a = layout[l.from] ?: return@forEach
            val b = layout[l.to] ?: return@forEach
            val hot = focused != null && (l.from == focused.id || l.to == focused.id)
            for (i in 1 until EDGE_DOTS) {
                val t = i / EDGE_DOTS.toFloat()
                val (x, y, z) = project(a + (b - a) * t)
                val sz = if (hot) 9 else 6
                val half = with(density) { sz.dp.toPx() } / 2f
                Box(
                    Modifier.offset { IntOffset((x - half).roundToInt(), (y - half).roundToInt()) }
                        .zOffset { z }
                        .size(sz.dp)
                        .clip(CircleShape)
                        .background(if (hot) Color(0xFFFFE08A) else Color.White.copy(alpha = if (focused == null) 0.45f else 0.18f))
                )
            }
        }

        // Nodes.
        s.cards.forEach { c ->
            val p = layout[c.id] ?: return@forEach
            val (x, y, z) = project(p)
            val color = colorFor(c.docId)
            val isFocus = focused?.id == c.id
            val dim = focused != null && !isFocus && c.id !in neighbours
            val degree = s.links.count { it.from == c.id || it.to == c.id }
            val nodeW = 150 + degree * 6
            val nodeWpx = with(density) { nodeW.dp.toPx() }
            Column(
                Modifier.offset { IntOffset((x - nodeWpx / 2).roundToInt(), (y - 30).roundToInt()) }
                    .zOffset { z + if (isFocus) halfD * 0.25f else 0f }
                    .widthIn(max = nodeW.dp)
                    .graphicsLayer { alpha = if (dim) 0.35f else 1f }
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF141A2E).copy(alpha = 0.82f))
                    .border(if (isFocus) 3.dp else 1.5.dp, color.copy(alpha = if (isFocus) 1f else 0.7f), RoundedCornerShape(18.dp))
                    .spatialHoverEffect()
                    .clickable { Workspace.focusCard(if (isFocus) null else c.id) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                Text(c.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
            }
        }

        // Focus panel.
        if (focused != null) {
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).widthIn(max = 760.dp)
                    .zOffset { halfD * 0.6f }
                    .clip(RoundedCornerShape(22.dp)).background(Color(0xFF141A2E).copy(alpha = 0.92f)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dot(colorFor(focused.docId), 12)
                    Text(focused.title, style = PicoTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Button(onClick = { nav.openCard(focused.id) }, size = ButtonDefaults.Small) { Text("在新窗口打开") }
                }
                Text(focused.summary, fontSize = 16.sp, lineHeight = 26.sp)
                val rel = s.links.filter { it.from == focused.id || it.to == focused.id }
                if (rel.isNotEmpty()) {
                    Text(
                        rel.joinToString("   ") { l -> "→ ${s.card(if (l.from == focused.id) l.to else l.from)?.title ?: "?"}（${l.relation}）" },
                        fontSize = 13.sp, color = PicoTheme.colorScheme.labelSecondary, maxLines = 3,
                    )
                }
            }
        }

        Toolbar {
            ToolbarAction(if (spin) "⏸ 停止旋转" else "▶ 自动旋转") { spin = !spin }
            ToolbarAction("↺ 重置视角") { yaw = 20f; pitch = -12f; Workspace.focusCard(null) }
            ToolbarAction(if (busy != null) "AI 处理中…" else "✦ AI 重新关联") { if (busy == null) Actions.relinkAll() }
            ToolbarAction("${s.cards.size} 卡片 · ${s.links.size} 关联") {}
        }
    }
}


/** Visible empty state for the volumetric graph: a framed volume, placeholder stars, and a hint card. */
@Composable
private fun EmptyGraphPlaceholder(
    modifier: Modifier,
    project: (P3) -> Triple<Float, Float, Float>,
    halfD: Float,
) {
    val density = LocalDensity.current
    // Fixed decorative constellation — rotates with the camera so the empty volume still feels 3D.
    val stars = remember {
        listOf(
            P3(-0.62f, -0.28f, -0.45f) to Palette[0],
            P3(-0.18f, 0.42f, -0.15f) to Palette[1],
            P3(0.55f, -0.36f, 0.22f) to Palette[2],
            P3(0.28f, 0.30f, 0.48f) to Palette[3],
            P3(-0.48f, 0.08f, 0.38f) to Palette[4],
            P3(0.08f, -0.48f, -0.32f) to Palette[5],
            P3(0.42f, 0.08f, -0.55f) to Palette[6],
        )
    }
    val starLinks = remember { listOf(0 to 1, 1 to 4, 4 to 0, 2 to 3, 3 to 6, 5 to 2, 1 to 3) }

    Box(modifier) {
        // Back plane so the volume is a visible room, not a transparent hole.
        Box(
            Modifier.align(Alignment.Center)
                .size(width = 900.dp, height = 620.dp)
                .zOffset { -halfD * 0.88f }
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF0E1428).copy(alpha = 0.78f))
                .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(32.dp)),
        )
        // Front frame, marking the near face of the volume.
        Box(
            Modifier.align(Alignment.Center)
                .fillMaxSize()
                .padding(20.dp)
                .zOffset { halfD * 0.08f }
                .border(2.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(28.dp)),
        )

        starLinks.forEach { (ia, ib) ->
            val a = stars[ia].first
            val b = stars[ib].first
            for (i in 1 until 6) {
                val t = i / 6f
                val (x, y, z) = project(a + (b - a) * t)
                val sz = 5
                val half = with(density) { sz.dp.toPx() } / 2f
                Box(
                    Modifier.offset { IntOffset((x - half).roundToInt(), (y - half).roundToInt()) }
                        .zOffset { z }
                        .size(sz.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.42f)),
                )
            }
        }
        stars.forEach { (p, color) ->
            val (x, y, z) = project(p)
            val sz = 16
            val half = with(density) { sz.dp.toPx() } / 2f
            Box(
                Modifier.offset { IntOffset((x - half).roundToInt(), (y - half).roundToInt()) }
                    .zOffset { z }
                    .size(sz.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
            )
        }

        Column(
            Modifier.align(Alignment.Center)
                .zOffset { halfD * 0.18f }
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141A2E).copy(alpha = 0.94f))
                .border(1.5.dp, Color(0xFF5B8CFF).copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                .padding(horizontal = 36.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("✨ 知识星图", style = PicoTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text("还没有卡片", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD5DEF5))
            Text(
                "在阅读窗口选择段落，点“生成知识卡片”，卡片会作为星星出现在这里。",
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = Color(0xFFB4C0DC),
                textAlign = TextAlign.Center,
            )
        }
    }
}
