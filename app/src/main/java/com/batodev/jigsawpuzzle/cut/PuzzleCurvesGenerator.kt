package com.batodev.jigsawpuzzle.cut

import java.util.concurrent.atomic.AtomicLong
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Generates the SVG outline for a puzzle's cut lines: wavy tab curves between
 * pieces (see [BezierEdgeCurves]) plus a border.
 */
class PuzzleCurvesGenerator {
    companion object {
        private const val DEFAULT_TENSION = 0.1
        private const val DEFAULT_JITTER = 0.05
    }

    var xn = 0.0
    var yn = 0.0
    var width = 0.0
    var height = 0.0
    private var offset = 0.0
    private var radius = 0.0

    /**
     * Generates the SVG string representing the puzzle piece outlines.
     * The SVG includes horizontal and vertical curves, and a border.
     * @return A String containing the SVG representation of the puzzle outlines.
     */
    fun generateSvg(): String {
        val edges =
            BezierEdgeCurves(
                GridDimensions(width, height, xn, yn),
                offset,
                DEFAULT_TENSION,
                DEFAULT_JITTER,
            )
        var data = "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.0\" "
        data += "width=\"$width\" height=\"$height\" viewBox=\"0 0 $width $height\">\n"
        data += "<path fill=\"none\" stroke=\"Black\" stroke-width=\"1\" d=\""
        data += edges.horizontal()
        data += "\" />\n"
        data += "<path fill=\"none\" stroke=\"Black\" stroke-width=\"1\" d=\""
        data += edges.vertical()
        data += "\" />\n"
        data += "<path fill=\"none\" stroke=\"Black\" stroke-width=\"1\" d=\""
        data += puzzleBorderPath(offset, width, height, radius)
        data += "\" />\n"
        data += "</svg>"
        return data
    }
}

/**
 * Straight/rounded rectangle border path for the puzzle outline, drawn once
 * around the whole grid (independent of the per-tab curve jitter).
 */
private fun puzzleBorderPath(
    offset: Double,
    width: Double,
    height: Double,
    radius: Double,
): String {
    var str = ""
    str += "M " + (offset + radius) + " " + offset + " "
    str += "L " + (offset + width - radius) + " " + offset + " "
    str += "A " + radius + " " + radius + " 0 0 1 " + (offset + width) + " " + (offset + radius) + " "
    str += "L " + (offset + width) + " " + (offset + height - radius) + " "
    str += "A " + radius + " " + radius + " 0 0 1 " + (offset + width - radius) + " " + (offset + height) + " "
    str += "L " + (offset + radius) + " " + (offset + height) + " "
    str += "A " + radius + " " + radius + " 0 0 1 " + offset + " " + (offset + height - radius) + " "
    str += "L " + offset + " " + (offset + radius) + " "
    str += "A " + radius + " " + radius + " 0 0 1 " + (offset + radius) + " " + offset + " "
    return str
}

/** Puzzle-grid size, grouped into one value so [BezierEdgeCurves] takes fewer constructor params. */
private data class GridDimensions(
    val width: Double,
    val height: Double,
    val xn: Double,
    val yn: Double,
)

/** Three consecutive bezier control points, i.e. the operands of one SVG "C" command. */
private data class CubicTriple(
    val p1: Pair<Double, Double>,
    val p2: Pair<Double, Double>,
    val p3: Pair<Double, Double>,
)

/** The 10 control points of one tab, grouped as they're consumed: an anchor plus 3 cubic triples. */
private data class TabControlPoints(
    val start: Pair<Double, Double>,
    val out: CubicTriple,
    val mid: CubicTriple,
    val back: CubicTriple,
)

/**
 * Computes the wavy "tab" curves along one axis of the puzzle grid (the
 * horizontal or vertical internal cut lines between pieces) as SVG
 * cubic-bezier path fragments, using [JitterSequence] to vary each tab so
 * neighboring pieces don't look identical.
 */
private class BezierEdgeCurves(
    private val dims: GridDimensions,
    private val offset: Double,
    private val t: Double,
    jitter: Double,
) {
    companion object {
        private const val ROUNDING_PRECISION = 100.0
        private const val LEAD_IN_FRACTION = 0.2
        private const val LEAD_OUT_FRACTION = 0.8
        private const val MID_FRACTION = 0.5
        private const val TENSION_MULTIPLIER_2 = 2.0
        private const val TENSION_MULTIPLIER_3 = 3.0
    }

    private val jitterSeq = JitterSequence(jitter)
    private var xi = 0.0
    private var yi = 0.0
    private var vertical = false
    private val flip get() = jitterSeq.flip

    fun horizontal(): String {
        vertical = false
        val str = StringBuilder()
        yi = 1.0
        while (yi < dims.yn) {
            xi = 0.0
            jitterSeq.first()
            val start = controlPoints().start
            str
                .append("M ")
                .append(start.first)
                .append(",")
                .append(start.second)
                .append(" ")
            while (xi < dims.xn) {
                val pts = controlPoints()
                appendCubic(str, pts.out)
                appendCubic(str, pts.mid)
                appendCubic(str, pts.back)
                jitterSeq.next()
                ++xi
            }
            ++yi
        }
        return str.toString()
    }

    fun vertical(): String {
        vertical = true
        val str = StringBuilder()
        xi = 1.0
        while (xi < dims.xn) {
            yi = 0.0
            jitterSeq.first()
            val start = controlPoints().start
            str
                .append("M ")
                .append(start.second)
                .append(",")
                .append(start.first)
                .append(" ")
            while (yi < dims.yn) {
                val pts = controlPoints()
                appendCubicSwapped(str, pts.out)
                appendCubicSwapped(str, pts.mid)
                appendCubicSwapped(str, pts.back)
                jitterSeq.next()
                ++yi
            }
            ++xi
        }
        return str.toString()
    }

    private fun appendCubic(
        str: StringBuilder,
        triple: CubicTriple,
    ) {
        str
            .append("C ")
            .append(triple.p1.first)
            .append(" ")
            .append(triple.p1.second)
            .append(" ")
            .append(triple.p2.first)
            .append(" ")
            .append(triple.p2.second)
            .append(" ")
            .append(triple.p3.first)
            .append(" ")
            .append(triple.p3.second)
            .append(" ")
    }

    private fun appendCubicSwapped(
        str: StringBuilder,
        triple: CubicTriple,
    ) {
        str
            .append("C ")
            .append(triple.p1.second)
            .append(" ")
            .append(triple.p1.first)
            .append(" ")
            .append(triple.p2.second)
            .append(" ")
            .append(triple.p2.first)
            .append(" ")
            .append(triple.p3.second)
            .append(" ")
            .append(triple.p3.first)
            .append(" ")
    }

    private fun segmentLength(): Double = if (vertical) dims.height / dims.yn else dims.width / dims.xn

    private fun segmentWidth(): Double = if (vertical) dims.width / dims.xn else dims.height / dims.yn

    private fun lCoord(v: Double): Double {
        val ret = offset + segmentLength() * (if (vertical) yi else xi) + segmentLength() * v
        return round2(ret)
    }

    private fun wCoord(v: Double): Double {
        val ret = offset + segmentWidth() * (if (vertical) xi else yi) + segmentWidth() * v * if (flip) -1.0 else 1.0
        return round2(ret)
    }

    private fun round2(v: Double): Double = (v * ROUNDING_PRECISION).roundToInt().toDouble() / ROUNDING_PRECISION

    // The 10 control points of one tab's cubic-bezier triple, expressed as fractions
    // of the segment (l axis) and jitter/tension offsets (w axis). `out`/`back` are
    // intentionally symmetric - the tab shape goes out from the segment then back to
    // the baseline.
    private fun controlPoints(): TabControlPoints {
        val a = jitterSeq.a
        val b = jitterSeq.b
        val c = jitterSeq.c
        val d = jitterSeq.d
        val e = jitterSeq.e

        val p0 = lCoord(0.0) to wCoord(0.0)
        val p1 = lCoord(LEAD_IN_FRACTION) to wCoord(a)
        val p2 = lCoord(MID_FRACTION + b + d) to wCoord(-t + c)
        val p3 = lCoord(MID_FRACTION - t + b) to wCoord(t + c)
        val p4 = lCoord(MID_FRACTION - TENSION_MULTIPLIER_2 * t + b - d) to wCoord(TENSION_MULTIPLIER_3 * t + c)
        val p5 = lCoord(MID_FRACTION + TENSION_MULTIPLIER_2 * t + b - d) to wCoord(TENSION_MULTIPLIER_3 * t + c)
        val p6 = lCoord(MID_FRACTION + t + b) to wCoord(t + c)
        val p7 = lCoord(MID_FRACTION + b + d) to wCoord(-t + c)
        val p8 = lCoord(LEAD_OUT_FRACTION) to wCoord(e)
        val p9 = lCoord(1.0) to wCoord(0.0)

        return TabControlPoints(
            start = p0,
            out = CubicTriple(p1, p2, p3),
            mid = CubicTriple(p4, p5, p6),
            back = CubicTriple(p7, p8, p9),
        )
    }
}

/**
 * Generates the small jitter offsets (a..e) that vary each puzzle-piece tab so
 * neighboring pieces don't look identical, plus the alternating `flip` bit that
 * keeps adjacent tabs pointing in opposite directions.
 */
private class JitterSequence(
    private val jitter: Double,
) {
    companion object {
        private const val RANDOM_MULTIPLIER = 10000.0
        private const val BOOL_THRESHOLD = 0.5
        private val seedOffset = AtomicLong(0L)

        private fun initialSeed(): Double = System.nanoTime().toDouble() + seedOffset.incrementAndGet()
    }

    private var seed = initialSeed()

    var a = 0.0
        private set
    var b = 0.0
        private set
    var c = 0.0
        private set
    var d = 0.0
        private set
    var e = 0.0
        private set
    var flip = false
        private set

    fun first() {
        e = uniform(-jitter, jitter)
        next()
    }

    fun next() {
        val flipOld = flip
        flip = randomBool()
        a = if (flip == flipOld) -e else e
        b = uniform(-jitter, jitter)
        c = uniform(-jitter, jitter)
        d = uniform(-jitter, jitter)
        e = uniform(-jitter, jitter)
    }

    private fun random(): Double {
        val x = sin(seed) * RANDOM_MULTIPLIER
        seed += 1.0
        return x - floor(x)
    }

    private fun uniform(
        min: Double,
        max: Double,
    ): Double {
        val r = random()
        return min + r * (max - min)
    }

    private fun randomBool(): Boolean = random() > BOOL_THRESHOLD
}
