package io.github.kamo030.koinboot.component.sentry

import co.touchlab.kermit.Logger
import io.github.kamo030.koinboot.core.KoinBootContext
import io.github.kamo030.koinboot.core.KoinBootLifecycleExtender
import io.github.kamo030.koinboot.core.asPropInstance
import io.github.kamo030.koinboot.core.platform.currentPlatform
import io.sentry.kotlin.multiplatform.Sentry

class SentryExtender : KoinBootLifecycleExtender {
    override fun doConfiguring(context: KoinBootContext) = with(context) {
        val sentryProperties = properties.asPropInstance<SentryProperties>() ?: SentryProperties()
        Logger.i("SentryExtender") { "Sentry ${if (sentryProperties.enable) "enable" else "disabled"}" }
        if (!sentryProperties.enable) return@with
        initSentry(sentryProperties)
        Logger.d("SentryExtender") { "Sentry initialized: $sentryProperties" }
        Logger.addLogWriter(SentryLogWriter(sentryProperties.level))
    }

    private fun initSentry(properties: SentryProperties) = with(properties) {
        Sentry.init { options ->
            options.dsn = dsn
            options.environment = environment
            options.release = release
            options.sampleRate = sampleRate
            options.debug = debug
        }
        Sentry.configureScope { scope ->
            val platform = currentPlatform()
            scope.setTag("os", platform.name)
            scope.setTag("arch", platform.arch.name)
        }
    }
}
