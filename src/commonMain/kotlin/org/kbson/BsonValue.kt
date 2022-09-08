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
@Suppress("TooManyFunctions")
public sealed class BsonValue {

    /**
     * Gets the BSON type of this value.
     *
     * @return the BSON type
     */
    public abstract val bsonType: BsonType

    /** @return true if this is a BsonNull, false otherwise */
    public fun isNull(): Boolean {
        return this is BsonNull
    }

    /** @return true if this is a BsonDocument, false otherwise */
    public fun isDocument(): Boolean {
        return this is BsonDocument
    }

    /** @return true if this is a BsonArray, false otherwise */
    public fun isArray(): Boolean {
        return this is BsonArray
    }

    /** @return true if this is a BsonString, false otherwise */
    public fun isString(): Boolean {
        return this is BsonString
    }

    /** @return true if this is a BsonNumber, false otherwise */
    public fun isNumber(): Boolean {
        return isInt32() || isInt64() || isDouble()
    }

    /** @return true if this is a BsonInt32, false otherwise */
    public fun isInt32(): Boolean {
        return this is BsonInt32
    }

    /** @return true if this is a BsonInt64, false otherwise */
    public fun isInt64(): Boolean {
        return this is BsonInt64
    }

    /** @return true if this is a BsonDecimal128, false otherwise */
    public fun isDecimal128(): Boolean {
        return this is BsonDecimal128
    }

    /** @return true if this is a BsonDouble, false otherwise */
    public fun isDouble(): Boolean {
        return this is BsonDouble
    }

    /** @return true if this is a BsonBoolean, false otherwise */
    public fun isBoolean(): Boolean {
        return this is BsonBoolean
    }

    /** @return true if this is an BsonObjectId, false otherwise */
    public fun isObjectId(): Boolean {
        return this is BsonObjectId
    }

    /** @return true if this is a BsonDbPointer, false otherwise */
    public fun isDBPointer(): Boolean {
        return this is BsonDbPointer
    }

    /** @return true if this is a BsonTimestamp, false otherwise */
    public fun isTimestamp(): Boolean {
        return this is BsonTimestamp
    }

    /** @return true if this is a BsonBinary, false otherwise */
    public fun isBinary(): Boolean {
        return this is BsonBinary
    }

    /** @return true if this is a BsonDateTime, false otherwise */
    public fun isDateTime(): Boolean {
        return this is BsonDateTime
    }

    /** @return true if this is a BsonSymbol, false otherwise */
    public fun isSymbol(): Boolean {
        return this is BsonSymbol
    }

    /** @return true if this is a BsonRegularExpression, false otherwise */
    public fun isRegularExpression(): Boolean {
        return this is BsonRegularExpression
    }

    /** @return true if this is a BsonJavaScript, false otherwise */
    public fun isJavaScript(): Boolean {
        return this is BsonJavaScript
    }

    /** @return true if this is a BsonJavaScriptWithScope, false otherwise */
    public fun isJavaScriptWithScope(): Boolean {
        return this is BsonJavaScriptWithScope
    }

    /** @return true if this is a BsonMaxKey, false otherwise */
    public fun isMaxKey(): Boolean {
        return this is BsonMaxKey
    }

    /** @return true if this is a BsonMinKey, false otherwise */
    public fun isMinKey(): Boolean {
        return this is BsonMinKey
    }

    /** @return true if this is a BsonUndefined, false otherwise */
    public fun isUndefined(): Boolean {
        return this is BsonUndefined
    }

    /**
     * Gets this value as a BsonDocument if it is one, otherwise throws exception
     *
     * @return a BsonDocument
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asDocument(): BsonDocument {
        throwIfInvalidType(BsonType.DOCUMENT)
        return this as BsonDocument
    }

    /**
     * Gets this value as a BsonArray if it is one, otherwise throws exception
     *
     * @return a BsonArray
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asArray(): BsonArray {
        throwIfInvalidType(BsonType.ARRAY)
        return this as BsonArray
    }

    /**
     * Gets this value as a BsonString if it is one, otherwise throws exception
     *
     * @return a BsonString
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asString(): BsonString {
        throwIfInvalidType(BsonType.STRING)
        return this as BsonString
    }

    /**
     * Gets this value as a BsonNumber if it is one, otherwise throws exception
     *
     * @return a BsonNumber
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asNumber(): BsonNumber {
        if (bsonType !== BsonType.INT32 && bsonType !== BsonType.INT64 && bsonType !== BsonType.DOUBLE) {
            throw BsonInvalidOperationException(
                "Value expected to be of a numerical BSON type is of unexpected type $bsonType")
        }
        return this as BsonNumber
    }

    /**
     * Gets this value as a BsonInt32 if it is one, otherwise throws exception
     *
     * @return a BsonInt32
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asInt32(): BsonInt32 {
        throwIfInvalidType(BsonType.INT32)
        return this as BsonInt32
    }

    /**
     * Gets this value as a BsonInt64 if it is one, otherwise throws exception
     *
     * @return a BsonInt64
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asInt64(): BsonInt64 {
        throwIfInvalidType(BsonType.INT64)
        return this as BsonInt64
    }

    /**
     * Gets this value as a BsonDecimal128 if it is one, otherwise throws exception
     *
     * @return a BsonDecimal128
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asDecimal128(): BsonDecimal128 {
        throwIfInvalidType(BsonType.DECIMAL128)
        return this as BsonDecimal128
    }

    /**
     * Gets this value as a BsonDouble if it is one, otherwise throws exception
     *
     * @return a BsonDouble
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asDouble(): BsonDouble {
        throwIfInvalidType(BsonType.DOUBLE)
        return this as BsonDouble
    }

    /**
     * Gets this value as a BsonBoolean if it is one, otherwise throws exception
     *
     * @return a BsonBoolean
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asBoolean(): BsonBoolean {
        throwIfInvalidType(BsonType.BOOLEAN)
        return this as BsonBoolean
    }

    /**
     * Gets this value as an BsonObjectId if it is one, otherwise throws exception
     *
     * @return an BsonObjectId
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asObjectId(): BsonObjectId {
        throwIfInvalidType(BsonType.OBJECT_ID)
        return this as BsonObjectId
    }

    /**
     * Gets this value as a BsonDbPointer if it is one, otherwise throws exception
     *
     * @return an BsonDbPointer
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asDBPointer(): BsonDbPointer {
        throwIfInvalidType(BsonType.DB_POINTER)
        return this as BsonDbPointer
    }

    /**
     * Gets this value as a BsonTimestamp if it is one, otherwise throws exception
     *
     * @return an BsonTimestamp
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asTimestamp(): BsonTimestamp {
        throwIfInvalidType(BsonType.TIMESTAMP)
        return this as BsonTimestamp
    }

    /**
     * Gets this value as a BsonBinary if it is one, otherwise throws exception
     *
     * @return an BsonBinary
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asBinary(): BsonBinary {
        throwIfInvalidType(BsonType.BINARY)
        return this as BsonBinary
    }

    /**
     * Gets this value as a BsonDateTime if it is one, otherwise throws exception
     *
     * @return an BsonDateTime
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asDateTime(): BsonDateTime {
        throwIfInvalidType(BsonType.DATE_TIME)
        return this as BsonDateTime
    }

    /**
     * Gets this value as a BsonSymbol if it is one, otherwise throws exception
     *
     * @return an BsonSymbol
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asSymbol(): BsonSymbol {
        throwIfInvalidType(BsonType.SYMBOL)
        return this as BsonSymbol
    }

    /**
     * Gets this value as a BsonRegularExpression if it is one, otherwise throws exception
     *
     * @return an BsonRegularExpression
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asRegularExpression(): BsonRegularExpression {
        throwIfInvalidType(BsonType.REGULAR_EXPRESSION)
        return this as BsonRegularExpression
    }

    /**
     * Gets this value as a `BsonJavaScript` if it is one, otherwise throws exception
     *
     * @return a BsonJavaScript
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asJavaScript(): BsonJavaScript {
        throwIfInvalidType(BsonType.JAVASCRIPT)
        return this as BsonJavaScript
    }

    /**
     * Gets this value as a BsonJavaScriptWithScope if it is one, otherwise throws exception
     *
     * @return a BsonJavaScriptWithScope
     * @throws org.kbson.BsonInvalidOperationException if this value is not of the expected type
     */
    public fun asJavaScriptWithScope(): BsonJavaScriptWithScope {
        throwIfInvalidType(BsonType.JAVASCRIPT_WITH_SCOPE)
        return this as BsonJavaScriptWithScope
    }

    private fun throwIfInvalidType(expectedType: BsonType) {
        if (bsonType !== expectedType) {
            throw BsonInvalidOperationException(
                "Value expected to be of type $expectedType is of unexpected type ${bsonType}")
        }
    }
}
