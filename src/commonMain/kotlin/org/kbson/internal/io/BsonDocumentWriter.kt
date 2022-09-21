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

import org.kbson.BsonArray
import org.kbson.BsonBinary
import org.kbson.BsonBoolean
import org.kbson.BsonDBPointer
import org.kbson.BsonDateTime
import org.kbson.BsonDecimal128
import org.kbson.BsonDocument
import org.kbson.BsonDouble
import org.kbson.BsonInt32
import org.kbson.BsonInt64
import org.kbson.BsonInvalidOperationException
import org.kbson.BsonJavaScript
import org.kbson.BsonJavaScriptWithScope
import org.kbson.BsonMaxKey
import org.kbson.BsonMinKey
import org.kbson.BsonNull
import org.kbson.BsonObjectId
import org.kbson.BsonRegularExpression
import org.kbson.BsonString
import org.kbson.BsonSymbol
import org.kbson.BsonTimestamp
import org.kbson.BsonUndefined
import org.kbson.BsonValue

/** A BsonWriter implementation that writes to a binary stream of data to a BsonDocument */
@Suppress("TooManyFunctions")
internal class BsonDocumentWriter : AbstractBsonWriter() {
    public val bsonDocument: BsonDocument = BsonDocument()
    private var context: BsonDocumentWriterContext =
        BsonDocumentWriterContext(null, BsonContextType.TOP_LEVEL, bsonDocument)
        set(context) {
            _context = context
            field = context
        }

    override fun doWriteStartDocument() {
        context =
            when (state) {
                State.INITIAL -> BsonDocumentWriterContext(context, BsonContextType.DOCUMENT, bsonDocument)
                State.VALUE -> BsonDocumentWriterContext(context, BsonContextType.DOCUMENT, BsonDocument())
                State.SCOPE_DOCUMENT ->
                    BsonDocumentWriterContext(context, BsonContextType.SCOPE_DOCUMENT, BsonDocument())
                else -> throw BsonInvalidOperationException("Unexpected state $state")
            }
    }

    override fun doWriteEndDocument() {
        val value: BsonValue = context.container
        context = context.parentContext!!

        if (context.contextType == BsonContextType.JAVASCRIPT_WITH_SCOPE) {
            val code: BsonString = context.container as BsonString
            val scope: BsonDocument = value as BsonDocument
            context = context.parentContext!!
            write(BsonJavaScriptWithScope(code.value, scope))
        } else if (context.contextType != BsonContextType.TOP_LEVEL) {
            write(value)
        }
    }

    override fun doWriteStartArray() {
        context = BsonDocumentWriterContext(context, BsonContextType.ARRAY, BsonArray())
    }

    override fun doWriteEndArray() {
        val value = context.container
        context = context.parentContext!!
        write(value)
    }

    override fun doWriteBinaryData(value: BsonBinary) {
        write(value)
    }

    override fun doWriteBoolean(value: Boolean) {
        write(BsonBoolean(value))
    }

    override fun doWriteDateTime(value: Long) {
        write(BsonDateTime(value))
    }

    override fun doWriteDBPointer(value: BsonDBPointer) {
        write(value)
    }

    override fun doWriteDouble(value: Double) {
        write(BsonDouble(value))
    }

    override fun doWriteInt32(value: Int) {
        write(BsonInt32(value))
    }

    override fun doWriteInt64(value: Long) {
        write(BsonInt64(value))
    }

    override fun doWriteDecimal128(value: BsonDecimal128) {
        write(value)
    }

    override fun doWriteJavaScript(value: BsonJavaScript) {
        write(value)
    }

    override fun doWriteJavaScriptWithScope(value: String) {
        context =
            BsonDocumentWriterContext(
                context, BsonContextType.JAVASCRIPT_WITH_SCOPE, BsonString(value), context.currentName)
    }

    override fun doWriteMaxKey() {
        write(BsonMaxKey)
    }

    override fun doWriteMinKey() {
        write(BsonMinKey)
    }

    override fun doWriteNull() {
        write(BsonNull)
    }

    override fun doWriteObjectId(value: BsonObjectId) {
        write(value)
    }

    override fun doWriteRegularExpression(value: BsonRegularExpression) {
        write(value)
    }

    override fun doWriteString(value: String) {
        write(BsonString(value))
    }

    override fun doWriteSymbol(value: String) {
        write(BsonSymbol(value))
    }

    override fun doWriteTimestamp(value: BsonTimestamp) {
        write(value)
    }

    override fun doWriteUndefined() {
        write(BsonUndefined)
    }

    override fun doWriteName(name: String) {
        context = BsonDocumentWriterContext(context.parentContext, context.contextType, context.container, name)
    }

    private fun write(value: BsonValue) {
        context.add(value)
    }

    /** An implementation of `Context`. */
    private inner class BsonDocumentWriterContext(
        val parentContext: BsonDocumentWriterContext?,
        contextType: BsonContextType,
        val container: BsonValue,
        val currentName: String? = null
    ) : Context(parentContext, contextType, currentName) {

        fun add(value: BsonValue) {
            require(container.isDocument() || container.isArray()) { "Cannot add $value to $container" }
            if (container.isDocument()) {
                container.asDocument().put(currentName!!, value)
            } else {
                container.asArray().add(value)
            }
        }
    }
}
