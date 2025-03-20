package com.atlasv.android.amplify.simpleappsync.storage

import android.content.Context
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.core.model.query.QueryOptions
import com.amplifyframework.datastore.storage.sqlite.CursorValueStringFactory
import com.amplifyframework.datastore.storage.sqlite.SQLCommandFactoryFactory
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent
import com.atlasv.android.amplify.simpleappsync.ext.MODEL_METHOD_GET_SORT
import com.atlasv.android.amplify.simpleappsync.ext.hasSortField
import com.atlasv.android.amplify.simpleappsync.ext.resolveMethod
import java.io.File
import java.util.concurrent.CountDownLatch

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifySqliteStorage(
    val appContext: Context,
    val modelProvider: ModelProvider,
    val schemaRegistry: SchemaRegistry,
    sqlCommandFactoryFactory: SQLCommandFactoryFactory,
    cursorValueStringFactory: CursorValueStringFactory,
    private val externalDbFileSupplier: () -> File,
    private val onSqliteInitSuccess: () -> Unit
) {
    private val logger by lazy {
        AmplifySimpleSyncComponent.loggerFactory?.invoke("amplify:sqlite-storage")
    }
    val initializationsLatch = CountDownLatch(1)
    val sqLiteStorageAdapter = SQLiteStorageAdapter.forModels(schemaRegistry, modelProvider).also {
        it.sqlCommandFactoryFactory = sqlCommandFactoryFactory
        it.cursorValueStringFactory = cursorValueStringFactory
        it.dbVersionCheckListener =
            AmplifyBuildInDbProvider(appContext, externalDbFileSupplier, onSqliteInitSuccess)
        initSQLiteStorageAdapter(it)
    }

    fun <T : Model> query(itemClass: Class<T>, options: QueryOptions): List<T>? {
        val startMs = System.currentTimeMillis()
        return use { adapter ->
            adapter.sqlQueryProcessor?.queryOfflineData(itemClass, options) { cause ->
                logger?.e(cause) { "query ${itemClass.simpleName} error" }
            }?.let { modelItems ->
                val modelItemsDistinctById = modelItems.distinctBy { modelItem ->
                    modelItem.resolveIdentifier()
                }
                /**
                 * 如果为一个Item在同一个区域配置重复的本地化，LEFT JOIN 会产生两条id相同的Item，这种情况下先给出错误提醒，需要在数据库侧移除多余的本地化项
                 */
                if (modelItemsDistinctById.size != modelItems.size) {
                    logger?.e { "query ${itemClass.simpleName} has same ids: DistinctById=${modelItemsDistinctById.size}, modelItems=${modelItems.size}" }
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
            logger?.d { "query ${itemClass.simpleName} count: ${result?.size}, take ${System.currentTimeMillis() - startMs}ms" }
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
                initializationsLatch.countDown()
                sqLiteStorageAdapter.dbVersionCheckListener?.onSqliteInitializedSuccess()
                logger?.d { "initSQLiteStorageAdapter finish" }
            },
            {
                logger?.e(it) { "initSQLiteStorageAdapter error" }
            }
        )
    }

    fun <R> use(action: (SQLiteStorageAdapter) -> R): R? {
        return try {
            initializationsLatch.await()
            action(sqLiteStorageAdapter)
        } catch (cause: Throwable) {
            logger?.e(cause) { "use sqLiteStorageAdapter error" }
            null
        }
    }
}