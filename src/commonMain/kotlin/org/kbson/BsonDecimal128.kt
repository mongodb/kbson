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

/**
 * A representation of the BSON Decimal128 type.
 *
 * @constructor Create an instance with the given high and low order bits representing this BsonDecimal128 as an IEEE 754-2008 128-bit
 * decimal floating point using the BID encoding scheme.
 * @property high the high-order 64 bits
 * @property low the low-order 64 bits
 */
public class BsonDecimal128(public val high: Long, public val low: Long) : BsonValue() {

    override val bsonType: BsonType
        get() = BsonType.DECIMAL128

    /**
     * Returns true if this Decimal128 is negative.
     *
     * @return true if this Decimal128 is negative
     */
    public fun isNegative(): Boolean {
        return (high and SIGN_BIT_MASK) == SIGN_BIT_MASK
    }

    /**
     * Returns true if this Decimal128 is infinite.
     *
     * @return true if this Decimal128 is infinite
     */
    public fun isInfinite(): Boolean {
        return (high and INFINITY_MASK) == INFINITY_MASK
    }

    /**
     * Returns true if this Decimal128 is finite.
     *
     * @return true if this Decimal128 is finite
     */
    public fun isFinite(): Boolean {
        return !isInfinite()
    }

    /**
     * Returns true if this Decimal128 is Not-A-Number (NaN).
     *
     * @return true if this Decimal128 is Not-A-Number
     */
    public fun isNaN(): Boolean {
        return (high and NaN_MASK) == NaN_MASK
    }

    override fun toString(): String {
        return "BsonDecimal128(high=$high, low=$low)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonDecimal128

        if (high != other.high) return false
        if (low != other.low) return false

        return true
    }

    override fun hashCode(): Int {
        var result = high.hashCode()
        result = 31 * result + low.hashCode()
        return result
    }

    private companion object {
        private const val INFINITY_MASK = 0x7800000000000000L
        private const val NaN_MASK = 0x7c00000000000000L
        private const val SIGN_BIT_MASK = 1L shl 63
    }
}
