package io.github.dianila68.gesturemacro.core.actions

import io.github.dianila68.gesturemacro.core.serialization.MacroAction

/**
 * Bridges [ActionCatalog] selections to concrete [MacroAction] instances.
 * Fixed entries (flashlight, media commands) assemble without extra input;
 * template entries ([ActionSpec.requiresPackage]) need a package name.
 *
 * The manual/typed path in [ui.MacroEditor] is unchanged — assembly is an
 * additional route, not a replacement, so imported and hand-typed macros work as before.
 */
object ActionAssembly {
    /**
     * Assembles a fixed (non-template) [ActionSpec] into a [MacroAction].
     * Throws [IllegalArgumentException] if [spec] requires a package (use [assembleWithPackage]).
     */
    fun assemble(spec: ActionSpec): MacroAction {
        require(!spec.requiresPackage) {
            "ActionSpec '${spec.id}' is a template — call assembleWithPackage(spec, packageName)"
        }
        return requireNotNull(spec.buildDirect()) {
            "ActionSpec '${spec.id}' is marked available but has no builder"
        }
    }

    /**
     * Assembles a template [ActionSpec] into a [MacroAction] using [packageName].
     * Throws [IllegalArgumentException] if [packageName] is blank or [spec] is not a template.
     */
    fun assembleWithPackage(spec: ActionSpec, packageName: String): MacroAction {
        require(spec.requiresPackage) {
            "ActionSpec '${spec.id}' is not a template — call assemble(spec)"
        }
        require(packageName.isNotBlank()) {
            "A package name is required to assemble '${spec.id}' (app launch template)"
        }
        return requireNotNull(spec.buildWithPackage(packageName)) {
            "ActionSpec '${spec.id}' is marked available but has no builder"
        }
    }

    /**
     * Convenience: assembles from a stable catalog id.
     * Returns null if the id is unknown or the entry is not available.
     */
    fun assembleById(id: String, packageName: String? = null): MacroAction? {
        val spec = ActionCatalog.forId(id)?.takeIf { it.available } ?: return null
        return if (spec.requiresPackage) {
            packageName?.takeIf { it.isNotBlank() }?.let { assembleWithPackage(spec, it) }
        } else {
            assemble(spec)
        }
    }
}
