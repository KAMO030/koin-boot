package io.github.kamo030.koinboot.component.ktor

import io.ktor.client.plugins.logging.*
import co.touchlab.kermit.Logger as KermitLogger


class KermitKtorLoggerAdapter(
    private val kermitLogger: KermitLogger = KermitLogger.withTag("Ktor")
) : Logger {

    override fun log(message: String) =
        kermitLogger.i { message }

}