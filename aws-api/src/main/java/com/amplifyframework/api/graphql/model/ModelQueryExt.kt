package com.amplifyframework.api.graphql.model

import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory
import com.amplifyframework.api.aws.GraphQLRequestOptions
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.util.TypeMaker

/**
 * weiping@atlasv.com
 * 2023/2/23
 */
object ModelQueryExt {
    fun <M : Model> list(
        modelType: Class<M>,
        predicate: QueryPredicate,
        options: GraphQLRequestOptions,
        pageLimit: Int,
    ): GraphQLRequest<PaginatedResult<ModelWithMetadata<M>>> {
        return AppSyncGraphQLRequestFactory.buildQuery(
            modelType, predicate, pageLimit, TypeMaker.getParameterizedType(
                PaginatedResult::class.java, ModelWithMetadata::class.java, modelType
            ), options
        )
    }
}