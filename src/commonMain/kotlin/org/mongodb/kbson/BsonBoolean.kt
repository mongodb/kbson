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
package org.mongodb.kbson

import kotlinx.serialization.Serializable
import org.mongodb.kbson.serialization.BsonBooleanSerializer
import kotlin.jvm.JvmStatic

/**
 * A representation of the BSON Boolean type.
 *
 * @constructor constructs a new instance with the given value
 * @property value the boolean value
 */
@Serializable(with = BsonBooleanSerializer::class)
public class BsonBoolean(public val value: Boolean) : BsonValue(), Comparable<BsonBoolean> {

    override val bsonType: BsonType
        get() = BsonType.BOOLEAN

    override fun compareTo(other: BsonBoolean): Int {
        return value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonBoolean

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return if (value) 1 else 0
    }

    override fun toString(): String {
        return "BsonBoolean(value=$value)"
    }

    public companion object {
        /** The true value. */
        @JvmStatic public val TRUE: BsonBoolean = BsonBoolean(true)

        /** The false value. */
        @JvmStatic public val FALSE: BsonBoolean = BsonBoolean(false)

        /**
         * Returns a `BsonBoolean` instance representing the specified `boolean` value.
         *
         * @param value a boolean value.
         * @return @link if `value` is true, [BsonBoolean.FALSE] if `value` is false
         */
        @JvmStatic
        public fun valueOf(value: Boolean): BsonBoolean {
            return if (value) TRUE else FALSE
        }
    }
}
