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
import org.kbson.internal.validateSerialization

@Suppress("MagicNumber", "EmptyFunctionBlock")
internal class ByteArrayBsonOutput(initialSize: Int = DEFAULT_BYTE_ARRAY_SIZE) : BsonOutput {

    override var position: Int = 0
    override val size: Int
        get() = position
    private var array: ByteArray = ByteArray(initialSize)

    fun toByteArray(): ByteArray {
        val byteArray = ByteArray(position)
        array.copyInto(destination = byteArray, destinationOffset = 0, startIndex = 0, endIndex = position)
        return byteArray
    }

    override fun writeBytes(bytes: ByteArray) {
        writeBytes(bytes, 0, bytes.size)
    }

    override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
        ensureCapacity(length)
        bytes.copyInto(destination = array, destinationOffset = position, startIndex = offset, endIndex = length)
        position += length
    }

    override fun writeByte(value: Byte) {
        ensureCapacity(1)
        array[position++] = value
    }

    override fun writeCString(value: String) {
        val byteArray = value.encodeToByteArray()
        validateSerialization(!byteArray.contains(NULL_BYTE)) {
            "BSON cstring '$value' is not valid because it contains a null character at index " +
                "${byteArray.indexOf(NULL_BYTE)}"
        }
        writeBytes(byteArray)
        writeByte(0)
    }

    override fun writeString(value: String) {
        val byteArray = value.encodeToByteArray() + 0
        writeInt32(byteArray.size)
        writeBytes(byteArray)
    }

    override fun writeDouble(value: Double) {
        writeInt64(value.toRawBits())
    }

    override fun writeInt32(value: Int) {
        for (i in 0..24 step 8) {
            writeByte((value shr i).toByte())
        }
    }

    override fun writeInt32(position: Int, value: Int) {
        val localPosition = this.position
        this.position = position
        writeInt32(value)
        this.position = localPosition
    }

    override fun writeInt64(value: Long) {
        for (i in 0..56 step 8) {
            writeByte((0xFFL and (value shr i)).toByte())
        }
    }

    override fun writeObjectId(value: BsonObjectId) {
        writeBytes(value.toByteArray())
    }

    override fun close() {}

    private fun ensureCapacity(elementsToAppend: Int) {
        if (position + elementsToAppend <= array.size) {
            return
        }
        val newArray = ByteArray((position + elementsToAppend).takeHighestOneBit() shl 1)
        array.copyInto(newArray)
        array = newArray
    }

    private companion object {
        private const val DEFAULT_BYTE_ARRAY_SIZE: Int = 1024
        private const val NULL_BYTE: Byte = 0x0
    }
}
