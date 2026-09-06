package com.flowtap.app.util

/**
 * UPI + banking apps that refuse to run while an accessibility service with
 * blocking capabilities is enabled. Used for the payment-conflict nudge:
 * when any are installed while FULL_POWER blocking is on, suggest
 * compatibility mode (UsageStats polling, no accessibility dependency).
 */
object PaymentApps {
    private val UPI = listOf(
        "com.google.android.apps.nbu.paisa.user", // GPay
        "com.phonepe.app", // PhonePe
        "net.one97.paytm", // Paytm
        "in.org.npci.upiapp", // BHIM
        "com.whatsapp", // WhatsApp Pay (also in RecommendedApps, harmless)
    )

    private val BANKING = listOf(
        "com.snapwork.hdfc", // HDFC
        "com.sbi.lotusintouch", // YONO SBI
        "com.icicibank.mobicule", // iMobile
        "com.axis.mobile", // Axis
        "com.kotak811.mobile", // Kotak
    )

    val ALL: List<String> = (UPI + BANKING).distinct()

    /** Curation order restricted to [installedPkgs]. */
    fun installed(installedPkgs: Set<String>): List<String> =
        ALL.filter { it in installedPkgs }.distinct()
}
