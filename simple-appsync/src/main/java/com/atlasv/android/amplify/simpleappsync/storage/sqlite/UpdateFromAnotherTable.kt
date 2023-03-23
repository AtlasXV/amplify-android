package com.atlasv.android.amplify.simpleappsync.storage.sqlite

import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.storage.sqlite.SQLCommandProcessor
import com.amplifyframework.datastore.storage.sqlite.SqlCommand
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent

/**
 * weiping@atlasv.com
 * 2023/3/23
 */
class UpdateFromAnotherTable(
    private val processor: SQLCommandProcessor,
    private val targetModelClass: Class<out Model>,
    private val fromModelClass: Class<out Model>
) {
    private val LOG get() = AmplifySimpleSyncComponent.LOG
    private val targetTableName = targetModelClass.simpleName
    private val fromTableName = fromModelClass.simpleName
    fun execute(fieldPairs: List<Pair<String, String>>, fromMatchKey: String, conditionStatements: List<String>) {
        val updateColumns = fieldPairs.joinToString(prefix = "(", postfix = ")") { (targetField, _) ->
            targetField
        }
        val fromColumns = fieldPairs.joinToString { (_, fromField) ->
            fromField
        }

        val isNotNullStatement = fieldPairs.joinToString(" AND ") { (_, fromField) ->
            "$fromField IS NOT NULL"
        }

        val isNotEqualStatement = fieldPairs.joinToString(" AND ") { (targetField, fromField) ->
            "$fromField != `$targetTableName`.${targetField}"
        }

        val matchIdStatement = "$fromMatchKey = `$targetTableName`.id"

        val conditionStatement = conditionStatements.joinToString(" AND ") {
            it
        }

        // UPDATE `VFX` SET (displayName, sort) = (SELECT name, sort from `VFXLocale` WHERE vfxID = `VFX`.id);
        val sqlStatement =
            "UPDATE `$targetTableName` " +
                    "SET $updateColumns = " +
                    "(SELECT $fromColumns FROM `$fromTableName` " +
                    "WHERE $matchIdStatement " +
                    "AND $isNotNullStatement " +
                    "AND $isNotEqualStatement AND $conditionStatement);"
        LOG.info("UpdateFromAnotherTable build sqlStatement: $sqlStatement")
        val command = SqlCommand(targetTableName, sqlStatement, emptyList())
        try {
            processor.execute(command)
        } catch (cause: Throwable) {
            LOG.error("UpdateFromAnotherTable error", cause)
        }
    }
}