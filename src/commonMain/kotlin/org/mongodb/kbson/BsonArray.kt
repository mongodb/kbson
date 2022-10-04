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
package org.mongodb.kbson

/**
 * A type-safe representation of the BSON array type.
 *
 * @constructor constructs the bson array with an initial list of values, defaults to no items
 * @param initial the initial list of [BsonValue]s in the array
 */
public class BsonArray(initial: List<BsonValue> = emptyList()) : BsonValue(), MutableList<BsonValue> {
    private val _values: MutableList<BsonValue>
    init {
        _values = initial.toMutableList()
    }

    /**
     * Construct an empty BsonArray with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the BsonArray
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    public constructor(initialCapacity: Int) : this(ArrayList<BsonValue>(initialCapacity))

    /** The values in this array as a list of BsonValue objects. */
    public val values: List<BsonValue>
        get() = _values.toList()

    /** Creates and returns a deep copy of this object */
    public fun clone(): BsonArray {
        val clonedValues =
            _values.fold(ArrayList<BsonValue>(_values.size)) { list, value ->
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

    override val bsonType: BsonType
        get() = BsonType.ARRAY

    override fun contains(element: BsonValue): Boolean {
        return _values.contains(element)
    }

    override fun containsAll(elements: Collection<BsonValue>): Boolean {
        return _values.containsAll(elements)
    }

    override fun get(index: Int): BsonValue {
        return _values[index]
    }

    override fun isEmpty(): Boolean {
        return _values.isEmpty()
    }

    override fun iterator(): MutableIterator<BsonValue> {
        return _values.iterator()
    }

    override fun listIterator(): MutableListIterator<BsonValue> {
        return _values.listIterator()
    }

    override fun listIterator(index: Int): MutableListIterator<BsonValue> {
        return _values.listIterator(index)
    }

    override fun remove(element: BsonValue): Boolean {
        return _values.remove(element)
    }

    override fun removeAll(elements: Collection<BsonValue>): Boolean {
        return _values.removeAll(elements)
    }

    override fun removeAt(index: Int): BsonValue {
        return _values.removeAt(index)
    }

    override fun retainAll(elements: Collection<BsonValue>): Boolean {
        return _values.retainAll(elements)
    }

    override fun set(index: Int, element: BsonValue): BsonValue {
        return _values.set(index, element)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<BsonValue> {
        return _values.subList(fromIndex, toIndex)
    }

    override fun lastIndexOf(element: BsonValue): Int {
        return _values.lastIndexOf(element)
    }

    override fun indexOf(element: BsonValue): Int {
        return _values.indexOf(element)
    }

    override fun toString(): String {
        return "BsonArray(values=${_values.joinToString(",", "[", "]")})"
    }

    override val size: Int
        get() = _values.size

    override fun clear() {
        _values.clear()
    }

    override fun addAll(elements: Collection<BsonValue>): Boolean {
        return _values.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<BsonValue>): Boolean {
        return _values.addAll(index, elements)
    }

    override fun add(index: Int, element: BsonValue) {
        _values.add(index, element)
    }

    override fun add(element: BsonValue): Boolean {
        return _values.add(element)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BsonArray

        if (_values != other._values) return false

        return true
    }

    override fun hashCode(): Int {
        return _values.hashCode()
    }
}
