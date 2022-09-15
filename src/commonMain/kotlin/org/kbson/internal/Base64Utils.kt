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

import kotlin.experimental.and

/** Base64 utils to convert to and from a Base64 encoded string and ByteArray */
@Suppress("MagicNumber")
public object Base64Utils {
    private const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private const val BASE64_MASK: Byte = 0x3f
    private const val BASE64_PAD = '='

    private val BASE64_INVERSE_ALPHABET = IntArray(256) { BASE64_ALPHABET.indexOf(it.toChar()) }

    /**
     * Encodes a ByteArray into a Base64 string
     *
     * @param byteArray the input ByteArray
     * @return the Base64 string representation
     * @see [toByteArray] to decode the string back to a byte array
     */
    public fun toBase64String(byteArray: ByteArray): String {
        val output = mutableListOf<Byte>()
        var padding = 0
        var position = 0
        while (position < byteArray.size) {
            var b = byteArray[position].toInt() and 0xFF shl 16 and 0xFFFFFF
            if (position + 1 < byteArray.size) b = b or (byteArray[position + 1].toInt() and 0xFF shl 8) else padding++
            if (position + 2 < byteArray.size) b = b or (byteArray[position + 2].toInt() and 0xFF) else padding++

            val remaining = 4 - padding
            repeat(remaining) {
                val c = b and 0xFC0000 shr 18
                output.add(c.toBase64())
                b = b shl 6
            }
            position += 3
        }
        repeat(padding) { output.add(BASE64_PAD.code.toByte()) }
        return output.toByteArray().decodeToString()
    }

    /**
     * Decodes a Base64 String into a ByteArray
     *
     * @param base64 the Base64 String
     * @return the byteArray
     */
    public fun toByteArray(base64: String): ByteArray {
        val byteArray = base64.trimEnd(BASE64_PAD).encodeToByteArray()
        val bytes = ArrayList<Byte>()
        byteArray.toList().chunked(4).forEach { data ->
            val chunk =
                data.foldIndexed(0) { index, result, current ->
                    result or (current.fromBase64().toInt() shl ((3 - index) * 6))
                }

            var count = data.size - 1
            var b = chunk
            while (count > 0) {
                val c = b and 0xFF0000 shr 16
                bytes.add(c.toByte())
                b = b shl 8
                count--
            }
        }
        return bytes.toByteArray()
    }

    private fun Int.toBase64(): Byte = BASE64_ALPHABET[this].code.toByte()

    private fun Byte.fromBase64(): Byte = BASE64_INVERSE_ALPHABET[toInt() and 0xff].toByte() and BASE64_MASK
}
