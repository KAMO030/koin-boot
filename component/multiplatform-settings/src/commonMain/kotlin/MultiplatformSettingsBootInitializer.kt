package io.github.kamo030

import io.github.kamo030.koinboot.component.multiplatformsettings.MultiplatformSettingsAutoConfiguration
import io.github.kamo030.koinboot.core.KoinBootInitializer


val MultiplatformSettingsBootInitializer: KoinBootInitializer = {
    autoConfigurations(MultiplatformSettingsAutoConfiguration)
}


