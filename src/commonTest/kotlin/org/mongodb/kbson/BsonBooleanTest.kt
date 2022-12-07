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
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BsonBooleanTest {

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { BsonBoolean.TRUE_VALUE.isBoolean() }
        assertEquals(BsonType.BOOLEAN, BsonBoolean.TRUE_VALUE.bsonType)
    }

    @Test
    fun shouldHaveAccessToTheUnderlyingValues() {
        assertTrue(BsonBoolean.valueOf(true).value)
        assertFalse(BsonBoolean.valueOf(false).value)
    }

    @Test
    fun shouldOverrideEquals() {
        assertEquals(BsonBoolean.TRUE_VALUE, BsonBoolean(true))
        assertEquals(BsonBoolean.FALSE_VALUE, BsonBoolean(false))
        assertNotEquals(BsonBoolean.TRUE_VALUE, BsonBoolean(false))
        assertNotEquals(BsonBoolean.FALSE_VALUE, BsonBoolean(true))
    }

    @Test
    fun shouldBeComparable() {
        assertEquals(-1, BsonBoolean.FALSE_VALUE.compareTo(BsonBoolean.TRUE_VALUE))
        assertEquals(0, BsonBoolean.TRUE_VALUE.compareTo(BsonBoolean.TRUE_VALUE))
        assertEquals(0, BsonBoolean.FALSE_VALUE.compareTo(BsonBoolean.FALSE_VALUE))
        assertEquals(1, BsonBoolean.TRUE_VALUE.compareTo(BsonBoolean.FALSE_VALUE))
    }
}
