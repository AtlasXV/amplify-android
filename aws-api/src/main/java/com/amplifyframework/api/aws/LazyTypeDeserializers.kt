/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.api.aws

import com.amplifyframework.core.model.Model
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

const val ITEMS_KEY = "items"
const val NEXT_TOKEN_KEY = "nextToken"

@Throws(JsonParseException::class)
private fun getJsonObject(json: JsonElement): JsonObject {
    return json as? JsonObject ?: throw JsonParseException(
        "Got a JSON value that was not an object " +
            "Unable to deserialize the response"
    )
}

@Throws(JsonParseException::class)
private fun <M : Model> deserializeItems(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
): List<M> {
    val pType = typeOfT as? ParameterizedType
        ?: throw JsonParseException("Expected a parameterized type during list deserialization.")
    val type = pType.actualTypeArguments[0]

    val jsonObject = getJsonObject(json)

    val itemsJsonArray = if (jsonObject.has(ITEMS_KEY) && jsonObject.get(ITEMS_KEY).isJsonArray) {
        jsonObject.getAsJsonArray(ITEMS_KEY)
    } else {
        throw JsonParseException(
            "Got JSON from an API call which was supposed to go with a List " +
                "but is in the form of an object rather than an array. " +
                "It also is not in the standard format of having an items " +
                "property with the actual array of data so we do not know how " +
                "to deserialize it."
        )
    }

    return itemsJsonArray.map {
        context.deserialize(it.asJsonObject, type)
    }
}
