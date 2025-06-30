package io.github.kamo030.koinboot.core

import co.touchlab.kermit.Logger
import io.github.kamo030.koinboot.core.configuration.KoinAutoConfiguration
import io.github.kamo030.koinboot.core.configuration.KoinAutoConfigurationScope
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.module.Module

class KoinBootContext {

    private var _phase: KoinPhase = KoinPhase.Starting

    private val extenders = mutableListOf<KoinLifecycleExtender>()

    /**
     * [io.github.kamo030.koinboot.core.KoinBoot.run] 方法中锁定住的扩展器集合保证数量和顺序不变
     */
    private val immutableExtenders by lazy { extenders.toList() }

    private val autoConfigurations = mutableListOf<KoinAutoConfiguration>()
    private val modules = mutableListOf<Module>()

    val configModules = Module()

    val properties = KoinProperties()

    lateinit var application: KoinApplication

    val phase: KoinPhase get() = _phase

    val koin: Koin get() = application.koin

    val logger = Logger.withTag("KoinBoot")

    // 生命周期管理
    fun addLifecycleExtender(vararg extenders: KoinLifecycleExtender) {
        this.extenders.addAll(extenders)
    }

    fun removeLifecycleExtender(vararg extenders: KoinLifecycleExtender) {
        this.extenders.removeAll(extenders)
    }

    private fun changePhase(newPhase: KoinPhase) {
        _phase = newPhase
        immutableExtenders.forEach { extender ->
            runCatching { extender.doPhaseChange(newPhase, this) }
                .onFailure { e -> logger.e(e) { "Error in phase $newPhase in extender $extender" } }
                .getOrThrow()
        }
    }

    // 自动配置管理
    fun addAutoConfigurations(vararg configurations: KoinAutoConfiguration) {
        autoConfigurations.addAll(configurations)
    }

    internal fun loadModules() {
        // 加载所有用户模块
        application.modules(modules)
        // 执行自动配置
        val configModules = configModules
        val scope = KoinAutoConfigurationScope(configModules, koin)
        autoConfigurations
            .sortedBy { it.order }
            .filter { config -> with(config) { scope.match() } }
            .forEach { config -> with(config) { scope.configure() } }
        // 加载所有自动配置模块
        application.modules(configModules)
    }


    internal fun loadProperties() {
        application.properties(properties)
        koin.declareProperties(properties)
    }

    // 模块管理
    fun addModules(vararg modules: Module) {
        this.modules.addAll(modules)
    }

    internal fun executePhase(phase: KoinPhase, action: () -> Unit = {}) {
        changePhase(phase)
        runCatching(action)
            .onFailure { e -> logger.e(e) { "Error in phase $phase" } }
            .getOrThrow()
    }

}