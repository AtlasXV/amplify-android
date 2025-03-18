package com.atlasv.android.amplify.simpleappsync.storage

import android.content.Context
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.amplifyframework.datastore.storage.sqlite.migrations.AmplifyDbVersionCheckListener
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent.Companion.LOG
import com.atlasv.android.amplify.simpleappsync.ext.getDbVersion
import java.io.File

class AmplifyBuildInDbProvider(
    private val appContext: Context,
    private val externalDbFileSupplier: () -> File,
    private val onSqliteInitSuccess: () -> Unit
) : AmplifyDbVersionCheckListener {

    private val dbName = SQLiteStorageAdapter.DEFAULT_DATABASE_NAME

    override fun onSqliteInitializeStarted() {
        try {
            val dbFile = appContext.getDatabasePath(dbName)
            val externalDbFile = externalDbFileSupplier.invoke()
            val currentDbVersion = dbFile.getDbVersion()
            val externalDbVersion = externalDbFile.getDbVersion()
            if (currentDbVersion != externalDbVersion) {
                LOG?.d { "currentDbVersion=$currentDbVersion, externalDbVersion=${externalDbVersion}, need updateWithInnerDb" }
                updateWithInnerDb()
            } else {
                LOG?.d { "currentDbVersion == externalDbVersion: ${currentDbVersion}" }
            }
        } catch (cause: Throwable) {
            LOG?.e(cause) { "onSqliteInitializeStarted failed" }
        }
    }

    private fun updateWithInnerDb() {
        val db = appContext.getDatabasePath(dbName)
        appContext.resources.assets.open(dbName).use { innerDb ->
            db.outputStream().use { innerDb.copyTo(it) }
        }
        LOG?.d { "updateWithInnerDb finish, now currentDbVersion=${appContext.getDatabasePath(dbName).getDbVersion()}" }
    }

    override fun onSqliteInitializedSuccess() {
        onSqliteInitSuccess.invoke()
    }
}