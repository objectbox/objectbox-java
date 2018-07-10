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

package io.objectbox.index.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;

@Entity
public class EntityLongIndex {

    @Id(assignable = true)
    long id;

    @Index
    long indexedLong;
    Float float1;
    Float float2;
    Float float3;
    Float float4;
    Float float5;

    public EntityLongIndex() {
    }

    public EntityLongIndex(long id, long indexedLong, Float float1, Float float2, Float float3, Float float4, Float float5) {
        this.id = id;
        this.indexedLong = indexedLong;
        this.float1 = float1;
        this.float2 = float2;
        this.float3 = float3;
        this.float4 = float4;
        this.float5 = float5;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getIndexedLong() {
        return indexedLong;
    }

    public void setIndexedLong(long indexedLong) {
        this.indexedLong = indexedLong;
    }

    public Float getFloat1() {
        return float1;
    }

    public void setFloat1(Float float1) {
        this.float1 = float1;
    }

    public Float getFloat2() {
        return float2;
    }

    public void setFloat2(Float float2) {
        this.float2 = float2;
    }

    public Float getFloat3() {
        return float3;
    }

    public void setFloat3(Float float3) {
        this.float3 = float3;
    }

    public Float getFloat4() {
        return float4;
    }

    public void setFloat4(Float float4) {
        this.float4 = float4;
    }

    public Float getFloat5() {
        return float5;
    }

    public void setFloat5(Float float5) {
        this.float5 = float5;
    }

    @Override
    public String toString() {
        return "EntityLongIndex (ID: " + id + ")";
    }
}
