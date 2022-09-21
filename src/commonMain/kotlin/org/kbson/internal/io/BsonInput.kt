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

/** A Bson Input stream */
@Suppress("TooManyFunctions")
internal interface BsonInput {

    /** The current position in the stream */
    public val position: Int

    /**
     * Reads a single byte from the stream
     *
     * @return the byte value
     */
    fun readByte(): Byte

    /**
     * Reads the specified number of bytes into the given byte array. This is equivalent to `readBytes(bytes, 0,
     * bytes.length)`.
     *
     * @param bytes the byte array to write into
     * @return the bytes byte array that has been written to
     */
    fun readBytes(bytes: ByteArray): ByteArray

    /**
     * Reads the specified number of bytes into the given byte array starting at the specified offset.
     *
     * @param bytes the byte array to write into
     * @param offset the offset to start writing
     * @param length the number of bytes to write
     * @return the bytes byte array that has been written to
     */
    fun readBytes(bytes: ByteArray, offset: Int, length: Int): ByteArray

    /**
     * Reads a BSON Int64 value from the stream.
     *
     * @return the Int64 value
     */
    fun readInt64(): Long

    /**
     * Reads a BSON Double value from the stream.
     *
     * @return the double value
     */
    fun readDouble(): Double

    /**
     * Reads a BSON Int32 value from the stream.
     *
     * @return the Int32 value
     */
    fun readInt32(): Int

    /**
     * Reads a BSON String value from the stream.
     *
     * @return the string
     */
    fun readString(): String

    /**
     * Reads a BSON ObjectId value from the stream.
     *
     * @return the ObjectId
     */
    fun readObjectId(): BsonObjectId

    /**
     * Reads a BSON CString value from the stream.
     *
     * @return the CString
     */
    fun readCString(): String

    /**
     * Skips the specified number of bytes in the stream.
     *
     * @param numBytes the number of bytes to skip
     */
    fun skip(numBytes: Int)

    /** Skips a BSON CString value from the stream. */
    fun skipCString()
}
