package io.github.kamo030.koinboot

import io.github.kamo030.koinboot.core.KoinBootInitializer
import io.github.kamo030.koinboot.component.multiplatformsettings.MultiplatformSettingsAutoConfiguration


val MultiplatformSettingsBootInitializer: KoinBootInitializer = {
    autoConfigurations(MultiplatformSettingsAutoConfiguration)
}


