package com.atlasv.android.amplify.simpleappsync

import android.content.Context
import com.amplifyframework.api.aws.AppSyncGraphQLRequest
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.storage.sqlite.CursorValueStringFactory
import com.amplifyframework.datastore.storage.sqlite.SQLCommandFactoryFactory
import com.amplifyframework.kotlin.core.Amplify
import com.atlasv.android.amplify.simpleappsync.ext.AmplifyExtSettings
import com.atlasv.android.amplify.simpleappsync.ext.simpleFormat
import com.atlasv.android.amplify.simpleappsync.request.DefaultMergeRequestFactory
import com.atlasv.android.amplify.simpleappsync.request.MergeRequestFactory
import com.atlasv.android.amplify.simpleappsync.response.AmplifyModelMerger
import com.atlasv.android.amplify.simpleappsync.response.AmplifySyncResponse
import com.atlasv.android.amplify.simpleappsync.storage.AmplifySqliteStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

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
    sqlCommandFactoryFactory: SQLCommandFactoryFactory,
    cursorValueStringFactory: CursorValueStringFactory,
    private val buildInDbMigrate: () -> Unit,
    private val extraVersion: Long, // 支持不更改modelVersion也能重新使用内置数据库
    private val onSqliteInitSuccess: () -> Unit
) {

    private val mutex = Mutex()
    val storage by lazy {
        AmplifySqliteStorage(
            appContext,
            dataStoreConfiguration,
            modelProvider,
            schemaRegistry,
            sqlCommandFactoryFactory,
            cursorValueStringFactory,
            buildInDbMigrate,
            extraVersion,
            onSqliteInitSuccess
        )
    }

    val merger by lazy {
        AmplifyModelMerger(storage)
    }

    suspend fun syncFromRemote(
        grayRelease: Int, dbInitTime: Long, dataExpireInterval: Long = TimeUnit.DAYS.toMillis(30)
    ): AmplifySyncResponse? {
        return mutex.withLock {
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
                val responseItemGroups = queryAllData(request)
                val newestSyncTime = if (oldDataExpired) currentTime else responseItemGroups.maxOfOrNull { list ->
                    list.maxOfOrNull {
                        it.syncMetadata.lastChangedAt?.secondsSinceEpoch ?: 0L
                    } ?: 0L
                }?.takeIf { it > lastSync }?.also {
                    LOG.info("newestSyncTime=$it")
                }
                merger.mergeResponse(responseItemGroups)
                AmplifyExtSettings.saveLastSync(appContext, modelProvider.version(), newestSyncTime)
                val newestUpdatedTime = newestSyncTime ?: lastSync
                LOG.info(
                    "syncFromRemote success, newestUpdatedTime=${Date(newestUpdatedTime).simpleFormat()}"
                )
                AmplifySyncResponse(newestUpdatedTime = newestUpdatedTime)
            } catch (cause: Throwable) {
                LOG.error("syncFromRemote error", cause)
                null
            }
        }
    }

    private suspend fun queryAllData(request: GraphQLRequest<List<GraphQLResponse<PaginatedResult<ModelWithMetadata<Model>>>>>): List<List<ModelWithMetadata<Model>>> {
        val response = Amplify.API.query(request).data
        val responseItemGroups = response.map {
            it.data?.items?.toList().orEmpty()
        }
        val remainRequests = response.mapNotNull {
            it.data.requestForNextResult as? AppSyncGraphQLRequest<*>
        }
        return if (remainRequests.isEmpty()) {
            responseItemGroups
        } else {
            responseItemGroups + queryAllData(DefaultMergeRequestFactory.merge(remainRequests))
        }
    }

    private suspend fun getLastSyncTime(dbInitTime: Long): Long {
        if (modelProvider.version() != AmplifyExtSettings.getLastSyncDbVersion(appContext)) {
            return dbInitTime
        }
        return maxOf(dbInitTime, AmplifyExtSettings.getLastSyncTimestamp(appContext))
    }

    companion object {
        val LOG by lazy {
            Amplify.Logging.forNamespace("amplify:simple-sync")
        }
    }
}