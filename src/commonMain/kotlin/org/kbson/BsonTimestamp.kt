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
 * A representation the BSON timestamp type.
 *
 * Note: BSON has a special timestamp type for internal MongoDB use and is not associated with the regular
 * [BsonDateTime]. This internal timestamp type is a 64 bit value where:
 * - the most significant 32 bits are a time_t value (seconds since the Unix epoch)
 * - the least significant 32 bits are an incrementing ordinal for operations within a given second.
 *
 * @constructor construct a new instance
 * @property value the timestamp
 */
@Suppress("MagicNumber")
public class BsonTimestamp(public val value: Long = 0) : BsonValue(), Comparable<BsonTimestamp> {

    /**
     * Construct a new instance for the given time and increment.
     *
     * @param seconds the number of seconds since the epoch
     * @param increment the incrementing ordinal for operations within a given second.
     */
    public constructor(
        seconds: Int,
        increment: Int
    ) : this((seconds.toLong() shl 32) or (increment.toLong() and 0xFFFFFFFFL))

    /** Gets the time in seconds since epoch. */
    public val time: Int
        get() = (value shr 32).toInt()

    /** Gets the incrementing ordinal for operations within a given second */
    public val inc: Int
        get() = value.toInt()

    override val bsonType: BsonType
        get() = BsonType.TIMESTAMP

    override fun compareTo(other: BsonTimestamp): Int {
        return value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonTimestamp

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "BsonTimestamp(value=$value)"
    }
}
