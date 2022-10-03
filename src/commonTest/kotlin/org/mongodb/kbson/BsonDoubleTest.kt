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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BsonDoubleTest {

    private val bsonValue = BsonDouble(Double.MAX_VALUE)

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { bsonValue.isDouble() }
        assertTrue { bsonValue.isNumber() }
        assertEquals(BsonType.DOUBLE, bsonValue.bsonType)
    }

    @Test
    fun shouldHaveAccessToTheUnderlyingValues() {
        assertEquals(Double.MAX_VALUE, bsonValue.value)
    }

    @Test
    fun shouldOverrideEquals() {
        assertEquals(bsonValue, BsonDouble(Double.MAX_VALUE))
        assertNotEquals(bsonValue, BsonDouble(Double.POSITIVE_INFINITY))
    }

    @Test
    fun shouldBeComparable() {
        assertEquals(-1, BsonDouble(Double.MIN_VALUE).compareTo(BsonDouble(Double.MAX_VALUE)))
        assertEquals(0, BsonDouble(Double.MIN_VALUE).compareTo(BsonDouble(Double.MIN_VALUE)))
        assertEquals(1, BsonDouble(Double.MAX_VALUE).compareTo(BsonDouble(Double.MIN_VALUE)))

        assertEquals(-1, BsonDouble(Double.NEGATIVE_INFINITY).compareTo(BsonDouble(Double.POSITIVE_INFINITY)))
        assertEquals(1, BsonDouble(Double.POSITIVE_INFINITY).compareTo(BsonDouble(Double.NEGATIVE_INFINITY)))
    }
}
