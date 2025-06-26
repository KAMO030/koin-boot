package io.github.kamo030.koinboot.core

import kotlinx.serialization.json.*

/**
 * 将扁平的 Map<String, Any> 转换为嵌套的 JsonElement。
 * Map 中的键使用点（.）作为分隔符来表示嵌套结构。
 *
 * 例如：
 * 输入: mapOf("xx.enable" to true, "yy.enable" to false)
 * 输出: 一个 JsonObject，其内容为 {"xx":{"enable":true},"yy":{"enable":false}}
 *
 * @param flatMap 输入的扁平 Map。
 * @return 转换后的 JsonElement。
 * @throws IllegalArgumentException 如果存在键冲突（例如，"a.b" 和 "a" 同时作为键存在）。
 */
fun convertMapToJsonElement(flatMap: Map<String, Any>): JsonElement {
    // 使用 MutableMap 来构建层级结构，因为它易于修改。
    val root = mutableMapOf<String, Any?>()

    for ((key, value) in flatMap) {
        val keyParts = key.split('.')
        var currentNode: MutableMap<String, Any?> = root

        // 遍历除了最后一个部分之外的所有键部分，以构建或导航到正确的层级。
        for (i in 0 until keyParts.size - 1) {
            val part = keyParts[i]
            // 如果路径上的节点不存在，则创建一个新的 Map。
            val nextNode = currentNode.getOrPut(part) { mutableMapOf<String, Any?>() }

            if (nextNode is MutableMap<*, *>) {
                // 向下导航到下一层级。
                @Suppress("UNCHECKED_CAST")
                currentNode = nextNode as MutableMap<String, Any?>
            } else {
                // 如果路径上的某个部分已经是一个值而不是一个对象，则存在冲突。
                throw IllegalArgumentException("Key conflict: Part '$part' for key '$key' is already a value and cannot be an object.")
            }
        }

        val lastPart = keyParts.last()
        if (currentNode.containsKey(lastPart)) {
            // 如果最后一个部分已经存在，说明它之前被创建为一个对象（Map），
            // 因此不能被一个值覆盖。
            throw IllegalArgumentException("Key conflict: Part '$lastPart' for key '$key' is already an object and cannot be a value.")
        }
        // 在正确的层级设置最终的值。
        currentNode[lastPart] = value
    }

    // 将构建好的可变层级 Map 递归转换为不可变的 JsonElement。
    return convertToFinalJsonElement(root)
}

/**
 * 一个辅助函数，递归地将包含基本类型、数组和 Map  的层级结构转换为 JsonElement。
 */
fun convertToFinalJsonElement(item: Any?): JsonElement {
    return when (item) {
        null -> JsonNull
        is Map<*, *> -> {
            val mapContent = item.entries.associate { (key, value) ->
                key.toString() to convertToFinalJsonElement(value)
            }
            JsonObject(mapContent)
        }

        is Iterable<*> -> {
            val listContent = item.map { convertToFinalJsonElement(it) }
            JsonArray(listContent)
        }

        is Array<*> -> {
            val listContent = item.map { convertToFinalJsonElement(it) }
            JsonArray(listContent)
        }
        is String -> JsonPrimitive(item)
        is Number -> JsonPrimitive(item)
        is Boolean -> JsonPrimitive(item)
        is Enum<*> -> JsonPrimitive(item.name)
        else -> error("Unsupported type: ${item::class.qualifiedName}")
    }
}