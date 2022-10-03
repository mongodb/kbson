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
package org.mongodb.kbson.internal.io

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.BsonSerializationException

class ByteArrayBsonInputTest {

    @Test
    fun shouldStartAtSetPosition() {
        val byteArray = ByteArray(4)

        val bsonInput = ByteArrayBsonInput(byteArray)
        assertEquals(0, bsonInput.position)
    }

    @Test
    fun shouldHaveTheExpectedAvailableBytes() {
        val byteArray = ByteArray(4)

        var bsonInput = ByteArrayBsonInput(byteArray)
        assertEquals(4, bsonInput.availableBytes)

        bsonInput = ByteArrayBsonInput(byteArray, 3)
        assertEquals(3, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadAByte() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(11))

        assertEquals(11, bsonInput.readByte())
        assertEquals(1, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadIntoAByteArray() {
        val byteArray = byteArrayOf(1, 2, 3, 4)
        val bsonInput = ByteArrayBsonInput(byteArray)

        val intoByteArray = ByteArray(4)
        assertContentEquals(intoByteArray, bsonInput.readBytes(intoByteArray))
        assertContentEquals(byteArray, intoByteArray)
        assertEquals(4, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadIntoAByteArrayAtOffsetUntilLength() {
        val byteArray = byteArrayOf(1, 2, 3, 4)
        val bsonInput = ByteArrayBsonInput(byteArray)

        val bytesRead = ByteArray(6)
        assertContentEquals(bytesRead, bsonInput.readBytes(bytesRead, 2, 2))
        assertContentEquals(byteArrayOf(0, 0, 1, 2, 0, 0), bytesRead)
        assertEquals(2, bsonInput.position)
        assertEquals(2, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadALittleEndianInt32() {
        val byteArray = byteArrayOf(4, 3, 2, 1)
        val bsonInput = ByteArrayBsonInput(byteArray)

        assertEquals(0x1020304, bsonInput.readInt32())
        assertEquals(4, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadALittleEndianInt64() {
        val byteArray = byteArrayOf(8, 7, 6, 5, 4, 3, 2, 1)
        val bsonInput = ByteArrayBsonInput(byteArray)

        assertEquals(0x102030405060708L, bsonInput.readInt64())
        assertEquals(8, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadADouble() {
        val byteArray = byteArrayOf(110, -122, 27, -16, -7, 33, 9, 64)
        val bsonInput = ByteArrayBsonInput(byteArray)

        assertEquals(3.14159, bsonInput.readDouble())
        assertEquals(8, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadABsonObjectId() {
        val byteArray = byteArrayOf(12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
        val bsonInput = ByteArrayBsonInput(byteArray)

        assertEquals(BsonObjectId(byteArray), bsonInput.readObjectId())
        assertEquals(12, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadAnEmptyString() {
        val byteArray = byteArrayOf(1, 0, 0, 0, 0)
        val bsonInput = ByteArrayBsonInput(byteArray)

        assertEquals("", bsonInput.readString())
        assertEquals(5, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadAOneByteString() {
        val singleByteStrings = listOf(0x0, 0x1, 0x20, 0x7e, 0x7f)

        singleByteStrings.forEach {
            val byteArray = byteArrayOf(2, 0, 0, 0, it.toByte(), 0)
            val bsonInput = ByteArrayBsonInput(byteArray)

            assertEquals(Char(it).toString(), bsonInput.readString())
            assertEquals(6, bsonInput.position)
            assertEquals(0, bsonInput.availableBytes)
        }
    }

    @Test
    fun shouldReadAnInvalidOneByteString() {
        val byteArray = byteArrayOf(2, 0, 0, 0, -0x1, 0)
        val bsonInput = ByteArrayBsonInput(byteArray)

        assertEquals("\uFFFD", bsonInput.readString())
        assertEquals(6, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadAnAsciiString() {
        val string = "Kotlin"
        val bsonInput = ByteArrayBsonInput(byteArrayOf(7, 0, 0, 0, 75, 111, 116, 108, 105, 110, 0))

        assertEquals(string, bsonInput.readString())
        assertEquals(11, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadAUTF8String() {
        val byteArray = byteArrayOf(4, 0, 0, 0, 0xe0.toByte(), 0xa4.toByte(), 0x80.toByte(), 0)
        val bsonInput = ByteArrayBsonInput(byteArray)

        assertEquals("\u0900", bsonInput.readString())
        assertEquals(8, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadAnEmptyCString() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(0))

        assertEquals("", bsonInput.readCString())
        assertEquals(1, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadAOneByteCString() {
        val byteArray = byteArrayOf(0x1.toByte(), 0)
        val bsonInput = ByteArrayBsonInput(byteArray)

        assertEquals(Char(0x1).toString(), bsonInput.readCString())
        assertEquals(2, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadAnInvalidOneByteCString() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(-0x1, 0))

        assertEquals("\uFFFD", bsonInput.readCString())
        assertEquals(2, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadAnAsciiCString() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(75, 111, 116, 108, 105, 110, 0))

        assertEquals("Kotlin", bsonInput.readCString())
        assertEquals(7, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadAUTF8CString() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(0xe0.toByte(), 0xa4.toByte(), 0x80.toByte(), 0))

        assertEquals("\u0900", bsonInput.readCString())
        assertEquals(4, bsonInput.position)
        assertEquals(0, bsonInput.availableBytes)
    }

    @Test
    fun shouldReadFromPosition() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(4, 3, 2, 1))

        assertEquals(4, bsonInput.readByte())
        assertEquals(3, bsonInput.readByte())
        assertEquals(2, bsonInput.readByte())
        assertEquals(1, bsonInput.readByte())
    }

    @Test
    fun shouldSkip() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(4, 3, 2, 1))

        bsonInput.skip(3)
        assertEquals(3, bsonInput.position)
        assertEquals(1, bsonInput.availableBytes)
    }

    @Test
    fun shouldThrowWhenNoDataAvailable() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf())
        assertFailsWith<BsonSerializationException>() { bsonInput.readByte() }
    }

    @Test
    fun shouldThrowIfInt32IsInvalid() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(0, 0, 0))
        assertFailsWith<BsonSerializationException>() { bsonInput.readInt32() }
    }

    @Test
    fun shouldThrowIfInt64IsInvalid() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(0, 0, 0, 0, 0, 0, 0))
        assertFailsWith<BsonSerializationException>() { bsonInput.readInt64() }
    }

    @Test
    fun shouldThrowIfDoubleIsInvalid() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(0, 0, 0, 0, 0, 0, 0))
        assertFailsWith<BsonSerializationException>() { bsonInput.readDouble() }
    }

    @Test
    fun shouldThrowIfObjectIdIsInvalid() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
        assertFailsWith<BsonSerializationException>() { bsonInput.readObjectId() }
    }

    @Test
    fun shouldThrowReadBytesIfNotEnoughBytesAreAvailable() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(0, 0, 0, 0, 0, 0, 0))
        assertFailsWith<BsonSerializationException>() { bsonInput.readBytes(ByteArray(8)) }
    }

    @Test
    fun shouldThrowReadBytesPartiallyIfNotEnoughBytesAreAvailable() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(0, 0, 0, 0))
        assertFailsWith<BsonSerializationException>() { bsonInput.readBytes(ByteArray(8), 2, 5) }
    }

    @Test
    fun shouldThrowIfStringLengthIsInvalid() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(-1, -1, -1, -1, 41, 42, 43, 0))
        assertFailsWith<BsonSerializationException>() { bsonInput.readString() }
    }

    @Test
    fun shouldThrowIfStringIsNotNullTerminated() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(4, 0, 0, 0, 41, 42, 43, 99))
        assertFailsWith<BsonSerializationException>() { bsonInput.readString() }
    }

    @Test
    fun shouldThrowIfStringWithOneByteIsNotNullTerminated() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(2, 0, 0, 0, 1, 3))
        assertFailsWith<BsonSerializationException>() { bsonInput.readString() }
    }

    @Test
    fun shouldThrowIfCStringIsNotNullTerminated() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(0xe0.toByte(), 0xa4.toByte(), 0x80.toByte()))
        assertFailsWith<BsonSerializationException>() { bsonInput.readCString() }
    }

    @Test
    fun shouldThrowIfCStringWithOneByteIsNotNullTerminated() {
        val bsonInput = ByteArrayBsonInput(byteArrayOf(1.toByte()))
        assertFailsWith<BsonSerializationException>() { bsonInput.readCString() }
    }
}
