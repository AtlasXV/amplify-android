package com.amplifyframework.datastore.storage.sqlite.migrations

import com.amplifyframework.core.model.ModelProvider

interface AmplifyDbVersionCheckListener {
    fun onSqliteInitializeStarted()
    fun onStartCheckAmplifyDbVersion(modelsProvider: ModelProvider)
    fun onSqliteInitializedSuccess()
}