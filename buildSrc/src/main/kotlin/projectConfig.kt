import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile


fun Project.configureKotlinSourceSets(
    platformName: String,
    outputDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>
) {
    // Try to find Kotlin Multiplatform extension
    val kotlinExtension = extensions.findByType<KotlinMultiplatformExtension>()
    if (kotlinExtension != null) {
        val sourceSetName = "${platformName}Main"
        try {
            val sourceSet = kotlinExtension.sourceSets.getByName(sourceSetName)
            sourceSet.kotlin.srcDirs(outputDir)
            extensions.findByType<IdeaModel>()?.module?.generatedSourceDirs?.add(outputDir.get().asFile)
            logger.info("Added BuildConfig output directory to $sourceSetName source set")
        } catch (e: Exception) {
            logger.debug("Could not find source set $sourceSetName: ${e.message}")
        }
    } else {
        logger.debug("Kotlin Multiplatform extension not found")
    }
}

fun Project.configurePlatformTaskDependencies(platformName: String, generateTaskName: String) {
    // Configure dependencies using task name patterns and types
    tasks.configureEach {
        when {
            // Desktop compilation tasks
            platformName.lowercase() == "desktop" && (
                    name == "compileKotlinDesktop" ||
                            name.contains("compileKotlinDesktop")
                    ) -> {
                dependsOn(generateTaskName)
            }

            // Android compilation tasks
            platformName.lowercase() == "android" && (
                    name.contains("compileKotlinAndroid") ||
                            name.contains("KotlinAndroid")
                    ) -> {
                dependsOn(generateTaskName)
            }

            // iOS compilation tasks
            platformName.lowercase() == "ios" && (
                    name.contains("compileKotlinIos") ||
                            name.contains("KotlinIos")
                    ) -> {
                dependsOn(generateTaskName)
            }

            // Generic platform compilation tasks
            name.contains("compileKotlin${platformName.replaceFirstChar { it.uppercase() }}") -> {
                dependsOn(generateTaskName)
            }
        }
    }

    // Also configure KotlinCompile tasks specifically for iOS
    if (platformName.lowercase() == "ios") {
        tasks.matching {
            // iosSimulatorArm64MetadataElements
            it.name.contains("ios", ignoreCase = true) && it.name.contains("MetadataElements", ignoreCase = true)
        }.configureEach {
            dependsOn(generateTaskName)
        }
        tasks.withType(KotlinNativeCompile::class.java) {
            if (this.target.contains("ios", ignoreCase = true)) {
                dependsOn(generateTaskName)
            }
        }
    }
}