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

/** A representation of the BSON regular expression type */
class BsonRegularExpression(val pattern: String, options: String) : BsonValue() {

    /**
     * Gets the options for the regular expression
     *
     * @return the options.
     */
    val options: String

    init {
        this.options = options.toCharArray().apply { sort() }.joinToString("")
    }

    /** Construct a new instance without any options */
    constructor(pattern: String) : this(pattern, "")

    override fun getBsonType(): BsonType = BsonType.REGULAR_EXPRESSION

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonRegularExpression

        if (pattern != other.pattern) return false
        if (options != other.options) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pattern.hashCode()
        result = 31 * result + options.hashCode()
        return result
    }

    override fun toString(): String {
        return "BsonRegularExpression(pattern='$pattern', options='$options')"
    }
}
