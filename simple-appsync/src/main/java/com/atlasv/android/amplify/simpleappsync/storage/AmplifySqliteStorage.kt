package com.atlasv.android.amplify.simpleappsync.storage

import android.content.Context
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.core.model.query.QueryOptions
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.storage.sqlite.CursorValueStringFactory
import com.amplifyframework.datastore.storage.sqlite.SQLCommandFactoryFactory
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent.Companion.LOG
import com.atlasv.android.amplify.simpleappsync.config.AmplifySimpleSyncConfig
import com.atlasv.android.amplify.simpleappsync.ext.AmplifyExtSettings
import com.atlasv.android.amplify.simpleappsync.ext.MODEL_METHOD_GET_SORT
import com.atlasv.android.amplify.simpleappsync.ext.hasSortField
import com.atlasv.android.amplify.simpleappsync.ext.resolveMethod
import java.util.concurrent.CountDownLatch

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifySqliteStorage(
    val appContext: Context,
    val dataStoreConfiguration: DataStoreConfiguration,
    val extSettings: AmplifyExtSettings,
    val modelProvider: ModelProvider,
    val schemaRegistry: SchemaRegistry,
    sqlCommandFactoryFactory: SQLCommandFactoryFactory,
    cursorValueStringFactory: CursorValueStringFactory,
    private val buildInDbMigrate: (String) -> Unit,
    private val config: AmplifySimpleSyncConfig,
    private val onSqliteInitSuccess: () -> Unit
) {
    private val initializationsPending = CountDownLatch(1)
    val sqLiteStorageAdapter = SQLiteStorageAdapter.forModels(schemaRegistry, modelProvider).also {
        it.sqlCommandFactoryFactory = sqlCommandFactoryFactory
        it.cursorValueStringFactory = cursorValueStringFactory
        it.dbVersionCheckListener =
            AmplifyBuildInDbProvider(appContext, buildInDbMigrate, config, onSqliteInitSuccess, extSettings)
        initSQLiteStorageAdapter(it)
    }

    fun <T : Model> query(itemClass: Class<T>, options: QueryOptions): List<T>? {
        val startMs = System.currentTimeMillis()
        return use { adapter ->
            adapter.sqlQueryProcessor?.queryOfflineData(itemClass, options) { cause ->
                LOG?.e(cause) { "query ${itemClass.simpleName} error" }
            }?.let { modelItems ->
                val modelItemsDistinctById = modelItems.distinctBy { modelItem ->
                    modelItem.resolveIdentifier()
                }
                /**
                 * 如果为一个Item在同一个区域配置重复的本地化，LEFT JOIN 会产生两条id相同的Item，这种情况下先给出错误提醒，需要在数据库侧移除多余的本地化项
                 */
                if (modelItemsDistinctById.size != modelItems.size) {
                    LOG?.e { "query ${itemClass.simpleName} has same ids: DistinctById=${modelItemsDistinctById.size}, modelItems=${modelItems.size}" }
                }
                modelItemsDistinctById
            }?.let {
                val result = filterModels(options, it)
                val hasSortField = itemClass.hasSortField()
                if (hasSortField) {
                    result.sortedBy { model ->
                        model.resolveMethod(MODEL_METHOD_GET_SORT)?.toIntOrNull() ?: 0
                    }
                } else {
                    result
                }
            }
        }.also { result ->
            LOG?.d { "query ${itemClass.simpleName} count: ${result?.size}, take ${System.currentTimeMillis() - startMs}ms" }
        }
    }

    private fun <T : Model> filterModels(
        options: QueryOptions,
        inputList: List<T>
    ): List<T> {
        if (options.queryPredicate.toString().contains("field: online")) {
            return inputList.filter {
                options.queryPredicate.evaluate(it)
            }
        }
        return inputList
    }

    private fun initSQLiteStorageAdapter(sqLiteStorageAdapter: SQLiteStorageAdapter) {
        sqLiteStorageAdapter.initialize(
            appContext,
            {
                initializationsPending.countDown()
                sqLiteStorageAdapter.dbVersionCheckListener?.onSqliteInitializedSuccess()
                LOG?.d { "initSQLiteStorageAdapter finish" }
            },
            {
                LOG?.e(it) { "initSQLiteStorageAdapter error" }
            },
            dataStoreConfiguration
        )
    }

    fun <R> use(action: (SQLiteStorageAdapter) -> R): R? {
        return try {
            initializationsPending.await()
            action(sqLiteStorageAdapter)
        } catch (cause: Throwable) {
            LOG?.e(cause) { "use sqLiteStorageAdapter error" }
            null
        }
    }
}