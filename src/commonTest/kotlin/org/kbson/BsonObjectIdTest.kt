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

class BsonObjectIdTest {

    private val bsonValue = BsonObjectId("000000000000000000000000")

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { bsonValue.isObjectId() }
        assertEquals(BsonType.OBJECT_ID, bsonValue.getBsonType())
    }

    @Test
    fun testToBytes() {
        val expectedBytes = byteArrayOf(81, 6, -4, -102, -68, -126, 55, 85, -127, 54, -46, -119)
        assertContentEquals(expectedBytes, BsonObjectId(expectedBytes).toByteArray())
    }

    @Test
    fun testFromBytes() {
        assertFailsWith<IllegalArgumentException>("state should be: bytes has length of 12") {
            BsonObjectId(ByteArray(11))
        }
        assertFailsWith<IllegalArgumentException>("state should be: bytes has length of 12") {
            BsonObjectId(ByteArray(13))
        }

        val bytes = byteArrayOf(81, 6, -4, -102, -68, -126, 55, 85, -127, 54, -46, -119)
        val objectId = BsonObjectId(bytes)
        assertEquals(0x5106FC9A, objectId.timestamp)
    }

    @Test
    fun testHexStringConstructor() {
        val newId = BsonObjectId()
        assertEquals(newId, BsonObjectId(newId.toHexString()))
    }

    @Test
    fun testCompareTo() {
        val first = BsonObjectId(0, 0, 0, 1)
        val second = BsonObjectId(0, 1, 0, 1)
        val third = BsonObjectId(1, 0, 0, 1)
        assertEquals(0, first.compareTo(first))
        assertEquals(-1, first.compareTo(second))
        assertEquals(-1, first.compareTo(third))
        assertEquals(1, second.compareTo(first))
        assertEquals(1, third.compareTo(first))
    }

    @Test
    fun testToHexString() {
        assertEquals("000000000000000000000000", BsonObjectId(ByteArray(12)).toHexString())
        assertEquals(
            "7fffffff007fff7fff007fff",
            BsonObjectId(byteArrayOf(127, -1, -1, -1, 0, 127, -1, 127, -1, 0, 127, -1))
                .toHexString())
    }
}
