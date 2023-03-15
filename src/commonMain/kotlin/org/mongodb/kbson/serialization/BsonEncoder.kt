package org.mongodb.kbson.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import org.mongodb.kbson.BsonArray
import org.mongodb.kbson.BsonBinary
import org.mongodb.kbson.BsonBoolean
import org.mongodb.kbson.BsonDocument
import org.mongodb.kbson.BsonDouble
import org.mongodb.kbson.BsonInt32
import org.mongodb.kbson.BsonInt64
import org.mongodb.kbson.BsonNull
import org.mongodb.kbson.BsonString
import org.mongodb.kbson.BsonValue

@OptIn(ExperimentalSerializationApi::class)
internal sealed class BsonEncoder(
    override val serializersModule: SerializersModule,
    val nodeConsumer: (BsonValue) -> Unit
) : AbstractEncoder() {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
        when (descriptor.kind) {
            StructureKind.LIST ->
                BsonArrayEncoder(serializersModule) {
                    pushValue(it)
                }
            StructureKind.CLASS ->
                BsonClassEncoder(serializersModule) {
                    pushValue(it)
                }
            StructureKind.MAP ->
                BsonDocumentEncoder(serializersModule) {
                    pushValue(it)
                }
            StructureKind.OBJECT -> BsonDocumentEncoder(serializersModule) {
                // Mimics the Json encode behavior of returning an empty map on Kotlin Objects.
                pushValue(BsonDocument())
            }

            else -> throw IllegalStateException("Unsupported descriptor Kind ${descriptor.kind}")
        }

    override fun endStructure(descriptor: SerialDescriptor) {
        nodeConsumer(getCurrent())
    }

    override fun encodeInt(value: Int) {
        pushValue(BsonInt32(value))
    }

    override fun encodeString(value: String) {
        pushValue(BsonString(value))
    }

    override fun encodeBoolean(value: Boolean) {
        pushValue(BsonBoolean(value))
    }

    override fun encodeByte(value: Byte) {
        pushValue(BsonInt32(value.toInt()))
    }

    override fun encodeChar(value: Char) {
        pushValue(BsonString(value.toString()))
    }

    override fun encodeDouble(value: Double) {
        pushValue(BsonDouble(value))
    }

    override fun encodeFloat(value: Float) {
        pushValue(BsonDouble(value.toDouble()))
    }

    override fun encodeLong(value: Long) {
        pushValue(BsonInt64(value))
    }

    override fun encodeShort(value: Short) {
        pushValue(BsonInt32(value.toInt()))
    }

    override fun encodeNull() {
        pushValue(BsonNull)
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        pushValue(BsonString(enumDescriptor.getElementName(index)))
    }

    abstract fun getCurrent(): BsonValue

    abstract fun pushValue(value: BsonValue)
}

internal class PrimitiveBsonEncoder(
    override val serializersModule: SerializersModule,
    nodeConsumer: (BsonValue) -> Unit
) : BsonEncoder(serializersModule, nodeConsumer) {
    private var bsonValue: BsonValue = BsonNull

    override fun pushValue(value: BsonValue) {
        bsonValue = value
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        // Fast path for mapping BsonBinary to ByteArray
        when(value) {
            is ByteArray -> pushValue(BsonBinary(value))
            else -> super.encodeSerializableValue(serializer, value)
        }
        nodeConsumer(getCurrent())
    }

    override fun getCurrent(): BsonValue = bsonValue
}

internal sealed class StructuredBsonEncoder(
    override val serializersModule: SerializersModule,
    nodeConsumer: (BsonValue) -> Unit
) : BsonEncoder(serializersModule, nodeConsumer) {
    protected var elements = BsonArray()

    override fun pushValue(value: BsonValue) {
        elements.add(value)
    }
}

internal class BsonArrayEncoder(
    override val serializersModule: SerializersModule,
    nodeConsumer: (BsonValue) -> Unit
) : StructuredBsonEncoder(serializersModule, nodeConsumer) {
    // we can point it out directly
    override fun getCurrent(): BsonValue = elements
}

internal open class BsonDocumentEncoder(
    override val serializersModule: SerializersModule,
    nodeConsumer: (BsonValue) -> Unit
) : StructuredBsonEncoder(serializersModule, nodeConsumer) {
    // We have all keys and fields in a plain list
    override fun getCurrent(): BsonValue = BsonDocument(
        elements
            .chunked(2)
            .associate { entry ->
                require(entry[0] is BsonString) { "TODO Required BsonString" }
                (entry[0] as BsonString).value to entry[1]
            }
    )
}

@OptIn(ExperimentalSerializationApi::class)
internal class BsonClassEncoder(
    override val serializersModule: SerializersModule,
    nodeConsumer: (BsonValue) -> Unit
) : BsonDocumentEncoder(serializersModule, nodeConsumer) {
    // Push field names
    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        elements.add(BsonString(descriptor.getElementName(index)))
        return super.encodeElement(descriptor, index)
    }
}
