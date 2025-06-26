plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `mpp-lib-targets`
    kotlin("plugin.serialization")
}

android {
    namespace = "${getProperty("android.namespace")}.component.kermit"
}