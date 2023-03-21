package com.atlasv.android.amplify.simpleappsync.storage

import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.storage.sqlite.SqlCommand
import com.amplifyframework.datastore.storage.sqlite.SqlKeyword
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLPredicate
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable
import com.amplifyframework.util.Wrap
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent

/**
 * weiping@atlasv.com
 * 2023/3/20
 */
object SQLCommandFactoryExt {
    private const val COLUMN_NAME = "name"
    private const val COLUMN_SORT = "sort"
    val LOG get() = AmplifySimpleSyncComponent.LOG

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

    /**
     * UPDATE `VFX` SET `name` = ?, `sort` = ? WHERE `VFX`.`id` = ?;
     */
    fun updateLocaleFor(
        modelSchema: ModelSchema, modelId: String, itemDisplayName: String?, sort: Int?
    ): SqlCommand? {
        val localeColumns = setOf(COLUMN_NAME, COLUMN_SORT)
        val bindings = arrayListOf<Any>()

        val table = SQLiteTable.fromSchema(modelSchema)
        val stringBuilder = buildUpdateTableStateHeader(table.name)

        val columns = table.sortedColumns
        val columnsIterator: Iterator<SQLiteColumn> = columns.iterator()
        while (columnsIterator.hasNext()) {
            val columnName = columnsIterator.next().name
            if (!localeColumns.contains(columnName)) {
                continue
            }

            if (columnName == COLUMN_NAME && !itemDisplayName.isNullOrEmpty()) {
                buildWhereParam(stringBuilder, columnName)
                bindings.add(itemDisplayName)
            }
            if (columnName == COLUMN_SORT && sort != null) {
                if (!bindings.isEmpty()) {
                    stringBuilder.append(", ")
                }
                buildWhereParam(stringBuilder, columnName)
                bindings.add(sort)
            }
        }
        if (bindings.isEmpty()) {
            LOG.warn("updateLocaleFor(${modelSchema.name}-$modelId): no valid bindings, return null")
            return null
        }

        // Append WHERE statement
        val sqliteTable = SQLiteTable.fromSchema(modelSchema)
        val sqlPredicate = buildMatchIdSQLPredicate(sqliteTable, modelId)
        appendWhereMatchIdStatement(stringBuilder, sqlPredicate)
        val preparedUpdateStatement = stringBuilder.toString()

        bindings.addAll(sqlPredicate.bindings) // WHERE clause
        return SqlCommand(
            table.name, preparedUpdateStatement, bindings
        )
    }
}