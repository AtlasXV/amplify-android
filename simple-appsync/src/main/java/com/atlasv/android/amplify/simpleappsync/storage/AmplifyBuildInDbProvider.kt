package com.atlasv.android.amplify.simpleappsync.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.datastore.storage.sqlite.PersistentModelVersion
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.amplifyframework.datastore.storage.sqlite.migrations.AmplifyDbVersionCheckListener
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent.Companion.LOG
import java.io.File

class AmplifyBuildInDbProvider(
    private val appContext: Context,
    private val buildInDbMigrate: () -> Unit,
    private val extraVersion: Long,
    private val onSqliteInitSuccess: () -> Unit
) : AmplifyDbVersionCheckListener {
    private val amplifySettings = appContext.getSharedPreferences("sp_amplify_settings", Context.MODE_PRIVATE)
    private var updatedWithInnerDb = false
    override fun onSqliteInitializeStarted(modelsProvider: ModelProvider) {
        try {
            val dbFile = appContext.getDatabasePath("AmplifyDatastore.db")
            if (!dbFile.exists()) {
                LOG.info("AmplifyDatastore.db not exists, need updateWithInnerDb.")
                updateWithInnerDb()
                amplifySettings.edit().putLong(KEY_EXTRA_MODEL_VERSION, extraVersion).apply()
                return
            }
            var oldVersion = getBuildInDbVersion(dbFile)
            val newVersion: String = modelsProvider.version()
            val customModelVersion = amplifySettings.getLong(KEY_EXTRA_MODEL_VERSION, 0)
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
            if (isSuccess) {
                amplifySettings.edit().putLong(KEY_EXTRA_MODEL_VERSION, extraVersion).apply()
            }
        } catch (cause: Throwable) {
            LOG.error("onSqliteInitializeStarted failed", cause)
        }
    }

    private fun updateWithInnerDb() {
        if (updatedWithInnerDb) {
            LOG.debug("Already updateWithInnerDb, return")
            return
        }
        buildInDbMigrate()
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

    companion object {
        private const val KEY_EXTRA_MODEL_VERSION = "extra_model_version"
        fun getCurrentDbVersion(localStorageAdapter: SQLiteStorageAdapter): String? {
            return localStorageAdapter.sqlQueryProcessor?.queryOfflineData(
                PersistentModelVersion::class.java,
                Where.matchesAll()
            ) {}?.firstOrNull()?.version
        }
    }
}