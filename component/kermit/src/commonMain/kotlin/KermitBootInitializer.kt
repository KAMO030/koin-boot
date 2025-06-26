package io.github.kamo030

import io.github.kamo030.koinboot.component.kermit.KermitExtender
import io.github.kamo030.koinboot.core.KoinBootInitializer


val KermitBootInitializer: KoinBootInitializer = {
    extenders(KermitExtender())
}