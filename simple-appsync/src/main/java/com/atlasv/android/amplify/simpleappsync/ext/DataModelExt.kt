package com.atlasv.android.amplify.simpleappsync.ext

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.core.model.query.predicate.QueryPredicate
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