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

class BsonInt64Test {

    private val bsonValue = BsonInt64(Long.MAX_VALUE)

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { bsonValue.isInt64() }
        assertTrue { bsonValue.isNumber() }
        assertEquals(BsonType.INT64, bsonValue.getBsonType())
    }

    @Test
    fun shouldHaveAccessToTheUnderlyingValues() {
        assertEquals(Long.MAX_VALUE, bsonValue.value)
    }

    @Test
    fun shouldOverrideEquals() {
        assertEquals(bsonValue, BsonInt64(Long.MAX_VALUE))
        assertNotEquals(bsonValue, BsonInt64(Long.MIN_VALUE))
    }

    @Test
    fun shouldBeComparable() {
        assertEquals(-1, BsonInt64(Long.MIN_VALUE).compareTo(BsonInt64(Long.MAX_VALUE)))
        assertEquals(0, BsonInt64(Long.MIN_VALUE).compareTo(BsonInt64(Long.MIN_VALUE)))
        assertEquals(1, BsonInt64(Long.MAX_VALUE).compareTo(BsonInt64(Long.MIN_VALUE)))
    }
}
