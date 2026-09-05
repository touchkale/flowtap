package com.flowtap.app.util

/**
 * Curated packages users most often want blocked during a focus session:
 * social networks and messaging first, then video/streaming, then shopping,
 * dating and other classic distractors.
 *
 * Entries are suggestions only — callers intersect with the actually installed
 * packages, so unknown or region-specific IDs are harmless. Order is the
 * display order (most universally distracting first).
 */
object RecommendedApps {
    private val SOCIAL = listOf(
        "com.whatsapp",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.facebook.katana",
        "com.facebook.orca",
        "com.twitter.android",
        "com.snapchat.android",
        "com.reddit.frontpage",
        "org.telegram.messenger",
        "com.discord",
        "com.pinterest",
        "com.linkedin.android",
    )

    private val VIDEO = listOf(
        "com.google.android.youtube",
        "in.startv.hotstar",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "com.jio.media.ondemand",
        "com.wbd.stream",
        "com.hbo.hbomax",
    )

    private val MORE = listOf(
        "com.amazon.mShop.android.shopping",
        "com.flipkart.android",
        "com.bumble.app",
        "com.tinder",
    )

    val ALL: List<String> = SOCIAL + VIDEO + MORE

    /** Curation order restricted to [installedPkgs]. */
    fun installed(installedPkgs: Set<String>): List<String> =
        ALL.filter { it in installedPkgs }.distinct()
}
