package com.atlasv.android.amplify.simpleappsync.storage

import android.content.Context
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.datastore.storage.sqlite.PersistentModelVersion
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.amplifyframework.datastore.storage.sqlite.migrations.AmplifyDbVersionCheckListener
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent.Companion.LOG
import com.atlasv.android.amplify.simpleappsync.ext.amplifySettingsDataStore

class AmplifyBuildInDbProvider(
    private val appContext: Context,
    private val localStorageAdapter: SQLiteStorageAdapter,
    private val buildInDbMigrate: () -> Unit,
    private val extraVersion: Long,
    private val onSqliteInitSuccess: () -> Unit
) : AmplifyDbVersionCheckListener {
    private val amplifySettings = appContext.getSharedPreferences("sp_amplify_settings", Context.MODE_PRIVATE)
    private var updatedWithInnerDb = false
    override fun onSqliteInitializeStarted() {
        try {
            val db = appContext.getDatabasePath("AmplifyDatastore.db")
            if (!db.exists()) {
                LOG.info("AmplifyDatastore.db not exists, use build in db.")
                updateWithInnerDb()
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

    override fun onStartCheckAmplifyDbVersion(modelsProvider: ModelProvider) {
        try {
            var oldVersion = getCurrentDbVersion(localStorageAdapter)
            val newVersion: String = modelsProvider.version()
            val customModelVersion = amplifySettings.getLong(KEY_EXTRA_MODEL_VERSION, 0)
            LOG.debug(
                "onStartCheckAmplifyDbVersion: " +
                        "oldVersion=$oldVersion, newVersion=$newVersion, " +
                        "customModelVersion=$customModelVersion, extraVersion=$extraVersion"
            )
            if (oldVersion != newVersion || customModelVersion != extraVersion) {
                LOG.debug("Need to migrate db")
                updateWithInnerDb()
            } else {
                LOG.debug("No need to migrate db")
            }
            oldVersion = getCurrentDbVersion(localStorageAdapter)
            val isSuccess = oldVersion == newVersion
            LOG.debug("After buildInDbMigrate: oldVersion=$oldVersion, isSuccess=${isSuccess}")
            if (isSuccess) {
                amplifySettings.edit().putLong(KEY_EXTRA_MODEL_VERSION, extraVersion).apply()
            }
        } catch (cause: Throwable) {
            LOG.error("onStartCheckAmplifyDbVersion failed", cause)
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