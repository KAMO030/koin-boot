plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `mpp-lib-targets`
    kotlin("plugin.serialization")
    id("androidx.room")
    id("com.google.devtools.ksp")
}

dependencies {
    commonMainApi(libs.androidx.room.runtime)
    commonMainApi(libs.sqlite.bundled)
    commonMainApi(libs.paging.common)
}

android {
    namespace = "${getProperty("android.namespace")}.component.room"
}

// 以下room配置和 `androidx.room` 与 `com.google.devtools.ksp` 插件都是用于测试演示的
room {
    schemaDirectory("${projectDir}/schemas")
}
dependencies {
    listOf(libs.androidx.room.compiler)
        .forEach {
            add("kspDesktopTest", it)
        }
}

