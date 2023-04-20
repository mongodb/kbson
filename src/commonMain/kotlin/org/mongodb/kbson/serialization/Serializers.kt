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
package org.mongodb.kbson.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
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
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import org.mongodb.kbson.BsonArray
import org.mongodb.kbson.BsonBinary
import org.mongodb.kbson.BsonBoolean
import org.mongodb.kbson.BsonDBPointer
import org.mongodb.kbson.BsonDateTime
import org.mongodb.kbson.BsonDecimal128
import org.mongodb.kbson.BsonDocument
import org.mongodb.kbson.BsonDouble
import org.mongodb.kbson.BsonInt32
import org.mongodb.kbson.BsonInt64
import org.mongodb.kbson.BsonJavaScript
import org.mongodb.kbson.BsonJavaScriptWithScope
import org.mongodb.kbson.BsonMaxKey
import org.mongodb.kbson.BsonMinKey
import org.mongodb.kbson.BsonNull
import org.mongodb.kbson.BsonObjectId
import org.mongodb.kbson.BsonRegularExpression
import org.mongodb.kbson.BsonSerializationException
import org.mongodb.kbson.BsonString
import org.mongodb.kbson.BsonSymbol
import org.mongodb.kbson.BsonTimestamp
import org.mongodb.kbson.BsonType
import org.mongodb.kbson.BsonUndefined
import org.mongodb.kbson.BsonValue
import org.mongodb.kbson.ExperimentalKBsonSerializerApi
import org.mongodb.kbson.internal.Base64Utils
import org.mongodb.kbson.internal.HexUtils
import org.mongodb.kbson.internal.validateSerialization

@ExperimentalKBsonSerializerApi
internal fun <T> EJson.writeBson(value: T, serializer: SerializationStrategy<T>): BsonValue {
    if (value is BsonValue) return value
    lateinit var result: BsonValue
    val encoder = PrimitiveBsonEncoder(serializersModule) { result = it }
    encoder.encodeSerializableValue(serializer, value)
    return result
}

@ExperimentalKBsonSerializerApi
internal fun <T> EJson.readBson(element: BsonValue, deserializer: DeserializationStrategy<T>): T =
    BsonDecoder(element, serializersModule, ignoreUnknownKeys).decodeSerializableValue(deserializer)


/**
 * Serializes the given [value] into an equivalent [BsonValue] using a serializer retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to BSON.
 */
@ExperimentalKBsonSerializerApi
public inline fun <reified T : Any> EJson.encodeToBsonValue(value: T): BsonValue =
    encodeToBsonValue(serializersModule.serializer(), value)


/**
 * Deserializes the given [value] element into a value of type [T] using a deserializer retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given [BsonValue] element is not a valid BSON input for the type [T]
 * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
 */
@ExperimentalKBsonSerializerApi
public inline fun <reified T : Any> EJson.decodeFromBsonValue(value: BsonValue): T =
    decodeFromBsonValue(serializersModule.serializer(), value)

/**
 * Main entry point to work with EJSON serialization.
 * [EJSON](https://www.mongodb.com/docs/manual/reference/mongodb-extended-json/)
 * is a JSON format that can be used to encode all BSON datatypes.
 *
 * A default instance is provided via [EJson.Default], but if you require an instance with certain
 * registered serializers or different options you can instantiate it with [EJson].
 *
 * This string format also supports encoding to or from a [BsonValue] with the functions [decodeFromBsonValue]
 * and [encodeToBsonValue].
 *
 * Example of usage:
 * ```
 * @Serializable
 * class DataHolder(val id: Int, val data: String, val bsonValue: BsonValue)
 *
 * val ejson = EJson
 * val instance = DataHolder(42, "some data", BsonObjectId() }
 *
 * // Plain StringFormat usage
 * val stringOutput: String = ejson.encodeToString(instance)
 *
 * // BsonValue serialization
 * val bsonValue: BsonValue = ejson.encodeToBsonValue(instance)
 *
 * // Deserialize from string
 * val deserialized: DataHolder = ejson.decodeFromString<DataHolder>(stringOutput)
 *
 * // Deserialize from BsonValue
 * val deserializedFromBsonValue: DataHolder = ejson.decodeFromBsonValue<DataHolder>(bsonValue)
 *
 *  // Deserialize from string to BsonValue
 *  val deserializedToBsonValue: BsonValue = Bson(stringOutput)
 * ```
 *
 * It does not support polymorphic serializers yet.
 */
@ExperimentalKBsonSerializerApi
public sealed class EJson(
    public val ignoreUnknownKeys: Boolean,
    private val json: Json
) : StringFormat {

    /**
     * The default instance of [EJson] with default configuration.
     */
    public companion object Default : EJson(
        ignoreUnknownKeys = true,
        json = Json
    )

    override val serializersModule: SerializersModule = json.serializersModule

    /**
     * Serializes the given [value] into an equivalent [BsonValue] using the given [serializer]
     *
     * @throws [SerializationException] if the given value cannot be serialized to EJSON
     */
    public fun <T> encodeToBsonValue(
        serializer: SerializationStrategy<T>,
        value: T
    ): BsonValue = writeBson(value, serializer)

    /**
     * Deserializes the given [value] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given EJSON element is not a valid EJSON input for the type [T]
     */
    public fun <T> decodeFromBsonValue(
        deserializer: DeserializationStrategy<T>,
        value: BsonValue
    ): T = readBson(value, deserializer)

    /**
     * Deserializes the given EJSON [string] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given EJSON string is not a valid EJSON input for the type [T]
     */
    public override fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        string: String
    ): T = decodeFromBsonValue(
        deserializer,
        json.decodeFromString(string)
    )

    /**
     * Serializes the [value] into an equivalent EJSON using the given [serializer].
     *
     * @throws [SerializationException] if the given value cannot be serialized to EJSON.
     */
    public override fun <T> encodeToString(
        serializer: SerializationStrategy<T>,
        value: T
    ): String = json.encodeToString(
        encodeToBsonValue(
            serializer,
            value
        )
    )
}

/**
 * Creates an instance of [EJson] configured with a [ignoreUnknownKeys] and a custom [serializersModule].
 */
@ExperimentalKBsonSerializerApi
@OptIn(ExperimentalSerializationApi::class)
public fun EJson(
    ignoreUnknownKeys: Boolean = true,
    serializersModule: SerializersModule = EmptySerializersModule
): EJson = EJsonImpl(ignoreUnknownKeys, serializersModule)

@ExperimentalKBsonSerializerApi
@OptIn(ExperimentalSerializationApi::class)
private class EJsonImpl constructor(
    ignoreUnknownKeys: Boolean,
    serializersModule: SerializersModule
) : EJson(
    ignoreUnknownKeys,
    if (serializersModule == EmptySerializersModule) Json else Json {
        this.serializersModule = serializersModule
    })

public object Bson {
    /**
     * Create a BsonDocument from a Json string
     *
     * @param json the Json String
     * @return a BsonDocument
     */
    public operator fun invoke(jsonString: String): BsonValue = Json.decodeFromString(jsonString)

    /**
     * Create a Json string from a BsonValue
     *
     * @param bsonValue the BsonValue
     * @return the Json String
     */
    public fun toJson(bsonValue: BsonValue): String = Json.encodeToString(bsonValue)
}

internal sealed interface BsonSerializer

internal object BsonValueSerializer : KSerializer<BsonValue>, BsonSerializer {

    @Serializable
    private class BsonValueJson

    override val descriptor: SerialDescriptor = BsonValueJson.serializer().descriptor

    @Suppress("ComplexMethod", "LongMethod")
    override fun serialize(encoder: Encoder, value: BsonValue) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> {
                when (value.bsonType) {
                    BsonType.ARRAY -> BsonArraySerializer.serialize(encoder, value.asArray())
                    BsonType.DOCUMENT -> BsonDocumentSerializer.serialize(
                        encoder,
                        value.asDocument()
                    )
                    BsonType.BINARY -> BsonBinarySerializer.serialize(encoder, value.asBinary())
                    BsonType.BOOLEAN -> BsonBooleanSerializer.serialize(
                        encoder,
                        value.asBoolean()
                    )
                    BsonType.DATE_TIME -> BsonDateTimeSerializer.serialize(
                        encoder,
                        value.asDateTime()
                    )
                    BsonType.DB_POINTER -> BsonDBPointerSerializer.serialize(
                        encoder,
                        value.asDBPointer()
                    )
                    BsonType.DECIMAL128 -> BsonDecimal128Serializer.serialize(
                        encoder,
                        value.asDecimal128()
                    )
                    BsonType.DOUBLE -> BsonDoubleSerializer.serialize(encoder, value.asDouble())
                    BsonType.INT32 -> BsonInt32Serializer.serialize(encoder, value.asInt32())
                    BsonType.INT64 -> BsonInt64Serializer.serialize(encoder, value.asInt64())
                    BsonType.JAVASCRIPT -> BsonJavaScriptSerializer.serialize(
                        encoder,
                        value.asJavaScript()
                    )
                    BsonType.JAVASCRIPT_WITH_SCOPE ->
                        BsonJavaScriptWithScopeSerializer.serialize(
                            encoder,
                            value.asJavaScriptWithScope()
                        )
                    BsonType.MAX_KEY -> BsonMaxKeySerializer.serialize(
                        encoder,
                        value.asBsonMaxKey()
                    )
                    BsonType.MIN_KEY -> BsonMinKeySerializer.serialize(
                        encoder,
                        value.asBsonMinKey()
                    )
                    BsonType.NULL -> BsonNullSerializer.serialize(encoder, value.asBsonNull())
                    BsonType.OBJECT_ID -> BsonObjectIdSerializer.serialize(
                        encoder,
                        value.asObjectId()
                    )
                    BsonType.REGULAR_EXPRESSION ->
                        BsonRegularExpressionSerializer.serialize(
                            encoder,
                            value.asRegularExpression()
                        )
                    BsonType.STRING -> BsonStringSerializer.serialize(encoder, value.asString())
                    BsonType.SYMBOL -> BsonSymbolSerializer.serialize(encoder, value.asSymbol())
                    BsonType.TIMESTAMP -> BsonTimestampSerializer.serialize(
                        encoder,
                        value.asTimestamp()
                    )
                    BsonType.UNDEFINED -> BsonUndefinedSerializer.serialize(
                        encoder,
                        value.asBsonUndefined()
                    )
                    else -> throw SerializationException("Unsupported bson type: ${value.bsonType}")
                }
            }
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonValue {
        return when (decoder) {
            is BsonDecoder -> decoder.currentValue()
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
                        throw BsonSerializationException(
                            "Invalid Json: ${e.message} : Source: $jsonElement",
                            e
                        )
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

internal object BsonDocumentSerializer : KSerializer<BsonDocument>, BsonSerializer {

    private val serializer = MapSerializer(BsonDocumentKeySerializer, BsonValueSerializer)
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: BsonDocument) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, value)
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonDocument {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> BsonValueSerializer.deserialize(decoder).asDocument()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonArraySerializer : KSerializer<BsonArray>, BsonSerializer {

    private val serializer = ListSerializer(BsonValueSerializer)
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: BsonArray) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, value)
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonArray {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> BsonValueSerializer.deserialize(decoder).asArray()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonDocumentKeySerializer : KSerializer<String>, BsonSerializer {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BsonDocumentKey", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        return String.serializer().deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, value: String) {
        validateSerialization(!value.contains(Char(0))) { "Contains null byte" }
        String.serializer().serialize(encoder, value)
    }
}

internal object BsonBinarySerializer : KSerializer<BsonBinary>, BsonSerializer {
    private const val HEX_RADIX = 16

    // { "$binary": {"base64": "<payload>", "subType": "<t>"}}
    @Serializable
    private data class BsonValueJson(@SerialName("\$binary") val data: BsonValueData) {
        constructor(
            value: BsonBinary
        ) : this(
            BsonValueData(
                Base64Utils.toBase64String(value.data),
                HexUtils.toHexString(byteArrayOf(value.type))
            )
        )

        fun toBsonValue(): BsonBinary {
            return BsonBinary(
                data.subType.toInt(HEX_RADIX).toByte(),
                Base64Utils.toByteArray(data.base64)
            )
        }
    }

    @Serializable
    private data class BsonValueData(val base64: String, val subType: String)

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: BsonBinary) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonBinary {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonBooleanSerializer : KSerializer<BsonBoolean>, BsonSerializer {
    override val descriptor: SerialDescriptor = Boolean.serializer().descriptor

    override fun serialize(encoder: Encoder, value: BsonBoolean) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> encoder.encodeBoolean(value.value)
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonBoolean {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> BsonBoolean(decoder.decodeBoolean())
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonDateTimeSerializer : KSerializer<BsonDateTime>, BsonSerializer {

    // {"$date": {"$numberLong": "<millis>"}}
    @Serializable
    private data class BsonValueJson(@SerialName("\$date") val data: BsonValueData) {
        constructor(bsonValue: BsonDateTime) : this(BsonValueData(bsonValue.value.toString()))

        fun toBsonValue(): BsonDateTime {
            return BsonDateTime(data.millis.toLong())
        }
    }

    @Serializable
    private data class BsonValueData(@SerialName("\$numberLong") val millis: String)

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonDateTime) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonDateTime {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonDBPointerSerializer : KSerializer<BsonDBPointer>, BsonSerializer {
    // {"$dbPointer": {"$ref": <namespace>, "$id": {"$oid": <hex string>}}}.
    @Serializable
    private data class BsonValueJson(@SerialName("\$dbPointer") val data: BsonValueData) {
        constructor(bsonValue: BsonDBPointer) : this(
            BsonValueData(
                bsonValue.namespace,
                bsonValue.id
            )
        )

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
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonDBPointer) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonDBPointer {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonDecimal128Serializer : KSerializer<BsonDecimal128>, BsonSerializer {
    // {"$numberDecimal": <decimal as a string>} [1]
    @Serializable
    private data class BsonValueJson(@SerialName("\$numberDecimal") val data: String) {
        constructor(bsonValue: BsonDecimal128) : this(bsonValue.value.toString())

        fun toBsonValue(): BsonDecimal128 {
            return BsonDecimal128(data)
        }
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonDecimal128) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonDecimal128 {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonDoubleSerializer : KSerializer<BsonDouble>, BsonSerializer {
    // {"$numberDouble": <64-bit signed floating point as a decimal string>}
    @Serializable
    private data class BsonValueJson(@SerialName("\$numberDouble") val data: String) {
        constructor(value: BsonDouble) : this(value.value.toString())

        fun toBsonValue(): BsonDouble = BsonDouble(data.toDouble())
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonDouble) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonDouble {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonInt32Serializer : KSerializer<BsonInt32>, BsonSerializer {
    // {"$numberInt": <32-bit signed integer as a string>}
    @Serializable
    private data class BsonValueJson(@SerialName("\$numberInt") val data: String) {
        constructor(value: BsonInt32) : this(value.value.toString())

        fun toBsonValue(): BsonInt32 = BsonInt32(data.toInt())
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonInt32) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonInt32 {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonInt64Serializer : KSerializer<BsonInt64>, BsonSerializer {
    // {"$numberLong": <64-bit signed integer as a string>}
    @Serializable
    private data class BsonValueJson(@SerialName("\$numberLong") val data: String) {
        constructor(value: BsonInt64) : this(value.value.toString())

        fun toBsonValue(): BsonInt64 = BsonInt64(data.toLong())
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonInt64) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonInt64 {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonJavaScriptSerializer : KSerializer<BsonJavaScript>, BsonSerializer {
    // {"$code": string}
    @Serializable
    private data class BsonValueJson(@SerialName("\$code") val code: String) {
        constructor(value: BsonJavaScript) : this(value.code)

        fun toBsonValue(): BsonJavaScript = BsonJavaScript(code)
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonJavaScript) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonJavaScript {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonJavaScriptWithScopeSerializer : KSerializer<BsonJavaScriptWithScope>, BsonSerializer {
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
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ZAZ", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BsonJavaScriptWithScope) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonJavaScriptWithScope {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonMaxKeySerializer : KSerializer<BsonMaxKey>, BsonSerializer {
    // {"$maxKey": 1}
    @Serializable
    private data class BsonValueJson(@SerialName("\$maxKey") val data: Int) {
        init {
            require(data == 1) { "maxKey must equal 1" }
        }

        fun toBsonValue(): BsonMaxKey = BsonMaxKey
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonMaxKey) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(1))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonMaxKey {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonMinKeySerializer : KSerializer<BsonMinKey>, BsonSerializer {
    // {"$minKey": 1}
    @Serializable
    private data class BsonValueJson(@SerialName("\$minKey") val data: Int) {
        init {
            require(data == 1) { "minKey must equal 1" }
        }

        fun toBsonValue(): BsonMinKey = BsonMinKey
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonMinKey) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(1))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonMinKey {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

@ExperimentalSerializationApi
internal object BsonNullSerializer : KSerializer<BsonNull>, BsonSerializer {
    private val serializer = JsonNull.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: BsonNull) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> encoder.encodeNull()
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonNull {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> {
                serializer.deserialize(decoder)
                BsonNull
            }
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonObjectIdSerializer : KSerializer<BsonObjectId>, BsonSerializer {
    // {"$oid": <ObjectId bytes as 24-character, big-endian hex string>}
    @Serializable
    private data class BsonValueJson(@SerialName("\$oid") val data: String) {
        constructor(value: BsonObjectId) : this(value.toHexString())

        fun toBsonValue(): BsonObjectId = BsonObjectId(data)
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonObjectId) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonObjectId {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonRegularExpressionSerializer : KSerializer<BsonRegularExpression>, BsonSerializer {
    // {"$regularExpression": {pattern: string, "options": <BSON regular expression options as a
    // string or "" [2]>}}
    @Serializable
    private data class BsonValueJson(@SerialName("\$regularExpression") val data: BsonValueData) {
        constructor(bsonValue: BsonRegularExpression) : this(
            BsonValueData(
                bsonValue.pattern,
                bsonValue.options
            )
        )

        fun toBsonValue(): BsonRegularExpression =
            BsonRegularExpression(data.pattern, data.options)
    }

    @Serializable
    private data class BsonValueData(val pattern: String, val options: String) {
        init {
            validateSerialization(!pattern.contains(Char(0))) { "Invalid key: 'pattern' contains null byte: $pattern" }
            validateSerialization(!options.contains(Char(0))) { "Invalid key: 'options' contains null byte: $options" }
        }
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonRegularExpression) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonRegularExpression {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonStringSerializer : KSerializer<BsonString>, BsonSerializer {
    override val descriptor: SerialDescriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: BsonString) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> encoder.encodeString(value.value)
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonString {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> BsonString(decoder.decodeString())
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonSymbolSerializer : KSerializer<BsonSymbol>, BsonSerializer {
    // {"$symbol": string}
    @Serializable
    private data class BsonValueJson(@SerialName("\$symbol") val data: String) {
        constructor(value: BsonSymbol) : this(value.value)

        fun toBsonValue(): BsonSymbol = BsonSymbol(data)
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonSymbol) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonSymbol {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonTimestampSerializer : KSerializer<BsonTimestamp>, BsonSerializer {
    // {"$timestamp": {"t": pos-integer, "i": pos-integer}}
    @Serializable
    private data class BsonValueJson(@SerialName("\$timestamp") val data: BsonValueData) {
        constructor(value: BsonTimestamp) : this(
            BsonValueData(
                value.time.toUInt(),
                value.inc.toUInt()
            )
        )

        fun toBsonValue(): BsonTimestamp = BsonTimestamp(data.time.toInt(), data.inc.toInt())
    }

    @Serializable
    private data class BsonValueData(
        @SerialName("t") val time: UInt,
        @SerialName("i") val inc: UInt
    )

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonTimestamp) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(value))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonTimestamp {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}

internal object BsonUndefinedSerializer : KSerializer<BsonUndefined>, BsonSerializer {
    // {"$undefined": true}
    @Serializable
    private data class BsonValueJson(@SerialName("\$undefined") val data: Boolean) {
        init {
            require(data) { "Undefined must equal true" }
        }

        fun toBsonValue(): BsonUndefined = BsonUndefined
    }

    private val serializer: KSerializer<BsonValueJson> = BsonValueJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    override fun serialize(encoder: Encoder, value: BsonUndefined) {
        when (encoder) {
            is BsonEncoder,
            is JsonEncoder -> serializer.serialize(encoder, BsonValueJson(true))
            else -> throw SerializationException("Unknown encoder type: $encoder")
        }
    }

    override fun deserialize(decoder: Decoder): BsonUndefined {
        return when (decoder) {
            is BsonDecoder,
            is JsonDecoder -> serializer.deserialize(decoder).toBsonValue()
            else -> throw SerializationException("Unknown decoder type: $decoder")
        }
    }
}
