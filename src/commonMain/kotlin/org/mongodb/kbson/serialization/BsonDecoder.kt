package org.mongodb.kbson.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import org.mongodb.kbson.BsonArray
import org.mongodb.kbson.BsonBinary
import org.mongodb.kbson.BsonDocument
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
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return when (descriptor.kind) {
            StructureKind.LIST -> {
                ListBsonDecoder(currentValue().asArray(), serializersModule)
            }
            StructureKind.MAP -> {
                MapBsonDecoder(currentValue().asDocument(), serializersModule)
            }
            StructureKind.CLASS -> {
                ClassBsonDecoder(currentValue().asDocument(), serializersModule)
            }
            StructureKind.OBJECT -> TODO("decide what to do with it")
            else -> throw IllegalStateException("Unsupported descriptor kind ${descriptor.kind}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
        when {
            // TODO review if there is a better way to map these types
            currentValue() is BsonNull -> {
                when (deserializer) {
                    is BsonValueSerializer,
                    is BsonNullSerializer -> BsonNull
                    else -> null
                } as T
            }
            deserializer.descriptor.isByteArray() -> {
                // Fast path for mapping BsonBinary or BsonArray to ByteArray
                when (val value = currentValue()) {
                    is BsonBinary -> value.data
                    is BsonArray -> {
                        ByteArray(value.size) {
                            value[it].asNumber().intValue().toByte()
                        }
                    }
                    else -> throw IllegalStateException("TODO find right message")
                } as T
            }
            else -> super.decodeSerializableValue(deserializer)
        }

    override fun decodeInt(): Int = currentValue().asInt32().value

    override fun decodeString(): String = currentValue().asString().value

    override fun decodeBoolean(): Boolean = currentValue().asBoolean().value

    override fun decodeByte(): Byte = currentValue().asInt32().value.toByte()

    override fun decodeChar(): Char =
        when (val value = currentValue()) {
            is BsonString -> value.asString().value[0]
            is BsonNumber -> value.asNumber().intValue().toString()[0]
            else -> throw IllegalStateException("TODO cannot decode to char")
        }

    override fun decodeDouble(): Double = currentValue().asDouble().value

    override fun decodeFloat(): Float = currentValue().asDouble().value.toFloat()

    override fun decodeLong(): Long = currentValue().asInt64().value

    override fun decodeNull(): Nothing? =
        if (currentValue().isNull()) null else throw IllegalStateException("TODO not null value")

    override fun decodeShort(): Short = currentValue().asInt32().value.toShort()

    open fun currentValue(): BsonValue = value
}

internal class ListBsonDecoder(
    private val bsonArray: BsonArray,
    override val serializersModule: SerializersModule
) : BsonDecoder(bsonArray, serializersModule) {
    private var decodedElementCount = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (decodedElementCount < bsonArray.size) decodedElementCount++
        else CompositeDecoder.DECODE_DONE
    }

    override fun currentValue(): BsonValue = bsonArray[decodedElementCount - 1]
}

internal class ClassBsonDecoder(
    private val bsonDocument: BsonDocument,
    override val serializersModule: SerializersModule
) : BsonDecoder(bsonDocument, serializersModule) {
    private var decodedElementCount = 0
    private lateinit var entryKey: String

    @OptIn(ExperimentalSerializationApi::class)
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (decodedElementCount < bsonDocument.size) {
            decodedElementCount++.also {
                entryKey = descriptor.getElementName(it)
            }
        } else {
            CompositeDecoder.DECODE_DONE
        }
    }

    override fun currentValue(): BsonValue = bsonDocument[entryKey]!!
}

internal class MapBsonDecoder(
    bsonDocument: BsonDocument,
    override val serializersModule: SerializersModule
) : BsonDecoder(bsonDocument, serializersModule) {
    private var decodedElementCount = 0

    val values = BsonArray(
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
