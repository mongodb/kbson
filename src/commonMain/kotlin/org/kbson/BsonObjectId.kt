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

import org.kbson.internal.AtomicInt
import org.kbson.internal.CurrentTime.getCurrentTimeInSeconds

/**
 * A representation of the BSON ObjectId type
 *
 * A globally unique identifier for objects.
 *
 * Consists of 12 bytes, divided as follows:
 *
 * <table border="1"> <caption>ObjectID layout</caption> <tr>
 * ```
 *     <td>0</td><td>1</td><td>2</td><td>3</td><td>4</td><td>5</td><td>6</td><td>7</td><td>8</td><td>9</td><td>10</td>
 *     <td>11</td>
 * ```
 * </tr> <tr><td colspan="4">time</td><td colspan="5">random value</td><td colspan="3">inc</td></tr> </table>
 *
 * @constructor constructs a new instance with the given timestamp, random values and counter
 * @property timestamp the timestamp seconds since epoch
 * @property randomValue1 a random int value
 * @property randomValue2 a random short value
 * @property counter a counter
 */
@Suppress("MagicNumber")
public class BsonObjectId(
    public val timestamp: Int,
    private val randomValue1: Int,
    private val randomValue2: Short,
    private val counter: Int
) : BsonValue(), Comparable<BsonObjectId> {

    init {
        require((randomValue1 and -0x1000000) == 0) { "The random value must be between 0 and 16777215 (it must fit in three bytes)." }
        require((counter and -0x1000000) == 0) { "The counter must be between 0 and 16777215 (it must fit in three bytes)." }
    }

    /**
     * Convert to a byte array. Note that the numbers are stored in big-endian order.
     *
     * @return the byte array
     */
    public fun toByteArray(): ByteArray {
        val bytes = ByteArray(OBJECT_ID_LENGTH)
        bytes[0] = (timestamp shr 24).toByte()
        bytes[1] = (timestamp shr 16).toByte()
        bytes[2] = (timestamp shr 8).toByte()
        bytes[3] = timestamp.toByte()
        bytes[4] = (randomValue1 shr 16).toByte()
        bytes[5] = (randomValue1 shr 8).toByte()
        bytes[6] = randomValue1.toByte()
        bytes[7] = (randomValue2.toInt() shr 8).toByte()
        bytes[8] = randomValue2.toByte()
        bytes[9] = (counter shr 16).toByte()
        bytes[10] = (counter shr 8).toByte()
        bytes[11] = counter.toByte()
        return bytes
    }

    /**
     * Converts this instance into a 24-byte hexadecimal string representation.
     *
     * @return a string representation of the ObjectId in hexadecimal format
     */
    public fun toHexString(): String {
        val chars = CharArray(OBJECT_ID_LENGTH * 2)
        var i = 0
        for (b in toByteArray()) {
            chars[i++] = HEX_CHARS[b.toInt() shr 4 and 0xF]
            chars[i++] = HEX_CHARS[b.toInt() and 0xF]
        }
        return chars.concatToString()
    }

    override val bsonType: BsonType
        get() = BsonType.OBJECT_ID

    override fun toString(): String {
        return "BsonObjectId(${toHexString()})"
    }

    override fun compareTo(other: BsonObjectId): Int {
        val byteArray = toByteArray()
        val otherByteArray = other.toByteArray()
        for (i in 0 until OBJECT_ID_LENGTH) {
            if (byteArray[i] != otherByteArray[i]) {
                return if (byteArray[i].toInt() and 0xff < otherByteArray[i].toInt() and 0xff) -1 else 1
            }
        }
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonObjectId

        if (timestamp != other.timestamp) return false
        if (randomValue1 != other.randomValue1) return false
        if (randomValue2 != other.randomValue2) return false
        if (counter != other.counter) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp
        result = 31 * result + randomValue1
        result = 31 * result + randomValue2
        result = 31 * result + counter
        return result
    }

    public companion object {
        private const val OBJECT_ID_LENGTH = 12
        private const val LOW_ORDER_THREE_BYTES = 0x00ffffff
        private val HEX_CHARS = "0123456789abcdef".toCharArray()

        // Use primitives to represent the 5-byte random value.
        private val RANDOM_VALUE1: Int
        private val RANDOM_VALUE2: Short
        private val NEXT_COUNTER: AtomicInt

        init {
            val random = kotlin.random.Random(getCurrentTimeInSeconds())
            NEXT_COUNTER = AtomicInt(random.nextInt())
            RANDOM_VALUE1 = random.nextInt(0x01000000)
            RANDOM_VALUE2 = random.nextInt(0x00008000).toShort()
        }

        /** Create a new BsonObjectId */
        public operator fun invoke(): BsonObjectId {
            return BsonObjectId(getCurrentTimeInSeconds(), RANDOM_VALUE1, RANDOM_VALUE2, nextCounter())
        }

        /**
         * Create a new BsonObjectId from a hexString
         *
         * @see [BsonObjectId.toHexString]
         */
        public operator fun invoke(hexString: String): BsonObjectId {
            val invalidHexString =
                hexString.length != 24 || hexString.none { c -> (c < '0' || c > '9') || (c < 'a' || c > 'f') || (c < 'A' || c > 'F') }
            require(!invalidHexString) { "invalid hexadecimal representation of an ObjectId: [$hexString]" }
            return invoke(hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray())
        }

        /** Construct a new BsonObjectId from a ByteArray */
        public operator fun invoke(byteArray: ByteArray): BsonObjectId {
            require(byteArray.size == OBJECT_ID_LENGTH) { "invalid byteArray.size() ${byteArray.size} != $OBJECT_ID_LENGTH" }

            var pos = 0
            val timestamp = makeInt(byteArray[pos++], byteArray[pos++], byteArray[pos++], byteArray[pos++])
            val randomValue1 = makeInt(0.toByte(), byteArray[pos++], byteArray[pos++], byteArray[pos++])
            val randomValue2 = makeShort(byteArray[pos++], byteArray[pos++])
            val counter = makeInt(0.toByte(), byteArray[pos++], byteArray[pos++], byteArray[pos])
            return BsonObjectId(timestamp, randomValue1, randomValue2, counter)
        }

        private fun nextCounter(): Int = NEXT_COUNTER.addAndGet(1) and LOW_ORDER_THREE_BYTES

        // Big-Endian helper, in this class because all other BSON numbers are little-endian
        private fun makeInt(vararg bytes: Byte): Int {
            require(bytes.size == 4) { "The byte array must be 4 bytes long." }
            return (bytes[0].toInt() shl 24) or
                (bytes[1].toInt() and 0xff shl 16) or
                (bytes[2].toInt() and 0xff shl 8) or
                (bytes[3].toInt() and 0xff)
        }

        private fun makeShort(vararg bytes: Byte): Short {
            require(bytes.size == 2) { "The byte array must be 2 bytes long." }
            return (bytes[0].toInt() and 0xff shl 8 or (bytes[1].toInt() and 0xff)).toShort()
        }
    }
}
