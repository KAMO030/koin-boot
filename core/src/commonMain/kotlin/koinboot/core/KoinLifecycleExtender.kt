package io.github.kamo030.koinboot.core

// 生命周期阶段枚举
enum class KoinPhase {
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

interface KoinLifecycleExtender {
    fun doPhaseChange(phase: KoinPhase, context: KoinBootContext) {
        val call = when (phase) {
            KoinPhase.Starting -> ::doStarting
            KoinPhase.Configuring -> ::doConfiguring
            KoinPhase.PropertiesLoading -> ::doPropertiesLoading
            KoinPhase.ModulesLoading -> ::doModulesLoading
            KoinPhase.Ready -> ::doReady
            KoinPhase.Running -> ::doRunning
            KoinPhase.Stopping -> ::doStopping
            KoinPhase.Stopped -> ::doStopped
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




