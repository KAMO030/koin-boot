import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.ExistingDomainObjectDelegate
import org.gradle.kotlin.dsl.RegisteringDomainObjectDelegateProviderWithTypeAndAction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KProperty


@Suppress(
    "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE",
)
@PublishedApi
internal operator fun <U : Task> RegisteringDomainObjectDelegateProviderWithTypeAndAction<out TaskContainer, U>.provideDelegate(
    receiver: Any?,
    property: KProperty<*>,
) = ExistingDomainObjectDelegate.of(
    delegateProvider.register(property.name, type.java, action),
)

@PublishedApi
internal val Project.sourceSets: org.gradle.api.tasks.SourceSetContainer
    get() =
        (this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("sourceSets") as org.gradle.api.tasks.SourceSetContainer

@Suppress(
    "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE",
)
@PublishedApi
internal operator fun <T> ExistingDomainObjectDelegate<out T>.getValue(receiver: Any?, property: KProperty<*>): T =
    delegate

@OptIn(ExperimentalContracts::class)
inline fun <reified T> Any?.cast(): T {
    contract { returns() implies (this@cast is T) }
    return this as T
}

/**
 * Retrieves the [versionCatalogs][org.gradle.api.artifacts.VersionCatalogsExtension] extension.
 */
internal
val org.gradle.api.Project.`versionCatalogs`: org.gradle.api.artifacts.VersionCatalogsExtension
    get() =
        (this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("versionCatalogs") as org.gradle.api.artifacts.VersionCatalogsExtension

/**
 * Configures the [versionCatalogs][org.gradle.api.artifacts.VersionCatalogsExtension] extension.
 */
internal
fun org.gradle.api.Project.`versionCatalogs`(configure: Action<org.gradle.api.artifacts.VersionCatalogsExtension>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("versionCatalogs", configure)

