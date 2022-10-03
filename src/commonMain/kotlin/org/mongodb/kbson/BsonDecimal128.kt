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
package org.mongodb.kbson

import org.mongodb.kbson.internal.Decimal128

/**
 * A binary integer decimal representation of a 128-bit decimal value, supporting 34 decimal digits of significand and
 * an exponent range of -6143 to +6144.
 *
 * @see [BSON Decimal128
 * specification](https://github.com/mongodb/specifications/blob/master/source/bson-decimal128/decimal128.rst)
 *
 * @see [binary integer decimal](https://en.wikipedia.org/wiki/Binary_Integer_Decimal)
 *
 * @see [decimal128 floating-point format](https://en.wikipedia.org/wiki/Decimal128_floating-point_format)
 *
 * @see [754-2008 - IEEE Standard for Floating-Point Arithmetic](http://ieeexplore.ieee.org/document/4610935/)
 *
 * @property value the Decimal128 value
 */
public class BsonDecimal128 private constructor(internal val value: Decimal128) : BsonValue() {
    override val bsonType: BsonType
        get() = BsonType.DECIMAL128

    /**
     * Returns true if this BsonDecimal128 is negative.
     *
     * @return true if this BsonDecimal128 is negative
     */
    public val isNegative: Boolean
        get() = value.isNegative

    /**
     * Returns true if this BsonDecimal128 is infinite.
     *
     * @return true if this BsonDecimal128 is infinite
     */
    public val isInfinite: Boolean
        get() = value.isInfinite

    /**
     * Returns true if this BsonDecimal128 is finite.
     *
     * @return true if this BsonDecimal128 is finite
     */
    public val isFinite: Boolean
        get() = value.isFinite

    /**
     * Returns true if this BsonDecimal128 is Not-A-Number (NaN).
     *
     * @return true if this BsonDecimal128 is Not-A-Number
     */
    public val isNaN: Boolean
        get() = value.isNaN

    override fun toString(): String {
        return "BsonDecimal128(value=${value})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonDecimal128

        return this.value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    public companion object {

        /**
         * A constant holding the positive infinity of type `BsonDecimal128`. It is equal to the value return by
         * `BsonDecimal128("Infinity")`.
         */
        public val POSITIVE_INFINITY: BsonDecimal128 = BsonDecimal128(Decimal128.POSITIVE_INFINITY)

        /**
         * A constant holding the negative infinity of type `BsonDecimal128`. It is equal to the value return by
         * `BsonDecimal128("-Infinity")`.
         */
        public val NEGATIVE_INFINITY: BsonDecimal128 = BsonDecimal128(Decimal128.NEGATIVE_INFINITY)

        /**
         * A constant holding a negative Not-a-Number (-NaN) value of type `BsonDecimal128`. It is equal to the value
         * return by `BsonDecimal128("-NaN")`.
         */
        public val NEGATIVE_NaN: BsonDecimal128 = BsonDecimal128(Decimal128.NEGATIVE_NaN)

        /**
         * A constant holding a Not-a-Number (NaN) value of type `BsonDecimal128`. It is equal to the value return by
         * `BsonDecimal128("NaN")`.
         */
        public val NaN: BsonDecimal128 = BsonDecimal128(Decimal128.NaN)

        /**
         * A constant holding a positive zero value of type `BsonDecimal128`. It is equal to the value return by
         * `BsonDecimal128("0")`.
         */
        public val POSITIVE_ZERO: BsonDecimal128 = BsonDecimal128(Decimal128.POSITIVE_ZERO)

        /**
         * A constant holding a negative zero value of type `BsonDecimal128`. It is equal to the value return by
         * `BsonDecimal128("-0")`.
         */
        public val NEGATIVE_ZERO: BsonDecimal128 = BsonDecimal128(Decimal128.NEGATIVE_ZERO)

        /**
         * Create an instance with the given high and low order bits representing this BsonDecimal128 as an IEEE
         * 754-2008 128-bit decimal floating point using the BID encoding scheme.
         *
         * @param high the high-order 64 bits
         * @param low the low-order 64 bits
         * @return the BsonDecimal128 value representing the given high and low order bits
         */
        public fun fromIEEE754BIDEncoding(high: ULong, low: ULong): BsonDecimal128 =
            BsonDecimal128(Decimal128.fromIEEE754BIDEncoding(high, low))

        /**
         * Returns a BsonDecimal128 value representing the given String.
         *
         * @param value the BsonDecimal128 value represented as a String
         * @return the BsonDecimal128 value representing the given String
         * @throws NumberFormatException if the value is out of the BsonDecimal128 range
         * @see [From-String
         * Specification](https://github.com/mongodb/specifications/blob/master/source/bson-decimal128/decimal128.rst.from-string-representation)
         */
        @Suppress("MaxLineLength")
        public operator fun invoke(value: String): BsonDecimal128 {
            return BsonDecimal128(Decimal128(value))
        }
    }
}
