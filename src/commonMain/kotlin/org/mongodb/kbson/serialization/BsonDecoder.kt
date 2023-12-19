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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.modules.SerializersModule
import org.mongodb.kbson.BsonArray
import org.mongodb.kbson.BsonBinary
import org.mongodb.kbson.BsonDocument
import org.mongodb.kbson.BsonDouble
import org.mongodb.kbson.BsonInt32
import org.mongodb.kbson.BsonInt64
import org.mongodb.kbson.BsonInvalidOperationException
import org.mongodb.kbson.BsonNull
import org.mongodb.kbson.BsonNumber
import org.mongodb.kbson.BsonString
import org.mongodb.kbson.BsonType
import org.mongodb.kbson.BsonValue

@OptIn(ExperimentalSerializationApi::class)
internal open class BsonDecoder(
    private val value: BsonValue,
    override val serializersModule: SerializersModule,
    private val ignoreUnknownKeys: Boolean
) : AbstractDecoder() {
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0

    @Suppress("NestedBlockDepth")
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return when (descriptor.kind) {
            StructureKind.LIST -> {
                when (currentValue().bsonType) {
                    BsonType.BINARY -> ListBsonBinaryDecoder(
                        bsonBinary = currentValue().asBinary(),
                        serializersModule = serializersModule
                    )
                    BsonType.ARRAY -> ListBsonDecoder(
                        bsonArray = rethrowAsSerializationException {
                            currentValue().asArray()
                        },
                        serializersModule = serializersModule,
                        ignoreUnknownKeys = ignoreUnknownKeys
                    )
                    else -> error("Unsupported")
                }
            }
            StructureKind.MAP -> {
                MapBsonDecoder(
                    bsonDocument = rethrowAsSerializationException { currentValue().asDocument() },
                    serializersModule = serializersModule,
                    ignoreUnknownKeys = ignoreUnknownKeys
                )
            }
            StructureKind.CLASS -> {
                with(rethrowAsSerializationException { currentValue().asDocument() }) {
                    // validate ignoreUnknownKeys as a precondition. Each document entry must have a
                    // matching class field
                    if (!ignoreUnknownKeys) {
                        forEach { entry ->
                            if (descriptor.getElementIndex(entry.key) == UNKNOWN_NAME) {
                                throw SerializationException(
                                    "Could not decode class `${descriptor.serialName}`, " +
                                            "encountered unknown key `${entry.key}`."
                                )
                            }
                        }
                    }
                    ClassBsonDecoder(
                        bsonDocument = this,
                        serializersModule = serializersModule,
                        ignoreUnknownKeys = ignoreUnknownKeys
                    )
                }
            }
            StructureKind.OBJECT -> {
                // Mimics the Json decode behavior of using an empty map on Kotlin Objects.
                ClassBsonDecoder(
                    bsonDocument = BsonDocument(),
                    serializersModule = serializersModule,
                    ignoreUnknownKeys = ignoreUnknownKeys
                )
            }
            PolymorphicKind.OPEN,
            PolymorphicKind.SEALED -> throw SerializationException("Polymorphic values are not supported.")
            else -> error("Unsupported descriptor kind ${descriptor.kind}")
        }
    }

    // TODO document fast paths
    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = when {
        currentValue() is BsonNull -> fastPathBsonNull(deserializer)
        deserializer is BsonSerializer -> fastPathBsonValue(deserializer)
        else -> super.decodeSerializableValue(deserializer)
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <T> fastPathBsonValue(deserializer: BsonSerializer): T =
        if (currentValue().isNumber()) {
            val bsonNumber = currentValue().asNumber()
            // apply coercion
            when (deserializer) {
                is BsonInt32Serializer -> BsonInt32(bsonNumber.intValue())
                is BsonInt64Serializer -> BsonInt64(bsonNumber.longValue())
                is BsonDoubleSerializer -> BsonDouble(bsonNumber.doubleValue())
                else -> error("Could not deserialize BsonNumber")
            }
        } else {
            currentValue()
        } as T

    // Depending on the deserialization strategy we need to return BsonNull, if we decode
    // to BsonValue, or null otherwise.
    @Suppress("UNCHECKED_CAST")
    private inline fun <T> fastPathBsonNull(deserializer: DeserializationStrategy<T>): T =
        when (deserializer) {
            is BsonValueSerializer,
            is BsonNullSerializer -> BsonNull
            else -> null
        } as T

    override fun decodeInt(): Int =
        rethrowAsSerializationException { currentValue().asNumber().intValue() }

    override fun decodeString(): String =
        rethrowAsSerializationException { currentValue().asString().value }

    override fun decodeBoolean(): Boolean =
        rethrowAsSerializationException { currentValue().asBoolean().value }

    override fun decodeByte(): Byte =
        rethrowAsSerializationException { currentValue().asNumber().intValue().toByte() }

    override fun decodeChar(): Char = rethrowAsSerializationException {
        when (val value = currentValue()) {
            is BsonString -> value.asString().value[0]
            is BsonNumber -> value.asNumber().intValue().toString()[0]
            else -> error("Cannot decode $value as a Char.")
        }
    }

    override fun decodeDouble(): Double =
        rethrowAsSerializationException { currentValue().asNumber().doubleValue() }

    override fun decodeFloat(): Float =
        rethrowAsSerializationException { currentValue().asNumber().doubleValue().toFloat() }

    override fun decodeLong(): Long =
        rethrowAsSerializationException { currentValue().asNumber().longValue() }

    override fun decodeShort(): Short =
        rethrowAsSerializationException { currentValue().asNumber().intValue().toShort() }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val name = currentValue().asString().value
        return enumDescriptor.getElementIndex(name)
    }

    open fun currentValue(): BsonValue = value

    open fun <T> rethrowAsSerializationException(block: () -> T): T = try {
        block()
    } catch (e: BsonInvalidOperationException) {
        throw SerializationException(e.message, e)
    }
}

@ExperimentalSerializationApi
internal class ListBsonBinaryDecoder(
    private val bsonBinary: BsonBinary,
    override val serializersModule: SerializersModule,
) : AbstractDecoder() {
    private var decodedElementCount = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (decodedElementCount < bsonBinary.data.size) decodedElementCount++
        else CompositeDecoder.DECODE_DONE
    }

    override fun decodeByte(): Byte = bsonBinary.data[decodedElementCount - 1]
}

internal class ListBsonDecoder(
    private val bsonArray: BsonArray,
    override val serializersModule: SerializersModule,
    ignoreUnknownKeys: Boolean
) : BsonDecoder(bsonArray, serializersModule, ignoreUnknownKeys) {
    private var decodedElementCount = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (decodedElementCount < bsonArray.size) decodedElementCount++
        else CompositeDecoder.DECODE_DONE
    }

    override fun currentValue(): BsonValue = bsonArray[decodedElementCount - 1]
}

/**
 * Decodes a BsonDocument as a Class structured type.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class ClassBsonDecoder(
    private val bsonDocument: BsonDocument,
    override val serializersModule: SerializersModule,
    ignoreUnknownKeys: Boolean
) : BsonDecoder(bsonDocument, serializersModule, ignoreUnknownKeys) {
    private var decodedElementCount = 0
    private lateinit var entryKey: String
    private var isOptional: Boolean = false

    /**
     * This function is triggered each time a field is going to be decoded. During this stage
     * we have to extract the key that would be used in [currentValue] to access the value from the
     * [bsonDocument] that represents the class.
     */
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (decodedElementCount < descriptor.elementsCount) {
            val index = decodedElementCount++
            val key = descriptor.getElementName(index)
            val elementOptional = descriptor.isElementOptional(index)
            if (!elementOptional || bsonDocument[key] != null) {
                entryKey = key
                isOptional = elementOptional
                return index
            } else {
                continue
            }
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun currentValue(): BsonValue = bsonDocument[entryKey] ?: if (isOptional) {
        BsonNull
    } else {
        throw SerializationException("Could not decode field '$entryKey': Undefined value on a non-optional field")
    }

    override fun <T> rethrowAsSerializationException(block: () -> T): T = try {
        block()
    } catch (e: BsonInvalidOperationException) {
        throw SerializationException("Could not decode field '$entryKey': ${e.message}", e)
    }
}

internal class MapBsonDecoder(
    bsonDocument: BsonDocument,
    override val serializersModule: SerializersModule,
    ignoreUnknownKeys: Boolean
) : BsonDecoder(bsonDocument, serializersModule, ignoreUnknownKeys) {
    private var decodedElementCount = 0

    private val values = BsonArray(
        bsonDocument.flatMap { (key, value) ->
            listOf(BsonString(key), value)
        }.toList()
    )

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (decodedElementCount < values.size) decodedElementCount++
        else CompositeDecoder.DECODE_DONE
    }

    override fun currentValue(): BsonValue = values[decodedElementCount - 1]
}
