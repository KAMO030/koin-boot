plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
    `mpp-lib-targets`
}

dependencies{
    commonMainApi(libs.kermit)
    commonMainApi(libs.koin.core)
    commonMainApi(libs.kotlinx.serialization.json)
    commonTestApi(kotlin("test-annotations-common", libs.versions.kotlin.get()))
    commonTestApi(libs.kotlinx.coroutines.test)
    jvmTestApi(kotlin("test-junit5", libs.versions.kotlin.get()))
}


android {
    namespace = "${getProperty("android.namespace")}.core"
}
