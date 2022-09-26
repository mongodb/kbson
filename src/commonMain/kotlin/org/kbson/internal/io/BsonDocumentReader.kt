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
import org.kbson.BsonDocument
import org.kbson.BsonObjectId
import org.kbson.BsonRegularExpression
import org.kbson.BsonSerializationException
import org.kbson.BsonType
import org.kbson.BsonValue
import org.kbson.internal.validateOperation
import org.kbson.internal.validateSerialization

/**
 * A BsonReader implementation that reads from a binary stream of data. This is the most commonly used implementation.
 */
@Suppress("TooManyFunctions", "EmptyFunctionBlock", "MagicNumber")
internal class BsonDocumentReader(document: BsonDocument) : AbstractBsonReader() {

    private var context = BsonDocumentReaderContext(null, BsonContextType.TOP_LEVEL, document)
        set(context) {
            _context = context
            field = context
        }

    private var currentValue: BsonValue = document

    override fun doReadBinaryData(): BsonBinary {
        return currentValue.asBinary()
    }

    override fun doReadBoolean(): Boolean {
        return currentValue.asBoolean().value
    }

    override fun doReadDateTime(): Long {
        return currentValue.asDateTime().value
    }

    override fun doReadDouble(): Double {
        return currentValue.asDouble().value
    }

    override fun doReadEndArray() {
        context = context.popContext()
    }

    override fun doReadEndDocument() {
        context = context.popContext()
        state =
            when (context.contextType) {
                BsonContextType.ARRAY,
                BsonContextType.DOCUMENT -> State.TYPE
                BsonContextType.TOP_LEVEL -> State.DONE
                else -> throw BsonSerializationException("Unexpected ContextType.")
            }
    }

    override fun doReadInt32(): Int {
        return currentValue.asInt32().value
    }

    override fun doReadInt64(): Long {
        return currentValue.asInt64().value
    }

    override fun doReadDecimal128(): BsonDecimal128 {
        return currentValue.asDecimal128()
    }

    override fun doReadJavaScript(): String {
        return currentValue.asJavaScript().code
    }

    override fun doReadJavaScriptWithScope(): String {
        return currentValue.asJavaScriptWithScope().code
    }

    override fun doReadMaxKey() {}

    override fun doReadMinKey() {}

    override fun doReadNull() {}

    override fun doReadObjectId(): BsonObjectId {
        return currentValue.asObjectId()
    }

    override fun doReadRegularExpression(): BsonRegularExpression {
        return currentValue.asRegularExpression()
    }

    override fun doReadDBPointer(): BsonDBPointer {
        return currentValue.asDBPointer()
    }

    override fun doReadStartArray() {
        context = BsonDocumentReaderContext(context, BsonContextType.ARRAY, currentValue.asArray())
    }

    override fun doReadStartDocument() {
        val document: BsonDocument =
            if (currentValue.bsonType == BsonType.JAVASCRIPT_WITH_SCOPE) {
                currentValue.asJavaScriptWithScope().scope
            } else {
                currentValue.asDocument()
            }
        context = BsonDocumentReaderContext(context, BsonContextType.DOCUMENT, document)
    }

    override fun doReadString(): String {
        return currentValue.asString().value
    }

    override fun doReadSymbol(): String {
        return currentValue.asSymbol().value
    }

    override fun doReadTimestamp(): Long {
        return currentValue.asTimestamp().value
    }

    override fun doReadUndefined() {}

    override fun doSkipName() {}

    override fun doSkipValue() {}

    @Suppress("ReturnCount")
    override fun readBsonType(): BsonType {
        if (state == State.INITIAL || state == State.DONE || state == State.SCOPE_DOCUMENT) {
            // there is an implied type of Document for the top level and for scope documents
            currentBsonType = BsonType.DOCUMENT
            state = State.VALUE
            return currentBsonType!!
        }

        validateOperation(state == State.TYPE) {
            "readBsonType can only be called when State is ${State.TYPE}, not when State is $state."
        }

        when (context.contextType) {
            BsonContextType.ARRAY -> {
                if (context.arrayIterator.hasNext()) {
                    currentValue = context.arrayIterator.next()
                    state = State.VALUE
                } else {
                    state = State.END_OF_ARRAY
                    return BsonType.END_OF_DOCUMENT
                }
            }
            BsonContextType.DOCUMENT -> {
                if (context.documentIterator.hasNext()) {
                    val currentElement = context.documentIterator.next()
                    currentName = currentElement.key
                    currentValue = currentElement.value
                    state = State.NAME
                } else {
                    state = State.END_OF_DOCUMENT
                    return BsonType.END_OF_DOCUMENT
                }
            }
            else -> throw BsonSerializationException("Invalid ContextType.")
        }
        currentBsonType = currentValue.bsonType
        return currentValue.bsonType
    }

    /** An implementation of `AbstractBsonReader.Context`. */
    private inner class BsonDocumentReaderContext(
        private val parentContext: BsonDocumentReaderContext?,
        contextType: BsonContextType,
        val bsonValue: BsonValue
    ) : Context(contextType) {

        val arrayIterator by lazy { bsonValue.asArray().values.iterator() }
        val documentIterator by lazy { bsonValue.asDocument().entries.iterator() }

        fun popContext(): BsonDocumentReaderContext {
            validateSerialization(parentContext != null) { "Missing parent context." }
            return parentContext
        }
    }
}
