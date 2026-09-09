package com.flowtap.app.util

import com.flowtap.app.BuildConfig

/** Build identifier for future APK/source comparisons in QA notes. */
object BuildInfo {
    const val GIT_SHA = "6dcc4ee"
    val versionName: String get() = BuildConfig.VERSION_NAME
    const val VERSION_CODE: Int = 1
    fun label(): String = "v${BuildConfig.VERSION_NAME} ($GIT_SHA)"
}
