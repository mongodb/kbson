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

import org.mongodb.kbson.BsonBinary
import org.mongodb.kbson.BsonBinarySubType
import org.mongodb.kbson.BsonDBPointer
import org.mongodb.kbson.BsonDecimal128
import org.mongodb.kbson.BsonJavaScript
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.BsonRegularExpression
import org.mongodb.kbson.BsonTimestamp
import org.mongodb.kbson.BsonType

/**
 * A BsonWriter implementation that writes to a binary stream of data. This is the most commonly used implementation.
 */
@Suppress("TooManyFunctions", "MagicNumber")
internal class BsonBinaryWriter(val bsonOutput: ByteArrayBsonOutput) : AbstractBsonWriter() {

    private var context: BsonBinaryWriterContext = BsonBinaryWriterContext(null, BsonContextType.TOP_LEVEL, null, 0)
        set(context) {
            _context = context
            field = context
        }

    override fun doWriteStartDocument() {
        if (state == State.VALUE) {
            bsonOutput.writeByte(BsonType.DOCUMENT.toByte())
            writeCurrentName()
        }
        context = BsonBinaryWriterContext(context, BsonContextType.DOCUMENT, context.name, bsonOutput.position)
        bsonOutput.writeInt32(0) // reserve space for size
    }

    override fun doWriteEndDocument() {
        bsonOutput.writeByte(0)
        backpatchSize() // size of document

        context = context.parentContext!!
        if (context.contextType == BsonContextType.JAVASCRIPT_WITH_SCOPE) {
            backpatchSize()
            context = context.parentContext!!
        }
    }

    override fun doWriteStartArray() {
        bsonOutput.writeByte(BsonType.ARRAY.toByte())
        writeCurrentName()
        context = BsonBinaryWriterContext(context, BsonContextType.ARRAY, context.name, bsonOutput.position)
        bsonOutput.writeInt32(0) // reserve space for size
    }

    override fun doWriteEndArray() {
        bsonOutput.writeByte(0)
        backpatchSize() // size of document
        context = context.parentContext!!
    }

    override fun doWriteBinaryData(value: BsonBinary) {
        bsonOutput.writeByte(BsonType.BINARY.toByte())
        writeCurrentName()

        var totalLength: Int = value.data.size
        if (value.type == BsonBinarySubType.OLD_BINARY.value) {
            totalLength += 4
        }

        bsonOutput.writeInt32(totalLength)
        bsonOutput.writeByte(value.type)
        if (value.type == BsonBinarySubType.OLD_BINARY.value) {
            bsonOutput.writeInt32(totalLength - 4)
        }
        bsonOutput.writeBytes(value.data)
    }

    override fun doWriteBoolean(value: Boolean) {
        bsonOutput.writeByte(BsonType.BOOLEAN.toByte())
        writeCurrentName()
        bsonOutput.writeByte(if (value) 1 else 0)
    }

    override fun doWriteDateTime(value: Long) {
        bsonOutput.writeByte(BsonType.DATE_TIME.toByte())
        writeCurrentName()
        bsonOutput.writeInt64(value)
    }

    override fun doWriteDBPointer(value: BsonDBPointer) {
        bsonOutput.writeByte(BsonType.DB_POINTER.toByte())
        writeCurrentName()

        bsonOutput.writeString(value.namespace)
        bsonOutput.writeBytes(value.id.toByteArray())
    }

    override fun doWriteDouble(value: Double) {
        bsonOutput.writeByte(BsonType.DOUBLE.toByte())
        writeCurrentName()
        bsonOutput.writeDouble(value)
    }

    override fun doWriteInt32(value: Int) {
        bsonOutput.writeByte(BsonType.INT32.toByte())
        writeCurrentName()
        bsonOutput.writeInt32(value)
    }

    override fun doWriteInt64(value: Long) {
        bsonOutput.writeByte(BsonType.INT64.toByte())
        writeCurrentName()
        bsonOutput.writeInt64(value)
    }

    override fun doWriteDecimal128(value: BsonDecimal128) {
        bsonOutput.writeByte(BsonType.DECIMAL128.toByte())
        writeCurrentName()
        bsonOutput.writeInt64(value.value.low.toLong())
        bsonOutput.writeInt64(value.value.high.toLong())
    }

    override fun doWriteJavaScript(value: BsonJavaScript) {
        bsonOutput.writeByte(BsonType.JAVASCRIPT.toByte())
        writeCurrentName()
        bsonOutput.writeString(value.code)
    }

    override fun doWriteJavaScriptWithScope(value: String) {
        bsonOutput.writeByte(BsonType.JAVASCRIPT_WITH_SCOPE.toByte())
        writeCurrentName()
        context =
            BsonBinaryWriterContext(context, BsonContextType.JAVASCRIPT_WITH_SCOPE, context.name, bsonOutput.position)
        bsonOutput.writeInt32(0)
        bsonOutput.writeString(value)
    }

    override fun doWriteMaxKey() {
        bsonOutput.writeByte(BsonType.MAX_KEY.toByte())
        writeCurrentName()
    }

    override fun doWriteMinKey() {
        bsonOutput.writeByte(BsonType.MIN_KEY.toByte())
        writeCurrentName()
    }

    override fun doWriteNull() {
        bsonOutput.writeByte(BsonType.NULL.toByte())
        writeCurrentName()
    }

    override fun doWriteObjectId(value: BsonObjectId) {
        bsonOutput.writeByte(BsonType.OBJECT_ID.toByte())
        writeCurrentName()
        bsonOutput.writeBytes(value.toByteArray())
    }

    override fun doWriteRegularExpression(value: BsonRegularExpression) {
        bsonOutput.writeByte(BsonType.REGULAR_EXPRESSION.toByte())
        writeCurrentName()
        bsonOutput.writeCString(value.pattern)
        bsonOutput.writeCString(value.options)
    }

    override fun doWriteString(value: String) {
        bsonOutput.writeByte(BsonType.STRING.toByte())
        writeCurrentName()
        bsonOutput.writeString(value)
    }

    override fun doWriteSymbol(value: String) {
        bsonOutput.writeByte(BsonType.SYMBOL.toByte())
        writeCurrentName()
        bsonOutput.writeString(value)
    }

    override fun doWriteTimestamp(value: BsonTimestamp) {
        bsonOutput.writeByte(BsonType.TIMESTAMP.toByte())
        writeCurrentName()
        bsonOutput.writeInt64(value.value)
    }

    override fun doWriteUndefined() {
        bsonOutput.writeByte(BsonType.UNDEFINED.toByte())
        writeCurrentName()
    }

    override fun doWriteName(name: String) {
        context =
            BsonBinaryWriterContext(
                context.parentContext, context.contextType, name, context.startPosition, context.index)
    }

    private fun writeCurrentName() {
        if (context.contextType == BsonContextType.ARRAY) {
            bsonOutput.writeCString(context.index++.toString())
        } else {
            bsonOutput.writeCString(context.name!!)
        }
    }

    private fun backpatchSize() {
        val size: Int = bsonOutput.position - context.startPosition
        bsonOutput.writeInt32(bsonOutput.position - size, size)
    }

    /** An implementation of `AbstractBsonWriter.Context`. */
    private inner class BsonBinaryWriterContext(
        val parentContext: BsonBinaryWriterContext?,
        contextType: BsonContextType,
        currentName: String?,
        val startPosition: Int,
        var index: Int = 0 // used when contextType is an array
    ) : Context(contextType, currentName)

    companion object {
        operator fun invoke(): BsonBinaryWriter {
            return BsonBinaryWriter(ByteArrayBsonOutput())
        }
    }
}
