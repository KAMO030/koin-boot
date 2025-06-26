package io.github.kamo030.koinboot.component.sentry

import io.github.kamo030.koinboot.core.KoinPropInstance
import io.github.kamo030.koinboot.core.KoinProperties
import io.sentry.kotlin.multiplatform.SentryLevel


@KoinPropInstance("sentry")
data class SentryProperties(
    val enable: Boolean = true,
    val dsn: String = "",
    val environment: String = "development",
    val release: String = "Unknown",
    val sampleRate: Double = 1.0,
    val debug: Boolean = false,
    val level: SentryLevel = SentryLevel.WARNING
) {
    companion object {
        const val SENTRY_ENABLE = "sentry.enable"
        const val SENTRY_DSN = "sentry.dsn"
        const val SENTRY_ENVIRONMENT = "sentry.environment"
        const val SENTRY_RELEASE = "sentry.release"
        const val SENTRY_SAMPLE_RATE = "sentry.sampleRate"
        const val SENTRY_DEBUG = "sentry.debug"
        const val SENTRY_VERSION = "sentry.version"
        const val SENTRY_LEVEL = "sentry.level"
    }
}

var KoinProperties.sentry_enable: Boolean
    get() = (this[SentryProperties.SENTRY_ENABLE] as Boolean?) ?: true
    set(value) {
        SentryProperties.SENTRY_ENABLE(value)
    }

var KoinProperties.sentry_dsn: String
    get() = (this[SentryProperties.SENTRY_DSN] as String?) ?: ""
    set(value) {
        SentryProperties.SENTRY_DSN(value)
    }

var KoinProperties.sentry_environment: String
    get() = (this[SentryProperties.SENTRY_ENVIRONMENT] as String?) ?: "development"
    set(value) {
        SentryProperties.SENTRY_ENVIRONMENT(value)
    }

var KoinProperties.sentry_release: String
    get() = (this[SentryProperties.SENTRY_RELEASE] as String?) ?: ""
    set(value) {
        SentryProperties.SENTRY_RELEASE(value)
    }

var KoinProperties.sentry_sample_rate: Double
    get() = (this[SentryProperties.SENTRY_SAMPLE_RATE] as Double?) ?: 1.0
    set(value) {
        SentryProperties.SENTRY_SAMPLE_RATE(value)
    }

var KoinProperties.sentry_debug: Boolean
    get() = (this[SentryProperties.SENTRY_DEBUG] as Boolean?) ?: false
    set(value) {
        SentryProperties.SENTRY_DEBUG(value)
    }

var KoinProperties.level: SentryLevel
    get() = (this[SentryProperties.SENTRY_LEVEL] as SentryLevel?) ?: SentryLevel.WARNING
    set(value) {
        SentryProperties.SENTRY_LEVEL(value)
    }
