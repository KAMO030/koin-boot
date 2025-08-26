# KoinBoot：解决 Koin 实际使用中的痛点

> 一个基于 Koin 的应用框架，专注解决企业级开发中的配置管理、生命周期和模块装配问题

## 背景：从重复造轮子到统一架构

我是卡莫，一名 Kotlin 开发工程师。在公司的项目中，我们经常面临一个问题：
每个新项目启动时，都需要重新搭建一套相似的基础设施，比如网络、日志、缓存等。
由于不同项目选择的技术方案各不相同，导致后续的运维和维护成本居高不下。

为了解决这个问题，我们决定暂停新业务的开发，集中精力打造一个统一的技术脚手手架。
我们选择了 Kotlin Multiplatform (KMP) 作为跨平台方案，并使用 Koin 来进行依赖注入管理。
我们希望借助 Koin，让基础组件能像积木一样灵活地插拔和组合，开发者只需按需选用，就能快速构建应用。

## 初识 Koin：优秀但有局限

### Koin 的核心工作原理

在深入探讨之前，我们可以用一句话概括 Koin 的设计思路：它将依赖注入容器看作一个可配置的注册表。
开发者通过 module 来定义依赖关系，应用启动时将这些模块注册到 DI 容器中，运行时再按需从中获取实例。

```kotlin
// Koin 的基本用法
val appModule = module {
    single { HttpClient() }
    single { UserRepository(get()) }
    factory { UserViewModel(get()) }
}

startKoin {
    modules(appModule)
}

// 使用时通过容器获取实例
val httpClient: HttpClient = koin.get()
```

> 简单来说，Koin 并非通过反射来实例化对象，而是将对象的创建逻辑封装在一个可查询的映射（Map）中。运行时，通过一个唯一的 Key 来获取对应的实例。

```kotlin
inline fun indexKey(
    clazz: KClass<*>, typeQualifier: Qualifier?, scopeQualifier: Qualifier
): String {
    return buildString {
        append(clazz.getFullName())
        append(':')
        append(typeQualifier?.value ?: "")
        append(':')
        append(scopeQualifier)
    }
}
```

这个 Key 的生成规则可以总结为：
```
Type + Name + Scope = Key → Map<Key, InstanceFactory> → get<T>()
```

- Type: 声明实例时的类型(默认为泛型推导出的类型)
- Name: 声明实例时的名称(`named("...")`，默认为无)
- Scope: 作用域，默认为全局的 `__root__` 作用域

当你定义 `single<HttpClient>(named("client"))` 时，`Koin` 内部会完成以下工作：

1) 根据类型 `HttpClient`、名称 `"client"` 和默认的 `__root__` 作用域生成一个 Key。

2) 将这个 Key 和对应的实例工厂函数存入内部的 Map 中

3) 调用 `get<HttpClient>(named("client"))` 时，Koin 会用同样的方式生成 Key，并在 Map 中查找对应的工厂来创建或返回实例。

### 为什么选择 Koin

- 轻量级：- 避免反射，性能表现稳定
- 纯粹性：专注于依赖注入，使用门槛低、上手快
- Kotlin First：为 Kotlin 设计，语法友好，DSL 表达力强
- 跨平台：对 KMP 的跨平台能力友好

这在理论上看起来很理想，但在实际的深入使用中，我们还是遇到了一些需要权衡和解决的问题。

## 发现问题：Koin 的三个局限

在深度使用 Koin 的过程中，我们逐步意识到，它作为一个轻量级的依赖注入库，确实有其独特优势，但对于复杂场景会暴露出一些痛点。

### 1. 配置管理困难

**问题场景**：脚手架提供了一个默认的网络模块，但不同项目对网络超时时间有不同的要求。

```kotlin
// 现状：只能修改源码来适应不同项目
val networkModule = module {
    single<HttpClient> {
        HttpClient {
            install(HttpTimeout) {
                // 硬编码配置，无法动态调整
                // A项目要10秒，B项目要15秒怎么办？
                requestTimeoutMillis = 10_000
                // or
                // getProperty<T>() 缺乏类型安全，无法确定取出的值是什么类型
                connectTimeoutMillis = getProperty("ktor.client.timeout.request")
            }
        }
    }
}
```

这种方式存在几个问题：配置来源单一、无法动态调整，并且配置散落在各个模块中，难以维护。这种做法不仅违反了开闭原则，还导致每个新项目都需要修改脚手架的源码。

### 2. 缺少生命周期管理

**问题场景**：应用启动时，组件的初始化需要有严格的顺序。例如，错误监控（如 `Sentry`）必须在所有其他模块之前完成初始化。

> `Sentry` 必须在应用启动之初就绪，而后端服务则要等所有准备工作完成后才能启动。但在 `Koin` 中，我们只能在 `startKoin` 函数前后手动堆砌初始化代码。

> ⚠️ 这里的生命周期指的是容器生命周期，而不是安卓的生命周期。

```kotlin
// 现状：启动逻辑混乱，难以维护
fun main() {
    // 手动初始化各种服务
    Sentry.init { dsn = "..." }

    val koin = startKoin {
        modules(appModule)
    }

    // 手动启动后台服务
    backgroundService.start()

    // 项目复杂后，这里会变成难以维护的代码
}
```

### 3. 扩展能力有限

**问题场景**：无法实现模块的自动条件装配，这容易导致模块定义冲突。

```kotlin
// 框架提供的默认模块
val frameworkModule = module {
    single<HttpClientEngine> { OkHttp.create() }
}
// 业务方定义的测试模块
val businessModule = module {
    single<HttpClientEngine> { MockEngine.create() }
}

// 究竟哪个 HttpClientEngine 会生效？
// 结果完全取决于模块的加载顺序
startKoin {
    modules(frameworkModule, businessModule)
}
```

**核心问题**：Koin 本质上是一个依赖注入库，它很好地解决了“获取依赖”的问题，但对于“控制反转（IoC）”中更深层次的自动化管理需求则支持有限。
它更像一个性能优秀的“引擎”，而不是一辆可以直接驾驶的“汽车”。

## KoinBoot：从依赖注入到应用框架

### 从问题到解决方案的思路

当我们发现 Koin 作为依赖注入库无法满足真正的控制反转需求时，我们决定基于 Koin 构建一个更为完整的解决方案。
我们计划为 Koin 补充配置管理、生命周期控制、自动装配和模块自动导入等功能，使其成为一个开箱即用的应用框架。

## 解决方案一：配置管理系统 (KoinProperties)

### 为什么需要重新设计配置系统？

我们首先要解决的是配置管理问题。一个实用的脚手架，必须让开发者能够为不同环境、不同业务提供动态配置，而不是修改项目源码。

### 配置系统的设计过程

**第一阶段：选择底层存储格式**

所有配置文件，无论是 .properties、.yml 还是 .json，其本质都是键值对（Key-Value）。

```kotlin
"ktor.client.timeout.request" = 5000L
```

我们选择扁平化字符串作为底层存储，因为它具有最好的通用性，可以兼容任何配置来源。

**第二阶段：提供 DSL 写法**

直接操作字符串对开发者不友好, 层级结构不明显。我们利用 Kotlin 的语言特性，重写 String 的 invoke 操作符，实现了类似 JSON 的层级式 DSL 写法：

```kotlin
properties {
    "ktor" {
        "client" {
            "timeout" {
                "request"(30000L)
            }
        }
    }
}
```

框架在内部会自动将这种嵌套结构转换为 `ktor.client.timeout.request=30000L` 这样的扁平格式。

**第三阶段：实现代码提示**

但光有 DSL 还不够，基于字符串的操作不仅容易写错，也没有代码提示。
既然这些配置项的 Key 通常是固定的，我们自然想到，为什么不把它们定义成 KoinProperties 的扩展属性呢？

```kotlin
// 模块化的配置属性
properties {
    // 引入 Ktor 模块时，自动获得这些配置提示
    ktor_client_timeout_request = 30000L
    // 类型安全
    ktor_client_logging_enabled = true

    // 引入 Kermit 模块时，自动获得这些配置提示
    kermit_severity = Severity.VERBOSE
}
```

> 这样一来，当你引入 Ktor 模块时，IDE 就会自动提示相关的配置项；移除模块后，这些提示也会相应消失。

![img_3.png](images/img_3.png)

**第四阶段：类型安全的配置映射**

最后，我们需要一种更方便的方式来使用这些配置。如果每次都通过 Key 去 Map 中获取值，会产生大量模板代码。
一个更好的方案是将相关的配置项映射为一个类型安全的数据对象：

```kotlin
data class KtorClientProperties(
    val timeout: Timeout = Timeout(),
    val logging: Logging = Logging()
)

data class Timeout(
    val request: Long = 30000L,
    val connect: Long = 30000L,
    val socket: Long = 30000L,
)

// 使用时直接获取配置对象
val config = koin.get<KtorClientProperties>()
```

> 但是这样在只是用内层配置时，会很丢失层级结构，因此我们需要一种更通用的方式来映射配置项。
>
```kotlin
// 使用时直接获取配置对象
val config = koin.get<Timeout>()
```

所以我们需要基于 Kotlin Serialization 中 `@MetaSerializable` 的特性定义一个 `@KoinPropInstance` 注解标注配置属性的数据类，用 `preKey` 来指定序列化的层级起点。

> @MetaSerializable 能在运行时获取元数据拿到配置属性数据类上 `@KoinPropInstance` 配置的 `preKey` 字段的值
![img_12.png](images/img_12.png)
>
```kotlin
@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class KoinPropInstance(val preKey: String = "")
```

> 这样只需要包装一层函数通过 `@KoinPropInstance` 注解的类，就可以实现类型安全的配置映射与序列化对象。

```kotlin
@KoinPropInstance("ktor.client.timeout")
data class Timeout(
    val request: Long = 30000L,
    val connect: Long = 30000L,
    val socket: Long = 30000L,
)
val config = koin.get<Timeout>()
```

通过这四个阶段，我们实现了一套类型安全、支持代码提示且写法简洁的配置方案。

```kotlin
// set
val properties = KoinProperties().apply {
    ktor_client_timeout_request = 1000
    ktor_client_logging_level = LogLevel.BODY
}
// get
val ktorClientProperties = properties.asPropInstance<KtorProperties>()!!
val timeout = properties.asPropInstance<KtorProperties.Timeout>()!!
val logging = properties.asPropInstance<KtorProperties.Logging>()!!
//  assert
assert(1000L == timeout.request && ktorClientProperties.client.timeout.request == timeout.request)
assert(LogLevel.BODY == logging.level && ktorClientProperties.client.logging.level == logging.level)
```

## 解决方案二：生命周期管理 (KoinLifecycleExtender)

### 从"何时配置"到"何时生效"

解决了配置"怎么配"的问题后，另一个重要问题出现了：**"何时配置？"以及"配置何时生效？"**

仅有配置还不够，如果不能在正确的时间点初始化和销毁组件——比如 Sentry 必须在所有业务模块启动前完成初始化来捕获全局异常，才能保证应用启动与运行的全部异常被捕获。

### 标准化的生命周期管理机制

为此，我们引入了一套标准化的生命周期机制，其设计思想类似于 Android 的 Activity Lifecycle。

```kotlin
enum class KoinPhase {
    Starting,
    Configuring,
    ModulesLoading,
    PropertiesLoading,
    Ready,
    Running,
    Stopping,
    Stopped
}

interface KoinLifecycleExtender {
    fun doStarting(context: KoinBootContext) {}
    fun doConfiguring(context: KoinBootContext) {}
    fun doModulesLoading(context: KoinBootContext) {}
    fun doPropertiesLoading(context: KoinBootContext) {}
    fun doReady(context: KoinBootContext) {}
    fun doStopping(context: KoinBootContext) {}
    fun doStopped(context: KoinBootContext) {}
}
```

下面是一个具体的实现示例：

```kotlin
class SentryExtender : KoinLifecycleExtender {
    override fun doConfiguring(context: KoinBootContext) {
        // 在配置阶段初始化 Sentry
        Sentry.init {
            // sentry_dsn 是 KoinProperties 的扩展属性
            dsn = context.properties.sentry_dsn
            // 配置其他选项...
        }
    }

    override fun doStopping(context: KoinBootContext) {
        // 在停止阶  段关闭 Sentry
        Sentry.close()
    }
}
```

但是，谁来调 `KoinLifecycleExtender` 的这些方法？

### 生命周期的协调管理

我们需要一个管理层来协调这些生命周期扩展器：

```kotlin
class KoinBoot {
    // 管理生命周期
    // 调用扩展器
    // 最终启动 Koin
}

fun runKoinBoot(initializer: KoinBootInitializer): Koin = KoinBoot.run {
    // 1. 收集所有扩展器
    initializer(initializer)
    // ...
    // 2. 按生命周期顺序执行
    extenders.forEach { it.doConfiguring(context) }
    // ...
    extenders.forEach { it.doModulesLoading(context) }
    // ...
    extenders.forEach { it.doPropertiesLoading(context) }
    // ...
    // 3. 启动 Koin
    val koin = startKoin { /*...*/ }.koin
    // ...
    extenders.forEach { it.doReady(context) }
    // ...
    koin
}
```

在实际使用时，开发者只需要以声明式的方式注册扩展器即可：

```kotlin
// 使用时声明式注册扩展器
runKoinBoot {
    extenders(SentryExtender())
    // 所有 Sentry 相关逻辑都封装在扩展器中
    // 主启动流程保持简洁
}
```

通过这种方式，KoinBoot 将原本混乱的过程式启动代码，变成了声明式的、可插拔的模块化生命周期管理。
每个模块都可以独立创建声明扩展器去影响应用的启动生命周期，主启动流程保持简洁。

## 解决方案三：自动装配 (KoinAutoConfiguration)

### 从"强制提供"到"智能让位"

到目前为止，我们既可以灵活地配置属性，又能精确地控制它们的启停时机。但在实践中，我们遇到了一个更深层次的需求：

**脚手架提供了一些默认 `koin module`（比如一个预置了 HttpClient 的 `http module`），如果业务方想用自己的实现，该怎么办？**

按现有逻辑，我们还是会强行加载默认模块，可能导致冲突。我们需要让模块自己判断是否应该加载。

### 借鉴自动配置的设计理念

我们设计了 `KoinAutoConfiguration` 接口，让模块变得自动化：

```kotlin
interface KoinAutoConfiguration {

    /** 配置匹配条件: 如果为true则匹配成功会被调用 [configure] 方法 */
    fun KoinAutoConfigurationScope.match(): Boolean = true

    /** 配置作用域 */
    fun KoinAutoConfigurationScope.configure()

    /** 配置顺序，数字越小优先级越高  */
    val order: Int
        get() = Int.MAX_VALUE
}
```

该接口主要包含两个核心功能：
1. **条件化判断**：通过 `match` 方法检查环境，决定当前模块是否应该生效
2. **顺序控制**：通过 `order` 属性控制加载的模块优先级。

```kotlin
val KtorAutoConfiguration = koinAutoConfiguration {
    module {
        // 当不存在 HttpClientEngine 实例时，使用 OkHttp 引擎
        onMissInstances<HttpClientEngine> {
            single<HttpClientEngine> { OkHttp.create() }
        }
        single<HttpClient> { HttpClient(get<HttpClientEngine>()) }
    }
}

val userModule = module {
    single<HttpClientEngine> { MockEngine.create() }
}

runKoinBoot {
    autoConfigurations(KtorAutoConfiguration)
    modules(userModule)
    // 结果：使用 MockEngine，默认配置优雅让位
}
```

### 实现真正的控制反转

这种模式让我们的模块从 **"我提供什么你就必须用什么"**变成了**"你需要时我提供最佳实践，你自定义时我自动让位"**。

这样一来，脚手架既能做到开箱即用，又为业务定制保留了高度的灵活性。

## 解决方案四：自动导入 (koin-boot-initializer)

### 最后的障碍

到这里，我们已经打造出了一套功能完善的模块化组件：它们可动态灵活配置、有生命周期、还能自动加载默认模块。

但我们发现了一个问题：

- 开发者需要区分 `autoConfigurations` 和 `extenders`。
- 开发者需要了解每个模块的具体用法和类型。
- 开发者需要手动管理所有组件的注册调用。

```kotlin
runKoinBoot {
    // 需要手动注册所有自动配置
    autoConfigurations(
        KtorAutoConfiguration,
        MultiplatformSettingsAutoConfiguration
    )
    // 还需要手动注册所有生命周期扩展器
    extenders(KermitExtender(), SentryExtender())
    properties {
        // 业务配置...
    }
    modules(userModule)
}
```

这种手动的注册方式，是我们实现“即插即用”体验的最后一道障碍。

### 统一接口的尝试

我们首先尝试统一接口，让每个模块都提供一个 `BootInitializer`。这样，开发者就不再需要关心一个模块到底是 autoConfigurations 还是 extenders。

```kotlin
// SentryBootInitializer.kt
val SentryBootInitializer: KoinBootInitializer = {
    extenders(SentryExtender())
}
// MultiplatformSettingsBootInitializer.kt
val MultiplatformSettingsBootInitializer: KoinBootInitializer = {
    autoConfigurations(MultiplatformSettingsAutoConfiguration())
}
// main.kt
runKoinBoot {
    
    MultiplatformSettingsBootInitializer()
    SentryBootInitializer()
    // ...其他默认模块的BootInitializer
    properties {
        // 业务配置...
    }
    modules(userModule)
}
```

但这仍然不够理想：每当在 `build.gradle` 中添加一个新的模块依赖时，开发者必须记得在启动文件里手动调用它的 `BootInitializer`。
反之，移除依赖时，也必须记得删掉对应的调用代码。

### 让构建工具来帮忙

既然问题出在依赖管理上，那为什么不让 Gradle 来解决呢？

为此，我们编写了一个名为 `koin-boot-initializer` 的 Gradle 插件：

```kotlin
// build.gradle.kts - 声明需要的模块
plugins {
    `koin-boot-initializer`
}

val bootDependencies = listOf(
    projects.component.ktor,    // 网络模块
    projects.component.kermit,  // 日志模块
    projects.component.sentry   // 监控模块
)

koinBootInitializer {
    includes(bootDependencies)
}

dependencies {
    bootDependencies.forEach(::commonMainApi)
}
```

### 自动生成统一入口

这个插件会在编译时自动扫描所有依赖，找到其中包含的 `BootInitializer` 实现，然后生成一个统一的入口文件。

```kotlin
// 自动生成的 AppBootInitializer
val AppBootInitializer: KoinBootInitializer = {
    io.github.kamo030.KtorBootInitializer()
    io.github.kamo030.KermitBootInitializer()
    io.github.kamo030.MultiplatformSettingsBootInitializer()
}
```

使用时，代码就变得非常简洁了：

```kotlin
runKoinBoot {
    // 自动生成的统一入口
    AppBootInitializer()

    properties {
        // 业务配置...
    }
    module {
        // 业务模块声明...
    }
}
```

有了这个插件，无论是添加还是删除功能模块，我们都只需要在 `build.gradle.kts` 中修改依赖声明，而无需改动任何启动代码。这真正实现了“即插即用”。

## 最终成果：完整的开发体验

### 四个解决方案的协作

让我们来看看这四个解决方案组合起来，能带来怎样的开发体验:

### 第一步：声明需要的功能

在 `build.gradle.kts` 中，像管理普通依赖一样声明需要的功能模块。
```kotlin
// build.gradle.kts - 声明需要的组件
plugins {
    `koin-boot-initializer`
}

val bootDependencies = listOf(
    projects.component.ktor,    // 网络模块
    projects.component.kermit,  // 日志模块
    projects.component.sentry   // 监控模块
)

koinBootInitializer {
    includes(bootDependencies)
}

dependencies {
    bootDependencies.forEach(::commonMainApi)
}
```

### 第二步：配置想要的行为

在应用的入口处，调用启动应用的函数，配置属性与业务模块。

```kotlin
// main.kt - 简洁的启动代码
fun main() {
    val koin = runKoinBoot {
        // 调用自动生成的初始化器
        AppBootInitializer()

        // 通过带IDE提示的 DSL 进行配置
        properties {
            kermit_severity = Severity.Verbose
            ktor_client_logging_enabled = true
        }

        // 可选：提供业务方的自定义实现
        // module {
        //    single<HttpClientEngine> { OkHttp.create() }
        // }
    }

    // 直接从容器获取实例使用，无需关心初始化细节
    val httpClient = koin.get<HttpClient>()
    println("HttpClient is ready to use!")
}
```

### 第三步：享受自动化运行

当 `runKoinBoot` 执行时，框架内部会自动完成以下工作：

1. **生命周期管理**开始运转，`SentryExtender` 在 `Configuring` 阶段被调用，确保 `Sentry` 被优先初始化。
2. **自动装配**开始扫描。它发现业务方没有提供自定义的 `HttpClientEngine`，于是加载了 `Ktor` 模块中默认的 `OkHttp` 引擎。
3. 接着，框架初始化 `HttpClient`。在初始化过程中，它会从上下文中读取 `ktor_client_logging_enabled` 等配置，并根据这些配置来决定是否安装日志插件。
   ```kotlin
   val logging = koin.getPropInstance<KtorProperties.Logging>()
    // 日志配置
    if (logging.enabled) {
        install(Logging) {
            logger = get()
            level = logging.level
        }
    }
   ```
4. 所有模块都准备就绪后，应用成功启动。

### 真正的"即插即用"

如果我们决定不再需要 Kermit 日志库，操作非常简单：

1. 回到 build.gradle.kts，注释或删除 Kermit 的依赖声明。

    ```kotlin
    // build.gradle.kts
    val bootDependencies = listOf(
        projects.component.ktor,
        // 删掉 Kermit 依赖
        // projects.component.kermit, 
        projects.component.sentry
    )
    ```

2. 同步 `Gradle` 项目

这时 `main.kt` 中之前写的 `kermit_severity` 配置项会立刻在 IDE 中标红报错，提示该属性不存在。

这就是真正的"即插即用"：**依赖决定功能，代码自动校验**。

## 总结

### 从"手动挡"到"自动挡"

通过 KoinBoot，我们成功地将 Koin 这个纯粹的依赖注入“引擎”，升级成了一个自动化的、具备生命周期管理和自动配置模块能力的应用框架。

### 实现的核心价值

- **从依赖注入到控制反转**：让框架变得更自动化，使开发者能更专注于业务逻辑。
- **模块化开发**：添加依赖即获得功能，移除依赖则功能自动失效。
- **开发体验提升**：告别繁琐的手动配置和初始化代码。
- **真正的"即插即用"**：`Gradle` 依赖关系直接决定了应用的功能，并通过代码静态检查来保证一致性。

**KoinBoot = Koin + 配置管理 + 生命周期 + 自动装配 + 自动导入**

最终，我们实现了构建一个高效、灵活且易于维护的多平台脚手架的目标，让每个功能模块都成为了可插拔的默认自动配置组件，从而帮助我们构建更好的应用架构。

## 实际应用案例：动态配置管理系统

### 业务场景

假设我们正在开发一个企业级的移动应用，需要支持以下功能：

- 不同环境（开发、测试、生产）使用不同的服务器配置
- 运营人员可以在后台动态调整应用行为，例如功能开关、API 超时时间等
- 支持灰度发布，不同用户群体使用不同的配置
- 配置变更需要实时生效，无需重启应用

### 基于 KoinBoot 的解决方案

#### 1. 配置远程拉取扩展器

我们首先可以创建一个 `KoinLifecycleExtender`，它负责在应用启动的配置阶段从远程服务器拉取最新配置。

```kotlin
class RemoteConfigExtender(
    private val configService: ConfigService,
    private val configWatcher: ConfigWatcher
) : KoinLifecycleExtender {

    override fun doConfiguring(context: KoinBootContext) {
        // 在配置阶段，从远程服务器拉取最新配置
        val remoteConfig = fetchRemoteConfig()

        // 将远程配置合并到本地配置中
        context.properties.putAll(remoteConfig)

        // 启动配置监听服务
        startConfigWatcher(context)
    }

    private fun fetchRemoteConfig(): Map<String, Any> {
        // 调用配置服务 API
        return configService.getConfiguration(
            appVersion = BuildConfig.VERSION_NAME,
            userId = getCurrentUserId(),
            environment = getCurrentEnvironment()
        )
    }

    private fun startConfigWatcher(context: KoinBootContext) {
        // 使用 WebSocket 或长轮询监听配置变更
        configWatcher.onConfigChanged { newConfig ->
            // 配置变更时，更新 KoinProperties
            context.properties.putAll(newConfig)

            // 触发相关组件的重新配置
            notifyConfigChanged(context, newConfig)
        }
    }
}
```

#### 2. 热更新机制

为了响应配置变更，我们还需要一个热更新扩展器。

```kotlin
class HotReloadExtender(
    private val configWatcher: ConfigWatcher
) : KoinLifecycleExtender {

    override fun doReady(context: KoinBootContext) {
        // 应用就绪后，注册配置变更监听器
        configWatcher.onConfigChanged { changedKeys ->
            changedKeys.forEach { key ->
                when {
                    key.startsWith("ktor.client") -> {
                        // 重新配置网络客户端
                        reconfigureHttpClient(context)
                    }
                    key.startsWith("cache") -> {
                        // 重新配置缓存
                        reconfigureCache(context)
                    }
                    key.startsWith("feature_flags") -> {
                        // 更新功能开关
                        updateFeatureFlags(context)
                    }
                }
            }
        }
    }

    private fun reconfigureHttpClient(context: KoinBootContext) {
        val koin = context.koin
        val newConfig = koin.getPropInstance<KtorClientProperties>()

        // 重新创建 HttpClient 实例
        val newClient = HttpClient(koin.get<HttpClientEngine>()) {
            install(HttpTimeout) {
                requestTimeoutMillis = newConfig.timeout.request
                connectTimeoutMillis = newConfig.timeout.connect
            }
        }

        // 替换容器中的实例
        koin.declare(newClient, override = true)
    }
}
```

#### 3. 功能开关管理

功能开关也可以通过配置系统进行类型安全的管理。

```kotlin
@KoinPropInstance("feature_flags")
data class FeatureFlags(
    val newUserInterface: Boolean = false,
    val experimentalFeatures: Boolean = false,
    val premiumFeatures: Boolean = false
)

class FeatureFlagExtender : KoinLifecycleExtender {

    override fun doReady(context: KoinBootContext) {
        val featureFlags = context.koin.getPropInstance<FeatureFlags>()

        // 根据功能开关配置应用行为
        if (featureFlags.newUserInterface) {
            enableNewUI()
        }

        if (featureFlags.experimentalFeatures) {
            enableExperimentalFeatures()
        }
    }
}
```

#### 4. 完整的使用示例

将这些扩展器注册到 KoinBoot 中。

```kotlin
fun main() {
    val koin = runKoinBoot {
        // 加载所有自动发现的模块
        AppBootInitializer()

        // 注册动态配置扩展器
        extenders(
            RemoteConfigExtender(),
            HotReloadExtender(),
            FeatureFlagExtender()
        )

        // 本地默认配置（作为兜底）
        properties {
            ktor_client_timeout_request = 30000L
            feature_flags_newUserInterface = false
            cache_max_size = 1024
        }
    }

}
```

### 实现的效果

1. **动态配置更新**：运营人员在后台修改配置后，所有客户端能在短时间内自动更新，无需发布新版本
2. **灰度发布支持**：可以为不同用户群体下发不同的配置，实现渐进式的功能发布和测试
3. **实时故障恢复**：当发现线上新功能存在问题时，可以通过配置开关立即将其关闭
4. **A/B 测试**：可以方便地为不同用户群体提供不同的 UI 或功能配置，以进行效果对比

KoinBoot 让 koin 不仅是一个DI容器，而是更有潜力成为一个能够支撑现代应用开发需求的完整解决方案。
它让开发者能够更专注于业务逻辑的实现，而将配置管理、生命周期控制等基础设施问题交给框架来处理。
* [KotlinMultiplatform](https://kotlinlang.org/docs/multiplatform.html)
* [Koin](https://insert-koin.io/)