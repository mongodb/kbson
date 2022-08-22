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

class BsonNumberTest {

    @Test
    fun shouldConvertToInt() {
        assertEquals(1, BsonInt32(1).intValue())

        assertEquals(1, BsonInt64(1L).intValue())
        assertEquals(-1, BsonInt64(Long.MAX_VALUE).intValue())
        assertEquals(0, BsonInt64(Long.MIN_VALUE).intValue())

        assertEquals(3, BsonDouble(3.14).intValue())
    }

    @Test
    fun shouldConvertToLong() {
        assertEquals(1L, BsonInt32(1).longValue())
        assertEquals(1L, BsonInt64(1L).longValue())
        assertEquals(3L, BsonDouble(3.14).longValue())
    }

    @Test
    fun shouldConvertToDouble() {
        assertEquals(1.0, BsonInt32(1).doubleValue())
        assertEquals(1.0, BsonInt64(1L).doubleValue())

        assertEquals(9.223372036854776E18, BsonInt64(Long.MAX_VALUE).doubleValue())
        assertEquals(-9.223372036854776E18, BsonInt64(Long.MIN_VALUE).doubleValue())

        assertEquals(3.14, BsonDouble(3.14).doubleValue())
    }
}
