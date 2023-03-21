package com.atlasv.android.amplify.simpleappsync.response

import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.atlasv.android.amplify.simpleappsync.response.merge.ItemMergeToDbStrategy
import com.atlasv.android.amplify.simpleappsync.storage.AmplifySqliteStorage
import com.atlasv.android.amplify.simpleappsync.storage.deleteList
import com.atlasv.android.amplify.simpleappsync.storage.saveList

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifyModelMerger(
    private val sqliteStorage: AmplifySqliteStorage,
    private val mergeToDbStrategy: ItemMergeToDbStrategy = ItemMergeToDbStrategy()
) {

    fun mergeResponse(responseItemGroups: List<List<ModelWithMetadata<Model>>>) {
        for (group in responseItemGroups) {
            mergeAll(group)
        }
    }

    private fun <T : Model> mergeAll(modelWithMetadataList: List<ModelWithMetadata<T>>) {
        sqliteStorage.use { adapter ->
            val listToSave = modelWithMetadataList.mapNotNull { it.takeIf { !it.isDeleted }?.model }
            mergeToDbStrategy.onPreSave(listToSave)
            adapter.saveList(listToSave)
            adapter.deleteList(modelWithMetadataList.mapNotNull {
                it.takeIf { it.isDeleted }?.model
            })
        }
    }
}