package io.objectbox.sync;

import org.junit.Test;

import io.objectbox.sync.internal.Platform;


import static org.junit.Assert.assertNotNull;

public class PlatformTest {

    @Test
    public void buildsAndNeverNull() {
        assertNotNull(Platform.findPlatform());
    }

}
