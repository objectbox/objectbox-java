/*
 * Copyright 2020 ObjectBox Ltd.
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

import io.objectbox.Property;

/**
 * A condition on a {@link Property}, which can have an alias to allow referring to it later.
 */
public interface PropertyQueryCondition<T> extends QueryCondition<T> {

    /**
     * Assigns an alias to this condition that can later be used with the {@link Query} setParameter methods.
     */
    QueryCondition<T> alias(String name);

}
