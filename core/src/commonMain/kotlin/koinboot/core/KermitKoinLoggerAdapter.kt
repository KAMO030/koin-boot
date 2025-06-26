package io.github.kamo030.koinboot.core

import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE
import co.touchlab.kermit.Logger as KermitLogger

class KermitKoinLoggerAdapter(
    level: Level,
    private val kermitLogger: KermitLogger = KermitLogger.withTag("Koin"),
) : Logger(level = level) {

    override fun display(level: Level, msg: MESSAGE) {
        when (level) {
            Level.DEBUG -> kermitLogger.d { msg }
            Level.INFO -> kermitLogger.i { msg }
            Level.WARNING -> kermitLogger.w { msg }
            Level.ERROR -> kermitLogger.e { msg }
            Level.NONE -> {}
        }
    }
}

fun KoinBootContext.kermitLogger() {
    // 这里没办法只能打破生命周期的约束,不然会遇到先有鸡还是先有蛋的问题
    val level = properties.app_logger_level
    application.logger(KermitKoinLoggerAdapter(level))
}
