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

import kotlin.jvm.JvmStatic
import kotlinx.serialization.Serializable
import org.mongodb.kbson.serialization.BsonNullSerializer

/** A representation of the BSON Null type. */
@Serializable(with = BsonNullSerializer::class)
public object BsonNull : BsonValue() {
    override val bsonType: BsonType
        get() = BsonType.NULL

    override fun toString(): String {
        return "BsonNull()"
    }

    @JvmStatic public val VALUE: BsonNull = BsonNull
}
