package com.atlasv.android.amplify.simpleappsync.storage.sqlite

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.datastore.storage.sqlite.SQLCommandProcessor
import com.amplifyframework.datastore.storage.sqlite.SqlCommand
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent

/**
 * weiping@atlasv.com
 * 2023/3/23
 */
class UpdateFromTable(
    private val processor: SQLCommandProcessor,
    private val targetModelClass: Class<out Model>,
    private val fromModelClass: Class<out Model>
) {
    private val LOG get() = AmplifySimpleSyncComponent.LOG
    private val targetTableName = targetModelClass.simpleName
    private val fromTableName = fromModelClass.simpleName
    fun execute(fieldPairs: List<Pair<QueryField, QueryField>>, fromMatchKey: String) {
        val updateColumns = fieldPairs.joinToString(prefix = "(", postfix = ")") { (targetField, _) ->
            targetField.fieldName
        }
        val fromColumns = fieldPairs.joinToString { (_, fromField) ->
            fromField.fieldName
        }
        // UPDATE `VFX` SET (displayName, sort) = (SELECT name, sort from `VFXLocale` WHERE vfxID = `VFX`.id);
        val sqlStatement =
            "UPDATE `$targetTableName` SET $updateColumns = (SELECT $fromColumns FROM `$fromTableName` WHERE $fromMatchKey = `$targetTableName`.id);"
        LOG.info("UpdateFromTable build sqlStatement: $sqlStatement")
        val command = SqlCommand(targetTableName, sqlStatement, emptyList())
        try {
            processor.execute(command)
        } catch (cause: Throwable) {
            LOG.error("UpdateFromTable error", cause)
        }
    }
}