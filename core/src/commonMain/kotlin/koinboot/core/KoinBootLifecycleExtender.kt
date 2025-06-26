package io.github.kamo030.koinboot.core

// 生命周期阶段枚举
enum class KoinBootPhase {
    Starting,
    Configuring,
    ModulesLoading,
    PropertiesLoading,
    Ready,
    Running,
    Stopping,
    Stopped
}

/**
 * 生命周期扩展器
 *
 *
 */

interface KoinBootLifecycleExtender {
    fun doPhaseChange(phase: KoinBootPhase, context: KoinBootContext) {
        val call = when (phase) {
            KoinBootPhase.Starting -> ::doStarting
            KoinBootPhase.Configuring -> ::doConfiguring
            KoinBootPhase.PropertiesLoading -> ::doPropertiesLoading
            KoinBootPhase.ModulesLoading -> ::doModulesLoading
            KoinBootPhase.Ready -> ::doReady
            KoinBootPhase.Running -> ::doRunning
            KoinBootPhase.Stopping -> ::doStopping
            KoinBootPhase.Stopped -> ::doStopped
        }
        call(context)
    }

    fun doStarting(context: KoinBootContext) {}
    fun doConfiguring(context: KoinBootContext) {}
    fun doPropertiesLoading(context: KoinBootContext) {}
    fun doModulesLoading(context: KoinBootContext) {}
    fun doReady(context: KoinBootContext) {}
    fun doRunning(context: KoinBootContext) {}
    fun doStopping(context: KoinBootContext) {}
    fun doStopped(context: KoinBootContext) {}
}




