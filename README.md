# KoinBoot

>KoinBoot 是一个基于 Koin 依赖注入框架的自动装配系统，它通过生命周期管理和自动配置机制，简化了应用程序的初始化和配置过程.

## 快速开始

### 1. 添加依赖

在你的项目build.gradle.kts文件中添加KoinBoot插件和相关组件依赖：

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
    `koin-boot-initializer`
}

val bootDependencies = listOf<Dependency>(
    projects.component.ktor,        // HTTP客户端
    projects.component.kermit,      // 日志组件
    projects.component.multiplatformSettings, // 配置存储
    // 根据需要添加其他组件
)

koinBootInitializer {
    includes(bootDependencies)
}

dependencies {
    bootDependencies.forEach(::commonMainApi)
}
```

### 2. 使用KoinBoot启动应用

在应用入口处使用`runKoinBoot`初始化：

```kotlin
fun main() {
   val koin = runKoinBoot {
      AppBootInitializer()
      properties {
         // kermit的严重等级会影响所有的日志，所以先设为Verbose
         kermit_severity = Severity.Verbose
         // app的日志级别设置的是koin容器的日志级别，设为DEBUG后会有详细依赖注入信息
         app_logger_level = Level.DEBUG
         // ktor的日志默认不开启
         ktor_client_logging_enabled = true
         // 设置ktor的日志默认将在控制台看到更多输出
         ktor_client_logging_level = LogLevel.HEADERS
      }
      module {
         // 可以自定义HttpClientEngine, 会自动覆盖默认的HttpClientEngine
         // single<HttpClientEngine> { OkHttp.create() }

         // 可以在默认的HttpClient中进行配置
         single<HttpClientConfigDeclaration> {
            println("call config declaration")
            return@single { install(SSE) }
         }
      }
   }

   runBlocking {
      // 已经自动默认配置好了HttpClient和引擎直接get
      val response = koin.get<HttpClient>().get("https://ktor.io/docs/")
      println("response.status: ${response.status}")
      println("response.bodyAsText: ${response.bodyAsText()}")
      println("done")
   }
}
```

### 3. 开箱即用的功能

- **自动装配**：无需手动初始化和配置各组件
- **配置即代码**：通过properties块轻松配置所有组件
- **约定优于配置**：提供合理默认值，仅需配置必要选项
- **生命周期管理**：组件按正确顺序初始化和清理

## 更多: [指南](GUIDE.md)
