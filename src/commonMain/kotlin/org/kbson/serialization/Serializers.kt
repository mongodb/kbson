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
package org.kbson.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.modules.SerializersModule
import org.kbson.BsonArray
import org.kbson.BsonBinary
import org.kbson.BsonBoolean
import org.kbson.BsonDBPointer
import org.kbson.BsonDateTime
import org.kbson.BsonDecimal128
import org.kbson.BsonDocument
import org.kbson.BsonDouble
import org.kbson.BsonInt32
import org.kbson.BsonInt64
import org.kbson.BsonJavaScript
import org.kbson.BsonJavaScriptWithScope
import org.kbson.BsonMaxKey
import org.kbson.BsonMinKey
import org.kbson.BsonNull
import org.kbson.BsonObjectId
import org.kbson.BsonRegularExpression
import org.kbson.BsonSerializationException
import org.kbson.BsonString
import org.kbson.BsonSymbol
import org.kbson.BsonTimestamp
import org.kbson.BsonType
import org.kbson.BsonUndefined
import org.kbson.BsonValue
import org.kbson.internal.Base64Utils
import org.kbson.internal.HexUtils
import org.kbson.internal.validateSerialization

/**
 * The BsonSerializersModule
 *
 * Contains serializers that handle the conversion of Bson data to extended json and back
 */
public val BsonSerializersModule: SerializersModule = SerializersModule {
    contextual(BsonValue::class, BsonValueSerializer)
}

/** The Bson companion object */
public object Bson {

    private val extendedJson = Json { BsonSerializersModule }

    /**
     * Create a BsonDocument from a Json string
     *
     * @param json the Json String
     * @return a BsonDocument
     */
    public operator fun invoke(jsonString: String): BsonValue {
        return extendedJson.decodeFromString(BsonValueSerializer, jsonString)
    }

    /**
     * Create a Json string from a BsonValue
     *
     * @param bsonValue the BsonValue
     * @return the Json String
     */
    public fun toJson(bsonValue: BsonValue): String {
        return extendedJson.encodeToString(BsonValueSerializer, bsonValue)
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonValue::class)
private object BsonValueSerializer : KSerializer<BsonValue> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BsonDocument") {}

    @Suppress("ComplexMethod")
    override fun serialize(encoder: Encoder, value: BsonValue) {
        when (encoder) {
            is JsonEncoder -> {
                when (value.bsonType) {
                    BsonType.ARRAY -> ListSerializer(BsonValueSerializer).serialize(encoder, value.asArray())
                    BsonType.DOCUMENT -> BsonDocumentSerializer.serialize(encoder, value.asDocument())
                    BsonType.BINARY -> BsonBinarySerializer.serialize(encoder, value.asBinary())
                    BsonType.BOOLEAN -> BsonBooleanSerializer.serialize(encoder, value.asBoolean())
                    BsonType.DATE_TIME -> BsonDateTimeSerializer.serialize(encoder, value.asDateTime())
                    BsonType.DB_POINTER -> BsonDBPointerSerializer.serialize(encoder, value.asDBPointer())
                    BsonType.DECIMAL128 -> BsonDecimal128Serializer.serialize(encoder, value.asDecimal128())
                    BsonType.DOUBLE -> BsonDoubleSerializer.serialize(encoder, value.asDouble())
                    BsonType.INT32 -> BsonInt32Serializer.serialize(encoder, value.asInt32())
                    BsonType.INT64 -> BsonInt64Serializer.serialize(encoder, value.asInt64())
                    BsonType.JAVASCRIPT -> BsonJavaScriptSerializer.serialize(encoder, value.asJavaScript())
                    BsonType.JAVASCRIPT_WITH_SCOPE ->
                        BsonJavaScriptWithScopeSerializer.serialize(encoder, value.asJavaScriptWithScope())
                    BsonType.MAX_KEY -> BsonMaxKeySerializer.serialize(encoder, value.asBsonMaxKey())
                    BsonType.MIN_KEY -> BsonMinKeySerializer.serialize(encoder, value.asBsonMinKey())
                    BsonType.NULL -> BsonNullSerializer.serialize(encoder, value.asBsonNull())
                    BsonType.OBJECT_ID -> BsonObjectIdSerializer.serialize(encoder, value.asObjectId())
                    BsonType.REGULAR_EXPRESSION ->
                        BsonRegularExpressionSerializer.serialize(encoder, value.asRegularExpression())
                    BsonType.STRING -> BsonStringSerializer.serialize(encoder, value.asString())
                    BsonType.SYMBOL -> BsonSymbolSerializer.serialize(encoder, value.asSymbol())
                    BsonType.TIMESTAMP -> BsonTimestampSerializer.serialize(encoder, value.asTimestamp())
                    BsonType.UNDEFINED -> BsonUndefinedSerializer.serialize(encoder, value.asBsonUndefined())
                    else -> throw SerializationException("Unsupported bson type: ${value.bsonType}")
                }
            }
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonValue {
        return when (decoder) {
            is JsonDecoder -> processElement(decoder.decodeJsonElement(), decoder)
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }

    @Suppress("ComplexMethod", "ReturnCount", "TooGenericExceptionCaught")
    private fun processElement(jsonElement: JsonElement, decoder: JsonDecoder): BsonValue {
        when (jsonElement) {
            is JsonObject -> {
                deserializationStrategy(jsonElement)?.let {
                    try {
                        return decoder.json.decodeFromJsonElement(it, jsonElement)
                    } catch (e: Exception) {
                        throw BsonSerializationException("Invalid Json: ${e.message} : Source: $jsonElement", e)
                    }
                }
                val document = BsonDocument()
                jsonElement.forEach { (k, v) ->
                    run {
                        validateSerialization(!k.contains(Char(0))) {
                            "Invalid key: '$k' contains null byte: $jsonElement"
                        }
                        document[k] = processElement(v, decoder)
                    }
                }
                return document
            }
            is JsonArray -> {
                val bsonArray = BsonArray()
                jsonElement.forEach { v -> bsonArray.add(processElement(v, decoder)) }
                return bsonArray
            }
            is JsonPrimitive -> {
                jsonElement.booleanOrNull?.let {
                    return BsonBoolean(it)
                }
                jsonElement.longOrNull?.let {
                    return BsonInt64(it)
                }
                jsonElement.intOrNull?.let {
                    return BsonInt32(it)
                }
                jsonElement.floatOrNull?.let {
                    return BsonDouble(it.toDouble())
                }
                jsonElement.doubleOrNull?.let {
                    return BsonDouble(it)
                }
                jsonElement.contentOrNull?.let {
                    return BsonString(it)
                }
                return BsonNull
            }
            is JsonNull -> return BsonNull
            else -> throw SerializationException("Unknown jsonElement type: $jsonElement")
        }
    }

    @Suppress("ComplexMethod")
    private fun deserializationStrategy(element: JsonObject): DeserializationStrategy<out BsonValue>? {
        if (element.keys.isEmpty()) return null
        return when (element.keys.first()) {
            "\$binary" -> BsonBinarySerializer
            "\$code" -> if ("\$scope" in element) BsonJavaScriptWithScopeSerializer else BsonJavaScriptSerializer
            "\$date" -> BsonDateTimeSerializer
            "\$dbPointer" -> BsonDBPointerSerializer
            "\$maxKey" -> BsonMaxKeySerializer
            "\$minKey" -> BsonMinKeySerializer
            "\$numberDecimal" -> BsonDecimal128Serializer
            "\$numberDouble" -> BsonDoubleSerializer
            "\$numberInt" -> BsonInt32Serializer
            "\$numberLong" -> BsonInt64Serializer
            "\$oid" -> BsonObjectIdSerializer
            "\$regularExpression" -> BsonRegularExpressionSerializer
            "\$symbol" -> BsonSymbolSerializer
            "\$timestamp" -> BsonTimestampSerializer
            "\$undefined" -> BsonUndefinedSerializer
            else -> null
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonDocument::class)
private object BsonDocumentSerializer : KSerializer<BsonDocument> {

    val serializer = MapSerializer(BsonDocumentKeySerializer, BsonValueSerializer)
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonDocument", serializer.descriptor)

    override fun serialize(encoder: Encoder, value: BsonDocument) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, value)
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonDocument {
        return when (decoder) {
            is JsonDecoder -> BsonValueSerializer.deserialize(decoder).asDocument()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

private object BsonDocumentKeySerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BsonDocumentKey", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        TODO("Not yet implemented")
    }

    override fun serialize(encoder: Encoder, value: String) {
        validateSerialization(!value.contains(Char(0))) { "Contains null byte" }
        String.serializer().serialize(encoder, value)
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonBinary::class)
private object BsonBinarySerializer : KSerializer<BsonBinary> {
    private const val HEX_RADIX = 16

    // { "$binary": {"base64": "<payload>", "subType": "<t>"}}
    @Serializable
    private data class BsonValueJson(@SerialName("\$binary") val data: BsonValueData) {
        constructor(
            value: BsonBinary
        ) : this(BsonValueData(Base64Utils.toBase64String(value.data), HexUtils.toHexString(byteArrayOf(value.type))))

        fun toBsonValue(): BsonBinary {
            return BsonBinary(data.subType.toInt(HEX_RADIX).toByte(), Base64Utils.toByteArray(data.base64))
        }
    }
    @Serializable private data class BsonValueData(val base64: String, val subType: String)

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonBinary", serializer.descriptor)

    override fun serialize(encoder: Encoder, value: BsonBinary) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonBinary {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonBoolean::class)
private object BsonBooleanSerializer : KSerializer<BsonBoolean> {
    override val descriptor: SerialDescriptor = Boolean.serializer().descriptor

    override fun serialize(encoder: Encoder, value: BsonBoolean) {
        when (encoder) {
            is JsonEncoder -> encoder.encodeBoolean(value.value)
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }
    override fun deserialize(decoder: Decoder): BsonBoolean {
        return when (decoder) {
            is JsonDecoder -> BsonBoolean(decoder.decodeBoolean())
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonDateTime::class)
private object BsonDateTimeSerializer : KSerializer<BsonDateTime> {

    // {"$date": {"$numberLong": "<millis>"}}
    @Serializable
    private data class BsonValueJson(@SerialName("\$date") val data: BsonValueData) {
        constructor(bsonValue: BsonDateTime) : this(BsonValueData(bsonValue.value.toString()))
        fun toBsonValue(): BsonDateTime {
            return BsonDateTime(data.millis.toLong())
        }
    }
    @Serializable private data class BsonValueData(@SerialName("\$numberLong") val millis: String)

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonDateTime", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonDateTime) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonDateTime {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonDBPointer::class)
private object BsonDBPointerSerializer : KSerializer<BsonDBPointer> {
    // {"$dbPointer": {"$ref": <namespace>, "$id": {"$oid": <hex string>}}}.
    @Serializable
    private data class BsonValueJson(@SerialName("\$dbPointer") val data: BsonValueData) {
        constructor(bsonValue: BsonDBPointer) : this(BsonValueData(bsonValue.namespace, bsonValue.id))
        fun toBsonValue(): BsonDBPointer {
            return BsonDBPointer(data.ref, data.id)
        }
    }
    @Serializable
    private data class BsonValueData(
        @SerialName("\$ref") val ref: String,
        @Serializable(with = BsonObjectIdSerializer::class) @SerialName("\$id") val id: BsonObjectId
    )

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonDBPointer", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonDBPointer) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonDBPointer {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonDecimal128::class)
private object BsonDecimal128Serializer : KSerializer<BsonDecimal128> {
    // {"$numberDecimal": <decimal as a string>} [1]
    @Serializable
    private data class BsonValueJson(@SerialName("\$numberDecimal") val data: String) {
        constructor(bsonValue: BsonDecimal128) : this(bsonValue.value.toString())
        fun toBsonValue(): BsonDecimal128 {
            return BsonDecimal128(data)
        }
    }
    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonDecimal128", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonDecimal128) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonDecimal128 {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonDouble::class)
private object BsonDoubleSerializer : KSerializer<BsonDouble> {
    // {"$numberDouble": <64-bit signed floating point as a decimal string>}
    @Serializable
    private data class BsonValueJson(@SerialName("\$numberDouble") val data: String) {
        constructor(value: BsonDouble) : this(value.value.toString())
        fun toBsonValue(): BsonDouble = BsonDouble(data.toDouble())
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonDouble", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonDouble) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonDouble {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonInt32::class)
private object BsonInt32Serializer : KSerializer<BsonInt32> {
    // {"$numberInt": <32-bit signed integer as a string>}
    @Serializable
    private data class BsonValueJson(@SerialName("\$numberInt") val data: String) {
        constructor(value: BsonInt32) : this(value.value.toString())
        fun toBsonValue(): BsonInt32 = BsonInt32(data.toInt())
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonInt32", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonInt32) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonInt32 {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonInt64::class)
private object BsonInt64Serializer : KSerializer<BsonInt64> {
    // {"$numberLong": <64-bit signed integer as a string>}
    @Serializable
    private data class BsonValueJson(@SerialName("\$numberLong") val data: String) {
        constructor(value: BsonInt64) : this(value.value.toString())
        fun toBsonValue(): BsonInt64 = BsonInt64(data.toLong())
    }
    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonInt64", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonInt64) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonInt64 {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonJavaScript::class)
private object BsonJavaScriptSerializer : KSerializer<BsonJavaScript> {
    // {"$code": string}
    @Serializable
    private data class BsonValueJson(@SerialName("\$code") val code: String) {
        constructor(value: BsonJavaScript) : this(value.code)
        fun toBsonValue(): BsonJavaScript = BsonJavaScript(code)
    }
    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonJavaScript", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonJavaScript) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonJavaScript {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonJavaScriptWithScope::class)
private object BsonJavaScriptWithScopeSerializer : KSerializer<BsonJavaScriptWithScope> {
    // {"$code": string, "$scope": Document}
    @Serializable
    private data class BsonValueJson(
        @SerialName("\$code") val code: String,
        @Serializable(with = BsonDocumentSerializer::class) @SerialName("\$scope") val scope: BsonDocument
    ) {
        constructor(value: BsonJavaScriptWithScope) : this(value.code, value.scope)
        fun toBsonValue(): BsonJavaScriptWithScope = BsonJavaScriptWithScope(code, scope)
    }
    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ZAZ", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: BsonJavaScriptWithScope) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonJavaScriptWithScope {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonMaxKey::class)
private object BsonMaxKeySerializer : KSerializer<BsonMaxKey> {
    // {"$maxKey": 1}
    @Serializable
    private data class BsonValueJson(@SerialName("\$maxKey") val data: Int) {
        init {
            require(data == 1) { "maxKey must equal 1" }
        }
        fun toBsonValue(): BsonMaxKey = BsonMaxKey
    }
    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonMaxKey", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonMaxKey) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(1))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonMaxKey {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonMinKey::class)
private object BsonMinKeySerializer : KSerializer<BsonMinKey> {
    // {"$minKey": 1}
    @Serializable
    private data class BsonValueJson(@SerialName("\$minKey") val data: Int) {
        init {
            require(data == 1) { "minKey must equal 1" }
        }
        fun toBsonValue(): BsonMinKey = BsonMinKey
    }
    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonMinKey", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonMinKey) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(1))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonMinKey {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonNull::class)
private object BsonNullSerializer : KSerializer<BsonNull> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: BsonNull) {
        when (encoder) {
            is JsonEncoder -> encoder.encodeNull()
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonNull {
        return when (decoder) {
            is JsonDecoder -> {
                decoder.decodeNull()
                BsonNull
            }
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonObjectId::class)
private object BsonObjectIdSerializer : KSerializer<BsonObjectId> {
    // {"$oid": <ObjectId bytes as 24-character, big-endian hex string>}
    @Serializable
    private data class BsonValueJson(@SerialName("\$oid") val data: String) {
        constructor(value: BsonObjectId) : this(value.toHexString())
        fun toBsonValue(): BsonObjectId = BsonObjectId(data)
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonObjectId", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonObjectId) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonObjectId {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonRegularExpression::class)
private object BsonRegularExpressionSerializer : KSerializer<BsonRegularExpression> {
    // {"$regularExpression": {pattern: string, "options": <BSON regular expression options as a
    // string or "" [2]>}}
    @Serializable
    private data class BsonValueJson(@SerialName("\$regularExpression") val data: BsonValueData) {
        constructor(bsonValue: BsonRegularExpression) : this(BsonValueData(bsonValue.pattern, bsonValue.options))
        fun toBsonValue(): BsonRegularExpression = BsonRegularExpression(data.pattern, data.options)
    }
    @Serializable
    private data class BsonValueData(val pattern: String, val options: String) {
        init {
            validateSerialization(!pattern.contains(Char(0))) { "Invalid key: 'pattern' contains null byte: $pattern" }
            validateSerialization(!options.contains(Char(0))) { "Invalid key: 'options' contains null byte: $options" }
        }
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonRegularExpression", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonRegularExpression) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonRegularExpression {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonString::class)
private object BsonStringSerializer : KSerializer<BsonString> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: BsonString) {
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(value.value)
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonString {
        return when (decoder) {
            is JsonDecoder -> BsonString(decoder.decodeString())
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonSymbol::class)
private object BsonSymbolSerializer : KSerializer<BsonSymbol> {
    // {"$symbol": string}
    @Serializable
    private data class BsonValueJson(@SerialName("\$symbol") val data: String) {
        constructor(value: BsonSymbol) : this(value.value)
        fun toBsonValue(): BsonSymbol = BsonSymbol(data)
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonSymbol", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonSymbol) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonSymbol {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonTimestamp::class)
private object BsonTimestampSerializer : KSerializer<BsonTimestamp> {
    // {"$timestamp": {"t": pos-integer, "i": pos-integer}}
    @Serializable
    private data class BsonValueJson(@SerialName("\$timestamp") val data: BsonValueData) {
        constructor(value: BsonTimestamp) : this(BsonValueData(value.time.toUInt(), value.inc.toUInt()))
        fun toBsonValue(): BsonTimestamp = BsonTimestamp(data.time.toInt(), data.inc.toInt())
    }
    @Serializable private data class BsonValueData(@SerialName("t") val time: UInt, @SerialName("i") val inc: UInt)
    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonTimestamp", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonTimestamp) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonTimestamp {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BsonUndefined::class)
private object BsonUndefinedSerializer : KSerializer<BsonUndefined> {

    // {"$undefined": true}
    @Serializable
    private data class BsonValueJson(@SerialName("\$undefined") val data: Boolean) {
        init {
            require(data) { "Undefined must equal true" }
        }
        fun toBsonValue(): BsonUndefined = BsonUndefined
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = SerialDescriptor("BsonUndefined", serializer.descriptor)
    override fun serialize(encoder: Encoder, value: BsonUndefined) {
        when (encoder) {
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(true))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonUndefined {
        return when (decoder) {
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}
