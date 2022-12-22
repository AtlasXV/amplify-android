package com.atlasv.android.amplify.simpleappsync.response

import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.atlasv.android.amplify.simpleappsync.storage.deleteList
import com.atlasv.android.amplify.simpleappsync.ext.ensureDisplayName
import com.atlasv.android.amplify.simpleappsync.storage.saveList
import com.atlasv.android.amplify.simpleappsync.storage.AmplifySqliteStorage

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifyModelMerger(private val sqliteStorage: AmplifySqliteStorage) {
    fun <T : Model> mergeAll(modelWithMetadataList: List<ModelWithMetadata<T>>) {
        sqliteStorage.use { adapter ->

            adapter.saveList(modelWithMetadataList.mapNotNull {
                it.takeIf { !it.isDeleted }?.model?.apply {
                    ensureDisplayName()
                }
            })

            adapter.deleteList(modelWithMetadataList.mapNotNull {
                it.takeIf { it.isDeleted }?.model
            })
        }
    }
}