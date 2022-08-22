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

/** A representation the BSON timestamp type. */
class BsonTimestamp(val value: Long = 0) : BsonValue(), Comparable<BsonTimestamp> {

    /**
     * Construct a new instance for the given time and increment.
     *
     * @param seconds the number of seconds since the epoch
     * @param increment the increment.
     */
    constructor(
        seconds: Int,
        increment: Int
    ) : this((seconds.toLong() shl 32) or (increment.toLong() and 0xFFFFFFFFL))

    override fun getBsonType(): BsonType = BsonType.TIMESTAMP

    /**
     * Gets the time in seconds since epoch.
     *
     * @return an int representing time in seconds since epoch
     */
    fun getTime(): Int = (value shr 32).toInt()

    /**
     * Gets the increment value.
     *
     * @return an incrementing ordinal for operations within a given second
     */
    fun getInc(): Int = value.toInt()

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
