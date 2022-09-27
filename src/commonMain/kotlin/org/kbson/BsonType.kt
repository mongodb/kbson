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
 * Enumeration of all the BSON types currently supported.
 *
 * @property value the int value of this BSON type.
 */
public enum class BsonType(public val value: UByte) {
    /** Not a real BSON type. Used to signal the end of a document. */
    END_OF_DOCUMENT(0u), // no values of this type exist it marks the end of a document

    /** A BSON double. */
    DOUBLE(1u),

    /** A BSON string. */
    STRING(2u),

    /** A BSON document. */
    DOCUMENT(3u),

    /** A BSON array. */
    ARRAY(4u),

    /** BSON binary data. */
    BINARY(5u),

    /** A BSON undefined value. */
    UNDEFINED(6u),

    /** A BSON ObjectId. */
    OBJECT_ID(7u),

    /** A BSON bool. */
    BOOLEAN(8u),

    /** A BSON DateTime. */
    DATE_TIME(9u),

    /** A BSON null value. */
    NULL(10u),

    /** A BSON regular expression. */
    REGULAR_EXPRESSION(11u),

    /** A BSON regular expression. */
    DB_POINTER(12u),

    /** BSON JavaScript code. */
    JAVASCRIPT(13u),

    /** A BSON symbol. */
    SYMBOL(14u),

    /** BSON JavaScript code with a scope (a set of variables with values). */
    JAVASCRIPT_WITH_SCOPE(15u),

    /** A BSON 32-bit integer. */
    INT32(16u),

    /** A BSON timestamp. */
    TIMESTAMP(17u),

    /** A BSON 64-bit integer. */
    INT64(18u),

    /** A BSON Decimal128. */
    DECIMAL128(19u),

    /** A BSON MinKey value. */
    MIN_KEY(255u),

    /** A BSON MaxKey value. */
    MAX_KEY(127u);

    /**
     * Returns whether this type is some sort of containing type, e.g. a document or array.
     *
     * @return true if this is some sort of containing type rather than a primitive value
     */
    public fun isContainer(): Boolean {
        return this == DOCUMENT || this == ARRAY
    }

    internal fun toByte(): Byte {
        return this.value.toByte()
    }
}
