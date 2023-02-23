package com.amplifyframework.api.aws.ext

import com.amplifyframework.api.aws.AppSyncGraphQLRequest
import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.MutationType
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.appsync.ModelMetadata
import com.amplifyframework.datastore.appsync.ModelWithMetadataAdapter

/**
 * weiping@atlasv.com
 * 2023/2/23
 */
object AppSyncGraphQLRequestFactoryExt {
    fun <R, T : Model> buildMutation(
        model: T, predicate: QueryPredicate, type: MutationType, syncMetadata: ModelMetadata
    ): GraphQLRequest<R> {
        return AppSyncGraphQLRequestFactory.buildMutation<R, T>(
            model, predicate, type
        ).also {
            val request = it as AppSyncGraphQLRequest
            val inputVariableMap = request.variables["input"] as? HashMap<Any, Any>
            inputVariableMap ?: return@also
            if (!inputVariableMap.containsKey(ModelWithMetadataAdapter.VERSION_KEY)) {
                inputVariableMap[ModelWithMetadataAdapter.VERSION_KEY] = syncMetadata.version ?: 0
            }
        }
    }
}