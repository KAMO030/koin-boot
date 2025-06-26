plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `mpp-lib-targets`
    alias(libs.plugins.sentry)
    kotlin("plugin.serialization")
}

android {
    namespace = "${getProperty("android.namespace")}.component.sentry"
}

sentryKmp {
    enabled = false
    linker {
        frameworkPath.set("path/to/Sentry.xcframework")
    }
}
