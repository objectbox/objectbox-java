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

package io.objectbox.query;


import java.util.concurrent.Callable;

import io.objectbox.InternalAccess;
import io.objectbox.Property;

/**
 * Query for a specific property; create using {@link Query#property(Property)}.
 * Note: Property values do currently not consider any order defined for the main {@link Query} object
 * (subject to change in a future version).
 */
@SuppressWarnings("WeakerAccess") // WeakerAccess: allow inner class access without accessor
public class PropertyQuery {
    final Query query;
    final Property property;
    boolean distinct;
    boolean noCaseIfDistinct = true;

    PropertyQuery(Query query, Property property) {
        this.query = query;
        this.property = property;
    }

    /**
     * Only distinct values should be returned (e.g. 1,2,3 instead of 1,1,2,3,3,3).
     * <p>
     * Note: strings default to case-insensitive comparision;
     * to change that call {@link #distinct(QueryBuilder.StringOrder)}.
     */
    public PropertyQuery distinct() {
        distinct = true;
        return this;
    }

    /**
     * For string properties you can specify {@link io.objectbox.query.QueryBuilder.StringOrder#CASE_SENSITIVE} if you
     * want to have case sensitive distinct values (e.g. returning "foo","Foo","FOO" instead of "foo").
     */
    public PropertyQuery distinct(QueryBuilder.StringOrder stringOrder) {
        if (property.type != String.class) {
            throw new RuntimeException("Reserved for string properties, but got " + property);
        }
        distinct = true;
        noCaseIfDistinct = stringOrder == QueryBuilder.StringOrder.CASE_INSENSITIVE;
        return this;
    }

    /**
     * Find the values for the given string property for objects matching the query.
     * <p>
     * Note: null values are excluded from results.
     * <p>
     * Note: results are not guaranteed to be in any particular order.
     * <p>
     * See also: {@link #distinct}, {@link #distinct(QueryBuilder.StringOrder)}
     *
     * @return Found strings
     */
    public String[] findStrings() {
        return (String[]) query.callInReadTx(new Callable<String[]>() {
            @Override
            public String[] call() {
                long cursorHandle = InternalAccess.getActiveTxCursorHandle(query.box);
                boolean distinctNoCase = distinct && noCaseIfDistinct;
                return query.nativeFindStrings(query.handle, cursorHandle, property.id, distinct, distinctNoCase);
            }
        });
    }

    /**
     * Find the values for the given long property for objects matching the query.
     * <p>
     * Note: null values are excluded from results.
     * <p>
     * Note: results are not guaranteed to be in any particular order.
     * <p>
     * See also: {@link #distinct}
     *
     * @return Found longs
     */
    public long[] findLongs() {
        return (long[]) query.callInReadTx(new Callable<long[]>() {
            @Override
            public long[] call() {
                long cursorHandle = InternalAccess.getActiveTxCursorHandle(query.box);
                return query.nativeFindLongs(query.handle, cursorHandle, property.id, distinct);
            }
        });
    }

    /**
     * Find the values for the given int property for objects matching the query.
     * <p>
     * Note: null values are excluded from results.
     * <p>
     * Note: results are not guaranteed to be in any particular order.
     * <p>
     * See also: {@link #distinct}
     */
    public int[] findInts() {
        return (int[]) query.callInReadTx(new Callable<int[]>() {
            @Override
            public int[] call() {
                long cursorHandle = InternalAccess.getActiveTxCursorHandle(query.box);
                return query.nativeFindInts(query.handle, cursorHandle, property.id, distinct);
            }
        });
    }

    /**
     * Find the values for the given byte property for objects matching the query.
     * <p>
     * Note: null values are excluded from results.
     * <p>
     * Note: results are not guaranteed to be in any particular order.
     */
    public byte[] findBytes() {
        return (byte[]) query.callInReadTx(new Callable<byte[]>() {
            @Override
            public byte[] call() {
                long cursorHandle = InternalAccess.getActiveTxCursorHandle(query.box);
                return query.nativeFindBytes(query.handle, cursorHandle, property.id, distinct);
            }
        });
    }

}
