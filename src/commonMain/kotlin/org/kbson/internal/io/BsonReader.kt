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

import org.kbson.BsonBinary
import org.kbson.BsonBoolean
import org.kbson.BsonDBPointer
import org.kbson.BsonDateTime
import org.kbson.BsonDecimal128
import org.kbson.BsonDouble
import org.kbson.BsonInt32
import org.kbson.BsonInt64
import org.kbson.BsonJavaScript
import org.kbson.BsonMaxKey
import org.kbson.BsonMinKey
import org.kbson.BsonNull
import org.kbson.BsonObjectId
import org.kbson.BsonRegularExpression
import org.kbson.BsonString
import org.kbson.BsonSymbol
import org.kbson.BsonTimestamp
import org.kbson.BsonType
import org.kbson.BsonUndefined
import org.kbson.internal.Closeable

/** An interface for reading a logical BSON document using a pull-oriented API. */
@Suppress("TooManyFunctions")
public interface BsonReader : Closeable {
    /** @return The current BsonType. */
    public var currentBsonType: BsonType?

    /** @return the most recently read name. */
    public var currentName: String?

    /**
     * Reads the name of an element from the reader.
     *
     * @return The name of the element.
     */
    public fun readName(): String

    /**
     * Reads a BSON type from the reader.
     *
     * @return A BSON type.
     */
    public fun readBsonType(): BsonType

    /** Reads the start of a BSON document. */
    public fun readStartDocument()

    /** Reads the end of a BSON document from the reader. */
    public fun readEndDocument()

    /** Reads the start of a BSON array. */
    public fun readStartArray()

    /** Reads the end of a BSON array from the reader. */
    public fun readEndArray()

    /**
     * Reads BSON Binary data from the reader.
     *
     * @return A Binary.
     */
    public fun readBinary(): BsonBinary

    /**
     * Reads a BSON Boolean from the reader.
     *
     * @return A Boolean.
     */
    public fun readBoolean(): BsonBoolean

    /**
     * Reads a BSON DateTime from the reader.
     *
     * @return The number of milliseconds since the Unix epoch.
     */
    public fun readDateTime(): BsonDateTime

    /**
     * Reads a BSON Double from the reader.
     *
     * @return A Double.
     */
    public fun readDouble(): BsonDouble

    /**
     * Reads a BSON Int32 from the reader.
     *
     * @return An Int32.
     */
    public fun readInt32(): BsonInt32

    /**
     * Reads a BSON Int64 from the reader.
     *
     * @return An Int64.
     */
    public fun readInt64(): BsonInt64

    /**
     * Reads a BSON Decimal128 from the reader.
     *
     * @return A Decimal128
     */
    public fun readDecimal128(): BsonDecimal128

    /**
     * Reads a BSON JavaScript from the reader.
     *
     * @return A string.
     */
    public fun readJavaScript(): BsonJavaScript

    /**
     * Reads a BSON JavaScript with scope from the reader (call readStartDocument next to read the scope).
     *
     * @return A string.
     */
    public fun readJavaScriptWithScope(): String

    /** Reads a BSON MaxKey from the reader. */
    public fun readMaxKey(): BsonMaxKey

    /** Reads a BSON MinKey from the reader. */
    public fun readMinKey(): BsonMinKey

    /** Reads a BSON null from the reader. */
    public fun readNull(): BsonNull

    /**
     * Reads a BSON ObjectId from the reader.
     *
     * @return the `ObjectId` value
     */
    public fun readObjectId(): BsonObjectId

    /**
     * Reads a BSON regular expression from the reader.
     *
     * @return A regular expression.
     */
    public fun readRegularExpression(): BsonRegularExpression

    /**
     * Reads a BSON DBPointer from the reader.
     *
     * @return A DBPointer.
     */
    public fun readDBPointer(): BsonDBPointer

    /**
     * Reads a BSON String from the reader.
     *
     * @return A String.
     */
    public fun readString(): BsonString

    /**
     * Reads a BSON symbol from the reader.
     *
     * @return A string.
     */
    public fun readSymbol(): BsonSymbol

    /**
     * Reads a BSON timestamp from the reader.
     *
     * @return The combined timestamp/increment.
     */
    public fun readTimestamp(): BsonTimestamp

    /** Reads a BSON undefined from the reader. */
    public fun readUndefined(): BsonUndefined

    /** Skips the name (reader must be positioned on a name). */
    public fun skipName()

    /** Skips the value (reader must be positioned on a value). */
    public fun skipValue()
}
