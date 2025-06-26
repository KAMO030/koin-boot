@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

/*
 * 配置 JVM + Android 的 compose 项目. 默认不会配置 resources.
 *
 * 该插件必须在 kotlin, compose, android 之后引入.
 *
 * 如果开了 android, 就会配置 desktop + android, 否则只配置 jvm.
 */

val android = extensions.findByType(LibraryExtension::class)

configure<KotlinMultiplatformExtension> {

    iosArm64()
    iosSimulatorArm64()
    if (android != null) {
        jvm("desktop")
        androidTarget {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
            unitTestVariant.sourceSetTree.set(KotlinSourceSetTree.unitTest)
        }

        applyDefaultHierarchyTemplate {
            common {
                group("jvm") {
                    withJvm()
                    withAndroidTarget()
                }
                group("skiko") {
                    withJvm()
                    withNative()
                }
            }
        }

    } else {
        jvm()

        applyDefaultHierarchyTemplate()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets.commonMain.dependencies {
        if (project.path != ":core") {
            implementation(project(":core"))
        }
    }
    versionCatalogs.first().findVersion("kotlin").get().let {
        sourceSets.commonTest.dependencies {
            implementation(kotlin("test-annotations-common", it.preferredVersion))
        }
        sourceSets.jvmTest.dependencies {
            implementation(kotlin("test-junit5", it.preferredVersion))
        }
    }
    versionCatalogs.first().findLibrary("kotlinx-coroutines-test").let {
        sourceSets.commonTest.dependencies {
            implementation(it.get())
        }
    }
}

if (android != null){
    configure<LibraryExtension>{
        compileSdk = getProperty("android.compileSdk").toInt()
        defaultConfig {
            minSdk = getProperty("android.minSdk").toInt()
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    }
}
tasks.withType<Test> {
    useJUnitPlatform()
}
