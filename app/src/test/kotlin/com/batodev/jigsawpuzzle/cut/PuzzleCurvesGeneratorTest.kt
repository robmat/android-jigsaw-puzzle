package com.batodev.jigsawpuzzle.cut

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test

class PuzzleCurvesGeneratorTest {
    @Test
    fun ensureRandomness() {
        // Configure both generators with the exact same parameters
        val commonWidth = 300.0
        val commonHeight = 200.0
        val commonXn = 3.0 // Using a value like 3 for xn and yn
        val commonYn = 3.0 // to have enough curves to show differences

        val generator1 =
            PuzzleCurvesGenerator().apply {
                width = commonWidth
                height = commonHeight
                xn = commonXn
                yn = commonYn
            }
        val svgString1 = generator1.generateSvg()

        val generator2 =
            PuzzleCurvesGenerator().apply {
                width = commonWidth
                height = commonHeight
                xn = commonXn
                yn = commonYn
                // No changes in parameters, relying on internal randomness
            }
        val svgString2 = generator2.generateSvg()

        assertThat("SVG string 1 should not be null", svgString1, `is`(notNullValue()))
        assertThat("SVG string 2 should not be null", svgString2, `is`(notNullValue()))
        // Assert that the two generated SVG strings are different
        assertThat(
            "Generated SVGs should be different due to internal randomness",
            svgString1,
            `is`(not(equalTo(svgString2))),
        )
    }

    @Test
    fun generatesWellFormedSvg() {
        val generator =
            PuzzleCurvesGenerator().apply {
                width = 400.0
                height = 300.0
                xn = 4.0
                yn = 3.0
            }
        val svgString = generator.generateSvg()

        assertThat("SVG should start with <svg", svgString, startsWith("<svg"))
        assertThat("SVG should end with </svg>", svgString, endsWith("</svg>"))
        assertThat("SVG should contain width attribute", svgString, containsString("width=\"400.0\""))
        assertThat("SVG should contain height attribute", svgString, containsString("height=\"300.0\""))
    }
}
