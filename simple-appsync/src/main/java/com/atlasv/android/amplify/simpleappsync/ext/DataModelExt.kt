package com.atlasv.android.amplify.simpleappsync.ext

import android.database.sqlite.SQLiteDatabase
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.storage.sqlite.PersistentModelVersion
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent.Companion.LOG
import java.io.File
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date

/**
 * weiping@atlasv.com
 * 2022/12/22
 */

const val MODEL_METHOD_GET_SORT = "getSort"

fun <T : Model> Class<T>.hasSortField(): Boolean {
    return kotlin.runCatching {
        methods.any { it.name == MODEL_METHOD_GET_SORT }
    }.getOrElse { false }
}

fun Model.resolveMethod(methodName: String): String? {
    return try {
        if (!this.javaClass.methods.any { it.name == methodName }) {
            return null
        }
        val method: Method = this.javaClass.getMethod(methodName)
        method.invoke(this)?.toString() ?: ""
    } catch (exception: Throwable) {
        null
    }
}

fun Date.simpleFormat(): String {
    return SimpleDateFormat.getDateTimeInstance().format(this)
}

fun <T : Model> Class<T>.queryField(fieldName: String): QueryField? {
    return declaredFields.find { it.name == fieldName }?.let {
        QueryField.field(fieldName)
    }
}

fun QueryPredicate.then(newPredicate: QueryPredicate?): QueryPredicate {
    newPredicate ?: return this
    return this.and(newPredicate)
}

fun File.getDbVersion(): String? {
    val dbFile = this
    if (!dbFile.exists()) {
        return null
    }
    val db = SQLiteDatabase.openDatabase(dbFile.path, null, 0)
    val tableName = PersistentModelVersion::class.simpleName
    return db.use {
        val idColumnName = "${tableName}_id"
        val versionColumnName = "${tableName}_version"
        db.rawQuery(
            "SELECT `${tableName}`.`id` AS `${idColumnName}`, `${tableName}`.`version` AS `${versionColumnName}` FROM `${tableName}`;",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val modelId = cursor.getString(cursor.getColumnIndexOrThrow(idColumnName))
                val modelVersion = cursor.getString(cursor.getColumnIndexOrThrow(versionColumnName))
                LOG?.d { "getBuildInDbVersion: modelId=$modelId, modelVersion=$modelVersion, file=${this.path}" }
                modelVersion
            } else {
                null
            }
        }
    }
}