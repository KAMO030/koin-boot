package io.github.kamo030.koinboot.component.sentry

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel

/**
 * Sentry日志写入器
 */
class SentryLogWriter(level: SentryLevel) : LogWriter() {

    private val minSeverity = when (level) {
        SentryLevel.DEBUG -> Severity.Debug
        SentryLevel.INFO -> Severity.Info
        SentryLevel.WARNING -> Severity.Warn
        SentryLevel.ERROR -> Severity.Error
        SentryLevel.FATAL -> Severity.Assert
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        if (severity < minSeverity || !Sentry.isEnabled()) return
        sendToSentry(severity, message, tag, throwable)
    }

    private fun sendToSentry(severity: Severity, message: String, tag: String, throwable: Throwable?) {

        val sentryLevel = when (severity) {
            Severity.Verbose, Severity.Debug -> SentryLevel.DEBUG
            Severity.Info -> SentryLevel.INFO
            Severity.Warn -> SentryLevel.WARNING
            Severity.Error -> SentryLevel.ERROR
            Severity.Assert -> SentryLevel.FATAL
        }

        if (throwable != null) {
            Sentry.captureException(throwable) { scope ->
                scope.level = sentryLevel
                scope.setTag("tag", tag)
                scope.setExtra("message", message)
            }
        } else {
            Sentry.captureMessage(message) { scope ->
                scope.level = sentryLevel
                scope.setTag("tag", tag)
            }
        }
    }
}
