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

import java.lang.reflect.Constructor
import java.util.Collections
import java.util.UUID
import kotlin.jvm.internal.DefaultConstructorMarker
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class BsonValueApiTest {

    @Test
    fun bsonArrayTest() {
        val bsonClass = org.bson.BsonArray::class.java
        val kBsonClass = BsonArray::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors = getConstructorParams(kBsonClass)
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass, listOf("parse", "asBsonReader"))
        val kBsonMethods = getMethodNames(kBsonClass, listOf("getSize", "removeAt"))
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonBinaryTest() {
        val bsonClass = org.bson.BsonBinary::class.java
        val kBsonClass = BsonBinary::class.java

        val bsonConstructors = getConstructorParams(bsonClass) { c -> c.parameterTypes.contains(UUID::class.java) }
        val kBsonConstructors = getConstructorParams(kBsonClass)

        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass, listOf("asUuid"))
        val kBsonMethods = getMethodNames(kBsonClass, listOf("clone"))

        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonBooleanTest() {
        val bsonClass = org.bson.BsonBoolean::class.java
        val kBsonClass = BsonBoolean::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors = getConstructorParams(kBsonClass)
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass)
        val kBsonMethods = getMethodNames(kBsonClass, listOf("getTRUE", "getFALSE"))

        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonDateTimeTest() {
        val bsonClass = org.bson.BsonDateTime::class.java
        val kBsonClass = BsonDateTime::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors = getConstructorParams(kBsonClass) { it.parameterTypes.isEmpty() }
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass)
        val kBsonMethods = getMethodNames(kBsonClass)
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonDBPointerTest() {
        val bsonClass = org.bson.BsonDbPointer::class.java
        val kBsonClass = BsonDBPointer::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        bsonConstructors.forEach { Collections.replaceAll(it, "ObjectId", "BsonObjectId") }
        val kBsonConstructors = getConstructorParams(kBsonClass)
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass)
        val kBsonMethods = getMethodNames(kBsonClass)
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonDecimal128Test() {
        val bsonClass = org.bson.BsonDecimal128::class.java
        val kBsonClass = BsonDecimal128::class.java

        // Note: Constructors differ as no Decimal128 underlying class or BigDecimal so not
        // asserting.

        val bsonMethods =
            getMethodNames(bsonClass, listOf("intValue", "longValue", "doubleValue", "getValue", "decimal128Value"))
        val kBsonMethods =
            getMethodNames(
                kBsonClass,
                listOf("isNaN", "isInfinite", "isFinite", "isNegative", "getHigh", "getLow", "getValue\$kbson"))
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonDocumentTest() {
        val bsonClass = org.bson.BsonDocument::class.java
        val kBsonClass = BsonDocument::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors =
            getConstructorParams(kBsonClass) { c ->
                c.parameterTypes.contains(Map::class.java) ||
                    c.parameterTypes.map { p -> p.componentType }.contains(Pair::class.java)
            }
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods =
            getMethodNames(bsonClass, listOf("parse", "toBsonDocument", "asBsonReader", "readObject", "writeReplace"))
        val kBsonMethods =
            getMethodNames(kBsonClass, listOf("getSize", "getEntries", "getKeys", "getValues", "toByteArray"))
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonDoubleTest() {
        val bsonClass = org.bson.BsonDouble::class.java
        val kBsonClass = BsonDouble::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors = getConstructorParams(kBsonClass)
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass, listOf("decimal128Value"))
        val kBsonMethods = getMethodNames(kBsonClass)
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonInt32Test() {
        val bsonClass = org.bson.BsonInt32::class.java
        val kBsonClass = BsonInt32::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors = getConstructorParams(kBsonClass)
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass, listOf("decimal128Value"))
        val kBsonMethods = getMethodNames(kBsonClass)
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonInt64Test() {
        val bsonClass = org.bson.BsonInt64::class.java
        val kBsonClass = BsonInt64::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors = getConstructorParams(kBsonClass)
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass, listOf("decimal128Value"))
        val kBsonMethods = getMethodNames(kBsonClass)
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonJavaScriptTest() {
        val bsonClass = org.bson.BsonJavaScript::class.java
        val kBsonClass = BsonJavaScript::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors = getConstructorParams(kBsonClass)
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass)
        val kBsonMethods = getMethodNames(kBsonClass)
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonJavaScriptWithScopeTest() {
        val bsonClass = org.bson.BsonJavaScriptWithScope::class.java
        val kBsonClass = BsonJavaScriptWithScope::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors = getConstructorParams(kBsonClass)
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass, listOf("clone"))
        val kBsonMethods = getMethodNames(kBsonClass)
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonMaxKeyTest() {
        val bsonClass = org.bson.BsonMaxKey::class.java
        val kBsonClass = BsonMaxKey::class.java

        // No constructor as it's a singleton object

        val bsonMethods = getMethodNames(bsonClass)
        val kBsonMethods = getMethodNames(kBsonClass)
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonMinKeyTest() {
        val bsonClass = org.bson.BsonMinKey::class.java
        val kBsonClass = BsonMinKey::class.java

        // No constructor as it's a singleton object

        val bsonMethods = getMethodNames(bsonClass)
        val kBsonMethods = getMethodNames(kBsonClass)
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonNullTest() {
        val bsonClass = org.bson.BsonNull::class.java
        val kBsonClass = BsonNull::class.java

        // No constructor as it's a singleton object

        val bsonMethods = getMethodNames(bsonClass)
        val kBsonMethods = getMethodNames(kBsonClass, listOf("getVALUE", "getVALUE\$annotations"))
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonObjectIdTest() {
        val bsonClass = org.bson.BsonObjectId::class.java
        val kBsonClass = BsonObjectId::class.java

        // Note: Constructors differ as no ObjectId underlying class so not asserting.

        val bsonMethods = getMethodNames(bsonClass, listOf("getValue"))
        val kBsonMethods =
            getMethodNames(
                kBsonClass,
                listOf(
                    "toHexString", "toByteArray", "getTimestamp", "getRandomValue1", "getRandomValue2", "getCounter"))
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonRegularExpressionTest() {
        val bsonClass = org.bson.BsonRegularExpression::class.java
        val kBsonClass = BsonRegularExpression::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors = getConstructorParams(kBsonClass)
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass)
        val kBsonMethods = getMethodNames(kBsonClass)
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonStringTest() {
        val bsonClass = org.bson.BsonString::class.java
        val kBsonClass = BsonString::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors = getConstructorParams(kBsonClass)
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass)
        val kBsonMethods = getMethodNames(kBsonClass)
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonSymbolTest() {
        val bsonClass = org.bson.BsonSymbol::class.java
        val kBsonClass = BsonSymbol::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors = getConstructorParams(kBsonClass)
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass)
        val kBsonMethods = getMethodNames(kBsonClass, listOf("getValue"))

        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonTimestamp() {
        val bsonClass = org.bson.BsonTimestamp::class.java
        val kBsonClass = BsonTimestamp::class.java

        val bsonConstructors = getConstructorParams(bsonClass)
        val kBsonConstructors = getConstructorParams(kBsonClass)
        assertEquals(bsonConstructors, kBsonConstructors)

        val bsonMethods = getMethodNames(bsonClass)
        val kBsonMethods = getMethodNames(kBsonClass)
        assertEquals(bsonMethods, kBsonMethods)
    }

    @Test
    fun bsonUndefinedTest() {
        val bsonClass = org.bson.BsonUndefined::class.java
        val kBsonClass = BsonUndefined::class.java

        // No constructor as it's a singleton object

        val bsonMethods = getMethodNames(bsonClass)
        val kBsonMethods = getMethodNames(kBsonClass, listOf("getUNDEFINED", "getUNDEFINED\$annotations"))
        assertEquals(bsonMethods, kBsonMethods)
    }

    private fun getConstructorParams(
        clazz: Class<*>,
        exclusions: (Constructor<*>) -> Boolean = { false }
    ): List<List<String>> {
        return clazz.constructors
            .filterNot { it.parameterTypes.contains(DefaultConstructorMarker::class.java) }
            .filterNot { exclusions.invoke(it) }
            .map { it.parameterTypes.map { p -> p.simpleName }.toList() }
            .sortedBy { it.toString() }
    }

    private fun getMethodNames(clazz: Class<*>, exclusions: List<String> = listOf()): Set<String> {
        val kotlinExtraBsonValueMethods =
            listOf(
                "isMaxKey",
                "isMinKey",
                "isUndefined",
                "asBsonNull",
                "asBsonMinKey",
                "asBsonMaxKey",
                "asBsonUndefined",
                "toJson")
        return clazz.methods
            .map { it.name }
            .filterNot {
                it.startsWith("access\$get") || exclusions.contains(it) || kotlinExtraBsonValueMethods.contains(it)
            }
            .toSet()
    }
}
