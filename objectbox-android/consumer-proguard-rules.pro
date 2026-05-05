# When editing this file, also look at objectbox-java.pro of objectbox-java.

# Accessed by objectbox-java through reflection.
-keep class io.objectbox.android.internal.AndroidPlatform {*;}

# For provided android.arch.lifecycle:extensions
-dontwarn io.objectbox.android.ObjectBoxLiveData

# For provided android.arch.paging:runtime
-dontwarn io.objectbox.android.ObjectBoxDataSource*
