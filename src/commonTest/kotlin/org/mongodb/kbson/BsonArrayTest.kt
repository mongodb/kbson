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

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BsonArrayTest {

    private val listOfBsonValues = listOf(BsonBoolean.TRUE, BsonBoolean.FALSE)
    private val bsonValue = org.mongodb.kbson.BsonArray(listOfBsonValues)

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { bsonValue.isArray() }
        assertEquals(BsonType.ARRAY, org.mongodb.kbson.BsonArray().bsonType)
    }

    @Test
    fun shouldConstructAnEmptyArray() {
        val bsonValue = org.mongodb.kbson.BsonArray()

        assertTrue(bsonValue.isEmpty())
        assertEquals(0, bsonValue.size)
        assertTrue(bsonValue.values.isEmpty())
    }

    @Test
    fun shouldConstructWithInitialCapacity() {
        val bsonValue = org.mongodb.kbson.BsonArray(10)

        assertTrue(bsonValue.isEmpty())
        assertEquals(0, bsonValue.size)
        assertTrue(bsonValue.values.isEmpty())
    }

    @Test
    fun shouldConstructFromAList() {
        assertFalse(bsonValue.isEmpty())
        assertEquals(2, bsonValue.size)
        assertContentEquals(listOfBsonValues, bsonValue.values)
    }

    @Test
    fun shouldOverrideEquals() {
        assertEquals(org.mongodb.kbson.BsonArray(), org.mongodb.kbson.BsonArray())
        assertEquals(bsonValue, bsonValue)
        assertNotEquals(org.mongodb.kbson.BsonArray(), bsonValue)
        assertNotEquals(bsonValue, org.mongodb.kbson.BsonArray(listOf(BsonBoolean.TRUE, BsonBoolean.TRUE)))
    }

    @Test
    fun shouldSupportListMethods() {
        assertTrue(bsonValue.contains(BsonBoolean.TRUE))
        assertFalse(bsonValue.contains(BsonUndefined.UNDEFINED))

        assertTrue(bsonValue.containsAll(listOf(BsonBoolean.TRUE)))
        assertFalse(bsonValue.containsAll(listOf(BsonBoolean.TRUE, BsonUndefined.UNDEFINED)))

        val iterator = bsonValue.iterator()
        assertTrue(iterator.hasNext())
        assertEquals(BsonBoolean.TRUE, iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(BsonBoolean.FALSE, iterator.next())
        assertFalse(iterator.hasNext())

        val listIterator = bsonValue.listIterator()
        assertFalse(listIterator.hasPrevious())
        assertTrue(listIterator.hasNext())
        assertEquals(BsonBoolean.TRUE, listIterator.next())
        assertEquals(BsonBoolean.TRUE, listIterator.previous())
        assertEquals(BsonBoolean.TRUE, listIterator.next())
        assertEquals(BsonBoolean.FALSE, listIterator.next())
        assertTrue(listIterator.hasPrevious())
        assertFalse(listIterator.hasNext())

        val subListIterator = bsonValue.listIterator(1)
        assertTrue(subListIterator.hasPrevious())
        assertTrue(subListIterator.hasNext())
        assertEquals(BsonBoolean.FALSE, subListIterator.next())
        assertTrue(subListIterator.hasPrevious())
        assertFalse(subListIterator.hasNext())

        assertEquals(mutableListOf<BsonValue>(BsonBoolean.FALSE), bsonValue.subList(1, 2))
        assertEquals(
            2,
            org.mongodb.kbson
                .BsonArray(listOf(BsonBoolean.TRUE, BsonBoolean.TRUE, BsonBoolean.TRUE))
                .lastIndexOf(BsonBoolean.TRUE))
        assertEquals(1, bsonValue.indexOf(BsonBoolean.FALSE))
    }

    @Test
    fun shouldSupportMutableListMethods() {
        val mutableList = org.mongodb.kbson.BsonArray()

        assertTrue(mutableList.add(BsonBoolean.TRUE))
        assertFalse(mutableList.isEmpty())
        assertTrue(mutableList.remove(BsonBoolean.TRUE))
        assertTrue(mutableList.isEmpty())

        assertTrue(mutableList.addAll(listOfBsonValues))
        assertFalse(mutableList.isEmpty())
        assertTrue(mutableList.removeAll(listOfBsonValues))
        assertTrue(mutableList.isEmpty())

        mutableList.addAll(listOf(BsonNull, BsonBoolean.TRUE, BsonBoolean.FALSE, BsonUndefined))
        assertEquals(BsonNull, mutableList.removeAt(0))
        assertEquals(3, mutableList.size)

        mutableList.add(0, BsonNull)
        assertEquals(4, mutableList.size)
        mutableList.add(BsonMaxKey)
        assertEquals(5, mutableList.size)

        assertEquals(BsonNull, mutableList.set(0, BsonMinKey))
        assertEquals(BsonMinKey, mutableList.first())

        val expected = listOf(BsonMinKey, BsonBoolean.TRUE, BsonBoolean.FALSE, BsonUndefined, BsonMaxKey)
        assertContentEquals(expected, mutableList.values)

        assertTrue(mutableList.retainAll(listOf(BsonMinKey, BsonMaxKey, BsonUndefined)))
        assertContentEquals(listOf(BsonMinKey, BsonUndefined, BsonMaxKey), mutableList.values)

        assertTrue(mutableList.addAll(1, listOf(BsonBoolean.TRUE, BsonBoolean.FALSE)))
        assertEquals(expected, mutableList.values)

        mutableList.clear()
        assertTrue(mutableList.isEmpty())
    }

    @Test
    fun cloneShouldMakeADeepCopyOfAllMutableBsonValueTypes() {
        val bsonArray =
            org.mongodb.kbson.BsonArray(
                listOf(
                    BsonInt32(3),
                    org.mongodb.kbson.BsonArray(listOf(BsonInt32(11))),
                    BsonDocument("i3", BsonInt32(6)),
                    BsonBinary(listOf(1.toByte(), 2.toByte(), 3.toByte()).toByteArray()),
                    BsonJavaScriptWithScope("code", BsonDocument("a", BsonInt32(4)))))

        val clonedArray = bsonArray.clone()

        assertEquals(bsonArray, clonedArray)
        assertNotSame(bsonArray, clonedArray)

        // Immutable types are the same
        assertSame(bsonArray[0], clonedArray[0])
        assertSame(bsonArray[3], clonedArray[3])
        assertSame(bsonArray[4], clonedArray[4])

        // Mutable types are copies
        assertNotSame(bsonArray[1], clonedArray[1])
        assertNotSame(bsonArray[2], clonedArray[2])
    }
}
