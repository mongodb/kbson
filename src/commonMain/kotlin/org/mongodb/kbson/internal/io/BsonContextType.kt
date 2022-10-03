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
package org.mongodb.kbson.internal.io

/** Used by BsonReader and BsonWriter implementations to represent the current context. */
internal enum class BsonContextType {
    /** The top level of a BSON document. */
    TOP_LEVEL,

    /** A (possibly embedded) BSON document. */
    DOCUMENT,

    /** A BSON array. */
    ARRAY,

    /** A JAVASCRIPT_WITH_SCOPE BSON value. */
    JAVASCRIPT_WITH_SCOPE,

    /** The scope document of a JAVASCRIPT_WITH_SCOPE BSON value. */
    SCOPE_DOCUMENT
}
