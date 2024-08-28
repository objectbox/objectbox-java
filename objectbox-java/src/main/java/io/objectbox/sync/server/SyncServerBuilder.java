/*
 * Copyright 2019-2024 ObjectBox Ltd. All rights reserved.
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

package io.objectbox.sync.server;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.flatbuffers.FlatBufferBuilder;
import io.objectbox.sync.Credentials;
import io.objectbox.sync.SyncCredentials;
import io.objectbox.sync.SyncCredentialsToken;
import io.objectbox.sync.listener.SyncChangeListener;

/**
 * Creates a {@link SyncServer} and allows to set additional configuration.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@Experimental
public class SyncServerBuilder {

    final BoxStore boxStore;
    final String url;
    private final List<SyncCredentialsToken> credentials = new ArrayList<>();
    final List<PeerInfo> peers = new ArrayList<>();

    private @Nullable String certificatePath;
    SyncChangeListener changeListener;

    public SyncServerBuilder(BoxStore boxStore, String url, SyncCredentials authenticatorCredentials) {
        checkNotNull(boxStore, "BoxStore is required.");
        checkNotNull(url, "Sync server URL is required.");
        checkNotNull(authenticatorCredentials, "Authenticator credentials are required.");
        if (!BoxStore.isSyncServerAvailable()) {
            throw new IllegalStateException(
                    "This library does not include ObjectBox Sync Server. " +
                            "Please visit https://objectbox.io/sync/ for options.");
        }
        this.boxStore = boxStore;
        this.url = url;
        authenticatorCredentials(authenticatorCredentials);
    }

    /**
     * Sets the path to a directory that contains a cert.pem and key.pem file to use to establish encrypted
     * connections.
     * <p>
     * Use the "wss://" protocol for the server URL to turn on encrypted connections.
     */
    public SyncServerBuilder certificatePath(String certificatePath) {
        checkNotNull(certificatePath, "Certificate path must not be null");
        this.certificatePath = certificatePath;
        return this;
    }

    /**
     * Adds additional authenticator credentials to authenticate clients with.
     * <p>
     * For the embedded server, currently only {@link SyncCredentials#sharedSecret} and {@link SyncCredentials#none}
     * are supported.
     */
    public SyncServerBuilder authenticatorCredentials(SyncCredentials authenticatorCredentials) {
        checkNotNull(authenticatorCredentials, "Authenticator credentials must not be null.");
        if (!(authenticatorCredentials instanceof SyncCredentialsToken)) {
            throw new IllegalArgumentException("Sync credentials of type " + authenticatorCredentials.getType()
                    + " are not supported");
        }
        credentials.add((SyncCredentialsToken) authenticatorCredentials);
        return this;
    }

    /**
     * Sets a listener to observe fine granular changes happening during sync.
     * <p>
     * This listener can also be {@link SyncServer#setSyncChangeListener(SyncChangeListener) set or removed}
     * on the Sync server directly.
     */
    public SyncServerBuilder changeListener(SyncChangeListener changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    /**
     * Adds a server peer, to which this server should connect to as a client using {@link SyncCredentials#none()}.
     */
    public SyncServerBuilder peer(String url) {
        return peer(url, SyncCredentials.none());
    }

    /**
     * Adds a server peer, to which this server should connect to as a client using the given credentials.
     */
    public SyncServerBuilder peer(String url, SyncCredentials credentials) {
        if (!(credentials instanceof SyncCredentialsToken)) {
            throw new IllegalArgumentException("Sync credentials of type " + credentials.getType()
                    + " are not supported");
        }
        peers.add(new PeerInfo(url, (SyncCredentialsToken) credentials));
        return this;
    }

    /**
     * Builds and returns a Sync server ready to {@link SyncServer#start()}.
     * <p>
     * Note: this clears all previously set authenticator credentials.
     */
    public SyncServer build() {
        if (credentials.isEmpty()) {
            throw new IllegalStateException("At least one authenticator is required.");
        }
        return new SyncServerImpl(this);
    }

    /**
     * Builds, {@link SyncServer#start() starts} and returns a Sync server.
     */
    public SyncServer buildAndStart() {
        SyncServer syncServer = build();
        syncServer.start();
        return syncServer;
    }

    private void checkNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * From this configuration, builds a {@link SyncServerOptions} FlatBuffer and returns it as bytes.
     * <p>
     * Clears configured credentials, they can not be used again after this returns.
     */
    byte[] buildSyncServerOptions() {
        FlatBufferBuilder fbb = new FlatBufferBuilder();
        // Always put values, even if they match the default values (defined in the generated classes)
        fbb.forceDefaults(true);

        // Serialize non-integer values first to get their offset
        int urlOffset = fbb.createString(url);
        int certificatePathOffset = 0;
        if (certificatePath != null) {
            certificatePathOffset = fbb.createString(certificatePath);
        }
        int authenticationMethodsOffset = buildAuthenticationMethods(fbb);
        int clusterPeersVectorOffset = buildClusterPeers(fbb);

        // TODO Support remaining options
        // After collecting all offsets, create options
        SyncServerOptions.startSyncServerOptions(fbb);
        SyncServerOptions.addUrl(fbb, urlOffset);
        SyncServerOptions.addAuthenticationMethods(fbb, authenticationMethodsOffset);
//        SyncServerOptions.addSyncFlags();
//        SyncServerOptions.addSyncServerFlags();
        if (certificatePathOffset > 0) {
            SyncServerOptions.addCertificatePath(fbb, certificatePathOffset);
        }
//        SyncServerOptions.addWorkerThreads();
//        SyncServerOptions.addHistorySizeMaxKb();
//        SyncServerOptions.addHistorySizeTargetKb();
//        SyncServerOptions.addAdminUrl();
//        SyncServerOptions.addAdminThreads();
//        SyncServerOptions.addClusterId();
        if (clusterPeersVectorOffset > 0) {
            SyncServerOptions.addClusterPeers(fbb, clusterPeersVectorOffset);
        }
//        SyncServerOptions.addClusterFlags();
        int offset = SyncServerOptions.endSyncServerOptions(fbb);
        fbb.finish(offset);

        return fbb.sizedByteArray();
    }

    private int buildAuthenticationMethods(FlatBufferBuilder fbb) {
        int[] credentialsOffsets = new int[credentials.size()];
        for (int i = 0; i < credentials.size(); i++) {
            credentialsOffsets[i] = buildCredentials(fbb, credentials.get(i));
        }
        return SyncServerOptions.createAuthenticationMethodsVector(fbb, credentialsOffsets);
    }

    private int buildCredentials(FlatBufferBuilder fbb, SyncCredentialsToken tokenCredentials) {
        int tokenBytesOffset = 0;
        byte[] tokenBytes = tokenCredentials.getTokenBytes();
        if (tokenBytes != null) {
            tokenBytesOffset = Credentials.createBytesVector(fbb, tokenBytes);
        }

        Credentials.startCredentials(fbb);
        Credentials.addType(fbb, tokenCredentials.getTypeId());
        if (tokenBytesOffset > 0) {
            Credentials.addBytes(fbb, tokenBytesOffset);
        }
        int credentialsOffset = Credentials.endCredentials(fbb);

        tokenCredentials.clear(); // Clear immediately, not needed anymore.

        return credentialsOffset;
    }

    private int buildClusterPeers(FlatBufferBuilder fbb) {
        if (peers.isEmpty()) {
            return 0;
        }

        int[] peersOffsets = new int[peers.size()];
        for (int i = 0; i < peers.size(); i++) {
            PeerInfo peer = peers.get(i);

            int urlOffset = fbb.createString(peer.url);
            int credentialsOffset = buildCredentials(fbb, peer.credentials);

            peersOffsets[i] = ClusterPeerConfig.createClusterPeerConfig(fbb, urlOffset, credentialsOffset);
        }

        return SyncServerOptions.createClusterPeersVector(fbb, peersOffsets);
    }

}
