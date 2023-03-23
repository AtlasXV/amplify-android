package com.atlasv.android.amplify.simpleappsync.request

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.datastore.DataStoreConfiguration
import com.atlasv.android.amplify.simpleappsync.ext.queryField
import com.atlasv.android.amplify.simpleappsync.ext.then
import java.util.*

/**
 * weiping@atlasv.com
 * 2023/3/20
 */
open class ModelQueryPredicateFactory {
    fun <T : Model> create(
        modelClass: Class<T>,
        dataStoreConfiguration: DataStoreConfiguration,
        lastSync: Long,
        grayRelease: Int
    ): QueryPredicate {
        val predicate =
            dataStoreConfiguration.syncExpressions[modelClass.simpleName]?.resolvePredicate() ?: QueryPredicates.all()
        return predicate.then(modelClass.queryField("grayRelease")?.gt(grayRelease))
            .let {
                if (lastSync > 0) {
                    it.then(modelClass.queryField("updatedAt")?.gt(Temporal.DateTime(Date(lastSync), 0)))
                } else {
                    it
                }
            }
    }
}