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
- The permissions that may be required by Nearby Connections, merged into the app manifest 
  (see [AndroidManifest.xml](src/main/AndroidManifest.xml)).
- The `play-services-nearby` dependency.

## Permissions

Note that not all permissions this library adds may be required. Depending on a device's Android SDK
version and your app's needs, only some of them may actually be necessary. For example, location 
permissions are typically not required for Nearby Connections on recent Android releases.

To remove them as needed, use [merge rule markers](https://developer.android.com/build/manage-manifests) 
in your app's manifest, for example:

```xml
<uses-permission
    android:name="android.permission.ACCESS_COARSE_LOCATION"
    tools:node="remove" />
<uses-permission
    android:name="android.permission.ACCESS_FINE_LOCATION"
    tools:node="remove" />
```

Also note that your app must [request any dangerous (runtime) permissions](https://developer.android.com/training/permissions/requesting) 
at runtime when needed. This library provides a [MeshSyncPermissions](src/main/java/io/objectbox/meshsync/android/MeshSyncPermissions.kt)
helper class to request the permissions included by default.

## Usage

```kotlin
val meshConfig: MeshConfig = AndroidMeshSync.createConfig(context, "example.myapp.mesh")

val syncClient: SyncClient = Sync.client(boxStore)
        .url(SYNC_SERVER_URL)
        .credentials(credentials)
        .mesh(meshConfig)
        .buildAndStart()

val mesh: MeshSync? = syncClient.getMesh() // state, connected peer count, stats
```
