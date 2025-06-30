plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
    `mpp-lib-targets`
    `koin-boot-initializer`
//    alias(libs.plugins.sentry)
}

val bootDependencies = listOf<Dependency>(
    projects.component.ktor,
    projects.component.kermit,
    projects.component.multiplatformSettings,
)

koinBootInitializer{
    includes(bootDependencies)
}

dependencies{
    bootDependencies.forEach(::commonMainApi)
}


android {
    namespace = "${getProperty("android.namespace")}.sample"
}
