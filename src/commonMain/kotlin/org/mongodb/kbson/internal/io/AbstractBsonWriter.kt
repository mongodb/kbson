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
import org.mongodb.kbson.BsonDBPointer
import org.mongodb.kbson.BsonDecimal128
import org.mongodb.kbson.BsonInvalidOperationException
import org.mongodb.kbson.BsonJavaScript
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.BsonRegularExpression
import org.mongodb.kbson.BsonTimestamp
import org.mongodb.kbson.BsonType
import org.mongodb.kbson.internal.validateOperation
import org.mongodb.kbson.internal.validateSerialization

/** Represents a BSON writer for some external format (see subclasses). */
@Suppress("TooManyFunctions")
internal abstract class AbstractBsonWriter(private val maxSerializationDepth: Int = 1024) : BsonWriter {

    /**
     * The current state of this writer. The current state determines what sort of actions are valid for this writer at
     * this time.
     *
     * @return the current state of the writer.
     */
    protected var state: State = State.INITIAL

    /**
     * The context, which will indicate which state the writer is in, for example which part of a document it's
     * currently writing.
     *
     * @return the current context.
     */
    @Suppress("VariableNaming") protected var _context: Context? = null
    private var serializationDepth = 0

    /**
     * Returns whether this writer has been closed.
     *
     * @return true if the [.close] method has been called.
     */
    protected var isClosed: Boolean = false

    /** Handles the logic to start writing a document */
    protected abstract fun doWriteStartDocument()

    /** Handles the logic of writing the end of a document */
    protected abstract fun doWriteEndDocument()

    /** Handles the logic to start writing an array */
    protected abstract fun doWriteStartArray()

    /** Handles the logic of writing the end of an array */
    protected abstract fun doWriteEndArray()

    /**
     * Handles the logic of writing a `BsonBinary` value
     *
     * @param value the `BsonBinary` value to write
     */
    protected abstract fun doWriteBinaryData(value: BsonBinary)

    /**
     * Handles the logic of writing a boolean value
     *
     * @param value the `boolean` value to write
     */
    protected abstract fun doWriteBoolean(value: Boolean)

    /**
     * Handles the logic of writing a date time value
     *
     * @param value the `long` value to write
     */
    protected abstract fun doWriteDateTime(value: Long)

    /**
     * Handles the logic of writing a DBPointer value
     *
     * @param value the `BsonDBPointer` value to write
     */
    protected abstract fun doWriteDBPointer(value: BsonDBPointer)

    /**
     * Handles the logic of writing a Double value
     *
     * @param value the `double` value to write
     */
    protected abstract fun doWriteDouble(value: Double)

    /**
     * Handles the logic of writing an int32 value
     *
     * @param value the `int` value to write
     */
    protected abstract fun doWriteInt32(value: Int)

    /**
     * Handles the logic of writing an int64 value
     *
     * @param value the `long` value to write
     */
    protected abstract fun doWriteInt64(value: Long)

    /**
     * Handles the logic of writing a Decimal128 value
     *
     * @param value the `Decimal128` value to write
     * @since 3.4
     */
    protected abstract fun doWriteDecimal128(value: BsonDecimal128)

    /**
     * Handles the logic of writing a JavaScript function
     *
     * @param value the `String` value to write
     */
    protected abstract fun doWriteJavaScript(value: BsonJavaScript)

    /**
     * Handles the logic of writing a scoped JavaScript function
     *
     * @param value the `boolean` value to write
     */
    protected abstract fun doWriteJavaScriptWithScope(value: String)

    /** Handles the logic of writing a Max key */
    protected abstract fun doWriteMaxKey()

    /** Handles the logic of writing a Min key */
    protected abstract fun doWriteMinKey()

    /** Handles the logic of writing a Null value */
    protected abstract fun doWriteNull()

    /**
     * Handles the logic of writing an ObjectId
     *
     * @param value the `ObjectId` value to write
     */
    protected abstract fun doWriteObjectId(value: BsonObjectId)

    /**
     * Handles the logic of writing a regular expression
     *
     * @param value the `BsonRegularExpression` value to write
     */
    protected abstract fun doWriteRegularExpression(value: BsonRegularExpression)

    /**
     * Handles the logic of writing a String
     *
     * @param value the `String` value to write
     */
    protected abstract fun doWriteString(value: String)

    /**
     * Handles the logic of writing a Symbol
     *
     * @param value the `boolean` value to write
     */
    protected abstract fun doWriteSymbol(value: String)

    /**
     * Handles the logic of writing a timestamp
     *
     * @param value the `BsonTimestamp` value to write
     */
    protected abstract fun doWriteTimestamp(value: BsonTimestamp)

    /** Handles the logic of writing an Undefined value */
    protected abstract fun doWriteUndefined()

    /**
     * Handles the logic of writing the element name.
     *
     * @param name the name of the element
     */
    protected abstract fun doWriteName(name: String)

    override fun writeStartDocument() {
        checkPreconditions("writeStartDocument", State.INITIAL, State.VALUE, State.SCOPE_DOCUMENT, State.DONE)
        serializationDepth++
        validateSerialization(serializationDepth <= maxSerializationDepth) {
            "Maximum serialization depth exceeded (does the object being serialized have a circular reference)."
        }
        doWriteStartDocument()
        state = State.NAME
    }

    override fun writeEndDocument() {
        checkPreconditions("writeEndDocument", State.NAME)
        val contextType: BsonContextType = _context!!.contextType
        validateOperation(contextType == BsonContextType.DOCUMENT || contextType == BsonContextType.SCOPE_DOCUMENT) {
            "WriteEndDocuent can only be called when ContextType is DOCUMENT or SCOPE_DOCUMENT, " +
                "not when ContextType is $contextType."
        }

        serializationDepth--
        doWriteEndDocument()
        state =
            if (_context?.contextType == BsonContextType.TOP_LEVEL) {
                State.DONE
            } else {
                nextState
            }
    }

    override fun writeStartArray() {
        checkPreconditions("writeStartArray", State.VALUE)
        serializationDepth++
        validateSerialization(serializationDepth <= maxSerializationDepth) {
            "Maximum serialization depth exceeded (does the object being serialized have a circular reference)."
        }
        doWriteStartArray()
        state = State.VALUE
    }

    override fun writeEndArray() {
        checkPreconditions("writeEndArray", State.VALUE)
        val contextType = _context!!.contextType
        validateOperation(contextType == BsonContextType.ARRAY) {
            "WriteEndArray can only be called when ContextType is Array, not when ContextType is $contextType."
        }
        serializationDepth--
        doWriteEndArray()
        state = nextState
    }

    override fun writeBinaryData(value: BsonBinary) {
        checkPreconditions("writeBinaryData", State.VALUE, State.INITIAL)
        doWriteBinaryData(value)
        state = nextState
    }

    override fun writeBoolean(value: Boolean) {
        checkPreconditions("writeBoolean", State.VALUE, State.INITIAL)
        doWriteBoolean(value)
        state = nextState
    }

    override fun writeDateTime(value: Long) {
        checkPreconditions("writeDateTime", State.VALUE, State.INITIAL)
        doWriteDateTime(value)
        state = nextState
    }

    override fun writeDBPointer(value: BsonDBPointer) {
        checkPreconditions("writeDBPointer", State.VALUE, State.INITIAL)
        doWriteDBPointer(value)
        state = nextState
    }

    override fun writeDouble(value: Double) {
        checkPreconditions("writeDBPointer", State.VALUE, State.INITIAL)
        doWriteDouble(value)
        state = nextState
    }

    override fun writeInt32(value: Int) {
        checkPreconditions("writeInt32", State.VALUE)
        doWriteInt32(value)
        state = nextState
    }

    override fun writeInt64(value: Long) {
        checkPreconditions("writeInt64", State.VALUE)
        doWriteInt64(value)
        state = nextState
    }

    override fun writeDecimal128(value: BsonDecimal128) {
        checkPreconditions("writeInt64", State.VALUE)
        doWriteDecimal128(value)
        state = nextState
    }

    override fun writeJavaScript(value: BsonJavaScript) {
        checkPreconditions("writeJavaScript", State.VALUE)
        doWriteJavaScript(value)
        state = nextState
    }

    override fun writeJavaScriptWithScope(value: String) {
        checkPreconditions("writeJavaScriptWithScope", State.VALUE)
        doWriteJavaScriptWithScope(value)
        state = State.SCOPE_DOCUMENT
    }

    override fun writeMaxKey() {
        checkPreconditions("writeMaxKey", State.VALUE)
        doWriteMaxKey()
        state = nextState
    }

    override fun writeMinKey() {
        checkPreconditions("writeMinKey", State.VALUE)
        doWriteMinKey()
        state = nextState
    }

    override fun writeName(name: String) {
        validateOperation(state == State.NAME) {
            "writeName can only be called when State is ${State.NAME}, not when State is $state"
        }
        doWriteName(name)
        state = State.VALUE
    }

    override fun writeNull() {
        checkPreconditions("writeNull", State.VALUE)
        doWriteNull()
        state = nextState
    }

    override fun writeObjectId(value: BsonObjectId) {
        checkPreconditions("writeObjectId", State.VALUE)
        doWriteObjectId(value)
        state = nextState
    }

    override fun writeRegularExpression(value: BsonRegularExpression) {
        checkPreconditions("writeRegularExpression", State.VALUE)
        doWriteRegularExpression(value)
        state = nextState
    }

    override fun writeString(value: String) {
        checkPreconditions("writeString", State.VALUE)
        doWriteString(value)
        state = nextState
    }

    override fun writeSymbol(value: String) {
        checkPreconditions("writeSymbol", State.VALUE)
        doWriteSymbol(value)
        state = nextState
    }

    override fun writeTimestamp(value: BsonTimestamp) {
        checkPreconditions("writeTimestamp", State.VALUE)
        doWriteTimestamp(value)
        state = nextState
    }

    override fun writeUndefined() {
        checkPreconditions("writeUndefined", State.VALUE)
        doWriteUndefined()
        state = nextState
    }

    /**
     * Returns the next valid state for this writer. For example, transitions from [State.VALUE] to [State.NAME] once a
     * value is written.
     *
     * @return the next `State`
     */
    private val nextState: State
        get() =
            if (_context?.contextType == BsonContextType.ARRAY) {
                State.VALUE
            } else {
                State.NAME
            }

    /**
     * Checks if this writer's current state is in the list of given states.
     *
     * @param validStates an array of `State`s to compare this writer's state to.
     * @return true if this writer's state is in the given list.
     */
    private fun checkState(vararg validStates: State): Boolean {
        return validStates.find { it == state } != null
    }

    /**
     * Checks the writer is in the correct state. If the writer's current state is in the list of given states, this
     * method will complete without exception. Throws an [BsonInvalidOperationException] if the writer is closed. Throws
     * BsonInvalidOperationException if the method is trying to do something that is not permitted in the current state.
     *
     * @param methodName the name of the method being performed that checks are being performed for
     * @param validStates the list of valid states for this operation
     * @see .throwInvalidState
     */
    private fun checkPreconditions(methodName: String, vararg validStates: State) {
        check(!isClosed) { "BsonWriter is closed" }
        validateOperation(checkState(*validStates)) {
            "$methodName can only be called when the State is ${validStates.joinToString(" or ") { it.name }}, " +
                "not when State is $state."
        }
    }

    override fun close() {
        isClosed = true
    }

    override fun pipe(reader: BsonReader) {
        pipeDocument(reader)
    }

    private fun pipeDocument(reader: BsonReader) {
        reader.readStartDocument()
        writeStartDocument()
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            writeName(reader.readName())
            pipeValue(reader)
        }
        reader.readEndDocument()
        writeEndDocument()
    }

    @Suppress("ComplexMethod")
    private fun pipeValue(reader: BsonReader) {
        when (reader.currentBsonType) {
            BsonType.ARRAY -> pipeArray(reader)
            BsonType.BINARY -> writeBinaryData(reader.readBinary())
            BsonType.BOOLEAN -> writeBoolean(reader.readBoolean().value)
            BsonType.DATE_TIME -> writeDateTime(reader.readDateTime().value)
            BsonType.DB_POINTER -> writeDBPointer(reader.readDBPointer())
            BsonType.DECIMAL128 -> writeDecimal128(reader.readDecimal128())
            BsonType.DOCUMENT -> pipeDocument(reader)
            BsonType.DOUBLE -> writeDouble(reader.readDouble().doubleValue())
            BsonType.INT32 -> writeInt32(reader.readInt32().intValue())
            BsonType.INT64 -> writeInt64(reader.readInt64().longValue())
            BsonType.JAVASCRIPT -> writeJavaScript(reader.readJavaScript())
            BsonType.JAVASCRIPT_WITH_SCOPE -> pipeJavascriptWithScope(reader)
            BsonType.OBJECT_ID -> writeObjectId(reader.readObjectId())
            BsonType.REGULAR_EXPRESSION -> writeRegularExpression(reader.readRegularExpression())
            BsonType.STRING -> writeString(reader.readString().value)
            BsonType.SYMBOL -> writeSymbol(reader.readSymbol().value)
            BsonType.TIMESTAMP -> writeTimestamp(reader.readTimestamp())
            BsonType.MAX_KEY -> {
                reader.readMaxKey()
                writeMaxKey()
            }
            BsonType.MIN_KEY -> {
                reader.readMinKey()
                writeMinKey()
            }
            BsonType.NULL -> {
                reader.readNull()
                writeNull()
            }
            BsonType.UNDEFINED -> {
                reader.readUndefined()
                writeUndefined()
            }
            else -> throw IllegalArgumentException("unhandled BSON type: " + reader.currentBsonType)
        }
    }
    private fun pipeArray(reader: BsonReader) {
        reader.readStartArray()
        writeStartArray()
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            pipeValue(reader)
        }
        reader.readEndArray()
        writeEndArray()
    }

    private fun pipeJavascriptWithScope(reader: BsonReader) {
        writeJavaScriptWithScope(reader.readJavaScriptWithScope())
        pipeDocument(reader)
    }

    /** The state of a writer. Indicates where in a document the writer is. */
    protected enum class State {
        /** The initial state. */
        INITIAL,

        /** The writer is positioned to write a name. */
        NAME,

        /** The writer is positioned to write a value. */
        VALUE,

        /**
         * The writer is positioned to write a scope document (call WriteStartDocument to start writing the scope
         * document).
         */
        SCOPE_DOCUMENT,

        /** The writer is done. */
        DONE
    }

    /**
     * The context for the writer. Records the parent context, creating a bread crumb trail to trace back up to the root
     * context of the reader. Also records the [BsonContextType], indicating whether the reader is reading a document,
     * array, or other complex sub-structure.
     */
    @Suppress("ConstructorParameterNaming")
    open inner class Context(val contextType: BsonContextType, val name: String? = null)
}
