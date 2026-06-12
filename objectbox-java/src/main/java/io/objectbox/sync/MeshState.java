/*
 * Copyright 2026 ObjectBox Ltd.
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

package io.objectbox.sync;

import io.objectbox.annotation.apihint.Experimental;

/**
 * State of a {@link MeshSync} as returned by {@link MeshSync#getState()}.
 */
@Experimental
public enum MeshState {

    /**
     * State is unknown, e.g. the native API reported a state that's not recognized yet.
     */
    UNKNOWN(0),

    /**
     * Created, but not started yet.
     */
    CREATED(1),

    /**
     * Discovery is active (not enough peers connected yet).
     */
    DISCOVERING(2),

    /**
     * Fully connected (enough peers connected).
     */
    FULLY_CONNECTED(3),

    /**
     * Stopped.
     */
    STOPPED(4),

    /**
     * Stopped and being torn down.
     */
    DEAD(5);

    public final int id;

    MeshState(int id) {
        this.id = id;
    }

    public static MeshState fromId(int id) {
        for (MeshState value : values()) {
            if (value.id == id) return value;
        }
        return UNKNOWN;
    }

}
