package com.atlasv.android.amplify.simpleappsync.response.merge

import com.amplifyframework.core.model.Model

/**
 * 网络请求的数据项合并到数据库
 * weiping@atlasv.com
 * 2023/3/21
 */
open class ItemMergeToDbStrategy {
    fun <T : Model> onPreSave(items: List<T>) {

    }
}