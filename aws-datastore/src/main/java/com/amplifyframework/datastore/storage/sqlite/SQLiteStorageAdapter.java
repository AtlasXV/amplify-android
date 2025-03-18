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

package com.amplifyframework.datastore.storage.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.model.CompoundModelProvider;
import com.amplifyframework.datastore.model.SystemModelsProviderFactory;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.StorageResult;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.datastore.storage.sqlite.migrations.AmplifyDbVersionCheckListener;
import com.amplifyframework.datastore.storage.sqlite.migrations.ModelMigrations;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.Immutable;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

/**
 * An implementation of {@link LocalStorageAdapter} using {@link android.database.sqlite.SQLiteDatabase}.
 */
public final class SQLiteStorageAdapter implements LocalStorageAdapter {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");
    private static final long THREAD_POOL_TERMINATE_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Thread pool size is determined as number of processors multiplied by this value.  We want to allow more threads
    // than available processors to parallelize primarily IO bound work, but still provide a limit to avoid out of
    // memory errors.
    private static final int THREAD_POOL_SIZE_MULTIPLIER = 20;

    @SuppressWarnings("checkstyle:all") // Keep logger first
    public static final String DEFAULT_DATABASE_NAME = "AmplifyDatastore.db";

    private final String databaseName;

    // Provider of the Models that will be warehouse-able by the DataStore
    // and models that are used internally for DataStore to track metadata
    private final ModelProvider modelsProvider;

    // SchemaRegistry instance that gives the ModelSchema, CustomTypeSchema (Flutter) and Model objects
    // based on Model class name lookup mechanism.
    private final SchemaRegistry schemaRegistry;

    // ThreadPool for SQLite operations.
    private ExecutorService threadPool;

    // Data is read from SQLite and de-serialized using GSON
    // into a strongly typed Java object.
    private final Gson gson;

    // Used to publish events to the observables subscribed.
    private final Subject<StorageItemChange<? extends Model>> itemChangeSubject;

    // Represents a connection to the SQLite database. This database reference
    // can be used to do all SQL operations against the underlying database
    // that this handle represents.
    private SQLiteDatabase databaseConnectionHandle;

    // The helper object controls the lifecycle of database creation, update
    // and opening connection to database.
    private SQLiteStorageHelper sqliteStorageHelper;

    // Responsible for executing all commands on the SQLiteDatabase.
    public SQLCommandProcessor sqlCommandProcessor;

    // Factory that produces SQL commands.
    public SQLCommandFactory sqlCommandFactory;

    public SQLCommandFactoryFactory sqlCommandFactoryFactory = new SQLCommandFactoryFactory();

    public CursorValueStringFactory cursorValueStringFactory = new CursorValueStringFactory();

    @Nullable
    public AmplifyDbVersionCheckListener dbVersionCheckListener = null;

    // The helper object to iterate through associated models of a given model.
    private SQLiteModelTree sqliteModelTree;

    // Stores the reference to disposable objects for cleanup
    private final CompositeDisposable toBeDisposed;

    public SqlQueryProcessor sqlQueryProcessor;

    /**
     * Construct the SQLiteStorageAdapter object.
     * @param schemaRegistry A registry of schema for all models and custom types used by the system
     * @param userModelsProvider Provides the models that will be usable by the DataStore
     * @param systemModelsProvider Provides the models that are used by the DataStore system internally
     */
    private SQLiteStorageAdapter(
            SchemaRegistry schemaRegistry,
            ModelProvider userModelsProvider,
            ModelProvider systemModelsProvider) {
        this(schemaRegistry, userModelsProvider, systemModelsProvider, DEFAULT_DATABASE_NAME);
    }

    private SQLiteStorageAdapter(
        SchemaRegistry schemaRegistry,
        ModelProvider userModelsProvider,
        ModelProvider systemModelsProvider,
        String databaseName) {
        this.schemaRegistry = schemaRegistry;
        this.modelsProvider = CompoundModelProvider.of(systemModelsProvider, userModelsProvider);
        this.gson = GsonFactory.instance();
        this.itemChangeSubject = PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        this.toBeDisposed = new CompositeDisposable();
        this.databaseName = databaseName;
    }

    /**
     * Gets a SQLiteStorageAdapter that can be initialized to use the provided models.
     * @param schemaRegistry Registry of schema for all models and custom types in the system
     * @param userModelsProvider A provider of models that will be represented in SQL
     * @return A SQLiteStorageAdapter that will host the provided models in SQL tables
     */
    @NonNull
    public static SQLiteStorageAdapter forModels(
            @NonNull SchemaRegistry schemaRegistry,
            @NonNull ModelProvider userModelsProvider) {
        return new SQLiteStorageAdapter(
            schemaRegistry,
            Objects.requireNonNull(userModelsProvider),
            SystemModelsProviderFactory.create()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void initialize(
            @NonNull Context context,
            @NonNull Consumer<List<ModelSchema>> onSuccess,
            @NonNull Consumer<DataStoreException> onError) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);
        // Create a thread pool large enough to take advantage of parallelization, but small enough to avoid
        // OutOfMemoryError and CursorWindowAllocationException issues.
        this.threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * THREAD_POOL_SIZE_MULTIPLIER);
        threadPool.submit(() -> {
            try {
                if (dbVersionCheckListener != null) {
                    dbVersionCheckListener.onSqliteInitializeStarted();
                }
                /*
                 * Start with a fresh registry.
                 */
                schemaRegistry.clear();
                /*
                 * Create {@link ModelSchema} objects for the corresponding {@link Model}.
                 * Any exception raised during this when inspecting the Model classes
                 * through reflection will be notified via the `onError` callback.
                 */
                schemaRegistry.register(modelsProvider.modelSchemas(), modelsProvider.customTypeSchemas());

                /*
                 * Create the CREATE TABLE and CREATE INDEX commands for each of the
                 * Models. Instantiate {@link SQLiteStorageHelper} to execute those
                 * create commands.
                 */
                this.sqlCommandFactory = sqlCommandFactoryFactory.create(schemaRegistry, gson);
                CreateSqlCommands createSqlCommands = getCreateCommands(modelsProvider.modelNames());
                sqliteStorageHelper = SQLiteStorageHelper.getInstance(
                        context,
                        databaseName,
                        DATABASE_VERSION,
                        createSqlCommands);

                /*
                 * Create and/or open a database. This also invokes
                 * {@link SQLiteStorageHelper#onCreate(SQLiteDatabase)} which executes the tasks
                 * to create tables and indexes. When the function returns without any exception
                 * being thrown, invoke the `onError` callback.
                 *
                 * Errors are thrown when there is no write permission to the database, no space
                 * left in the database for any write operation and other errors thrown while
                 * creating and opening a database. All errors are passed through the
                 * `onError` callback.
                 *
                 * databaseConnectionHandle represents a connection handle to the database.
                 * All database operations will happen through this handle.
                 */
                databaseConnectionHandle = sqliteStorageHelper.getWritableDatabase();

                /*
                 * Create helper instance that can traverse through model relations.
                 */
                this.sqliteModelTree = new SQLiteModelTree(
                    schemaRegistry,
                    databaseConnectionHandle
                );

                /*
                 * Create a command processor which runs the actual SQL transactions.
                 */
                this.sqlCommandProcessor = new SQLCommandProcessor(databaseConnectionHandle);

                sqlQueryProcessor = new SqlQueryProcessor(sqlCommandProcessor,
                        sqlCommandFactory,
                        schemaRegistry);
                sqlQueryProcessor.cursorValueStringFactory = cursorValueStringFactory;

                /*
                 * Detect if the version of the models stored in SQLite is different
                 * from the version passed in through {@link ModelProvider#version()}.
                 * Delete the database if there is a version change.
                 */
                toBeDisposed.add(updateModels().subscribe(
                    () -> onSuccess.accept(
                        Immutable.of(new ArrayList<>(schemaRegistry.getModelSchemaMap().values()))
                    ),
                    throwable -> onError.accept(new DataStoreException(
                        "Error in initializing the SQLiteStorageAdapter",
                        throwable, AmplifyException.TODO_RECOVERY_SUGGESTION
                    ))
                ));
            } catch (Exception exception) {
                onError.accept(new DataStoreException(
                    "Error in initializing the SQLiteStorageAdapter",
                    exception, "See attached exception"
                ));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void save(
            @NonNull T item,
            @NonNull StorageItemChange.Initiator initiator,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<StorageItemChange<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(initiator);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);
        threadPool.submit(() -> {
            StorageResult<T> result = saveInternal(item, initiator, predicate);
            if (result instanceof StorageResult.Success) {
                StorageResult.Success<T> success = (StorageResult.Success<T>) result;
                onSuccess.accept(success.getStorageItemChange());
            } else if (result instanceof StorageResult.Failure) {
                StorageResult.Failure<T> failure = (StorageResult.Failure<T>) result;
                onError.accept(failure.getException());
            }
        });
    }

    private <T extends Model> StorageResult<T> saveInternal(
            @NonNull T item,
            @NonNull StorageItemChange.Initiator initiator,
            @NonNull QueryPredicate predicate) {
        try {
            final ModelSchema modelSchema = schemaRegistry.getModelSchemaForModelClass(item.getModelName());

            final StorageItemChange.Type writeType;
            SerializedModel patchItem = null;

            if (sqlQueryProcessor.modelExists(item, QueryPredicates.all())) {
                // if data exists already, then UPDATE the row
                writeType = StorageItemChange.Type.UPDATE;

                // Check if existing data meets the condition, only if a condition other than all() was provided.
                if (!QueryPredicates.all().equals(predicate) && !sqlQueryProcessor.modelExists(item, predicate)) {
                    throw new DataStoreException(
                            "Save failed because condition did not match existing model instance.",
                            "The save will continue to fail until the model instance is updated."
                    );
                }
                if (initiator == StorageItemChange.Initiator.DATA_STORE_API) {
                    // When saving items via the DataStore API, compute a SerializedModel of the changed model.
                    // This is not necessary when save
                    // is initiated by the sync engine, so skip it for optimization to avoid the extra SQL query.
                    patchItem = SerializedModel.create(item, modelSchema);
                }
            } else if (!QueryPredicates.all().equals(predicate)) {
                // insert not permitted with a condition
                throw new DataStoreException(
                        "Conditional update must be performed against an already existing data. " +
                                "Insertion is not permitted while using a predicate.",
                        "Please save without specifying a predicate."
                );
            } else {
                // if data doesn't exist yet, then INSERT a new row
                writeType = StorageItemChange.Type.CREATE;
            }

            // execute local save
            writeData(item, writeType);

            // publish successful save
            StorageItemChange<T> change = StorageItemChange.<T>builder()
                    .item(item)
                    .patchItem(patchItem != null ? patchItem : SerializedModel.create(item, modelSchema))
                    .modelSchema(modelSchema)
                    .type(writeType)
                    .predicate(predicate)
                    .initiator(initiator)
                    .build();
            itemChangeSubject.onNext(change);
            return new StorageResult.Success<>(change);
        } catch (DataStoreException dataStoreException) {
            return new StorageResult.Failure<>(dataStoreException);
        } catch (Exception someOtherTypeOfException) {
            String modelToString = item.getModelName() + "[primaryKey =" + item.getPrimaryKeyString() + "]";
            DataStoreException dataStoreException = new DataStoreException(
                    "Error in saving the model: " + modelToString,
                    someOtherTypeOfException, "See attached exception for details."
            );
            return new StorageResult.Failure<>(dataStoreException);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull QueryOptions options,
            @NonNull Consumer<Iterator<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError) {
        Objects.requireNonNull(itemClass);
        Objects.requireNonNull(options);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);
        threadPool.submit(() -> {
            List<T> models = sqlQueryProcessor.queryOfflineData(itemClass, options, onError);
            onSuccess.accept(models.iterator());
        });
    }

    private CreateSqlCommands getCreateCommands(@NonNull Set<String> modelNames) {
        final Set<SqlCommand> createTableCommands = new HashSet<>();
        final Set<SqlCommand> createIndexCommands = new HashSet<>();
        for (String modelName : modelNames) {
            final ModelSchema modelSchema =
                schemaRegistry.getModelSchemaForModelClass(modelName);
            createTableCommands.add(sqlCommandFactory.createTableFor(modelSchema));
            createIndexCommands.addAll(sqlCommandFactory.createIndexesFor(modelSchema));
            createIndexCommands.addAll(sqlCommandFactory.createIndexesForForeignKeys(modelSchema));
        }
        return new CreateSqlCommands(createTableCommands, createIndexCommands);
    }

    public  <T extends Model> void writeData(
            T item,
            StorageItemChange.Type writeType
    ) throws DataStoreException {
        final String modelName = item.getModelName();
        final ModelSchema modelSchema = schemaRegistry.getModelSchemaForModelClass(modelName);
        final SQLiteTable sqliteTable = SQLiteTable.fromSchema(modelSchema);

        // Generate SQL command for given action
        switch (writeType) {
            case CREATE:
                LOG.verbose("Creating item in " + sqliteTable.getName() + " identified by ID: " + item
                        .getPrimaryKeyString());
                sqlCommandProcessor.execute(sqlCommandFactory.insertFor(modelSchema, item));
                break;
            case UPDATE:
                LOG.verbose("Updating item in " + sqliteTable.getName() + " identified by ID: " + item
                        .getPrimaryKeyString());
                sqlCommandProcessor.execute(sqlCommandFactory.updateFor(modelSchema, item));
                break;
            case DELETE:
                LOG.verbose("Deleting item in " + sqliteTable.getName() + " identified by ID: " +
                        item.getPrimaryKeyString());
                final SQLiteColumn primaryKey = sqliteTable.getPrimaryKey();
                if (primaryKey != null) {
                    final String primaryKeyName = sqliteTable.getPrimaryKey().getName();
                    final QueryPredicate matchId = QueryField.field(modelName, primaryKeyName)
                            .eq(item.getPrimaryKeyString());
                    sqlCommandProcessor.execute(sqlCommandFactory.deleteFor(modelSchema, matchId));
                }
                break;
            default:
                throw new DataStoreException(
                    "Unexpected change was requested: " + writeType.name(),
                    "Valid storage changes are CREATE, UPDATE, and DELETE."
                );
        }
    }

    /*
     * Detect if the version of the models stored in SQLite is different
     * from the version passed in through {@link ModelProvider#version()}.
     * Drop all tables if the version has changed.
     */
    private Completable updateModels() {
        return PersistentModelVersion.fromLocalStorage(this).flatMap(iterator -> {
            if (iterator.hasNext()) {
                PersistentModelVersion persistentModelVersion = iterator.next();
                String oldVersion = persistentModelVersion.getVersion();
                String newVersion = modelsProvider.version();
                LOG.info("Successfully read model version from local storage: " +
                        "oldVersion=" + oldVersion + ", newVersion=" + newVersion + ". " +
                        "Checking if the model version need to be updated...");
                if (!ObjectsCompat.equals(oldVersion, newVersion)) {
                    LOG.info("Updating version as it has changed from " + oldVersion + " to " + newVersion);
                    Objects.requireNonNull(sqliteStorageHelper);
                    Objects.requireNonNull(databaseConnectionHandle);
                    sqliteStorageHelper.update(databaseConnectionHandle, oldVersion, newVersion);
                } else {
                    LOG.debug("Database up to date. Checking ModelMetadata.");
                    new ModelMigrations(databaseConnectionHandle, modelsProvider).apply();
                }
            }
            PersistentModelVersion persistentModelVersion = new PersistentModelVersion(modelsProvider.version());
            return PersistentModelVersion.saveToLocalStorage(this, persistentModelVersion);
        }).ignoreElement();
    }

    @Nullable
    public SQLiteDatabase getSQLiteDatabase() {
        return databaseConnectionHandle;
    }
}
