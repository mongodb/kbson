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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BsonDocumentTest {

    private val bsonNull = BsonNull
    private val bsonInt32 = BsonInt32(42)
    private val bsonInt64 = BsonInt64(52L)
    private val bsonDecimal128 = BsonDecimal128(1, 0)
    private val bsonBoolean = BsonBoolean(true)
    private val bsonDateTime = BsonDateTime()
    private val bsonDouble = BsonDouble(62.0)
    private val bsonString = BsonString("the fox ...")
    private val bsonMinKey = BsonMinKey
    private val bsonMaxKey = BsonMaxKey
    private val bsonJavaScript = BsonJavaScript("int i = 0;")
    private val bsonObjectId = BsonObjectId()
    private val bsonJavaScriptWithScope =
        BsonJavaScriptWithScope("int x = y", BsonDocument("y", BsonInt32(1)))
    private val bsonRegularExpression = BsonRegularExpression("^test.*regex.*xyz$", "i")
    private val bsonSymbol = BsonSymbol("ruby stuff")
    private val bsonTimestamp = BsonTimestamp(0x12345678, 5)
    private val bsonUndefined = BsonUndefined
    private val bsonBinary =
        BsonBinary(
            80.toByte(),
            listOf(5.toByte(), 4.toByte(), 3.toByte(), 2.toByte(), 1.toByte()).toByteArray())
    private val bsonArray =
        BsonArray(
            listOf(
                BsonInt32(1),
                BsonInt64(2L),
                BsonBoolean(true),
                BsonArray(listOf(BsonInt32(1), BsonInt32(2), BsonInt32(3))),
                BsonDocument("a", BsonInt64(2L))))
    private val bsonDocument = BsonDocument("a", BsonInt32(1))

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { bsonDocument.isDocument() }
        assertEquals(BsonType.DOCUMENT, bsonDocument.getBsonType())
    }

    @Test
    fun conversionMethodsShouldBehaveCorrectlyForTheHappyPath() {

        val document =
            BsonDocument(
                Pair("null", bsonNull),
                Pair("int32", bsonInt32),
                Pair("int64", bsonInt64),
                Pair("decimal128", bsonDecimal128),
                Pair("boolean", bsonBoolean),
                Pair("date", bsonDateTime),
                Pair("double", bsonDouble),
                Pair("string", bsonString),
                Pair("minKey", bsonMinKey),
                Pair("maxKey", bsonMaxKey),
                Pair("javaScript", bsonJavaScript),
                Pair("objectId", bsonObjectId),
                Pair("codeWithScope", bsonJavaScriptWithScope),
                Pair("regex", bsonRegularExpression),
                Pair("symbol", bsonSymbol),
                Pair("timestamp", bsonTimestamp),
                Pair("undefined", bsonUndefined),
                Pair("bsonBinary", bsonBinary),
                Pair("array", bsonArray),
                Pair("document", bsonDocument))

        document.isNull("null")
        assertEquals(bsonInt32, document.getInt32("int32"))
        assertEquals(bsonInt64, document.getInt64("int64"))
        assertEquals(bsonDecimal128, document.getDecimal128("decimal128"))
        assertEquals(bsonBoolean, document.getBoolean("boolean"))
        assertEquals(bsonDateTime, document.getDateTime("date"))
        assertEquals(bsonDouble, document.getDouble("double"))
        assertEquals(bsonString, document.getString("string"))
        assertEquals(bsonObjectId, document.getObjectId("objectId"))
        assertEquals(bsonRegularExpression, document.getRegularExpression("regex"))
        assertEquals(bsonBinary, document.getBinary("bsonBinary"))
        assertEquals(bsonTimestamp, document.getTimestamp("timestamp"))
        assertEquals(bsonArray, document.getArray("array"))
        assertEquals(bsonDocument, document.getDocument("document"))
        assertEquals(bsonInt32, document.getNumber("int32"))
        assertEquals(bsonInt64, document.getNumber("int64"))
        assertEquals(bsonDouble, document.getNumber("double"))

        assertEquals(bsonInt32, document.getInt32("int32", BsonInt32(2)))
        assertEquals(bsonInt64, document.getInt64("int64", BsonInt64(4)))
        assertEquals(bsonDecimal128, document.getDecimal128("decimal128", BsonDecimal128(1, 2)))
        assertEquals(bsonDouble, document.getDouble("double", BsonDouble(343.0)))
        assertEquals(bsonBoolean, document.getBoolean("boolean", BsonBoolean(false)))
        assertEquals(bsonDateTime, document.getDateTime("date", BsonDateTime(3453)))
        assertEquals(bsonString, document.getString("string", BsonString("df")))
        assertEquals(bsonObjectId, document.getObjectId("objectId", BsonObjectId()))
        assertEquals(
            bsonRegularExpression,
            document.getRegularExpression("regex", BsonRegularExpression("^foo", "i")))
        assertEquals(
            bsonBinary,
            document.getBinary("bsonBinary", BsonBinary(listOf(5.toByte()).toByteArray())))
        assertEquals(bsonTimestamp, document.getTimestamp("timestamp", BsonTimestamp(343, 23)))
        assertEquals(bsonArray, document.getArray("array", BsonArray()))
        assertEquals(bsonDocument, document.getDocument("document", BsonDocument()))
        assertEquals(bsonInt32, document.getNumber("int32", BsonInt32(2)))
        assertEquals(bsonInt64, document.getNumber("int64", BsonInt32(2)))
        assertEquals(bsonDouble, document.getNumber("double", BsonInt32(2)))

        assertEquals(bsonInt32, document.get("int32")!!.asInt32())
        assertEquals(bsonInt64, document.get("int64")!!.asInt64())
        assertEquals(bsonDecimal128, document.get("decimal128")!!.asDecimal128())
        assertEquals(bsonBoolean, document.get("boolean")!!.asBoolean())
        assertEquals(bsonDateTime, document.get("date")!!.asDateTime())
        assertEquals(bsonDouble, document.get("double")!!.asDouble())
        assertEquals(bsonString, document.get("string")!!.asString())
        assertEquals(bsonObjectId, document.get("objectId")!!.asObjectId())
        assertEquals(bsonTimestamp, document.get("timestamp")!!.asTimestamp())
        assertEquals(bsonBinary, document.get("bsonBinary")!!.asBinary())
        assertEquals(bsonArray, document.get("array")!!.asArray())
        assertEquals(bsonDocument, document.get("document")!!.asDocument())

        assertTrue(document.isInt32("int32"))
        assertTrue(document.isNumber("int32"))
        assertTrue(document.isInt64("int64"))
        assertTrue(document.isDecimal128("decimal128"))
        assertTrue(document.isNumber("int64"))
        assertTrue(document.isBoolean("boolean"))
        assertTrue(document.isDateTime("date"))
        assertTrue(document.isDouble("double"))
        assertTrue(document.isNumber("double"))
        assertTrue(document.isString("string"))
        assertTrue(document.isObjectId("objectId"))
        assertTrue(document.isTimestamp("timestamp"))
        assertTrue(document.isBinary("bsonBinary"))
        assertTrue(document.isArray("array"))
        assertTrue(document.isDocument("document"))
    }

    @Test
    fun isTypeMethodsShouldReturnFalseForMissingKeys() {
        val document = BsonDocument()

        assertFalse(document.isNull("null"))
        assertFalse(document.isNumber("number"))
        assertFalse(document.isInt32("int32"))
        assertFalse(document.isInt64("int64"))
        assertFalse(document.isDecimal128("decimal128"))
        assertFalse(document.isBoolean("boolean"))
        assertFalse(document.isDateTime("date"))
        assertFalse(document.isDouble("double"))
        assertFalse(document.isString("string"))
        assertFalse(document.isObjectId("objectId"))
        assertFalse(document.isTimestamp("timestamp"))
        assertFalse(document.isBinary("bsonBinary"))
        assertFalse(document.isArray("array"))
        assertFalse(document.isDocument("document"))
    }

    @Test
    fun getMethodsShouldReturnDefaultValuesForMissingKeys() {
        val document = BsonDocument()

        assertEquals(bsonNull, document.get("m", bsonNull))
        assertEquals(bsonArray, document.getArray("m", bsonArray))
        assertEquals(bsonBoolean, document.getBoolean("m", bsonBoolean))
        assertEquals(bsonDateTime, document.getDateTime("m", bsonDateTime))
        assertEquals(bsonDocument, document.getDocument("m", bsonDocument))
        assertEquals(bsonDouble, document.getDouble("m", bsonDouble))
        assertEquals(bsonInt32, document.getInt32("m", bsonInt32))
        assertEquals(bsonInt64, document.getInt64("m", bsonInt64))
        assertEquals(bsonDecimal128, document.getDecimal128("m", bsonDecimal128))
        assertEquals(bsonString, document.getString("m", bsonString))
        assertEquals(bsonObjectId, document.getObjectId("m", bsonObjectId))
        assertEquals(bsonString, document.getString("m", bsonString))
        assertEquals(bsonTimestamp, document.getTimestamp("m", bsonTimestamp))
        assertEquals(bsonInt32, document.getNumber("m", bsonInt32))
        assertEquals(
            bsonRegularExpression, document.getRegularExpression("m", bsonRegularExpression))
        assertEquals(bsonBinary, document.getBinary("m", bsonBinary))
    }

    @Test
    fun cloneShouldMakeADeepCopyOfAllMutableBsonValueTypes() {
        val document =
            BsonDocument("d", BsonDocument().append("i2", BsonInt32(1)))
                .append("i", BsonInt32(2))
                .append(
                    "a",
                    BsonArray(
                        listOf(
                            BsonInt32(3),
                            BsonArray(listOf(BsonInt32(11))),
                            BsonDocument("i3", BsonInt32(6)),
                            BsonBinary(listOf(1.toByte(), 2.toByte(), 3.toByte()).toByteArray()),
                            BsonJavaScriptWithScope("code", BsonDocument("a", BsonInt32(4))))))
                .append("b", BsonBinary(listOf(1.toByte(), 2.toByte(), 3.toByte()).toByteArray()))
                .append("js", BsonJavaScriptWithScope("code", BsonDocument("a", BsonInt32(4))))

        val clone = document.clone()

        assertEquals(document, clone)
        assertNotSame(document, clone)

        // Immutable types are the same
        assertSame(document.get("i"), clone.get("i"))
        assertSame(document.get("b"), clone.get("b"))
        assertSame(document.get("js"), clone.get("js"))

        // Mutable types are copies
        assertNotSame(document.get("d"), clone.get("d"))
        assertNotSame(document.get("a"), clone.get("a"))

        // Test deep clone
        val subArray = document.get("a")!!.asArray()
        val clonedArray = clone.get("a")!!.asArray()

        // Immutable types are the same
        assertSame(subArray[0], clonedArray[0])
        assertSame(subArray[3], clonedArray[3])
        assertSame(subArray[4], clonedArray[4])

        // Mutable types are copies
        assertNotSame(subArray[1], clonedArray[1])
        assertNotSame(subArray[2], clonedArray[2])
    }

    @Test
    fun getMethodsShouldThrowIfKeyIsAbsent() {
        val document = BsonDocument()

        assertFailsWith<BsonInvalidOperationException> { document.getInt32("int32") }
        assertFailsWith<BsonInvalidOperationException> { document.getInt64("int64") }
        assertFailsWith<BsonInvalidOperationException> { document.getDecimal128("decimal128") }
        assertFailsWith<BsonInvalidOperationException> { document.getBoolean("boolean") }
        assertFailsWith<BsonInvalidOperationException> { document.getDateTime("date") }
        assertFailsWith<BsonInvalidOperationException> { document.getDouble("double") }
        assertFailsWith<BsonInvalidOperationException> { document.getString("string") }
        assertFailsWith<BsonInvalidOperationException> { document.getObjectId("objectId") }
        assertFailsWith<BsonInvalidOperationException> { document.getRegularExpression("regex") }
        assertFailsWith<BsonInvalidOperationException> { document.getBinary("bsonBinary") }
        assertFailsWith<BsonInvalidOperationException> { document.getTimestamp("timestamp") }
        assertFailsWith<BsonInvalidOperationException> { document.getArray("array") }
        assertFailsWith<BsonInvalidOperationException> { document.getDocument("document") }
        assertFailsWith<BsonInvalidOperationException> { document.getNumber("int32") }
    }

    @Test
    fun shouldGetFirstKey() {
        val document = BsonDocument()

        assertFailsWith<NoSuchElementException> { document.getFirstKey() }

        document.put("i", bsonInt32)
        assertEquals("i", document.getFirstKey())
    }

    @Test
    fun shouldTestEquality() {
        val emptyDocument = BsonDocument()
        assertEquals(emptyDocument, BsonDocument())
        assertEquals(emptyDocument, BsonDocument(0))
        assertEquals(emptyDocument, BsonDocument(mapOf()))
        assertEquals(emptyDocument, BsonDocument(listOf()))
        assertEquals(
            emptyDocument, BsonDocument(*arrayListOf<Pair<String, BsonValue>>().toTypedArray()))

        val expectedDocument =
            BsonDocument("null", bsonNull)
                .append("int32", bsonInt32)
                .append("int64", bsonInt64)
                .append("decimal128", bsonDecimal128)
                .append("boolean", bsonBoolean)
                .append("date", bsonDateTime)
                .append("double", bsonDouble)
                .append("string", bsonString)
                .append("minKey", bsonMinKey)
                .append("maxKey", bsonMaxKey)
                .append("javaScript", bsonJavaScript)
                .append("objectId", bsonObjectId)
                .append("codeWithScope", bsonJavaScriptWithScope)
                .append("regex", bsonRegularExpression)
                .append("symbol", bsonSymbol)
                .append("timestamp", bsonTimestamp)
                .append("undefined", bsonUndefined)
                .append("bsonBinary", bsonBinary)
                .append("array", bsonArray)
                .append("document", bsonDocument)

        val pairs =
            arrayListOf(
                Pair("null", bsonNull),
                Pair("int32", bsonInt32),
                Pair("int64", bsonInt64),
                Pair("decimal128", bsonDecimal128),
                Pair("boolean", bsonBoolean),
                Pair("date", bsonDateTime),
                Pair("double", bsonDouble),
                Pair("string", bsonString),
                Pair("minKey", bsonMinKey),
                Pair("maxKey", bsonMaxKey),
                Pair("javaScript", bsonJavaScript),
                Pair("objectId", bsonObjectId),
                Pair("codeWithScope", bsonJavaScriptWithScope),
                Pair("regex", bsonRegularExpression),
                Pair("symbol", bsonSymbol),
                Pair("timestamp", bsonTimestamp),
                Pair("undefined", bsonUndefined),
                Pair("bsonBinary", bsonBinary),
                Pair("array", bsonArray),
                Pair("document", bsonDocument))

        assertEquals(expectedDocument, BsonDocument(mapOf(*pairs.toTypedArray())))
        assertEquals(expectedDocument, BsonDocument(pairs.map { BsonElement(it.first, it.second) }))
        assertEquals(expectedDocument, BsonDocument(*pairs.toTypedArray()))
    }
}
