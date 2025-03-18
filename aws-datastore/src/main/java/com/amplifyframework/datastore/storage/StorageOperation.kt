package com.amplifyframework.datastore.storage

import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.DataStoreException

internal sealed class StorageResult<T : Model> {
    class Success<T : Model>(val storageItemChange: StorageItemChange<T>) : StorageResult<T>()
    class Failure<T : Model>(val exception: DataStoreException) : StorageResult<T>()
}
