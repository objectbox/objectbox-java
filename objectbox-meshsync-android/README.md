# ObjectBox Mesh Sync for Android

Adds peer-to-peer (P2P) mesh synchronization between ObjectBox Sync clients on Android, without a
central server, using [Google Nearby Connections](https://developers.google.com/nearby/connections/overview).

This is a plain (no native code, single variant) Android library that apps add **in addition** to the
Sync variant of the ObjectBox Android library (e.g. `objectbox-sync-android`), which contains the native
mesh sync implementation. Keeping it separate avoids multiplying the variants of the main Android library
(basic/sync × with/without Admin) and keeps the Nearby dependency out of apps that don't use mesh sync.

What this library provides:

- `AndroidMeshSync.createConfig(context, meshId)` creating a `MeshConfig` (see `objectbox-java`) with the
  Nearby Connections mesh network attached, to pass to `SyncBuilder.mesh()`.
- The permissions required by Nearby Connections, merged into the app manifest
  (see `src/main/AndroidManifest.xml`). Apps must still request the dangerous (runtime) permissions.
- The `play-services-nearby` dependency.

Usage:

```java
MeshConfig meshConfig = AndroidMeshSync.createConfig(context, "com.myapp.mesh");

SyncClient syncClient = Sync.client(boxStore)
        .url("ws://server:9999")
        .credentials(credentials)
        .mesh(meshConfig)
        .buildAndStart();

MeshSync mesh = syncClient.getMesh(); // state, connected peer count, stats
```

Note: `io.objectbox.android.internal.meshsync.NearbyMeshNetwork` must keep its package and member names,
as they must match the JNI exports of the native library (`AndroidMeshNetworkJni.cpp` in the internal
repository, which also contains instrumented tests for it).
