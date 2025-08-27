# KoinBoot Guide

## Table of Contents

1. [KoinBoot Implementation Principles](#koinboot-implementation-principles)
2. [KoinBoot Core Components](#koinboot-core-components)
    - [KoinAutoConfiguration](#koinautoconfiguration)
    - [KoinBootLifecycleExtender](#koinbootlifecycleextender)
    - [KoinBootInitializer](#koinbootinitializer)
3. [koin-boot-initializer Plugin](#koin-boot-initializer-plugin)
4. [Steps to Adapt Third-party Libraries](#steps-to-adapt-third-party-libraries)
    - [Step 1: Create Configuration Properties Class](#step-1-create-configuration-properties-class)
    - [Step 2: Choose Adaptation Method](#step-2-choose-adaptation-method)
    - [Step 3: Create BootInitializer](#step-3-create-bootinitializer)
5. [Detailed Adaptation Methods](#detailed-adaptation-methods)
    - [Method 1: Using KoinAutoConfiguration](#method-1-using-koinautoconfiguration)
    - [Method 2: Using KoinBootLifecycleExtender](#method-2-using-koinbootlifecycleextender)
6. [Existing Adaptation Examples Analysis](#existing-adaptation-examples-analysis)
    - [Ktor Adaptation Analysis](#ktor-adaptation-analysis)
    - [Sentry Adaptation Analysis](#sentry-adaptation-analysis)
    - [MultiplatformSettings Adaptation Analysis](#multiplatformsettings-adaptation-analysis)
    - [Kermit Adaptation Analysis](#kermit-adaptation-analysis)
7. [Best Practices](#best-practices)
8. [Frequently Asked Questions](#frequently-asked-questions)

## KoinBoot Implementation Principles

KoinBoot is an auto-configuration system based on the Koin dependency injection framework. It simplifies application
initialization and configuration processes through lifecycle management and auto-configuration mechanisms. The core
philosophy of KoinBoot is:

1. **Convention over Configuration**: Provide reasonable default configurations to reduce user configuration work
2. **Lifecycle Management**: Ensure components are initialized in the correct order through well-defined startup phases
3. **Extensibility**: Allow functionality extension through auto-configuration and lifecycle extenders

The KoinBoot startup process is divided into the following phases:

1. **Starting**: Startup phase, checking if already started
2. **Configuring**: Configuration phase, executing application declaration and configuring Kermit logging
3. **PropertiesLoading**: Properties loading phase, loading configuration properties
4. **ModulesLoading**: Modules loading phase, loading user modules and executing auto-configuration
5. **Ready**: Ready phase, system is ready
6. **Running**: Running phase, system is running
7. **Stopping**: Stopping phase, system is stopping
8. **Stopped**: Stopped phase, system has stopped

In each phase, KoinBoot executes corresponding operations and calls the appropriate methods of registered lifecycle
extenders. This phase-based design ensures that component initialization and cleanup occur in the correct order.

## KoinBoot Core Components

### KoinAutoConfiguration

`KoinAutoConfiguration` is an interface used to define auto-configuration. It has three main parts:

1. **match()**: Determines whether the configuration should be applied, returns true by default
2. **configure()**: Contains the actual configuration logic
3. **order**: Controls the order of configuration, lower numbers have higher priority

```kotlin
interface KoinAutoConfiguration {
    fun KoinAutoConfigurationScope.match(): Boolean = true
    fun KoinAutoConfigurationScope.configure()
    val order: Int get() = Int.MAX_VALUE
}
```

KoinBoot provides a helper function `koinAutoConfiguration()` to create implementations without subclassing:

```kotlin
val MyAutoConfiguration = koinAutoConfiguration {
    module {
        // Configuration logic
    }
}
```

The main purpose of `KoinAutoConfiguration` is:

1. **Provide default implementations**: When users haven't configured their own implementations, provide defaults
2. **Configure components**: Configure components based on configuration properties
3. **Register dependencies**: Register components to the Koin container

During the ModulesLoading phase, KoinBoot executes all registered auto-configurations, sorted by their order values.

### KoinBootLifecycleExtender

`KoinBootLifecycleExtender` is an interface used to define lifecycle extensions. It provides methods for each lifecycle
phase:

```kotlin
interface KoinBootLifecycleExtender {
    fun doPhaseChange(phase: KoinBootPhase, context: KoinBootContext) {
        // Call appropriate methods based on phase
    }

    fun doStarting(context: KoinBootContext) {}
    fun doConfiguring(context: KoinBootContext) {}
    fun doPropertiesLoading(context: KoinBootContext) {}
    fun doModulesLoading(context: KoinBootContext) {}
    fun doReady(context: KoinBootContext) {}
    fun doRunning(context: KoinBootContext) {}
    fun doStopping(context: KoinBootContext) {}
    fun doStopped(context: KoinBootContext) {}
}
```

The main purpose of `KoinBootLifecycleExtender` is:

1. **Initialize components**: Initialize components at appropriate phases
2. **Clean up resources**: Clean up resources during the stopping phase
3. **Extend functionality**: Extend KoinBoot's functionality at different phases

Lifecycle extenders can perform operations at any phase, but typically initialize components during the Configuring
phase and clean up resources during the Stopping phase.

### KoinBootInitializer

`KoinBootInitializer` is a type alias used to define initialization logic:

```kotlin
typealias KoinBootInitializer = KoinBootDSL.() -> Unit
```

Each library that supports auto-configuration provides a `KoinBootInitializer` to register its auto-configuration or
lifecycle extenders. `KoinBootInitializer` is the bridge connecting libraries to KoinBoot, telling KoinBoot how to
configure and initialize the library.

## koin-boot-initializer Plugin

`koin-boot-initializer` is a Gradle plugin used to automatically collect and compose all BootInitializers from
dependencies. It works by:

1. Scanning BootInitializers in project dependencies
2. Generating a new BootInitializer that composes all BootInitializers
3. Adding the generated BootInitializer to the source code

The plugin's main components include:

1. **KoinBootInitializerExtension**: Extension for configuring the plugin
2. **GenerateKoinBootInitializerTask**: Task for generating BootInitializer
3. **KoinBootInitializerPlugin**: Main plugin class

Plugin configuration options include:

1. **generatedPackage**: Package name for generated BootInitializer, defaults to "io.github.kamo030.koinboot.generated"
2. **generatedInitializerName**: Name of generated BootInitializer, defaults to "AppBootInitializer"
3. **include**: Include specified dependencies
4. **includes**: Include multiple dependencies

Plugin usage:

```kotlin
plugins {
    // Other plugins
    `koin-boot-initializer`
}

val componentDependencies = listOf<Dependency>(
    projects.component.kermit,
    projects.component.multiplatformSettings,
    projects.component.sentry,
    projects.component.ktor,
)

koinBootInitializer {
    includes(componentDependencies)
}
```

The plugin generates a BootInitializer similar to the following:

```kotlin
// Generated AppBootInitializer example
/**
 * Auto-generated KoinBootInitializer
 * This file is automatically generated by a Gradle build script based on project dependencies.
 * Do not modify this file manually.
 *
 * Found BootInitializers from dependencies:
 * - [io.github.kamo030.koinboot.KermitBootInitializer]
 * - [io.github.kamo030.koinboot.MultiplatformSettingsBootInitializer]
 * - [io.github.kamo030.koinboot.SentryBootInitializer]
 * - [io.github.kamo030.koinboot.KtorBootInitializer]
 */
val AppBootInitializer: KoinBootInitializer = {
    KermitBootInitializer()
    MultiplatformSettingsBootInitializer()
    SentryBootInitializer()
    KtorBootInitializer()
}
```

This generated BootInitializer calls all BootInitializers from included dependencies, achieving auto-configuration.

## Steps to Adapt Third-party Libraries

Adapting third-party libraries to support KoinBoot auto-configuration typically includes the following steps:

### Step 1: Create Configuration Properties Class

First, create a configuration properties class to define library configuration options:

```kotlin
@KoinPropInstance("mylib")
data class MyLibProperties(
    val enabled: Boolean = true,
    val timeout: Long = 30000,
    // Other configuration options
) {
    companion object {
        const val MYLIB_ENABLED = "mylib.enabled"
        const val MYLIB_TIMEOUT = "mylib.timeout"
        // Other constants
    }
}

// Extension properties for easy access
var KoinProperties.mylib_enabled: Boolean
get() = (this[MyLibProperties.MYLIB_ENABLED] as Boolean?) ?: true
set(value) {
    MyLibProperties.MYLIB_ENABLED(value)
}

var KoinProperties.mylib_timeout: Long
get() = (this[MyLibProperties.MYLIB_TIMEOUT] as Long?) ?: 30000
set(value) {
    MyLibProperties.MYLIB_TIMEOUT(value)
}
```

The configuration properties class should:

1. Use the `@KoinPropInstance` annotation, specifying the property prefix
2. Provide reasonable default values
3. Define constants representing property names
4. Provide extension properties for easy access

### Step 2: Choose Adaptation Method

There are two main methods for adapting third-party libraries:

1. **Using KoinAutoConfiguration**: Suitable for libraries that need to register dependencies, such as Ktor,
   MultiplatformSettings
2. **Using KoinBootLifecycleExtender**: Suitable for libraries that need to perform initialization or cleanup at
   specific lifecycle phases, such as Sentry, Kermit

The choice depends on the library's characteristics and requirements:

- If the library mainly needs to register dependencies, use KoinAutoConfiguration
- If the library needs to perform operations at specific lifecycle phases, use KoinBootLifecycleExtender
- If both are needed, both methods can be used simultaneously

### Step 3: Create BootInitializer

Finally, create a BootInitializer to register your auto-configuration or lifecycle extenders:

```kotlin
// If using KoinAutoConfiguration
val MyLibBootInitializer: KoinBootInitializer = {
    autoConfigurations(MyLibAutoConfiguration)
}

// If using KoinBootLifecycleExtender
val MyLibBootInitializer: KoinBootInitializer = {
    extenders(MyLibExtender())
}

// If using both
val MyLibBootInitializer: KoinBootInitializer = {
    autoConfigurations(MyLibAutoConfiguration)
    extenders(MyLibExtender())
}
```

The BootInitializer should:

1. Be visible at package level so the koin-boot-initializer plugin can find it
2. Use the library name as prefix, such as `KtorBootInitializer`
3. Register auto-configuration or lifecycle extenders

## Detailed Adaptation Methods

### Method 1: Using KoinAutoConfiguration

If you choose to use KoinAutoConfiguration, create an implementation:

```kotlin
internal val MyLibAutoConfiguration = koinAutoConfiguration {
    // Get configuration properties
    val properties = koin.getPropInstance<MyLibProperties> { MyLibProperties() }

    module {
        // Automatically create default client when user hasn't configured MyLibClient
        onMissInstances<MyLibClient> {
            single<MyLibClient> {
                MyLibClient(properties.timeout)
            }
        }

        // Other dependency configurations
    }
}
```

The KoinAutoConfiguration implementation should:

1. Get configuration properties
2. Use `onMissInstances` to check if users have already configured components
3. Configure components based on configuration properties
4. Register dependencies

`onMissInstances` is a helper function that checks if users have already configured an instance of a certain type. If
not, it executes the provided code block. This ensures users can override default implementations.

### Method 2: Using KoinBootLifecycleExtender

If you choose to use KoinBootLifecycleExtender, create an implementation:

```kotlin
class MyLibExtender : KoinBootLifecycleExtender {
    override fun doConfiguring(context: KoinBootContext) = with(context) {
        // Get configuration properties
        val properties = properties.asPropInstance<MyLibProperties>() ?: MyLibProperties()

        if (!properties.enabled) return@with

        // Initialize library
        initMyLib(properties)
    }

    private fun initMyLib(properties: MyLibProperties) {
        // Initialization code
    }

    override fun doStopping(context: KoinBootContext) {
        // Cleanup code
    }
}
```

The KoinBootLifecycleExtender implementation should:

1. Override relevant lifecycle methods
2. Initialize components in the doConfiguring method
3. Clean up resources in the doStopping method
4. Check configuration properties to decide whether to perform operations

## Existing Adaptation Examples Analysis

### Ktor Adaptation Analysis

Ktor uses the KoinAutoConfiguration approach:

1. **KtorProperties**: Defines configuration options for Ktor client, such as timeout, logging, retry, etc.
2. **KtorAutoConfiguration**: Created using koinAutoConfiguration, configures HttpClient and related components
3. **KtorBootInitializer**: Registers KtorAutoConfiguration

```kotlin
val KtorBootInitializer: KoinBootInitializer = {
    autoConfigurations(KtorAutoConfiguration)
}
```

Ktor adaptation features:

1. Provides rich configuration options such as timeout, logging, retry, etc.
2. Uses `onMissInstances` to check if users have already configured HttpClient, Json, etc.
3. Configures various HttpClient plugins based on configuration properties
4. Supports user-defined HttpClientEngine

### Sentry Adaptation Analysis

Sentry uses the KoinBootLifecycleExtender approach:

1. **SentryProperties**: Defines Sentry configuration options such as DSN, environment, release version, etc.
2. **SentryExtender**: Implements KoinBootLifecycleExtender, initializes Sentry during doConfiguring phase
3. **SentryBootInitializer**: Registers SentryExtender

```kotlin
val SentryBootInitializer: KoinBootInitializer = {
    extenders(SentryExtender())
}
```

Sentry adaptation features:

1. Initializes Sentry during doConfiguring phase
2. Configures Sentry based on configuration properties
3. Supports disabling Sentry

### MultiplatformSettings Adaptation Analysis

MultiplatformSettings uses the KoinAutoConfiguration approach:

1. **MultiplatformSettingsProperties**: Defines MultiplatformSettings configuration options
2. **MultiplatformSettingsAutoConfiguration**: Created using koinAutoConfiguration, configures Settings and
   Settings.Factory
3. **MultiplatformSettingsBootInitializer**: Registers MultiplatformSettingsAutoConfiguration

```kotlin
val MultiplatformSettingsBootInitializer: KoinBootInitializer = {
    autoConfigurations(MultiplatformSettingsAutoConfiguration)
}
```

MultiplatformSettings adaptation features:

1. Provides default implementations for Settings and Settings.Factory
2. Supports user-defined Settings.Factory
3. Configures Settings based on configuration properties

### Kermit Adaptation Analysis

Kermit uses the KoinBootLifecycleExtender approach:

1. **KermitProperties**: Defines Kermit configuration options
2. **KermitExtender**: Implements KoinBootLifecycleExtender, configures Kermit logging
3. **KermitBootInitializer**: Registers KermitExtender

```kotlin
val KermitBootInitializer: KoinBootInitializer = {
    extenders(KermitExtender())
}
```

Kermit adaptation features:

1. Configures Kermit logging during doConfiguring phase
2. Supports configuring log levels
3. Supports adding loggers

## Best Practices

1. **Provide reasonable default values**: Ensure configuration properties have reasonable defaults to reduce user
   configuration work
2. **Use onMissInstances**: Only provide default implementations when users haven't defined their own
3. **Consider platform specificity**: For multiplatform libraries, use expect/actual to handle platform-specific code
4. **Document configuration options**: Clearly document all configuration options and their default values
5. **Follow naming conventions**: Use consistent naming conventions such as `<LibraryName>Properties`,
   `<LibraryName>AutoConfiguration`, `<LibraryName>Extender`, `<LibraryName>BootInitializer`
6. **Modular design**: Break functionality into small, composable modules
7. **Test adaptation code**: Write tests to ensure adaptation code works correctly in various scenarios
8. **Consider conditional configuration**: Use the match() method to conditionally apply configurations
9. **Pay attention to configuration order**: Use the order property to control configuration sequence
10. **Provide extension points**: Allow users to extend your adaptation code

## Frequently Asked Questions

### Q: How to choose the adaptation method?

A: The choice of adaptation method depends on the library's characteristics and requirements:

- If the library mainly needs to register dependencies, use KoinAutoConfiguration
- If the library needs to perform operations at specific lifecycle phases, use KoinBootLifecycleExtender
- If both are needed, both methods can be used simultaneously

### Q: How to handle platform-specific code?

A: For multiplatform libraries, use expect/actual to handle platform-specific code:

```kotlin
// In commonMain
expect fun createPlatformSpecificComponent(properties: MyLibProperties): MyLibComponent

// In androidMain
actual fun createPlatformSpecificComponent(properties: MyLibProperties): MyLibComponent {
    // Android-specific implementation
}

// In iosMain
actual fun createPlatformSpecificComponent(properties: MyLibProperties): MyLibComponent {
    // iOS-specific implementation
}
```

### Q: How to handle relationships between dependencies?

A: Use the order property to control configuration sequence, ensuring dependencies are initialized in the correct order:

```kotlin
val MyLibAutoConfiguration = koinAutoConfiguration(order = 100) {
    // Configuration logic
}

val MyOtherLibAutoConfiguration = koinAutoConfiguration(order = 200) {
    // Configuration logic, depends on MyLibAutoConfiguration
}
```

Lower numbers have higher priority, so MyLibAutoConfiguration will execute before MyOtherLibAutoConfiguration.