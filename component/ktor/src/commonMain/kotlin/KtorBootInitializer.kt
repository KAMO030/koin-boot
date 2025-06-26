package io.github.kamo030

import io.github.kamo030.koinboot.component.ktor.KtorAutoConfiguration
import io.github.kamo030.koinboot.core.KoinBootInitializer

val KtorBootInitializer: KoinBootInitializer = {
    autoConfigurations(KtorAutoConfiguration)
}
