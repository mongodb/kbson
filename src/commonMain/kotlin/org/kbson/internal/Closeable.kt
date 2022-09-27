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
package org.kbson.internal

/**
 * A Closeable is a source or destination of data that can be closed.
 *
 * The close method is invoked to release resources that the object is holding.
 */
public interface Closeable {

    /**
     * Closes this stream and releases any system resources associated with it.
     *
     * If the stream is already closed then invoking this method has no effect.
     */
    public fun close()
}

/**
 * Executes the given [block] function on this resource and then closes it down correctly whether an exception is thrown
 * or not.
 *
 * @param block a function to process this [Closeable] resource.
 * @return the result of [block] function invoked on this resource.
 */
public inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        this?.close()
    }
}
