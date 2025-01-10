package com.atlasv.android.amplify.simpleappsync.util

/**
 * Created by weiping on 2025/1/10
 */
interface AmplifyLogger {
    fun d(messageSupplier: () -> String)
    fun w(messageSupplier: () -> String)
    fun e(messageSupplier: () -> String)
    fun w(cause: Throwable?, messageSupplier: () -> String)
    fun e(cause: Throwable?, messageSupplier: () -> String)
}