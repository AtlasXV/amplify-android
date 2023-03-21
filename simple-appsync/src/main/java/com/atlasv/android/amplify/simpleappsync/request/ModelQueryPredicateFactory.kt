package com.atlasv.android.amplify.simpleappsync.request

import android.content.Context
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.core.model.temporal.Temporal
import com.amplifyframework.datastore.DataStoreConfiguration
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent
import com.atlasv.android.amplify.simpleappsync.ext.AmplifyExtSettings
import com.atlasv.android.amplify.simpleappsync.ext.queryField
import com.atlasv.android.amplify.simpleappsync.ext.then
import java.util.*

/**
 * weiping@atlasv.com
 * 2023/3/20
 */
open class ModelQueryPredicateFactory(private val appContext: Context) {
    val LOG get() = AmplifySimpleSyncComponent.LOG
    suspend fun <T : Model> create(
        modelClass: Class<T>,
        dataStoreConfiguration: DataStoreConfiguration,
        lastSync: Long,
        grayRelease: Int,
        locale: String
    ): QueryPredicate {
        val lastLocale = AmplifyExtSettings.getLastModelLocale(appContext)
        val rebuildLocale = lastLocale != locale
        val targetSync = if (rebuildLocale && modelClass.simpleName.endsWith("Locale")) 0 else lastSync
        val predicate =
            dataStoreConfiguration.syncExpressions[modelClass.simpleName]?.resolvePredicate() ?: QueryPredicates.all()
        return predicate.then(modelClass.queryField("grayRelease")?.gt(grayRelease))
            .then(modelClass.queryField("locale")?.eq(locale)).let {
                if (targetSync > 0) {
                    it.then(modelClass.queryField("updatedAt")?.gt(Temporal.DateTime(Date(lastSync), 0)))
                } else {
                    it
                }
            }
    }
}