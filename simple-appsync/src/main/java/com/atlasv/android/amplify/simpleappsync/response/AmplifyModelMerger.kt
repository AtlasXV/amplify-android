package com.atlasv.android.amplify.simpleappsync.response

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.ModelSchema
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent.Companion.LOG
import com.atlasv.android.amplify.simpleappsync.ext.ensureDisplayName
import com.atlasv.android.amplify.simpleappsync.ext.itemName
import com.atlasv.android.amplify.simpleappsync.ext.materialID
import com.atlasv.android.amplify.simpleappsync.ext.sort
import com.atlasv.android.amplify.simpleappsync.storage.AmplifySqliteStorage
import com.atlasv.android.amplify.simpleappsync.storage.SQLCommandFactoryExt
import com.atlasv.android.amplify.simpleappsync.storage.deleteList
import com.atlasv.android.amplify.simpleappsync.storage.saveList

/**
 * weiping@atlasv.com
 * 2022/12/6
 */
class AmplifyModelMerger(
    private val sqliteStorage: AmplifySqliteStorage, private val modelPreSaveAction: (Model) -> Unit
) {

    fun <T : Model> mergeAll(modelWithMetadataList: List<ModelWithMetadata<T>>) {
        sqliteStorage.use { adapter ->

            adapter.saveList(modelWithMetadataList.mapNotNull {
                it.takeIf { !it.isDeleted }?.model?.apply {
                    ensureDisplayName()
                    modelPreSaveAction(this)
                }
            })

            adapter.deleteList(modelWithMetadataList.mapNotNull {
                it.takeIf { it.isDeleted }?.model
            })
        }
    }

    fun <T : Model> localize(modelClass: Class<T>, localeModel: Model) {
        val modelId = localeModel.materialID ?: return
        val modelSchema = ModelSchema.fromModelClass(modelClass)
        val command = SQLCommandFactoryExt.updateLocaleFor(
            modelSchema = modelSchema,
            modelId = modelId,
            itemDisplayName = localeModel.itemName,
            sort = localeModel.sort
        ) ?: return
        val sqliteTable = SQLiteTable.fromSchema(modelSchema)

        sqliteStorage.use {
            LOG.verbose("Localize item in " + sqliteTable.name + " identified by ID: " + modelId)
            it.sqlCommandProcessor?.execute(command)
        }
    }
}