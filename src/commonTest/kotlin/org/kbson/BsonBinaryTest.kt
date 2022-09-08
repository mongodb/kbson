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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BsonBinaryTest {

    private val data = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16).map { it.toByte() }.toByteArray()

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { BsonBinary(data).isBinary() }
        assertEquals(BsonType.BINARY, BsonBinary(data).bsonType)
    }

    @Test
    fun shouldHaveAccessToTheUnderlyingValues() {
        val bsonValue = BsonBinary(data)

        assertEquals(data, bsonValue.data)
        assertEquals(BsonBinarySubType.BINARY.value, bsonValue.type)
    }

    @Test
    fun shouldInitializeWithDataAndSubType() {
        var bsonValue = BsonBinary(BsonBinarySubType.BINARY, data)

        assertEquals(BsonBinarySubType.BINARY.value, bsonValue.type)
        assertEquals(data, bsonValue.data)

        bsonValue = BsonBinary(BsonBinarySubType.FUNCTION, data)
        assertEquals(BsonBinarySubType.FUNCTION.value, bsonValue.type)
        assertEquals(data, bsonValue.data)

        bsonValue = BsonBinary(BsonBinarySubType.MD5, data)
        assertEquals(BsonBinarySubType.MD5.value, bsonValue.type)
        assertEquals(data, bsonValue.data)

        bsonValue = BsonBinary(BsonBinarySubType.OLD_BINARY, data)
        assertEquals(BsonBinarySubType.OLD_BINARY.value, bsonValue.type)
        assertEquals(data, bsonValue.data)

        bsonValue = BsonBinary(BsonBinarySubType.USER_DEFINED, data)
        assertEquals(BsonBinarySubType.USER_DEFINED.value, bsonValue.type)
        assertEquals(data, bsonValue.data)

        bsonValue = BsonBinary(BsonBinarySubType.UUID_LEGACY, data)
        assertEquals(BsonBinarySubType.UUID_LEGACY.value, bsonValue.type)
        assertEquals(data, bsonValue.data)

        bsonValue = BsonBinary(BsonBinarySubType.UUID_STANDARD, data)
        assertEquals(BsonBinarySubType.UUID_STANDARD.value, bsonValue.type)
        assertEquals(data, bsonValue.data)

        bsonValue = BsonBinary(BsonBinarySubType.ENCRYPTED, data)
        assertEquals(BsonBinarySubType.ENCRYPTED.value, bsonValue.type)
        assertEquals(data, bsonValue.data)

        bsonValue = BsonBinary(BsonBinarySubType.COLUMN, data)
        assertEquals(BsonBinarySubType.COLUMN.value, bsonValue.type)
        assertEquals(data, bsonValue.data)
    }

    @Test
    fun shouldOverrideEquals() {
        assertEquals(BsonBinary(BsonBinarySubType.BINARY, data), BsonBinary(BsonBinarySubType.BINARY, data))
        assertNotEquals(BsonBinary(BsonBinarySubType.BINARY, data), BsonBinary(BsonBinarySubType.USER_DEFINED, data))
    }

    @Test
    fun implementsClone() {
        val original = BsonBinary(BsonBinarySubType.BINARY, data)
        val clone = original.clone()
        assertEquals(original, clone)

        original.data[0] = 9
        assertNotEquals(original, clone)
    }
}
