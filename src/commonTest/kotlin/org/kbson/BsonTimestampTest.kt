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

import kotlin.test.*

class BsonTimestampTest {

    private val bsonValue = BsonTimestamp()

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { bsonValue.isTimestamp() }
        assertEquals(BsonType.TIMESTAMP, bsonValue.getBsonType())
    }

    @Test
    fun shouldHaveAccessToTheUnderlyingValues() {
        val bsonValue = BsonTimestamp(120, 55)
        assertEquals(120, bsonValue.getTime())
        assertEquals(55, bsonValue.getInc())
        assertEquals(515396075575, bsonValue.value)
    }

    @Test
    fun shouldOverrideEquals() {
        assertEquals(bsonValue, BsonTimestamp())
        assertNotEquals(bsonValue, BsonTimestamp(1))
    }

    @Test
    fun shouldBeComparable() {
        assertEquals(-1, BsonTimestamp(Long.MIN_VALUE).compareTo(BsonTimestamp(Long.MAX_VALUE)))
        assertEquals(0, BsonTimestamp(Long.MIN_VALUE).compareTo(BsonTimestamp(Long.MIN_VALUE)))
        assertEquals(1, BsonTimestamp(Long.MAX_VALUE).compareTo(BsonTimestamp(Long.MIN_VALUE)))
    }
}