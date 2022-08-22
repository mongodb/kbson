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
package org.kbson

/** Base class for any BSON type. */
sealed class BsonValue protected constructor() {

    /**
     * Gets the BSON type of this value.
     *
     * @return the BSON type, which may not be null (but may be BSONType.NULL)
     */
    abstract fun getBsonType(): BsonType

    /** @return true if this is a BsonNull, false otherwise */
    open fun isNull(): Boolean {
        return this is BsonNull
    }

    /** @return true if this is a BsonDocument, false otherwise */
    open fun isDocument(): Boolean {
        return this is BsonDocument
    }

    /** @return true if this is a BsonArray, false otherwise */
    open fun isArray(): Boolean {
        return this is BsonArray
    }

    /** @return true if this is a BsonString, false otherwise */
    open fun isString(): Boolean {
        return this is BsonString
    }

    /** @return true if this is a BsonNumber, false otherwise */
    open fun isNumber(): Boolean {
        return isInt32() || isInt64() || isDouble()
    }

    /** @return true if this is a BsonInt32, false otherwise */
    open fun isInt32(): Boolean {
        return this is BsonInt32
    }

    /** @return true if this is a BsonInt64, false otherwise */
    open fun isInt64(): Boolean {
        return this is BsonInt64
    }

    /** @return true if this is a BsonDecimal128, false otherwise */
    open fun isDecimal128(): Boolean {
        return this is BsonDecimal128
    }

    /** @return true if this is a BsonDouble, false otherwise */
    open fun isDouble(): Boolean {
        return this is BsonDouble
    }

    /** @return true if this is a BsonBoolean, false otherwise */
    open fun isBoolean(): Boolean {
        return this is BsonBoolean
    }

    /** @return true if this is an BsonObjectId, false otherwise */
    open fun isObjectId(): Boolean {
        return this is BsonObjectId
    }

    /** @return true if this is a BsonDbPointer, false otherwise */
    open fun isDBPointer(): Boolean {
        return this is BsonDbPointer
    }

    /** @return true if this is a BsonTimestamp, false otherwise */
    open fun isTimestamp(): Boolean {
        return this is BsonTimestamp
    }

    /** @return true if this is a BsonBinary, false otherwise */
    open fun isBinary(): Boolean {
        return this is BsonBinary
    }

    /** @return true if this is a BsonDateTime, false otherwise */
    open fun isDateTime(): Boolean {
        return this is BsonDateTime
    }

    /** @return true if this is a BsonSymbol, false otherwise */
    open fun isSymbol(): Boolean {
        return this is BsonSymbol
    }

    /** @return true if this is a BsonRegularExpression, false otherwise */
    open fun isRegularExpression(): Boolean {
        return this is BsonRegularExpression
    }

    /** @return true if this is a BsonJavaScript, false otherwise */
    open fun isJavaScript(): Boolean {
        return this is BsonJavaScript
    }

    /** @return true if this is a BsonJavaScriptWithScope, false otherwise */
    open fun isJavaScriptWithScope(): Boolean {
        return this is BsonJavaScriptWithScope
    }

    /** @return true if this is a BsonMaxKey, false otherwise */
    open fun isMaxKey(): Boolean {
        return this is BsonMaxKey
    }

    /** @return true if this is a BsonMinKey, false otherwise */
    open fun isMinKey(): Boolean {
        return this is BsonMinKey
    }

    /** @return true if this is a BsonUndefined, false otherwise */
    open fun isUndefined(): Boolean {
        return this is BsonUndefined
    }

    /**
     * Gets this value as a BsonDocument if it is one, otherwise throws exception
     *
     * @return a BsonDocument
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asDocument(): BsonDocument {
        throwIfInvalidType(BsonType.DOCUMENT)
        return this as BsonDocument
    }

    /**
     * Gets this value as a BsonArray if it is one, otherwise throws exception
     *
     * @return a BsonArray
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asArray(): BsonArray {
        throwIfInvalidType(BsonType.ARRAY)
        return this as BsonArray
    }

    /**
     * Gets this value as a BsonString if it is one, otherwise throws exception
     *
     * @return a BsonString
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asString(): BsonString {
        throwIfInvalidType(BsonType.STRING)
        return this as BsonString
    }

    /**
     * Gets this value as a BsonNumber if it is one, otherwise throws exception
     *
     * @return a BsonNumber
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asNumber(): BsonNumber {
        if (getBsonType() !== BsonType.INT32 &&
            getBsonType() !== BsonType.INT64 &&
            getBsonType() !== BsonType.DOUBLE) {
            throw BsonInvalidOperationException(
                "Value expected to be of a numerical BSON type is of unexpected type ${getBsonType()}")
        }
        return this as BsonNumber
    }

    /**
     * Gets this value as a BsonInt32 if it is one, otherwise throws exception
     *
     * @return a BsonInt32
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asInt32(): BsonInt32 {
        throwIfInvalidType(BsonType.INT32)
        return this as BsonInt32
    }

    /**
     * Gets this value as a BsonInt64 if it is one, otherwise throws exception
     *
     * @return a BsonInt64
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asInt64(): BsonInt64 {
        throwIfInvalidType(BsonType.INT64)
        return this as BsonInt64
    }

    /**
     * Gets this value as a BsonDecimal128 if it is one, otherwise throws exception
     *
     * @return a BsonDecimal128
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     * @since 3.4
     */
    open fun asDecimal128(): BsonDecimal128 {
        throwIfInvalidType(BsonType.DECIMAL128)
        return this as BsonDecimal128
    }

    /**
     * Gets this value as a BsonDouble if it is one, otherwise throws exception
     *
     * @return a BsonDouble
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asDouble(): BsonDouble {
        throwIfInvalidType(BsonType.DOUBLE)
        return this as BsonDouble
    }

    /**
     * Gets this value as a BsonBoolean if it is one, otherwise throws exception
     *
     * @return a BsonBoolean
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asBoolean(): BsonBoolean {
        throwIfInvalidType(BsonType.BOOLEAN)
        return this as BsonBoolean
    }

    /**
     * Gets this value as an BsonObjectId if it is one, otherwise throws exception
     *
     * @return an BsonObjectId
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asObjectId(): BsonObjectId {
        throwIfInvalidType(BsonType.OBJECT_ID)
        return this as BsonObjectId
    }

    /**
     * Gets this value as a BsonDbPointer if it is one, otherwise throws exception
     *
     * @return an BsonDbPointer
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asDBPointer(): BsonDbPointer {
        throwIfInvalidType(BsonType.DB_POINTER)
        return this as BsonDbPointer
    }

    /**
     * Gets this value as a BsonTimestamp if it is one, otherwise throws exception
     *
     * @return an BsonTimestamp
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asTimestamp(): BsonTimestamp {
        throwIfInvalidType(BsonType.TIMESTAMP)
        return this as BsonTimestamp
    }

    /**
     * Gets this value as a BsonBinary if it is one, otherwise throws exception
     *
     * @return an BsonBinary
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asBinary(): BsonBinary {
        throwIfInvalidType(BsonType.BINARY)
        return this as BsonBinary
    }

    /**
     * Gets this value as a BsonDateTime if it is one, otherwise throws exception
     *
     * @return an BsonDateTime
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asDateTime(): BsonDateTime {
        throwIfInvalidType(BsonType.DATE_TIME)
        return this as BsonDateTime
    }

    /**
     * Gets this value as a BsonSymbol if it is one, otherwise throws exception
     *
     * @return an BsonSymbol
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asSymbol(): BsonSymbol {
        throwIfInvalidType(BsonType.SYMBOL)
        return this as BsonSymbol
    }

    /**
     * Gets this value as a BsonRegularExpression if it is one, otherwise throws exception
     *
     * @return an BsonRegularExpression
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asRegularExpression(): BsonRegularExpression {
        throwIfInvalidType(BsonType.REGULAR_EXPRESSION)
        return this as BsonRegularExpression
    }

    /**
     * Gets this value as a `BsonJavaScript` if it is one, otherwise throws exception
     *
     * @return a BsonJavaScript
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asJavaScript(): BsonJavaScript {
        throwIfInvalidType(BsonType.JAVASCRIPT)
        return this as BsonJavaScript
    }

    /**
     * Gets this value as a BsonJavaScriptWithScope if it is one, otherwise throws exception
     *
     * @return a BsonJavaScriptWithScope
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    open fun asJavaScriptWithScope(): BsonJavaScriptWithScope {
        throwIfInvalidType(BsonType.JAVASCRIPT_WITH_SCOPE)
        return this as BsonJavaScriptWithScope
    }

    private fun throwIfInvalidType(expectedType: BsonType) {
        if (getBsonType() !== expectedType) {
            throw BsonInvalidOperationException(
                "Value expected to be of type $expectedType is of unexpected type ${getBsonType()}")
        }
    }
}
