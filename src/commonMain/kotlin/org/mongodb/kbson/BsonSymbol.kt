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
import org.mongodb.kbson.serialization.BsonSymbolSerializer

/**
 * A representation of the BSON Symbol type.
 *
 * Note: It's deprecated in BSON Specification and present here only for compatibility reasons.
 *
 * @property value the symbol value
 */
@Serializable(with = BsonSymbolSerializer::class)
public class BsonSymbol(public val value: String) : BsonValue() {

    /** Gets the symbol value */
    public val symbol: String
        get() = value

    override val bsonType: BsonType
        get() = BsonType.SYMBOL

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonSymbol

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "BsonSymbol(value='$value')"
    }
}
