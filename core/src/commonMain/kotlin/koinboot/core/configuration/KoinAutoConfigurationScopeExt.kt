package io.github.kamo030.koinboot.core.configuration

import io.github.kamo030.koinboot.core.eqProperty
import io.github.kamo030.koinboot.core.hasInstance
import io.github.kamo030.koinboot.core.hasProperties
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier._q

/**
 * 当指定的限定符实例存在时执行代码块
 *
 * @param qualifiers 需要检查的限定符
 */
fun KoinAutoConfigurationScope.existInstance(vararg qualifiers: Qualifier) = koin.hasInstance(*qualifiers)

inline fun KoinAutoConfigurationScope.onExistInstance(vararg qualifiers: Qualifier, then: () -> Unit) {
    if (existInstance(*qualifiers)) {
        then()
    }
}

inline fun <reified T> KoinAutoConfigurationScope.onExistInstance(then: () -> Unit) {
    if (existInstance(_q<T>())) {
        then()
    }
}

/**
 * 当指定的限定符实例不存在时执行代码块
 *
 * @param qualifiers 需要检查的限定符
 */
fun KoinAutoConfigurationScope.missInstances(vararg qualifiers: Qualifier) = !existInstance(*qualifiers)

inline fun KoinAutoConfigurationScope.onMissInstances(vararg qualifiers: Qualifier, then: () -> Unit) {
    if (missInstances(*qualifiers)) {
        then()
    }
}

inline fun <reified T> KoinAutoConfigurationScope.onMissInstances(then: () -> Unit) {
    if (missInstances(_q<T>())) {
        then()
    }
}


/**
 * 当指定的属性存在时返回true
 *
 * @param keys 需要检查的属性键
 */
fun KoinAutoConfigurationScope.existProperties(vararg keys: String): Boolean = koin.hasProperties(*keys)

inline fun KoinAutoConfigurationScope.onExistProperties(vararg keys: String, then: () -> Unit) {
    if (existProperties(*keys)) {
        then()
    }
}

/**
 * 当指定的属性不存在时返回true
 *
 * @param keys 需要检查的属性键
 */
fun KoinAutoConfigurationScope.missProperties(vararg keys: String): Boolean = !existProperties(*keys)

inline fun KoinAutoConfigurationScope.onMissProperties(vararg keys: String, then: () -> Unit) {
    if (missProperties(*keys)) {
        then()
    }
}


/**
 * 当指定的属性不等于指定值时返回true
 *
 * @param key 需要检查的属性键
 * @param value 需要对比的属性值
 */
fun KoinAutoConfigurationScope.eqProperty(key: String, value: Any): Boolean = koin.eqProperty(key, value)

inline fun KoinAutoConfigurationScope.onEqProperty(key: String, value: Any, then: () -> Unit) {
    if (eqProperty(key, value)) {
        then()
    }
}