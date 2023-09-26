package com.amplifyframework.datastore.storage.sqlite.migrations

import com.amplifyframework.core.model.ModelProvider

interface AmplifyDbVersionCheckListener {
    fun onSqliteInitializeStarted(modelsProvider: ModelProvider)
    fun onSqliteInitializedSuccess()
}