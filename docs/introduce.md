# KoinBoot: Solving the Pain Points of Using Koin in Practice

> An application framework built on Koin, focused on solving configuration management, lifecycle, and module wiring issues in enterprise development

## Background: From Rebuilding the Wheel to a Unified Architecture

I’m Kamo, a Kotlin engineer. In our company’s projects, we often face a recurring problem:
Every new project requires re‑establishing similar infrastructure such as networking, logging, and caching.
Different projects choose different technical stacks, which drives up operations and maintenance costs later on.

To address this, we paused new business development and focused on building a unified technical scaffold.
We chose Kotlin Multiplatform (KMP) for cross‑platform development and used Koin for dependency injection.
With Koin, we hoped to make foundational components plug‑and‑play like building blocks so developers could quickly assemble applications by picking what they need.

## First Look at Koin: Great, but With Limits

### How Koin Works at Its Core

Before diving deeper, here’s Koin’s design idea in one sentence: it treats the dependency injection container as a configurable registry.
Developers define dependencies in modules, register these modules into the DI container at app startup, and fetch instances from it at runtime as needed.

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

// Retrieve instances from the container when needed
val httpClient: HttpClient = koin.get()
```

> Simply put, Koin doesn’t use reflection to instantiate objects. Instead, it encapsulates object creation logic in a queryable Map. At runtime, a unique key is used to retrieve the corresponding instance.

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

The key can be summarized as:
```
Type + Name + Scope = Key → Map<Key, InstanceFactory> → get<T>()
```

- Type: The declared type of the instance (defaults to the type inferred from generics)
- Name: The declared name of the instance (e.g., `named("...")`; none by default)
- Scope: Scope, defaulting to the global `__root__` scope

When you define `single<HttpClient>(named("client"))`, Koin does the following internally:

1) Generates a key based on the type `HttpClient`, the name `"client"`, and the default `__root__` scope.

2) Stores this key and the corresponding instance factory function into an internal Map.

3) When `get<HttpClient>(named("client"))` is called, Koin generates the same key and looks up the factory in the Map to create or return the instance.

### Why Choose Koin

- Lightweight: avoids reflection with stable performance
- Purist: focuses on DI; low barrier to entry, quick to adopt
- Kotlin‑first: designed for Kotlin; friendly syntax with expressive DSL
- Cross‑platform: friendly to KMP

This looks ideal in theory, but deeper usage reveals some trade‑offs and issues to solve.

## Problems Discovered: Three Limitations of Koin

As we used Koin more deeply, we realized that while it has unique advantages as a lightweight DI library, it exposes pain points in complex scenarios.

### 1) Difficult Configuration Management

Problem scenario: The scaffold provides a default networking module, but different projects require different network timeouts.

```kotlin
// Status quo: modifying source code is the only way to adapt to different projects
val networkModule = module {
   single<HttpClient> {
      HttpClient {
         install(HttpTimeout) {
            // Hard‑coded configuration; can’t be adjusted dynamically
            // Project A needs 10s, project B needs 15s — what to do?
            requestTimeoutMillis = 10_000
            // or
            // getProperty<T>() lacks type safety; you can’t be sure what type is returned
            connectTimeoutMillis = getProperty("ktor.client.timeout.request")
         }
      }
   }
}
```

This approach has several problems: configuration sources are single‑sourced, can’t be adjusted dynamically, and are scattered across modules, making them hard to maintain. It violates the open‑closed principle and forces every new project to modify the scaffold’s source code.

### 2) Missing Lifecycle Management

Problem scenario: Component initialization at app startup needs strict ordering. For example, error monitoring (e.g., `Sentry`) must initialize before all other modules.

> Sentry must be ready at the very beginning of app startup, while back‑end services should start only after everything is prepared. But with Koin, we can only pile initialization code around the `startKoin` call manually.

> Note: The lifecycle here refers to the container lifecycle, not Android’s lifecycle.

```kotlin
// Status quo: startup logic is messy and hard to maintain
fun main() {
    // Manually initialize various services
    Sentry.init { dsn = "..." }

    val koin = startKoin {
        modules(appModule)
    }

    // Manually start background services
    backgroundService.start()

    // As the project grows, this becomes unmaintainable
}
```

### 3) Limited Extensibility

Problem scenario: Conditional auto‑wiring of modules isn’t available, leading to conflicts between module definitions.

```kotlin
// Default module provided by the framework
val frameworkModule = module {
    single<HttpClientEngine> { OkHttp.create() }
}
// Test module defined by the business team
val businessModule = module {
    single<HttpClientEngine> { MockEngine.create() }
}

// Which HttpClientEngine takes effect?
// It entirely depends on module loading order
startKoin {
    modules(frameworkModule, businessModule)
}
```

Core issue: Koin is essentially a DI library that solves “getting dependencies” very well, but it provides limited support for deeper automation needs found in Inversion of Control (IoC).
It’s more like a high‑performance “engine” than a “car” you can drive directly.

## KoinBoot: From DI to an Application Framework

### From Problems to a Solution

When we found Koin insufficient for true IoC needs, we decided to build a more complete solution on top of it.
We planned to add configuration management, lifecycle control, auto‑configuration, and module auto‑import to turn it into a ready‑to‑use application framework.

## Solution 1: Configuration Management (KoinProperties)

### Why Redesign Configuration?

The first issue to solve is configuration management. A practical scaffold must allow developers to provide dynamic configurations for different environments and businesses, without modifying source code.

### Designing the Configuration System

Phase 1: Choose the underlying storage format

All configuration files — .properties, .yml, .json — are essentially key‑value pairs.

```kotlin
"ktor.client.timeout.request" = 5000L
```

We chose flat strings as the underlying storage because they are the most universal and can interoperate with any configuration source.

Phase 2: Provide a DSL

Working with strings directly is unfriendly and hides structure. Leveraging Kotlin’s language features, we override String’s invoke operator to create a JSON‑like hierarchical DSL:

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

Internally, the framework automatically flattens this nested structure into `ktor.client.timeout.request=30000L`.

Phase 3: Provide code completion

A DSL alone isn’t enough. String‑based operations are error‑prone and lack code completion.
Since these configuration keys are generally fixed, why not define them as extension properties on KoinProperties?

```kotlin
// Modular configuration properties
properties {
    // When you include the Ktor module, you automatically get these suggestions
    ktor_client_timeout_request = 30000L
    // Type‑safe
    ktor_client_logging_enabled = true

    // Include the Kermit module to get suggestions for these
    kermit_severity = Severity.VERBOSE
}
```

> This way, when you include the Ktor module, the IDE suggests relevant configuration properties; remove the module and the suggestions disappear.

![img_3.png](images/img_3.png)

Phase 4: Type‑safe configuration mapping

Finally, we need a more convenient way to use these configurations. If you fetch values from a Map by key each time, you’ll write a lot of boilerplate.
A better approach is to map related keys to a type‑safe data object:

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

// Retrieve the config object directly
val config = koin.get<KtorClientProperties>()
```

> However, when you only need an inner subset, you lose the hierarchy. We need a more general way to map configuration items.
>
```kotlin
// Retrieve the inner config directly
val config = koin.get<Timeout>()
```

So, based on Kotlin Serialization’s `@MetaSerializable`, we define an annotation `@KoinPropInstance` for configuration data classes, with `preKey` specifying the starting path for serialization.

> With @MetaSerializable we can access runtime metadata and read the `preKey` value configured on `@KoinPropInstance` for the config data class.
![img_12.png](images/img_12.png)
>
```kotlin
@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class KoinPropInstance(val preKey: String = "")
```

> With a small wrapper leveraging classes annotated by `@KoinPropInstance`, we can implement type‑safe mapping and object serialization from properties.

```kotlin
@KoinPropInstance("ktor.client.timeout")
data class Timeout(
    val request: Long = 30000L,
    val connect: Long = 30000L,
    val socket: Long = 30000L,
)
val config = koin.get<Timeout>()
```

With these four phases we built a type‑safe, IDE‑friendly, and concise configuration solution.

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
// assert
assert(1000L == timeout.request && ktorClientProperties.client.timeout.request == timeout.request)
assert(LogLevel.BODY == logging.level && ktorClientProperties.client.logging.level == logging.level)
```

## Solution 2: Lifecycle Management (KoinLifecycleExtender)

### From “When to Configure” to “When It Takes Effect”

After solving “how to configure,” the next crucial questions are: “when to configure?” and “when will it take effect?”

Configuration alone isn’t enough. If components aren’t initialized or destroyed at the right time — e.g., Sentry must be initialized before business modules to capture global exceptions — you can’t guarantee all startup/runtime exceptions are captured.

### A Standardized Lifecycle Mechanism

We introduced a standardized lifecycle mechanism, inspired by Android’s Activity lifecycle.

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

A concrete implementation example:

```kotlin
class SentryExtender : KoinLifecycleExtender {
    override fun doConfiguring(context: KoinBootContext) {
        // Initialize Sentry during the Configuring phase
        Sentry.init {
            // sentry_dsn is an extension property on KoinProperties
            dsn = context.properties.sentry_dsn
            // Other options…
        }
    }

    override fun doStopping(context: KoinBootContext) {
        // Close Sentry during the Stopping phase
        Sentry.close()
    }
}
```

But who calls these `KoinLifecycleExtender` methods?

### Orchestrating the Lifecycle

We need a manager to orchestrate lifecycle extenders:

```kotlin
class KoinBoot {
    // Manage lifecycle
    // Invoke extenders
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

In practice, developers just register extenders declaratively:

```kotlin
// Declaratively register extenders
runKoinBoot {
    extenders(SentryExtender())
    // All Sentry logic is encapsulated in the extender
    // The main startup flow stays clean
}
```

With this, KoinBoot turns messy, procedural startup code into declarative, pluggable, modular lifecycle management.
Each module can independently declare extenders to influence the application’s startup lifecycle, keeping the main flow clean.

## Solution 3: Auto‑Configuration (KoinAutoConfiguration)

### From “Mandatory Defaults” to “Smart Fallbacks”

So far, we can flexibly configure properties and precisely control when things start and stop. In practice, we needed more:

The scaffold provides some default Koin modules (e.g., an `http module` with a pre‑configured HttpClient). What if the business team wants its own implementation?

By default we’d still load the framework module, potentially causing conflicts. We need modules to decide for themselves whether they should load.

### Borrowing the Auto‑Configuration Idea

We designed the `KoinAutoConfiguration` interface to make modules autonomous:

```kotlin
interface KoinAutoConfiguration {

    /** Matching condition: when true, the [configure] method will be invoked */
    fun KoinAutoConfigurationScope.match(): Boolean = true

    /** Configuration scope */
    fun KoinAutoConfigurationScope.configure()

    /** Load order; the smaller the number, the higher the priority */
    val order: Int
        get() = Int.MAX_VALUE
}
```

This interface provides two core capabilities:
1. Conditional checks: the `match` method decides whether the module should be active.
2. Ordering: the `order` property controls priority when loading modules.

```kotlin
val KtorAutoConfiguration = koinAutoConfiguration {
    module {
        // If there is no HttpClientEngine, use OkHttp
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
    // Result: MockEngine is used; the default config gracefully yields
}
```

### Achieving True Inversion of Control

This pattern turns our modules from “you must use what I provide” into “I provide best practices by default, and step aside when you customize.”

Thus the scaffold works out‑of‑the‑box while retaining high flexibility for business customization.

## Solution 4: Auto‑Import (koin‑boot‑initializer)

### The Last Obstacle

By now we have modular components that are dynamically configurable, lifecycle‑aware, and auto‑load defaults.

But we found a lingering issue:

- Developers must distinguish between `autoConfigurations` and `extenders`.
- They must know each module’s usage and types.
- They must manually manage registration for all components.

```kotlin
runKoinBoot {
    // Manually register all auto‑configurations
    autoConfigurations(
        KtorAutoConfiguration,
        MultiplatformSettingsAutoConfiguration
    )
    // Manually register all lifecycle extenders
    extenders(KermitExtender(), SentryExtender())
    properties {
        // Business configuration…
    }
    modules(userModule)
}
```

This manual registration is the final barrier to plug‑and‑play.

### A Unified Interface Attempt

We first unified the interface so that each module exposes a `BootInitializer`. Developers no longer need to care whether a module is an auto‑configuration or an extender.

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
    // ...BootInitializers of other default modules
    properties {
        // Business configuration…
    }
    modules(userModule)
}
```

Still not ideal: whenever you add a new module dependency in `build.gradle`, you must remember to call its `BootInitializer` in the startup code; and remove it when the dependency is removed.

### Let the Build Tool Help

Since the problem lies in dependency management, let Gradle handle it.

We wrote a Gradle plugin named `koin-boot-initializer`:

```kotlin
// build.gradle.kts — declare required modules
plugins {
    `koin-boot-initializer`
}

val bootDependencies = listOf(
    projects.component.ktor,    // Networking module
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

### Auto‑generate a Unified Entry Point

At compile time the plugin scans all dependencies to find `BootInitializer` implementations and generates a unified entry file.

```kotlin
// Auto‑generated AppBootInitializer
val AppBootInitializer: KoinBootInitializer = {
    io.github.kamo030.KtorBootInitializer()
    io.github.kamo030.KermitBootInitializer()
    io.github.kamo030.MultiplatformSettingsBootInitializer()
}
```

Usage becomes trivial:

```kotlin
runKoinBoot {
    // Auto‑generated unified entry
    AppBootInitializer()

    properties {
        // Business configuration…
    }
    module {
        // Business module declarations…
    }
}
```

With this plugin, whether adding or removing modules, you only change dependencies in `build.gradle.kts`, without touching startup code — true plug‑and‑play.

## The Result: A Complete Developer Experience

### How the Four Solutions Work Together

Let’s see what the combined experience looks like:

### Step 1: Declare the capabilities you need

In `build.gradle.kts`, declare the component modules just like normal dependencies.
```kotlin
// build.gradle.kts — declare needed components
plugins {
    `koin-boot-initializer`
}

val bootDependencies = listOf(
    projects.component.ktor,    // Networking module
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

### Step 2: Configure desired behavior

At the application entry, invoke the boot function and configure properties and business modules.

```kotlin
// main.kt — concise startup code
fun main() {
    val koin = runKoinBoot {
        // Call the auto‑generated initializer
        AppBootInitializer()

        // Configure via an IDE‑friendly DSL
        properties {
            kermit_severity = Severity.Verbose
            ktor_client_logging_enabled = true
        }

        // Optional: provide a custom implementation
        // module {
        //    single<HttpClientEngine> { OkHttp.create() }
        // }
    }

    // Fetch instances from the container; no need to care about init details
    val httpClient = koin.get<HttpClient>()
    println("HttpClient is ready to use!")
}
```

### Step 3: Enjoy automated runtime

When `runKoinBoot` runs, the framework automatically does the following:

1. Lifecycle management starts. `SentryExtender` is invoked in the Configuring phase so that Sentry is initialized first.
2. Auto‑configuration scans and finds no custom `HttpClientEngine`, so it loads Ktor’s default OkHttp engine.
3. The framework then initializes `HttpClient`. During initialization it reads configurations like `ktor_client_logging_enabled` from the context and decides whether to install the Logging plugin accordingly.
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
4. Once all modules are ready, the application starts successfully.

### True plug‑and‑play

If we decide Kermit logging is no longer needed, it’s simple:

1. Go back to build.gradle.kts and comment out or remove the Kermit dependency.

    ```kotlin
    // build.gradle.kts
    val bootDependencies = listOf(
        projects.component.ktor,
        // remove Kermit dependency
        // projects.component.kermit,
        projects.component.sentry
    )
    ```

2. Sync the Gradle project.

At this point, the `kermit_severity` property previously written in main.kt will immediately turn red in the IDE, indicating that the property no longer exists.

That’s true plug‑and‑play: dependencies determine functionality, and code is automatically validated.

## Summary

### From “Manual” to “Automatic”

With KoinBoot, we’ve upgraded Koin from a pure DI “engine” into an application framework with automation, lifecycle management, and auto‑configuration.

### Core value delivered

- From DI to IoC: more automation so developers can focus on business logic.
- Modular development: add a dependency to gain functionality; remove it to drop the feature automatically.
- Better developer experience: say goodbye to tedious manual configuration and initialization code.
- True plug‑and‑play: Gradle dependencies directly determine app capabilities, with static code checks ensuring consistency.

KoinBoot = Koin + Configuration Management + Lifecycle + Auto‑Configuration + Auto‑Import

Ultimately, we built an efficient, flexible, and maintainable multiplatform scaffold, making each feature module a pluggable default auto‑configuration component to help us build better app architectures.

## Real‑World Case: Dynamic Configuration Management System

### Business scenario

Suppose we’re building an enterprise‑grade mobile app that needs to support:

- Different server configurations for dev/test/prod
- Operators can adjust app behavior dynamically from a console (feature flags, API timeouts, etc.)
- Gray releases: different user cohorts get different configurations
- Changes take effect in real time without restarting the app

### Solution based on KoinBoot

#### 1) Remote configuration fetch extender

Create a `KoinLifecycleExtender` that fetches the latest configuration from a remote server during the Configuring phase at startup.

```kotlin
class RemoteConfigExtender(
    private val configService: ConfigService,
    private val configWatcher: ConfigWatcher
) : KoinLifecycleExtender {

    override fun doConfiguring(context: KoinBootContext) {
        // Fetch the latest configuration during the Configuring phase
        val remoteConfig = fetchRemoteConfig()

        // Merge remote config into local properties
        context.properties.putAll(remoteConfig)

        // Start watching for config changes
        startConfigWatcher(context)
    }

    private fun fetchRemoteConfig(): Map<String, Any> {
        // Call configuration service API
        return configService.getConfiguration(
            appVersion = BuildConfig.VERSION_NAME,
            userId = getCurrentUserId(),
            environment = getCurrentEnvironment()
        )
    }

    private fun startConfigWatcher(context: KoinBootContext) {
        // Use WebSocket or long polling to watch for changes
        configWatcher.onConfigChanged { newConfig ->
            // Update KoinProperties on changes
            context.properties.putAll(newConfig)

            // Trigger reconfiguration of relevant components
            notifyConfigChanged(context, newConfig)
        }
    }
}
```

#### 2) Hot reload mechanism

To react to config changes, we also need a hot reload extender.

```kotlin
class HotReloadExtender(
    private val configWatcher: ConfigWatcher
) : KoinLifecycleExtender {

    override fun doReady(context: KoinBootContext) {
        // After the app is ready, register a change listener
        configWatcher.onConfigChanged { changedKeys ->
            changedKeys.forEach { key ->
                when {
                    key.startsWith("ktor.client") -> {
                        // Reconfigure the HTTP client
                        reconfigureHttpClient(context)
                    }
                    key.startsWith("cache") -> {
                        // Reconfigure cache
                        reconfigureCache(context)
                    }
                    key.startsWith("feature_flags") -> {
                        // Update feature flags
                        updateFeatureFlags(context)
                    }
                }
            }
        }
    }

    private fun reconfigureHttpClient(context: KoinBootContext) {
        val koin = context.koin
        val newConfig = koin.getPropInstance<KtorClientProperties>()

        // Recreate HttpClient instance
        val newClient = HttpClient(koin.get<HttpClientEngine>()) {
            install(HttpTimeout) {
                requestTimeoutMillis = newConfig.timeout.request
                connectTimeoutMillis = newConfig.timeout.connect
            }
        }

        // Replace the instance in the container
        koin.declare(newClient, override = true)
    }
}
```

#### 3) Feature flag management

Feature flags can also be managed type‑safely via the configuration system.

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

        // Configure app behavior based on flags
        if (featureFlags.newUserInterface) {
            enableNewUI()
        }

        if (featureFlags.experimentalFeatures) {
            enableExperimentalFeatures()
        }
    }
}
```

#### 4) Full usage example

Register these extenders into KoinBoot.

```kotlin
fun main() {
    val koin = runKoinBoot {
        // Load all auto‑discovered modules
        AppBootInitializer()

        // Register dynamic configuration extenders
        extenders(
            RemoteConfigExtender(),
            HotReloadExtender(),
            FeatureFlagExtender()
        )

        // Local default configs (as fallback)
        properties {
            ktor_client_timeout_request = 30000L
            feature_flags_newUserInterface = false
            cache_max_size = 1024
        }
    }

}
```

### Benefits achieved

1. Dynamic configuration updates: after operators change settings, all clients update automatically within a short time — no new release needed
2. Gray release support: deliver different configs to different cohorts for progressive rollout and testing
3. Instant incident mitigation: when a newly released feature misbehaves, turn it off immediately via configuration
4. A/B testing: easily deliver different UIs or feature sets to different cohorts for comparison

KoinBoot makes Koin not just a DI container, but a comprehensive solution capable of supporting modern application needs.
It lets developers focus on business logic while the framework handles configuration management, lifecycle control, and other plumbing.
* [KotlinMultiplatform](https://kotlinlang.org/docs/multiplatform.html)
* [Koin](https://insert-koin.io/)
