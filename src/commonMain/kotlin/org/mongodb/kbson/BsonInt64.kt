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
import org.mongodb.kbson.serialization.BsonInt64Serializer

/**
 * A representation of the BSON Int64 type.
 *
 * @constructor constructs a new instance with the given value
 * @property value the value
 */
@Serializable(with = BsonInt64Serializer::class)
public class BsonInt64(public val value: Long) : BsonNumber(value), Comparable<BsonInt64> {

    override val bsonType: BsonType
        get() = BsonType.INT64

    override fun compareTo(other: BsonInt64): Int {
        return value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonInt64

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "BsonInt64(value=$value)"
    }
}
