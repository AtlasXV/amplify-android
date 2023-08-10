package com.amplifyframework.datastore.storage.sqlite

import android.database.Cursor
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn

/**
 * weiping@atlasv.com
 * 2023/3/25
 */
open class CursorValueStringFactory {
    open fun getStringFromCursor(
        cursor: Cursor,
        tableName: String,
        columnName: String
    ): Pair<Int, String?>? {
        /**
         * [SQLiteColumn.getAliasedName]: tableName_name
         */
        val columnAliasedName = tableName + SQLiteColumn.CUSTOM_ALIAS_DELIMITER + columnName
        val columnIndex = cursor.getColumnIndexOrThrow(columnAliasedName)
        return getStringFromCursor(cursor, columnIndex)
    }

    fun getStringFromCursor(cursor: Cursor, columnIndex: Int): Pair<Int, String?> {
        // This check is necessary, because primitive values will return 0 even when null
        if (cursor.isNull(columnIndex)) {
            return columnIndex to null
        }
        return columnIndex to cursor.getString(columnIndex)
    }

    open fun onMapForModelBuilt(map: HashMap<String, Any>, modelSchema: ModelSchema) {}
}