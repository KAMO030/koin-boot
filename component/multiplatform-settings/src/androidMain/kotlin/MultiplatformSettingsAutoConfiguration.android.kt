package io.github.kamo030.koinboot.component.multiplatformsettings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.definition.Definition

internal actual fun settingsFactory(): Definition<Settings.Factory> = {
    SharedPreferencesSettings.Factory(get())
}
