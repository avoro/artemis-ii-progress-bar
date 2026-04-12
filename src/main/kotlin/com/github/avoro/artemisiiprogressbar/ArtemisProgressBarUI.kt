package com.github.avoro.artemisiiprogressbar

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.Timer
import javax.swing.plaf.basic.BasicProgressBarUI

class ArtemisProgressBarUI : BasicProgressBarUI() {

    companion object {
        @JvmStatic
        fun createUI(c: JComponent): ArtemisProgressBarUI = ArtemisProgressBarUI()

        private const val NATIVE_H = 20
        private const val ROCKET_W = 38
        private const val ROCKET_H = 14
        private const val MOON_W = 22
        private const val MOON_H = 20

        // Palette
        private val SPACE1 = Color(0x1a1a2e)
        private val SPACE2 = Color(0x16162a)
        private val SPACE3 = Color(0x121226)
        private val STAR1 = Color(0xffffff)
        private val STAR2 = Color(0xc8c8d0)
        private val STAR3 = Color(0x8888a0)
        private val MOON_EDGE = Color(0x9090a0)
        private val MOON_LIGHT = Color(0xa8a8b8)
        private val MOON_MID = Color(0x7a7a8e)
        private val MOON_DARK = Color(0x5a5a6e)
        private val MOON_CRATER = Color(0x686878)
        private val OR1 = Color(0xd89040)
        private val OR2 = Color(0xc07830)
        private val OR3 = Color(0xa86820)
        private val OR_H = Color(0xe8a858)
        private val OR_L = Color(0xf0c070)
        private val WH1 = Color(0xf0f0f8)
        private val WH2 = Color(0xd8d8e8)
        private val WH3 = Color(0xb8b8c8)
        private val WH4 = Color(0x9898a8)
        private val TIP = Color(0xc0c0d0)
        private val CAP_D = Color(0xa0a0b0)
        private val NOZ = Color(0x505060)
        private val NOZ_D = Color(0x383848)
        private val FILL1 = Color(0x2a4a7a)
        private val FILL2 = Color(0x3868a8)

        private val CHAR_MAP = mapOf(
            'T' to TIP,   'P' to WH1,  'p' to WH2,  'Q' to CAP_D,
            'H' to OR_H,  'O' to OR1,  'o' to OR2,  'D' to OR3,  'L' to OR_L,
            'W' to WH1,   'w' to WH2,  'S' to WH3,  's' to WH4,
            'N' to NOZ,   'n' to NOZ_D,
        )

        private val MOON_MAP = mapOf(
            'x' to MOON_EDGE, 'L' to MOON_LIGHT, 'M' to MOON_MID,
            'E' to MOON_DARK, 'c' to MOON_CRATER,
        )

        private val ROCKET_ROWS = arrayOf(
            "........................................",
            "................Wwwwwp..................",
            "...............NWwwSwwpQ................",
            "................WwwSwwpQ................",
            ".............NHHOOOoooDOOooLLOPpQ.......",
            "............NHHHOOOOoooDOOoooLLOPppQT...",
            "...........nNHHHOOOOOoooDOOooooLLOPppQT.",
            "...........nNHHHOOOOOoooDOOooooLLOPppQT.",
            "............NHHHOOOOoooDOOoooLLOPppQT...",
            ".............NHHOOOoooDOOooLLOPpQ.......",
            "................WwwSwwpQ................",
            "...............NWwwSwwpQ................",
            "................Wwwwwp..................",
            "........................................",
        )

        private val MOON_ROWS = arrayOf(
            "........xxxxxx........",
            "......xxLLLLLLxx......",
            ".....xLLLLEELLLLx.....",
            "....xLLLEEEELLLLLx....",
            "...xLLLLEEELLLLLLLx...",
            "..xLLLLLLLLLLLcLLLLx..",
            "..xLMLLLLLLLLccLLMLx..",
            ".xLMMMLLLLLLLcLLMMLLx.",
            ".xLMMMLLLLLLLLLLMMLLx.",
            ".xLLMLLLLcLLLLLLLLLLx.",
            ".xLLLLLLccLLLLLLLLLLx.",
            ".xLLLLLLcLLLLLLMMLLLx.",
            ".xLLLMMLLLLLLLMMMLLLx.",
            "..xLMMMLLLLLLLLMLLLx..",
            "..xLLMLLLLLLLLLLLLLx..",
            "...xLLLLLLLLLLcLLLx...",
            "....xLLLLLLLLccLLx....",
            ".....xLLLLLLLcLLx.....",
            "......xxLLLLLLxx......",
            "........xxxxxx........",
        )

        data class Pixel(val x: Int, val y: Int, val color: Color)

        private fun parseRows(rows: Array<String>, map: Map<Char, Color>): List<Pixel> =
            rows.flatMapIndexed { y, row ->
                row.mapIndexedNotNull { x, ch -> map[ch]?.let { Pixel(x, y, it) } }
            }

        val ROCKET_PIXELS: List<Pixel> = parseRows(ROCKET_ROWS, CHAR_MAP)
        val ROCKET_TOP_SRB: List<Pixel> = ROCKET_PIXELS.filter { it.y in 1..3 }
        val ROCKET_CORE: List<Pixel>    = ROCKET_PIXELS.filter { it.y in 4..9 }
        val ROCKET_BOT_SRB: List<Pixel> = ROCKET_PIXELS.filter { it.y in 10..12 }
        val MOON_PIXELS: List<Pixel> = parseRows(MOON_ROWS, MOON_MAP)

        data class Star(val nx: Double, val ny: Double, val color: Color, val twinkle: Boolean, val phase: Int)

        val STARS: List<Star> = buildList {
            var seed = 42L
            fun next(): Double {
                seed = seed * 6364136223846793005L + 1442695040888963407L
                return ((seed ushr 33).toDouble() / 2147483648.0).coerceIn(0.0, 0.9999)
            }
            repeat(40) {
                val nx = next()
                val ny = next()
                val color = when {
                    next() < 0.3 -> STAR3
                    next() < 0.6 -> STAR2
                    else -> STAR1
                }
                val twinkle = next() < 0.3
                val phase = (next() * 60).toInt()
                add(Star(nx, ny, color, twinkle, phase))
            }
        }
    }

    private var frame = 0
    private var timer: Timer? = null

    override fun installUI(c: JComponent) {
        super.installUI(c)
        progressBar.isOpaque = false
        timer = Timer(33) {
            frame = (frame + 1) % 3600
            progressBar?.repaint()
        }
        timer?.start()
    }

    override fun uninstallUI(c: JComponent) {
        timer?.stop()
        timer = null
        super.uninstallUI(c)
    }

    override fun getPreferredSize(c: JComponent): Dimension =
        Dimension(super.getPreferredSize(c).width, NATIVE_H * 2)

    override fun paintString(g: Graphics, x: Int, y: Int, w: Int, h: Int, amountFull: Int, d: Insets) {
        // suppress text rendering over pixel art
    }

    private fun paintScene(g: Graphics2D, width: Int, height: Int, progress: Double, indeterminate: Boolean = false) {
        val ps = maxOf(1, height / NATIVE_H)
        val nw = width / ps

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)

        // Background: 3 horizontal bands
        for (y in 0 until NATIVE_H) {
            g.color = when {
                y < 7  -> SPACE1
                y < 14 -> SPACE2
                else   -> SPACE3
            }
            g.fillRect(0, y * ps, width, ps)
        }

        // Stars
        for (star in STARS) {
            val sx = (star.nx * nw).toInt()
            val sy = (star.ny * NATIVE_H).toInt()
            if (!star.twinkle || (frame + star.phase) % 60 < 40) {
                g.color = star.color
                g.fillRect(sx * ps, sy * ps, ps, ps)
            }
        }

        // Progress fill: animated diagonal stripe, stops before moon
        val fillNw = ((nw - MOON_W - 2) * progress).toInt().coerceAtLeast(0)
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f)
        for (y in 0 until NATIVE_H) {
            val rowOffset = (y + frame) % 6
            var x = 0
            while (x < fillNw) {
                val phase = (x + rowOffset) % 6
                g.color = if (phase < 3) FILL1 else FILL2
                val span = minOf(if (phase < 3) 3 - phase else 6 - phase, fillNw - x)
                g.fillRect(x * ps, y * ps, span * ps, ps)
                x += span
            }
        }
        g.composite = AlphaComposite.SrcOver

        // Moon: anchored to right edge
        val moonX = nw - MOON_W
        val moonY = (NATIVE_H - MOON_H) / 2
        for (p in MOON_PIXELS) {
            g.color = p.color
            g.fillRect((moonX + p.x) * ps, (moonY + p.y) * ps, ps, ps)
        }

        // Rocket: travels left → right as progress increases
        val travelNw = nw - MOON_W - ROCKET_W - 2
        val rocketX = (travelNw * progress).toInt().coerceAtLeast(0)
        val rocketY = (NATIVE_H - ROCKET_H) / 2

        when {
            indeterminate || progress < 0.5 -> {
                for (p in ROCKET_PIXELS) {
                    g.color = p.color
                    g.fillRect((rocketX + p.x) * ps, (rocketY + p.y) * ps, ps, ps)
                }
            }
            progress < 0.75 -> {
                val sep = ((progress - 0.5) / 0.25).toFloat()
                val srbOffset = (sep * 5).toInt()
                val srbAlpha = 1.0f - sep

                // Core stays in place
                for (p in ROCKET_CORE) {
                    g.color = p.color
                    g.fillRect((rocketX + p.x) * ps, (rocketY + p.y) * ps, ps, ps)
                }

                // SRBs fall back and drift outward, then fade
                val srbBackOffset = (sep * 8).toInt()
                g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, srbAlpha)
                for (p in ROCKET_TOP_SRB) {
                    g.color = p.color
                    g.fillRect((rocketX + p.x - srbBackOffset) * ps, (rocketY + p.y - srbOffset) * ps, ps, ps)
                }
                for (p in ROCKET_BOT_SRB) {
                    g.color = p.color
                    g.fillRect((rocketX + p.x - srbBackOffset) * ps, (rocketY + p.y + srbOffset) * ps, ps, ps)
                }
                g.composite = AlphaComposite.SrcOver
            }
            else -> {
                for (p in ROCKET_CORE) {
                    g.color = p.color
                    g.fillRect((rocketX + p.x) * ps, (rocketY + p.y) * ps, ps, ps)
                }
            }
        }
    }

    override fun paintDeterminate(g: Graphics, c: JComponent) {
        val g2 = g.create() as Graphics2D
        try {
            paintScene(g2, c.width, c.height, progressBar.percentComplete, indeterminate = false)
        } finally {
            g2.dispose()
        }
    }

    override fun paintIndeterminate(g: Graphics, c: JComponent) {
        val g2 = g.create() as Graphics2D
        try {
            val p = (frame % 120) / 120.0
            paintScene(g2, c.width, c.height, p, indeterminate = true)
        } finally {
            g2.dispose()
        }
    }
}
