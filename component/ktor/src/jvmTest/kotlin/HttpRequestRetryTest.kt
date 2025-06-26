package io.github.kamo030.koinboot.component.ktor.test

import io.github.kamo030.koinboot.KtorBootInitializer
import io.github.kamo030.koinboot.core.app_logger_level
import io.github.kamo030.koinboot.core.runKoinBoot
import io.github.kamo030.koinboot.component.ktor.*
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools
import kotlin.test.assertEquals

class HttpRequestRetryTest {

    @AfterEach
    fun stopKoin() =
        KoinPlatformTools.defaultContext().stopKoin()


    fun mockEngineModule(handler: MockRequestHandler) = module {
        single<HttpClientEngine> {
            MockEngine(handler)
        }
    }

    @Test
    fun `test retry on server errors`() = runTest {
        var requestCount = 0
        val maxRetries = 2
        val mockEngineModule = mockEngineModule { request ->
            requestCount++
            if (requestCount <= maxRetries) {
                respond(
                    content = "Service Unavailable",
                    status = HttpStatusCode.ServiceUnavailable,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
            } else {
                respond(
                    content = "OK",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
            }
        }


        val koin = runKoinBoot {
            KtorBootInitializer()
            properties {
                ktor_client_retry_attempts = maxRetries
                ktor_client_retry_delay_millis = 10L
                ktor_client_logging_enabled = true
                app_logger_level = Level.INFO
            }
            modules(mockEngineModule)
        }

        val client = koin.get<HttpClient>()
        val response = client.get("https://test.server/path")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(maxRetries + 1, requestCount)
    }

    @Test
    fun `test retry on specific status codes`() = runTest {
        var requestCount = 0
        val mockEngineModule = mockEngineModule { request ->
            requestCount++
            when (requestCount) {
                1 -> respond(
                    content = "Too Many Requests",
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )

                2 -> respond(
                    content = "Gateway Timeout",
                    status = HttpStatusCode.GatewayTimeout,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )

                else -> respond(
                    content = "OK",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
            }
        }

        val koin = runKoinBoot {
            KtorBootInitializer()
             properties {
                ktor_client_retry_attempts = 2
                ktor_client_retry_delay_millis = 10L
                ktor_client_retry_on_response_codes = setOf(429, 504)
            }
            modules(mockEngineModule)
        }

        val client = koin.get<HttpClient>()
        val response = client.get("https://test.server/path")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(3, requestCount)
    }

    @Test
    fun `test retry on connection timeout`() = runTest {
        var requestCount = 0
        val mockEngineModule = mockEngineModule { request ->
            requestCount++
            if (requestCount == 1) {
                throw ConnectTimeoutException(request.url.toString(), 100)
            } else {
                respond(
                    content = "OK",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
            }
        }

        val koin = runKoinBoot {
            KtorBootInitializer()
            properties {
                ktor_client_retry_attempts = 1
                ktor_client_retry_delay_millis = 10L
                ktor_client_retry_on_connect_timeout = true
            }
            modules(mockEngineModule)
        }

        val client = koin.get<HttpClient>()
        val response = client.get("https://test.server/path")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(2, requestCount)
    }

    @Test
    fun `test retry on all request methods`() = runTest {
        var requestCount = 0
        val mockEngineModule = mockEngineModule { request ->
            requestCount++
            if (requestCount == 1) {
                respond(
                    content = "Service Unavailable",
                    status = HttpStatusCode.ServiceUnavailable,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
            } else {
                respond(
                    content = "OK",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
            }
        }

        val koin = runKoinBoot {
            KtorBootInitializer()
            properties {
                ktor_client_retry_attempts = 1
                ktor_client_retry_delay_millis = 10L
                ktor_client_retry_on_all_requests = true
            }
            modules(mockEngineModule)
        }

        val client = koin.get<HttpClient>()
        // 测试POST请求（非幂等请求）是否会被重试
        val response = client.post("https://test.server/path") {
            setBody("test body")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(2, requestCount)
    }
}