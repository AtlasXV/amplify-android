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

package com.amplifyframework.api.aws;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.predicate.BeginsWithQueryOperator;
import com.amplifyframework.core.model.query.predicate.BetweenQueryOperator;
import com.amplifyframework.core.model.query.predicate.ContainsQueryOperator;
import com.amplifyframework.core.model.query.predicate.EqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterOrEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.GreaterThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.LessOrEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.LessThanQueryOperator;
import com.amplifyframework.core.model.query.predicate.NotEqualQueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryOperator;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateGroup;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;
import com.amplifyframework.util.FieldFinder;
import com.amplifyframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converts provided model or class type into a request container
 * with automatically generated GraphQL documents that follow
 * AppSync specifications.
 */
final class AppSyncGraphQLRequestFactory {
    private static final int DEFAULT_QUERY_LIMIT = 1000;
    private static final int DEFAULT_LEVEL_DEPTH = 2;

    // This class should not be instantiated
    private AppSyncGraphQLRequestFactory() { }

    public static <T extends Model> GraphQLRequest<T> buildQuery(
            Class<T> modelClass,
            String objectId
    ) {
        StringBuilder doc = new StringBuilder();
        Map<String, Object> variables = new HashMap<>();
        ModelSchema schema = ModelSchema.fromModelClass(modelClass);
        String graphQlTypeName = schema.getName();

        doc.append("query ")
            .append("Get")
            .append(StringUtils.capitalizeFirst(graphQlTypeName))
            .append("(")
            .append("$id: ID!) { get")
            .append(StringUtils.capitalizeFirst(graphQlTypeName))
            .append("(id: $id) { ")
            .append(getModelFields(modelClass, DEFAULT_LEVEL_DEPTH))
            .append("}}");

        variables.put("id", objectId);

        return new GraphQLRequest<>(
                doc.toString(),
                variables,
                modelClass,
                new GsonVariablesSerializer()
        );
    }

    public static <T extends Model> GraphQLRequest<T> buildQuery(
            Class<T> modelClass,
            QueryPredicate predicate
    ) throws AmplifyException {
        StringBuilder doc = new StringBuilder();
        Map<String, Object> variables = new HashMap<>();
        ModelSchema schema = ModelSchema.fromModelClass(modelClass);
        String graphQlTypeName = schema.getName();

        doc.append("query ")
            .append("List")
            .append(StringUtils.capitalizeFirst(graphQlTypeName))
            .append("(")
            .append("$filter: Model")
            .append(graphQlTypeName)
            .append("FilterInput ")
            .append("$limit: Int $nextToken: String) { list")
            .append(StringUtils.capitalizeFirst(graphQlTypeName))
            .append("s(filter: $filter, limit: $limit, nextToken: $nextToken) { items {")
            .append(getModelFields(modelClass, DEFAULT_LEVEL_DEPTH))
            .append("} nextToken }}");

        if (!predicateIsEmpty(predicate)) {
            variables.put("filter", parsePredicate(predicate));
            variables.put("limit", DEFAULT_QUERY_LIMIT);
        }

        return new GraphQLRequest<>(
                doc.toString(),
                variables,
                modelClass,
                new GsonVariablesSerializer()
        );
    }

    @SuppressWarnings("unchecked")
    public static <T extends Model> GraphQLRequest<T> buildMutation(
            T model,
            QueryPredicate predicate,
            MutationType type
    ) throws AmplifyException {
        // model is of type T so this is a safe cast - hence the warning suppression
        Class<T> modelClass = (Class<T>) model.getClass();

        StringBuilder doc = new StringBuilder();
        ModelSchema schema = ModelSchema.fromModelClass(modelClass);
        String typeStr = type.toString();
        String graphQlTypeName = schema.getName();

        doc.append("mutation ")
            .append(StringUtils.capitalize(typeStr))
            .append(StringUtils.capitalizeFirst(graphQlTypeName))
            .append("($input: ")
            .append(StringUtils.capitalize(typeStr))
            .append(StringUtils.capitalizeFirst(graphQlTypeName))
            .append("Input!");

        if (!predicateIsEmpty(predicate)) {
            doc.append(", $condition: Model")
                    .append(graphQlTypeName)
                    .append("ConditionInput");
        }

        doc.append("){ ")
            .append(typeStr.toLowerCase(Locale.getDefault()))
            .append(StringUtils.capitalizeFirst(graphQlTypeName))
            .append("(input: $input");

        if (!predicateIsEmpty(predicate)) {
            doc.append(", condition: $condition");
        }

        doc.append(") { ")
            .append(getModelFields(modelClass, DEFAULT_LEVEL_DEPTH))
            .append("}}");

        Map<String, Object> variables = new HashMap<>();

        if (type.equals(MutationType.DELETE)) {
            variables.put("input", Collections.singletonMap("id", model.getId()));
        } else {
            variables.put("input", schema.getMapOfFieldNameAndValues(model));
        }

        if (!predicateIsEmpty(predicate)) {
            variables.put("condition", parsePredicate(predicate));
        }

        return new GraphQLRequest<>(
                doc.toString(),
                variables,
                modelClass,
                new GsonVariablesSerializer()
        );
    }

    public static <T extends Model> GraphQLRequest<T> buildSubscription(
            Class<T> modelClass,
            SubscriptionType type
    ) {
        StringBuilder doc = new StringBuilder();
        ModelSchema schema = ModelSchema.fromModelClass(modelClass);
        String typeStr = type.toString();
        String graphQlTypeName = schema.getName();

        doc.append("subscription ")
                .append(StringUtils.allCapsToPascalCase(typeStr))
                .append(StringUtils.capitalizeFirst(graphQlTypeName))
                .append("{")
                .append(StringUtils.allCapsToCamelCase(typeStr))
                .append(StringUtils.capitalizeFirst(graphQlTypeName))
                .append("{")
                .append(getModelFields(modelClass, DEFAULT_LEVEL_DEPTH))
                .append("}}");

        return new GraphQLRequest<>(
                doc.toString(),
                modelClass,
                new GsonVariablesSerializer()
        );
    }

    public static <T extends Model> GraphQLRequest<T> buildSubscription(
            Class<T> modelClass,
            QueryPredicate predicate,
            SubscriptionType type
    ) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private static Map<String, Object> parsePredicate(QueryPredicate queryPredicate) throws AmplifyException {
        if (queryPredicate instanceof QueryPredicateOperation) {
            QueryPredicateOperation qpo = (QueryPredicateOperation) queryPredicate;
            QueryOperator op = qpo.operator();
            return Collections.singletonMap(
                    qpo.field(),
                    Collections.singletonMap(appSyncOpType(op.type()), appSyncOpValue(op))
            );
        } else if (queryPredicate instanceof QueryPredicateGroup) {
            QueryPredicateGroup qpg = (QueryPredicateGroup) queryPredicate;

            if (qpg.type().equals(QueryPredicateGroup.Type.NOT)) {
                try {
                    return Collections.singletonMap("not", parsePredicate(qpg.predicates().get(0)));
                } catch (IndexOutOfBoundsException exception) {
                    throw new AmplifyException(
                            "Predicate group of type NOT must include a value to negate.",
                            exception,
                            "Check if you created a NOT condition in your Predicate with no included value.",
                            false
                    );
                }
            } else {
                List<Map<String, Object>> predicates = new ArrayList<>();

                for (QueryPredicate predicate : qpg.predicates()) {
                    predicates.add(parsePredicate(predicate));
                }

                return Collections.singletonMap(qpg.type().toString().toLowerCase(Locale.getDefault()), predicates);
            }
        } else {
            throw new AmplifyException(
                    "Tried to parse an unsupported QueryPredicate",
                    null,
                    "Try changing to one of the supported values: QueryPredicateOperation, QueryPredicateGroup.",
                    false
            );
        }
    }

    private static String appSyncOpType(QueryOperator.Type type) throws AmplifyException {
        switch (type) {
            case NOT_EQUAL:
                return "ne";
            case EQUAL:
                return "eq";
            case LESS_OR_EQUAL:
                return "le";
            case LESS_THAN:
                return "lt";
            case GREATER_OR_EQUAL:
                return "ge";
            case GREATER_THAN:
                return "gt";
            case CONTAINS:
                return "contains";
            case BETWEEN:
                return "between";
            case BEGINS_WITH:
                return "beginsWith";
            default:
                throw new AmplifyException(
                        "Tried to parse an unsupported QueryOperator type",
                        null,
                        "Check if a new QueryOperator.Type enum has been created which is not supported" +
                        "in the AppSyncGraphQLRequestFactory.",
                        false
                );
        }
    }

    private static Object appSyncOpValue(QueryOperator qOp) throws AmplifyException {
        switch (qOp.type()) {
            case NOT_EQUAL:
                return ((NotEqualQueryOperator) qOp).value();
            case EQUAL:
                return ((EqualQueryOperator) qOp).value();
            case LESS_OR_EQUAL:
                return ((LessOrEqualQueryOperator) qOp).value();
            case LESS_THAN:
                return ((LessThanQueryOperator) qOp).value();
            case GREATER_OR_EQUAL:
                return ((GreaterOrEqualQueryOperator) qOp).value();
            case GREATER_THAN:
                return ((GreaterThanQueryOperator) qOp).value();
            case CONTAINS:
                return ((ContainsQueryOperator) qOp).value();
            case BETWEEN:
                BetweenQueryOperator betweenOp = (BetweenQueryOperator) qOp;
                return Arrays.asList(betweenOp.start(), betweenOp.end());
            case BEGINS_WITH:
                return ((BeginsWithQueryOperator) qOp).value();
            default:
                throw new AmplifyException(
                        "Tried to parse an unsupported QueryOperator type",
                        null,
                        "Check if a new QueryOperator.Type enum has been created which is not supported" +
                                "in the AppSyncGraphQLRequestFactory.",
                        false
                );
        }
    }

    @SuppressWarnings("unchecked")
    private static String getModelFields(Class<? extends Model> clazz, int levelsDeepToGo) {
        if (levelsDeepToGo < 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        ModelSchema schema = ModelSchema.fromModelClass(clazz);
        final Set<Field> classFields = FieldFinder.findFieldsIn(clazz);
        Iterator<Field> iterator = classFields.iterator();

        while (iterator.hasNext()) {
            Field field = iterator.next();
            String fieldName = field.getName();

            if (schema.getAssociations().containsKey(fieldName)) {
                if (List.class.isAssignableFrom(field.getType())) {
                    if (levelsDeepToGo >= 1) {
                        result.append(fieldName).append(" ");

                        ParameterizedType listType = (ParameterizedType) field.getGenericType();
                        Class<Model> listTypeClass = (Class<Model>) listType.getActualTypeArguments()[0];

                        result.append("{ items {")
                                .append(getModelFields(listTypeClass, levelsDeepToGo - 1)) // cast checked above
                                .append("} nextToken }");
                    }
                } else if (levelsDeepToGo >= 1) {
                    result.append(fieldName).append(" ");

                    result.append("{")
                            .append(getModelFields((Class<Model>) field.getType(), levelsDeepToGo - 1))
                            .append("}");
                }
            } else {
                result.append(fieldName).append(" ");
            }
        }

        return result.toString();
    }

    // While this is currently a simple null check, in the future it could possibly be more complex
    private static boolean predicateIsEmpty(QueryPredicate predicate) {
        return predicate == null;
    }
}