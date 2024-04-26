package com.atlasv.android.amplify.simpleappsync.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.datastore.storage.sqlite.PersistentModelVersion
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.amplifyframework.datastore.storage.sqlite.migrations.AmplifyDbVersionCheckListener
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent.Companion.LOG
import com.atlasv.android.amplify.simpleappsync.config.AmplifySimpleSyncConfig
import com.atlasv.android.amplify.simpleappsync.ext.AmplifyExtSettings
import java.io.File

class AmplifyBuildInDbProvider(
    private val appContext: Context,
    private val buildInDbMigrate: (String) -> Unit,
    private val config: AmplifySimpleSyncConfig,
    private val onSqliteInitSuccess: () -> Unit,
    private val extSettings: AmplifyExtSettings
) : AmplifyDbVersionCheckListener {

    private var updatedWithInnerDb = false
    private val dbName = SQLiteStorageAdapter.DEFAULT_DATABASE_NAME

    override fun onSqliteInitializeStarted(modelsProvider: ModelProvider) {
        try {
            val extraVersion = config.extraVersion
            val dbFile = appContext.getDatabasePath(dbName)
            if (!dbFile.exists()) {
                LOG.info("$dbName not exists, need updateWithInnerDb.")
                updateWithInnerDb()
                return
            }
            var oldVersion = getBuildInDbVersion(dbFile)
            val newVersion: String = modelsProvider.version()
            val customModelVersion = extSettings.getExtraModelVersion()
            LOG.debug(
                "updateWithInnerDb check: " +
                        "oldVersion=$oldVersion, newVersion=$newVersion, " +
                        "customModelVersion=$customModelVersion, extraVersion=$extraVersion"
            )
            if (oldVersion != newVersion || customModelVersion != extraVersion) {
                LOG.debug("Need updateWithInnerDb")
                updateWithInnerDb()
            } else {
                LOG.debug("No need to updateWithInnerDb")
            }
            oldVersion = getBuildInDbVersion(dbFile)
            val isSuccess = oldVersion == newVersion
            LOG.debug("After updateWithInnerDb: oldVersion=$oldVersion, isSuccess=${isSuccess}")
        } catch (cause: Throwable) {
            LOG.error("onSqliteInitializeStarted failed", cause)
        }
    }

    private fun updateWithInnerDb() {
        if (updatedWithInnerDb) {
            LOG.debug("Already updateWithInnerDb, return")
            return
        }
        buildInDbMigrate(dbName)
        extSettings.saveExtraModelVersion(config.extraVersion)
        extSettings.saveLastSyncTimestamp(config.buildInDbUpdatedAt)
        updatedWithInnerDb = true
        LOG.debug("updateWithInnerDb finish")
    }

    private fun getBuildInDbVersion(dbFile: File): String? {
        val db = SQLiteDatabase.openDatabase(dbFile.path, null, 0)
        val tableName = PersistentModelVersion::class.simpleName
        return db.use {
            val idColumnName = "${tableName}_id"
            val versionColumnName = "${tableName}_version"
            db.rawQuery(
                "SELECT `${tableName}`.`id` AS `${idColumnName}`, `${tableName}`.`version` AS `${versionColumnName}` FROM `${tableName}`;",
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val modelId = cursor.getString(cursor.getColumnIndexOrThrow(idColumnName))
                    val modelVersion = cursor.getString(cursor.getColumnIndexOrThrow(versionColumnName))
                    LOG.debug("getBuildInDbVersion: modelId=$modelId, modelVersion=$modelVersion")
                    modelVersion
                } else {
                    null
                }
            }
        }
    }

    override fun onSqliteInitializedSuccess() {
        onSqliteInitSuccess.invoke()
    }
}