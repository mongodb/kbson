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

class BsonDecimal128Test {

    private val bsonValue = BsonDecimal128.fromIEEE754BIDEncoding(1uL, 2uL)

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { bsonValue.isDecimal128() }
        assertEquals(BsonType.DECIMAL128, bsonValue.bsonType)
    }

    @Test
    fun shouldHaveDecimalSpecificMethods() {
        assertFalse(BsonDecimal128("1").isNegative)
        assertTrue(BsonDecimal128("-1.0000000000001").isNegative)
        assertTrue(BsonDecimal128("-Infinity").isNegative)
        assertTrue(BsonDecimal128("-0").isNegative)
        assertTrue(BsonDecimal128("12").isFinite)
        assertTrue(BsonDecimal128.POSITIVE_INFINITY.isInfinite)
        assertTrue(BsonDecimal128.NEGATIVE_INFINITY.isInfinite)
        assertFalse(BsonDecimal128.NaN.isFinite)
        assertTrue(BsonDecimal128.NaN.isNaN)
    }

    @Test
    fun shouldHaveCompanionHelpersForSpecialTypes() {
        assertEquals(BsonDecimal128.NaN, BsonDecimal128("NaN"))
        assertEquals(BsonDecimal128.NEGATIVE_NaN, BsonDecimal128("-NaN"))
        assertEquals(BsonDecimal128.POSITIVE_INFINITY, BsonDecimal128("Infinity"))
        assertEquals(BsonDecimal128.NEGATIVE_INFINITY, BsonDecimal128("-Infinity"))
        assertEquals(BsonDecimal128.POSITIVE_ZERO, BsonDecimal128("0"))
        assertEquals(BsonDecimal128.NEGATIVE_ZERO, BsonDecimal128("-0"))
    }

    @Test
    fun shouldOverrideEquals() {
        assertEquals(bsonValue, BsonDecimal128("1.8446744073709551618E-6157"))
        assertNotEquals(bsonValue, BsonDecimal128("2.2"))
    }
}
