package com.atlasv.android.amplify.simpleappsync

import android.content.Context
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.storage.sqlite.CursorValueStringFactory
import com.amplifyframework.datastore.storage.sqlite.SQLCommandFactoryFactory
import com.amplifyframework.kotlin.core.Amplify
import com.atlasv.android.amplify.simpleappsync.ext.AmplifyExtSettings
import com.atlasv.android.amplify.simpleappsync.ext.simpleFormat
import com.atlasv.android.amplify.simpleappsync.request.MergeRequestFactory
import com.atlasv.android.amplify.simpleappsync.response.AmplifyModelMerger
import com.atlasv.android.amplify.simpleappsync.response.merge.ItemMergeStrategy
import com.atlasv.android.amplify.simpleappsync.storage.AmplifySqliteStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifySimpleSyncComponent(
    private val appContext: Context,
    val dataStoreConfiguration: DataStoreConfiguration,
    val modelProvider: ModelProvider,
    val schemaRegistry: SchemaRegistry,
    private val mergeListFactory: MergeRequestFactory,
    private val itemMergeStrategy: ItemMergeStrategy,
    sqlCommandFactoryFactory: SQLCommandFactoryFactory,
    cursorValueStringFactory: CursorValueStringFactory
) {

    private val mutex = Mutex()
    val storage by lazy {
        AmplifySqliteStorage(
            appContext,
            dataStoreConfiguration,
            modelProvider,
            schemaRegistry,
            sqlCommandFactoryFactory,
            cursorValueStringFactory
        )
    }

    val merger by lazy {
        AmplifyModelMerger(storage, itemMergeStrategy)
    }

    suspend fun syncFromRemote(
        grayRelease: Int, dbInitTime: Long, dataExpireInterval: Long = TimeUnit.DAYS.toMillis(30)
    ) {
        mutex.withLock {
            try {
                var lastSync = getLastSyncTime(dbInitTime)
                val currentTime = System.currentTimeMillis()
                val dataAge = currentTime - lastSync
                var oldDataExpired = false
                if (lastSync > 0 && dataAge > dataExpireInterval) {
                    LOG.info("dataAge=${dataAge}ms, exceed $dataExpireInterval, need full sync")
                    lastSync = 0
                    oldDataExpired = true
                }

                val request = mergeListFactory.create(
                    appContext, dataStoreConfiguration, modelProvider, schemaRegistry, lastSync, grayRelease
                )
                val responseItemGroups = Amplify.API.query(request).data.map {
                    it.data.items.toList()
                }
                val newestSyncTime = if (oldDataExpired) currentTime else responseItemGroups.maxOfOrNull { list ->
                    list.maxOfOrNull {
                        it.syncMetadata.lastChangedAt?.secondsSinceEpoch ?: 0L
                    } ?: 0L
                }?.takeIf { it > lastSync }?.also {
                    LOG.info("newestSyncTime=$it")
                }
                merger.mergeResponse(responseItemGroups)
                AmplifyExtSettings.saveLastSync(appContext, modelProvider.version(), newestSyncTime)
                LOG.info(
                    "syncFromRemote success, date=${Date(newestSyncTime ?: lastSync).simpleFormat()}"
                )
            } catch (cause: Throwable) {
                LOG.error("syncFromRemote error", cause)
            }
        }
    }

    private suspend fun getLastSyncTime(dbInitTime: Long): Long {
        if (modelProvider.version() != AmplifyExtSettings.getLastSyncDbVersion(appContext)) {
            return dbInitTime
        }
        return maxOf(dbInitTime, AmplifyExtSettings.getLastSyncTimestamp(appContext))
    }

    companion object {
        val LOG = Amplify.Logging.forNamespace("amplify:simple-sync")
    }
}