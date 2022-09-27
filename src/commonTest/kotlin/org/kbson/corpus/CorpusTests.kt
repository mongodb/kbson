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
package org.kbson.corpus

import com.goncalossilva.resources.Resource
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
        val lossy: Boolean = false
    )
    @Serializable data class InvalidBson(val description: String, @SerialName("bson") val invalidBson: String)

    @Serializable
    data class TestData(
        val description: String,
        @SerialName("bson_type") val bsonType: String,
        @SerialName("valid") val validBsonScenarios: List<ValidBson> = emptyList(),
        @SerialName("decodeErrors") val invalidBsonScenarios: List<InvalidBson> = emptyList(),
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
    }

    private fun testValid(scenario: ValidBson) {
        // TODO("Not implemented yet")
        assertTrue(scenario.description.isNotEmpty())
    }

    @Suppress("ThrowsCount")
    private fun testDecodeError(scenario: InvalidBson) {
        // TODO("Not implemented yet")
        assertTrue(scenario.description.isNotEmpty())
    }
}
