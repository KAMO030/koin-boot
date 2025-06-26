package io.github.kamo030.koinboot.core

import org.koin.core.logger.Level


@KoinPropInstance("app")
data class AppProperties(
    val logger: Logger = Logger(),
) {
    companion object {
        const val APP_LOGGER_LEVEL = "app.logger.level"
    }

    @KoinPropInstance("app.logger")
    data class Logger(
        val level: Level = Level.DEBUG,
    )
}


var KoinProperties.app_logger_level: Level
    get() = (this[AppProperties.APP_LOGGER_LEVEL] as Level?) ?: Level.DEBUG
    set(value) {
        AppProperties.APP_LOGGER_LEVEL(value)
    }
