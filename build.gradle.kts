plugins {
    kotlin("multiplatform") apply false
    kotlin("android") apply false
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
    id("com.android.library") apply false
    id("com.android.application") apply false
    id("androidx.room") version libs.versions.room apply false
    id("com.google.devtools.ksp") version libs.versions.ksp apply false
    alias(libs.plugins.sentry) apply false
}

allprojects {
    group = properties["group"].toString()
    version = properties["version"].toString()

    repositories {
        maven("https://jitpack.io")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        maven("https://maven.aliyun.com/repository/public")
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://androidx.dev/storage/compose-compiler/repository/")
        maven("https://jogamp.org/deployment/maven")
    }
}


