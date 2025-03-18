package com.amplifyframework.datastore.storage.sqlite.migrations

interface AmplifyDbVersionCheckListener {
    fun onSqliteInitializeStarted()
    fun onSqliteInitializedSuccess()
}