# NearbyMeshNetwork is called from native code (via JNI method lookup) and declares native methods,
# so it must not be removed, renamed or have members stripped.
-keep class io.objectbox.meshsync.android.internal.NearbyMeshNetwork { *; }
