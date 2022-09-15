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

@Suppress("MagicNumber")
internal class UInt128(val high: ULong, val low: ULong) : Comparable<UInt128> {

    override fun compareTo(other: UInt128): Int {
        var result = high.compareTo(other.high)
        if (result == 0) {
            result = low.compareTo(other.low)
        }
        return result
    }

    override fun toString(): String {
        var builder: StringBuilder? = null // don't create the builder until we actually need it
        var value = this
        while (true) {
            // convert 9 decimal digits at a time to a string
            val (quotient, remainder) = divide(value, 1_000_000_000u)
            val fragmentString = remainder.toString()
            value = quotient
            if (value == ZERO) {
                return if (builder == null) {
                    fragmentString // values with 9 decimal digits or less don't need the builder
                } else {
                    builder.insert(0, fragmentString)
                    builder.toString()
                }
            }
            if (builder == null) {
                builder = StringBuilder(38)
            }
            builder.insert(0, fragmentString)
            builder.insert(0, "0".repeat(9 - fragmentString.length))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UInt128

        if (high != other.high) return false
        if (low != other.low) return false

        return true
    }

    override fun hashCode(): Int {
        var result = high.hashCode()
        result = 31 * result + low.hashCode()
        return result
    }

    companion object {
        private val ZERO = UInt128(0uL, 0uL)

        @Suppress("ReturnCount")
        fun parse(startString: String): UInt128 {
            require(startString.isNotEmpty())
            var s = startString

            // remove leading zeroes (and return true if value is zero)
            if (s[0] == '0') {
                if (s.length == 1) {
                    return ZERO
                } else {
                    s = s.replaceFirst(Regex("^0+"), "")
                    if (s.isEmpty()) {
                        return ZERO
                    }
                }
            }

            // parse 9 or fewer decimal digits at a time
            var value = ZERO
            while (s.isNotEmpty()) {
                var fragmentSize = s.length % 9
                if (fragmentSize == 0) {
                    fragmentSize = 9
                }
                val fragmentString = s.substring(0, fragmentSize)
                val fragmentValue = fragmentString.toUInt()
                var combinedValue = multiply(value, 1_000_000_000u)
                combinedValue = add(combinedValue, UInt128(0uL, fragmentValue.toULong()))
                // overflow means s represents a value larger than UInt128.MaxValue
                require(combinedValue >= value)
                value = combinedValue
                s = s.substring(fragmentSize)
            }
            return value
        }

        private fun add(x: UInt128, y: UInt128): UInt128 {
            var high = x.high + y.high
            val low = x.low + y.low
            if (low < x.low) {
                high += 1uL
            }
            return UInt128(high, low)
        }

        private fun multiply(x: UInt128, y: UInt): UInt128 {
            var a = x.high shr 32
            var b = x.high and 0xffffffffuL
            var c = x.low shr 32
            var d = x.low and 0xffffffffuL

            d *= y
            c = c * y + (d shr 32)
            b = b * y + (c shr 32)
            a = a * y + (b shr 32)

            val low = (c shl 32) + (d and 0xffffffffuL)
            val high = (a shl 32) + (b and 0xffffffffuL)

            return UInt128(high, low)
        }

        private data class DivisionResult(val quotient: UInt128, val remainder: UInt)

        private fun divide(x: UInt128, divisor: UInt): DivisionResult {
            if (x.high == 0uL && x.low == 0uL) {
                return DivisionResult(ZERO, 0u)
            }

            val remainder: UInt

            var a = x.high shr 32
            var b = x.high and 0xffffffffuL
            var c = x.low shr 32
            var d = x.low and 0xffffffffuL

            var temp = a
            a = (temp / divisor) and 0xffffffffuL
            temp = ((temp % divisor) shl 32) + b
            b = (temp / divisor) and 0xffffffffuL
            temp = ((temp % divisor) shl 32) + c
            c = temp / divisor and 0xffffffffuL
            temp = ((temp % divisor) shl 32) + d
            d = (temp / divisor) and 0xffffffffuL

            val high = (a shl 32) + b
            val low = (c shl 32) + d
            remainder = (temp % divisor).toUInt()

            return DivisionResult(UInt128(high, low), remainder)
        }
    }
}
