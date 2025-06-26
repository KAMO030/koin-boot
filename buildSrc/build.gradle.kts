plugins {
    // The Kotlin DSL plugin provides a convenient way to develop convention plugins.
    // Convention plugins are located in `src/main/kotlin`, with the file extension `.gradle.kts`,
    // and are applied in the project's `build.gradle.kts` files as required.
    `kotlin-dsl`
}

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

dependencies {
    api(libs.kotlin.gradle.plugin)
    api(libs.android.gradle.plugin)
    api(libs.android.application.gradle.plugin)
    api(libs.android.library.gradle.plugin)
    implementation("com.android.tools.build:gradle:8.0.0")

    api(libs.compose.multiplatfrom.gradle.plugin)
    api(libs.kotlin.compose.compiler.gradle.plugin)
}