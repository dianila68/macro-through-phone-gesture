package io.github.dianila68.gesturemacro.core.data

import io.github.dianila68.gesturemacro.core.security.IntegritySealer
import io.github.dianila68.gesturemacro.core.serialization.AccessibilityAction
import io.github.dianila68.gesturemacro.core.serialization.GestureMacro

/**
 * Threat T5 policy applied at the Room boundary. Only macros that can drive other
 * apps (accessibility actions) are sealed; everything else is unsealed and trusted
 * as before. Verification fails closed: a sealed macro whose stored document does
 * not match its seal (or whose seal is missing, e.g. a row inserted out-of-band) is
 * force-disabled on load — it can never silently fire.
 */
class MacroIntegrity(private val sealer: IntegritySealer?) {

    fun sealFor(document: String, macro: GestureMacro): String? =
        if (macro.requiresSeal()) sealer?.seal(document) else null

    /** Returns the macro safe to surface; sealed macros that fail verification are disabled. */
    fun verifyOnLoad(macro: GestureMacro, document: String, seal: String?): GestureMacro {
        if (!macro.requiresSeal()) return macro
        val trusted = sealer?.verify(document, seal) == true
        return if (trusted) macro else macro.copy(enabled = false)
    }

    private fun GestureMacro.requiresSeal(): Boolean = actions.any { it is AccessibilityAction }
}
