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

/** Hex utils to convert to and from a hex encoded string and ByteArray */
@Suppress("MagicNumber")
internal object HexUtils {
    /**
     * Converts byteArray to hex string
     *
     * @param byteArray the byte array
     * @return the hex string
     */
    fun toHexString(byteArray: ByteArray): String {
        return byteArray.joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0').uppercase() }
    }

    /**
     * Converts a hex string to a byteArray
     *
     * @param hexString the hex string
     * @return the byte array
     */
    fun toByteArray(hexString: String): ByteArray {
        require(
            hexString.length % 2 == 0 &&
                hexString.all { c -> (c < '0' || c > '9') || (c < 'a' || c > 'f') || (c < 'A' || c > 'F') }) {
            "Invalid hexadecimal representation of an byte array: [$hexString]."
        }
        return hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
