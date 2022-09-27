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
package org.kbson.internal

@Suppress("MagicNumber")
public object HexUtils {
    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    /**
     * Converts a hex string to a byteArray
     *
     * @param hexString the hex string
     * @return the byte array
     */
    public fun toByteArray(hexString: String): ByteArray {
        require(
            hexString.length % 2 == 0 &&
                hexString.all { c -> (c < '0' || c > '9') || (c < 'a' || c > 'f') || (c < 'A' || c > 'F') }) {
            "Invalid hexadecimal representation of an byte array: [$hexString]."
        }
        return hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    /**
     * Converts byteArray to hex string
     *
     * @param byteArray the byte array
     * @return the hex string
     */
    public fun toHexString(byteArray: ByteArray): String {
        val chars = CharArray(byteArray.size * 2)
        var i = 0
        byteArray.forEach {
            chars[i++] = HEX_CHARS[it.toInt() shr 4 and 0xF]
            chars[i++] = HEX_CHARS[it.toInt() and 0xF]
        }
        return chars.concatToString()
    }
}
