package io.github.kamo030.koinboot.component.multiplatformsettings

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.core.definition.Definition


internal actual fun settingsFactory(): Definition<Settings.Factory> = {
    NSUserDefaultsSettings.Factory()
}