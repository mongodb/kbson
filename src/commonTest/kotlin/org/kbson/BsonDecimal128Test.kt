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

    private val bsonValue = BsonDecimal128(1, 2)

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { bsonValue.isDecimal128() }
        assertEquals(BsonType.DECIMAL128, bsonValue.bsonType)
    }

    @Test
    fun shouldHaveAccessToTheUnderlyingValues() {
        assertEquals(1, bsonValue.high)
        assertEquals(2, bsonValue.low)
    }

    @Test
    fun shouldHaveDecimalSpecificMethods() {
        assertFalse(BsonDecimal128(1, 1).isNegative())
        assertTrue(BsonDecimal128(-1, 0x0000000000000000L).isNegative())
        assertTrue(BsonDecimal128(-0x4fc0000000000000L, 0x0000000000000000L).isNegative())
        assertTrue(BsonDecimal128(-0x800000000000000L, 0x0000000000000000L).isNegative())
        assertTrue(BsonDecimal128(1, 0x0000000000000000L).isFinite())
        assertTrue(BsonDecimal128(0x7800000000000000L, 0x0000000000000000L).isInfinite())
        assertTrue(BsonDecimal128(-0x800000000000000L, 0x0000000000000000L).isInfinite())
        assertTrue(BsonDecimal128(0x7c00000000000000L, 0x0000000000000000L).isNaN())
    }

    @Test
    fun shouldOverrideEquals() {
        assertEquals(bsonValue, BsonDecimal128(1L, 2L))
        assertNotEquals(bsonValue, BsonDecimal128(2, 2))
    }
}
