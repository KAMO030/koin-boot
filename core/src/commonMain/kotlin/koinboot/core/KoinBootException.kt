package io.github.kamo030.koinboot.core

class KoinBootException(
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)