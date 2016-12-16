package io.objectbox.internal;

import io.objectbox.annotation.apihint.Internal;

@Internal
/**
 * Give native code the chance to add additional info for tools like Crashlytics.
 */
public interface CrashReportLogger {
    void log(String message);
}
