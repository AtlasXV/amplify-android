package com.amplifyframework.api.graphql.model

import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory
import com.amplifyframework.api.aws.ext.AppSyncGraphQLRequestFactoryExt
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.MutationType
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.datastore.appsync.ModelMetadata

/**
 * weiping@atlasv.com
 * 2023/2/23
 */
object ModelMutationExt {
    fun <M : Model> update(model: M, syncMetadata: ModelMetadata): GraphQLRequest<M> {
        return AppSyncGraphQLRequestFactoryExt.buildMutation(
            model, QueryPredicates.all(), MutationType.UPDATE, syncMetadata
        )
    }
}