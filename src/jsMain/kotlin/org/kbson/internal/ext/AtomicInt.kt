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
package org.kbson.internal.ext

/**
 * Js version of an atomic integer
 *
 * In JavaScript, a function always runs to completion, before any other function is called. So
 * there no locking required to protect the int.
 */
internal actual class AtomicInt actual constructor(value: Int) {

    private var atomicInt: Int

    init {
        atomicInt = value
    }

    /** Gets the current value */
    actual fun getValue(): Int = atomicInt
    /**
     * Increments the value by [delta] and returns the new value.
     *
     * @param delta the value to add
     * @return the new value
     */
    actual fun addAndGet(delta: Int): Int {
        atomicInt += delta
        return atomicInt
    }

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     *
     * @param expected the expected value
     * @param new the new value
     * @return true if successful
     */
    actual fun compareAndSet(expected: Int, new: Int): Boolean {
        if (atomicInt == expected) {
            atomicInt = new
            return true
        }
        return false
    }

    /** Increments value by one. */
    actual fun increment() {
        atomicInt++
    }

    /** Decrements value by one. */
    actual fun decrement() {
        atomicInt--
    }
}
