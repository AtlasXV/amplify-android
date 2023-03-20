package com.atlasv.android.amplify.simpleappsync.ext

import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.api.graphql.model.ModelPagination
import com.amplifyframework.api.graphql.model.ModelQueryExt
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.datastore.appsync.DataStoreGraphQLRequestOptions
import com.amplifyframework.datastore.appsync.ModelWithMetadata

/**
 * weiping@atlasv.com
 * 2023/2/23
 */
object DataStoreModelQueryExt {
    inline fun <reified M : Model> list(
        predicate: QueryPredicate = QueryPredicates.all(),
        pageLimit: Int = ModelPagination.DEFAULT_LIMIT,
    ): GraphQLRequest<PaginatedResult<ModelWithMetadata<M>>> {
        return ModelQueryExt.list(options = DataStoreGraphQLRequestOptions(), predicate, pageLimit)
    }

    inline fun <reified M : Model> get(
        modelId: String
    ): GraphQLRequest<M> {
        return ModelQueryExt.get(modelId, DataStoreGraphQLRequestOptions())
    }
}