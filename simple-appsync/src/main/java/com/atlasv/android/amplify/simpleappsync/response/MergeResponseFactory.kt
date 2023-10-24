package com.atlasv.android.amplify.simpleappsync.response

import com.amplifyframework.api.aws.GsonGraphQLResponseFactory
import com.amplifyframework.api.graphql.GraphQLRequest
import com.amplifyframework.api.graphql.GraphQLResponse
import com.atlasv.android.amplify.simpleappsync.AmplifySimpleSyncComponent.Companion.LOG
import com.atlasv.android.amplify.simpleappsync.request.MergeListRequest
import org.json.JSONObject

/**
 * weiping@atlasv.com
 * 2022/12/5
 */
class MergeResponseFactory : GraphQLResponse.Factory {
    private val gqlResponseFactory by lazy {
        GsonGraphQLResponseFactory()
    }

    override fun <R : Any?> buildResponse(
        request: GraphQLRequest<R>?,
        apiResponseJson: String?,
    ): GraphQLResponse<R> {
        return buildResponse(request, apiResponseJson, null)
    }

    override fun <R : Any?> buildResponse(
        request: GraphQLRequest<R>?,
        apiResponseJson: String?,
        apiName: String?
    ): GraphQLResponse<R> {
        try {
            if (request == null) {
                return GraphQLResponse(emptyList<Any>() as R, emptyList())
            }

            if (request !is MergeListRequest) {
                return gqlResponseFactory.buildResponse(request, apiResponseJson, apiName)
            }

            apiResponseJson ?: error("Empty apiResponseJson")
            val resObj = JSONObject(apiResponseJson)
            val dataObj = resObj.optJSONObject("data")
                ?: error("No data in apiResponseJson, message=${getDataErrorMessage(resObj)}")
            val keys = dataObj.keys()

            val result = arrayListOf<Any>()
            for (k in keys) {
                val modelObj = dataObj.opt(k)
                val singleObj = JSONObject()
                singleObj.put("data", JSONObject().apply {
                    put(k, modelObj)
                })
                val childRequest = request.children.find { k == "list${it.queryModelName}" } ?: continue
                result.add(gqlResponseFactory.buildResponse(childRequest, singleObj.toString(), apiName))
            }
            checkErrors(resObj)
            LOG.info("Build merge response finish")
            return GraphQLResponse(result as R, emptyList())
        } catch (cause: Throwable) {
            LOG.error("buildResponse failed", cause)
            return GraphQLResponse(emptyList<Any>() as R, emptyList())
        }
    }

    private fun checkErrors(resObj: JSONObject) {
        kotlin.runCatching {
            val errorArray = resObj.optJSONArray("errors")?.takeIf { it.length() > 0 } ?: return
            for (i in 0 until errorArray.length()) {
                val item = errorArray.optJSONObject(i) ?: continue
                val errorType = item.optString("errorType")
                val message = item.optString("message")
                LOG.error("Build merge response has error: [$errorType]: $message")
            }
        }
    }

    private fun getDataErrorMessage(resObj: JSONObject): String {
        val errors = resObj.optJSONArray("errors")?.takeIf { it.length() > 0 } ?: return ""
        val messageBuilder = StringBuilder()
        for (i in 0 until errors.length()) {
            messageBuilder.append(errors.optJSONObject(i).optString("message")).append("\n")
        }
        return messageBuilder.toString()
    }
}