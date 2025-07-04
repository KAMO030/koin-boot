package io.github.kamo030.koinboot.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable

@OptIn(ExperimentalSerializationApi::class)
@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class KoinPropInstance(val preKey: String = "")
