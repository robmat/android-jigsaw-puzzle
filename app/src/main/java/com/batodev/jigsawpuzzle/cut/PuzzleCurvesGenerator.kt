package com.batodev.jigsawpuzzle.cut

import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A class for generating the SVG curves for the puzzle pieces.
 */
class PuzzleCurvesGenerator {
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

    private var seed = 2.0
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
    private fun random(): Double {
        val x = sin(seed) * 10000
        seed += 1.0
        return x - floor(x)
    }

    private fun uniform(min: Double, max: Double): Double {
        val r = random()
        return min + r * (max - min)
    }

    private fun rBool(): Boolean {
        return random() > 0.5
    }

    private fun first() {
        e = uniform(-j, j)
        next()
    }

    operator fun next() {
        val flipold = flip
        flip = rBool()
        a = if (flip == flipold) -e else e
        b = uniform(-j, j)
        c = uniform(-j, j)
        d = uniform(-j, j)
        e = uniform(-j, j)
    }

    private fun sl(): Double {
        return if (vertical) height / yn else width / xn
    }

    private fun sw(): Double {
        return if (vertical) width / xn else height / yn
    }

    private fun ol(): Double {
        return offset + sl() * if (vertical) yi else xi
    }

    private fun ow(): Double {
        return offset + sw() * if (vertical) xi else yi
    }

    private fun l(v: Double): Double {
        val ret = ol() + sl() * v
        return (ret * 100).roundToInt().toDouble() / 100
    }

    private fun w(v: Double): Double {
        val ret = ow() + sw() * v * if (flip) -1.0 else 1.0
        return (ret * 100).roundToInt().toDouble() / 100
    }

    private fun p0l(): Double {
        return l(0.0)
    }

    private fun p0w(): Double {
        return w(0.0)
    }

    private fun p1l(): Double {
        return l(0.2)
    }

    private fun p1w(): Double {
        return w(a)
    }

    private fun p2l(): Double {
        return l(0.5 + b + d)
    }

    private fun p2w(): Double {
        return w(-t + c)
    }

    private fun p3l(): Double {
        return l(0.5 - t + b)
    }

    private fun p3w(): Double {
        return w(t + c)
    }

    private fun p4l(): Double {
        return l(0.5 - 2.0 * t + b - d)
    }

    private fun p4w(): Double {
        return w(3.0 * t + c)
    }

    private fun p5l(): Double {
        return l(0.5 + 2.0 * t + b - d)
    }

    private fun p5w(): Double {
        return w(3.0 * t + c)
    }

    private fun p6l(): Double {
        return l(0.5 + t + b)
    }

    private fun p6w(): Double {
        return w(t + c)
    }

    private fun p7l(): Double {
        return l(0.5 + b + d)
    }

    private fun p7w(): Double {
        return w(-t + c)
    }

    private fun p8l(): Double {
        return l(0.8)
    }

    private fun p8w(): Double {
        return w(e)
    }

    private fun p9l(): Double {
        return l(1.0)
    }

    private fun p9w(): Double {
        return w(0.0)
    }

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
