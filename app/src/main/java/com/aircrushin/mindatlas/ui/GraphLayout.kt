package com.aircrushin.mindatlas.ui

import com.aircrushin.mindatlas.data.Card
import com.aircrushin.mindatlas.data.Link
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class P3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: P3) = P3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: P3) = P3(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Float) = P3(x * k, y * k, z * k)
    fun len() = sqrt(x * x + y * y + z * z)

    /** Rotate around Y (yaw) then X (pitch), in degrees. */
    fun rotate(yawDeg: Float, pitchDeg: Float): P3 {
        val a = Math.toRadians(yawDeg.toDouble()); val b = Math.toRadians(pitchDeg.toDouble())
        val x1 = (x * cos(a) + z * sin(a)).toFloat()
        val z1 = (-x * sin(a) + z * cos(a)).toFloat()
        val y2 = (y * cos(b) - z1 * sin(b)).toFloat()
        val z2 = (y * sin(b) + z1 * cos(b)).toFloat()
        return P3(x1, y2, z2)
    }
}

/**
 * Deterministic 3D force-directed layout normalised to the unit cube [-1, 1].
 * Cards from the same document get a weak pull to a shared anchor so readings cluster in space.
 */
object GraphLayout {
    fun layout(cards: List<Card>, links: List<Link>, iterations: Int = 220): Map<String, P3> {
        if (cards.isEmpty()) return emptyMap()
        if (cards.size == 1) return mapOf(cards[0].id to P3(0f, 0f, 0f))
        val idx = cards.mapIndexed { i, c -> c.id to i }.toMap()
        val rnd = Random(cards.size * 31 + links.size)
        val pos = Array(cards.size) { i ->
            val r = Random(cards[i].id.hashCode())
            P3(r.nextFloat() * 2 - 1, r.nextFloat() * 2 - 1, r.nextFloat() * 2 - 1)
        }
        val docIds = cards.map { it.docId }.distinct()
        val anchors = docIds.mapIndexed { i, d ->
            val a = 2 * Math.PI * i / max(1, docIds.size)
            d to P3((0.6 * cos(a)).toFloat(), (0.35 * sin(a * 1.7)).toFloat(), (0.6 * sin(a)).toFloat())
        }.toMap()
        val edges = links.mapNotNull { l -> val a = idx[l.from]; val b = idx[l.to]; if (a != null && b != null) a to b else null }
        val k = 0.9f / sqrt(cards.size.toFloat())
        for (it in 0 until iterations) {
            val t = 0.12f * (1f - it.toFloat() / iterations) + 0.005f
            val disp = Array(cards.size) { P3(0f, 0f, 0f) }
            for (i in cards.indices) for (j in i + 1 until cards.size) {
                var d = pos[i] - pos[j]
                var l = d.len()
                if (l < 1e-3f) { d = P3(rnd.nextFloat() - .5f, rnd.nextFloat() - .5f, rnd.nextFloat() - .5f); l = d.len() }
                val f = k * k / l
                val v = d * (f / l)
                disp[i] = disp[i] + v; disp[j] = disp[j] - v
            }
            for ((a, b) in edges) {
                val d = pos[a] - pos[b]
                val l = max(d.len(), 1e-3f)
                val v = d * (l / k / l * 0.5f * l)
                disp[a] = disp[a] - v; disp[b] = disp[b] + v
            }
            for (i in cards.indices) {
                val anchor = anchors[cards[i].docId] ?: P3(0f, 0f, 0f)
                disp[i] = disp[i] + (anchor - pos[i]) * 0.08f
                val l = disp[i].len()
                if (l > 0) pos[i] = pos[i] + disp[i] * (min(l, t) / l)
            }
        }
        // Normalise into the unit cube, centred.
        val cx = pos.map { it.x }.average().toFloat(); val cy = pos.map { it.y }.average().toFloat(); val cz = pos.map { it.z }.average().toFloat()
        val c = P3(cx, cy, cz)
        val maxR = pos.maxOf { (it - c).len() }.coerceAtLeast(1e-3f)
        return cards.indices.associate { i -> cards[i].id to (pos[i] - c) * (1f / maxR) }
    }
}
