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

import kotlin.math.abs
import org.kbson.internal.Decimal128.Flags.FirstFormExponentBits
import org.kbson.internal.Decimal128.Flags.SecondFormExponentBits
import org.kbson.internal.Decimal128.Flags.isFirstForm
import org.kbson.internal.Decimal128.Flags.isNaN
import org.kbson.internal.Decimal128.Flags.isNegative
import org.kbson.internal.Decimal128.Flags.isNegativeInfinity
import org.kbson.internal.Decimal128.Flags.isPositiveInfinity
import org.kbson.internal.Decimal128.Flags.isSecondForm

/**
 * A binary integer decimal representation of a 128-bit decimal value, supporting 34 decimal digits of significand and
 * an exponent range of -6143 to +6144.
 *
 * @see [BSON Decimal128
 * specification](https://github.com/mongodb/specifications/blob/master/source/bson-decimal128/decimal128.rst)
 * @see [binary integer decimal](https://en.wikipedia.org/wiki/Binary_Integer_Decimal)
 * @see [decimal128 floating-point format](https://en.wikipedia.org/wiki/Decimal128_floating-point_format)
 * @see [754-2008 - IEEE Standard for Floating-Point Arithmetic](http://ieeexplore.ieee.org/document/4610935/)
 */
@Suppress("MagicNumber")
internal class Decimal128
private constructor(
    /**
     * Gets the low-order 64 bits of the IEEE 754-2008 128-bit decimal floating point encoding for this Decimal128,
     * using the BID encoding scheme.
     *
     * @return the low-order 64 bits of this Decimal128
     */
    val high: ULong,

    /**
     * Gets the high-order 64 bits of the IEEE 754-2008 128-bit decimal floating point encoding for this Decimal128,
     * using the BID encoding scheme.
     *
     * @return the high-order 64 bits of this Decimal128
     */
    val low: ULong
) {

    /**
     * Returns true if this Decimal128 is negative.
     *
     * @return true if this Decimal128 is negative
     */
    val isNegative: Boolean
        get() = high and SIGN_BIT_MASK == SIGN_BIT_MASK

    /**
     * Returns true if this Decimal128 is infinite.
     *
     * @return true if this Decimal128 is infinite
     */
    val isInfinite: Boolean
        get() = high and INFINITY_MASK == INFINITY_MASK

    /**
     * Returns true if this Decimal128 is finite.
     *
     * @return true if this Decimal128 is finite
     */
    val isFinite: Boolean
        get() = !isInfinite

    /**
     * Returns true if this Decimal128 is Not-A-Number (NaN).
     *
     * @return true if this Decimal128 is Not-A-Number
     */
    val isNaN: Boolean
        get() = high and NaN_MASK == NaN_MASK

    /**
     * Returns the String representation of the Decimal128 value.
     *
     * @return the String representation
     * @see [To-String
     * Specification](https://github.com/mongodb/specifications/blob/master/source/bson-decimal128/decimal128.rst#to-string-representation)
     */
    @Suppress("MaxLineLength")
    override fun toString(): String {
        val high = mapIEEEHighBitsToDecimal128HighBits(high)
        val low = low
        return when {
            isFirstForm(high) -> firstFormToString(high, low)
            isSecondForm(high) -> secondFormToString(high)
            isNegativeInfinity(high) -> "-Infinity"
            isPositiveInfinity(high) -> "Infinity"
            isNaN(high) -> "NaN"
            else -> error("Unsupported Decimal128 string conversion. This is a bug.")
        }
    }

    private fun firstFormToString(high: ULong, low: ULong): String {
        val exponent = getExponent(high)
        val significand = getSignificand(high, low)
        val coefficientString = significand.toString()
        val adjustedExponent = exponent + coefficientString.length - 1
        val result =
            when {
                exponent > 0 || adjustedExponent < -6 -> {
                    toStringWithExponentialNotation(coefficientString, adjustedExponent)
                }
                else -> {
                    toStringWithoutExponentialNotation(coefficientString, exponent)
                }
            }
        return when {
            isNegative(high) -> {
                "-$result"
            }
            else -> {
                result
            }
        }
    }

    private fun secondFormToString(high: ULong): String {
        return when (val exponent = getExponent(high)) {
            // invalid representation treated as zero
            0 -> {
                when {
                    isNegative(high) -> "-0"
                    else -> "0"
                }
            }
            else -> {
                var exponentString = exponent.toString()
                if (exponent > 0) {
                    exponentString = "+$exponentString"
                }
                (if (isNegative(high)) "-0E" else "0E") + exponentString
            }
        }
    }

    /**
     * Returns true if the encoded representation of this instance is the same as the encoded representation of `o`.
     *
     * One consequence is that, whereas `Double.NaN != Double.NaN`, `new Decimal128("NaN").equals(new Decimal128("NaN")`
     * returns true.
     *
     * Another consequence is that, as with BigDecimal, `new Decimal128("1.0").equals(new Decimal128("1.00")` returns
     * false, because the precision is not the same and therefore the representation is not the same.
     *
     * @param other the object to compare for equality
     * @return true if the instances are equal
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Decimal128

        if (high != other.high) return false
        if (low != other.low) return false

        return true
    }

    override fun hashCode(): Int {
        var result = (low xor (low shr 32)).toInt()
        result = 31 * result + (high xor (high shr 32)).toInt()
        return result
    }

    internal object Flags {
        val SignBit = (1uL shl 63)
        const val FirstFormExponentBits = 0x7FFE000000000000uL
        const val FirstFormSignificandBits = 0x0001FFFFFFFFFFFFuL
        const val SecondFormExponentBits = 0x1FFF800000000000uL
        private const val FirstFormLeadingBits = 0x6000000000000000uL
        private const val FirstFormLeadingBitsMax = 0x4000000000000000uL
        private const val SecondFormLeadingBits = 0x7800000000000000uL
        private const val SecondFormLeadingBitsMin = 0x6000000000000000uL
        private const val SecondFormLeadingBitsMax = 0x7000000000000000uL
        private const val SignedInfinityBits = 0xFC00000000000000uL
        private const val PositiveInfinity = 0x7800000000000000uL
        private const val NegativeInfinity = 0xF800000000000000uL
        private const val PartialNaNBits = 0x7C00000000000000uL
        private const val PartialNaN = 0x7C00000000000000uL

        fun isFirstForm(highBits: ULong): Boolean {
            return highBits.and(FirstFormLeadingBits) <= FirstFormLeadingBitsMax
        }

        fun isSecondForm(highBits: ULong): Boolean {
            val secondFormLeadingBits = highBits.and(SecondFormLeadingBits)
            return (secondFormLeadingBits >= SecondFormLeadingBitsMin) and
                (secondFormLeadingBits <= SecondFormLeadingBitsMax)
        }

        fun isNegative(highBits: ULong): Boolean {
            return highBits.and(SignBit) != 0uL
        }

        fun isNegativeInfinity(highBits: ULong): Boolean {
            return highBits.and(SignedInfinityBits) == NegativeInfinity
        }

        fun isPositiveInfinity(highBits: ULong): Boolean {
            return highBits.and(SignedInfinityBits) == PositiveInfinity
        }

        fun isNaN(highBits: ULong): Boolean {
            return highBits and PartialNaNBits == PartialNaN
        }
    }

    @Suppress("TooManyFunctions")
    companion object {
        private const val EXPONENT_MAX: Int = 6111
        private const val EXPONENT_MIN: Int = -6176
        private val MAX_SIGNIFICAND = UInt128.parse("9999999999999999999999999999999999")
        private const val INFINITY_MASK = 0x7800000000000000uL
        private const val NaN_MASK = 0x7c00000000000000uL
        private val SIGN_BIT_MASK: ULong = 1uL.shl(63)

        /**
         * A constant holding the positive infinity of type `Decimal128`. It is equal to the value return by
         * `Decimal128("Infinity")`.
         */
        val POSITIVE_INFINITY: Decimal128 = fromIEEE754BIDEncoding(INFINITY_MASK, 0uL)

        /**
         * A constant holding the negative infinity of type `Decimal128`. It is equal to the value return by
         * `Decimal128("-Infinity")`.
         */
        val NEGATIVE_INFINITY: Decimal128 = fromIEEE754BIDEncoding(INFINITY_MASK or SIGN_BIT_MASK, 0uL)

        /**
         * A constant holding a negative Not-a-Number (-NaN) value of type `Decimal128`. It is equal to the value return
         * by `Decimal128("-NaN")`.
         */
        val NEGATIVE_NaN: Decimal128 = fromIEEE754BIDEncoding(NaN_MASK or SIGN_BIT_MASK, 0uL)

        /**
         * A constant holding a Not-a-Number (NaN) value of type `Decimal128`. It is equal to the value return by
         * `Decimal128("NaN")`.
         */
        val NaN: Decimal128 = fromIEEE754BIDEncoding(NaN_MASK, 0uL)

        /**
         * A constant holding a positive zero value of type `Decimal128`. It is equal to the value return by
         * `Decimal128("0")`.
         */
        val POSITIVE_ZERO: Decimal128 = fromIEEE754BIDEncoding(0x3040000000000000uL, 0x0000000000000000uL)

        /**
         * A constant holding a negative zero value of type `Decimal128`. It is equal to the value return by
         * `Decimal128("-0")`.
         */
        val NEGATIVE_ZERO: Decimal128 = fromIEEE754BIDEncoding(0xb040000000000000uL, 0x0000000000000000uL)

        /**
         * Create an instance with the given high and low order bits representing this Decimal128 as an IEEE 754-2008
         * 128-bit decimal floating point using the BID encoding scheme.
         *
         * @param high the high-order 64 bits
         * @param low the low-order 64 bits
         * @return the Decimal128 value representing the given high and low order bits
         */
        fun fromIEEE754BIDEncoding(high: ULong, low: ULong): Decimal128 = Decimal128(high, low)

        /**
         * Returns a Decimal128 value representing the given String.
         *
         * @param value the Decimal128 value represented as a String
         * @return the Decimal128 value representing the given String
         * @throws NumberFormatException if the value is out of the Decimal128 range
         * @see [ From-String
         * Specification](https://github.com/mongodb/specifications/blob/master/source/bson-decimal128/decimal128.rst.from-string-representation)
         */
        @Suppress("MaxLineLength", "ComplexMethod", "ThrowsCount")
        operator fun invoke(value: String): Decimal128 {
            if (value.isEmpty()) {
                throw NumberFormatException()
            }
            val regularExpression =
                """^(?<sign>[+-])?(?<significand>\d+([.]\d*)?|[.]\d+)(?<exponent>[eE](?<exponentSign>[+-])?(?<exponentDigits>\d+))?$"""
            return when (val matchResult = Regex(regularExpression).matchEntire(value)) {
                null -> {
                    parseSpecialValues(value)
                }
                else -> {
                    val groups = matchResult.groups as MatchNamedGroupCollection

                    val signGroup = groups["sign"]?.value
                    val isNegative = signGroup != null && signGroup == "-"

                    var exponent = 0

                    val exponentGroup = groups["exponent"]?.value
                    if (exponentGroup != null && exponentGroup.isNotEmpty()) {
                        exponent = groups["exponentDigits"]!!.value.toInt()
                        val exponentSignString = groups["exponentSign"]?.value
                        if (exponentSignString != null && exponentSignString == "-") {
                            exponent = -exponent
                        }
                    }

                    var significandString: String = groups["significand"]!!.value
                    var decimalPointIndex: Int
                    if (significandString.indexOf('.').also { decimalPointIndex = it } != -1) {
                        exponent -= significandString.length - (decimalPointIndex + 1)
                        significandString =
                            significandString.substring(0, decimalPointIndex) +
                                significandString.substring(decimalPointIndex + 1)
                    }

                    significandString = removeLeadingZeroes(significandString)
                    val clampOrRoundResult = clampOrRound(exponent, significandString)
                    exponent = clampOrRoundResult.exponent
                    significandString = clampOrRoundResult.significandString

                    if (exponent > EXPONENT_MAX || exponent < EXPONENT_MIN) {
                        throw NumberFormatException("Can't parse to Decimal128:$value")
                    }
                    if (significandString.length > 34) {
                        throw NumberFormatException("Can't parse to Decimal128:$value")
                    }

                    val significand = UInt128.parse(significandString)
                    fromComponents(isNegative, exponent, significand)
                }
            }
        }

        private fun parseSpecialValues(value: String): Decimal128 {
            return when {
                isInfinityString(value) -> POSITIVE_INFINITY
                isNegativeInfinityString(value) -> NEGATIVE_INFINITY
                value.equals("NaN", ignoreCase = true) -> NaN
                value.equals("-NaN", ignoreCase = true) -> NEGATIVE_NaN
                else -> throw NumberFormatException("Can't parse to Decimal128:$value")
            }
        }

        private fun isNegativeInfinityString(value: String) =
            value.equals("-Inf", ignoreCase = true) || value.equals("-Infinity", ignoreCase = true)

        private fun isInfinityString(value: String) =
            value.equals("Inf", ignoreCase = true) ||
                value.equals("Infinity", ignoreCase = true) ||
                value.equals("+Inf", ignoreCase = true) ||
                value.equals("+Infinity", ignoreCase = true)

        private data class ClampOrRoundResult(val exponent: Int, val significandString: String)

        @Suppress("NestedBlockDepth")
        private fun clampOrRound(exponent: Int, significandString: String): ClampOrRoundResult {
            var newExponent = exponent
            var newSignificandString = significandString
            when {
                exponent > EXPONENT_MAX -> {
                    if (significandString == "0") {
                        // since significand is zero simply use the largest possible exponent
                        newExponent = EXPONENT_MAX
                    } else {
                        // use clamping to bring the exponent into range
                        val numberOfTrailingZeroesToAdd = exponent - EXPONENT_MAX
                        val digitsAvailable = 34 - significandString.length
                        if (numberOfTrailingZeroesToAdd <= digitsAvailable) {
                            newExponent = EXPONENT_MAX
                            newSignificandString = significandString + "0".repeat(numberOfTrailingZeroesToAdd)
                        }
                    }
                }
                exponent < EXPONENT_MIN -> {
                    if (significandString == "0") {
                        // since significand is zero simply use the smallest possible exponent
                        newExponent = EXPONENT_MIN
                    } else {
                        // use exact rounding to bring the exponent into range
                        val numberOfTrailingZeroesToRemove = EXPONENT_MIN - exponent
                        if (numberOfTrailingZeroesToRemove < significandString.length) {
                            val trailingDigits =
                                significandString.substring(significandString.length - numberOfTrailingZeroesToRemove)
                            if (trailingDigits.matches(Regex("^0+$"))) {
                                newExponent = EXPONENT_MIN
                                newSignificandString =
                                    significandString.substring(
                                        0, significandString.length - numberOfTrailingZeroesToRemove)
                            }
                        }
                    }
                }
                significandString.length > 34 -> {
                    // use exact rounding to reduce significand to 34 digits
                    val numberOfTrailingZeroesToRemove = significandString.length - 34
                    if (exponent + numberOfTrailingZeroesToRemove <= EXPONENT_MAX) {
                        val trailingDigits =
                            significandString.substring(significandString.length - numberOfTrailingZeroesToRemove)
                        if (trailingDigits.matches(Regex("^0+$"))) {
                            newExponent += numberOfTrailingZeroesToRemove
                            newSignificandString =
                                significandString.substring(
                                    0, significandString.length - numberOfTrailingZeroesToRemove)
                        }
                    }
                }
            }
            return ClampOrRoundResult(newExponent, newSignificandString)
        }

        private fun fromComponents(isNegative: Boolean, exponent: Int, significand: UInt128): Decimal128 {
            require(!(exponent < EXPONENT_MIN || exponent > EXPONENT_MAX))
            require(significand <= MAX_SIGNIFICAND)
            val biasedExponent = mapExponentToDecimal128BiasedExponent(exponent)
            var high = biasedExponent.toULong().shl(49).or(significand.high)
            if (isNegative) {
                high = Flags.SignBit.or(high)
            }
            return fromIEEE754BIDEncoding(mapDecimal128HighBitsToIEEEHighBits(high), significand.low)
        }

        private fun mapExponentToDecimal128BiasedExponent(exponent: Int): Int {
            // internally we use a different bias than IEEE so that a Decimal128 struct filled with
            // zero bytes is a
            // true Decimal128 zero
            // Decimal128Bias is defined as:
            // exponents from     0 to 6111: biasedExponent = exponent
            // exponents from -6176 to   -1: biasedExponent = exponent + 12288
            return when {
                exponent >= 0 -> exponent
                else -> exponent + 12288
            }
        }

        private fun removeLeadingZeroes(startingSignificandString: String): String {
            var significandString = startingSignificandString
            return when {
                significandString[0] == '0' && significandString.length > 1 -> {
                    significandString = significandString.replaceFirst("^0+".toRegex(), "")
                    significandString.ifEmpty { "0" }
                }
                else -> significandString
            }
        }

        private fun mapIEEEHighBitsToDecimal128HighBits(highBits: ULong): ULong {
            // for IEEEBias from    0 to  6175: Decimal128Bias = IEEEBias + 6112
            // for IEEEBias from 6176 to 12287: Decimal128Bias = IEEEBias - 6176
            return when {
                isFirstForm(highBits) -> {
                    val exponentBits = highBits and FirstFormExponentBits
                    if (exponentBits <= 6175uL.shl(49)) {
                        highBits + 6112uL.shl(49)
                    } else {
                        highBits - 6176uL.shl(49)
                    }
                }
                isSecondForm(highBits) -> {
                    val exponentBits = highBits and SecondFormExponentBits
                    if (exponentBits <= 6175uL.shl(47)) {
                        highBits + 6112uL.shl(47)
                    } else {
                        highBits - 6176uL.shl(47)
                    }
                }
                else -> highBits
            }
        }

        private fun mapDecimal128HighBitsToIEEEHighBits(high: ULong): ULong {
            // for Decimal128Bias from    0 to  6111: IEEEBias = Decimal128Bias + 6176
            // for Decimal128Bias from 6112 to 12287: IEEEBias = Decimal128Bias - 6112
            return when {
                isFirstForm(high) -> {
                    if (high and FirstFormExponentBits <= 6111uL shl 49) {
                        high + (6176uL shl 49)
                    } else {
                        high - (6112uL shl 49)
                    }
                }
                isSecondForm(high) -> {
                    if (high and SecondFormExponentBits <= 6111uL shl 47) {
                        high + (6176uL shl 47)
                    } else {
                        high - (6112uL shl 47)
                    }
                }
                else -> high
            }
        }

        private fun toStringWithoutExponentialNotation(startingCoefficientString: String, exponent: Int): String {
            var coefficientString = startingCoefficientString
            return when (exponent) {
                0 -> coefficientString
                else -> {
                    val exponentAbsoluteValue = abs(exponent)
                    val minimumCoefficientStringLength = exponentAbsoluteValue + 1
                    if (coefficientString.length < minimumCoefficientStringLength) {
                        coefficientString =
                            "0".repeat(minimumCoefficientStringLength - coefficientString.length) + coefficientString
                    }
                    val decimalPointIndex = coefficientString.length - exponentAbsoluteValue
                    coefficientString.substring(0, decimalPointIndex) +
                        "." +
                        coefficientString.substring(decimalPointIndex)
                }
            }
        }

        private fun toStringWithExponentialNotation(startingCoefficientString: String, adjustedExponent: Int): String {
            var coefficientString = startingCoefficientString
            if (coefficientString.length > 1) {
                coefficientString = coefficientString[0] + "." + coefficientString.substring(1)
            }
            var exponentString = adjustedExponent.toString()
            if (adjustedExponent >= 0) {
                exponentString = "+$exponentString"
            }
            return coefficientString + "E" + exponentString
        }

        private fun getSignificand(high: ULong, low: ULong): UInt128 =
            UInt128(getSignificandHighBits(high), getSignificandLowBits(high, low))

        private fun getSignificandHighBits(high: ULong): ULong =
            when {
                isFirstForm(high) -> high and Flags.FirstFormSignificandBits
                isSecondForm(high) -> 0uL
                else -> error("getSignificandHighBits cannot be called for Infinity or NaN.")
            }

        private fun getSignificandLowBits(high: ULong, low: ULong): ULong =
            when {
                isFirstForm(high) -> low
                isSecondForm(high) -> 0uL
                else -> error("getSignificandLowBits cannot be called for Infinity or NaN.")
            }

        private fun getExponent(high: ULong): Int =
            when {
                isFirstForm(high) -> {
                    mapDecimal128BiasedExponentToExponent((high and FirstFormExponentBits).shr(49).toInt())
                }
                isSecondForm(high) -> {
                    mapDecimal128BiasedExponentToExponent((high and SecondFormExponentBits).shr(47).toInt())
                }
                else -> {
                    error("getExponent cannot be called for Infinity or NaN.")
                }
            }

        private fun mapDecimal128BiasedExponentToExponent(biasedExponent: Int): Int =
            when {
                biasedExponent <= 6111 -> biasedExponent
                else -> biasedExponent - 12288
            }
    }
}
