package io.github.kamo030.koinboot.core

import io.github.kamo030.koinboot.core.serialization.convertMapToJsonElement
import io.github.kamo030.koinboot.core.serialization.convertToFinalJsonElement
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


/**
 * 用于唯一标识所管理的属性 Map 的限定符。
 */
@PublishedApi
internal val ManagedKoinProperties = named("managed_koin_properties")

typealias KoinPropDeclaration = KoinProperties.() -> Unit

/**
 * 提供一种 DSL，用于以分层方式定义和访问 Koin 属性。
 *
 * 这个类允许你使用类似构建器的语法来定义属性，支持嵌套块，
 * 这些嵌套块会自动转换为点分隔的属性键 (例如, `database.url`)。
 *
 * 它还充当了两个属性来源的统一视图：
 * 1. 在其 DSL 块内本地定义的属性。
 * 2. Koin 容器中已存在的全局属性。
 *
 * 当查找一个属性时，它会首先检查本地定义的属性，如果未找到，
 * 则会委托给 Koin 的 `getProperty` 方法。
 *
 * 用法示例：
 * ```
 * koinApplication.properties {
 *     "server" {
 *         "host"("localhost")
 *         "port"(8080)
 *     }
 * }
 * ```
 * 上述代码会定义 "server.host" 和 "server.port" 两个属性。
 */
class KoinProperties(
    val properties: MutableMap<String, Any> = mutableMapOf(),
    private val koin: Koin? = null
) : Map<String, Any> by properties {


    override fun get(key: String): Any? =
        properties[key] ?: koin?.getProperty(key)

    private var currentKey: String = ""

    operator fun String.invoke(value: Any) {
        properties[mergeCurrentKey()] = value
    }

    operator fun String.invoke(block: KoinProperties.() -> Unit) {
        val preKey = currentKey
        currentKey = mergeCurrentKey()
        runCatching { block() }
        currentKey = preKey
    }

    inline operator fun <reified T : Any> T.unaryPlus() {
        properties.putAll(toFlatPropertyMap())
    }

    private fun String.mergeCurrentKey(): String {
        return if (currentKey.isNotEmpty()) {
            "$currentKey.$this"
        } else {
            this
        }
    }

}

fun Koin.declareProperties(properties: KoinProperties) =
    declare(properties, ManagedKoinProperties)



/**
 * 从 Koin 容器中检索属性，并将其反序列化为一个类型为 [T] 的对象实例。
 *
 * 这个函数通过一个前缀键 [preKey] 来识别相关的属性集。如果 [preKey] 未被直接提供，
 * 它会尝试从类型 [T] 的 `@KoinPropInstance` 注解中自动推断。
 *
 * 它首先获取在 Koin 中注册的 `KoinProperties` 单例，然后调用 `asPropInstance`
 * 来执行转换。
 *
 * @param T 要创建的属性实例的类型。
 * @param preKey 用于过滤和分组相关属性的前缀。如果为 null，则会从 [T] 的注解中推断。
 * @return 一个 [T] 的实例，其字段值从 Koin 属性中填充。
 * @throws KoinBootException 如果属性转换失败，例如找不到前缀或反序列化出错。
 */
inline fun <reified T> Koin.getPropInstance(preKey: String? = null): T? {
    return runCatching {
        val koinProperties = getOrNull<KoinProperties>(ManagedKoinProperties)
        koinProperties?.asPropInstance<T>(preKey)
    }.recover {
        throw KoinBootException(
            "属性实例对象转换失败：preKey: $preKey, Type: ${T::class}",
            it
        )
    }.getOrThrow()
}

inline fun <reified T> Koin.getPropInstance(preKey: String? = null, default: () -> T): T =
    getPropInstance(preKey) ?: default()

/**
 * 将属性 Map 的子集转换为一个类型为 [T] 的数据对象实例。
 *
 * 此函数通过以下步骤工作：
 * 1. 筛选出所有键以指定 [preKey] 开头的属性。
 * 2. 从这些键中移除 [preKey] 前缀（以及任何前导的点），以匹配 [T] 的字段名。
 * 3. 将清理后的 Map 转换为 JSON 元素。
 * 4. 使用 `kotlinx.serialization` 将 JSON 元素反序列化为 [T] 类型的对象。
 *
 * @param T 目标对象的类型，必须是可序列化的。
 * @param preKey 用于标识相关属性的前缀。
 * @return 一个填充了属性值的 [T] 的新实例。
 */
inline fun <reified T> Map<String, Any>.asPropInstance(preKey: String? = null): T? {
    val preKey = preKey ?: serializer<T>().descriptor.annotations.filterIsInstance<KoinPropInstance>().first().preKey
    val instanceProperties =
        if (preKey.isEmpty()) {
            this
        } else {
            this.filter { (key, _) -> key.startsWith(preKey) }
                .mapKeys { (key, _) -> key.removePrefix(preKey).trimStart { it -> it == '.' } }
        }

    if (instanceProperties.isEmpty()) {
        return null
    }

    // 只有一个值并且这个值的key被删除了说明这个值就是最终的值的key
    // see test koin one property
    val jsonElement = if (instanceProperties.size == 1 && instanceProperties.keys.first() == "") {
        convertToFinalJsonElement(instanceProperties.values.first())
    } else {
        convertMapToJsonElement(instanceProperties)
    }
    return Json.decodeFromJsonElement(jsonElement)
}



/**
 * 基于 kotlinx.serialization 将实例转换为扁平化的属性 Map
 */
inline fun <reified T> T.toFlatPropertyMap(): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    val serializer = serializer<T>()
    val json = Json { encodeDefaults = true }

    // 将对象序列化为 JsonElement
    val jsonElement = json.encodeToJsonElement(serializer, this)

    processJsonElement(jsonElement, serializer.descriptor, result)
    return result
}

// 递归处理 JsonElement，生成扁平化的属性 Map
fun processJsonElement(
    element: JsonElement,
    descriptor: SerialDescriptor,
    result: MutableMap<String, Any>,
    currentPath: String = ""
) {
    when (element) {
        is JsonObject -> {
            // 从描述符获取注解信息
            val koinPropAnnotation = descriptor.annotations
                .filterIsInstance<KoinPropInstance>()
                .firstOrNull()

            val prefix = koinPropAnnotation?.preKey ?: ""

            // 确定当前对象的路径前缀
            val objectPath = when {
                currentPath.isEmpty() && prefix.isNotEmpty() -> prefix
                currentPath.isNotEmpty() && prefix.isNotEmpty() -> prefix
                currentPath.isNotEmpty() && prefix.isEmpty() -> currentPath
                else -> ""
            }

            // 遍历对象的所有字段
            element.forEach { (key, value) ->
                val elementIndex = descriptor.elementNames.indexOf(key)
                if (elementIndex >= 0) {
                    val elementDescriptor = descriptor.elementDescriptors.elementAt(elementIndex)
                    val propertyPath = if (objectPath.isNotEmpty()) {
                        "$objectPath.$key"
                    } else {
                        key
                    }

                    processJsonElement(value, elementDescriptor, result, propertyPath)
                }
            }
        }

        is JsonArray -> {
            // 处理数组类型（如果需要的话）
            element.forEachIndexed { index, item ->
                val arrayPath = "$currentPath[$index]"
                processJsonElement(item, descriptor, result, arrayPath)
            }
        }

        is JsonPrimitive -> {
            // 基本类型直接存储
            when {
                element.contentOrNull != null -> result[currentPath] = element.content
                element.booleanOrNull != null -> result[currentPath] = element.boolean
                element.intOrNull != null -> result[currentPath] = element.int
                element.longOrNull != null -> result[currentPath] = element.long
                element.floatOrNull != null -> result[currentPath] = element.float
                element.doubleOrNull != null -> result[currentPath] = element.double
                else -> result[currentPath] = element.content
            }
        }
    }
}

fun <T : Any> KoinComponent.property(key: String): ReadOnlyProperty<Any?, T> =
    object : ReadOnlyProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = getKoin().getProperty<T>(key)
            ?: error("property $key not found")
    }
