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

class BsonJavaScriptWithScopeTest {

    private val code = "function() {}"
    private val scope = BsonDocument()
    private val bsonValue = BsonJavaScriptWithScope(code, scope)

    @Test
    fun shouldHaveTheExpectedBsonType() {
        assertTrue { bsonValue.isJavaScriptWithScope() }
        assertEquals(BsonType.JAVASCRIPT_WITH_SCOPE, bsonValue.getBsonType())
    }

    @Test
    fun shouldHaveAccessToTheUnderlyingValues() {
        assertEquals(code, bsonValue.code)
        assertEquals(scope, bsonValue.scope)
    }

    @Test
    fun shouldOverrideEquals() {
        assertEquals(bsonValue, BsonJavaScriptWithScope(code, scope))
        assertNotEquals(bsonValue, BsonJavaScriptWithScope("function() { return false;}", scope))
    }
}
