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
import org.kbson.BsonBinarySubType
import org.kbson.BsonDBPointer
import org.kbson.BsonDecimal128
import org.kbson.BsonObjectId
import org.kbson.BsonRegularExpression
import org.kbson.BsonSerializationException
import org.kbson.BsonType
import org.kbson.internal.validateOperation
import org.kbson.internal.validateSerialization

/**
 * A BsonReader implementation that reads from a binary stream of data. This is the most commonly used implementation.
 */
@Suppress("TooManyFunctions", "EmptyFunctionBlock", "MagicNumber")
internal class BsonBinaryReader(private val bsonInput: ByteArrayBsonInput) : AbstractBsonReader() {

    private var context: BsonBinaryReaderContext = BsonBinaryReaderContext(null, BsonContextType.TOP_LEVEL, 0, 0)
        set(context) {
            _context = context
            field = context
        }

    override fun doReadStartDocument() {
        val contextType: BsonContextType =
            if (state == State.SCOPE_DOCUMENT) BsonContextType.SCOPE_DOCUMENT else BsonContextType.DOCUMENT
        val startPosition: Int = bsonInput.position // position of size field
        val size = readSize()
        context = BsonBinaryReaderContext(context, contextType, startPosition, size)
    }

    override fun doReadEndDocument() {
        context = context.popContext(bsonInput.position)
        if (context.contextType == BsonContextType.JAVASCRIPT_WITH_SCOPE) {
            context = context.popContext(bsonInput.position) // JavaScriptWithScope
        }
    }

    override fun doReadStartArray() {
        val startPosition: Int = bsonInput.position // position of size field
        val size = readSize()
        context = BsonBinaryReaderContext(context, BsonContextType.ARRAY, startPosition, size)
    }

    override fun doReadEndArray() {
        context = context.popContext(bsonInput.position)
    }

    override fun doReadBinaryData(): BsonBinary {
        var numBytes: Int = readSize()
        val type = bsonInput.readByte()
        if (type == BsonBinarySubType.OLD_BINARY.value) {
            val repeatedNumBytes = bsonInput.readInt32()
            validateSerialization(repeatedNumBytes == numBytes - 4) {
                "Binary sub type OldBinary has inconsistent sizes"
            }
            numBytes -= 4
        }
        return BsonBinary(type, bsonInput.readBytes(ByteArray(numBytes)))
    }

    override fun doReadBoolean(): Boolean {
        val booleanByte = bsonInput.readByte()
        validateSerialization(booleanByte.toInt() == 0 || booleanByte.toInt() == 1) {
            "Expected a boolean value but found $booleanByte"
        }
        return booleanByte.toInt() == 0x1
    }

    override fun doReadDateTime(): Long {
        return bsonInput.readInt64()
    }

    override fun doReadDouble(): Double {
        return bsonInput.readDouble()
    }

    override fun doReadInt32(): Int {
        return bsonInput.readInt32()
    }

    override fun doReadInt64(): Long {
        return bsonInput.readInt64()
    }

    override fun doReadDecimal128(): BsonDecimal128 {
        val low = bsonInput.readInt64()
        val high = bsonInput.readInt64()
        return BsonDecimal128.fromIEEE754BIDEncoding(high.toULong(), low.toULong())
    }

    override fun doReadJavaScript(): String {
        return bsonInput.readString()
    }

    override fun doReadJavaScriptWithScope(): String {
        val startPosition: Int = bsonInput.position // position of size field
        val size = readSize()
        context = BsonBinaryReaderContext(context, BsonContextType.JAVASCRIPT_WITH_SCOPE, startPosition, size)
        return bsonInput.readString()
    }

    override fun doReadMaxKey() {}

    override fun doReadMinKey() {}

    override fun doReadNull() {}

    override fun doReadObjectId(): BsonObjectId {
        return bsonInput.readObjectId()
    }

    override fun doReadRegularExpression(): BsonRegularExpression {
        return BsonRegularExpression(bsonInput.readCString(), bsonInput.readCString())
    }

    override fun doReadDBPointer(): BsonDBPointer {
        return BsonDBPointer(bsonInput.readString(), bsonInput.readObjectId())
    }

    override fun doReadString(): String {
        return bsonInput.readString()
    }

    override fun doReadSymbol(): String {
        return bsonInput.readString()
    }

    override fun doReadTimestamp(): Long {
        return bsonInput.readInt64()
    }

    override fun doReadUndefined() {}

    override fun doSkipName() {}

    @Suppress("ComplexMethod")
    override fun doSkipValue() {
        validateOperation(state == State.VALUE) {
            "skipValue can only be called when State is ${State.VALUE}, not when State is $state."
        }

        val skip: Int =
            when (currentBsonType) {
                BsonType.ARRAY -> readSize() - 4
                BsonType.BINARY -> readSize() + 1
                BsonType.BOOLEAN -> 1
                BsonType.DATE_TIME -> 8
                BsonType.DOCUMENT -> readSize() - 4
                BsonType.DOUBLE -> 8
                BsonType.INT32 -> 4
                BsonType.INT64 -> 8
                BsonType.DECIMAL128 -> 16
                BsonType.JAVASCRIPT -> readSize()
                BsonType.JAVASCRIPT_WITH_SCOPE -> readSize() - 4
                BsonType.MAX_KEY -> 0
                BsonType.MIN_KEY -> 0
                BsonType.NULL -> 0
                BsonType.OBJECT_ID -> 12
                BsonType.REGULAR_EXPRESSION -> {
                    bsonInput.skipCString()
                    bsonInput.skipCString()
                    0
                }
                BsonType.STRING -> readSize()
                BsonType.SYMBOL -> readSize()
                BsonType.TIMESTAMP -> 8
                BsonType.UNDEFINED -> 0
                BsonType.DB_POINTER -> readSize() + 12 // String followed by ObjectId
                else -> throw BsonSerializationException("Unexpected BSON type: $currentBsonType")
            }
        bsonInput.skip(skip)
        state = State.TYPE
    }

    override fun readBsonType(): BsonType {
        check(!isClosed) { "BsonBinaryReader" }

        if (state == State.INITIAL || state == State.DONE || state == State.SCOPE_DOCUMENT) {
            // there is an implied type of Document for the top level and for scope documents
            currentBsonType = BsonType.DOCUMENT
            state = State.VALUE
            return currentBsonType!!
        }
        validateOperation(state == State.TYPE) {
            "readBsonType can only be called when State is ${State.TYPE}, not when State is $state."
        }

        val bsonTypeByte = bsonInput.readByte()
        val bsonType: BsonType? = BsonType.values().find { it.value == bsonTypeByte }
        validateSerialization(bsonType != null) {
            "Detected unknown BSON type '$bsonTypeByte' for field name \"${bsonInput.readCString()}\". "
        }

        currentBsonType = bsonType

        return when (context.contextType) {
            BsonContextType.ARRAY -> {
                if (currentBsonType == BsonType.END_OF_DOCUMENT) {
                    state = State.END_OF_ARRAY
                    BsonType.END_OF_DOCUMENT
                } else {
                    bsonInput.skipCString() // ignore array element names
                    state = State.VALUE
                    currentBsonType!!
                }
            }
            BsonContextType.DOCUMENT,
            BsonContextType.SCOPE_DOCUMENT -> {
                if (currentBsonType == BsonType.END_OF_DOCUMENT) {
                    state = State.END_OF_DOCUMENT
                    BsonType.END_OF_DOCUMENT
                } else {
                    currentName = bsonInput.readCString()
                    state = State.NAME
                    currentBsonType!!
                }
            }
            else ->
                throw BsonSerializationException(
                    "BsonType EndOfDocument is not valid when ContextType is ${context.contextType}")
        }
    }

    public companion object {
        public operator fun invoke(byteArray: ByteArray): BsonBinaryReader {
            return BsonBinaryReader(ByteArrayBsonInput(byteArray))
        }
    }

    private fun readSize(): Int {
        val size = bsonInput.readInt32()
        validateSerialization(size >= 0) { "Size $size is not valid because it is negative." }
        return size
    }

    /** An implementation of `AbstractBsonReader.Context`. */
    private inner class BsonBinaryReaderContext(
        private val parentContext: BsonBinaryReaderContext?,
        contextType: BsonContextType,
        private val startPosition: Int,
        private val size: Int
    ) : Context(parentContext, contextType) {
        fun popContext(position: Int): BsonBinaryReaderContext {
            val actualSize = position - startPosition
            validateSerialization(actualSize == size) { "Expected size to be $size, not $actualSize." }
            return parentContext!!
        }
    }
}
