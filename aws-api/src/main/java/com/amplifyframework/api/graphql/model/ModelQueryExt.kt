package com.amplifyframework.api.graphql.model

import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory
import com.amplifyframework.api.aws.GraphQLRequestOptions
import com.amplifyframework.api.aws.ext.AppSyncGraphQLRequestFactoryExt
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.util.TypeMaker

/**
 * weiping@atlasv.com
 * 2023/2/23
 */
object ModelQueryExt {
    inline fun <reified M : Model> list(
        options: GraphQLRequestOptions,
        predicate: QueryPredicate = QueryPredicates.all(),
        pageLimit: Int = ModelPagination.DEFAULT_LIMIT,
    ): GraphQLRequest<PaginatedResult<ModelWithMetadata<M>>> {
        val modelType = M::class.java
        return AppSyncGraphQLRequestFactory.buildListQueryInternal(
            modelClass = modelType,
            predicate = predicate,
            limit = pageLimit,
            responseType = TypeMaker.getParameterizedType(
                PaginatedResult::class.java, ModelWithMetadata::class.java, modelType
            ),
            includes = null,
            pageToken = null,
            requestOptions = options
        )
    }

    inline fun <reified M : Model> get(
        modelId: String, requestOptions: GraphQLRequestOptions
    ): GraphQLRequest<M> {
        return AppSyncGraphQLRequestFactoryExt.buildQuery(M::class.java, modelId, requestOptions)
    }

}