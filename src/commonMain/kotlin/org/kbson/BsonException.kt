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
 * A general runtime exception raised in BSON processing.
 *
 * @constructor constructs a new instance with the given message, cause and errorCode
 * @param message the error message
 * @param cause the error cause
 * @param errorCode the error code
 */
public open class BsonException(
    message: String? = null,
    cause: Throwable? = null,
    private val errorCode: Int? = null
) : RuntimeException(message, cause) {

    /**
     * Returns if the error code is set (i.e., not null).
     *
     * @return true if the error code is not null.
     */
    public fun hasErrorCode(): Boolean {
        return errorCode != null
    }

    /**
     * Return the error code if set
     *
     * @return the error code
     */
    public fun getErrorCode(): Int? {
        return errorCode
    }

    override fun toString(): String {
        return "BsonException(message=$message, cause=$cause, errorCode=$errorCode)"
    }
}
