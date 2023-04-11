package com.atlasv.android.amplify.simpleappsync.request

import com.amplifyframework.api.aws.GraphQLRequestOptions
import com.amplifyframework.core.model.Model

/**
 * weiping@atlasv.com
 * 2023/4/11
 */
interface GraphQLRequestOptionsFactory {
    fun <T : Model> create(modelClass: Class<T>): GraphQLRequestOptions
}