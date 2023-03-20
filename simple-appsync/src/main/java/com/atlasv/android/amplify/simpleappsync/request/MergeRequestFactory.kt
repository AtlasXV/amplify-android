package com.atlasv.android.amplify.simpleappsync.request

import android.content.Context
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.amplifyframework.api.graphql.PaginatedResult
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelProvider
import com.amplifyframework.core.model.SchemaRegistry
import com.amplifyframework.datastore.DataStoreConfiguration
import com.amplifyframework.datastore.appsync.ModelWithMetadata

/**
 * [灰度发布说明](https://cwtus1pn64.feishu.cn/docs/doccnDayF0ZErCLnMyySPGJchOg#9m94LF)
 * weiping@atlasv.com
 * 2022/12/7
 */
interface MergeRequestFactory {
    fun create(
        appContext: Context,
        dataStoreConfiguration: DataStoreConfiguration,
        modelProvider: ModelProvider,
        schemaRegistry: SchemaRegistry,
        lastSync: Long,
        grayRelease: Int,
        locale: String,
        rebuildLocale: Boolean
    ): GraphQLRequest<List<GraphQLResponse<PaginatedResult<ModelWithMetadata<Model>>>>>
}

