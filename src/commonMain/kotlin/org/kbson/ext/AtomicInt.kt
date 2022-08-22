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
package org.kbson.ext

/** Cross-platform atomic integer */
expect class AtomicInt(value: Int) {

    /** Gets the current value */
    fun getValue(): Int

    /**
     * Increments the value by [delta] and returns the new value.
     *
     * @param delta the value to add
     * @return the new value
     */
    fun addAndGet(delta: Int): Int

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     *
     * @param expected the expected value
     * @param new the new value
     * @return true if successful
     */
    fun compareAndSet(expected: Int, new: Int): Boolean

    /** Increments value by one. */
    fun increment()

    /** Decrements value by one. */
    fun decrement()
}
