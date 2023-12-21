package com.atlasv.android.amplify.simpleappsync.config

import java.util.concurrent.TimeUnit

/**
 * ### 参数说明
 * * [buildInDbUpdatedAt]内置数据库的更新时间
 * * [extraVersion] 支持不更改modelVersion也能重新使用内置数据库
 * * [grayRelease] 灰度值，取值0<=grayRelease<=16
 * * [dataExpireInterval] 如果数据超过30天，全量更新
 *
 * Created by weiping on 2023/12/21
 */
class AmplifySimpleSyncConfig(
    val buildInDbUpdatedAt: Long,
    val extraVersion: Long,
    val grayRelease: Int,
    val dataExpireInterval: Long = TimeUnit.DAYS.toMillis(30)
)