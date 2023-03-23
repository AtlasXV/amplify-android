package com.atlasv.android.amplify.simpleappsync.storage.sqlite

import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.storage.sqlite.SQLCommandProcessor
import com.amplifyframework.datastore.storage.sqlite.SqlCommand
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent

/**
 * weiping@atlasv.com
 * 2023/3/23
 */
class UpdateSingleTableColum(
    private val processor: SQLCommandProcessor,
    private val targetModelClass: Class<out Model>,
) {
    private val LOG get() = AmplifySimpleSyncComponent.LOG
    private val targetTableName = targetModelClass.simpleName
    fun setNullColumnFromAnother(targetField: String, fromField: String) {
        val sqlStatement =
            "UPDATE `$targetTableName` " +
                    "SET $targetField = $fromField " +
                    "WHERE $targetField IS NULL " +
                    "AND $fromField IS NOT NULL;"
        LOG.info("UpdateSingleTableColum setNullColumnFromAnother: $sqlStatement")
        val command = SqlCommand(targetTableName, sqlStatement, emptyList())
        try {
            processor.execute(command)
        } catch (cause: Throwable) {
            LOG.error("UpdateSingleTableColum setNullColumnFromAnother error", cause)
        }
    }

    fun setNullColumnDefaultValue(targetField: String, destValue: Any) {
        val sqlStatement =
            "UPDATE `$targetTableName` " +
                    "SET $targetField = $destValue " +
                    "WHERE $targetField IS NULL;"
        LOG.info("UpdateSingleTableColum setNullColumnDefaultValue: $sqlStatement")
        val command = SqlCommand(targetTableName, sqlStatement, emptyList())
        try {
            processor.execute(command)
        } catch (cause: Throwable) {
            LOG.error("UpdateSingleTableColum setNullColumnDefaultValue error", cause)
        }
    }
}