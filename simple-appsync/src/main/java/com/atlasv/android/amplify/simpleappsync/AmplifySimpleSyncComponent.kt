package com.atlasv.android.amplify.simpleappsync

import android.content.Context
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.storage.sqlite.CursorValueStringFactory
import com.amplifyframework.datastore.storage.sqlite.SQLCommandFactoryFactory
import com.amplifyframework.kotlin.core.Amplify
import com.atlasv.android.amplify.simpleappsync.config.AmplifySimpleSyncConfig
import com.atlasv.android.amplify.simpleappsync.ext.AmplifyExtSettings
import com.atlasv.android.amplify.simpleappsync.storage.AmplifySqliteStorage
import kotlinx.coroutines.sync.Mutex

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifySimpleSyncComponent(
    private val appContext: Context,
    val dataStoreConfiguration: DataStoreConfiguration,
    val modelProvider: ModelProvider,
    val schemaRegistry: SchemaRegistry,
    sqlCommandFactoryFactory: SQLCommandFactoryFactory,
    cursorValueStringFactory: CursorValueStringFactory,
    private val buildInDbMigrate: (String) -> Unit,
    private val onSqliteInitSuccess: () -> Unit,
    private val config: AmplifySimpleSyncConfig
) {
    val extSettings by lazy {
        AmplifyExtSettings(appContext)
    }
    val storage by lazy {
        AmplifySqliteStorage(
            appContext,
            dataStoreConfiguration,
            extSettings,
            modelProvider,
            schemaRegistry,
            sqlCommandFactoryFactory,
            cursorValueStringFactory,
            buildInDbMigrate,
            config,
            onSqliteInitSuccess,
        )
    }

    companion object {
        val LOG by lazy {
            Amplify.Logging.forNamespace("amplify:simple-sync")
        }
    }
}