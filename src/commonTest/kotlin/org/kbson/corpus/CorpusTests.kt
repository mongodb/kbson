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
@file:Suppress("UNUSED")

package org.kbson.corpus

import com.goncalossilva.resources.Resource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.kbson.BsonDecimal128
import org.kbson.BsonDocument
import org.kbson.BsonSerializationException
import org.kbson.BsonType
import org.kbson.BsonValue
import org.kbson.internal.Assertions.assertThrows
import org.kbson.internal.HexUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class ArrayTest : CorpusTest("array.json")

class BinaryTest : CorpusTest("binary.json")

class BooleanTest : CorpusTest("boolean.json")

class CodeTest : CorpusTest("code.json")

class CodeWithScopeTest : CorpusTest("code_w_scope.json")

class DateTimeTest : CorpusTest("datetime.json")

class DBPointerTest : CorpusTest("dbpointer.json")

class DBRefTest : CorpusTest("dbref.json")

class Decimal128No1Test : CorpusTest("decimal128-1.json")

class Decimal128No2Test : CorpusTest("decimal128-2.json")

class Decimal128No3Test : CorpusTest("decimal128-3.json")

class Decimal128No4Test : CorpusTest("decimal128-4.json")

class Decimal128No5Test : CorpusTest("decimal128-5.json")

class Decimal128No6Test : CorpusTest("decimal128-6.json")

class Decimal128No7Test : CorpusTest("decimal128-7.json")

class DocumentTest : CorpusTest("document.json")

class DoubleTest : CorpusTest("double.json")

class Int32Test : CorpusTest("int32.json")

class Int64Test : CorpusTest("int64.json")

class MaxkeyTest : CorpusTest("maxkey.json")

class MinkeyTest : CorpusTest("minkey.json")

class MultitypeTest : CorpusTest("multi-type.json")

class MultitypeDeprecatedTest : CorpusTest("multi-type-deprecated.json")

class NullTest : CorpusTest("null.json")

class ObjectIdTest : CorpusTest("oid.json")

class RegexTest : CorpusTest("regex.json")

class StringTest : CorpusTest("string.json")

class SymbolTest : CorpusTest("symbol.json")

class TimestampTest : CorpusTest("timestamp.json")

class TopTest : CorpusTest("top.json")

class UndefinedTest : CorpusTest("undefined.json")

@Suppress("UnnecessaryAbstractClass")
abstract class CorpusTest(filename: String) {

    @Serializable
    data class ValidBson(
        val description: String,
        @SerialName("canonical_bson") val canonicalBsonHex: String,
        @SerialName("canonical_extjson") val canonicalJson: String,
        @SerialName("converted_bson") val convertedBsonHex: String? = null,
        @SerialName("degenerate_bson") val degenerateBsonHex: String? = null,
        @SerialName("degenerate_extjson") val degenerateJson: String? = null,
        val lossy: Boolean = false
    )
    @Serializable data class InvalidBson(val description: String, @SerialName("bson") val invalidBson: String)
    @Serializable data class ParseError(val description: String, @SerialName("string") val invalidString: String)

    @Serializable
    data class TestData(
        val description: String,
        @SerialName("bson_type") val bsonType: String,
        @SerialName("valid") val validBsonScenarios: List<ValidBson> = emptyList(),
        @SerialName("decodeErrors") val invalidBsonScenarios: List<InvalidBson> = emptyList(),
        @SerialName("parseErrors") val parseErrorScenarios: List<ParseError> = emptyList(),
        val deprecated: Boolean = false
    )

    private val data: TestData

    init {
        val resource = Resource("src/commonTest/resources/bson/$filename")
        val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
            useAlternativeNames = true
        }
        this.data = json.decodeFromString(resource.readText())
    }

    @Test
    fun shouldPassAllOutcomes() {
        data.validBsonScenarios.forEach { testValid(it) }
        data.invalidBsonScenarios.forEach { testDecodeError(it) }
        data.parseErrorScenarios.forEach { testParseError(it) }
    }

    private fun testValid(scenario: ValidBson) {
        val decodedDocument = hexStringToBsonDocument(scenario.canonicalBsonHex, scenario.description)

        assertEquals(
            scenario.canonicalBsonHex.uppercase(),
            HexUtils.toHexString(decodedDocument.toByteArray()),
            "Failed to create expected BSON for document with description: " +
                    "${data.description}:${scenario.description}")

        scenario.degenerateBsonHex?.let {
            val decodedDegenerateDocument: BsonDocument = BsonDocument.invoke(HexUtils.toByteArray(it))
            assertEquals(
                scenario.canonicalBsonHex.uppercase(),
                HexUtils.toHexString(decodedDegenerateDocument.toByteArray()),
                "Failed to create expected canonical BSON from degenerate BSON for document with description:" +
                    " ${data.description}:${scenario.description}.")
        }

        val parsedCanonicalJsonDocument = BsonDocument(scenario.canonicalJson)

        if (shouldCompareJson(scenario.canonicalJson)) {
            // native_to_canonical_extended_json( bson_to_native(cB) ) = cEJ
            assertEquals(
                scenario.canonicalJson.stripWhiteSpace(),
                decodedDocument.toJson().stripWhiteSpace(),
                "Failed to create expected canonical JSON for document with description " +
                        "${data.description}:${scenario.description}.")

            // native_to_canonical_extended_json( json_to_native(cEJ) ) = cEJ
            assertEquals(
                scenario.canonicalJson.stripWhiteSpace(),
                parsedCanonicalJsonDocument.toJson().stripWhiteSpace(),
                "Failed to create expected canonical JSON from parsing canonical JSON")


            scenario.degenerateJson?.let {
                // native_to_bson( json_to_native(dEJ) ) = cB
                if (!scenario.degenerateJson.contains("\$uuid")) {
                    assertEquals(
                        scenario.canonicalBsonHex.uppercase(),
                        HexUtils.toHexString(BsonDocument(scenario.degenerateJson).toByteArray()),
                        "Failed to create expected canonical BSON from degenerate JSON for document with description:" +
                                " ${data.description}:${scenario.description}."
                    )
                }
            }
        }

        if (!scenario.lossy) {
            // native_to_bson( json_to_native(cEJ) ) = cB
            assertEquals(
                scenario.canonicalBsonHex.uppercase(),
                HexUtils.toHexString(parsedCanonicalJsonDocument.toByteArray()),
                "Failed to create expected canonical BSON from parsing canonical JSON. " +
                        "${data.description}:${scenario.description}")
        }
    }

    @Suppress("ThrowsCount")
    private fun testDecodeError(scenario: InvalidBson) {
        assertFailsWith<BsonSerializationException>("${scenario.description} should have failed parsing") {
            // Working around the fact that the kbson doesn't report an error for invalid UTF-8, but
            // rather replaces the invalid
            // sequence with the replacement character

            val byteArray = HexUtils.toByteArray(scenario.invalidBson)
            val decodedDocument: BsonDocument = BsonDocument.invoke(byteArray)

            val value: BsonValue? = decodedDocument[decodedDocument.getFirstKey()]
            val decodedString: String =
                when (value?.bsonType) {
                    BsonType.STRING -> value.asString().value
                    BsonType.DB_POINTER -> value.asDBPointer().namespace
                    BsonType.JAVASCRIPT -> value.asJavaScript().code
                    BsonType.SYMBOL -> value.asSymbol().value
                    BsonType.JAVASCRIPT_WITH_SCOPE -> value.asJavaScriptWithScope().code
                    else -> throw UnsupportedOperationException("Unsupported test for BSON type ${value?.bsonType}")
                }
            if (decodedString.contains("\uFFFD")) {
                throw BsonSerializationException("String contains replacement character")
            }

            val decodedByteArray = decodedDocument.toByteArray()
            if (byteArray.size > decodedByteArray.size) {
                throw BsonSerializationException(
                    """Should have consumed all bytes, but ${byteArray.size - decodedByteArray.size},
                       | were not decoded for document with description ${scenario.description}""".trimMargin())
            }
        }
    }

    private fun testParseError(scenario: ParseError) {
        val ignored =
            listOf(
                "Bad \$timestamp ('t' type is string, not number)", "Bad \$timestamp ('i' type is string, not number)")

        if (ignored.contains(scenario.description))
            return // Json is quite lenient and coerces the string values to ints
        if (scenario.description.contains("\$uuid")) return // $uuid currently not supported

        if (data.description.startsWith("Decimal")) {
            assertThrows(NumberFormatException::class) { BsonDecimal128(scenario.invalidString) }
        } else {
            assertThrows(BsonSerializationException::class) { BsonDocument(scenario.invalidString) }
        }
    }

    private fun hexStringToBsonDocument(hexString: String, description: String): BsonDocument {
        try {
            return BsonDocument(HexUtils.toByteArray(hexString))
        } catch (e: Exception) {
            fail("$description: Failed to decode Document: ${e.message} :: ${e.stackTraceToString()}")
        }
    }

    private fun String.stripWhiteSpace(): String = this.replace(" ", "")

    @Suppress("RedundantIf")
    private fun shouldCompareJson(canonicalJson: String): Boolean {
        return if (canonicalJson.contains("\\u")) {
            // The corpus escapes all non-ascii characters but kotlinx.serialization doesn't
            false
        } else if (1.0.toString() != "1.0" &&
            listOf("Double type", "Multiple types within the same document").contains(data.description)) {
            // Node has different string representations for Doubles
            false
        } else {
            true
        }
    }
}
