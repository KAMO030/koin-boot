package io.github.kamo030.koinboot

import io.github.kamo030.koinboot.component.room.RoomAutoConfiguration
import io.github.kamo030.koinboot.core.KoinBootInitializer

val RoomBootInitializer: KoinBootInitializer = {
    autoConfigurations(RoomAutoConfiguration)
}


