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

import java.util.Collection;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.query.QueryCondition;
import io.objectbox.query.QueryCondition.PropertyCondition;
import io.objectbox.query.QueryCondition.PropertyCondition.Operation;

/**
 * Meta data describing a property
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
    public QueryCondition eq(Object value) {
        return new PropertyCondition(this, Operation.EQUALS, value);
    }

    /** Creates an "not equal ('<>')" condition  for this property. */
    public QueryCondition notEq(Object value) {
        return new PropertyCondition(this, Operation.NOT_EQUALS, value);
    }

    /** Creates an "BETWEEN ... AND ..." condition  for this property. */
    public QueryCondition between(Object value1, Object value2) {
        Object[] values = {value1, value2};
        return new PropertyCondition(this, Operation.BETWEEN, values);
    }

    /** Creates an "IN (..., ..., ...)" condition  for this property. */
    public QueryCondition in(Object... inValues) {
        return new PropertyCondition(this, Operation.IN, inValues);
    }

    /** Creates an "IN (..., ..., ...)" condition  for this property. */
    public QueryCondition in(Collection<?> inValues) {
        return in(inValues.toArray());
    }

    /** Creates an "greater than ('>')" condition  for this property. */
    public QueryCondition gt(Object value) {
        return new PropertyCondition(this, Operation.GREATER_THAN, value);
    }

    /** Creates an "less than ('<')" condition  for this property. */
    public QueryCondition lt(Object value) {
        return new PropertyCondition(this, Operation.LESS_THAN, value);
    }


    /** Creates an "IS NULL" condition  for this property. */
    public QueryCondition isNull() {
        return new PropertyCondition(this, Operation.IS_NULL, null);
    }

    /** Creates an "IS NOT NULL" condition  for this property. */
    public QueryCondition isNotNull() {
        return new PropertyCondition(this, Operation.IS_NOT_NULL, null);
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
