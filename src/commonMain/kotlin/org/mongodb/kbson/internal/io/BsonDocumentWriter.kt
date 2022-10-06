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

import org.mongodb.kbson.BsonArray
import org.mongodb.kbson.BsonBinary
import org.mongodb.kbson.BsonBoolean
import org.mongodb.kbson.BsonDBPointer
import org.mongodb.kbson.BsonDateTime
import org.mongodb.kbson.BsonDecimal128
import org.mongodb.kbson.BsonDocument
import org.mongodb.kbson.BsonDouble
import org.mongodb.kbson.BsonInt32
import org.mongodb.kbson.BsonInt64
import org.mongodb.kbson.BsonInvalidOperationException
import org.mongodb.kbson.BsonJavaScript
import org.mongodb.kbson.BsonJavaScriptWithScope
import org.mongodb.kbson.BsonMaxKey
import org.mongodb.kbson.BsonMinKey
import org.mongodb.kbson.BsonNull
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.BsonRegularExpression
import org.mongodb.kbson.BsonString
import org.mongodb.kbson.BsonSymbol
import org.mongodb.kbson.BsonTimestamp
import org.mongodb.kbson.BsonUndefined
import org.mongodb.kbson.BsonValue
import org.mongodb.kbson.internal.validateSerialization

/** A BsonWriter implementation that writes to a binary stream of data to a BsonDocument */
@Suppress("TooManyFunctions")
internal class BsonDocumentWriter : AbstractBsonWriter() {
    val bsonDocument: BsonDocument = BsonDocument()
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
    ) : Context(contextType, currentName) {

        fun add(value: BsonValue) {
            require(container.isDocument() || container.isArray()) { "Cannot add $value to $container" }
            if (container.isDocument()) {
                validateSerialization(currentName != null) { "Missing fields current name." }
                container.asDocument()[currentName] = value
            } else {
                container.asArray().add(value)
            }
        }
    }
}
