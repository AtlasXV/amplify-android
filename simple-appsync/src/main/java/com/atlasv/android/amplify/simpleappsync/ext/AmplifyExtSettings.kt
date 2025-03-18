package com.atlasv.android.amplify.simpleappsync.ext

import android.content.Context
import androidx.core.content.edit

/**
 * weiping@atlasv.com
 * 2022/12/22
 */

class AmplifyExtSettings(private val appContext: Context) {
    private val amplifySettings by lazy {
        appContext.getSharedPreferences("sp_amplify_settings", Context.MODE_PRIVATE)
    }

    fun getExtraModelVersion(): Long {
        return amplifySettings.getLong(KEY_EXTRA_MODEL_VERSION, 0)
    }

    fun saveExtraModelVersion(extraVersion: Long) {
        amplifySettings.edit {
            putLong(KEY_EXTRA_MODEL_VERSION, extraVersion)
        }
    }

    companion object {
        private const val KEY_EXTRA_MODEL_VERSION = "extra_model_version"
    }
}