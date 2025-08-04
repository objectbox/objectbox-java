/*
 * Copyright 2019-2020 ObjectBox Ltd.
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

package io.objectbox.sync.listener;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.SyncChange;

/**
 * Notifies of fine granular changes on the object level happening during sync.
 * Register your listener using {@link io.objectbox.sync.SyncBuilder#changeListener(SyncChangeListener) SyncBuilder.changesListener(SyncChangesListener)}.
 * Note that enabling fine granular notification can slightly reduce performance.
 * <p>
 * See also {@link SyncListener} for the general sync listener.
 */
@SuppressWarnings({"unused"})
@Experimental
public interface SyncChangeListener {

    // Note: this method is expected by JNI, check before modifying/removing it.

    /**
     * Called each time when data from sync was applied locally.
     *
     * @param syncChanges This contains the entity type (schema) ID, the removed IDs and the put IDs for that entity.
     */
    void onSyncChanges(SyncChange[] syncChanges);

}
