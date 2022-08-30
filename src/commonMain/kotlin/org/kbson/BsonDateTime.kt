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

import org.kbson.internal.CurrentTime.getCurrentTimeInMillis

/**
 * A representation of the BSON DateTime type.
 *
 * @constructor constructs a new instance with the given value
 * @property value the time in milliseconds since epoch
 */
public class BsonDateTime(public val value: Long) : BsonValue(), Comparable<BsonDateTime> {

    /** Construct a new instance with 'now' as the current date time */
    public constructor() : this(getCurrentTimeInMillis())

    override val bsonType: BsonType
        get() = BsonType.DATE_TIME

    override fun compareTo(other: BsonDateTime): Int {
        return value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonDateTime

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "BsonDateTime(value=$value)"
    }
}
