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
    boolean enableNull;

    double nullValueDouble;
    float nullValueFloat;
    String nullValueString;
    long nullValueLong;

    PropertyQuery(Query query, Property property) {
        this.query = query;
        this.property = property;
    }

    /** Clears all values (e.g. distinct and null value). */
    public PropertyQuery reset() {
        distinct = false;
        noCaseIfDistinct = true;
        enableNull = false;
        nullValueDouble = 0;
        nullValueFloat = 0;
        nullValueString = null;
        nullValueLong = 0;
        return this;
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

    public PropertyQuery nullValue(long nullValue) {
        enableNull = true;
        this.nullValueLong = nullValue;
        return this;
    }

    public PropertyQuery nullValue(float nullValue) {
        enableNull = true;
        this.nullValueFloat = nullValue;
        return this;
    }

    public PropertyQuery nullValue(double nullValue) {
        enableNull = true;
        this.nullValueDouble = nullValue;
        return this;
    }

    public PropertyQuery nullValue(String nullValue) {
        if (nullValue == null) {
            throw new IllegalArgumentException("Null strings are not allowed (yet)");
        }
        enableNull = true;
        this.nullValueString = nullValue;
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
                boolean distinctNoCase = distinct && noCaseIfDistinct;
                long cursorHandle = query.cursorHandle();
                return query.nativeFindStrings(query.handle, cursorHandle, property.id, distinct, distinctNoCase,
                        enableNull, nullValueString);
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
                return query.nativeFindLongs(query.handle, query.cursorHandle(), property.id, distinct,
                        enableNull, nullValueLong);
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
                return query.nativeFindInts(query.handle, query.cursorHandle(), property.id, distinct,
                        enableNull, (int) nullValueLong);
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
    public short[] findShorts() {
        return (short[]) query.callInReadTx(new Callable<short[]>() {
            @Override
            public short[] call() {
                return query.nativeFindShorts(query.handle, query.cursorHandle(), property.id, distinct,
                        enableNull, (short) nullValueLong);
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
    public char[] findChars() {
        return (char[]) query.callInReadTx(new Callable<char[]>() {
            @Override
            public char[] call() {
                return query.nativeFindChars(query.handle, query.cursorHandle(), property.id, distinct,
                        enableNull, (char) nullValueLong);
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
                return query.nativeFindBytes(query.handle, query.cursorHandle(), property.id, distinct,
                        enableNull, (byte) nullValueLong);
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
    public float[] findFloats() {
        return (float[]) query.callInReadTx(new Callable<float[]>() {
            @Override
            public float[] call() {
                return query.nativeFindFloats(query.handle, query.cursorHandle(), property.id, distinct,
                        enableNull, nullValueFloat);
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
    public double[] findDoubles() {
        return (double[]) query.callInReadTx(new Callable<double[]>() {
            @Override
            public double[] call() {
                return query.nativeFindDoubles(query.handle, query.cursorHandle(), property.id, distinct,
                        enableNull, nullValueDouble);
            }
        });
    }

    private String findString(final boolean unique) {
        return (String) query.callInReadTx(new Callable<String>() {
            @Override
            public String call() {
                boolean distinctCase = distinct && !noCaseIfDistinct;
                return query.nativeFindString(query.handle, query.cursorHandle(), property.id, unique, distinct,
                        distinctCase, enableNull, nullValueString);
            }
        });
    }

    public String findFirstString() {
        return findString(false);
    }

    public String findUniqueString() {
        return findString(true);
    }

    private Object findNumber(final boolean unique) {
        return query.callInReadTx(new Callable<Object>() {
            @Override
            public Object call() {
                return query.nativeFindNumber(query.handle, query.cursorHandle(), property.id, unique, distinct,
                        enableNull, nullValueLong, nullValueFloat, nullValueDouble);
            }
        });
    }

    public Long findFirstLong() {
        return (Long) findNumber(false);
    }

    public Long findUniqueLong() {
        return (Long) findNumber(true);
    }

    public Integer findFirstInt() {
        return (Integer) findNumber(false);
    }

    public Integer findUniqueInt() {
        return (Integer) findNumber(true);
    }

    public Short findFirstShort() {
        return (Short) findNumber(false);
    }

    public Short findUniqueShort() {
        return (Short) findNumber(true);
    }

    public Character findFirstChar() {
        return (Character) findNumber(false);
    }

    public Character findUniqueChar() {
        return (Character) findNumber(true);
    }

    public Byte findFirstByte() {
        return (Byte) findNumber(false);
    }

    public Byte findUniqueByte() {
        return (Byte) findNumber(true);
    }

    public Boolean findFirstBoolean() {
        return (Boolean) findNumber(false);
    }

    public Boolean findUniqueBoolean() {
        return (Boolean) findNumber(true);
    }

    public Float findFirstFloat() {
        return (Float) findNumber(false);
    }

    public Float findUniqueFloat() {
        return (Float) findNumber(true);
    }

    public Double findFirstDouble() {
        return (Double) findNumber(false);
    }

    public Double findUniqueDouble() {
        return (Double) findNumber(true);
    }

}
