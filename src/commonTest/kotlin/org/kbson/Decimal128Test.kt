/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kbson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import org.kbson.Decimal128.Companion.NEGATIVE_INFINITY
import org.kbson.Decimal128.Companion.NEGATIVE_NaN
import org.kbson.Decimal128.Companion.NEGATIVE_ZERO
import org.kbson.Decimal128.Companion.NaN
import org.kbson.Decimal128.Companion.POSITIVE_INFINITY
import org.kbson.Decimal128.Companion.POSITIVE_ZERO
import org.kbson.Decimal128.Companion.fromIEEE754BIDEncoding
import org.kbson.Decimal128.Companion.parse

class Decimal128Test {

    @Test
    fun shouldHaveCorrectConstants() {
        // expect
        assertEquals(fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000000uL), POSITIVE_ZERO)
        assertEquals(fromIEEE754BIDEncoding(0xb040000000000000uL, 0x0000000000000000uL), NEGATIVE_ZERO)
        assertEquals(fromIEEE754BIDEncoding(0x7800000000000000uL, 0x0000000000000000uL), POSITIVE_INFINITY)
        assertEquals(fromIEEE754BIDEncoding(0xf800000000000000uL, 0x0000000000000000uL), NEGATIVE_INFINITY)
        assertEquals(fromIEEE754BIDEncoding(0x7c00000000000000uL, 0x0000000000000000uL), NaN)
    }

    @Test
    fun shouldConstructFromHighAndLow() {
        // given
        val subject = fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000001uL)

        // then
        assertEquals(0x3040000000000000uL, subject.high)
        assertEquals(0x0000000000000001uL, subject.low)
    }

    @Test
    fun shouldConstructFromSimpleString() {
        // expect
        assertEquals(fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000000uL), parse("0"))
        assertEquals(fromIEEE754BIDEncoding(0xb040000000000000uL, 0x0000000000000000uL), parse("-0"))
        assertEquals(fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000001uL), parse("1"))
        assertEquals(fromIEEE754BIDEncoding(0xb040000000000000uL, 0x0000000000000001uL), parse("-1"))
        assertEquals(fromIEEE754BIDEncoding(0x3040000000000000uL, 0x002bdc545d6b4b87uL), parse("12345678901234567"))
        assertEquals(fromIEEE754BIDEncoding(0x3040000000000000uL, 0x000000e67a93c822uL), parse("989898983458"))
        assertEquals(fromIEEE754BIDEncoding(0xb040000000000000uL, 0x002bdc545d6b4b87uL), parse("-12345678901234567"))
        assertEquals(fromIEEE754BIDEncoding(0x3036000000000000uL, 0x0000000000003039uL), parse("0.12345"))
        assertEquals(fromIEEE754BIDEncoding(0x3032000000000000uL, 0x0000000000003039uL), parse("0.0012345"))
        assertEquals(fromIEEE754BIDEncoding(0x3040000000000000uL, 0x002bdc545d6b4b87uL), parse("00012345678901234567"))
    }

    @Test
    fun shouldRoundExactly() {
        // expect
        assertEquals(parse("1.234567890123456789012345678901234"), parse("1.234567890123456789012345678901234"))
        assertEquals(parse("1.234567890123456789012345678901234"), parse("1.2345678901234567890123456789012340"))
        assertEquals(parse("1.234567890123456789012345678901234"), parse("1.23456789012345678901234567890123400"))
        assertEquals(parse("1.234567890123456789012345678901234"), parse("1.234567890123456789012345678901234000"))
    }

    @Test
    fun shouldClampPositiveExponents() {
        // expect
        assertEquals(parse("10E6111"), parse("1E6112"))
        assertEquals(parse("100E6111"), parse("1E6113"))
        assertEquals(parse("100000000000000000000000000000000E+6111"), parse("1E6143"))
        assertEquals(parse("1000000000000000000000000000000000E+6111"), parse("1E6144"))
        assertEquals(parse("1100000000000000000000000000000000E+6111"), parse("11E6143"))
        assertEquals(parse("0E6111"), parse("0E8000"))
        assertEquals(parse("0E6111"), parse("0E2147483647"))
        assertEquals(parse("-10E6111"), parse("-1E6112"))
        assertEquals(parse("-100E6111"), parse("-1E6113"))
        assertEquals(parse("-100000000000000000000000000000000E+6111"), parse("-1E6143"))
        assertEquals(parse("-1000000000000000000000000000000000E+6111"), parse("-1E6144"))
        assertEquals(parse("-1100000000000000000000000000000000E+6111"), parse("-11E6143"))
        assertEquals(parse("-0E6111"), parse("-0E8000"))
        assertEquals(parse("-0E6111"), parse("-0E2147483647"))
    }

    @Test
    fun shouldClampNegativeExponents() {
        // expect
        assertEquals(parse("0E-6176"), parse("0E-8000"))
        assertEquals(parse("0E-6176"), parse("0E-2147483647"))
        assertEquals(parse("1E-6176"), parse("10E-6177"))
        assertEquals(parse("1E-6176"), parse("100E-6178"))
        assertEquals(parse("11E-6176"), parse("110E-6177"))
        assertEquals(parse("-0E-6176"), parse("-0E-8000"))
        assertEquals(parse("-0E-6176"), parse("-0E-2147483647"))
        assertEquals(parse("-1E-6176"), parse("-10E-6177"))
        assertEquals(parse("-1E-6176"), parse("-100E-6178"))
        assertEquals(parse("-11E-6176"), parse("-110E-6177"))
    }

    @Test
    fun shouldDetectInfinity() {
        // expect
        assertTrue(POSITIVE_INFINITY.isInfinite)
        assertTrue(NEGATIVE_INFINITY.isInfinite)
        assertFalse(parse("0").isInfinite)
        assertFalse(parse("9.999999999999999999999999999999999E+6144").isInfinite)
        assertFalse(parse("9.999999999999999999999999999999999E-6143").isInfinite)
        assertFalse(POSITIVE_INFINITY.isFinite)
        assertFalse(NEGATIVE_INFINITY.isFinite)
        assertTrue(parse("0").isFinite)
        assertTrue(parse("9.999999999999999999999999999999999E+6144").isFinite)
        assertTrue(parse("9.999999999999999999999999999999999E-6143").isFinite)
    }

    @Test
    fun shouldDetectNaN() {
        // expect
        assertTrue(NaN.isNaN)
        assertTrue(fromIEEE754BIDEncoding(0x7e00000000000000uL, 0x0uL).isNaN) // SNaN
        assertFalse(POSITIVE_INFINITY.isNaN)
        assertFalse(NEGATIVE_INFINITY.isNaN)
        assertFalse(parse("0").isNaN)
        assertFalse(parse("9.999999999999999999999999999999999E+6144").isNaN)
        assertFalse(parse("9.999999999999999999999999999999999E-6143").isNaN)
    }

    @Test
    fun shouldConvertNaNToString() {
        // expect
        assertEquals("NaN", NaN.toString())
    }

    @Test
    fun shouldConvertNaNFromString() {
        // expect
        assertEquals(NaN, parse("NaN"))
        assertEquals(NaN, parse("nan"))
        assertEquals(NaN, parse("nAn"))
        assertEquals(NEGATIVE_NaN, parse("-NaN"))
        assertEquals(NEGATIVE_NaN, parse("-nan"))
        assertEquals(NEGATIVE_NaN, parse("-nAn"))
    }

    @Test
    fun shouldConvertInfinityToString() {
        // expect
        assertEquals("Infinity", POSITIVE_INFINITY.toString())
        assertEquals("-Infinity", NEGATIVE_INFINITY.toString())
    }

    @Test
    fun shouldConvertInfinityFromString() {
        // expect
        assertEquals(POSITIVE_INFINITY, parse("Inf"))
        assertEquals(POSITIVE_INFINITY, parse("inf"))
        assertEquals(POSITIVE_INFINITY, parse("inF"))
        assertEquals(POSITIVE_INFINITY, parse("+Inf"))
        assertEquals(POSITIVE_INFINITY, parse("+inf"))
        assertEquals(POSITIVE_INFINITY, parse("+inF"))
        assertEquals(POSITIVE_INFINITY, parse("Infinity"))
        assertEquals(POSITIVE_INFINITY, parse("infinity"))
        assertEquals(POSITIVE_INFINITY, parse("infiniTy"))
        assertEquals(POSITIVE_INFINITY, parse("+Infinity"))
        assertEquals(POSITIVE_INFINITY, parse("+infinity"))
        assertEquals(POSITIVE_INFINITY, parse("+infiniTy"))
        assertEquals(NEGATIVE_INFINITY, parse("-Inf"))
        assertEquals(NEGATIVE_INFINITY, parse("-inf"))
        assertEquals(NEGATIVE_INFINITY, parse("-inF"))
        assertEquals(NEGATIVE_INFINITY, parse("-Infinity"))
        assertEquals(NEGATIVE_INFINITY, parse("-infinity"))
        assertEquals(NEGATIVE_INFINITY, parse("-infiniTy"))
    }

    @Test
    fun shouldConvertFiniteToString() {
        // expect
        assertEquals("0", parse("0").toString())
        assertEquals("-0", parse("-0").toString())
        assertEquals("0E+10", parse("0E10").toString())
        assertEquals("-0E+10", parse("-0E10").toString())
        assertEquals("1", parse("1").toString())
        assertEquals("-1", parse("-1").toString())
        assertEquals("-1.1", parse("-1.1").toString())
        assertEquals("1.23E-7", parse("123E-9").toString())
        assertEquals("0.00000123", parse("123E-8").toString())
        assertEquals("0.0000123", parse("123E-7").toString())
        assertEquals("0.000123", parse("123E-6").toString())
        assertEquals("0.00123", parse("123E-5").toString())
        assertEquals("0.0123", parse("123E-4").toString())
        assertEquals("0.123", parse("123E-3").toString())
        assertEquals("1.23", parse("123E-2").toString())
        assertEquals("12.3", parse("123E-1").toString())
        assertEquals("123", parse("123E0").toString())
        assertEquals("1.23E+3", parse("123E1").toString())
        assertEquals("0.0001234", parse("1234E-7").toString())
        assertEquals("0.001234", parse("1234E-6").toString())
        assertEquals("1E+6", parse("1E6").toString())
    }

    @Test
    @Suppress("EqualsNullCall")
    fun testEquals() {
        // given
        val d1 = fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000001uL)
        val d2 = fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000001uL)
        val d3 = fromIEEE754BIDEncoding(0x3040000000000001uL, 0x0000000000000001uL)
        val d4 = fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000011uL)

        // expect
        assertEquals(d1, d1)
        assertEquals(d1, d2)
        assertNotEquals(d1, d3)
        assertNotEquals(d1, d4)
        assertFalse(d1.equals(null))
        assertFalse(d1.equals(0x0uL))
    }

    @Test
    fun testHashCode() {
        // expect
        assertEquals(809500703, fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000001uL).hashCode())
    }

    @Test
    @Suppress("SwallowedException", "LongMethod")
    fun shouldNotRoundInexactly() {
        try {
            parse("12345678901234567890123456789012345E+6111")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("123456789012345678901234567890123456E+6111")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("1234567890123456789012345678901234567E+6111")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("12345678901234567890123456789012345E-6176")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("123456789012345678901234567890123456E-6176")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("1234567890123456789012345678901234567E-6176")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("-12345678901234567890123456789012345E+6111")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("-123456789012345678901234567890123456E+6111")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("-1234567890123456789012345678901234567E+6111")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("-12345678901234567890123456789012345E-6176")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("-123456789012345678901234567890123456E-6176")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("-1234567890123456789012345678901234567E-6176")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
    }

    @Test
    @Suppress("SwallowedException")
    fun shouldNotClampLargeExponentsIfNoExtraPrecisionIsAvailable() {
        try {
            parse("1234567890123456789012345678901234E+6112")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("1234567890123456789012345678901234E+6113")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("1234567890123456789012345678901234E+6114")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("-1234567890123456789012345678901234E+6112")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("-1234567890123456789012345678901234E+6113")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("-1234567890123456789012345678901234E+6114")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
    }

    @Test
    @Suppress("SwallowedException")
    fun shouldNotClampSmallExponentsIfNoExtraPrecisionCanBeDiscarded() {
        try {
            parse("1234567890123456789012345678901234E-6177")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("1234567890123456789012345678901234E-6178")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("1234567890123456789012345678901234E-6179")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("-1234567890123456789012345678901234E-6177")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("-1234567890123456789012345678901234E-6178")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            parse("-1234567890123456789012345678901234E-6179")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
    }
}
