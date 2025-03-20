package com.atlasv.android.amplify.simpleappsync

import android.content.Context
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.datastore.storage.sqlite.CursorValueStringFactory
import com.amplifyframework.datastore.storage.sqlite.SQLCommandFactoryFactory
import com.atlasv.android.amplify.simpleappsync.storage.AmplifySqliteStorage
import com.atlasv.android.amplify.simpleappsync.util.AmplifyLogger
import java.io.File

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
    private val externalDbFileSupplier: () -> File,
    private val onSqliteInitSuccess: () -> Unit
) {
    val storage by lazy {
        AmplifySqliteStorage(
            appContext,
            modelProvider,
            schemaRegistry,
            sqlCommandFactoryFactory,
            cursorValueStringFactory,
            externalDbFileSupplier,
            onSqliteInitSuccess,
        )
    }

    companion object {
        var loggerFactory: ((tag: String) -> AmplifyLogger)? = null
        val defaultLogger by lazy {
            loggerFactory?.invoke("amplify:app")
        }
    }
}