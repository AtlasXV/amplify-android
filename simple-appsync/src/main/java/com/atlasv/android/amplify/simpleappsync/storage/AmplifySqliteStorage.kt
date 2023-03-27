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
import java.util.concurrent.CountDownLatch

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifySqliteStorage(
    val appContext: Context,
    val dataStoreConfiguration: DataStoreConfiguration,
    val modelProvider: ModelProvider,
    val schemaRegistry: SchemaRegistry,
    sqlCommandFactoryFactory: SQLCommandFactoryFactory,
    cursorValueStringFactory: CursorValueStringFactory
) {
    private val initializationsPending = CountDownLatch(1)
    val sqLiteStorageAdapter = SQLiteStorageAdapter.forModels(schemaRegistry, modelProvider).also {
        it.sqlCommandFactoryFactory = sqlCommandFactoryFactory
        it.cursorValueStringFactory = cursorValueStringFactory
        initSQLiteStorageAdapter(it)
    }

    fun <T : Model> query(itemClass: Class<T>, options: QueryOptions): List<T>? {
        return use {
            it.sqlQueryProcessor?.queryOfflineData(itemClass, options) { cause ->
                LOG.error("query ${itemClass.simpleName} error", cause)
            }.also { result ->
                LOG.info("query ${itemClass.simpleName} count: ${result?.size}")
            }
        }
    }

    private fun initSQLiteStorageAdapter(sqLiteStorageAdapter: SQLiteStorageAdapter) {
        sqLiteStorageAdapter.initialize(
            appContext,
            {
                initializationsPending.countDown()
                LOG.info("initSQLiteStorageAdapter finish")
            },
            {
                LOG.error("initSQLiteStorageAdapter error", it)
            },
            dataStoreConfiguration
        )
    }

    fun <R> use(action: (SQLiteStorageAdapter) -> R): R? {
        return try {
            initializationsPending.await()
            action(sqLiteStorageAdapter)
        } catch (cause: Throwable) {
            LOG.error("use sqLiteStorageAdapter error", cause)
            null
        }
    }
}