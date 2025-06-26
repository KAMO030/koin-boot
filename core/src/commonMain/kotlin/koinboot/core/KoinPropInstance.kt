package io.github.kamo030.koinboot.core

import kotlinx.serialization.MetaSerializable

@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class KoinPropInstance(val preKey: String = "")
