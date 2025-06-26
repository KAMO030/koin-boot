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
}


android {
    namespace = "${getProperty("android.namespace")}.core"
}
