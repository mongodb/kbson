package org.mongodb.kbson

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.mongodb.kbson.serialization.Bson
import kotlin.test.Test
import kotlin.test.assertEquals

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
                val encodedValue = Bson.encodeToString(it)
                val decodedValue = Bson.decodeFromString<BsonValue>(encodedValue)

                assertEquals(it, decodedValue)
            }
    }


    // Simple types

    // classes

    // nullability

    // malformed strings

    private val bsonDataSet: List<BsonValue> = BsonType.values()
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

}