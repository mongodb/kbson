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

import kotlin.test.*

class BsonArrayTest {

    private val listOfBsonValues = listOf(BsonBoolean.TRUE, BsonBoolean.FALSE)
    private val bsonValue = BsonArray(listOfBsonValues)

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { bsonValue.isArray() }
        assertEquals(BsonType.ARRAY, BsonArray().getBsonType())
    }

    @Test
    fun shouldConstructAnEmptyArray() {
        val bsonValue = BsonArray()

        assertTrue(bsonValue.isEmpty())
        assertEquals(0, bsonValue.size)
        assertTrue(bsonValue.getValues().isEmpty())
    }

    @Test
    fun shouldConstructWithInitialCapacity() {
        val bsonValue = BsonArray(10)

        assertTrue(bsonValue.isEmpty())
        assertEquals(0, bsonValue.size)
        assertTrue(bsonValue.getValues().isEmpty())
    }

    @Test
    fun shouldConstructFromAList() {
        assertFalse(bsonValue.isEmpty())
        assertEquals(2, bsonValue.size)
        assertContentEquals(listOfBsonValues, bsonValue.getValues())
    }

    @Test
    fun shouldOverrideEquals() {
        assertEquals(BsonArray(), BsonArray())
        assertEquals(bsonValue, bsonValue)
        assertNotEquals(BsonArray(), bsonValue)
        assertNotEquals(bsonValue, BsonArray(listOf(BsonBoolean.TRUE, BsonBoolean.TRUE)))
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
            BsonArray(listOf(BsonBoolean.TRUE, BsonBoolean.TRUE, BsonBoolean.TRUE))
                .lastIndexOf(BsonBoolean.TRUE))
        assertEquals(1, bsonValue.indexOf(BsonBoolean.FALSE))
    }

    @Test
    fun cloneShouldMakeADeepCopyOfAllMutableBsonValueTypes() {
        val bsonArray =
            BsonArray(
                listOf(
                    BsonInt32(3),
                    BsonArray(listOf(BsonInt32(11))),
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
