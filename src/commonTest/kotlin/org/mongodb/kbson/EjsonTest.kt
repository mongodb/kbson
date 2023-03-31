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
@file:OptIn(ExperimentalKSerializerApi::class)

package org.mongodb.kbson

import assertFailsWithMessage
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import org.mongodb.kbson.serialization.Ejson
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class EjsonTest {
    @Test
    fun bsonTypes() {
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
    fun bsonTypes_invalidType() {
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
            .forEach { bsonValue ->
                assertDecodingFailsWithInvalidType(bsonValue)
            }
    }

    @Test
    fun kotlinTypes() {
        // Different list are required to retain type argument information.
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
    fun kotlinTypes_invalidType() {
        // Different list are required to retain type argument information.
        assertDecodingFailsWithInvalidType(true)
        assertDecodingFailsWithInvalidType(Short.MAX_VALUE)
        assertDecodingFailsWithInvalidType(Int.MAX_VALUE)
        assertDecodingFailsWithInvalidType(Long.MAX_VALUE)
        assertDecodingFailsWithInvalidType(Float.MAX_VALUE)
        assertDecodingFailsWithInvalidType(Double.MAX_VALUE)
        assertDecodingFailsWithInvalidType("hello world")
        assertDecodingFailsWithInvalidType('4')
        assertDecodingFailsWithInvalidType(byteArrayOf(10, 0, 10))
    }

    @Test
    fun collections() {
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
            setOf(setOf<String>("hello world"))
        ).assertRoundTrip()

        listOf(
            mapOf<String, String>("hello" to "world")
        ).assertRoundTrip()

        listOf(
            mapOf<String, Map<String, String>>("hello" to mapOf("hello" to "world"))
        ).assertRoundTrip()
    }

    @Test
    fun collections_invalidType() {
        assertDecodingFailsWithInvalidType(listOf<String>("hello world"))
        assertDecodingFailsWithInvalidType(setOf<String>("hello world"))
        assertDecodingFailsWithInvalidType(mapOf<String, String>("hello" to "world"))
    }

    @Test
    fun userDefinedClasses() {
        val value = AllTypes().apply {
            allTypesObject = AllTypes()
        }

        assertRoundTrip(value)
    }

    @Test
    fun userDefinedClasses_subsetOfAllFields_ignoreUnknownKeys() {
        val value = AllTypes().apply {
            allTypesObject = AllTypes()
        }

        assertRoundTrip(value) { expected, actual: SubsetOfAllTypes ->
            assertEquals(expected.string, actual.string)
        }
    }

    @Test
    fun userDefinedClasses_subsetOfAllFields_dontIgnoreUnknownKeys() {
        val ejson = Ejson(ignoreUnknownKeys = false)

        val value = AllTypes().apply {
            allTypesObject = AllTypes()
        }
        val encodedValue = ejson.encodeToString(value)

        assertFailsWithMessage<SerializationException>(
            "Could not decode class " +
                    "`org.mongodb.kbson.EjsonTest.SubsetOfAllTypes`, encountered unknown key `boolean`."
        ) {
            ejson.decodeFromString<SubsetOfAllTypes>(encodedValue)
        }
    }

    @Test
    fun userDefinedClasses_transientFields() {
        val value = AllTypes().apply {
            allTypesObject = AllTypes()
        }

        assertRoundTrip(value) { expected, actual: TransientFields ->
            assertEquals(expected.string, actual.string)
            assertNotEquals(expected.char, actual.char)
        }
    }

    @Test
    fun userDefinedClasses_optionalFields() {
        val value = AllTypes().apply {
            allTypesObject = AllTypes()
        }

        assertRoundTrip(value) { expected, actual: OptionalFields ->
            assertEquals(expected.string, actual.string)
        }
    }

    @Test
    fun userDefinedClasses_notMappedFields() {
        val value = AllTypes().apply {
            allTypesObject = AllTypes()
        }
        val encodedValue = Ejson.encodeToString(value)
        assertFailsWithMessage<SerializationException>(
            "Could not decode field " +
                    "'unexistent': Undefined value on a non-optional field"
        ) {
            Ejson.decodeFromString<NotMappedFields>(encodedValue)
        }
    }

    @Test
    fun userDefinedClasses_notMappedOptionalFields() {
        val value = AllTypes().apply {
            allTypesObject = AllTypes()
        }

        assertRoundTrip(value) { expected, actual: NotMappedOptionalFields ->
            assertEquals(expected.string, actual.string)
        }
    }

    @Test
    fun userDefinedClasses_wrongFieldType() {
        val value = AllTypes().apply {
            allTypesObject = AllTypes()
        }
        val encodedValue = Ejson.encodeToString(value)

        assertFailsWithMessage<SerializationException>(
            "Could not decode field" +
                    " 'string': Value expected to be of type BOOLEAN is of unexpected type STRING"
        ) {
            Ejson.decodeFromString<WrongFieldType>(encodedValue)
        }
    }

    @Test
    fun nullValue() {
        assertRoundTrip(null as String?)
        assertRoundTrip(null as AllTypes?)

        assertRoundTrip(BsonNull)

        assertRoundTrip(listOf<String?>(null, null))
        assertRoundTrip(listOf<BsonValue?>(null, null))
        assertRoundTrip(listOf<BsonValue>(BsonNull, BsonNull))
    }

    @Test
    fun decodeMalformedEjsonString() {
        assertFailsWith<SerializationException>(
            "Unexpected JSON token at offset 5: Expected" +
                    " EOF after parsing, but had ] instead"
        ) {
            Ejson.decodeFromString<BsonArray?>("💥&&][💎")
        }
    }

    @Test
    fun enums() {
        assertRoundTrip(SerializableEnum.A)
        assertRoundTrip(SerializableEnum.B)
    }

    @Test
    fun objects() {
        assertRoundTrip(SerializableObject)
    }

    @Test
    fun contextualClass() {
        val expected = ContextualClassHolder(
            contextualClass = ContextualClass(
                string = "helloworld"
            )
        )

        val contextualSerializer = object : KSerializer<ContextualClass> {
            override val descriptor: SerialDescriptor =
                buildClassSerialDescriptor("ContextualClass") {
                    element("string", String.serializer().descriptor)
                }

            override fun deserialize(decoder: Decoder): ContextualClass =
                ContextualClass(decoder.decodeString())

            override fun serialize(encoder: Encoder, value: ContextualClass) {
                encoder.encodeString(value.string)
            }
        }

        assertRoundTrip(
            value = expected,
            ejson = Ejson(serializersModule = SerializersModule {
                contextual(ContextualClass::class, contextualSerializer)
            })
        ) { expected, actual: ContextualClassHolder ->
            assertEquals(expected, actual)
        }
    }

    @Test
    fun contextualMissingSerializerFails() {
        val expected = ContextualClassHolder(
            contextualClass = ContextualClass(
                string = "helloworld"
            )
        )

        assertFailsWithMessage<SerializationException>("Serializer for class 'ContextualClass' is not found.") {
            Ejson.encodeToString(expected)
        }
    }

    @Test
    fun polymorphic() {
        val expected = ClassA("Realm")
        val expectedEjson = Ejson.encodeToString(expected)
        assertFailsWithMessage<SerializationException>("Polymorphic values are not supported.") {
            Ejson.decodeFromString<PolymorphicInterface>(expectedEjson)
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

    // Assert that decoding `value` as an [AllTypes] fails.
    private inline fun <reified T> assertDecodingFailsWithInvalidType(value: T) {
        val encodedValue = Ejson.encodeToString(value)
        assertFailsWithMessage<SerializationException>("Value expected to be of type ") {
            Ejson.decodeFromString<AllTypes>(encodedValue)
        }
    }

    private inline fun <reified T> Iterable<T>.assertRoundTrip() {
        for (value in this) assertRoundTrip(value)
    }

    private inline fun <reified T> assertRoundTrip(value: T) =
        assertRoundTrip(value) { expected: T, actual: T ->
            when (expected) {
                is ByteArray -> assertContentEquals(value as ByteArray, actual as ByteArray)
                else -> assertEquals(value, actual)
            }
        }

    private inline fun <reified E : Any?, reified A> assertRoundTrip(
        value: E,
        ejson: Ejson = Ejson,
        block: (expected: E, actual: A) -> Unit
    ) {
        val encodedValue = ejson.encodeToString(value)
        val decodedValue: A = ejson.decodeFromString(encodedValue)
        block(value, decodedValue)
    }

    interface PolymorphicInterface {
        val name: String
    }

    @Serializable
    data class ClassA(override val name: String) : PolymorphicInterface

    @Serializable
    enum class SerializableEnum(val value: Int) {
        A(4),
        B(5),
    }

    @Serializable
    object SerializableObject {
        const val name = ""
        var surname = ""
    }

    @Serializable
    data class SubsetOfAllTypes(
        val string: String
    )

    @Serializable
    data class TransientFields(
        val string: String,
        @Transient val char: Char = '0'
    )

    @Serializable
    data class OptionalFields(
        val string: String?
    )

    @Serializable
    data class NotMappedFields(
        val string: String?,
        val unexistent: String
    )

    @Serializable
    data class NotMappedOptionalFields(
        val string: String?,
        val unexistent: String? = null
    )

    @Serializable
    data class WrongFieldType(
        val string: Boolean
    )

    data class ContextualClass(
        val string: String
    )

    @Serializable
    data class ContextualClassHolder(
        @Contextual
        val contextualClass: ContextualClass
    )

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
        var allTypesObject: AllTypes? = null

        @Suppress("ComplexMethod")
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
            if (allTypesObject != other.allTypesObject) return false

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
            result = 31 * result + (allTypesObject?.hashCode() ?: 0)
            return result
        }
    }
}
