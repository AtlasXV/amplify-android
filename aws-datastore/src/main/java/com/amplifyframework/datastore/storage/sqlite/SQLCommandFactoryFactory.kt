package com.amplifyframework.datastore.storage.sqlite

import com.amplifyframework.core.model.SchemaRegistry
import com.google.gson.Gson

/**
 * weiping@atlasv.com
 * 2023/3/24
 */
open class SQLCommandFactoryFactory {
    open fun create(
        schemaRegistry: SchemaRegistry, gson: Gson
    ): SQLCommandFactory {
        return SQLiteCommandFactory(schemaRegistry, gson)
    }
}