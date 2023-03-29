package org.mongodb.kbson.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.modules.SerializersModule
import org.mongodb.kbson.BsonArray
import org.mongodb.kbson.BsonBinary
import org.mongodb.kbson.BsonDocument
import org.mongodb.kbson.BsonInvalidOperationException
import org.mongodb.kbson.BsonNull
import org.mongodb.kbson.BsonNumber
import org.mongodb.kbson.BsonString
import org.mongodb.kbson.BsonValue

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.isByteArray(): Boolean =
    kind == StructureKind.LIST && getElementDescriptor(0).kind == PrimitiveKind.BYTE

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
                ListBsonDecoder(
                    bsonArray = rethrowAsSerializationException { currentValue().asArray() },
                    serializersModule = serializersModule,
                    ignoreUnknownKeys = ignoreUnknownKeys
                )
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
                                throw SerializationException("Could not decode class `${descriptor.serialName}`, " +
                                        "encountered unknown key `${entry.key}`.")
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

    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
        when {
            // Depending on the deserialization strategy we need to return BsonNull, if we decode
            // to BsonValue, or null otherwise.
            currentValue() is BsonNull -> {
                when (deserializer) {
                    is BsonValueSerializer,
                    is BsonNullSerializer -> BsonNull
                    else -> null
                } as T
            }
            deserializer.descriptor.isByteArray() -> {
                // Fast path to decode a BsonBinary or BsonArray as a ByteArray
                when (val value = currentValue()) {
                    is BsonBinary -> value.data
                    is BsonArray -> {
                        ByteArray(value.size) {
                            value[it].asNumber().intValue().toByte()
                        }
                    }
                    else -> error("Cannot decode $value as a ByteArray.")
                } as T
            }
            else -> super.decodeSerializableValue(deserializer)
        }

    override fun decodeInt(): Int =
        rethrowAsSerializationException { currentValue().asInt32().value }

    override fun decodeString(): String =
        rethrowAsSerializationException { currentValue().asString().value }

    override fun decodeBoolean(): Boolean =
        rethrowAsSerializationException { currentValue().asBoolean().value }

    override fun decodeByte(): Byte =
        rethrowAsSerializationException { currentValue().asInt32().value.toByte() }

    override fun decodeChar(): Char = rethrowAsSerializationException {
        when (val value = currentValue()) {
            is BsonString -> value.asString().value[0]
            is BsonNumber -> value.asNumber().intValue().toString()[0]
            else -> error("Cannot decode $value as a Char.")
        }
    }

    override fun decodeDouble(): Double =
        rethrowAsSerializationException { currentValue().asDouble().value }

    override fun decodeFloat(): Float =
        rethrowAsSerializationException { currentValue().asDouble().value.toFloat() }

    override fun decodeLong(): Long =
        rethrowAsSerializationException { currentValue().asInt64().value }

    override fun decodeShort(): Short =
        rethrowAsSerializationException { currentValue().asInt32().value.toShort() }

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
        return if (decodedElementCount < descriptor.elementsCount) {
            decodedElementCount++.also {
                entryKey = descriptor.getElementName(it)
                isOptional = descriptor.isElementOptional(it)
            }
        } else {
            CompositeDecoder.DECODE_DONE
        }
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
