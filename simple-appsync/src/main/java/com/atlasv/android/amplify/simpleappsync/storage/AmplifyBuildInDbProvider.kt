package com.atlasv.android.amplify.simpleappsync.storage

import android.content.Context
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.amplifyframework.datastore.storage.sqlite.migrations.AmplifyDbVersionCheckListener
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent
import com.atlasv.android.amplify.simpleappsync.ext.getDbLastSyncTime
import java.io.File

class AmplifyBuildInDbProvider(
    private val appContext: Context,
    private val externalDbFileSupplier: () -> File,
    private val onSqliteInitSuccess: () -> Unit
) : AmplifyDbVersionCheckListener {
    private val dbName = SQLiteStorageAdapter.DEFAULT_DATABASE_NAME
    private val logger by lazy {
        AmplifySimpleSyncComponent.loggerFactory?.invoke("amplify:sqlite-storage")
    }

    override fun onSqliteInitializeStarted() {
        logger?.w { "onSqliteInitializeStarted" }
        try {
            val dbFile = appContext.getDatabasePath(dbName)
            val externalDbFile = externalDbFileSupplier.invoke()
            val currentDbVersion = dbFile.getDbLastSyncTime()
            val externalDbVersion = externalDbFile.getDbLastSyncTime()
            if (currentDbVersion <= 0 || currentDbVersion < externalDbVersion) {
                logger?.w {
                    "currentDbVersion=$currentDbVersion, externalDbVersion=${externalDbVersion}, need updateWithInnerDb"
                }
                updateWithInnerDb(externalDbFile)
                val newestDbLastSyncTime = appContext.getDatabasePath(dbName).getDbLastSyncTime()
                logger?.w {
                    "updateWithInnerDb finish, now currentDbVersion=${newestDbLastSyncTime}"
                }
            } else {
                logger?.w { "currentDbVersion == externalDbVersion: $currentDbVersion, no need to update" }
            }
        } catch (cause: Throwable) {
            logger?.e(cause) { "onSqliteInitializeStarted failed" }
        }
    }

    private fun updateWithInnerDb(externalDbFile: File) {
        val db = appContext.getDatabasePath(dbName)
        externalDbFile.inputStream().use { innerDb ->
            db.outputStream().use { innerDb.copyTo(it) }
        }
    }

    override fun onSqliteInitializedSuccess() {
        onSqliteInitSuccess.invoke()
    }
}