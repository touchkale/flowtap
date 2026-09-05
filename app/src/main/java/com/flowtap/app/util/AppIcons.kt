package com.flowtap.app.util

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real app logos for blocked-app UI (PackageManager icons, memory-cached).
 * Loading runs on Dispatchers.IO; callers render a letter avatar until the
 * bitmap arrives (or permanently, for uninstalled packages).
 */
object AppIcons {
    private const val MAX_ENTRIES = 30

    private val cache = object : LruCache<String, ImageBitmap>(MAX_ENTRIES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = 1
    }

    @Synchronized
    fun cached(pkg: String): ImageBitmap? = cache.get(pkg)

    /**
     * Returns the cached icon, loading + caching it on a miss.
     * Null when the package is unknown/uninstalled or has no icon.
     * [sizePx] is the decode size; explicit bounds keep adaptive icons safe.
     */
    suspend fun load(context: Context, pkg: String, sizePx: Int): ImageBitmap? {
        cached(pkg)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(pkg)
                drawable.toBitmap(sizePx.coerceAtLeast(1), sizePx.coerceAtLeast(1)).asImageBitmap()
            }.getOrNull()
        } ?: return null
        synchronized(this) { cache.put(pkg, bitmap) }
        return bitmap
    }

    /** Warms the cache (e.g. for a session's blocked apps at start). */
    suspend fun preload(context: Context, packages: List<String>, sizePx: Int) {
        packages.forEach { load(context, it, sizePx) }
    }
}
