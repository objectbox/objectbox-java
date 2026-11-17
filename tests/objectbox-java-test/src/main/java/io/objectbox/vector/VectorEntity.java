/*
 * Copyright 2026 ObjectBox Ltd. <https://objectbox.io>
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

package io.objectbox.vector;

import javax.annotation.Nullable;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * An entity with a float vector property; its HNSW index is defined by {@link MyObjectBox}.
 */
@Entity
public class VectorEntity {

    @Id
    long id;
    @Nullable
    String name;
    int category;
    @Nullable
    float[] vector;

    public VectorEntity() {
    }

    public VectorEntity(@Nullable String name, int category, @Nullable float[] vector) {
        this(0, name, category, vector);
    }

    /**
     * For the database: takes all properties.
     */
    public VectorEntity(long id, @Nullable String name, int category, @Nullable float[] vector) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.vector = vector;
    }

    public long getId() {
        return id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public int getCategory() {
        return category;
    }

    @Nullable
    public float[] getVector() {
        return vector;
    }

    @Override
    public String toString() {
        return "VectorEntity (ID: " + id + ", name: " + name + ")";
    }
}
