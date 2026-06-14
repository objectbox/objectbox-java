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

package io.objectbox.android.internal.meshsync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Android implementation of mesh network using Google Nearby Connections API.
 * <p>
 * The package and member names must match the JNI exports of the ObjectBox Android (Sync) native
 * library (AndroidMeshNetworkJni.cpp), do not rename.
 * <p>
 * Life-cycle note: stop() must be called to release native resources;
 * this typically happens from the native side when the native component is stopped.
 *
 * <p>Internal class - do not use directly.</p>
 */
public class NearbyMeshNetwork {
    private static final String TAG = "ObxMesh";

    /**
     * Used for advertising and discovery:
     * we must use CLUSTER strategy as we have an M-to-N mesh (>= 1 outgoing and >= 1 incoming connections).
     * See also <a href="https://developers.google.com/nearby/connections/strategies">Nearby Connections strategies</a>.
     */
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    /**
     * Pointer to the paired C++ AndroidMeshNetwork (C++ class name retained); 0 after {@link #stop()}.
     */
    private volatile long nativeHandle;
    private final String serviceId;
    private final ConnectionsClient connectionsClient;
    private boolean isAdvertising = false;
    private boolean isDiscovering = false;

    // Stat counters for testing/diagnostics
    private final AtomicInteger endpointsFound = new AtomicInteger();
    private final AtomicInteger endpointsLost = new AtomicInteger();
    private final AtomicInteger connectionsInitiated = new AtomicInteger();
    private final AtomicInteger connectionsSucceeded = new AtomicInteger();
    private final AtomicInteger connectionsFailed = new AtomicInteger();
    private final AtomicInteger disconnections = new AtomicInteger();
    private final AtomicInteger payloadsReceived = new AtomicInteger();
    private final AtomicInteger bytesReceived = new AtomicInteger();
    private final AtomicInteger payloadsSent = new AtomicInteger();
    private final AtomicInteger payloadSendFailures = new AtomicInteger();

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        /**
         * <a href="https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback#onConnectionInitiated(java.lang.String,%20com.google.android.gms.nearby.connection.ConnectionInfo)">ConnectionLifecycleCallback.onConnectionInitiated</a>:
         * A basic encrypted channel has been created between you and the endpoint.
         * Both sides are now asked if they wish to accept or reject the connection before
         * any data can be sent over this channel.
         * <p>
         * This called when receiving an incoming connection request based on this client advertising (see
         * {@link NearbyMeshNetwork#startAdvertising}), and also after this client discovers an advertiser when
         * requesting an outgoing connection (see {@link NearbyMeshNetwork#requestConnection}).
         */
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo info) {
            // The ObjectBox MeshSync connection metadata travels in the endpoint *name* field (see startAdvertising()
            // and requestConnection()), hex-encoded.
            // Recover the raw bytes by decoding the name; a non-hex name (e.g. a foreign endpoint) yields empty
            // bytes so the magic-header check in nativeOnConnectionInitiated rejects it gracefully.
            String endpointName = info.getEndpointName();
            byte[] endpointInfo = hexToBytes(endpointName);
            boolean incoming = info.isIncomingConnection();
            Log.d(TAG, "Connection initiated for ID: " + endpointId + ", name: " + endpointName +
                    ", incoming: " + incoming +
                    ", info bytes: " + endpointInfo.length +
                    ", auth digits: " + info.getAuthenticationDigits());
            boolean acceptConnection = nativeOnConnectionInitiated(nativeHandle, endpointId, endpointInfo, incoming);
            connectionsInitiated.incrementAndGet();
            if (acceptConnection) {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
                        .addOnSuccessListener(unused -> Log.d(TAG, "Connection accepted: " + endpointId))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to accept connection: " + endpointId, e));
            } else {
                connectionsClient.rejectConnection(endpointId)
                        .addOnSuccessListener(unused -> Log.d(TAG, "Connection rejected: " + endpointId))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to reject connection: " + endpointId, e));
            }
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            boolean success = result.getStatus().isSuccess();
            Log.d(TAG, "Connection result: " + endpointId + ", success: " + success);
            nativeOnConnectionResult(nativeHandle, endpointId, success);
            if (success) {
                connectionsSucceeded.incrementAndGet();
            } else {
                connectionsFailed.incrementAndGet();
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            Log.d(TAG, "Disconnected: " + endpointId);
            nativeOnDisconnected(nativeHandle, endpointId);
            disconnections.incrementAndGet();
        }
    };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            // The Nearby library surfaces the bytes we advertised via the endpoint *name* field, not the
            // *info* field (getEndpointInfo() returns a Nearby-internal value). We advertise hex-encoded
            // (ASCII-only) text, so we recover the original binary metadata by hex-decoding the name.
            // A non-hex name (foreign endpoint) yields empty bytes -> native magic-header check rejects it.
            String endpointName = info.getEndpointName();
            byte[] endpointInfo = hexToBytes(endpointName);
            Log.d(TAG, "Endpoint found with ID: " + endpointId + ", name: " + endpointName +
                    ", name length: " + endpointName.length() + ", info bytes: " + endpointInfo.length);
            nativeOnEndpointFound(nativeHandle, endpointId, endpointName, info.getServiceId(), endpointInfo);
            endpointsFound.incrementAndGet();
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            Log.d(TAG, "Endpoint lost with ID: " + endpointId);
            nativeOnEndpointLost(nativeHandle, endpointId);
            endpointsLost.incrementAndGet();
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            if (payload.getType() == Payload.Type.BYTES) {
                byte[] bytes = payload.asBytes();
                if (bytes != null) {
                    Log.d(TAG, "Payload received from " + endpointId + ": " + bytes.length + " bytes");
                    nativeOnPayloadReceived(nativeHandle, endpointId, bytes);
                    payloadsReceived.incrementAndGet();
                    bytesReceived.addAndGet(bytes.length);
                }
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
            // Currently only handling BYTES payloads which complete immediately
        }
    };

    /**
     * Creates a new {@code NearbyMeshNetwork} and its paired native counterpart.
     *
     * @param context   Android context (Application context recommended)
     * @param serviceId Unique service ID for this mesh network
     */
    public NearbyMeshNetwork(@NonNull Context context, @NonNull String serviceId) {
        this.serviceId = serviceId;
        this.connectionsClient = Nearby.getConnectionsClient(context);
        this.nativeHandle = nativeCreate();
        if (this.nativeHandle == 0) {
            throw new IllegalStateException("Failed to create native mesh network");
        }
        Log.d(TAG, "Created NearbyMeshNetwork: serviceId=" + serviceId);
    }

    /**
     * Starts advertising this endpoint.
     * Returns immediately - the result is delivered asynchronously via native callback.
     *
     * @param endpointInfo Metadata to broadcast to discovering endpoints
     * @return true if the request was initiated (not an indication of final success)
     */
    public boolean startAdvertising(@NonNull byte[] endpointInfo) {
        if (isAdvertising) {
            Log.d(TAG, "Already advertising");
            return true;
        }

        AdvertisingOptions options = new AdvertisingOptions.Builder()
                .setStrategy(STRATEGY)
                .build();

        // Nearby surfaces our metadata on the discoverer via getEndpointName() (a String), not
        // getEndpointInfo(). To put arbitrary binary (incl. NUL / high bytes from the peer ID prefix) into a
        // text field safely, hex-encode it: ASCII-only, no NUL, and unaffected by Nearby's String<->byte
        // charset handling. The discoverer hex-decodes it back in onEndpointFound().
        String endpointName = bytesToHex(endpointInfo);
        connectionsClient.startAdvertising(endpointName, serviceId, connectionLifecycleCallback, options)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Advertising started with name: " + endpointName + ", service ID: " + serviceId);
                    isAdvertising = true;
                    nativeOnAdvertisingStarted(nativeHandle, true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to start advertising with name: " + endpointName +
                            ", service ID: " + serviceId, e);
                    nativeOnAdvertisingStarted(nativeHandle, false);
                });

        return true; // Request initiated successfully
    }

    /**
     * Stops advertising this endpoint.
     */
    public void stopAdvertising() {
        if (isAdvertising) {
            connectionsClient.stopAdvertising();
            isAdvertising = false;
            Log.d(TAG, "Advertising stopped");
        }
    }

    /**
     * Starts discovering nearby endpoints.
     * Returns immediately - the result is delivered asynchronously via native callback.
     *
     * @return true if the request was initiated (not an indication of final success)
     */
    public boolean startDiscovery() {
        if (isDiscovering) {
            Log.d(TAG, "Already discovering");
            return true;
        }

        DiscoveryOptions options = new DiscoveryOptions.Builder()
                .setStrategy(STRATEGY)
                .build();

        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, options)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Discovery started with service ID: " + serviceId);
                    isDiscovering = true;
                    nativeOnDiscoveryStarted(nativeHandle, true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to start discovery with service ID: " + serviceId, e);
                    nativeOnDiscoveryStarted(nativeHandle, false);
                });

        return true; // Request initiated successfully
    }

    /**
     * Stops discovering nearby endpoints.
     */
    public void stopDiscovery() {
        if (isDiscovering) {
            connectionsClient.stopDiscovery();
            isDiscovering = false;
            Log.d(TAG, "Discovery stopped");
        }
    }

    /**
     * Requests a connection to a discovered endpoint.
     * Returns immediately - the result is delivered asynchronously via native callback.
     *
     * @param endpointId The endpoint to connect to
     * @return true if the request was initiated (not an indication of final success)
     */
    public boolean requestConnection(@NonNull String endpointId, @NonNull byte[] endpointInfo) {
        // Hex-encode the connection metadata for the same reason as in startAdvertising(): it travels in the
        // endpoint name (String) field and must survive Nearby's String<->byte handling unharmed.
        String endpointName = bytesToHex(endpointInfo);
        connectionsClient.requestConnection(endpointName, endpointId, connectionLifecycleCallback)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Connection requested to ID: " + endpointId + ", name: " + endpointName);
                    nativeOnConnectionRequested(nativeHandle, endpointId, true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to request connection to ID: " + endpointId, e);
                    nativeOnConnectionRequested(nativeHandle, endpointId, false);
                });

        return true; // Request initiated successfully
    }

    /**
     * Disconnects from an endpoint.
     *
     * @param endpointId The endpoint to disconnect from
     */
    public void disconnect(@NonNull String endpointId) {
        connectionsClient.disconnectFromEndpoint(endpointId);
        Log.d(TAG, "Disconnected from: " + endpointId);
    }

    /**
     * Sends bytes to a connected endpoint.
     * Returns immediately - the result is delivered asynchronously via native callback.
     *
     * @param endpointId The endpoint to send to
     * @param bytes      The data to send
     * @return true if the request was initiated (not an indication of final success)
     */
    public boolean send(@NonNull String endpointId, @NonNull byte[] bytes) {
        Payload payload = Payload.fromBytes(bytes);

        connectionsClient.sendPayload(endpointId, payload)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Payload sent to " + endpointId + ": " + bytes.length + " bytes");
                    payloadsSent.incrementAndGet();
                    nativeOnPayloadSent(nativeHandle, endpointId, true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send payload to: " + endpointId, e);
                    payloadSendFailures.incrementAndGet();
                    nativeOnPayloadSent(nativeHandle, endpointId, false);
                });

        return true; // Request initiated successfully
    }

    /**
     * Stops all mesh network activities and releases resources.
     * Releases the shared pointer of AndroidMeshNetwork (primarily owned by MeshSync C++ class).
     */
    public void stop() {
        // Clear nativeHandle early so that it is not used anymore to make this less(!) race-y.
        long handleToDestroy;
        synchronized (this) {
            handleToDestroy = nativeHandle;
            nativeHandle = 0;
        }

        stopAdvertising();
        stopDiscovery();
        connectionsClient.stopAllEndpoints();

        if (handleToDestroy != 0) {
            nativeDestroy(handleToDestroy);
        }
        Log.d(TAG, "Mesh network stopped");
    }

    public boolean isAdvertising() {
        return isAdvertising;
    }

    public boolean isDiscovering() {
        return isDiscovering;
    }

    /** Returns the number of endpoints discovered so far. */
    public int getEndpointsFound() { return endpointsFound.get(); }

    /** Returns the number of endpoints lost so far. */
    public int getEndpointsLost() { return endpointsLost.get(); }

    /** Returns the number of connections initiated (onConnectionInitiated callbacks). */
    public int getConnectionsInitiated() { return connectionsInitiated.get(); }

    /** Returns the number of successful connections. */
    public int getConnectionsSucceeded() { return connectionsSucceeded.get(); }

    /** Returns the number of failed connections. */
    public int getConnectionsFailed() { return connectionsFailed.get(); }

    /** Returns the number of disconnections. */
    public int getDisconnections() { return disconnections.get(); }

    /** Returns the number of payloads received. */
    public int getPayloadsReceived() { return payloadsReceived.get(); }

    /** Returns the total number of bytes received across all payloads. */
    public int getBytesReceived() { return bytesReceived.get(); }

    /** Returns the number of payloads sent successfully. */
    public int getPayloadsSent() { return payloadsSent.get(); }

    /** Returns the number of payload send failures. */
    public int getPayloadSendFailures() { return payloadSendFailures.get(); }

    /** Logs all stat counters at INFO level for diagnostics. */
    public void logStats() {
        Log.i(TAG, "Stats for serviceId='" + serviceId + "': " +
                "endpointsFound=" + endpointsFound.get() +
                ", endpointsLost=" + endpointsLost.get() +
                ", connectionsInitiated=" + connectionsInitiated.get() +
                ", connectionsSucceeded=" + connectionsSucceeded.get() +
                ", connectionsFailed=" + connectionsFailed.get() +
                ", disconnections=" + disconnections.get() +
                ", payloadsReceived=" + payloadsReceived.get() +
                ", bytesReceived=" + bytesReceived.get() +
                ", payloadsSent=" + payloadsSent.get() +
                ", payloadSendFailures=" + payloadSendFailures.get());
    }

    // Use upper case letters to match objectbox::toHexString
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    /** Encodes bytes as a hex String (ASCII-only, no NUL); safe to carry in the Nearby endpoint name. */
    private static String bytesToHex(@NonNull byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            chars[i * 2] = HEX_CHARS[v >>> 4];
            chars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(chars);
    }

    /**
     * Decodes a hex String back to bytes. Returns an empty array for null or malformed (non-hex / odd-length)
     * input, so a foreign or corrupted endpoint name is rejected by the native magic-header check.
     */
    @NonNull
    private static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty() || (hex.length() & 1) != 0) return new byte[0];
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) return new byte[0];
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    // Callbacks from Java listeners into C++.
    private native void nativeOnEndpointFound(long handle, String endpointId, String endpointName, String serviceId,
            byte[] endpointInfo);

    private native void nativeOnEndpointLost(long handle, String endpointId);

    private native boolean nativeOnConnectionInitiated(long handle, String endpointId, byte[] endpointInfo,
            boolean incoming);

    private native void nativeOnConnectionResult(long handle, String endpointId, boolean success);

    private native void nativeOnDisconnected(long handle, String endpointId);

    private native void nativeOnPayloadReceived(long handle, String endpointId, byte[] bytes);

    // Native callbacks for async operation results
    private native void nativeOnAdvertisingStarted(long handle, boolean success);

    private native void nativeOnDiscoveryStarted(long handle, boolean success);

    private native void nativeOnConnectionRequested(long handle, String endpointId, boolean success);

    private native void nativeOnPayloadSent(long handle, String endpointId, boolean success);

    /**
     * Creates a C++ AndroidMeshNetwork instance from Java.
     *
     * @return Native handle to the C++ object, which is actually a pointer to a shared_ptr allocated via new.
     */
    private native long nativeCreate();

    /**
     * Destroys a C++ AndroidMeshNetwork instance.
     *
     * @param handle Native handle from nativeCreate
     */
    private native void nativeDestroy(long handle);

    /**
     * Returns the native handle to the paired C++ mesh network; a pointer to a
     * {@code std::shared_ptr<MeshNetworkInterface>}, matching the contract of
     * {@code InternalSyncAccess.addNetworkInternalHandle()}.
     */
    public long getNativeHandle() {
        return nativeHandle;
    }
}
