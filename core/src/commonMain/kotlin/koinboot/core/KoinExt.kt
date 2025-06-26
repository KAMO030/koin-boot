@file:OptIn(KoinInternalApi::class)

package io.github.kamo030.koinboot.core


import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.definition.Kind
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.TypeQualifier


fun Koin.hasInstance(vararg qualifiers: Qualifier): Boolean {
    if (qualifiers.isEmpty()) return true
    return instanceRegistry.instances.values
        .any { factory ->
            val beanDefinition = factory.beanDefinition
            if (beanDefinition.kind == Kind.Scoped) return@any false
            qualifiers.all { qualifier ->
                if (qualifier is TypeQualifier) {
                    return@all qualifier.type == beanDefinition.primaryType || beanDefinition.secondaryTypes.contains(
                        qualifier.type
                    )
                }
                return beanDefinition.qualifier?.value == qualifier.value
            }
        }
}

fun Koin.hasProperties(vararg keys: String): Boolean = keys.all { getProperty<Any>(it) != null }

fun Koin.eqProperty(key: String, value: Any): Boolean = getProperty<Any>(key)?.toString() == value.toString()
