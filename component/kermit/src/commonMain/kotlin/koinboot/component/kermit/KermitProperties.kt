package io.github.kamo030.koinboot.component.kermit

import co.touchlab.kermit.Severity
import io.github.kamo030.koinboot.core.KoinPropInstance
import io.github.kamo030.koinboot.core.KoinProperties


@KoinPropInstance("kermit")
data class KermitProperties(
    val severity: Severity = Severity.Debug,
) {
    companion object {
        const val KERMIT_SEVERITY = "kermit.severity"
    }
}

var KoinProperties.kermit_severity: Severity
    get() = (this[KermitProperties.KERMIT_SEVERITY] as? Severity) ?: Severity.Debug
    set(value) {
        KermitProperties.KERMIT_SEVERITY(value)
    }
