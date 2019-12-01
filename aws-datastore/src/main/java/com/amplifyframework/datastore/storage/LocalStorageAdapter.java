/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.datastore.storage;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;

/**
 * A LocalStorageAdapter provides a simple set of interactions to
 * save, delete, query, and observe changes to object models. An instance of an
 * object model is called an "item" in the storage.
 *
 * An implementation of a LocalStorageAdapter is intended to provide a durable
 * local repository implementation, where item does not leave the device on which
 * this software is running (item is "local" to the localhost).
 *
 * Plausible implementations of the LocalStorageAdapter might use SQLite, SharedPreferences,
 * Room, Realm, Flat-file, in-memory, etc., etc.
 */
public interface LocalStorageAdapter {

    /**
     * Initialize the storage engine s.t. it will be able to host models
     * of the provided types. A {@link ModelSchema} will be generated for each
     * {@link Model} provided by the {@link ModelProvider}.
     *
     * This method must be called before any other method on the LocalStorageAdapter
     * may be used. Only models that have been provided at initialization time are "in-play"
     * for use with the LocalStorageAdapter. It is a user error to try to save/query/delete
     * any model type that has not been initialized by this call.
     *
     * @param context An Android Context
     * @param listener A listener to be invoked upon completion of the initialization
     */
    void initialize(
            @NonNull Context context,
            @NonNull ResultListener<List<ModelSchema>> listener);

    /**
     * Save an item into local storage. The {@link ResultListener} will be invoked when the
     * save operation is completed, to notify the caller of success or failure.
     * @param item the item to save into the repository
     * @param initiator An identification of the actor who initiated this save
     * @param itemSaveListener A listener that will be invoked when the save terminates.
     * @param <T> The type of the item being stored
     */
    <T extends Model> void save(
            @NonNull T item,
            @NonNull StorageItemChange.Initiator initiator,
            @NonNull ResultListener<StorageItemChange.Record> itemSaveListener);

    /**
     * Query the storage for items of a given type.
     * @param itemClass Items that have this class will be solicited
     * @param queryResultsListener A listener that will be notified when the query terminates
     * @param <T> Type type of the items that are being queried
     */
    <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull ResultListener<Iterator<T>> queryResultsListener);

    /**
     * Query the storage for items of a given type with specific conditions.
     * @param itemClass Items that have this class will be solicited
     * @param predicate Predicate condition to apply to query
     * @param queryResultsListener A listener that will be notified when the query terminates
     * @param <T> Type type of the items that are being queried
     */
    <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @Nullable QueryPredicate predicate,
            @NonNull ResultListener<Iterator<T>> queryResultsListener);

    /**
     * Deletes an item from storage.
     * @param item Item to delete
     * @param initiator An identification of the actor who initiated this deletion
     * @param itemDeletionListener Listener that will be callback-ed when deletion terminates
     * @param <T> The type of item being deleted
     */
    <T extends Model> void delete(
            @NonNull T item,
            @NonNull StorageItemChange.Initiator initiator,
            @NonNull ResultListener<StorageItemChange.Record> itemDeletionListener);

    /**
     * Observe all changes to that occur to any/all objects in the storage.
     * @return An observable which emits an {@link StorageItemChange} notification every time
     *         any object managed by the storage adapter is changed in any way.
     */
    Observable<StorageItemChange.Record> observe();

    /**
     * Terminate use of the local storage.
     * This should release all resources used by the implementation.
     */
    void terminate();
}