package io.github.kamo030.koinboot.core.configuration

import io.github.kamo030.koinboot.core.getPropInstance
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.ModuleDeclaration

class KoinAutoConfigurationScope(
    private val module: Module,
    val koin: Koin
) {
    inline fun <reified T> propertyInstance(preKey: String? = null) = koin.getPropInstance<T>(preKey)

    fun module(moduleDeclaration: ModuleDeclaration) = module.moduleDeclaration()
}