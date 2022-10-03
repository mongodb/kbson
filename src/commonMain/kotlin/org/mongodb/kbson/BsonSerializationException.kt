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
 * An exception indicating a failure to serialize or deserialize a BSON value.
 * @constructor constructs a new instance with the given message
 * @param message the message
 * @param cause the error cause
 */
public class BsonSerializationException(message: String, cause: Throwable? = null) : BsonException(message, cause) {
    override fun toString(): String {
        return "BsonSerializationException(message=$message, cause=${cause?.message})"
    }
}
