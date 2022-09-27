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
import org.kbson.BsonDBPointer
import org.kbson.BsonDecimal128
import org.kbson.BsonJavaScript
import org.kbson.BsonObjectId
import org.kbson.BsonRegularExpression
import org.kbson.BsonTimestamp
import org.kbson.internal.Closeable

/** An interface for writing a logical BSON document using a push-oriented API. */
@Suppress("TooManyFunctions")
internal interface BsonWriter : Closeable {

    /**
     * Writes the name of an element to the writer.
     *
     * @param name The name of the element.
     */
    fun writeName(name: String)

    /**
     * Writes the start of a BSON document to the writer.
     *
     * @throws org.kbson.BsonSerializationException if maximum serialization depth exceeded.
     */
    fun writeStartDocument()

    /** Writes the end of a BSON document to the writer. */
    fun writeEndDocument()

    /**
     * Writes the start of a BSON array to the writer.
     *
     * @throws org.kbson.BsonSerializationException if maximum serialization depth exceeded.
     */
    fun writeStartArray()

    /** Writes the end of a BSON array to the writer. */
    fun writeEndArray()

    /**
     * Writes a BSON Binary data element to the writer.
     *
     * @param binary The Binary data.
     */
    fun writeBinaryData(value: BsonBinary)

    /**
     * Writes a BSON Boolean to the writer.
     *
     * @param value The Boolean value.
     */
    fun writeBoolean(value: Boolean)

    /**
     * Writes a BSON DateTime to the writer.
     *
     * @param value The number of milliseconds since the Unix epoch.
     */
    fun writeDateTime(value: Long)

    /**
     * Writes a BSON DBPointer to the writer.
     *
     * @param value The DBPointer to write
     */
    fun writeDBPointer(value: BsonDBPointer)

    /**
     * Writes a BSON Double to the writer.
     *
     * @param value The Double value.
     */
    fun writeDouble(value: Double)

    /**
     * Writes a BSON Int32 to the writer.
     *
     * @param value The Int32 value.
     */
    fun writeInt32(value: Int)

    /**
     * Writes a BSON Int64 to the writer.
     *
     * @param value The Int64 value.
     */
    fun writeInt64(value: Long)

    /**
     * Writes a BSON Decimal128 to the writer.
     *
     * @param value The Decimal128 value.
     */
    fun writeDecimal128(value: BsonDecimal128)

    /**
     * Writes a BSON JavaScript to the writer.
     *
     * @param value The JavaScript code.
     */
    fun writeJavaScript(value: BsonJavaScript)

    /**
     * Writes a BSON JavaScript to the writer
     *
     * @param value The JavaScript code.
     */
    fun writeJavaScriptWithScope(value: String)

    /** Writes a BSON MaxKey to the writer. */
    fun writeMaxKey()

    /** Writes a BSON MinKey to the writer. */
    fun writeMinKey()

    /** Writes a BSON null to the writer. */
    fun writeNull()

    /**
     * Writes a BSON ObjectId to the writer.
     *
     * @param value The ObjectId value.
     */
    fun writeObjectId(value: BsonObjectId)

    /**
     * Writes a BSON regular expression to the writer.
     *
     * @param value the regular expression to write.
     */
    fun writeRegularExpression(value: BsonRegularExpression)

    /**
     * Writes a BSON String to the writer.
     *
     * @param value The String value.
     */
    fun writeString(value: String)

    /**
     * Writes a BSON Symbol to the writer.
     *
     * @param value The symbol.
     */
    fun writeSymbol(value: String)

    /**
     * Writes a BSON Timestamp to the writer.
     *
     * @param value The combined timestamp/increment value.
     */
    fun writeTimestamp(value: BsonTimestamp)

    /** Writes a BSON undefined to the writer. */
    fun writeUndefined()

    /**
     * Reads a single document from a BsonReader and writes it to this.
     *
     * @param reader The source.
     */
    fun pipe(reader: BsonReader)
}
