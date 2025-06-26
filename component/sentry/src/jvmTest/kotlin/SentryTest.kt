package io.github.kamo030.koinboot.component.sentry.test

import co.touchlab.kermit.Logger
import io.github.kamo030.koinboot.SentryBootInitializer
import io.github.kamo030.koinboot.core.runKoinBoot
import io.github.kamo030.koinboot.component.sentry.*
import io.github.kamo030.koinboot.core.platform.MyukoBuildConfig
import org.junit.jupiter.api.Test

class SentryTest {

    @Test
    fun `test sentry`() {
        runKoinBoot {
            SentryBootInitializer()
            properties {
                sentry_enable = true
                sentry_dsn = MyukoBuildConfig.sentryDsn
                sentry_environment = "test"
                sentry_debug = MyukoBuildConfig.isDebug
                sentry_release = "1.0.0"
            }
        }
        Logger.i("SentryTest") { "SentryTest INFO" }
        Logger.w("SentryTest") { "SentryTest WARNING" }
        Logger.e("SentryTest", RuntimeException("SentryTest ERROR")) { "SentryTest ERROR" }

    }

}