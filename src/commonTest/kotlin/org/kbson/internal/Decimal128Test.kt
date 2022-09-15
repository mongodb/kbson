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
package org.kbson.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class Decimal128Test {

    @Test
    fun shouldHaveCorrectConstants() {
        // expect
        assertEquals(
            Decimal128.fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000000uL), Decimal128.POSITIVE_ZERO)
        assertEquals(
            Decimal128.fromIEEE754BIDEncoding(0xb040000000000000uL, 0x0000000000000000uL), Decimal128.NEGATIVE_ZERO)
        assertEquals(
            Decimal128.fromIEEE754BIDEncoding(0x7800000000000000uL, 0x0000000000000000uL), Decimal128.POSITIVE_INFINITY)
        assertEquals(
            Decimal128.fromIEEE754BIDEncoding(0xf800000000000000uL, 0x0000000000000000uL), Decimal128.NEGATIVE_INFINITY)
        assertEquals(Decimal128.fromIEEE754BIDEncoding(0x7c00000000000000uL, 0x0000000000000000uL), Decimal128.NaN)
    }

    @Test
    fun shouldConstructFromHighAndLow() {
        // given
        val subject = Decimal128.fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000001uL)

        // then
        assertEquals(0x3040000000000000uL, subject.high)
        assertEquals(0x0000000000000001uL, subject.low)
    }

    @Test
    fun shouldConstructFromSimpleString() {
        // expect
        assertEquals(Decimal128.fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000000uL), Decimal128("0"))
        assertEquals(Decimal128.fromIEEE754BIDEncoding(0xb040000000000000uL, 0x0000000000000000uL), Decimal128("-0"))
        assertEquals(Decimal128.fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000001uL), Decimal128("1"))
        assertEquals(Decimal128.fromIEEE754BIDEncoding(0xb040000000000000uL, 0x0000000000000001uL), Decimal128("-1"))
        assertEquals(
            Decimal128.fromIEEE754BIDEncoding(0x3040000000000000uL, 0x002bdc545d6b4b87uL),
            Decimal128("12345678901234567"))
        assertEquals(
            Decimal128.fromIEEE754BIDEncoding(0x3040000000000000uL, 0x000000e67a93c822uL), Decimal128("989898983458"))
        assertEquals(
            Decimal128.fromIEEE754BIDEncoding(0xb040000000000000uL, 0x002bdc545d6b4b87uL),
            Decimal128("-12345678901234567"))
        assertEquals(
            Decimal128.fromIEEE754BIDEncoding(0x3036000000000000uL, 0x0000000000003039uL), Decimal128("0.12345"))
        assertEquals(
            Decimal128.fromIEEE754BIDEncoding(0x3032000000000000uL, 0x0000000000003039uL), Decimal128("0.0012345"))
        assertEquals(
            Decimal128.fromIEEE754BIDEncoding(0x3040000000000000uL, 0x002bdc545d6b4b87uL),
            Decimal128("00012345678901234567"))
    }

    @Test
    fun shouldRoundExactly() {
        // expect
        assertEquals(
            Decimal128("1.234567890123456789012345678901234"), Decimal128("1.234567890123456789012345678901234"))
        assertEquals(
            Decimal128("1.234567890123456789012345678901234"), Decimal128("1.2345678901234567890123456789012340"))
        assertEquals(
            Decimal128("1.234567890123456789012345678901234"), Decimal128("1.23456789012345678901234567890123400"))
        assertEquals(
            Decimal128("1.234567890123456789012345678901234"), Decimal128("1.234567890123456789012345678901234000"))
    }

    @Test
    fun shouldClampPositiveExponents() {
        // expect
        assertEquals(Decimal128("10E6111"), Decimal128("1E6112"))
        assertEquals(Decimal128("100E6111"), Decimal128("1E6113"))
        assertEquals(Decimal128("100000000000000000000000000000000E+6111"), Decimal128("1E6143"))
        assertEquals(Decimal128("1000000000000000000000000000000000E+6111"), Decimal128("1E6144"))
        assertEquals(Decimal128("1100000000000000000000000000000000E+6111"), Decimal128("11E6143"))
        assertEquals(Decimal128("0E6111"), Decimal128("0E8000"))
        assertEquals(Decimal128("0E6111"), Decimal128("0E2147483647"))
        assertEquals(Decimal128("-10E6111"), Decimal128("-1E6112"))
        assertEquals(Decimal128("-100E6111"), Decimal128("-1E6113"))
        assertEquals(Decimal128("-100000000000000000000000000000000E+6111"), Decimal128("-1E6143"))
        assertEquals(Decimal128("-1000000000000000000000000000000000E+6111"), Decimal128("-1E6144"))
        assertEquals(Decimal128("-1100000000000000000000000000000000E+6111"), Decimal128("-11E6143"))
        assertEquals(Decimal128("-0E6111"), Decimal128("-0E8000"))
        assertEquals(Decimal128("-0E6111"), Decimal128("-0E2147483647"))
    }

    @Test
    fun shouldClampNegativeExponents() {
        // expect
        assertEquals(Decimal128("0E-6176"), Decimal128("0E-8000"))
        assertEquals(Decimal128("0E-6176"), Decimal128("0E-2147483647"))
        assertEquals(Decimal128("1E-6176"), Decimal128("10E-6177"))
        assertEquals(Decimal128("1E-6176"), Decimal128("100E-6178"))
        assertEquals(Decimal128("11E-6176"), Decimal128("110E-6177"))
        assertEquals(Decimal128("-0E-6176"), Decimal128("-0E-8000"))
        assertEquals(Decimal128("-0E-6176"), Decimal128("-0E-2147483647"))
        assertEquals(Decimal128("-1E-6176"), Decimal128("-10E-6177"))
        assertEquals(Decimal128("-1E-6176"), Decimal128("-100E-6178"))
        assertEquals(Decimal128("-11E-6176"), Decimal128("-110E-6177"))
    }

    @Test
    fun shouldDetectInfinity() {
        // expect
        assertTrue(Decimal128.POSITIVE_INFINITY.isInfinite)
        assertTrue(Decimal128.NEGATIVE_INFINITY.isInfinite)
        assertFalse(Decimal128("0").isInfinite)
        assertFalse(Decimal128("9.999999999999999999999999999999999E+6144").isInfinite)
        assertFalse(Decimal128("9.999999999999999999999999999999999E-6143").isInfinite)
        assertFalse(Decimal128.POSITIVE_INFINITY.isFinite)
        assertFalse(Decimal128.NEGATIVE_INFINITY.isFinite)
        assertTrue(Decimal128("0").isFinite)
        assertTrue(Decimal128("9.999999999999999999999999999999999E+6144").isFinite)
        assertTrue(Decimal128("9.999999999999999999999999999999999E-6143").isFinite)
    }

    @Test
    fun shouldDetectDecimal128NaN() {
        // expect
        assertTrue(Decimal128.NaN.isNaN)
        assertTrue(Decimal128.fromIEEE754BIDEncoding(0x7e00000000000000uL, 0x0uL).isNaN) // SDecimal128.NaN
        assertFalse(Decimal128.POSITIVE_INFINITY.isNaN)
        assertFalse(Decimal128.NEGATIVE_INFINITY.isNaN)
        assertFalse(Decimal128("0").isNaN)
        assertFalse(Decimal128("9.999999999999999999999999999999999E+6144").isNaN)
        assertFalse(Decimal128("9.999999999999999999999999999999999E-6143").isNaN)
    }

    @Test
    fun shouldConvertDecimal128NaNToString() {
        // expect
        assertEquals("NaN", Decimal128.NaN.toString())
        assertEquals("-NaN", Decimal128.NEGATIVE_NaN.toString())
    }

    @Test
    fun shouldConvertDecimal128NaNFromString() {
        // expect
        assertEquals(Decimal128.NaN, Decimal128("NaN"))
        assertEquals(Decimal128.NaN, Decimal128("nan"))
        assertEquals(Decimal128.NaN, Decimal128("nAn"))
        assertEquals(Decimal128.NEGATIVE_NaN, Decimal128("-NaN"))
        assertEquals(Decimal128.NEGATIVE_NaN, Decimal128("-nan"))
        assertEquals(Decimal128.NEGATIVE_NaN, Decimal128("-nAn"))
    }

    @Test
    fun shouldConvertInfinityToString() {
        // expect
        assertEquals("Infinity", Decimal128.POSITIVE_INFINITY.toString())
        assertEquals("-Infinity", Decimal128.NEGATIVE_INFINITY.toString())
    }

    @Test
    fun shouldConvertInfinityFromString() {
        // expect
        assertEquals(Decimal128.POSITIVE_INFINITY, Decimal128("Inf"))
        assertEquals(Decimal128.POSITIVE_INFINITY, Decimal128("inf"))
        assertEquals(Decimal128.POSITIVE_INFINITY, Decimal128("inF"))
        assertEquals(Decimal128.POSITIVE_INFINITY, Decimal128("+Inf"))
        assertEquals(Decimal128.POSITIVE_INFINITY, Decimal128("+inf"))
        assertEquals(Decimal128.POSITIVE_INFINITY, Decimal128("+inF"))
        assertEquals(Decimal128.POSITIVE_INFINITY, Decimal128("Infinity"))
        assertEquals(Decimal128.POSITIVE_INFINITY, Decimal128("infinity"))
        assertEquals(Decimal128.POSITIVE_INFINITY, Decimal128("infiniTy"))
        assertEquals(Decimal128.POSITIVE_INFINITY, Decimal128("+Infinity"))
        assertEquals(Decimal128.POSITIVE_INFINITY, Decimal128("+infinity"))
        assertEquals(Decimal128.POSITIVE_INFINITY, Decimal128("+infiniTy"))
        assertEquals(Decimal128.NEGATIVE_INFINITY, Decimal128("-Inf"))
        assertEquals(Decimal128.NEGATIVE_INFINITY, Decimal128("-inf"))
        assertEquals(Decimal128.NEGATIVE_INFINITY, Decimal128("-inF"))
        assertEquals(Decimal128.NEGATIVE_INFINITY, Decimal128("-Infinity"))
        assertEquals(Decimal128.NEGATIVE_INFINITY, Decimal128("-infinity"))
        assertEquals(Decimal128.NEGATIVE_INFINITY, Decimal128("-infiniTy"))
    }

    @Test
    fun shouldConvertFiniteToString() {
        // expect
        assertEquals("0", Decimal128("0").toString())
        assertEquals("-0", Decimal128("-0").toString())
        assertEquals("0E+10", Decimal128("0E10").toString())
        assertEquals("-0E+10", Decimal128("-0E10").toString())
        assertEquals("1", Decimal128("1").toString())
        assertEquals("-1", Decimal128("-1").toString())
        assertEquals("-1.1", Decimal128("-1.1").toString())
        assertEquals("1.23E-7", Decimal128("123E-9").toString())
        assertEquals("0.00000123", Decimal128("123E-8").toString())
        assertEquals("0.0000123", Decimal128("123E-7").toString())
        assertEquals("0.000123", Decimal128("123E-6").toString())
        assertEquals("0.00123", Decimal128("123E-5").toString())
        assertEquals("0.0123", Decimal128("123E-4").toString())
        assertEquals("0.123", Decimal128("123E-3").toString())
        assertEquals("1.23", Decimal128("123E-2").toString())
        assertEquals("12.3", Decimal128("123E-1").toString())
        assertEquals("123", Decimal128("123E0").toString())
        assertEquals("1.23E+3", Decimal128("123E1").toString())
        assertEquals("0.0001234", Decimal128("1234E-7").toString())
        assertEquals("0.001234", Decimal128("1234E-6").toString())
        assertEquals("1E+6", Decimal128("1E6").toString())
    }

    @Test
    @Suppress("EqualsNullCall")
    fun testEquals() {
        // given
        val d1 = Decimal128.fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000001uL)
        val d2 = Decimal128.fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000001uL)
        val d3 = Decimal128.fromIEEE754BIDEncoding(0x3040000000000001uL, 0x0000000000000001uL)
        val d4 = Decimal128.fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000011uL)

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
        assertEquals(
            809500703, Decimal128.fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000001uL).hashCode())
    }

    @Test
    @Suppress("SwallowedException", "LongMethod")
    fun shouldNotRoundInexactly() {
        try {
            Decimal128("12345678901234567890123456789012345E+6111")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("123456789012345678901234567890123456E+6111")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("1234567890123456789012345678901234567E+6111")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("12345678901234567890123456789012345E-6176")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("123456789012345678901234567890123456E-6176")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("1234567890123456789012345678901234567E-6176")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("-12345678901234567890123456789012345E+6111")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("-123456789012345678901234567890123456E+6111")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("-1234567890123456789012345678901234567E+6111")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("-12345678901234567890123456789012345E-6176")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("-123456789012345678901234567890123456E-6176")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("-1234567890123456789012345678901234567E-6176")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
    }

    @Test
    @Suppress("SwallowedException")
    fun shouldNotClampLargeExponentsIfNoExtraPrecisionIsAvailable() {
        try {
            Decimal128("1234567890123456789012345678901234E+6112")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("1234567890123456789012345678901234E+6113")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("1234567890123456789012345678901234E+6114")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("-1234567890123456789012345678901234E+6112")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("-1234567890123456789012345678901234E+6113")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("-1234567890123456789012345678901234E+6114")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
    }

    @Test
    @Suppress("SwallowedException")
    fun shouldNotClampSmallExponentsIfNoExtraPrecisionCanBeDiscarded() {
        try {
            Decimal128("1234567890123456789012345678901234E-6177")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("1234567890123456789012345678901234E-6178")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("1234567890123456789012345678901234E-6179")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("-1234567890123456789012345678901234E-6177")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("-1234567890123456789012345678901234E-6178")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
        try {
            Decimal128("-1234567890123456789012345678901234E-6179")
            fail()
        } catch (e: IllegalArgumentException) {
            // pass
        }
    }
}
