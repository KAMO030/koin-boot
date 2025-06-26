package io.github.kamo030.koinboot.component.multiplatformsettings

import com.russhwolf.settings.Settings
import io.github.kamo030.koinboot.core.configuration.koinAutoConfiguration
import io.github.kamo030.koinboot.core.configuration.onMissInstances
import org.koin.core.definition.Definition

internal val MultiplatformSettingsAutoConfiguration = koinAutoConfiguration {
    module {
        // 用户没有配置 Settings.Factory 的情况下，自动使用默认Settings.Factory
        onMissInstances<Settings.Factory> {
            single<Settings.Factory>(definition = settingsFactory())
        }

        // 用户没有配置 Settings 的情况下，自动使用默认Settings
        onMissInstances<Settings> {
            factory<Settings> { (name: String) -> get<Settings.Factory>().create(name) }
        }
    }
}

internal expect fun settingsFactory(): Definition<Settings.Factory>
