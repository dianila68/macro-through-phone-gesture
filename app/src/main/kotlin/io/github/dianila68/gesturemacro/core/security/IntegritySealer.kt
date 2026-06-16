package io.github.dianila68.gesturemacro.core.security

/**
 * Threat T5: macros that can drive other apps get an HMAC seal over their stored
 * document so a DB edited outside the app (backup extraction, root) cannot smuggle
 * an enabled accessibility macro back in. Verification failure fails closed.
 *
 * This interface is pure JVM — no android.* imports. The Android Keystore
 * implementation lives in `io.github.dianila68.gesturemacro.android.security`
 * (ticket-023).
 */
interface IntegritySealer {
    fun seal(payload: String): String

    fun verify(payload: String, sealValue: String?): Boolean
}

// ── Backward-compat shims (ticket-023) ───────────────────────────────────────
// Remove these once all call sites are migrated to the android.security package.

@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.security.HmacSealer",
    replaceWith = ReplaceWith(
        "HmacSealer",
        "io.github.dianila68.gesturemacro.android.security.HmacSealer",
    ),
)
typealias HmacSealer = io.github.dianila68.gesturemacro.android.security.HmacSealer

@Deprecated(
    "Moved to io.github.dianila68.gesturemacro.android.security.KeystoreSealerFactory",
    replaceWith = ReplaceWith(
        "KeystoreSealerFactory",
        "io.github.dianila68.gesturemacro.android.security.KeystoreSealerFactory",
    ),
)
typealias KeystoreSealerFactory = io.github.dianila68.gesturemacro.android.security.KeystoreSealerFactory
