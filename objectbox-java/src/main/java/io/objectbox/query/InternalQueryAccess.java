/*
 * Copyright 2024 ObjectBox Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox.query;

import io.objectbox.annotation.apihint.Internal;

/**
 * This is a workaround to access internal APIs for tests.
 * <p>
 * To avoid this, future APIs should be exposed via interfaces with an internal implementation that can be used by
 * tests.
 */
@Internal
public class InternalQueryAccess {

    /**
     * For testing only.
     */
    public static <T> void nativeFindFirst(Query<T> query, long cursorHandle) {
        query.nativeFindFirst(query.handle, cursorHandle);
    }

}
