package io.github.kamo030.koinboot.core

import io.github.kamo030.koinboot.core.configuration.KoinAutoConfiguration
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.ModuleDeclaration

typealias KoinBootInitializer = KoinBootDSL.() -> Unit

class KoinBoot {
    private val context = KoinBootContext()
    private var started = false

    // 构建器方法
    fun withAutoConfigurations(vararg configurations: KoinAutoConfiguration): KoinBoot = apply {
        context.addAutoConfigurations(*configurations)
    }

    fun withLifecycleExtender(vararg extenders: KoinBootLifecycleExtender): KoinBoot = apply {
        context.addLifecycleExtender(*extenders)
    }

    fun withProperties(config: KoinPropDeclaration): KoinBoot = apply {
        context.properties.config()
    }

    fun withModules(vararg modules: Module): KoinBoot = apply {
        context.addModules(*modules)
    }

    private fun KoinApplication.doStart(appDeclaration: KoinAppDeclaration = {}) {
        context.application = this
        // 阶段1: 配置阶段
        context.executePhase(KoinBootPhase.Configuring) {
            appDeclaration()
            context.kermitLogger()
        }
        // 阶段2: 属性加载阶段
        context.executePhase(KoinBootPhase.PropertiesLoading) {
            context.loadProperties()
        }
        // 阶段3: 自动配置阶段
        context.executePhase(KoinBootPhase.ModulesLoading) {
            context.loadModules()
        }
    }

    // 启动方法
    fun run(appDeclaration: KoinAppDeclaration = {}): KoinBootContext {
        // 阶段0: 启动
        context.executePhase(KoinBootPhase.Starting) {
            if (started) throw IllegalStateException("KoinBoot already started")
        }

        runCatching {
            startKoin { doStart(appDeclaration) }
        }.onSuccess {
            // 阶段4: 就绪阶段
            context.executePhase(KoinBootPhase.Ready)

            // 阶段5: 运行阶段
            context.executePhase(KoinBootPhase.Running) {
                started = true
            }
        }.onFailure { exception ->
            stop()
            throw KoinBootException("Failed to start KoinBoot", exception)
        }
        return context
    }

    fun stop() {
        if (!started) return

        context.executePhase(KoinBootPhase.Stopping) {
            context.application.close()
        }

        context.executePhase(KoinBootPhase.Stopped) {
            started = false
        }
    }

}

class KoinBootDSL(
    private val koinBoot: KoinBoot
) {

    @PublishedApi
    internal var appDeclaration: KoinAppDeclaration = {}

    fun appDeclaration(appDeclaration: KoinAppDeclaration) {
        this.appDeclaration = appDeclaration
    }

    fun extenders(vararg extenders: KoinBootLifecycleExtender) {
        koinBoot.withLifecycleExtender(*extenders)
    }

    fun modules(vararg modules: Module) {
        koinBoot.withModules(*modules)
    }

    fun module(moduleDeclaration: ModuleDeclaration) {
        koinBoot.withModules(Module().apply(moduleDeclaration))
    }

    fun autoConfigurations(vararg configurations: KoinAutoConfiguration) {
        koinBoot.withAutoConfigurations(*configurations)
    }

    fun properties(config: KoinPropDeclaration) {
        koinBoot.withProperties(config)
    }

    fun properties(properties: Map<String, Any>) {
        koinBoot.withProperties {
            this.properties.putAll(properties)
        }
    }
}

inline fun runKoinBoot(
    initializer: KoinBootInitializer
): Koin {
    val koinBoot = KoinBoot()
    val koinBootDSL = KoinBootDSL(koinBoot)
    initializer(koinBootDSL)
    return koinBoot.run(koinBootDSL.appDeclaration).koin
}