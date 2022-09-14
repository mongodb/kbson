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

import org.kbson.BsonObjectId
import org.kbson.BsonSerializationException
import org.kbson.internal.validateSerialization

@Suppress("MagicNumber", "TooManyFunctions")
public class ByteArrayBsonInput(private val bytes: ByteArray, private val endIndex: Int = bytes.size) : BsonInput {
    override var position: Int = 0
    public val availableBytes: Int
        get() = endIndex - position

    override fun readByte(): Byte {
        validateSerialization(availableBytes >= 1) {
            "Unexpected EOF, only $availableBytes bytes available. Requested 1."
        }
        return bytes[position++]
    }

    override fun readBytes(bytes: ByteArray): ByteArray {
        return readBytes(bytes, 0, bytes.size)
    }

    override fun readBytes(bytes: ByteArray, offset: Int, length: Int): ByteArray {
        require(offset >= 0 && offset <= bytes.size) { "Invalid offset: $offset. Only ${bytes.size}" }
        require(length >= 0 && length <= (bytes.size - offset)) {
            "Invalid length: $length. Only $availableBytes bytes available."
        }
        validateSerialization(availableBytes >= length) {
            "Unexpected EOF, only $availableBytes bytes available. Requested $length."
        }

        this.bytes.copyInto(
            destination = bytes, destinationOffset = offset, startIndex = position, endIndex = position + length)
        position += length
        return bytes
    }

    override fun readInt64(): Long {
        val longBytes = slice(Long.SIZE_BYTES)

        var longValue: Long = 0
        for (i in 0..56 step 8) {
            longValue = longValue or (0xFFL and longBytes.readByte().toLong() shl i)
        }
        return longValue
    }

    override fun readDouble(): Double {
        return Double.fromBits(readInt64())
    }

    override fun readInt32(): Int {
        val intBytes = slice(Int.SIZE_BYTES)
        var intValue = 0
        for (i in 0..24 step 8) {
            intValue = intValue or (0xFF and intBytes.readByte().toInt() shl i)
        }
        return intValue
    }

    override fun readString(): String {
        val size = readInt32()
        return readString(size)
    }

    override fun readObjectId(): BsonObjectId {
        return BsonObjectId(readBytes(ByteArray(BsonObjectId.OBJECT_ID_LENGTH)))
    }

    override fun readCString(): String {
        return readString(calculateStringSize())
    }

    override fun skip(numBytes: Int) {
        validateSerialization(availableBytes >= numBytes) {
            "Unexpected EOF, only $availableBytes bytes available. Requested $numBytes."
        }
        position += numBytes
    }

    override fun skipCString() {
        skip(calculateStringSize())
    }

    private fun calculateStringSize(): Int {
        var localPosition = position
        var size = 1
        while (localPosition < endIndex) {
            if (bytes[localPosition++].toInt() == 0) {
                return size
            }
            size++
        }
        throw BsonSerializationException("Found a BSON string that is not null-terminated")
    }

    private fun readString(size: Int): String {
        validateSerialization(size > 0) {
            "While decoding a BSON string found a size that is not a positive number: $size."
        }
        validateSerialization(availableBytes >= size) {
            "Unexpected EOF, only $availableBytes bytes available. Requested $size."
        }

        if (size == 2) {
            val asciiByte: Byte = readByte() // if only one byte in the string, it must be ascii.
            val nullByte: Byte = readByte() // read null terminator
            validateSerialization(nullByte.toInt() == 0) { "Found a BSON string that is not null-terminated" }

            return if (asciiByte < 0) REPLACEMENT_CHARACTER else ONE_BYTE_ASCII_STRINGS[asciiByte.toInt()].toString()
        } else {
            val result = bytes.decodeToString(position, position + size - 1)
            position += size - 1
            val nullByte: Byte = readByte()
            validateSerialization(nullByte.toInt() == 0) { "Found a BSON string that is not null-terminated" }
            return result
        }
    }

    public fun slice(size: Int): ByteArrayBsonInput {
        validateSerialization(availableBytes >= size) {
            "Unexpected EOF, only $availableBytes bytes available. Requested $size."
        }
        val result = ByteArrayBsonInput(bytes, position + size)
        result.position = position
        position += size
        return result
    }

    private companion object {
        private const val REPLACEMENT_CHARACTER = "\uFFFD"
        private val ONE_BYTE_ASCII_STRINGS = arrayOfNulls<String>(Byte.MAX_VALUE + 1)

        init {
            for (b in ONE_BYTE_ASCII_STRINGS.indices) {
                ONE_BYTE_ASCII_STRINGS[b] = b.toChar().toString()
            }
        }
    }
}
