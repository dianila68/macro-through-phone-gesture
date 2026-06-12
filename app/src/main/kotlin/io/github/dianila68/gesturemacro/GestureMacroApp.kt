package io.github.dianila68.gesturemacro

import android.app.Application
import io.github.dianila68.gesturemacro.core.data.MacroStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GestureMacroApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        MacroStore.init(this, appScope)
    }
}
