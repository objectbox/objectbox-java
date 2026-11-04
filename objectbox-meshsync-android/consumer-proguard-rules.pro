# NearbyMeshNetwork (provided by the database library, not this project) is called from native code
# when using Mesh Sync (via JNI method lookup) and declares native methods, so it must not be
# removed, renamed or have members stripped.
# This rule is here instead of the database library, as the database library may also be used when
# not using Mesh Sync. This allows R8 to remove the class in that case (and avoids class not found
# errors of its imports).
-keep class io.objectbox.meshsync.android.internal.NearbyMeshNetwork { *; }
