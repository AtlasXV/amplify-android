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

package com.amplifyframework.core;

import com.amplifyframework.logging.AmplifyLoggerFactory;
import com.amplifyframework.logging.LoggingCategory;

/**
 * This is the top-level customer-facing interface to the Amplify
 * framework.
 * <p>
 * The Amplify System has the following responsibilities:
 * <p>
 * 1) Add, Get and Remove Category plugins with the Amplify System
 * 2) Configure and reset the Amplify System with the information
 * from the amplifyconfiguration.json.
 * <p>
 * Configure using amplifyconfiguration.json
 * <pre>
 *     {@code
 *      Amplify.configure(getApplicationContext());
 *     }
 * </pre>
 * <p>
 * Note: there is also a Kotlin facade class called Amplify.
 * If you are writing Java, import this version of Amplify.
 * If you are writing Kotlin, import com.amplifyframework.kotlin.Amplify instead.
 */
public final class Amplify {
    // These static references provide an entry point to the different categories.
    // For example, you can call storage operations through Amplify.Storage.list(String path).
    @SuppressWarnings("checkstyle:all")
    public static final LoggingCategory Logging = new LoggingCategory();
    public static AmplifyLoggerFactory loggerFactory = new AmplifyLoggerFactory();
}

