package io.objectbox.internal;

import io.objectbox.annotation.apihint.Internal;

/**
 * Give native code the chance to add additional info for tools like Crashlytics.
 */
@Internal
public interface CrashReportLogger {
    void log(String message);
}
