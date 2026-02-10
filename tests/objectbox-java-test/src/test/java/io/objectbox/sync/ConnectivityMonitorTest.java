/*
 * Copyright 2025 ObjectBox Ltd.
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

import org.junit.Test;

import java.util.List;

import javax.annotation.Nullable;

import io.objectbox.sync.listener.SyncChangeListener;
import io.objectbox.sync.listener.SyncCompletedListener;
import io.objectbox.sync.listener.SyncConnectionListener;
import io.objectbox.sync.listener.SyncListener;
import io.objectbox.sync.listener.SyncLoginListener;
import io.objectbox.sync.listener.SyncTimeListener;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ConnectivityMonitorTest {

    @Test
    public void reactsToObserverChanges() {
        TestSyncClient testSyncClient = new TestSyncClient();
        TestConnectivityMonitor testMonitor = new TestConnectivityMonitor();

        // No observer set.
        testMonitor.removeObserver();
        assertEquals(0, testMonitor.onObserverSetCalled);
        assertEquals(1, testMonitor.onObserverRemovedCalled);

        testMonitor.reset();

        testMonitor.setObserver(testSyncClient);
        assertEquals(1, testMonitor.onObserverSetCalled);
        assertEquals(0, testMonitor.onObserverRemovedCalled);

        testMonitor.reset();

        testMonitor.removeObserver();
        assertEquals(0, testMonitor.onObserverSetCalled);
        assertEquals(1, testMonitor.onObserverRemovedCalled);
    }

    @Test
    public void settingNullObserverFails() {
        TestConnectivityMonitor testMonitor = new TestConnectivityMonitor();

        //noinspection ConstantConditions Ignore NotNull annotation on purpose.
        assertThrows(IllegalArgumentException.class, () -> testMonitor.setObserver(null));
    }

    @Test
    public void notifiesObserversOnlyIfSet() {
        TestSyncClient testSyncClient = new TestSyncClient();
        TestConnectivityMonitor testMonitor = new TestConnectivityMonitor();

        testMonitor.setObserver(testSyncClient);
        testMonitor.notifyConnectionAvailable();
        assertEquals(1, testSyncClient.notifyConnectionAvailableCalled);

        testSyncClient.reset();

        testMonitor.removeObserver();
        testMonitor.notifyConnectionAvailable();
        assertEquals(0, testSyncClient.notifyConnectionAvailableCalled);
    }

    private static class TestConnectivityMonitor extends ConnectivityMonitor {

        int onObserverSetCalled;
        int onObserverRemovedCalled;

        void reset() {
            onObserverSetCalled = 0;
            onObserverRemovedCalled = 0;
        }

        @Override
        public void onObserverSet() {
            onObserverSetCalled += 1;
        }

        @Override
        public void onObserverRemoved() {
            onObserverRemovedCalled += 1;
        }
    }

    private static class TestSyncClient implements SyncClient {

        int notifyConnectionAvailableCalled;

        void reset() {
            notifyConnectionAvailableCalled = 0;
        }

        @Override
        public String getServerUrl() {
            return null;
        }

        @Override
        public List<String> getUrls() {
            return null;
        }

        @Override
        public boolean isStarted() {
            return false;
        }

        @Override
        public boolean isLoggedIn() {
            return false;
        }

        @Override
        public long getLastLoginCode() {
            return 0;
        }

        @Override
        public long getServerTimeNanos() {
            return 0;
        }

        @Override
        public long getServerTimeDiffNanos() {
            return 0;
        }

        @Override
        public long getRoundtripTimeNanos() {
            return 0;
        }

        @Override
        public void setSyncLoginListener(@Nullable SyncLoginListener listener) {
        }

        @Override
        public void setSyncCompletedListener(@Nullable SyncCompletedListener listener) {
        }

        @Override
        public void setSyncConnectionListener(@Nullable SyncConnectionListener listener) {
        }

        @Override
        public void setSyncListener(@Nullable SyncListener listener) {
        }

        @Override
        public void setSyncChangeListener(@Nullable SyncChangeListener listener) {
        }

        @Override
        public void setSyncTimeListener(@Nullable SyncTimeListener timeListener) {
        }

        @Override
        public void putFilterVariable(String name, String value) {
        }

        @Override
        public void removeFilterVariable(String name) {
        }

        @Override
        public void removeAllFilterVariables() {
        }

        @Override
        public void setLoginCredentials(SyncCredentials credentials) {
        }

        @Override
        public void setLoginCredentials(List<SyncCredentials> credentials) {
        }

        @Override
        public void setLoginCredentials(SyncCredentials[] multipleCredentials) {
        }

        @Override
        public boolean awaitFirstLogin(long millisToWait) {
            return false;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void close() {
        }

        @Override
        public boolean requestUpdates() {
            return false;
        }

        @Override
        public boolean requestUpdatesOnce() {
            return false;
        }

        @Override
        public boolean cancelUpdates() {
            return false;
        }

        @Override
        public boolean requestFullSync() {
            return false;
        }

        @Override
        public void notifyConnectionAvailable() {
            notifyConnectionAvailableCalled += 1;
        }

        @Override
        public ObjectsMessageBuilder startObjectsMessage(long flags, @Nullable String topic) {
            return null;
        }
    }

}
