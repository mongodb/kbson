package org.mongodb.kbson.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import org.mongodb.kbson.BsonArray
import org.mongodb.kbson.BsonDocument
import org.mongodb.kbson.BsonInt32
import org.mongodb.kbson.BsonNull
import org.mongodb.kbson.BsonString
import org.mongodb.kbson.BsonValue

@OptIn(ExperimentalSerializationApi::class)
internal sealed class AbstractBsonEncoder(
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
            StructureKind.OBJECT -> TODO("Decide what to do")

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

    abstract fun getCurrent(): BsonValue

    abstract fun pushValue(value: BsonValue)
}

internal class PrimitiveBsonEncoder(
    override val serializersModule: SerializersModule,
    nodeConsumer: (BsonValue) -> Unit
) : AbstractBsonEncoder(serializersModule, nodeConsumer) {
    private var bsonValue: BsonValue = BsonNull

    override fun pushValue(value: BsonValue) {
        bsonValue = value
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        super.encodeSerializableValue(serializer, value)
        nodeConsumer(getCurrent())
    }

    override fun getCurrent(): BsonValue = bsonValue
}

internal sealed class StructuredAbstractBsonEncoder(
    override val serializersModule: SerializersModule,
    nodeConsumer: (BsonValue) -> Unit
) : AbstractBsonEncoder(serializersModule, nodeConsumer) {
    protected var elements = BsonArray()

    override fun pushValue(value: BsonValue) {
        elements.add(value)
    }
}

internal class BsonArrayEncoder(
    override val serializersModule: SerializersModule,
    nodeConsumer: (BsonValue) -> Unit
) : StructuredAbstractBsonEncoder(serializersModule, nodeConsumer) {
    // we can point it out directly
    override fun getCurrent(): BsonValue = elements
}

internal open class BsonDocumentEncoder(
    override val serializersModule: SerializersModule,
    nodeConsumer: (BsonValue) -> Unit
) : StructuredAbstractBsonEncoder(serializersModule, nodeConsumer) {
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
