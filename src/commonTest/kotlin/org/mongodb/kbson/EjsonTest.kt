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
@file:OptIn(ExperimentalKBsonSerializerApi::class)

package org.mongodb.kbson

import assertFailsWithMessage
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
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
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer
import org.mongodb.kbson.serialization.EJson
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class EjsonTest {
    /**
     * Generates a pair of the given value and its serializer strategy.
     *
     * KSerializer provides access to the serializer in compile time only. This forces to retrieve
     * the serializer when we declare the test data rather than when we use the data in a test.
     */
    private inline fun <reified T> valueWithSerializer(value: T) =
        value to EJson.serializersModule.serializer<T>()

    enum class SupportedKotlinTypes {
        ARRAY,
        BOOLEAN,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        STRING,
        CHAR,
        BYTEARRAY,
        SET,
        LIST,
        MAP,
    }

    private val supportedKotlinTypesDataset: List<Pair<Any, KSerializer<out Any>>> =
        SupportedKotlinTypes.values().flatMap { type ->
            when (type) {
                SupportedKotlinTypes.BOOLEAN -> listOf(
                    valueWithSerializer(true),
                    valueWithSerializer(false)
                )
                SupportedKotlinTypes.SHORT -> listOf(
                    valueWithSerializer(Short.MIN_VALUE),
                    valueWithSerializer(Short.MAX_VALUE)
                )
                SupportedKotlinTypes.INT -> listOf(
                    valueWithSerializer(Int.MIN_VALUE),
                    valueWithSerializer(Int.MAX_VALUE)
                )
                SupportedKotlinTypes.LONG -> listOf(
                    valueWithSerializer(Long.MIN_VALUE),
                    valueWithSerializer(Long.MAX_VALUE)
                )
                SupportedKotlinTypes.FLOAT -> listOf(
                    valueWithSerializer(Float.MIN_VALUE),
                    valueWithSerializer(Float.MAX_VALUE)
                )
                SupportedKotlinTypes.DOUBLE -> listOf(
                    valueWithSerializer(Double.MIN_VALUE),
                    valueWithSerializer(Double.MAX_VALUE)
                )
                SupportedKotlinTypes.STRING -> listOf(
                    valueWithSerializer("hello world"),
                    valueWithSerializer(""),
                    valueWithSerializer("🚀💎")
                )
                SupportedKotlinTypes.CHAR -> listOf(
                    valueWithSerializer('4'),
                    valueWithSerializer('c'),
                    valueWithSerializer('[')
                )
                SupportedKotlinTypes.BYTEARRAY -> listOf(
                    valueWithSerializer(byteArrayOf(10, 0, 10)),
                    valueWithSerializer(byteArrayOf()),
                    valueWithSerializer(byteArrayOf(0, 0))
                )
                SupportedKotlinTypes.ARRAY -> listOf(
                    valueWithSerializer(arrayOf<String>("hello world"))
                )
                SupportedKotlinTypes.LIST -> listOf(
                    valueWithSerializer(listOf<String>("hello world")),
                    valueWithSerializer(listOf<List<String>>(listOf("hello world"))),
                )
                SupportedKotlinTypes.SET -> listOf(
                    valueWithSerializer(setOf<String>("hello world")),
                    valueWithSerializer(setOf<Set<String>>(setOf("hello world"))),
                )
                SupportedKotlinTypes.MAP -> listOf(
                    valueWithSerializer(mapOf<String, String>("hello" to "world")),
                    valueWithSerializer(mapOf<String, Map<String, String>>("hello" to mapOf("hello" to "world"))),
                )
            }
        }

    private val bsonDataSet: List<Pair<BsonValue, KSerializer<out BsonValue>>> = BsonType.values()
        .flatMap {
            when (it) {
                BsonType.DOUBLE -> listOf(valueWithSerializer(BsonDouble(10.0)))
                BsonType.STRING -> listOf(valueWithSerializer(BsonString("hello world")))
                BsonType.DOCUMENT -> listOf(valueWithSerializer(BsonDocument()))
                BsonType.ARRAY -> listOf(valueWithSerializer(BsonArray()))
                BsonType.BINARY -> listOf(valueWithSerializer(BsonBinary(byteArrayOf(10, 20, 30))))
                BsonType.UNDEFINED -> listOf(valueWithSerializer(BsonUndefined))
                BsonType.OBJECT_ID -> listOf(valueWithSerializer(BsonObjectId()))
                BsonType.BOOLEAN -> listOf(
                    valueWithSerializer(BsonBoolean.TRUE_VALUE),
                    valueWithSerializer(BsonBoolean.FALSE_VALUE)
                )
                BsonType.DATE_TIME -> listOf(valueWithSerializer(BsonDateTime()))
                BsonType.NULL -> listOf(valueWithSerializer(BsonNull))
                BsonType.REGULAR_EXPRESSION -> listOf(valueWithSerializer(BsonRegularExpression("")))
                BsonType.DB_POINTER -> listOf(
                    valueWithSerializer(
                        BsonDBPointer(
                            "test",
                            BsonObjectId()
                        )
                    )
                )
                BsonType.JAVASCRIPT -> listOf(
                    valueWithSerializer(
                        BsonJavaScript(
                            """
                    alert('Hello, world');
                """.trimIndent()
                        )
                    )
                )
                BsonType.SYMBOL -> listOf(valueWithSerializer(BsonSymbol("d")))
                BsonType.JAVASCRIPT_WITH_SCOPE -> listOf(
                    valueWithSerializer(
                        BsonJavaScriptWithScope(
                            """
                    alert('Hello, world');
                """.trimIndent(),
                            BsonDocument()
                        )
                    )
                )
                BsonType.INT32 -> listOf(
                    valueWithSerializer(BsonInt32(Int.MIN_VALUE)),
                    valueWithSerializer(BsonInt32(Int.MAX_VALUE))
                )
                BsonType.TIMESTAMP -> listOf(valueWithSerializer(BsonTimestamp()))
                BsonType.INT64 -> listOf(
                    valueWithSerializer(BsonInt64(Long.MIN_VALUE)),
                    valueWithSerializer(BsonInt64(Long.MAX_VALUE))
                )
                BsonType.DECIMAL128 -> listOf(valueWithSerializer(BsonDecimal128("100")))
                BsonType.MIN_KEY -> listOf(valueWithSerializer(BsonMinKey))
                BsonType.MAX_KEY -> listOf(valueWithSerializer(BsonMaxKey))
                BsonType.END_OF_DOCUMENT -> listOf()
            }
        }.let { dataset ->
            val bsonValues = dataset.map { it.first }
            listOf(
                valueWithSerializer(BsonArray(bsonValues)),
                valueWithSerializer(
                    BsonDocument(
                        bsonValues.mapIndexed { index, bsonValue ->
                            "$index" to bsonValue
                        }.toMap()
                    )
                )
            )
            dataset
        }

    @Test
    fun roundTripAllTypes() {
        supportedKotlinTypesDataset + bsonDataSet.forEach { (data: Any, serializer: KSerializer<out Any>) ->
            assertRoundTrip(data, serializer as KSerializer<Any>)
        }
    }

    @Test
    fun roundTripAllTypes_failsInvalidType() {
        supportedKotlinTypesDataset + bsonDataSet.filterNot {
            it.first == BsonNull // BsonNull would always decode to a valid type
        }.forEach { (data: Any, _) ->
            assertDecodingFailsWithInvalidType(data)
        }
    }

    @Test
    fun nullValues() {
        assertRoundTrip(null as String?)
        assertRoundTrip(null as AllTypes?)

        assertRoundTrip(BsonNull)

        assertRoundTrip(listOf<String?>(null, null))
        assertRoundTrip(listOf<BsonValue?>(null, null))
        assertRoundTrip(listOf<BsonValue>(BsonNull, BsonNull))
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
        val ejson = EJson(ignoreUnknownKeys = false)

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
    fun userDefinedClasses_optionalFieldWithDefaults() {
        assertEquals(OptionalFieldsWithDefaults().string, EJson.decodeFromString("{}"))
    }

    @Test
    fun userDefinedClasses_notMappedFields() {
        val value = AllTypes().apply {
            allTypesObject = AllTypes()
        }
        val encodedValue = EJson.encodeToString(value)
        assertFailsWithMessage<SerializationException>(
            "Could not decode field " +
                    "'unexistent': Undefined value on a non-optional field"
        ) {
            EJson.decodeFromString<NotMappedFields>(encodedValue)
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
        val encodedValue = EJson.encodeToString(value)

        assertFailsWithMessage<SerializationException>(
            "Could not decode field" +
                    " 'string': Value expected to be of type BOOLEAN is of unexpected type STRING"
        ) {
            EJson.decodeFromString<WrongFieldType>(encodedValue)
        }
    }

    @Test
    fun decodeMalformedEjsonString() {
        assertFailsWith<SerializationException>(
            "Unexpected JSON token at offset 5: Expected" +
                    " EOF after parsing, but had ] instead"
        ) {
            EJson.decodeFromString<BsonArray?>("💥&&][💎")
        }
    }

    @Test
    fun userDefinedClasses_enums() {
        assertRoundTrip(SerializableEnum.A)
        assertRoundTrip(SerializableEnum.B)
    }

    @Test
    fun userDefinedClasses_objects() {
        assertRoundTrip(SerializableObject)
    }

    @Test
    fun userDefinedClasses_contextualClass() {
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
            ejson = EJson(serializersModule = SerializersModule {
                contextual(ContextualClass::class, contextualSerializer)
            })
        ) { expected, actual: ContextualClassHolder ->
            assertEquals(expected, actual)
        }
    }

    @Test
    fun userDefinedClasses_contextualMissingSerializerFails() {
        val expected = ContextualClassHolder(
            contextualClass = ContextualClass(
                string = "helloworld"
            )
        )

        assertFailsWithMessage<SerializationException>("Serializer for class 'ContextualClass' is not found.") {
            EJson.encodeToString(expected)
        }
    }

    @Test
    fun userDefinedClasses_polymorphic() {
        EJson(
            serializersModule = SerializersModule {
                polymorphic(PolymorphicInterface::class) {
                    subclass(ClassA::class)
                }
            }
        ).let { ejson ->
            val expected = ClassA("Realm")

            assertFailsWithMessage<SerializationException>("Polymorphic values are not supported.") {
                ejson.encodeToString(
                    // Native requires using the polymorphic serializer explicitly.
                    serializer = PolymorphicSerializer(PolymorphicInterface::class),
                    value = expected
                )
            }

            val expectedEjson = ejson.encodeToString(expected)
            assertFailsWithMessage<SerializationException>("Polymorphic values are not supported.") {
                ejson.decodeFromString<PolymorphicInterface>(
                    // Native requires using the polymorphic serializer explicitly.
                    deserializer = PolymorphicSerializer(PolymorphicInterface::class),
                    string = expectedEjson
                )
            }
        }
    }

    @Test
    fun nonStrict_primitiveValues() {
        val bsonValues = EJson.decodeFromString<BsonValue>("[true, 1, 1.5, \"kbson\"]")
        assertEquals(
            BsonArray(listOf(BsonBoolean(true), BsonInt64(1), BsonDouble(1.5), BsonString("kbson"))), bsonValues)
    }

    @Test
    fun nonStrict_strings() {
        val bsonValues = EJson.decodeFromString<BsonValue>("[\"true\", \"1\", \"1.5\", \"kbson\"]")
        assertEquals(
            BsonArray(listOf(BsonString("true"), BsonString("1"), BsonString("1.5"), BsonString("kbson"))), bsonValues)
    }

    // Assert that decoding `value` as an [AllTypes] fails.
    private inline fun <reified T> assertDecodingFailsWithInvalidType(value: T) {
        val encodedValue = EJson.encodeToString(value)
        assertFailsWithMessage<SerializationException>("Value expected to be of type ") {
            EJson.decodeFromString<AllTypes>(encodedValue)
        }
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
        ejson: EJson = EJson,
        block: (expected: E, actual: A) -> Unit
    ) {
        val encodedValue = ejson.encodeToString(value)
        val decodedValue: A = ejson.decodeFromString(encodedValue)
        block(value, decodedValue)
    }

    private fun <E> assertRoundTrip(
        expected: E,
        serializer: KSerializer<E>,
    ) {
        val encodedValue = EJson.encodeToString(serializer, expected)
        val actual: E = EJson.decodeFromString(serializer, encodedValue)

        when (expected) {
            is ByteArray -> assertContentEquals(expected as ByteArray, actual as ByteArray)
            is Array<*> -> {
                (actual as Array<*>)
                assertContentEquals(expected.asList(), actual.asList())
            }
            else -> assertEquals(expected, actual)
        }
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
    data class OptionalFieldsWithDefaults(
        val string: String = "Hello, world!"
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
        val stringArray = arrayOf("hello world")
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
            if (!stringArray.contentEquals(other.stringArray)) return false
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
            result = 31 * result + stringArray.contentHashCode()
            result = 31 * result + stringList.hashCode()
            result = 31 * result + stringMap.hashCode()
            result = 31 * result + (allTypesObject?.hashCode() ?: 0)
            return result
        }
    }
}
