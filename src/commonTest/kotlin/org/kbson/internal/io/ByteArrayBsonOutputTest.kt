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
package org.kbson.internal.io

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.kbson.BsonObjectId
import org.kbson.BsonSerializationException

class ByteArrayBsonOutputTest {

    @Test
    fun shouldStartWithSetPositionAndSize() {
        val bsonOutput = ByteArrayBsonOutput()
        assertEquals(0, bsonOutput.position)
        assertEquals(0, bsonOutput.size)
    }

    @Test
    fun shouldwriteAByte() {
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeByte(11)

        assertEquals(1, bsonOutput.position)
        assertEquals(1, bsonOutput.size)
        assertContentEquals(byteArrayOf(11), bsonOutput.toByteArray())
    }

    @Test
    fun shouldWriteIntoAByteArray() {
        val byteArray = byteArrayOf(1, 2, 3, 4)
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeBytes(byteArray)
        assertContentEquals(byteArray, bsonOutput.toByteArray())
        assertEquals(4, bsonOutput.position)
        assertEquals(4, bsonOutput.size)
    }

    @Test
    fun shouldWriteALittleEndianInt32() {
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeInt32(0x1020304)

        assertContentEquals(byteArrayOf(4, 3, 2, 1), bsonOutput.toByteArray())
        assertEquals(4, bsonOutput.position)
        assertEquals(4, bsonOutput.size)
    }

    @Test
    fun shouldWriteALittleEndianInt64() {
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeInt64(0x102030405060708L)
        assertContentEquals(byteArrayOf(8, 7, 6, 5, 4, 3, 2, 1), bsonOutput.toByteArray())
        assertEquals(8, bsonOutput.position)
        assertEquals(8, bsonOutput.size)
    }

    @Test
    fun shouldWriteADouble() {
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeDouble(3.14159)
        assertContentEquals(byteArrayOf(110, -122, 27, -16, -7, 33, 9, 64), bsonOutput.toByteArray())
        assertEquals(8, bsonOutput.position)
        assertEquals(8, bsonOutput.size)
    }

    @Test
    fun shouldWriteABsonObjectId() {
        val byteArray = byteArrayOf(12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeObjectId(BsonObjectId(byteArray))

        assertContentEquals(byteArray, bsonOutput.toByteArray())
        assertEquals(12, bsonOutput.position)
        assertEquals(12, bsonOutput.size)
    }

    @Test
    fun shouldWriteAnEmptyString() {
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeString("")

        assertContentEquals(byteArrayOf(1, 0, 0, 0, 0), bsonOutput.toByteArray())
        assertEquals(5, bsonOutput.position)
        assertEquals(5, bsonOutput.size)
    }

    @Test
    fun shouldWriteAnAsciiString() {
        val string = "Kotlin"
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeString(string)

        assertContentEquals(byteArrayOf(7, 0, 0, 0, 75, 111, 116, 108, 105, 110, 0), bsonOutput.toByteArray())
        assertEquals(11, bsonOutput.position)
        assertEquals(11, bsonOutput.size)
    }

    @Test
    fun shouldWriteAUTF8String() {
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeString("\u0900")

        assertContentEquals(
            byteArrayOf(4, 0, 0, 0, 0xe0.toByte(), 0xa4.toByte(), 0x80.toByte(), 0), bsonOutput.toByteArray())
        assertEquals(8, bsonOutput.position)
        assertEquals(8, bsonOutput.size)
    }

    @Test
    fun shouldWriteWhenNullCharacterCString() {
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeString("hell\u0000world")

        assertContentEquals(
            byteArrayOf(11, 0, 0, 0, 104, 101, 108, 108, 0, 119, 111, 114, 108, 100, 0), bsonOutput.toByteArray())
        assertEquals(15, bsonOutput.position)
        assertEquals(15, bsonOutput.size)
    }

    @Test
    fun shouldWriteAnEmptyCString() {
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeCString("")

        assertContentEquals(byteArrayOf(0), bsonOutput.toByteArray())
        assertEquals(1, bsonOutput.position)
        assertEquals(1, bsonOutput.size)
    }

    @Test
    fun shouldWriteAnAsciiCString() {
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeCString("Kotlin")

        assertContentEquals(byteArrayOf(75, 111, 116, 108, 105, 110, 0), bsonOutput.toByteArray())
        assertEquals(7, bsonOutput.position)
        assertEquals(7, bsonOutput.size)
    }

    @Test
    fun shouldWriteAUTF8CString() {
        val bsonOutput = ByteArrayBsonOutput()
        bsonOutput.writeCString("\u0900")

        assertContentEquals(byteArrayOf(0xe0.toByte(), 0xa4.toByte(), 0x80.toByte(), 0), bsonOutput.toByteArray())
        assertEquals(4, bsonOutput.position)
        assertEquals(4, bsonOutput.size)
    }

    @Test
    fun shouldThrowWhenNullCharacterInCString() {
        val bsonOutput = ByteArrayBsonOutput()
        assertFailsWith<BsonSerializationException>() { bsonOutput.writeCString("hell\u0000world") }
    }

    @Test
    fun shouldGrow() {
        val bsonOutput = ByteArrayBsonOutput(4)

        bsonOutput.writeBytes(byteArrayOf(1, 2, 3, 4))
        bsonOutput.writeBytes(byteArrayOf(5, 6, 7, 8, 9, 10))

        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), bsonOutput.toByteArray())
        assertEquals(10, bsonOutput.position)
        assertEquals(10, bsonOutput.size)
    }
}
