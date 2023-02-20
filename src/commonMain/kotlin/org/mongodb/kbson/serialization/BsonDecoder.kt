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
internal open class BsonDecoder(
    private val value: BsonValue,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0

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
            StructureKind.OBJECT -> TODO("decide what to do with it")
            else -> throw IllegalStateException("Unsupported descriptor kind ${descriptor.kind}")
        }
    }

    override fun decodeInt(): Int = currentValue().asInt32().value

    override fun decodeString(): String = currentValue().asString().value

    open fun currentValue(): BsonValue = value
}

internal class ListBsonDecoder(
    private val bsonArray: BsonArray,
    override val serializersModule: SerializersModule
) : BsonDecoder(bsonArray, serializersModule) {
    private var decodedElementCount = -1

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (decodedElementCount < bsonArray.size) decodedElementCount++
        else CompositeDecoder.DECODE_DONE
    }

    override fun currentValue(): BsonValue = bsonArray[decodedElementCount]
}

internal class ClassBsonDecoder(
    private val bsonDocument: BsonDocument,
    override val serializersModule: SerializersModule
) : BsonDecoder(bsonDocument, serializersModule) {
    private var decodedElementCount = -1
    private lateinit var entryKey: String

    @OptIn(ExperimentalSerializationApi::class)
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (decodedElementCount < bsonDocument.size) {
            decodedElementCount++.also {
                entryKey = descriptor.getElementName(decodedElementCount)
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
    private var decodedElementCount = -1

    val values = BsonArray(
        bsonDocument.flatMap { (key, value) ->
            listOf(BsonString(key), value)
        }.toList()
    )

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (decodedElementCount < values.size) decodedElementCount++
        else CompositeDecoder.DECODE_DONE
    }

    override fun currentValue(): BsonValue = values[decodedElementCount]
}
