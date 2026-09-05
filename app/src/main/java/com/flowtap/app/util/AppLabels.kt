package com.flowtap.app.util

import android.content.Context

/** Resolves installed-app display labels from package names (for blocked-app UI). */
object AppLabels {
    fun label(context: Context, pkg: String): String? = runCatching {
        val pm = context.packageManager
        val ai = pm.getApplicationInfo(pkg, 0)
        pm.getApplicationLabel(ai).toString()
    }.getOrNull()

    fun labels(context: Context, packages: List<String>): List<String> =
        packages.mapNotNull { label(context, it) }

    /** Package + display label pairs (keeps package identity for icon lookup). */
    fun labeledPackages(context: Context, packages: List<String>): List<Pair<String, String>> =
        packages.mapNotNull { pkg -> label(context, pkg)?.let { pkg to it } }
}
