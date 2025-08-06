package com.batodev.jigsawpuzzle.cut

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test

class PuzzleCurvesGeneratorTest {
    @Test
    fun ensureRandomness() {
        // Configure both generators with the exact same parameters
        val commonWidth = 300.0
        val commonHeight = 200.0
        val commonXn = 3.0 // Using a value like 3 for xn and yn
        val commonYn = 3.0 // to have enough curves to show differences

        val generator1 = PuzzleCurvesGenerator().apply {
            width = commonWidth
            height = commonHeight
            xn = commonXn
            yn = commonYn
        }
        val svgString1 = generator1.generateSvg()

        val generator2 = PuzzleCurvesGenerator().apply {
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
        assertThat("Generated SVGs should be different due to internal randomness", svgString1, `is`(not(equalTo(svgString2))))
    }
}
