package com.atlasv.android.amplify.simpleappsync

import android.content.Context
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.datastore.storage.sqlite.CursorValueStringFactory
import com.amplifyframework.datastore.storage.sqlite.SQLCommandFactoryFactory
import com.atlasv.android.amplify.simpleappsync.config.AmplifySimpleSyncConfig
import com.atlasv.android.amplify.simpleappsync.ext.AmplifyExtSettings
import com.atlasv.android.amplify.simpleappsync.storage.AmplifySqliteStorage
import com.atlasv.android.amplify.simpleappsync.util.AmplifyLogger

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifySimpleSyncComponent(
    private val appContext: Context,
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
        var LOG: AmplifyLogger? = null
    }
}