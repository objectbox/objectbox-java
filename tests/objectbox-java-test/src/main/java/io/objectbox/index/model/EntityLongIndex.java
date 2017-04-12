package io.objectbox.index.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
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

    @Generated(hash = 37687253)
    public EntityLongIndex() {
    }

    @Generated(hash = 2116856237)
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
