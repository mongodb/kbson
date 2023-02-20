package org.mongodb.kbson.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import org.mongodb.kbson.BsonArray
import org.mongodb.kbson.BsonDocument
import org.mongodb.kbson.BsonString
import org.mongodb.kbson.BsonValue


@OptIn(ExperimentalSerializationApi::class)
internal class ListBsonDecoder(
    val value: BsonArray,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    var decodedElementCount = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (decodedElementCount < value.size) decodedElementCount++
        else CompositeDecoder.DECODE_DONE
    }

    override fun decodeInt(): Int = value[decodedElementCount - 1].asInt32().value

    override fun decodeString(): String = value[decodedElementCount - 1].asString().value
}

@OptIn(ExperimentalSerializationApi::class)
internal class ClassBsonDecoder(
    val value: BsonDocument,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    var decodedElementCount = 0
    lateinit var nextName: String
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (decodedElementCount < value.size) {
            nextName = descriptor.getElementName(decodedElementCount)
            decodedElementCount++
        } else {
            CompositeDecoder.DECODE_DONE
        }
    }

    override fun decodeInt(): Int = value[nextName]!!.asInt32().value

    override fun decodeString(): String = value[nextName]!!.asString().value
}

@OptIn(ExperimentalSerializationApi::class)
internal class MapBsonDecoder(
    value: BsonDocument,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    var decodedElementCount = 0

    val values = BsonArray(
        value.flatMap { (key, value) ->
            listOf(BsonString(key), value)
        }.toList()
    )

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (decodedElementCount < values.size) decodedElementCount++
        else CompositeDecoder.DECODE_DONE
    }

    override fun decodeInt(): Int = values[decodedElementCount - 1].asInt32().value

    override fun decodeString(): String = values[decodedElementCount - 1].asString().value
}

@OptIn(ExperimentalSerializationApi::class)
internal class PrimitiveBsonDecoder(
    val value: BsonValue,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        TODO("Not yet implemented")
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return when (descriptor.kind) {
            StructureKind.LIST -> {
                ListBsonDecoder(value.asArray(), serializersModule)
            }
            StructureKind.MAP -> {
                MapBsonDecoder(value.asDocument(), serializersModule)
            }
            StructureKind.CLASS -> {
                ClassBsonDecoder(value.asDocument(), serializersModule)
            }
            else -> TODO()
        }
    }

    override fun decodeInt(): Int = value.asInt32().value

    override fun decodeString(): String = value.asString().value
}