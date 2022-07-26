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
package org.mongodb.kbson.internal

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.mongodb.kbson.BsonInvalidOperationException
import org.mongodb.kbson.BsonSerializationException

@OptIn(ExperimentalContracts::class)
internal inline fun validateSerialization(value: Boolean, lazyMessage: () -> Any) {
    contract { returns() implies value }
    if (!value) {
        val message = lazyMessage()
        throw BsonSerializationException(message.toString())
    }
}

@OptIn(ExperimentalContracts::class)
internal inline fun validateOperation(value: Boolean, lazyMessage: () -> Any) {
    contract { returns() implies value }
    if (!value) {
        val message = lazyMessage()
        throw BsonInvalidOperationException(message.toString())
    }
}
