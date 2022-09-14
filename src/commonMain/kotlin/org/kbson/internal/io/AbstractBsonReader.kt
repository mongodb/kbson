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
import org.kbson.BsonException
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
import org.kbson.internal.validateOperation

/** Abstract base class for BsonReader implementations. */
@Suppress("TooManyFunctions")
public abstract class AbstractBsonReader : BsonReader {

    /** @return The current BsonType. */
    public override var currentBsonType: BsonType? = null

    /** The current state of this reader. */
    public var state: State = State.INITIAL

    /**
     * The context, which will indicate which state the reader is in, for example which part of a document it's
     * currently reading.
     */
    @Suppress("VariableNaming") protected var _context: Context? = null

    /**
     * Return true if the reader has been closed.
     *
     * @return true if closed
     */
    protected var isClosed: Boolean = false

    /** @return the most recently read name. */
    public override var currentName: String? = null

    /** Handles the logic to read the start of a document */
    protected abstract fun doReadStartDocument()

    /** Handles the logic when reading the end of a document */
    protected abstract fun doReadEndDocument()

    /** Handles the logic to read the start of an array */
    protected abstract fun doReadStartArray()

    /** Handles the logic when reading the end of an array */
    protected abstract fun doReadEndArray()

    /**
     * Handles the logic to read binary data
     *
     * @return the BsonBinary value
     */
    protected abstract fun doReadBinaryData(): BsonBinary

    /**
     * Handles the logic to read booleans
     *
     * @return the boolean value
     */
    protected abstract fun doReadBoolean(): Boolean

    /**
     * Handles the logic to read date time
     *
     * @return the long value
     */
    protected abstract fun doReadDateTime(): Long

    /**
     * Handles the logic to read doubles
     *
     * @return the double value
     */
    protected abstract fun doReadDouble(): Double

    /**
     * Handles the logic to read 32 bit ints
     *
     * @return the int value
     */
    protected abstract fun doReadInt32(): Int

    /**
     * Handles the logic to read 64 bit ints
     *
     * @return the long value
     */
    protected abstract fun doReadInt64(): Long

    /**
     * Handles the logic to read BsonDecimal128
     *
     * @return the BsonDecimal128 value
     */
    protected abstract fun doReadDecimal128(): BsonDecimal128

    /**
     * Handles the logic to read JavaScript functions
     *
     * @return the String value
     */
    protected abstract fun doReadJavaScript(): String

    /**
     * Handles the logic to read scoped JavaScript functions
     *
     * @return the scoped Javascript function
     */
    protected abstract fun doReadJavaScriptWithScope(): String

    /**
     * Handles the logic to read an ObjectId
     *
     * @return the ObjectId value
     */
    protected abstract fun doReadObjectId(): BsonObjectId

    /**
     * Handles the logic to read a regular expression
     *
     * @return the BsonRegularExpression value
     */
    protected abstract fun doReadRegularExpression(): BsonRegularExpression

    /**
     * Handles the logic to read a DBPointer
     *
     * @return the BsonDBPointer value
     */
    protected abstract fun doReadDBPointer(): BsonDBPointer

    /**
     * Handles the logic to read a String
     *
     * @return the String value
     */
    protected abstract fun doReadString(): String

    /**
     * Handles the logic to read a Symbol
     *
     * @return the String value
     */
    protected abstract fun doReadSymbol(): String

    /**
     * Handles the logic to read a timestamp
     *
     * @return the BsonTimestamp value
     */
    protected abstract fun doReadTimestamp(): Long

    /** Handles the logic to read an Undefined value */
    protected abstract fun doReadUndefined()

    /** Handles the logic to read a Max key */
    protected abstract fun doReadMaxKey()

    /** Handles the logic to read a Min key */
    protected abstract fun doReadMinKey()

    /** Handles the logic to read a null value */
    protected abstract fun doReadNull()

    /** Handles any logic required to skip the name (reader must be positioned on a name). */
    protected abstract fun doSkipName()

    /** Handles any logic required to skip the value (reader must be positioned on a value). */
    protected abstract fun doSkipValue()

    override fun readStartDocument() {
        checkPreconditions("readStartDocument", BsonType.DOCUMENT)
        doReadStartDocument()
        state = State.TYPE
    }

    public override fun readEndDocument() {
        check(!isClosed && _context != null) { "BinaryWriter is closed" }
        validateOperation(
            _context?.contextType == BsonContextType.DOCUMENT ||
                _context?.contextType == BsonContextType.SCOPE_DOCUMENT) {
            "readEndDocument can only be called when contextType is ${BsonContextType.DOCUMENT} or " +
                "${BsonContextType.SCOPE_DOCUMENT} , not when contextType is ${_context?.contextType}."
        }
        if (state == State.TYPE) {
            readBsonType() // will set state to EndOfDocument if at end of document
        }
        validateOperation(state == State.END_OF_DOCUMENT) {
            "readEndDocument can only be called when State is ${State.END_OF_DOCUMENT}, not when State is $state."
        }
        doReadEndDocument()
        setStateOnEnd()
    }

    override fun readStartArray() {
        checkPreconditions("readStartArray", BsonType.ARRAY)
        doReadStartArray()
        state = State.TYPE
    }

    public override fun readEndArray() {
        check(!isClosed) { "BinaryWriter is closed" }
        validateOperation(_context?.contextType == BsonContextType.ARRAY) {
            "readEndArray can only be called when contextType is ${BsonContextType.ARRAY}, " +
                "not when contextType is ${_context?.contextType}."
        }
        if (state == State.TYPE) {
            readBsonType() // will set state to EndOfArray if at end of array
        }
        validateOperation(state == State.END_OF_ARRAY) {
            "readEndArray can only be called when State is ${State.END_OF_ARRAY}, not when State is $state."
        }
        doReadEndArray()
        setStateOnEnd()
    }

    public override fun readBinary(): BsonBinary {
        checkPreconditions("readBinaryData", BsonType.BINARY)
        state = nextState
        return doReadBinaryData()
    }

    public override fun readBoolean(): BsonBoolean {
        checkPreconditions("readBoolean", BsonType.BOOLEAN)
        state = nextState
        return BsonBoolean(doReadBoolean())
    }

    public override fun readDateTime(): BsonDateTime {
        checkPreconditions("readDateTime", BsonType.DATE_TIME)
        state = nextState
        return BsonDateTime(doReadDateTime())
    }

    public override fun readDouble(): BsonDouble {
        checkPreconditions("readDouble", BsonType.DOUBLE)
        state = nextState
        return BsonDouble(doReadDouble())
    }

    public override fun readInt32(): BsonInt32 {
        checkPreconditions("readInt32", BsonType.INT32)
        state = nextState
        return BsonInt32(doReadInt32())
    }

    public override fun readInt64(): BsonInt64 {
        checkPreconditions("readInt64", BsonType.INT64)
        state = nextState
        return BsonInt64(doReadInt64())
    }

    public override fun readDecimal128(): BsonDecimal128 {
        checkPreconditions("readDecimal", BsonType.DECIMAL128)
        state = nextState
        return doReadDecimal128()
    }

    public override fun readJavaScript(): BsonJavaScript {
        checkPreconditions("readJavaScript", BsonType.JAVASCRIPT)
        state = nextState
        return BsonJavaScript(doReadJavaScript())
    }

    public override fun readJavaScriptWithScope(): String {
        checkPreconditions("readJavaScriptWithScope", BsonType.JAVASCRIPT_WITH_SCOPE)
        state = State.SCOPE_DOCUMENT
        return doReadJavaScriptWithScope()
    }

    public override fun readMaxKey(): BsonMaxKey {
        checkPreconditions("readMaxKey", BsonType.MAX_KEY)
        state = nextState
        doReadMaxKey()
        return BsonMaxKey
    }

    public override fun readMinKey(): BsonMinKey {
        checkPreconditions("readMinKey", BsonType.MIN_KEY)
        state = nextState
        doReadMinKey()
        return BsonMinKey
    }

    public override fun readNull(): BsonNull {
        checkPreconditions("readNull", BsonType.NULL)
        state = nextState
        doReadNull()
        return BsonNull
    }

    public override fun readObjectId(): BsonObjectId {
        checkPreconditions("readObjectId", BsonType.OBJECT_ID)
        state = nextState
        return doReadObjectId()
    }

    public override fun readRegularExpression(): BsonRegularExpression {
        checkPreconditions("readRegularExpression", BsonType.REGULAR_EXPRESSION)
        state = nextState
        return doReadRegularExpression()
    }

    public override fun readDBPointer(): BsonDBPointer {
        checkPreconditions("readDBPointer", BsonType.DB_POINTER)
        state = nextState
        return doReadDBPointer()
    }

    public override fun readString(): BsonString {
        checkPreconditions("readString", BsonType.STRING)
        state = nextState
        return BsonString(doReadString())
    }

    public override fun readSymbol(): BsonSymbol {
        checkPreconditions("readSymbol", BsonType.SYMBOL)
        state = nextState
        return BsonSymbol(doReadSymbol())
    }

    public override fun readTimestamp(): BsonTimestamp {
        checkPreconditions("readTimestamp", BsonType.TIMESTAMP)
        state = nextState
        return BsonTimestamp(doReadTimestamp())
    }

    public override fun readUndefined(): BsonUndefined {
        checkPreconditions("readUndefined", BsonType.UNDEFINED)
        state = nextState
        doReadUndefined()
        return BsonUndefined
    }

    public override fun skipName() {
        check(!isClosed) { "BsonReader is closed" }
        validateOperation(state == State.NAME) {
            "skipName can only be called when State is ${State.NAME}, not when State is $state."
        }
        state = State.VALUE
        doSkipName()
    }

    public override fun skipValue() {
        check(!isClosed) { "BsonReader is closed" }
        validateOperation(state == State.VALUE) {
            "skipValue can only be called when State is ${State.VALUE}, not when State is $state."
        }
        doSkipValue()
        state = State.TYPE
    }

    public override fun readName(): String {
        check(!isClosed) { "BsonReader is closed" }
        if (state == State.TYPE) {
            readBsonType()
        }
        validateOperation(state == State.NAME) {
            "readName can only be called when State is ${State.NAME}, not when State is $state."
        }
        state = State.VALUE
        return currentName!!
    }

    /** Closes the reader. */
    public override fun close() {
        isClosed = true
    }

    /**
     * Ensures any conditions are met before reading commences. Throws exceptions if the conditions are not met.
     *
     * @param methodName the name of the current method, which will indicate the field being read
     * @param type the type of this field
     */
    protected open fun checkPreconditions(methodName: String, type: BsonType) {
        check(!isClosed) { "BsonReader is closed" }
        if (state == State.INITIAL || state == State.SCOPE_DOCUMENT || state == State.TYPE) {
            readBsonType()
        }
        if (state == State.NAME) {
            // ignore name
            skipName()
        }
        validateOperation(state == State.VALUE) {
            "$methodName can only be called when State is ${State.VALUE}, not when State is $state."
        }

        validateOperation(currentBsonType == type) {
            "$methodName can only be called when CurrentBsonType!! is $type, not when CurrentBsonType is:" +
                " $currentBsonType."
        }
    }

    /**
     * Returns the next `State` to transition to, based on the [AbstractBsonReader.Context] of this reader.
     *
     * @return the next state
     */
    private var nextState: State = State.INITIAL
        get() {
            check(_context != null) { "Unexpected ContextType" }
            nextState =
                when (_context!!.contextType) {
                    BsonContextType.ARRAY,
                    BsonContextType.DOCUMENT,
                    BsonContextType.SCOPE_DOCUMENT -> State.TYPE
                    BsonContextType.TOP_LEVEL -> State.DONE
                    else -> throw BsonException("Unexpected ContextType ${_context!!.contextType}")
                }
            return field
        }

    private fun setStateOnEnd() {
        state =
            when (_context?.contextType) {
                BsonContextType.ARRAY,
                BsonContextType.DOCUMENT -> State.TYPE
                BsonContextType.TOP_LEVEL -> State.DONE
                null -> State.DONE
                else -> throw BsonException("Unexpected ContextType ${_context?.contextType}")
            }
    }

    /**
     * The context for the reader. Records the parent context, creating a bread crumb trail to trace back up to the root
     * context of the reader. Also records the [BsonContextType], indicating whether the reader is reading a document,
     * array, or other complex sub-structure.
     */
    @Suppress("ConstructorParameterNaming")
    public open inner class Context(public val _parentContext: Context?, public val contextType: BsonContextType)

    /** The state of a reader. Indicates where in a document the reader is. */
    public enum class State {
        /** The initial state. */
        INITIAL,

        /** The reader is positioned at the type of an element or value. */
        TYPE,

        /** The reader is positioned at the name of an element. */
        NAME,

        /** The reader is positioned at a value. */
        VALUE,

        /** The reader is positioned at a scope document. */
        SCOPE_DOCUMENT,

        /** The reader is positioned at the end of a document. */
        END_OF_DOCUMENT,

        /** The reader is positioned at the end of an array. */
        END_OF_ARRAY,

        /** The reader has finished reading a document. */
        DONE,

        /** The reader is closed. */
        CLOSED
    }
}
