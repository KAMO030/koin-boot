package io.github.kamo030.koinboot.sample

import co.touchlab.kermit.Severity
import io.github.kamo030.generated.AppBootInitializer
import io.github.kamo030.koinboot.component.kermit.kermit_severity
import io.github.kamo030.koinboot.component.ktor.HttpClientConfigDeclaration
import io.github.kamo030.koinboot.component.ktor.ktor_client_logging_enabled
import io.github.kamo030.koinboot.component.ktor.ktor_client_logging_level
import io.github.kamo030.koinboot.core.app_logger_level
import io.github.kamo030.koinboot.core.runKoinBoot
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.koin.core.logger.Level

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