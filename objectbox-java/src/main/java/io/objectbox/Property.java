/*
 * Copyright (C) 2016 Markus Junginger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.query.WhereCondition.PropertyCondition;

/**
 * Meta data describing a property
 *
 * @author Markus
 */
public class Property {
    public final int ordinal;
    public final Class<?> type;
    public final String name;
    public final boolean primaryKey;
    public final String dbName;

    private int id;

    public Property(int ordinal, Class<?> type, String name, boolean primaryKey, String dbName) {
        this.ordinal = ordinal;
        this.type = type;
        this.name = name;
        this.primaryKey = primaryKey;
        this.dbName = dbName;
    }

    /** Creates an "equal ('=')" condition  for this property. */
    public PropertyCondition eq(Object value) {
        return new PropertyCondition(this, value);
    }

    @Internal
    public int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Property \"" + name + "\" (ID: " + id + ")";
    }
}
