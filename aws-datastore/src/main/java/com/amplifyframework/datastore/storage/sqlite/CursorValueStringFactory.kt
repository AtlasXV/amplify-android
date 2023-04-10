package com.amplifyframework.datastore.storage.sqlite

import android.database.Cursor
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn
import java.util.HashMap

/**
 * weiping@atlasv.com
 * 2023/3/25
 */
open class CursorValueStringFactory {
    open fun getStringFromCursor(cursor: Cursor, column: SQLiteColumn): Pair<Int, String?>? {
        val columnIndex = cursor.getColumnIndexOrThrow(column.aliasedName)
        return getStringFromCursor(cursor, columnIndex)
    }

    fun getStringFromCursor(cursor: Cursor, columnIndex: Int): Pair<Int, String?>? {
        // This check is necessary, because primitive values will return 0 even when null
        if (cursor.isNull(columnIndex)) {
            return null
        }
        return columnIndex to cursor.getString(columnIndex)
    }

    open fun onMapForModelBuilt(map: HashMap<String, Any>, modelSchema: ModelSchema) {}
}