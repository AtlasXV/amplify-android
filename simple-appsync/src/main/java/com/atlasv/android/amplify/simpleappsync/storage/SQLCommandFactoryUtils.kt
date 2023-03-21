package com.atlasv.android.amplify.simpleappsync.storage

import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.storage.sqlite.SqlKeyword
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLPredicate
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable
import com.amplifyframework.util.Wrap

/**
 * weiping@atlasv.com
 * 2023/3/20
 */
object SQLCommandFactoryUtils {
    /**
     * `name` = ?
     */
    fun buildWhereParam(stringBuilder: StringBuilder, columnName: String) {
        stringBuilder.append(Wrap.inBackticks(columnName)).append(SqlKeyword.DELIMITER).append(SqlKeyword.EQUAL)
            .append(SqlKeyword.DELIMITER).append("?")
    }

    /**
     * UPDATE `VFX` SET
     */
    fun buildUpdateTableStateHeader(tableName: String): StringBuilder {
        return StringBuilder().append("UPDATE").append(SqlKeyword.DELIMITER).append(Wrap.inBackticks(tableName))
            .append(SqlKeyword.DELIMITER).append("SET").append(SqlKeyword.DELIMITER)
    }

    fun buildMatchIdSQLPredicate(sqliteTable: SQLiteTable, id: String): SQLPredicate {
        val primaryKeyName = sqliteTable.primaryKeyColumnName
        val matchId: QueryPredicate = QueryField.field(primaryKeyName).eq(id)
        return SQLPredicate(matchId)
    }

    /**
     * WHERE `VFX`.`id` = ?;
     */
    fun appendWhereMatchIdStatement(stringBuilder: StringBuilder, sqlPredicate: SQLPredicate) {
        stringBuilder.append(SqlKeyword.DELIMITER).append(SqlKeyword.WHERE).append(SqlKeyword.DELIMITER)
            .append(sqlPredicate).append(";")
    }
}