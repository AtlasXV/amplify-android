package com.amplifyframework.datastore.storage.sqlite

import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable

/**
 * weiping@atlasv.com
 * 2023/3/24
 */
class CustomTableJoinConfig(
    private val modelSchema: ModelSchema, private val joinType: SqlKeyword, private val joinOnCondition: String
) {
    val table by lazy {
        SQLiteTable.fromSchema(modelSchema)
    }
    fun getJoinOnStatement(): String {
        return "$joinType `${modelSchema.name}` ${SqlKeyword.ON} $joinOnCondition"
    }
}