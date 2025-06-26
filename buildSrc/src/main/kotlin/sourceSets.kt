import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectList
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget


inline fun <reified T> Any?.safeAs(): T? {
    return this as? T
}

val Project.kotlinSourceSets get() = extensions.findByName("kotlin").safeAs<KotlinProjectExtension>()?.sourceSets

fun Project.allKotlinTargets(): NamedDomainObjectCollection<KotlinTarget> {
    return extensions.findByName("kotlin")?.safeAs<KotlinSingleTargetExtension<*>>()
        ?.target?.let { namedDomainObjectListOf(it) }
        ?: extensions.findByName("kotlin")?.safeAs<KotlinMultiplatformExtension>()?.targets
        ?: namedDomainObjectListOf()
}

private inline fun <reified T> Project.namedDomainObjectListOf(vararg values: T): NamedDomainObjectList<T> {
    return objects.namedDomainObjectList(T::class.java).apply { addAll(values) }
}

val Project.isKotlinJvmProject: Boolean get() = extensions.findByName("kotlin") is KotlinJvmProjectExtension
val Project.isKotlinMpp: Boolean get() = extensions.findByName("kotlin") is KotlinMultiplatformExtension
