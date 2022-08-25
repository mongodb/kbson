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
 * The Binary subtype
 *
 * @property value they byte representation of this binary subtype
 */
public enum class BsonBinarySubType(public val value: Byte) {
    /** Binary data. */
    BINARY(0x00.toByte()),

    /** A function. */
    FUNCTION(0x01.toByte()),

    /** Obsolete binary data subtype (use Binary instead). */
    OLD_BINARY(0x02.toByte()),

    /** A UUID in a driver dependent legacy byte order. */
    UUID_LEGACY(0x03.toByte()),

    /** A UUID in standard network byte order. */
    UUID_STANDARD(0x04.toByte()),

    /** An MD5 hash. */
    MD5(0x05.toByte()),

    /** Encrypted data. */
    ENCRYPTED(0x06.toByte()),

    /** Columnar data */
    COLUMN(0x07.toByte()),

    /** User defined binary data. */
    USER_DEFINED(0x80.toByte())
}
