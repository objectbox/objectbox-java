/*
 * Copyright 2020 ObjectBox Ltd.
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

/**
 * <a href="https://objectbox.io/sync/">ObjectBox Sync</a> allows to automatically synchronize local data with a sync
 * destination (e.g. a sync server) and vice versa. This is the sync <b>client</b> package.
 * <p>
 * These are the typical steps to set up a sync client:
 * <ol>
 *     <li>Create a BoxStore as usual (using MyObjectBox).</li>
 *     <li>Build a {@link io.objectbox.sync.SyncClient} using
 *     {@link io.objectbox.sync.Sync#client(io.objectbox.BoxStore) Sync.client(boxStore)}, a
 *     {@link io.objectbox.sync.SyncBuilder#url(java.lang.String)} and at least one set of credentials with
 *     {@link io.objectbox.sync.SyncBuilder#credentials(io.objectbox.sync.SyncCredentials)}.</li>
 *     <li>Optional: use the {@link io.objectbox.sync.SyncBuilder} instance from the last step to configure the sync
 *     client and set initial listeners.</li>
 *     <li>Call {@link io.objectbox.sync.SyncBuilder#buildAndStart()} to get an instance of
 *     {@link io.objectbox.sync.SyncClient} (and hold on to it). Synchronization is now active.</li>
 *     <li>Optional: Interact with {@link io.objectbox.sync.SyncClient}.</li>
 * </ol>
 */
@ParametersAreNonnullByDefault
package io.objectbox.sync;

import javax.annotation.ParametersAreNonnullByDefault;