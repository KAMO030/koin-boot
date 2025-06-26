package io.github.kamo030.koinboot.component.multiplatformsettings

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import io.github.kamo030.koinboot.component.multiplatformsettings.MultiplatformSettingsProperties.Companion.MULTIPLATFORM_SETTINGS_DESKTOP_PATH
import org.koin.core.definition.Definition
import java.util.prefs.Preferences

internal actual fun settingsFactory(): Definition<Settings.Factory> = {
    val path = getKoin().getProperty<String>(MULTIPLATFORM_SETTINGS_DESKTOP_PATH)
    PreferencesSettings.Factory(
        path?.let {
            Preferences.systemRoot().node(path)
        } ?: Preferences.userRoot()
    )
}