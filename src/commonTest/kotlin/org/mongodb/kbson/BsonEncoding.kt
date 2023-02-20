package org.mongodb.kbson

import org.mongodb.kbson.serialization.Bson
import org.mongodb.kbson.serialization.encodeToBsonValue
import kotlin.test.Test

class BsonEncoding {

    class AllBsonTypes {
        val bsonArray: BsonArray = BsonArray()
    }

    @Test
    fun encoding() {
        val i = Bson.encodeToBsonValue(6)
        val s = Bson.encodeToBsonValue("hello world")
        val l = Bson.encodeToBsonValue(listOf(5, 6))
        val d = Bson.encodeToBsonValue(mapOf("hello world" to 6, "hello world2" to 7))
        val d2 = Bson.encodeToBsonValue(HelloWorld())
        val d3 = Bson.encodeToBsonValue(listOf(HelloWorld(), HelloWorld()))
        val d4 = Bson.encodeToBsonValue(BsonString("hello world"))
    }

}