package com.batodev.jigsawpuzzle.cut

import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import java.util.concurrent.atomic.AtomicLong // Import for AtomicLong

/**
 * A class for generating the SVG curves for the puzzle pieces.
 */
class PuzzleCurvesGenerator {

    companion object {
        // Using AtomicLong for a thread-safe incrementing seed component
        private val seedOffset = AtomicLong(0L)

        // Function to generate an initial seed, more robust than just System.nanoTime()
        private fun initializeSeed(): Double {
            return System.nanoTime().toDouble() + seedOffset.incrementAndGet()
        }
    }

    /**
     * Generates the SVG string representing the puzzle piece outlines.
     * The SVG includes horizontal and vertical curves, and a border.
     * @return A String containing the SVG representation of the puzzle outlines.
     */
    fun generateSvg(): String {
        var data = "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.0\" "
        data += "width=\"$width\" height=\"$height\" viewBox=\"0 0 $width $height\">\n"
        data += "<path fill=\"none\" stroke=\"Black\" stroke-width=\"1\" d=\""
        data += genHorizontalCurves()
        data += "\" />\n"
        data += "<path fill=\"none\" stroke=\"Black\" stroke-width=\"1\" d=\""
        data += genVerticalCurves()
        data += "\" />\n"
        data += "<path fill=\"none\" stroke=\"Black\" stroke-width=\"1\" d=\""
        data += genBorder()
        data += "\" />\n"
        data += "</svg>"
        return data
    }

    // Initialize seed using the new method
    private var seed = initializeSeed()
    private var a = 0.0
    private var b = 0.0
    private var c = 0.0
    private var d = 0.0
    private var e = 0.0
    private var t = 0.1
    private var j = 0.05
    private var xi = 0.0
    private var yi = 0.0
    var xn = 0.0
    var yn = 0.0
    private var offset = 0.0
    var width = 0.0
    var height = 0.0
    private var radius = 0.0
    private var flip = false
    private var vertical = false

    /**
     * Generates a pseudo-random double value between 0.0 (inclusive) and 1.0 (exclusive).
     * This method uses the sine function for randomness based on an internal seed.
     * @return A pseudo-random double.
     */
    private fun random(): Double {
        val x = sin(seed) * 10000
        seed += 1.0 // Incrementing seed here is part of the existing pseudo-random generation
        return x - floor(x)
    }

    /**
     * Generates a uniform random double value within a specified range.
     * @param min The minimum value (inclusive).
     * @param max The maximum value (exclusive).
     * @return A uniform random double within the specified range.
     */
    private fun uniform(min: Double, max: Double): Double {
        val r = random()
        return min + r * (max - min)
    }

    /**
     * Generates a random boolean value.
     * @return True if the random value is greater than 0.5, false otherwise.
     */
    private fun rBool(): Boolean {
        return random() > 0.5
    }

    /**
     * Initializes the first set of curve parameters.
     */
    private fun first() {
        e = uniform(-j, j)
        next()
    }

    /**
     * Generates the next set of curve parameters based on random values and previous state.
     */
    operator fun next() {
        val flipold = flip
        flip = rBool()
        a = if (flip == flipold) -e else e
        b = uniform(-j, j)
        c = uniform(-j, j)
        d = uniform(-j, j)
        e = uniform(-j, j)
    }

    /**
     * Calculates the segment length based on whether the curves are vertical or horizontal.
     * @return The segment length.
     */
    private fun sl(): Double {
        return if (vertical) height / yn else width / xn
    }

    /**
     * Calculates the segment width based on whether the curves are vertical or horizontal.
     * @return The segment width.
     */
    private fun sw(): Double {
        return if (vertical) width / xn else height / yn
    }

    /**
     * Calculates the offset length for a point on the curve.
     * @return The offset length.
     */
    private fun ol(): Double {
        return offset + sl() * if (vertical) yi else xi
    }

    /**
     * Calculates the offset width for a point on the curve.
     * @return The offset width.
     */
    private fun ow(): Double {
        return offset + sw() * if (vertical) xi else yi
    }

    /**
     * Calculates the 'l' coordinate for a given value 'v'.
     * @param v The input value.
     * @return The calculated 'l' coordinate, rounded to two decimal places.
     */
    private fun l(v: Double): Double {
        val ret = ol() + sl() * v
        return (ret * 100).roundToInt().toDouble() / 100
    }

    /**
     * Calculates the 'w' coordinate for a given value 'v'.
     * @param v The input value.
     * @return The calculated 'w' coordinate, rounded to two decimal places.
     */
    private fun w(v: Double): Double {
        val ret = ow() + sw() * v * if (flip) -1.0 else 1.0
        return (ret * 100).roundToInt().toDouble() / 100
    }

    /**
     * Calculates the 'l' coordinate for the starting point (0.0).
     * @return The 'l' coordinate for the starting point.
     */
    private fun p0l(): Double {
        return l(0.0)
    }

    /**
     * Calculates the 'w' coordinate for the starting point (0.0).
     * @return The 'w' coordinate for the starting point.
     */
    private fun p0w(): Double {
        return w(0.0)
    }

    /**
     * Calculates the 'l' coordinate for point 1 (0.2).
     * @return The 'l' coordinate for point 1.
     */
    private fun p1l(): Double {
        return l(0.2)
    }

    /**
     * Calculates the 'w' coordinate for point 1 (a).
     * @return The 'w' coordinate for point 1.
     */
    private fun p1w(): Double {
        return w(a)
    }

    /**
     * Calculates the 'l' coordinate for point 2 (0.5 + b + d).
     * @return The 'l' coordinate for point 2.
     */
    private fun p2l(): Double {
        return l(0.5 + b + d)
    }

    /**
     * Calculates the 'w' coordinate for point 2 (-t + c).
     * @return The 'w' coordinate for point 2.
     */
    private fun p2w(): Double {
        return w(-t + c)
    }

    /**
     * Calculates the 'l' coordinate for point 3 (0.5 - t + b).
     * @return The 'l' coordinate for point 3.
     */
    private fun p3l(): Double {
        return l(0.5 - t + b)
    }

    /**
     * Calculates the 'w' coordinate for point 3 (t + c).
     * @return The 'w' coordinate for point 3.
     */
    private fun p3w(): Double {
        return w(t + c)
    }

    /**
     * Calculates the 'l' coordinate for point 4 (0.5 - 2.0 * t + b - d).
     * @return The 'l' coordinate for point 4.
     */
    private fun p4l(): Double {
        return l(0.5 - 2.0 * t + b - d)
    }

    /**
     * Calculates the 'w' coordinate for point 4 (3.0 * t + c).
     * @return The 'w' coordinate for point 4.
     */
    private fun p4w(): Double {
        return w(3.0 * t + c)
    }

    /**
     * Calculates the 'l' coordinate for point 5 (0.5 + 2.0 * t + b - d).
     * @return The 'l' coordinate for point 5.
     */
    private fun p5l(): Double {
        return l(0.5 + 2.0 * t + b - d)
    }

    /**
     * Calculates the 'w' coordinate for point 5 (3.0 * t + c).
     * @return The 'w' coordinate for point 5.
     */
    private fun p5w(): Double {
        return w(3.0 * t + c)
    }

    /**
     * Calculates the 'l' coordinate for point 6 (0.5 + t + b).
     * @return The 'l' coordinate for point 6.
     */
    private fun p6l(): Double {
        return l(0.5 + t + b)
    }

    /**
     * Calculates the 'w' coordinate for point 6 (t + c).
     * @return The 'w' coordinate for point 6.
     */
    private fun p6w(): Double {
        return w(t + c)
    }

    /**
     * Calculates the 'l' coordinate for point 7 (0.5 + b + d).
     * @return The 'l' coordinate for point 7.
     */
    private fun p7l(): Double {
        return l(0.5 + b + d)
    }

    /**
     * Calculates the 'w' coordinate for point 7 (-t + c).
     * @return The 'w' coordinate for point 7.
     */
    private fun p7w(): Double {
        return w(-t + c)
    }

    /**
     * Calculates the 'l' coordinate for point 8 (0.8).
     * @return The 'l' coordinate for point 8.
     */
    private fun p8l(): Double {
        return l(0.8)
    }

    /**
     * Calculates the 'w' coordinate for point 8 (e).
     * @return The 'w' coordinate for point 8.
     */
    private fun p8w(): Double {
        return w(e)
    }

    /**
     * Calculates the 'l' coordinate for point 9 (1.0).
     * @return The 'l' coordinate for point 9.
     */
    private fun p9l(): Double {
        return l(1.0)
    }

    /**
     * Calculates the 'w' coordinate for point 9 (0.0).
     * @return The 'w' coordinate for point 9.
     */
    private fun p9w(): Double {
        return w(0.0)
    }

    /**
     * Generates the SVG path data for horizontal curves.
     * @return A String containing the SVG path data for horizontal curves.
     */
    private fun genHorizontalCurves(): String {
        val str = StringBuilder()
        vertical = false
        yi = 1.0
        while (yi < yn) {
            xi = 0.0
            first()
            str.append("M ").append(p0l()).append(",").append(p0w()).append(" ")
            while (xi < xn) {
                str.append("C ").append(p1l()).append(" ").append(p1w()).append(" ").append(p2l())
                    .append(" ").append(p2w()).append(" ").append(p3l()).append(" ").append(p3w())
                    .append(" ")
                str.append("C ").append(p4l()).append(" ").append(p4w()).append(" ").append(p5l())
                    .append(" ").append(p5w()).append(" ").append(p6l()).append(" ").append(p6w())
                    .append(" ")
                str.append("C ").append(p7l()).append(" ").append(p7w()).append(" ").append(p8l())
                    .append(" ").append(p8w()).append(" ").append(p9l()).append(" ").append(p9w())
                    .append(" ")
                next()
                ++xi
            }
            ++yi
        }
        return str.toString()
    }

    /**
     * Generates the SVG path data for vertical curves.
     * @return A String containing the SVG path data for vertical curves.
     */
    private fun genVerticalCurves(): String {
        val str = StringBuilder()
        vertical = true
        xi = 1.0
        while (xi < xn) {
            yi = 0.0
            first()
            str.append("M ").append(p0w()).append(",").append(p0l()).append(" ")
            while (yi < yn) {
                str.append("C ").append(p1w()).append(" ").append(p1l()).append(" ").append(p2w())
                    .append(" ").append(p2l()).append(" ").append(p3w()).append(" ").append(p3l())
                    .append(" ")
                str.append("C ").append(p4w()).append(" ").append(p4l()).append(" ").append(p5w())
                    .append(" ").append(p5l()).append(" ").append(p6w()).append(" ").append(p6l())
                    .append(" ")
                str.append("C ").append(p7w()).append(" ").append(p7l()).append(" ").append(p8w())
                    .append(" ").append(p8l()).append(" ").append(p9w()).append(" ").append(p9l())
                    .append(" ")
                next()
                ++yi
            }
            ++xi
        }
        return str.toString()
    }

    /**
     * Generates the SVG path data for the border of the puzzle.
     * @return A String containing the SVG path data for the border.
     */
    private fun genBorder(): String {
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
}
