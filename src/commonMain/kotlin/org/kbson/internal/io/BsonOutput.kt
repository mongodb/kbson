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
import org.kbson.internal.Closeable

/** An output stream that is optimized for writing BSON values directly to the underlying stream. */
internal interface BsonOutput : Closeable {
    /**
     * Gets the current position in the stream.
     *
     * @return the current position
     */
    public var position: Int

    /**
     * Gets the current size of the stream in number of bytes.
     *
     * @return the size of the stream
     */
    public val size: Int

    /**
     * Writes all the bytes in the byte array to the stream.
     * @param bytes the non-null byte array
     */
    fun writeBytes(bytes: ByteArray)

    /**
     * Writes `length` bytes from the byte array, starting at `offset`.
     * @param bytes the non-null byte array
     * @param offset the offset to start writing from
     * @param length the number of bytes to write
     */
    fun writeBytes(bytes: ByteArray, offset: Int, length: Int)

    /**
     * Write a single byte to the stream. The byte to be written is the eight low-order bits of the specified value. The
     * 24 high-order bits of the value are ignored.
     *
     * @param value the value
     */
    fun writeByte(value: Byte)

    /**
     * Writes a BSON CString to the stream.
     *
     * @param value the value
     */
    fun writeCString(value: String)

    /**
     * Writes a BSON String to the stream.
     *
     * @param value the value
     */
    fun writeString(value: String)

    /**
     * Writes a BSON double to the stream.
     *
     * @param value the value
     */
    fun writeDouble(value: Double)

    /**
     * Writes a 32-bit BSON integer to the stream.
     *
     * @param value the value
     */
    fun writeInt32(value: Int)

    /**
     * Writes a 32-bit BSON integer to the stream at the given position. This is useful for patching in the size of a
     * document once the last byte of it has been encoded and its size it known.
     *
     * @param position the position to write the value, which must be greater than or equal to 0 and less than or equal
     * to the current size
     * @param value the value
     */
    fun writeInt32(position: Int, value: Int)

    /**
     * Writes a 64-bit BSON integer to the stream.
     *
     * @param value the value
     */
    fun writeInt64(value: Long)

    /**
     * Writes a BSON ObjectId to the stream.
     *
     * @param value the value
     */
    fun writeObjectId(value: BsonObjectId)
}
