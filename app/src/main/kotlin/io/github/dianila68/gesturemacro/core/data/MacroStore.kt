package io.github.dianila68.gesturemacro.core.data

import android.content.Context
import io.github.dianila68.gesturemacro.core.actions.ExecResult
import io.github.dianila68.gesturemacro.core.security.KeystoreSealerFactory
import io.github.dianila68.gesturemacro.core.serialization.GestureMacro
import io.github.dianila68.gesturemacro.core.serialization.MacroCodec
import io.github.dianila68.gesturemacro.core.serialization.PatternKind
import io.github.dianila68.gesturemacro.core.serialization.SensorKind
import io.github.dianila68.gesturemacro.core.serialization.SystemToggleAction
import io.github.dianila68.gesturemacro.core.serialization.Trigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Room-backed macro store (ticket-007). Initialized once from the Application;
 * exposes a hot snapshot so the engine reads macros without touching the DB
 * on the gesture path.
 */
object MacroStore {
    private val state = MutableStateFlow<List<GestureMacro>>(emptyList())
    private var dao: MacroDao? = null
    private var scope: CoroutineScope? = null
    private var integrity = MacroIntegrity(null)

    val macros: StateFlow<List<GestureMacro>> = state

    fun init(context: Context, appScope: CoroutineScope) {
        if (dao != null) return
        val database = MacroDatabase.build(context)
        dao = database.macroDao()
        scope = appScope
        integrity = MacroIntegrity(KeystoreSealerFactory.create())
        appScope.launch {
            seedIfEmpty()
            database.macroDao().observeAll().collect { entities ->
                state.value = entities.mapNotNull { entity ->
                    val macro = MacroCodec.decodeFromStorage(entity.document)
                        ?.copy(enabled = entity.enabled)
                        ?: return@mapNotNull null
                    integrity.verifyOnLoad(macro, entity.document, entity.integritySeal)
                }
            }
        }
    }

    fun upsert(macro: GestureMacro) {
        launchOnDao { it.upsert(toEntity(macro)) }
    }

    fun remove(id: String) {
        launchOnDao { it.delete(id) }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        launchOnDao { it.setEnabled(id, enabled) }
    }

    fun recordExecution(macro: GestureMacro, results: List<ExecResult>) {
        val failures = results.filterIsInstance<ExecResult.Failure>()
        launchOnDao {
            it.insertLog(
                ExecutionLogEntity(
                    macroId = macro.id,
                    macroName = macro.name,
                    timestamp = System.currentTimeMillis(),
                    success = failures.isEmpty(),
                    detail = if (failures.isEmpty()) {
                        "${results.size} action(s) ok"
                    } else {
                        failures.joinToString("; ") { f -> f.reason }
                    },
                ),
            )
        }
    }

    fun observeRecentLogs() = dao?.observeRecentLogs()

    private fun launchOnDao(block: suspend (MacroDao) -> Unit) {
        val d = dao ?: return
        scope?.launch { block(d) }
    }

    private fun toEntity(macro: GestureMacro): MacroEntity {
        val document = MacroCodec.encodeForStorage(macro)
        return MacroEntity(
            id = macro.id,
            name = macro.name,
            enabled = macro.enabled,
            document = document,
            integritySeal = integrity.sealFor(document, macro),
        )
    }

    private suspend fun seedIfEmpty() {
        val d = dao ?: return
        if (d.count() == 0) {
            d.upsert(toEntity(builtInShakeFlashlight()))
        }
    }

    private fun builtInShakeFlashlight() = GestureMacro(
        version = 1,
        id = "builtin-shake-flashlight",
        name = "Shake to toggle flashlight",
        enabled = true,
        trigger = Trigger(sensor = SensorKind.ACCELEROMETER, pattern = PatternKind.SHAKE),
        actions = listOf(SystemToggleAction(target = "flashlight")),
    )
}
