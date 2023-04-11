package com.atlasv.android.amplify.simpleappsync.request

import com.amplifyframework.api.aws.GraphQLRequestOptions
import com.amplifyframework.api.aws.LeafSerializationBehavior
import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.appsync.DataStoreGraphQLRequestOptions

/**
 * weiping@atlasv.com
 * 2023/4/11
 */
class NoneLeafGraphQLRequestOptionsFactory : GraphQLRequestOptionsFactory {
    override fun <T : Model> create(modelClass: Class<T>): GraphQLRequestOptions {
        return object : DataStoreGraphQLRequestOptions() {
            override fun leafSerializationBehavior(): LeafSerializationBehavior {
                return LeafSerializationBehavior.NONE
            }
        }
    }
}