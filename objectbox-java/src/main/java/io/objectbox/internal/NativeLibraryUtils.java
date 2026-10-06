/*
 * Copyright 2026 ObjectBox Ltd. <https://objectbox.io>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox.internal;


import io.objectbox.BoxStore;
import io.objectbox.exception.FeatureNotAvailableException;

/**
 * Provides utility methods related to the platform-specific database library.
 */
public class NativeLibraryUtils {

    private static final String FEATURE_NOT_AVAIL_TEMPLATE =
            "The included ObjectBox database library does not include the %s feature." +
                    " Please visit %s for options.";
    private static final String URL_SYNC_LANDING_PAGE = "https://objectbox.io/sync/";

    private NativeLibraryUtils() {
    }

    /**
     * If {@link BoxStore#hasFeature(Feature)} returns false, throws a
     * {@link FeatureNotAvailableException} with a unified, helpful message.
     *
     * @param feature the feature to check for
     * @param featureName a user-friendly name for the feature to use in the message
     * @param infoUrl the URL where the user can learn about options to get that feature
     */
    private static void checkHasFeature(Feature feature, String featureName, String infoUrl)
            throws FeatureNotAvailableException {
        if (!BoxStore.hasFeature(feature)) {
            throw new FeatureNotAvailableException(
                    String.format(FEATURE_NOT_AVAIL_TEMPLATE, featureName, infoUrl));
        }
    }

    /**
     * Calls {@link #checkHasFeature} with {@link Feature#SYNC}.
     */
    public static void checkHasSyncFeature() {
        checkHasFeature(Feature.SYNC, "Sync", URL_SYNC_LANDING_PAGE);
    }

    /**
     * Calls {@link #checkHasSyncFeature} with {@link Feature#SYNC_SERVER}.
     */
    public static void checkHasSyncServerFeature() {
        checkHasFeature(Feature.SYNC_SERVER, "Sync Server", URL_SYNC_LANDING_PAGE);
    }

}
