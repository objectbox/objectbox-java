/*
 * Copyright 2021 ObjectBox Ltd.
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

package io.objectbox.tree;

import io.objectbox.annotation.apihint.Experimental;

/**
 * (Potentially internal) value object created in our JNI layer to represent a leaf with all stored data.
 * Note that only one of the value properties is actually set for any node.
 */
@Experimental
public class LeafNode {
    final public long id;
    final public long branchId;
    final public long metaId;

    public long integerValue;
    public double floatingValue;

    /**
     * One of String, byte[], String[]
     */
    public Object objectValue;

    /**
     * See {@link io.objectbox.model.PropertyType} for values.
     * Attention: does not represent the type accurately yet:
     * 1) Strings are Bytes, 2) all integer type are Long, 3) all FPs are Double.
     */
    public short valueType;

    /**
     * All-args constructor used by JNI (don't change, it's actually used).
     */
    public LeafNode(long id, long branchId, long metaId, long integerValue, double floatingValue, Object objectValue,
                    short valueType) {
        this.id = id;
        this.branchId = branchId;
        this.metaId = metaId;
        this.integerValue = integerValue;
        this.floatingValue = floatingValue;
        this.objectValue = objectValue;
        this.valueType = valueType;
    }
}
