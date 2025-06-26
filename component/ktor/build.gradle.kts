plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `mpp-lib-targets`
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.serialization.kotlinx.json)
            api(libs.ktor.client.core)
            api(libs.ktor.client.logging)
            api(libs.ktor.client.auth)
            api(libs.ktor.client.content.negotiation)
//        api(libs.ktor.client.websockets)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            api(libs.ktor.client.okhttp)
        }
        desktopMain.dependencies {
            api(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            api(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "${getProperty("android.namespace")}.component.ktor"
}