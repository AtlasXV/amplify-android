/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.util;

import android.annotation.SuppressLint;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.BuildConfig;
import com.amplifyframework.logging.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A utility to construct a User-Agent header, to be sent with all network operations.
 */
public final class UserAgent {
    private static final Logger LOG = Amplify.Logging.logger("amplify:core");

    private static UserAgent instance = null;

    private final String libraryName;
    private final String libraryVersion;

    // Using LinkedHashMap to preserve the ordering in string representation
    private final Map<String, String> extras = new LinkedHashMap<>();

    private UserAgent(String libraryName, String libraryVersion, Map<String, String> extras) {
        this.libraryName = libraryName;
        this.libraryVersion = libraryVersion;
        this.extras.putAll(extras);

        String deviceManufacturer = escape(sanitize(Build.MANUFACTURER));
        String deviceName = escape(sanitize(Build.MODEL));
        String userLanguage = sanitize(System.getProperty("user.language"));
        String userRegion = sanitize(System.getProperty("user.region"));

        this.extras.put(deviceManufacturer, deviceName);
        this.extras.put("locale", String.format("%s_%s", userLanguage, userRegion));
    }


    /**
     * Reset User-Agent configuration for testing purposes.
     * No-op if User-Agent was not configured yet.
     */
    @VisibleForTesting
    public static synchronized void reset() {
        instance = null;
    }

    /**
     * Gets a String to use as the value of a User-Agent header.
     * <p>
     * Clients should use this method only to get the user agent portion generated by library.
     * <br/>
     * This string does not contain extra metadata added by the SDK.
     * </p>
     *
     * @return A value for a User-Agent header.
     */
    @SuppressLint("SyntheticAccessor")
    @NonNull
    public static String string() {
        if (instance == null) {
            LOG.debug("User-Agent is not yet configured. Returning default Android user-agent.");
            return new UserAgent(
                    Platform.ANDROID.getLibraryName(),
                    BuildConfig.VERSION_NAME,
                    new HashMap<>()
            ).toString();
        }

        return instance.toString();
    }

    /**
     * Gets the string representation of the User-agent.
     *
     * @return A sting value for user-agent
     */
    @NonNull
    @Override
    public String toString() {
        return String.format("%s:%s", libraryName, libraryVersion) + getExtrasString();
    }

    private String getExtrasString() {
        StringBuilder sb = new StringBuilder();
        extras.forEach((key, value) -> sb.append(String.format(" md/%s/%s", key, value)));
        return sb.toString();
    }

    @NonNull
    private static String sanitize(@Nullable String string) {
        return string != null ? string : "UNKNOWN";
    }

    @NonNull
    private static String escape(@NonNull String string) {
        return string.replace(" ", "_");
    }

    /**
     * Returns true if running on Flutter.
     *
     * @return Returns true if running on Flutter.
     */
    public static boolean isFlutter() {
        return string().contains(Platform.FLUTTER.libraryName);
    }

    /**
     * Enum to represent various platforms that use Amplify library for tracking
     * usage metrics.
     * <p>
     * to supply a configuration builder with any additional platform (not Android)
     * that uses this library.
     * <p>
     * e.g.
     * <pre>
     * AmplifyConfiguration configuration = AmplifyConfiguration.builder(configJson)
     *     .addPlatform(UserAgent.Platform.FLUTTER, "1.0.0")
     *     .build();
     * </pre>
     */
    public enum Platform {
        /**
         * This is the default platform that will be included in every user-agent
         * that is generated by this library.
         * A user should never be using this enum to specify their platform version
         * to avoid redundancy.
         */
        ANDROID("amplify-android"),

        /**
         * The Flutter library calls on Android Amplify. This enum should be specified
         * during the construction of an Amplify configuration object to indicate that
         * Flutter library is being used.
         */
        FLUTTER("amplify-flutter");

        private final String libraryName;

        Platform(String libraryName) {
            this.libraryName = libraryName;
        }

        /**
         * Gets the library name to be used by the user agent.
         *
         * @return the library name for a given platform
         */
        public String getLibraryName() {
            return libraryName;
        }
    }
}
