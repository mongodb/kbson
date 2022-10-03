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

/**
 * A representation of the BSON JavaScript with scope type.
 *
 * @constructor constructs a new instance with the given code and scope
 * @property code the javascript code as a string
 * @property scope the javascript scope
 */
public class BsonJavaScriptWithScope(public val code: String, public val scope: BsonDocument) : BsonValue() {
    override val bsonType: BsonType
        get() = BsonType.JAVASCRIPT_WITH_SCOPE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonJavaScriptWithScope

        if (code != other.code) return false
        if (scope != other.scope) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + scope.hashCode()
        return result
    }

    override fun toString(): String {
        return "BsonJavaScriptWithScope(code='$code', scope=$scope)"
    }
}
