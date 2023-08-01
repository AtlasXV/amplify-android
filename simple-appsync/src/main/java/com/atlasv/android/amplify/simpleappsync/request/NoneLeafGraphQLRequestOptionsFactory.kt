package com.atlasv.android.amplify.simpleappsync.request

import com.amplifyframework.api.aws.GraphQLRequestOptions
import com.amplifyframework.api.aws.LeafSerializationBehavior
import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.appsync.DataStoreGraphQLRequestOptions

/**
 * 避免数据嵌套，减少数据量。根据使用场景，我们只需取第一层数据。
 *
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