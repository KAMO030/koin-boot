package io.github.kamo030.koinboot

import io.github.kamo030.koinboot.core.KoinBootInitializer
import io.github.kamo030.koinboot.component.ktor.KtorAutoConfiguration

val KtorBootInitializer: KoinBootInitializer = {
    autoConfigurations(KtorAutoConfiguration)
}
