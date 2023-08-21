package com.amplifyframework.logging

open class AmplifyLoggerFactory {
    open fun create(namespace: String, loggerThreshold: LogLevel): Logger {
        return TimberLogger(namespace, loggerThreshold)
    }
}