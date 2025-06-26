package io.github.kamo030.koinboot

import io.github.kamo030.koinboot.component.kermit.KermitExtender
import io.github.kamo030.koinboot.core.KoinBootInitializer


val KermitBootInitializer: KoinBootInitializer = {
    extenders(KermitExtender())
}