package org.mongodb.kbson

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mongodb.kbson.serialization.Bson
import org.mongodb.kbson.serialization.decodeFromBsonValue
import org.mongodb.kbson.serialization.encodeToBsonValue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BsonEncoding {
    @Test
    fun roundtripBsonTypes() {
        listOf(
            BsonArray(bsonDataSet.toList()),
            BsonDocument(
                bsonDataSet
                    .mapIndexed { index, bsonValue ->
                        "$index" to bsonValue
                    }
                    .toMap()
            )
        ) + bsonDataSet
            .forEach {
                assertRoundTrip(it)
            }
    }

    @Test
    fun roundtripKotlinTypes() {
        listOf(true, false).assertRoundTrip()
        listOf(Short.MAX_VALUE, Short.MIN_VALUE).assertRoundTrip()
        listOf(Int.MAX_VALUE, Int.MIN_VALUE).assertRoundTrip()
        listOf(Long.MAX_VALUE, Long.MIN_VALUE).assertRoundTrip()
        listOf(Float.MAX_VALUE, Float.MIN_VALUE).assertRoundTrip()
        listOf(Double.MAX_VALUE, Double.MIN_VALUE).assertRoundTrip()
        listOf("hello world", "", "🚀💎").assertRoundTrip()
        listOf('4', 'c', '[').assertRoundTrip()
        listOf(byteArrayOf(10, 0, 10), byteArrayOf(), byteArrayOf(0, 0)).assertRoundTrip()
    }

    @Test
    fun testCollections() {
        listOf(
            listOf<String>("hello world")
        ).assertRoundTrip()

        listOf(
            listOf<List<String>>(listOf("hello world"))
        ).assertRoundTrip()

        listOf(
            setOf<String>("hello world")
        ).assertRoundTrip()

        listOf(
            setOf(listOf<String>("hello world"))
        ).assertRoundTrip()

        listOf(
            mapOf<String, String>("hello" to "world")
        ).assertRoundTrip()

        listOf(
            mapOf<String, Map<String, String>>("hello" to mapOf("hello" to "world"))
        ).assertRoundTrip()
    }

    @Test
    fun roundtripClass() {
        val value = AllTypes().apply {
            allTypes = AllTypes()
        }

        assertRoundTrip(value)
    }

    @Test
    fun roundtripNullValue() {
        assertRoundTrip(null as String?)
        assertRoundTrip(null as AllTypes?)

        assertRoundTrip(BsonNull)

        assertRoundTrip(listOf<String?>(null, null))
        assertRoundTrip(listOf<BsonValue?>(null, null))
        assertRoundTrip(listOf<BsonValue>(BsonNull, BsonNull))
    }

    @Test
    fun decodeMalformedEjsonString() {
        assertFailsWith<SerializationException> {
            Bson.decodeFromString<BsonArray?>("💥&&][💎")
        }
    }

    @Serializable
    enum class SerializableEnum(val value: Int) {
        A(4),
        B(5),
    }

    @Test
    fun roundtripEnum() {
        assertRoundTrip(SerializableEnum.A)
        assertRoundTrip(SerializableEnum.B)
    }

    @Serializable
    object SerializableObject {
        val name = ""
        var surname = ""
    }

    @Test
    fun roundtripObject() {
        assertRoundTrip(SerializableObject)
    }

    @Serializable
    class AllTypes {
        val boolean = true
        val short = Short.MAX_VALUE
        val int = Int.MAX_VALUE
        val long = Long.MAX_VALUE
        val float = Float.MAX_VALUE
        val double = Double.MAX_VALUE
        val char = '4'
        val string = "hello world"
        val bsonValue = BsonString("hello world")
        val byteArray = byteArrayOf(10, 0, 10)
        val stringList = listOf("hello world")
        val stringMap = mapOf("hello" to "world")
        var allTypes: AllTypes? = null
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as AllTypes

            if (boolean != other.boolean) return false
            if (short != other.short) return false
            if (int != other.int) return false
            if (long != other.long) return false
            if (float != other.float) return false
            if (double != other.double) return false
            if (char != other.char) return false
            if (string != other.string) return false
            if (bsonValue != other.bsonValue) return false
            if (!byteArray.contentEquals(other.byteArray)) return false
            if (stringList != other.stringList) return false
            if (stringMap != other.stringMap) return false
            if (allTypes != other.allTypes) return false

            return true
        }

        override fun hashCode(): Int {
            var result = boolean.hashCode()
            result = 31 * result + short
            result = 31 * result + int
            result = 31 * result + long.hashCode()
            result = 31 * result + float.hashCode()
            result = 31 * result + double.hashCode()
            result = 31 * result + char.hashCode()
            result = 31 * result + string.hashCode()
            result = 31 * result + bsonValue.hashCode()
            result = 31 * result + byteArray.contentHashCode()
            result = 31 * result + stringList.hashCode()
            result = 31 * result + stringMap.hashCode()
            result = 31 * result + (allTypes?.hashCode() ?: 0)
            return result
        }
    }

    private val bsonDataSet: List<BsonValue> = BsonType.values()
        .filter {
            it != BsonType.NULL // Tested separately
        }
        .flatMap {
            when (it) {
                BsonType.DOUBLE -> listOf(BsonDouble(10.0))
                BsonType.STRING -> listOf(BsonString("hello world"))
                BsonType.DOCUMENT -> listOf(BsonDocument())
                BsonType.ARRAY -> listOf(BsonArray())
                BsonType.BINARY -> listOf(BsonBinary(byteArrayOf(10, 20, 30)))
                BsonType.UNDEFINED -> listOf(BsonUndefined)
                BsonType.OBJECT_ID -> listOf(BsonObjectId())
                BsonType.BOOLEAN -> listOf(BsonBoolean.TRUE_VALUE, BsonBoolean.FALSE_VALUE)
                BsonType.DATE_TIME -> listOf(BsonDateTime())
                BsonType.NULL -> listOf(BsonNull)
                BsonType.REGULAR_EXPRESSION -> listOf(BsonRegularExpression(""))
                BsonType.DB_POINTER -> listOf(BsonDBPointer("test", BsonObjectId()))
                BsonType.JAVASCRIPT -> listOf(
                    BsonJavaScript(
                        """
                    alert('Hello, world');
                """.trimIndent()
                    )
                )
                BsonType.SYMBOL -> listOf(BsonSymbol("d"))
                BsonType.JAVASCRIPT_WITH_SCOPE -> listOf(
                    BsonJavaScriptWithScope(
                        """
                    alert('Hello, world');
                """.trimIndent(),
                        BsonDocument()
                    )
                )
                BsonType.INT32 -> listOf(BsonInt32(Int.MIN_VALUE), BsonInt32(Int.MAX_VALUE))
                BsonType.TIMESTAMP -> listOf(BsonTimestamp())
                BsonType.INT64 -> listOf(BsonInt64(Long.MIN_VALUE), BsonInt64(Long.MAX_VALUE))
                BsonType.DECIMAL128 -> listOf(BsonDecimal128("100"))
                BsonType.MIN_KEY -> listOf(BsonMinKey)
                BsonType.MAX_KEY -> listOf(BsonMaxKey)
                BsonType.END_OF_DOCUMENT -> listOf()
            }
        }

    private inline fun <reified T> Iterable<T>.assertRoundTrip() {
        for (value in this) assertRoundTrip(value)
    }

    private inline fun <reified T> assertRoundTrip(value: T) {
        val encodedValue = Bson.encodeToString(value)
        val decodedValue: T = Bson.decodeFromString(encodedValue)
        when (value) {
            is ByteArray -> assertContentEquals(value as ByteArray, decodedValue as ByteArray)
            else -> assertEquals(value, decodedValue)
        }
    }
}