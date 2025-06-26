plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `mpp-lib-targets`
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.multiplatform.settings)
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.multiplatform.settings.test)
    }
}
android {
    namespace = "${getProperty("android.namespace")}.component.multiplatformsettings"
}