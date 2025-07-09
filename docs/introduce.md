# KoinBoot: Solving Pain Points in Real-World Koin Usage

> An application framework based on Koin, focused on solving configuration management, lifecycle, and module assembly problems in enterprise-level development

## Background: From Reinventing the Wheel to Unified Architecture

Hi My name is Kamo, and I'm a Kotlin development engineer. In our company's projects, we encountered a very common problem: every new project had to rebuild similar tech stacks—network layer, logging, caching, and other infrastructure. Different projects used different technology choices, making maintenance costs very high.

To solve this problem, we decided to pause our current business development and focus on building a unified technical scaffold. We chose KMP as the cross-platform solution and used Koin to manage dependency injection.

Our goal was simple: **make basic components as flexible as building blocks, allowing developers to select what they need and quickly build stable applications**.

## First Encounter with Koin: Excellent but Limited

### Core Working Principle of Koin

Before diving into the problems, let me briefly introduce how Koin works. Many people might think dependency injection is complex, but Koin's design philosophy is simple:

```kotlin
// Basic usage of Koin
val appModule = module {
    single { HttpClient() }
    single { UserRepository(get()) }
    factory { UserViewModel(get()) }
}

startKoin {
    modules(appModule)
}

// When using
val httpClient = koin.get<HttpClient>()
```

**The essence of Koin is actually a smart Map**:

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

```
Type + Name + Scope = Key → Map<Key, InstanceFactory> → get<T>()
```

When you define `single<HttpClient>(named("client"))`, Koin will:
1. Generate a Key (based on type: `HttpClient`, name: `"client"`, scope: defaults to `__root__` when not defined)
2. Store this Key and the corresponding instance factory in the Map
3. When calling `get<HttpClient>(named("client"))`, use the same method to generate the Key and look it up in the Map

### Why Choose Koin

1. **Lightweight**: No reflection, excellent performance
2. **Pure**: Focused on dependency injection, simple and understandable
3. **Kotlin First**: Friendly syntax, elegant DSL
4. **Cross-platform**: Perfect KMP support

It looks perfect, but in actual usage...

## Discovering Problems: Three Limitations of Koin

After deep usage of Koin, we found that while it's excellent as a dependency injection library, it still has gaps from our requirements:

### 1. Configuration Management Difficulties

**Problem scenario**: The scaffold provides a default network module, but different projects need different timeout values.

```kotlin
// Current situation: Can only modify source code to adapt to different projects
val networkModule = module {
    single<HttpClient> {
        HttpClient {
            install(HttpTimeout) {
                // Hard-coded configuration, cannot be dynamically adjusted
                // What if Project A needs 10 seconds, Project B needs 15 seconds?
                requestTimeoutMillis = 10_000
                // or
                // Type unsafe when setting: don't know what type is being put in
                connectTimeoutMillis = getProperty("ktor.client.timeout.request")
            }
        }
    }
}
```

This approach has single configuration sources, cannot be dynamically adjusted, scattered across modules making maintenance difficult, and violates the open-closed principle - every project needs to modify framework code.

### 2. Lack of Lifecycle Management

**Problem scenario**: Application startup needs strict order control, like error monitoring must initialize first.

> Sentry must initialize before all modules, background services should start only after everything is ready. But in Koin, you can only manually stack code before and after `startKoin`

```kotlin
// Current situation: Chaotic startup logic, hard to maintain
fun main() {
    // Manually initialize various services
    Sentry.init { dsn = "..." }
    
    val koin = startKoin {
        modules(appModule)
    }
    
    // Manually start background services
    backgroundService.start()
    
    // As the project becomes complex, this becomes unmaintainable code
}
```

### 3. Limited Extension Capabilities

**Problem scenario**: Cannot implement smart conditional assembly, prone to module conflicts.

```kotlin
val frameworkModule = module {
    single<HttpClientEngine> { OkHttp.create() }
}

val businessModule = module {
    single<HttpClientEngine> { MockEngine.create() }
}

// Which one will take effect? Only decided by loading order
startKoin {
    modules(frameworkModule, businessModule)
}
```

**Core problem**: Koin as a dependency injection library cannot meet true inversion of control needs. It's more like an excellent "engine" rather than a "car" that can be used directly.

## KoinBoot: From Dependency Injection to Application Framework

### From Problem to Solution Approach

When we discovered that Koin as a dependency injection library couldn't meet true inversion of control needs, we decided to build a more complete solution based on Koin. Koin is more like an excellent "engine", while we need a "car" that can be used directly.

**KoinBoot's Goal**: Add configuration management, lifecycle control, auto-configuration, and module import capabilities to Koin, this powerful dependency injection engine, making it a truly practical application framework.

Let's see how KoinBoot solves these problems through four core features:

## Solution 1: Configuration Management System (KoinProperties)

### Why Redesign the Configuration System?

The first problem we need to solve is configuration management. A practical scaffold must allow developers to provide dynamic configuration for different environments and businesses, rather than modifying framework source code.

### Design Process of Configuration System

Our configuration system went through four design stages:

**Stage 1: Choose Underlying Storage Format**

All configuration files are essentially key-value pairs:
- `.properties` files: `key=value`
- `.yml/.json` files: hierarchical key-value pairs

```kotlin
// Choose flat string format for compatibility with various configuration sources
"ktor.client.timeout.request" = 5000L
```

We chose flat strings as underlying storage for compatibility with any configuration source.

**Stage 2: Provide DSL Syntax**

Direct string manipulation is not developer-friendly, and hierarchical structure is not obvious. We used Kotlin's language features to override the String invoke operator, implementing JSON-like DSL syntax:

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

The framework automatically converts this nested structure to flat format `ktor.client.timeout.request=30000L`.

**Stage 3: Implement Smart Code Hints**

Having only DSL is not enough - no code hints, and string-based operators are error-prone.

Since configuration property field keys are fixed, why not make them extension properties of `KoinProperties`?

```kotlin
// Modular configuration properties
properties {
    // When introducing Ktor module, automatically get these configuration hints
    ktor_client_timeout_request = 30000L
    // Type safe
    ktor_client_logging_enabled = true

    // When introducing Kermit module, automatically get these configuration hints
    kermit_severity = Severity.VERBOSE
}
```

This way, when you introduce the Ktor module, you automatically get related configuration hints; when you remove the module, the hints disappear too.

**Stage 4: Type-Safe Configuration Mapping**

Finally, we need to conveniently use these configurations. If we always get values from the map by key, it generates a lot of boilerplate code. A better approach is to map configurations to type-safe objects:

```kotlin
// Map configurations to type-safe objects through annotations and serialization
@KoinPropInstance("ktor.client")
data class KtorClientProperties(
    val timeout: Timeout = Timeout(),
    val logging: Logging = Logging()
)

@KoinPropInstance("ktor.client.timeout")
data class Timeout(
    val request: Long = 30000L,
    val connect: Long = 30000L,
    val socket: Long = 30000L,
)

// Use configuration object directly
val config = koin.get<KtorClientProperties>()
```

![img_3.png](img_3.png)

Based on Kotlin Serialization, use `@KoinPropInstance` annotation to mark configuration classes, using `preKey` to specify the serialization hierarchy starting point.

![img_12.png](img_12.png)

```kotlin
@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class KoinPropInstance(val preKey: String = "")
```

Through these four stages, we implemented a type-safe, code-hinted, concise configuration solution.

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

## Solution 2: Lifecycle Management (KoinLifecycleExtender)

### From "When to Configure" to "When to Take Effect"

After solving the problem of "how to configure", another important question arose: **"When to configure?" and "When to take effect?"**

Configuration alone is not enough. If components cannot be initialized and destroyed at the right time—for example, Sentry must complete initialization before all business modules start to capture global exceptions—the system will have problems.

### Standardized Lifecycle Management Mechanism

We introduced a standardized lifecycle mechanism, similar to Android's Activity Lifecycle:

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

Specific implementation example:

```kotlin
class SentryExtender : KoinLifecycleExtender {
    override fun doConfiguring(context: KoinBootContext) {
        // Initialize Sentry in configuration phase
        Sentry.init {
            // sentry_dsn is an extension property of KoinProperties
            dsn = context.properties.sentry_dsn
            // Configure other options...
        }
    }

    override fun doStopping(context: KoinBootContext) {
        // Close Sentry in stopping phase
        Sentry.close()
    }
}
```

But who calls these methods of `KoinLifecycleExtender`?

### Coordinated Lifecycle Management

We need a management layer to coordinate these lifecycle extenders:

```kotlin
class KoinBoot {
    // Manage lifecycle
    // Call extenders
    // Finally start Koin
}

fun runKoinBoot(initializer: KoinBootInitializer): Koin = KoinBoot.run {
    // 1. Collect all extenders
    initializer(initializer)
    // ...
    // 2. Execute in lifecycle order
    extenders.forEach { it.doConfiguring(context) }
    // ...
    extenders.forEach { it.doModulesLoading(context) }
    // ...
    extenders.forEach { it.doPropertiesLoading(context) }
    // ...
    // 3. Start Koin
    val koin = startKoin { /*...*/ }.koin
    // ...
    extenders.forEach { it.doReady(context) }
    // ...
    koin
}
```

Usage only requires declarative registration:

```kotlin
// Declaratively register extenders when using
runKoinBoot {
    extenders(SentryExtender())
    // All Sentry-related logic is encapsulated in the extender
    // Main startup flow remains clean
}
```

Through this approach, KoinBoot transforms originally chaotic procedural startup code into declarative, pluggable modular lifecycle management. Each module independently manages its own lifecycle, keeping the main startup flow clean.

## Solution 3: Auto-Configuration (KoinAutoConfiguration)

### From "Forced Provision" to "Smart Yielding"

So far, we can flexibly configure modules and precisely control startup/shutdown timing. But in practice, we discovered a deeper need:

**What if the business side wants to use their own implementation instead of the default modules provided by the scaffold (like the preset `HttpClient`)?**

With current logic, we would still forcibly load the default module, potentially causing conflicts. The scaffold should be a "servant", not a "dictator". We need modules to judge for themselves whether they should be loaded.

### Borrowing Auto-Configuration Design Philosophy

We designed the `KoinAutoConfiguration` interface to make modules "smart":

```kotlin
interface KoinAutoConfiguration {

    /** Configuration match condition: if true, matching succeeds and [configure] method will be called */
    fun KoinAutoConfigurationScope.match(): Boolean = true

    /** Configuration scope */
    fun KoinAutoConfigurationScope.configure()

    /** Configuration order, smaller numbers have higher priority */
    val order: Int
        get() = Int.MAX_VALUE
}
```

1. **Conditional judgment**: Check environment to decide whether to take effect
2. **Order control**: Control loading order

```kotlin
val KtorAutoConfiguration = koinAutoConfiguration {
    module {
        // When HttpClientEngine instance doesn't exist, use OkHttp engine
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
    // Result: Use MockEngine, default configuration gracefully yields
}
```

### Implementing True Inversion of Control

This pattern transforms our modules from **"you must use what I provide"** to **"I provide best practices when you need them, I gracefully yield when you customize"**.

This way, the scaffold is both ready to use out of the box and highly flexible.

## Solution 4: Auto-Import (koin-boot-initializer)

### The Final Obstacle

By now, we've built feature-complete module components: configurable, with lifecycle, and smart.

But we discovered a problem:

- Developers need to distinguish between `autoConfigurations` and `extenders`
- Need to understand the specific types and usage of each module
- Need to manually manage all component invocation methods

```kotlin
runKoinBoot {
    autoConfigurations(
        KtorAutoConfiguration,
        MultiplatformSettingsAutoConfiguration
    )
    extenders(KermitExtender(), SentryExtender())
    properties {
        // Business configuration...
    }
    modules(userModule)
}
```

This manual operation is the final obstacle to achieving "plug-and-play".

### Unified Interface Attempt

We tried to unify the interface, letting each module provide a `BootInitializer`, so business-side developers don't need to care about `autoConfigurations` vs `extenders`:

```kotlin
runKoinBoot {
    KtorBootInitializer()
    KermitBootInitializer()
    MultiplatformSettingsBootInitializer()
    SentryBootInitializer()
    properties {
        // Business configuration...
    }
    modules(userModule)
}
```

But this still has problems: every time you add a new module dependency in `build.gradle`, you must remember to manually call its `BootInitializer` in the startup file. When removing dependencies, you must remember to delete the corresponding code.

### Let Build Tools Help

Since the problem is in dependency management, why not let Gradle solve it?

We developed the `koin-boot-initializer` Gradle plugin:

```kotlin
// build.gradle.kts - Declare needed modules
plugins {
    `koin-boot-initializer`
}

val bootDependencies = listOf(
    projects.component.ktor,    // Network module
    projects.component.kermit,  // Logging module
    projects.component.sentry   // Monitoring module
)

koinBootInitializer {
    includes(bootDependencies)
}

dependencies {
    bootDependencies.forEach(::commonMainApi)
}
```

### Auto-Generate Unified Entry Point

The plugin automatically identifies dependencies, finds all `BootInitializer`s, then generates a unified entry file:

```kotlin
// Auto-generated AppBootInitializer
val AppBootInitializer: KoinBootInitializer = {
    io.github.kamo030.KtorBootInitializer()
    io.github.kamo030.KermitBootInitializer()
    io.github.kamo030.MultiplatformSettingsBootInitializer()
}
```

When using, you only need:

```kotlin
runKoinBoot {
    // Auto-generated unified entry point
    AppBootInitializer()

    properties {
        // Business configuration...
    }
    module {
        // Business module declarations...
    }
}
```

With this plugin, whether adding or removing functional modules, we only need to modify dependency declarations in `build.gradle.kts`, without modifying any startup code. This achieves true "plug-and-play".

## Final Result: Complete Development Experience

### Collaboration of Four Solutions

Let's see the development experience when four solutions are combined:

### Step 1: Declare Needed Features

```kotlin
// build.gradle.kts - Declare needed components
plugins {
    `koin-boot-initializer`
}

val bootDependencies = listOf(
    projects.component.ktor,    // Network module
    projects.component.kermit,  // Logging module
    projects.component.sentry   // Monitoring module
)

koinBootInitializer {
    includes(bootDependencies)
}

dependencies {
    bootDependencies.forEach(::commonMainApi)
}
```

### Step 2: Configure Desired Behavior

```kotlin
// main.kt - Clean startup code
fun main() {
    val koin = runKoinBoot {
        // Call auto-generated initializer
        AppBootInitializer()

        // Smart-hinted configuration
        properties {
            kermit_severity = Severity.Verbose
            ktor_client_logging_enabled = true
        }

        // Optional: Business customization
        // module {
        //    single<HttpClientEngine> { OkHttp.create() }
        // }
    }

    // Use directly without caring about initialization details
    val httpClient = koin.get<HttpClient>()
    println("HttpClient is ready to use!")
}
```

### Step 3: Enjoy Automated Execution

When `runKoinBoot` executes:

1. **Lifecycle management** starts operating, Sentry is initialized with priority
2. **Auto-configuration** starts scanning, finds no custom `HttpClient`, so loads the default `HttpClient`
    1. Finds that default `HttpClient` needs `HttpClientEngine`, and business-side developer hasn't defined `HttpClientEngine`, so loads the default `HttpClientEngine`
    2. Gets configured `ktor_client_logging_enabled` through context and automatically initializes `HttpClient` with default configuration
   ```kotlin
   val logging = koin.getPropInstance<KtorProperties.Logging>()
    // Logging configuration
    if (logging.enabled) {
        install(Logging) {
            logger = get()
            level = logging.level
        }
    }
   ```
3. All modules are ready, application starts

### True "Plug-and-Play"

If we decide we no longer need Kermit logging, we only need to:

```kotlin
// build.gradle.kts
val bootDependencies = listOf(
    projects.component.ktor,
    // Remove Kermit dependency
    // projects.component.kermit, 
    projects.component.sentry
)
```

1. Go to `build.gradle.kts` and remove the Kermit dependency
2. Sync the project again

At this point, `kermit_severity` in `main.kt` will immediately show an error, indicating the configuration item doesn't exist.

This is true "plug-and-play": **dependencies determine functionality, code automatically validates**.

## Summary

### From "Manual Transmission" to "Automatic Transmission"

Through KoinBoot, we successfully upgraded a pure dependency injection "engine" into an automated, lifecycle-aware, intelligently configurable application framework.

### Core Values Achieved

- **From dependency injection to inversion of control**: Making framework smarter, developers more focused on business
- **Modular development**: Adding dependencies gains functionality, removing dependencies loses functionality
- **Enhanced development experience**: Goodbye to tedious manual configuration
- **True "plug-and-play"**: Dependencies determine functionality, automatic error validation

**KoinBoot = Koin + Configuration Management + Lifecycle + Auto-Configuration + Auto-Import**

Finally, we achieved our goal of building efficient, flexible, and maintainable multi-platform scaffolds, making every module a pluggable smart component. Building better application architecture.

* [KotlinMultiplatform](https://kotlinlang.org/docs/multiplatform.html)
* [Koin](https://insert-koin.io/)