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
package org.kbson

/** A type-safe representation of the BSON array type. */
class BsonArray(initial: List<BsonValue> = emptyList()) : BsonValue(), List<BsonValue> {
    val values: MutableList<BsonValue>
    init {
        values = initial.toMutableList()
    }

    /**
     * Construct an empty BsonArray with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the BsonArray
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    constructor(initialCapacity: Int) : this(ArrayList<BsonValue>(initialCapacity))

    /** Creates and returns a deep copy of this object */
    fun clone(): BsonArray {
        val clonedValues =
            values.fold(ArrayList<BsonValue>(values.size)) { list, value ->
                if (value.isArray()) {
                    list.add(value.asArray().clone())
                } else if (value.isDocument()) {
                    list.add(value.asDocument().clone())
                } else {
                    list.add(value)
                }
                list
            }
        return BsonArray(clonedValues)
    }

    override fun getBsonType(): BsonType {
        return BsonType.ARRAY
    }

    override fun contains(element: BsonValue): Boolean {
        return values.contains(element)
    }

    override fun containsAll(elements: Collection<BsonValue>): Boolean {
        return values.containsAll(elements)
    }

    override fun get(index: Int): BsonValue {
        return values[index]
    }

    override fun isEmpty(): Boolean {
        return values.isEmpty()
    }

    override fun iterator(): Iterator<BsonValue> {
        return values.iterator()
    }

    override fun listIterator(): ListIterator<BsonValue> {
        return values.listIterator()
    }

    override fun listIterator(index: Int): ListIterator<BsonValue> {
        return values.listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<BsonValue> {
        return values.subList(fromIndex, toIndex)
    }

    override fun lastIndexOf(element: BsonValue): Int {
        return values.lastIndexOf(element)
    }

    override fun indexOf(element: BsonValue): Int {
        return values.indexOf(element)
    }

    override fun toString(): String {
        return "BsonArray{values=$values}"
    }

    override val size: Int
        get() = values.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonArray

        if (values != other.values) return false

        return true
    }

    override fun hashCode(): Int {
        return values.hashCode()
    }
}
