package com.amplifyframework.api.aws.ext

import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.MutationType
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.appsync.ModelWithMetadataAdapter

/**
 * weiping@atlasv.com
 * 2023/2/23
 */
object AppSyncGraphQLRequestFactoryExt {
    fun <R, T : Model, M : ModelWithMetadata<T>> buildMutation(
        modelWithMetadata: M, predicate: QueryPredicate, type: MutationType
    ): GraphQLRequest<R> {
        val syncMetadata = modelWithMetadata.syncMetadata
        val model = modelWithMetadata.model
        return AppSyncGraphQLRequestFactory.buildMutation<R, T>(
            model, predicate, type
        ).also {
            if (!it.variables.containsKey(ModelWithMetadataAdapter.VERSION_KEY)) {
                it.variables[ModelWithMetadataAdapter.VERSION_KEY] = syncMetadata.version
            }
        }
    }
}