package io.github.kamo030.koinboot.component.kermit.test

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.github.kamo030.koinboot.KermitBootInitializer
import io.github.kamo030.koinboot.component.kermit.kermit_severity
import io.github.kamo030.koinboot.core.runKoinBoot
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KermitTest {
    @Test
    fun `test kermit`() {
        runKoinBoot {
            KermitBootInitializer()
            properties {
                kermit_severity = Severity.Warn
            }
        }
        Logger.i { "这个日志不会打印" }
        Logger.w { "这个日志会打印" }
        assertEquals(Severity.Warn, Logger.config.minSeverity)
    }
}