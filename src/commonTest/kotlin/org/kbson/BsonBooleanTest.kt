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

class BsonBooleanTest {

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { BsonBoolean.TRUE.isBoolean() }
        assertEquals(BsonType.BOOLEAN, BsonBoolean.TRUE.getBsonType())
    }

    @Test
    fun shouldHaveAccessToTheUnderlyingValues() {
        assertTrue(BsonBoolean.valueOf(true).value)
        assertFalse(BsonBoolean.valueOf(false).value)
    }

    @Test
    fun shouldOverrideEquals() {
        assertEquals(BsonBoolean.TRUE, BsonBoolean(true))
        assertEquals(BsonBoolean.FALSE, BsonBoolean(false))
        assertNotEquals(BsonBoolean.TRUE, BsonBoolean(false))
        assertNotEquals(BsonBoolean.FALSE, BsonBoolean(true))
    }

    @Test
    fun shouldBeComparable() {
        assertEquals(-1, BsonBoolean.FALSE.compareTo(BsonBoolean.TRUE))
        assertEquals(0, BsonBoolean.TRUE.compareTo(BsonBoolean.TRUE))
        assertEquals(0, BsonBoolean.FALSE.compareTo(BsonBoolean.FALSE))
        assertEquals(1, BsonBoolean.TRUE.compareTo(BsonBoolean.FALSE))
    }
}
