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

/**
 * A type-safe container for a BSON document.
 *
 * @constructor constructs the bson document with the initial values, defaults to an empty document
 * @param initial the initial values
 */
@Suppress("TooManyFunctions")
public class BsonDocument(initial: Map<String, BsonValue> = LinkedHashMap()) :
    BsonValue(), MutableMap<String, BsonValue> {
    private val _values: LinkedHashMap<String, BsonValue>

    init {
        _values = if (initial is LinkedHashMap) initial else LinkedHashMap(initial)
    }

    /**
     * Construct an empty document with the specified initial capacity.
     * @param initialCapacity the initial capacity
     */
    public constructor(initialCapacity: Int) : this(LinkedHashMap(initialCapacity))

    /**
     * Construct a new instance with a single key value pair
     * @param key the key
     * @param value the value
     */
    public constructor(key: String, value: BsonValue) : this(linkedMapOf(key to value))

    /**
     * Construct a new instance with the given list [BsonElement]s
     * @param bsonElements the initial list of [BsonElement]s
     */
    public constructor(bsonElements: List<BsonElement>) : this(bsonElements.associate { Pair(it.name, it.value) })

    /**
     * Construct a new instance with the varargs of key value pairs
     * @param pairs the initial pairs of values
     */
    public constructor(vararg pairs: Pair<String, BsonValue>) : this(pairs.toMap())

    override val entries: MutableSet<MutableMap.MutableEntry<String, BsonValue>>
        get() = _values.entries
    override val keys: MutableSet<String>
        get() = _values.keys
    override val size: Int
        get() = _values.size
    override val values: MutableCollection<BsonValue>
        get() = _values.values

    override fun clear() {
        _values.clear()
    }

    override fun containsKey(key: String): Boolean = _values.containsKey(key)

    override fun containsValue(value: BsonValue): Boolean = _values.containsValue(value)

    override fun get(key: String): BsonValue? = _values[key]

    override fun isEmpty(): Boolean = _values.isEmpty()
    override fun remove(key: String): BsonValue? {
        return _values.remove(key)
    }

    override fun putAll(from: Map<out String, BsonValue>) {
        _values.putAll(from)
    }

    override fun put(key: String, value: BsonValue): BsonValue? {
        return _values.put(key, value)
    }

    override val bsonType: BsonType
        get() = BsonType.DOCUMENT

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is BsonDocument) return false
        // Use entries to force other BsonDocument implementations to decode
        return entries == other.entries
    }

    override fun hashCode(): Int {
        return _values.hashCode()
    }

    override fun toString(): String {
        return "BsonDocument($_values)"
    }

    /** Clone the document */
    public fun clone(): BsonDocument {
        val clonedValues = HashMap<String, BsonValue>()
        _values.onEach { entry ->
            if (entry.value.isArray()) {
                clonedValues[entry.key] = entry.value.asArray().clone()
            } else if (entry.value.isDocument()) {
                clonedValues[entry.key] = entry.value.asDocument().clone()
            } else {
                clonedValues[entry.key] = entry.value
            }
        }
        return BsonDocument(clonedValues)
    }

    /**
     * Gets the first key in the document.
     *
     * @return the first key in the document
     * @throws NoSuchElementException if the document is empty
     */
    public fun getFirstKey(): String {
        return keys.iterator().next()
    }

    /**
     * Put the given key and value into this document, and return the document.
     *
     * @param key the key
     * @param value the value
     * @return this
     */
    public fun append(key: String, value: BsonValue): BsonDocument {
        put(key, value)
        return this
    }

    /**
     * Returns true if the value of the key is a BsonNull, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonNull, returns false if the document does not contain the key.
     */
    public fun isNull(key: String): Boolean {
        return get(key)?.isNull() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonDocument, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonDocument, returns false if the document does not contain the key.
     */
    public fun isDocument(key: String): Boolean {
        return get(key)?.isDocument() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonArray, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonArray, returns false if the document does not contain the key.
     */
    public fun isArray(key: String): Boolean {
        return get(key)?.isArray() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonNumber, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonNumber, returns false if the document does not contain the key.
     */
    public fun isNumber(key: String): Boolean {
        return get(key)?.isNumber() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonInt32, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonInt32, returns false if the document does not contain the key.
     */
    public fun isInt32(key: String): Boolean {
        return get(key)?.isInt32() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonInt64, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonInt64, returns false if the document does not contain the key.
     */
    public fun isInt64(key: String): Boolean {
        return get(key)?.isInt64() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonDecimal128, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonDecimal128, returns false if the document does not contain the key.
     */
    public fun isDecimal128(key: String): Boolean {
        return get(key)?.isDecimal128() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonDouble, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonDouble, returns false if the document does not contain the key.
     */
    public fun isDouble(key: String): Boolean {
        return get(key)?.isDouble() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonBoolean, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonBoolean, returns false if the document does not contain the key.
     */
    public fun isBoolean(key: String): Boolean {
        return get(key)?.isBoolean() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonString, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonString, returns false if the document does not contain the key.
     */
    public fun isString(key: String): Boolean {
        return get(key)?.isString() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonDateTime, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonDateTime, returns false if the document does not contain the key.
     */
    public fun isDateTime(key: String): Boolean {
        return get(key)?.isDateTime() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonTimestamp, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonTimestamp, returns false if the document does not contain the key.
     */
    public fun isTimestamp(key: String): Boolean {
        return get(key)?.isTimestamp() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonObjectId, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonObjectId, returns false if the document does not contain the key.
     */
    public fun isObjectId(key: String): Boolean {
        return get(key)?.isObjectId() ?: false
    }

    /**
     * Returns true if the value of the key is a BsonBinary, returns false if the document does not contain the key.
     *
     * @param key the key
     * @return true if the value of the key is a BsonBinary, returns false if the document does not contain the key.
     */
    public fun isBinary(key: String): Boolean {
        return get(key)?.isBinary() ?: false
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonValue
     */
    public operator fun get(key: String, defaultValue: BsonValue): BsonValue {
        val value = get(key)
        return value ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonDocument.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonDocument
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getDocument(key: String, defaultValue: BsonDocument): BsonDocument {
        return get(key)?.asDocument() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonArray.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonArray
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getArray(key: String, defaultValue: BsonArray): BsonArray {
        return get(key)?.asArray() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonNumber.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonNumber
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getNumber(key: String, defaultValue: BsonNumber): BsonNumber {
        return get(key)?.asNumber() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonInt32.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonInt32
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getInt32(key: String, defaultValue: BsonInt32): BsonInt32 {
        return get(key)?.asInt32() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonInt64.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonInt64
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getInt64(key: String, defaultValue: BsonInt64): BsonInt64 {
        return get(key)?.asInt64() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonDecimal128.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonDecimal128
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getDecimal128(key: String, defaultValue: BsonDecimal128): BsonDecimal128 {
        return get(key)?.asDecimal128() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonDouble.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonDouble
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getDouble(key: String, defaultValue: BsonDouble): BsonDouble {
        return get(key)?.asDouble() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonBoolean.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonBoolean
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getBoolean(key: String, defaultValue: BsonBoolean): BsonBoolean {
        return get(key)?.asBoolean() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonString.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonString
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getString(key: String, defaultValue: BsonString): BsonString {
        return get(key)?.asString() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonDateTime.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonDateTime
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getDateTime(key: String, defaultValue: BsonDateTime): BsonDateTime {
        return get(key)?.asDateTime() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonTimestamp.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonTimestamp
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getTimestamp(key: String, defaultValue: BsonTimestamp): BsonTimestamp {
        return get(key)?.asTimestamp() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonObjectId.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonObjectId
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getObjectId(key: String, defaultValue: BsonObjectId): BsonObjectId {
        return get(key)?.asObjectId() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonBinary.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonBinary
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getBinary(key: String, defaultValue: BsonBinary): BsonBinary {
        return get(key)?.asBinary() ?: defaultValue
    }

    /**
     * If the document does not contain the given key, return the given default value. Otherwise, gets the value of the
     * key as a BsonRegularExpression.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value of the key as a BsonRegularExpression
     * @throws org.kbson.BsonInvalidOperationException if the document contains the key but the value is not of the
     * expected type
     */
    public fun getRegularExpression(key: String, defaultValue: BsonRegularExpression): BsonRegularExpression {
        return get(key)?.asRegularExpression() ?: defaultValue
    }

    /**
     * Gets the value of the key if it is a BsonDocument, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonDocument
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not a
     * BsonDocument
     */
    public fun getDocument(key: String): BsonDocument {
        throwIfKeyAbsent(key)
        return get(key)!!.asDocument()
    }

    /**
     * Gets the value of the key if it is a BsonArray, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonArray
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getArray(key: String): BsonArray {
        throwIfKeyAbsent(key)
        return get(key)!!.asArray()
    }

    /**
     * Gets the value of the key if it is a BsonNumber, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonNumber
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getNumber(key: String): BsonNumber {
        throwIfKeyAbsent(key)
        return get(key)!!.asNumber()
    }

    /**
     * Gets the value of the key if it is a BsonInt32, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonInt32
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getInt32(key: String): BsonInt32 {
        throwIfKeyAbsent(key)
        return get(key)!!.asInt32()
    }

    /**
     * Gets the value of the key if it is a BsonInt64, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonInt64
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getInt64(key: String): BsonInt64 {
        throwIfKeyAbsent(key)
        return get(key)!!.asInt64()
    }

    /**
     * Gets the value of the key if it is a BsonDecimal128, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonDecimal128
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getDecimal128(key: String): BsonDecimal128 {
        throwIfKeyAbsent(key)
        return get(key)!!.asDecimal128()
    }

    /**
     * Gets the value of the key if it is a BsonDouble, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonDouble
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getDouble(key: String): BsonDouble {
        throwIfKeyAbsent(key)
        return get(key)!!.asDouble()
    }

    /**
     * Gets the value of the key if it is a BsonBoolean, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonBoolean
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getBoolean(key: String): BsonBoolean {
        throwIfKeyAbsent(key)
        return get(key)!!.asBoolean()
    }

    /**
     * Gets the value of the key if it is a BsonString, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonString
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getString(key: String): BsonString {
        throwIfKeyAbsent(key)
        return get(key)!!.asString()
    }

    /**
     * Gets the value of the key if it is a BsonDateTime, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonDateTime
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getDateTime(key: String): BsonDateTime {
        throwIfKeyAbsent(key)
        return get(key)!!.asDateTime()
    }

    /**
     * Gets the value of the key if it is a BsonTimestamp, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonTimestamp
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getTimestamp(key: String): BsonTimestamp {
        throwIfKeyAbsent(key)
        return get(key)!!.asTimestamp()
    }

    /**
     * Gets the value of the key if it is a BsonObjectId, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonObjectId
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getObjectId(key: String): BsonObjectId {
        throwIfKeyAbsent(key)
        return get(key)!!.asObjectId()
    }

    /**
     * Gets the value of the key if it is a BsonRegularExpression, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonRegularExpression
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getRegularExpression(key: String): BsonRegularExpression {
        throwIfKeyAbsent(key)
        return get(key)!!.asRegularExpression()
    }

    /**
     * Gets the value of the key if it is a BsonBinary, or throws if not.
     *
     * @param key the key
     * @return the value of the key as a BsonBinary
     * @throws org.kbson.BsonInvalidOperationException if the document does not contain the key or the value is not of
     * the expected type
     */
    public fun getBinary(key: String): BsonBinary {
        throwIfKeyAbsent(key)
        return get(key)!!.asBinary()
    }

    private fun throwIfKeyAbsent(key: Any) {
        if (!containsKey(key)) {
            throw BsonInvalidOperationException("Document does not contain key: '$key'")
        }
    }
}
