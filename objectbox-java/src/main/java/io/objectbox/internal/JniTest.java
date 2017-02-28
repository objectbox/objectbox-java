package io.objectbox.internal;

public class JniTest {
    public static native boolean createAndDeleteIntArray();

    public static native int[] returnIntArray();
}
