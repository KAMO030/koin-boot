package io.github.kamo030.koinboot.component.kermit

import co.touchlab.kermit.Logger
import io.github.kamo030.koinboot.core.KoinBootContext
import io.github.kamo030.koinboot.core.KoinBootLifecycleExtender

class KermitExtender : KoinBootLifecycleExtender {
    override fun doConfiguring(context: KoinBootContext) = with(context) {
        val severity = properties.kermit_severity
        Logger.setMinSeverity(severity)
        logger.i(tag = "KermitExtender") { "setMinSeverity :$severity" }
    }
}