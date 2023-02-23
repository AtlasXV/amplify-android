package com.amplifyframework.api.aws.ext

import com.amplifyframework.AmplifyException
import com.amplifyframework.api.aws.ApiGraphQLRequestOptions
import com.amplifyframework.api.aws.AppSyncGraphQLRequest
import com.amplifyframework.api.aws.AppSyncGraphQLRequestFactory
import com.amplifyframework.api.aws.GraphQLRequestOptions
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.MutationType
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.api.graphql.QueryType
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.datastore.appsync.ModelMetadata
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.appsync.ModelWithMetadataAdapter
import com.amplifyframework.util.TypeMaker

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

    fun <R, T : Model> buildQuery(
        modelClass: Class<T>, objectId: String, requestOptions: GraphQLRequestOptions
    ): GraphQLRequest<R> {
        return try {
            AppSyncGraphQLRequest.builder().modelClass(modelClass).operation(QueryType.GET)
                .requestOptions(requestOptions).responseType(
                    TypeMaker.getParameterizedType(
                        ModelWithMetadata::class.java, modelClass
                    )
                ).variable("id", "ID!", objectId).build()
        } catch (exception: AmplifyException) {
            throw IllegalStateException(
                "Could not generate a schema for the specified class", exception
            )
        }
    }
}