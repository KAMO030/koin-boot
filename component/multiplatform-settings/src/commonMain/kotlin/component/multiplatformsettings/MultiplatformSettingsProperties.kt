package io.github.kamo030.koinboot.component.multiplatformsettings

import io.github.kamo030.koinboot.core.KoinPropInstance
import io.github.kamo030.koinboot.core.KoinProperties

@KoinPropInstance("multiplatform-settings")
data class MultiplatformSettingsProperties(
    val desktop: DeskTop = DeskTop(path = null)
) {
    companion object {
        const val MULTIPLATFORM_SETTINGS_DESKTOP_PATH = "multiplatform-settings.desktop.path"
    }

    @KoinPropInstance("multiplatform-settings.desktop")
    data class DeskTop(
        val path: String? = null
    )
}


@Suppress("unused")
var KoinProperties.multiplatform_settings_desktop_path: String?
    get() = (this[MultiplatformSettingsProperties.Companion.MULTIPLATFORM_SETTINGS_DESKTOP_PATH] as String?) ?: null
    set(value) {
        value?.let { MultiplatformSettingsProperties.Companion.MULTIPLATFORM_SETTINGS_DESKTOP_PATH(it) }
    }
