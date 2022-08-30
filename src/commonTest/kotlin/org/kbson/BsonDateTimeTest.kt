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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.kbson.internal.CurrentTime.getCurrentTimeInMillis

class BsonDateTimeTest {

    private val bsonDateTime = BsonDateTime()

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { bsonDateTime.isDateTime() }
        assertEquals(BsonType.DATE_TIME, bsonDateTime.getBsonType())
    }

    @Test
    fun shouldHaveAccessToTheUnderlyingValues() {
        val bsonValue = BsonDateTime(0)
        assertEquals(0, bsonValue.value)
    }

    @Test
    fun shouldOverrideEquals() {
        assertEquals(BsonDateTime(0), BsonDateTime(0))
        assertNotEquals(BsonDateTime(1), BsonDateTime(0))
    }

    @Test
    fun shouldBeComparable() {
        val zeroBsonValue = BsonDateTime(0)
        val nowBsonValue = delayedBsonDateTime()
        val latestBsonValue = BsonDateTime(Long.MAX_VALUE)

        assertEquals(-1, zeroBsonValue.compareTo(nowBsonValue))
        assertEquals(0, zeroBsonValue.compareTo(zeroBsonValue))
        assertEquals(1, nowBsonValue.compareTo(zeroBsonValue))

        assertEquals(-1, bsonDateTime.compareTo(nowBsonValue))
        assertEquals(1, latestBsonValue.compareTo(nowBsonValue))
    }

    private fun delayedBsonDateTime(defaultTime: Long = bsonDateTime.value): BsonDateTime {
        // There is no sleep method so this busy loop
        // forces at least 1 ms difference between current times.
        var currentTime = defaultTime
        while (currentTime == defaultTime) {
            currentTime = getCurrentTimeInMillis()
        }
        return BsonDateTime()
    }
}
