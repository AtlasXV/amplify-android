package com.atlasv.android.amplify.simpleappsync.response.merge

import com.atlasv.android.amplify.simpleappsync.storage.AmplifySqliteStorage


/**
 * 网络请求的数据项合并到数据库
 * weiping@atlasv.com
 * 2023/3/21
 */
interface ItemMergeStrategy {
    fun onAfterSaved(sqliteStorage: AmplifySqliteStorage)
}