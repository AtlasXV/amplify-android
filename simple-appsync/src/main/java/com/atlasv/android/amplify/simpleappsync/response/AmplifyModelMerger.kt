package com.atlasv.android.amplify.simpleappsync.response

import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter
import com.atlasv.android.amplify.simpleappsync.storage.AmplifySqliteStorage
import com.atlasv.android.amplify.simpleappsync.storage.deleteList
import com.atlasv.android.amplify.simpleappsync.storage.saveList

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifyModelMerger(
    private val sqliteStorage: AmplifySqliteStorage
) {

    fun <T : Model> mergeResponse(responseItemGroups: List<List<ModelWithMetadata<T>>>) {
        sqliteStorage.use { adapter ->
            for (group in responseItemGroups) {
                mergeAll(adapter, group)
            }
        }
    }

    private fun <T : Model> mergeAll(adapter: SQLiteStorageAdapter, modelWithMetadataList: List<ModelWithMetadata<T>>) {
        val listToSave = modelWithMetadataList.mapNotNull { it.takeIf { !it.isDeleted }?.model }
        adapter.saveList(listToSave)
        adapter.deleteList(modelWithMetadataList.mapNotNull {
            it.takeIf { it.isDeleted }?.model
        })
    }
}