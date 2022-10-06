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
package org.mongodb.kbson.corpus

import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

internal actual object ResourceLoader {

    actual fun readText(resourceName: String): String {
        val path = "src/commonTest/resources/$resourceName"
        val absolutePath =
            NSBundle.mainBundle.pathForResource(path.substringBeforeLast("."), path.substringAfterLast("."))
        return NSString.stringWithContentsOfFile(absolutePath, NSUTF8StringEncoding, null)!!
    }
}
