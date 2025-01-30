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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.FeatureNotAvailableException;
import io.objectbox.flatbuffers.FlatBufferBuilder;
import io.objectbox.sync.Credentials;
import io.objectbox.sync.Sync;
import io.objectbox.sync.SyncCredentials;
import io.objectbox.sync.SyncCredentialsToken;
import io.objectbox.sync.SyncFlags;
import io.objectbox.sync.listener.SyncChangeListener;

/**
 * Creates a {@link SyncServer} and allows to set additional configuration.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class SyncServerBuilder {

    final BoxStore boxStore;
    final URI url;
    private final List<SyncCredentialsToken> credentials = new ArrayList<>();

    private @Nullable String certificatePath;
    SyncChangeListener changeListener;
    private @Nullable String clusterId;
    private final List<ClusterPeerInfo> clusterPeers = new ArrayList<>();
    private int clusterFlags;
    private long historySizeMaxKb;
    private long historySizeTargetKb;
    private int syncFlags;
    private int syncServerFlags;
    private int workerThreads;

    private String publicKey;
    private String publicKeyUrl;
    private String claimIss;
    private String claimAud;

    private static void checkFeatureSyncServerAvailable() {
        if (!BoxStore.isSyncServerAvailable()) {
            throw new FeatureNotAvailableException(
                    "This library does not include ObjectBox Sync Server. " +
                            "Please visit https://objectbox.io/sync/ for options.");
        }
    }

    /**
     * Use {@link Sync#server(BoxStore, String, SyncCredentials)} instead.
     */
    @Internal
    public SyncServerBuilder(BoxStore boxStore, String url, SyncCredentials authenticatorCredentials) {
        checkNotNull(boxStore, "BoxStore is required.");
        checkNotNull(url, "Sync server URL is required.");
        checkNotNull(authenticatorCredentials, "Authenticator credentials are required.");
        checkFeatureSyncServerAvailable();
        this.boxStore = boxStore;
        try {
            this.url = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Sync server URL is invalid: " + url, e);
        }
        authenticatorCredentials(authenticatorCredentials);
    }

    /**
     * Use {@link Sync#server(BoxStore, String, SyncCredentials)} instead.
     */
    @Internal
    public SyncServerBuilder(BoxStore boxStore, String url, SyncCredentials[] multipleAuthenticatorCredentials) {
        checkNotNull(boxStore, "BoxStore is required.");
        checkNotNull(url, "Sync server URL is required.");
        checkNotNull(multipleAuthenticatorCredentials, "Authenticator credentials are required.");
        checkFeatureSyncServerAvailable();
        this.boxStore = boxStore;
        try {
            this.url = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Sync server URL is invalid: " + url, e);
        }
        authenticatorCredentials(multipleAuthenticatorCredentials);
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
     * Adds additional authenticator credentials to authenticate clients or peers with.
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
     * Adds additional authenticator credentials to authenticate clients or peers with.
     * <p>
     * For the embedded server, currently only {@link SyncCredentials#sharedSecret} and {@link SyncCredentials#none}
     * are supported.
     */
    public SyncServerBuilder authenticatorCredentials(SyncCredentials[] multipleAuthenticatorCredentials) {
        checkNotNull(multipleAuthenticatorCredentials, "Authenticator credentials must not be null.");
        for (SyncCredentials credentials : multipleAuthenticatorCredentials) {
            authenticatorCredentials(credentials);
        }
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
     * Enables cluster mode (requires the Cluster feature) and associates this cluster peer with the given ID.
     * <p>
     * Cluster peers need to share the same ID to be in the same cluster.
     *
     * @see #clusterPeer(String, SyncCredentials)
     * @see #clusterFlags(int)
     */
    public SyncServerBuilder clusterId(String id) {
        checkNotNull(id, "Cluster ID must not be null");
        this.clusterId = id;
        return this;
    }

    /**
     * @deprecated Use {@link #clusterPeer(String, SyncCredentials) clusterPeer(url, SyncCredentials.none())} instead.
     */
    @Deprecated
    public SyncServerBuilder peer(String url) {
        return clusterPeer(url, SyncCredentials.none());
    }

    /**
     * @deprecated Use {@link #clusterPeer(String, SyncCredentials)} instead.
     */
    @Deprecated
    public SyncServerBuilder peer(String url, SyncCredentials credentials) {
        return clusterPeer(url, credentials);
    }

    /**
     * Adds a (remote) cluster peer, to which this server should connect to as a client using the given credentials.
     * <p>
     * To use this, must set a {@link #clusterId(String)}.
     */
    public SyncServerBuilder clusterPeer(String url, SyncCredentials credentials) {
        if (!(credentials instanceof SyncCredentialsToken)) {
            throw new IllegalArgumentException("Sync credentials of type " + credentials.getType()
                    + " are not supported");
        }
        clusterPeers.add(new ClusterPeerInfo(url, (SyncCredentialsToken) credentials));
        return this;
    }

    /**
     * Sets bit flags to configure the cluster behavior of the Sync server (aka cluster peer).
     * <p>
     * To use this, must set a {@link #clusterId(String)}.
     *
     * @param flags One or more of {@link ClusterFlags}.
     */
    public SyncServerBuilder clusterFlags(int flags) {
        this.clusterFlags = flags;
        return this;
    }

    /**
     * Sets the maximum transaction history size.
     * <p>
     * Once the maximum size is reached, old transaction logs are deleted to stay below this limit. This is sometimes
     * also called "history pruning" in the context of Sync.
     * <p>
     * If not set or set to 0, defaults to no limit.
     *
     * @see #historySizeTargetKb(long)
     */
    public SyncServerBuilder historySizeMaxKb(long historySizeMaxKb) {
        this.historySizeMaxKb = historySizeMaxKb;
        return this;
    }

    /**
     * Sets the target transaction history size.
     * <p>
     * Once the maximum size ({@link #historySizeMaxKb(long)}) is reached, old transaction logs are deleted until this
     * size target is reached (lower than the maximum size). Using this target size typically lowers the frequency of
     * history pruning and thus may improve efficiency.
     * <p>
     * If not set or set to 0, defaults to {@link #historySizeMaxKb(long)}.
     */
    public SyncServerBuilder historySizeTargetKb(long historySizeTargetKb) {
        this.historySizeTargetKb = historySizeTargetKb;
        return this;
    }

    /**
     * Sets bit flags to adjust Sync behavior, like additional logging.
     *
     * @param syncFlags One or more of {@link SyncFlags}.
     */
    public SyncServerBuilder syncFlags(int syncFlags) {
        this.syncFlags = syncFlags;
        return this;
    }

    /**
     * Sets bit flags to configure the Sync server.
     *
     * @param syncServerFlags One or more of {@link SyncServerFlags}.
     */
    public SyncServerBuilder syncServerFlags(int syncServerFlags) {
        this.syncServerFlags = syncServerFlags;
        return this;
    }

    /**
     * Sets the number of workers for the main task pool.
     * <p>
     * If not set or set to 0, this uses a hardware-dependant default, e.g. 3 * CPU "cores".
     */
    public SyncServerBuilder workerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
        return this;
    }

    /**
     * Set the public key used to verify JWT tokens.
     * <p>
     * The public key should be in the PEM format.
     */
    public SyncServerBuilder jwtConfigPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    /**
     * Set the JWKS (Json Web Key Sets) URL to fetch the current public key used to verify JWT tokens.
     */
    public SyncServerBuilder jwtConfigPublicKeyUrl(String publicKeyUrl) {
        this.publicKeyUrl = publicKeyUrl;
        return this;
    }

    /**
     * Set the JWT claim "iss" (issuer) used to verify JWT tokens.
     */
    public SyncServerBuilder jwtConfigClaimIss(String claimIss) {
        this.claimIss = claimIss;
        return this;
    }

    /**
     * Set the JWT claim "aud" (audience) used to verify JWT tokens.
     */
    public SyncServerBuilder jwtConfigClaimAud(String claimAud) {
        this.claimAud = claimAud;
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
        if (!clusterPeers.isEmpty() || clusterFlags != 0) {
            checkNotNull(clusterId, "Cluster ID must be set to use cluster features.");
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
        int urlOffset = fbb.createString(url.toString());
        int certificatePathOffset = 0;
        if (certificatePath != null) {
            certificatePathOffset = fbb.createString(certificatePath);
        }
        int clusterIdOffset = 0;
        if (clusterId != null) {
            clusterIdOffset = fbb.createString(clusterId);
        }
        int authenticationMethodsOffset = buildAuthenticationMethods(fbb);
        int clusterPeersVectorOffset = buildClusterPeers(fbb);
        int jwtConfigOffset = 0;
        if (publicKey != null || publicKeyUrl != null) {
            if (claimAud == null) {
                throw new IllegalArgumentException("claimAud must be set");
            }
            if (claimIss == null) {
                throw new IllegalArgumentException("claimIss must be set");
            }
            jwtConfigOffset = buildJwtConfig(fbb, publicKey, publicKeyUrl, claimIss, claimAud);
        }
        // Clear credentials immediately to make abuse less likely,
        // but only after setting all options to allow (re-)using the same credentials object
        // for authentication and cluster peers login credentials.
        for (SyncCredentialsToken credential : credentials) {
            credential.clear();
        }
        for (ClusterPeerInfo peer : clusterPeers) {
            peer.credentials.clear();
        }

        // After collecting all offsets, create options
        SyncServerOptions.startSyncServerOptions(fbb);
        SyncServerOptions.addUrl(fbb, urlOffset);
        SyncServerOptions.addAuthenticationMethods(fbb, authenticationMethodsOffset);
        if (syncFlags != 0) {
            SyncServerOptions.addSyncFlags(fbb, syncFlags);
        }
        if (syncServerFlags != 0) {
            SyncServerOptions.addSyncFlags(fbb, syncServerFlags);
        }
        if (certificatePathOffset != 0) {
            SyncServerOptions.addCertificatePath(fbb, certificatePathOffset);
        }
        if (workerThreads != 0) {
            SyncServerOptions.addWorkerThreads(fbb, workerThreads);
        }
        if (historySizeMaxKb != 0) {
            SyncServerOptions.addHistorySizeMaxKb(fbb, historySizeMaxKb);
        }
        if (historySizeTargetKb != 0) {
            SyncServerOptions.addHistorySizeTargetKb(fbb, historySizeTargetKb);
        }
        if (clusterIdOffset != 0) {
            SyncServerOptions.addClusterId(fbb, clusterIdOffset);
        }
        if (clusterPeersVectorOffset != 0) {
            SyncServerOptions.addClusterPeers(fbb, clusterPeersVectorOffset);
        }
        if (clusterFlags != 0) {
            SyncServerOptions.addClusterFlags(fbb, clusterFlags);
        }
        if (jwtConfigOffset != 0) {
            SyncServerOptions.addJwtConfig(fbb, jwtConfigOffset);
        }
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
        if (tokenBytesOffset != 0) {
            Credentials.addBytes(fbb, tokenBytesOffset);
        }
        return Credentials.endCredentials(fbb);
    }

    private int buildJwtConfig(FlatBufferBuilder fbb, @Nullable String publicKey, @Nullable String publicKeyUrl, String claimIss, String claimAud) {
        if (publicKey == null && publicKeyUrl == null) {
            throw new IllegalArgumentException("Either publicKey or publicKeyUrl must be set");
        }
        int publicKeyOffset = 0;
        int publicKeyUrlOffset = 0;
        if (publicKey != null) {
            publicKeyOffset = fbb.createString(publicKey);
        } else {
            publicKeyUrlOffset = fbb.createString(publicKeyUrl);
        }
        int claimIssOffset = fbb.createString(claimIss);
        int claimAudOffset = fbb.createString(claimAud);
        JwtConfig.startJwtConfig(fbb);
        if (publicKeyOffset != 0) {
            JwtConfig.addPublicKey(fbb, publicKeyOffset);
        } else {
            JwtConfig.addPublicKeyUrl(fbb, publicKeyUrlOffset);
        }
        JwtConfig.addClaimIss(fbb, claimIssOffset);
        JwtConfig.addClaimAud(fbb, claimAudOffset);
        return JwtConfig.endJwtConfig(fbb);
    }

    private int buildClusterPeers(FlatBufferBuilder fbb) {
        if (clusterPeers.isEmpty()) {
            return 0;
        }

        int[] peersOffsets = new int[clusterPeers.size()];
        for (int i = 0; i < clusterPeers.size(); i++) {
            ClusterPeerInfo peer = clusterPeers.get(i);

            int urlOffset = fbb.createString(peer.url);
            int credentialsOffset = buildCredentials(fbb, peer.credentials);

            peersOffsets[i] = ClusterPeerConfig.createClusterPeerConfig(fbb, urlOffset, credentialsOffset);
        }

        return SyncServerOptions.createClusterPeersVector(fbb, peersOffsets);
    }

}
