# When editing this file, also look at consumer-proguard-rules.pro of objectbox-android.

-keepattributes *Annotation*

# Native methods
-keepclasseswithmembernames class io.objectbox.** {
    native <methods>;
}

# For __boxStore field in entities
-keep class io.objectbox.BoxStore

-keep class * extends io.objectbox.Cursor {
    <init>(...);
}

# Native code expects names to match
-keep class io.objectbox.relation.ToOne {
    void setTargetId(long);
}
-keep class io.objectbox.relation.ToMany

-keep @interface io.objectbox.annotation.Entity

# Keep entity constructors
-keep @io.objectbox.annotation.Entity class * { <init>(...); }

# For relation ID fields
-keepclassmembers @io.objectbox.annotation.Entity class * {
    <fields>;
}

-keep interface io.objectbox.converter.PropertyConverter {*;}
-keep class * implements io.objectbox.converter.PropertyConverter {*;}

-keep class io.objectbox.exception.DbException {*;}
-keep class * extends io.objectbox.exception.DbException {*;}

-keep class io.objectbox.exception.DbExceptionListener {*;}
-keep class * implements io.objectbox.exception.DbExceptionListener {*;}

# for essentials
-dontwarn sun.misc.Unsafe
