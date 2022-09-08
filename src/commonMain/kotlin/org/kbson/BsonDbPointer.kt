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
 * A representation of the BSON DBPointer type.
 *
 * Note: It's deprecated in BSON Specification and present here only for compatibility reasons.
 *
 * @constructor constructs a new instance with the given namespace and id
 * @property namespace the namespace
 * @property id the id
 */
public class BsonDbPointer(public val namespace: String, public val id: BsonObjectId) :
    BsonValue() {

    override val bsonType: BsonType
        get() = BsonType.DB_POINTER

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonDbPointer

        if (namespace != other.namespace) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = namespace.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    override fun toString(): String {
        return "BsonDbPointer(namespace='$namespace', id=$id)"
    }
}
