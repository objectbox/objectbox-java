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

package io.objectbox.sync;

import io.objectbox.annotation.apihint.Internal;

/**
 * Internal sync access for ObjectBox platform integrations, e.g. platform-specific mesh networks.
 * This class is not part of the public API and may change without notice.
 */
@Internal
public final class InternalSyncAccess {

    /**
     * Creates a mesh sync configuration. See the {@link MeshConfig} setters for details on each
     * option.
     *
     * @param meshId The mesh network identifier (required); nodes with different IDs ignore each
     * other.
     */
    public static MeshConfigImpl createMeshConfig(String meshId) {
        return new MeshConfigImpl(meshId);
    }

    /**
     * Adds a platform-specific native network (transport) to a mesh config.
     *
     * @param networkInternalHandle an internal handle to a native mesh network created by a
     * platform library.
     */
    public static void addNetworkInternalHandle(MeshConfigImpl config, long networkInternalHandle) {
        config.networkInternalHandles.add(networkInternalHandle);
    }

    private InternalSyncAccess() {
    }

}
