package io.github.kamo030.koinboot.component.multiplatformsettings

import com.russhwolf.settings.Settings
import org.koin.core.Koin
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope

fun Koin.getSettings(name: String): Settings {
    return get<Settings> { parametersOf(name) }
}

fun Scope.getSettings(name: String): Settings {
    return get<Settings> { parametersOf(name) }
}