/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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

package io.objectbox;

import java.io.Serializable;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;

@Internal
public interface EntityInfo<T> extends Serializable {
    String getEntityName();

    String getDbName();

    Class<T> getEntityClass();

    int getEntityId();

    Property<T>[] getAllProperties();

    Property<T> getIdProperty();

    IdGetter<T> getIdGetter();

    CursorFactory<T> getCursorFactory();

    // TODO replace reflection: BoxGetter<T> getBoxGetter();
}
